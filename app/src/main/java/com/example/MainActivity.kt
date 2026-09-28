package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricPreferences
import com.example.ui.MainAppScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AccountingViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: AccountingViewModel by viewModels()
    private lateinit var biometricPreferences: BiometricPreferences
    private val isUnlockedState = mutableStateOf(false)
    private var isAppInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricPreferences = BiometricPreferences(this)

        // If biometric lock is disabled by user, set as unlocked
        if (!biometricPreferences.isBiometricEnabled) {
            isUnlockedState.value = true
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                com.example.ui.theme.AppThemeMode.DARK -> true
                com.example.ui.theme.AppThemeMode.LIGHT -> false
                com.example.ui.theme.AppThemeMode.SYSTEM -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var isSplashActive by remember { mutableStateOf(true) }
                val isUnlocked by remember { isUnlockedState }

                Crossfade(
                    targetState = isSplashActive,
                    animationSpec = tween(durationMillis = 500),
                    label = "splash_crossfade"
                ) { showSplash ->
                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = { isSplashActive = false }
                        )
                    } else if (biometricPreferences.isBiometricEnabled && !isUnlocked) {
                        BiometricLockScreen(
                            onUnlocked = {
                                isUnlockedState.value = true
                            }
                        )
                    } else {
                        MainAppScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isAppInBackground = true
    }

    override fun onResume() {
        super.onResume()
        if (isAppInBackground) {
            isAppInBackground = false
            if (biometricPreferences.isBiometricEnabled && biometricPreferences.isLockOnBackgroundEnabled) {
                isUnlockedState.value = false
            }
        }
    }
}
