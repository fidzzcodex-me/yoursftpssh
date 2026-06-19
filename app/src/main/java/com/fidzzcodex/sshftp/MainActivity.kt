package com.fidzzcodex.sshftp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fidzzcodex.sshftp.ui.navigation.AppNavigation
import com.fidzzcodex.sshftp.ui.theme.SSHFTPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            SSHFTPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
