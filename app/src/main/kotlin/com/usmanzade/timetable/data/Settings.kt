package com.usmanzade.timetable.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.Locale

enum class CalendarMode { DAY, WEEK, MONTH }

class Settings(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("timetable", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _favorites = MutableStateFlow(readList(KEY_FAVORITES, Subject.serializer()))
    val favorites: StateFlow<List<Subject>> = _favorites

    private val _activeKey = MutableStateFlow(sp.getString(KEY_ACTIVE, null))
    val activeKey: StateFlow<String?> = _activeKey

    fun addFavorite(s: Subject, makeActive: Boolean = true) {
        val list = _favorites.value
        if (list.none { it.key == s.key }) {
            _favorites.value = list + s
            writeList(KEY_FAVORITES, Subject.serializer(), _favorites.value)
        }
        if (makeActive) setActive(s.key)
    }

    fun renameFavorite(key: String, newName: String) {
        _favorites.value = _favorites.value.map {
            if (it.key == key) it.copy(customName = newName.trim().ifBlank { null }) else it
        }
        writeList(KEY_FAVORITES, Subject.serializer(), _favorites.value)
    }

    fun removeFavorite(key: String) {
        _favorites.value = _favorites.value.filterNot { it.key == key }
        writeList(KEY_FAVORITES, Subject.serializer(), _favorites.value)
        if (_activeKey.value == key) setActive(_favorites.value.firstOrNull()?.key)
    }

    fun setActive(key: String?) {
        _activeKey.value = key
        sp.edit().putString(KEY_ACTIVE, key).apply()
    }

    fun activeSubject(): Subject? = _favorites.value.firstOrNull { it.key == _activeKey.value }

    private val _theme = MutableStateFlow(sp.getInt(KEY_THEME, 0))
    val theme: StateFlow<Int> = _theme
    fun setTheme(v: Int) { _theme.value = v; sp.edit().putInt(KEY_THEME, v).apply() }

    private val _monet = MutableStateFlow(sp.getBoolean(KEY_MONET, false))
    val monet: StateFlow<Boolean> = _monet
    fun setMonet(v: Boolean) { _monet.value = v; sp.edit().putBoolean(KEY_MONET, v).apply() }

    private val _language = MutableStateFlow(sp.getInt(KEY_LANG, 1))
    val language: StateFlow<Int> = _language
    fun setLanguage(v: Int) { _language.value = v; sp.edit().putInt(KEY_LANG, v).apply() }

    fun isRussian(): Boolean = when (_language.value) {
        1 -> true
        2 -> false
        else -> Locale.getDefault().language == "ru"
    }

    private val _mode = MutableStateFlow(CalendarMode.entries[sp.getInt(KEY_MODE, CalendarMode.MONTH.ordinal)])
    val calendarMode: StateFlow<CalendarMode> = _mode
    fun setCalendarMode(m: CalendarMode) { _mode.value = m; sp.edit().putInt(KEY_MODE, m.ordinal).apply() }

    private val _typeColors = MutableStateFlow(readTypeColors())
    val typeColors: StateFlow<Map<LessonCategory, Int>> = _typeColors

    private val _customColors = MutableStateFlow(sp.getBoolean(KEY_CUSTOM_COLORS, false))
    val customColors: StateFlow<Boolean> = _customColors
    fun setCustomColors(v: Boolean) { _customColors.value = v; sp.edit().putBoolean(KEY_CUSTOM_COLORS, v).apply() }

    fun colorFor(c: LessonCategory): Int =
        if (_customColors.value) _typeColors.value[c] ?: c.defaultColor.toInt() else c.defaultColor.toInt()

    fun setTypeColor(c: LessonCategory, argb: Int) {
        _typeColors.value = _typeColors.value + (c to argb)
        saveTypeColors()
    }

    fun resetTypeColors() {
        _typeColors.value = emptyMap()
        saveTypeColors()
    }

    private fun saveTypeColors() {
        sp.edit().putString(KEY_TYPE_COLORS, _typeColors.value.entries.joinToString(";") { "${it.key.name}=${it.value}" }).apply()
    }

    private fun readTypeColors(): Map<LessonCategory, Int> =
        (sp.getString(KEY_TYPE_COLORS, "") ?: "").split(";").mapNotNull {
            val parts = it.split("=")
            val cat = runCatching { LessonCategory.valueOf(parts.getOrNull(0) ?: "") }.getOrNull()
            val v = parts.getOrNull(1)?.toIntOrNull()
            if (cat != null && v != null) cat to v else null
        }.toMap()

    private val _notify = MutableStateFlow(sp.getBoolean(KEY_NOTIFY, true))
    val notifyEnabled: StateFlow<Boolean> = _notify
    fun setNotifyEnabled(v: Boolean) { _notify.value = v; sp.edit().putBoolean(KEY_NOTIFY, v).apply() }

    private val _interval = MutableStateFlow(sp.getInt(KEY_INTERVAL, 60))
    val intervalMinutes: StateFlow<Int> = _interval
    fun setInterval(v: Int) { _interval.value = v; sp.edit().putInt(KEY_INTERVAL, v).apply() }

    private val _types = MutableStateFlow(
        sp.getStringSet(KEY_TYPES, null)?.mapNotNull { runCatching { ChangeType.valueOf(it) }.getOrNull() }?.toSet()
            ?: ChangeType.entries.toSet(),
    )
    val enabledTypes: StateFlow<Set<ChangeType>> = _types
    fun setTypeEnabled(t: ChangeType, on: Boolean) {
        _types.value = if (on) _types.value + t else _types.value - t
        sp.edit().putStringSet(KEY_TYPES, _types.value.map { it.name }.toSet()).apply()
    }

    private val _changes = MutableStateFlow(readList(KEY_CHANGES, ChangeRecord.serializer()))
    val changes: StateFlow<List<ChangeRecord>> = _changes

    fun addChanges(records: List<ChangeRecord>) {
        if (records.isEmpty()) return
        _changes.value = (records + _changes.value).take(200)
        writeList(KEY_CHANGES, ChangeRecord.serializer(), _changes.value)
    }

    fun markAllRead() {
        if (_changes.value.none { !it.read }) return
        _changes.value = _changes.value.map { it.copy(read = true) }
        writeList(KEY_CHANGES, ChangeRecord.serializer(), _changes.value)
    }

    fun clearChanges() {
        _changes.value = emptyList()
        writeList(KEY_CHANGES, ChangeRecord.serializer(), _changes.value)
    }

    private fun <T> readList(key: String, ser: kotlinx.serialization.KSerializer<T>): List<T> =
        runCatching { json.decodeFromString(ListSerializer(ser), sp.getString(key, "[]") ?: "[]") }
            .getOrDefault(emptyList())

    private fun <T> writeList(key: String, ser: kotlinx.serialization.KSerializer<T>, list: List<T>) {
        sp.edit().putString(key, json.encodeToString(ListSerializer(ser), list)).apply()
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_ACTIVE = "active"
        const val KEY_THEME = "theme"
        const val KEY_MONET = "monet"
        const val KEY_LANG = "lang"
        const val KEY_MODE = "mode"
        const val KEY_NOTIFY = "notify"
        const val KEY_INTERVAL = "interval"
        const val KEY_TYPES = "types"
        const val KEY_CHANGES = "changes"
        const val KEY_TYPE_COLORS = "type_colors"
        const val KEY_CUSTOM_COLORS = "custom_colors"
    }
}
