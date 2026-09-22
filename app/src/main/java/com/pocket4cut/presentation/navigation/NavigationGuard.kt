package com.pocket4cut.presentation.navigation

import androidx.navigation.NavController

/** Ignore a second tap from a destination that has already left the back stack. */
internal fun NavController.popBackStackIfCurrent(route: String): Boolean =
    currentDestination?.route == route && popBackStack()

/** Selection may have been resumed from Gallery; its leave action promises Home. */
internal fun NavController.popSelectionToHome(): Boolean =
    currentDestination?.route?.startsWith("${Routes.SELECTION}/") == true &&
        popBackStack(Routes.HOME, inclusive = false)
