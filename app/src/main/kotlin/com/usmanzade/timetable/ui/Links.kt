package com.usmanzade.timetable.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object Links {
    const val AUTHOR_GITHUB = "https://github.com/usmanzade"
    const val APP_REPO = "github.com/usmanzade/timetable"
    const val RELEASES = "https://github.com/usmanzade/timetable/releases"
}

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private val ONLINE = listOf(
    "онлайн", "online", "дистанц", "zoom", "teams", "webinar", "вебинар", "ссылк",
    "информационно-коммуникац", "коммуникационных технологий", "электронн",
)
private val ICT = Regex("(?<![а-яa-z])икт(?![а-яa-z])", RegexOption.IGNORE_CASE)

fun isMappable(location: String): Boolean {
    val l = location.lowercase()
    return location.isNotBlank() && ONLINE.none { l.contains(it) } && !ICT.containsMatchIn(location)
}

fun openMap(context: Context, location: String) {

    val address = location.replace(Regex(",?\\s*ауд(итория)?\\.?.*$", RegexOption.IGNORE_CASE), "").trim()
    val query = if (address.contains("петербург", ignoreCase = true)) address else "Санкт-Петербург, $address"
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(geo)
    } catch (e: ActivityNotFoundException) {
        openUrl(context, "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
    }
}
