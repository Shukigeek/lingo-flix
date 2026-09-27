package com.example.lingoFlix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] so Hilt can generate the application-level
 * dependency container. Every `@AndroidEntryPoint` component in the app is
 * attached to this container.
 */
@HiltAndroidApp
class LingoFlixApp : Application()
