package com.zeni.ia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zeni.ia.ui.ZeniApp
import com.zeni.ia.ui.ZeniTheme

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Check debe inicializarse antes de crear o usar cualquier SDK de Firebase.
        AppCheckInitializer.initialize(this)

        enableEdgeToEdge()
        setContent {
            ZeniTheme { ZeniApp(vm) }
        }
    }
}
