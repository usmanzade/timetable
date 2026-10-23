package com.usmanzade.timetable

import com.usmanzade.timetable.data.ChangeType

class Tr(val ru: Boolean) {
    operator fun invoke(ru: String, en: String): String = if (this.ru) ru else en
}

fun ChangeType.label(t: Tr): String = when (this) {
    ChangeType.ADDED -> t("Новое занятие", "New class")
    ChangeType.REMOVED -> t("Занятие удалено", "Class removed")
    ChangeType.CANCELLED -> t("Занятие отменено", "Class cancelled")
    ChangeType.TIME -> t("Перенос времени", "Time changed")
    ChangeType.LOCATION -> t("Другая аудитория", "Room changed")
    ChangeType.EDUCATOR -> t("Замена преподавателя", "Teacher changed")
}
