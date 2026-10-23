package com.usmanzade.timetable.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Serializable
data class Subject(
    val kind: Kind,
    val id: String,
    val name: String,
    val customName: String? = null,
) {
    @Serializable
    enum class Kind { GROUP, EDUCATOR }

    val key: String get() = "${kind.name.lowercase()}_$id"

    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: name
}

enum class LessonCategory(val ru: String, val en: String, val defaultColor: Long) {
    LECTURE("Лекция", "Lecture", 0xFF3482FF),
    SEMINAR("Семинар", "Seminar", 0xFF2DB86A),
    PRACTICE("Практическое занятие", "Practical class", 0xFF14B8A6),
    LAB("Лабораторная работа", "Laboratory work", 0xFF8B5CF6),
    EXAM("Экзамен", "Exam", 0xFFE94634),
    CREDIT("Зачёт", "Credit", 0xFFF59E0B),
    CONSULT("Консультация", "Consultation", 0xFF06B6D4),
    OTHER("Другое", "Other", 0xFF9CA3AF),
}

fun categoryOf(type: String?): LessonCategory {
    val t = type?.lowercase()?.trim() ?: return LessonCategory.OTHER
    return when {
        t.startsWith("лекц") || t.startsWith("lecture") -> LessonCategory.LECTURE
        t.startsWith("семин") || t.startsWith("seminar") -> LessonCategory.SEMINAR
        t.startsWith("практ") || t.startsWith("practic") -> LessonCategory.PRACTICE
        t.startsWith("лабор") || t.startsWith("lab") -> LessonCategory.LAB
        t.startsWith("экзам") || t.startsWith("exam") -> LessonCategory.EXAM
        t.startsWith("зач") || t.startsWith("test") || t.startsWith("credit") -> LessonCategory.CREDIT
        t.startsWith("консульт") || t.startsWith("consult") -> LessonCategory.CONSULT
        else -> LessonCategory.OTHER
    }
}

@Serializable
data class EducatorRef(val id: Long, val name: String)

@Serializable
data class Lesson(
    val subject: String,
    val start: String,
    val end: String,
    val locations: String = "",
    val educators: String = "",
    val educatorRefs: List<EducatorRef> = emptyList(),
    val groups: String = "",
    val cancelled: Boolean = false,
    val timeChanged: Boolean = false,
    val locationChanged: Boolean = false,
    val educatorChanged: Boolean = false,
    val allDay: Boolean = false,
) {
    val startDt: LocalDateTime get() = LocalDateTime.parse(start)
    val endDt: LocalDateTime get() = LocalDateTime.parse(end)
    val date: LocalDate get() = startDt.toLocalDate()
    val category: LessonCategory get() = categoryOf(type)

    val title: String
        get() {
            val i = subject.lastIndexOf(", ")
            return if (i > 0 && isKnownType(subject.substring(i + 2))) subject.substring(0, i) else subject
        }

    val type: String?
        get() {
            val i = subject.lastIndexOf(", ")
            return if (i > 0 && isKnownType(subject.substring(i + 2))) subject.substring(i + 2).trim() else null
        }

    private fun isKnownType(s: String): Boolean {
        val l = s.trim().lowercase()
        return KNOWN_TYPES.any { l.startsWith(it) }
    }

    companion object {
        val KNOWN_TYPES = listOf(
            "лекци", "семинар", "практи", "лаборатор", "экзамен", "зачет", "зачёт",
            "консультац", "контрольн", "коллоквиум", "курсов", "защит", "пересдач",
            "lecture", "seminar", "practic", "lab", "exam", "test",
        )
    }
}

enum class ChangeType { ADDED, REMOVED, CANCELLED, TIME, LOCATION, EDUCATOR }

@Serializable
data class ChangeRecord(
    val id: Long,
    val at: Long,
    val subjectKey: String,
    val subjectName: String,
    val type: ChangeType,
    val title: String,
    val date: String,
    val detail: String = "",
    val read: Boolean = false,
)

data class Division(val alias: String, val name: String)
data class StudyYear(val programId: Long, val name: String, val empty: Boolean)
data class StudyProgram(val name: String, val years: List<StudyYear>)
data class StudyLevel(val name: String, val programs: List<StudyProgram>)
data class GroupInfo(val id: Long, val name: String, val form: String)
data class EducatorInfo(val id: Long, val name: String)

fun LocalDate.weekMonday(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
