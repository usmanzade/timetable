package com.usmanzade.timetable.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

sealed interface EducatorResolution {
    data class One(val educator: EducatorInfo) : EducatorResolution
    data class Many(val candidates: List<EducatorInfo>) : EducatorResolution
    data object Failed : EducatorResolution
}

object EducatorMatch {
    private class Parsed(val surname: String, val initials: List<Char>)

    private fun norm(s: String) = s.lowercase().replace('ё', 'е')

    private fun parse(name: String): Parsed? {
        val t = name.replace(".", " ").replace(",", " ").split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        if (t.isEmpty()) return null
        return Parsed(norm(t[0]), t.drop(1).map { norm(it).first() })
    }

    private fun sameSurname(p: Parsed, e: EducatorInfo) = parse(e.name)?.surname == p.surname

    private fun strictMatches(p: Parsed, found: Collection<EducatorInfo>): List<EducatorInfo> =
        found.filter { e ->
            val q = parse(e.name) ?: return@filter false
            q.surname == p.surname && (p.initials.isEmpty() || q.initials.take(p.initials.size) == p.initials)
        }

    suspend fun resolve(
        name: String,
        lesson: Lesson?,
        search: suspend (String) -> List<EducatorInfo>,
        eventsOf: suspend (EducatorInfo, LocalDate, LocalDate) -> List<Lesson>,
    ): EducatorResolution {
        val p = parse(name) ?: return EducatorResolution.Failed
        val cap = { c: Char -> c.uppercaseChar() }
        val queries = buildList {
            if (p.initials.size >= 2) add("${name.trim().split(" ", ".", ",").first()} ${cap(p.initials[0])} ${cap(p.initials[1])}")
            if (p.initials.isNotEmpty()) add("${name.trim().split(" ", ".", ",").first()} ${cap(p.initials[0])}")
            if (p.initials.size >= 2) add("${name.trim().split(" ", ".", ",").first()} ${cap(p.initials[1])}")
            add(name.trim().split(" ", ".", ",").first())
        }.distinct()

        val all = LinkedHashMap<Long, EducatorInfo>()
        var anySuccess = false
        for (q in queries) {
            try {
                search(q).also { anySuccess = true }.forEach { all.putIfAbsent(it.id, it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                continue
            }

            if (lesson == null && strictMatches(p, all.values).size == 1) break
        }
        if (!anySuccess) return EducatorResolution.Failed

        val strict = strictMatches(p, all.values)
        val pool = strict.ifEmpty { all.values.filter { sameSurname(p, it) } }
        if (pool.isEmpty()) return EducatorResolution.Failed
        if (pool.size == 1 && strict.size == 1) return EducatorResolution.One(pool.first())

        if (lesson != null) {
            val monday = lesson.date.weekMonday()
            val checked = coroutineScope {
                pool.take(10).map { e ->
                    async {
                        val events = try {
                            eventsOf(e, monday, monday.plusDays(6))
                        } catch (x: kotlinx.coroutines.CancellationException) {
                            throw x
                        } catch (x: Exception) {
                            emptyList()
                        }
                        e to score(lesson, events)
                    }
                }.awaitAll()
            }
            val strong = checked.filter { it.second == 2 }.map { it.first }
            val weak = checked.filter { it.second == 1 }.map { it.first }
            val verified = strong.ifEmpty { weak }
            if (verified.size == 1) return EducatorResolution.One(verified.first())
            if (verified.size > 1) return EducatorResolution.Many(verified)
        }
        return if (pool.size == 1) EducatorResolution.One(pool.first()) else EducatorResolution.Many(pool.toList())
    }

    private fun score(lesson: Lesson, events: List<Lesson>): Int {
        var best = 0
        for (ev in events) {
            if (ev.start != lesson.start || ev.end != lesson.end) continue
            val sameTitle = norm(ev.subject) == norm(lesson.subject) || norm(ev.title) == norm(lesson.title)
            best = maxOf(best, if (sameTitle) 2 else 1)
        }
        return best
    }
}
