package com.usmanzade.timetable.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class Repository(context: Context) {
    private val dir = File(context.filesDir, "week-cache").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val mem = ConcurrentHashMap<String, List<Lesson>>()
    private val ser = ListSerializer(Lesson.serializer())

    private fun file(s: Subject, monday: LocalDate) = File(dir, "${s.key}_$monday.json")

    fun cached(s: Subject, monday: LocalDate): List<Lesson>? {
        val k = "${s.key}_$monday"
        mem[k]?.let { return it }
        val f = file(s, monday)
        if (!f.exists()) return null
        return runCatching { json.decodeFromString(ser, f.readText()) }.getOrNull()?.also { mem[k] = it }
    }

    fun ageMillis(s: Subject, monday: LocalDate): Long {
        val f = file(s, monday)
        return if (f.exists()) System.currentTimeMillis() - f.lastModified() else Long.MAX_VALUE
    }

    suspend fun fetchWeek(s: Subject, monday: LocalDate): List<Lesson> {
        val lessons = SpbuApi.events(s, monday, monday.plusDays(6)).sortedBy { it.start }
        mem["${s.key}_$monday"] = lessons
        if (!snapFile(s, monday).exists()) saveSnapshot(s, monday, lessons)
        runCatching { file(s, monday).writeText(json.encodeToString(ser, lessons)) }
        return lessons
    }

    private fun snapFile(s: Subject, monday: LocalDate) = File(dir, "snap_${s.key}_$monday.json")

    fun snapshot(s: Subject, monday: LocalDate): List<Lesson>? {
        val f = snapFile(s, monday)
        if (!f.exists()) return null
        return runCatching { json.decodeFromString(ser, f.readText()) }.getOrNull()
    }

    fun saveSnapshot(s: Subject, monday: LocalDate, lessons: List<Lesson>) {
        runCatching { snapFile(s, monday).writeText(json.encodeToString(ser, lessons)) }
    }

    fun forget(s: Subject) {
        mem.keys.filter { it.startsWith(s.key + "_") }.forEach { mem.remove(it) }
        dir.listFiles()?.filter { it.name.contains(s.key + "_") }?.forEach { it.delete() }
    }
}
