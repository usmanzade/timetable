package com.usmanzade.timetable.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

class ApiException(val code: Int, message: String) : Exception(message)

object SpbuApi {
    private const val BASE = "https://timetable.spbu.ru/api/v1/"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun get(path: String): JsonElement = withContext(Dispatchers.IO) {
        val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "ru")
            setRequestProperty("User-Agent", "SPbU-Timetable-Android/1.0")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw ApiException(code, "HTTP $code")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            json.parseToJsonElement(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonElement?.arr(): JsonArray? = this as? JsonArray
    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val v = this[k]
            if (v is JsonPrimitive && v.contentOrNull != null) return v.contentOrNull
        }
        return null
    }
    private fun JsonObject.long(vararg keys: String): Long? {
        for (k in keys) {
            val v = this[k]
            if (v is JsonPrimitive) v.longOrNull?.let { return it }
        }
        return null
    }
    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

    suspend fun divisions(): List<Division> =
        get("study/divisions").arr().orEmpty().mapNotNull {
            val o = it.obj() ?: return@mapNotNull null
            Division(o.str("Alias") ?: return@mapNotNull null, o.str("Name") ?: return@mapNotNull null)
        }

    suspend fun levels(alias: String): List<StudyLevel> =
        get("study/divisions/${enc(alias)}/programs/levels").arr().orEmpty().mapNotNull { lv ->
            val o = lv.obj() ?: return@mapNotNull null
            StudyLevel(
                name = o.str("StudyLevelName") ?: "",
                programs = o["StudyProgramCombinations"].arr().orEmpty().mapNotNull { p ->
                    val po = p.obj() ?: return@mapNotNull null
                    StudyProgram(
                        name = po.str("Name") ?: "",
                        years = po["AdmissionYears"].arr().orEmpty().mapNotNull { y ->
                            val yo = y.obj() ?: return@mapNotNull null
                            StudyYear(
                                programId = yo.long("StudyProgramId") ?: return@mapNotNull null,
                                name = yo.str("YearName") ?: yo.str("YearNumber") ?: "",
                                empty = yo.bool("IsEmpty"),
                            )
                        },
                    )
                },
            )
        }

    suspend fun groups(programId: Long): List<GroupInfo> {

        val root = try {
            get("progams/$programId/groups")
        } catch (e: ApiException) {
            get("programs/$programId/groups")
        }
        val list = root.arr() ?: root.obj()?.get("Groups").arr().orEmpty()
        return list.mapNotNull {
            val o = it.obj() ?: return@mapNotNull null
            GroupInfo(
                id = o.long("StudentGroupId", "Id") ?: return@mapNotNull null,
                name = o.str("StudentGroupName", "Name") ?: return@mapNotNull null,
                form = o.str("StudentGroupStudyForm") ?: "",
            )
        }
    }

    suspend fun searchEducators(query: String): List<EducatorInfo> {
        val root = get("educators/search/${enc(query)}")
        val list = root.arr() ?: root.obj()?.get("Educators").arr().orEmpty()
        return list.mapNotNull {
            val o = it.obj() ?: return@mapNotNull null
            EducatorInfo(
                id = o.long("Id", "EducatorId", "EducatorMasterId") ?: return@mapNotNull null,
                name = o.str("FullName", "DisplayName", "EducatorDisplayText", "LongName") ?: return@mapNotNull null,
            )
        }
    }

    suspend fun events(subject: Subject, from: LocalDate, to: LocalDate): List<Lesson> {
        val root = when (subject.kind) {
            Subject.Kind.GROUP -> get("groups/${subject.id}/events/$from/$to")
            Subject.Kind.EDUCATOR -> try {
                get("educators/${subject.id}/events/$from/$to")
            } catch (e: ApiException) {
                get("educators/${subject.id}/events")
            }
        }
        val obj = root.obj() ?: return emptyList()
        val days = obj["Days"].arr()
            ?: obj["EducatorEventsDays"].arr()
            ?: obj.values.firstOrNull { v ->
                v is JsonArray && v.firstOrNull().obj()?.containsKey("DayStudyEvents") == true
            }.arr()
            ?: return emptyList()

        val out = ArrayList<Lesson>()
        for (day in days) {
            for (ev in day.obj()?.get("DayStudyEvents").arr().orEmpty()) {
                val e = ev.obj() ?: continue
                val start = e.str("Start") ?: continue
                val end = e.str("End") ?: start
                val lesson = Lesson(
                    subject = e.str("Subject")?.trim().orEmpty(),
                    start = start,
                    end = end,
                    locations = e.str("LocationsDisplayText").orEmpty(),
                    educators = e.str("EducatorsDisplayText").orEmpty(),
                    educatorRefs = e["EducatorIds"].arr().orEmpty().mapNotNull { x ->
                        val o = x.obj() ?: return@mapNotNull null
                        val id = o.long("Item1", "Id") ?: return@mapNotNull null
                        EducatorRef(id, o.str("Item2", "Name", "DisplayName").orEmpty())
                    },
                    groups = e.str("ContingentUnitsDisplayTest", "ContingentUnitName").orEmpty(),
                    cancelled = e.bool("IsCancelled"),
                    timeChanged = e.bool("TimeWasChanged"),
                    locationChanged = e.bool("LocationsWereChanged"),
                    educatorChanged = e.bool("EducatorsWereReassigned"),
                    allDay = e.bool("AllDay"),
                )
                out += lesson
            }
        }

        return out.filter { it.date >= from && it.date <= to }
    }
}
