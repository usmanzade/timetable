package com.usmanzade.timetable

import android.app.Application
import com.usmanzade.timetable.work.Notifier
import com.usmanzade.timetable.work.Scheduler

class TimetableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifier.createChannel(this)
        Scheduler.apply(this)
    }
}
