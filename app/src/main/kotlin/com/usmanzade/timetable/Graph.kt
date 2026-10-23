package com.usmanzade.timetable

import android.content.Context
import com.usmanzade.timetable.data.Repository
import com.usmanzade.timetable.data.Settings

object Graph {
    lateinit var settings: Settings
        private set
    lateinit var repo: Repository
        private set

    private var ready = false

    fun init(context: Context) {
        if (ready) return
        val app = context.applicationContext
        settings = Settings(app)
        repo = Repository(app)
        ready = true
    }
}
