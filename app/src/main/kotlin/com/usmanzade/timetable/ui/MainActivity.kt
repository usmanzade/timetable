package com.usmanzade.timetable.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.usmanzade.timetable.Graph

class MainActivity : ComponentActivity() {
    private val requestedTab = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        intent?.getIntExtra(EXTRA_TAB, -1)?.takeIf { it >= 0 }?.let { requestedTab.value = it }

        setContent {
            AppRoot(
                requestedTab = requestedTab.value,
                onTabHandled = { requestedTab.value = null },
                onDarkChanged = { dark ->
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        ) { dark },
                        navigationBarStyle = SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        ) { dark },
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getIntExtra(EXTRA_TAB, -1).takeIf { it >= 0 }?.let { requestedTab.value = it }
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}
