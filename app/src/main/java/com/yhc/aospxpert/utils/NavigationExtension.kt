package com.yhc.aospxpert.utils

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import com.yhc.aospxpert.R

fun NavController.navigateTo(@IdRes id: Int): Boolean {
    return navigateTo(id, null)
}

@SuppressLint("RestrictedApi")
fun NavController.navigateTo(@IdRes id: Int, bundle: Bundle? = null): Boolean {
    val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)

    if (currentDestination!!.parent!!.findNode(id) is ActivityNavigator.Destination) {
        builder
            .setEnterAnim(R.anim.nav_default_enter_anim)
            .setExitAnim(R.anim.nav_default_exit_anim)
            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
    } else {
        builder
            .setEnterAnim(R.animator.nav_default_enter_anim)
            .setExitAnim(R.animator.nav_default_exit_anim)
            .setPopEnterAnim(R.animator.nav_default_pop_enter_anim)
            .setPopExitAnim(R.animator.nav_default_pop_exit_anim)
    }

    val options = builder.build()

    return try {
        navigate(id, bundle, options)
        true
    } catch (e: IllegalArgumentException) {
        val name = NavDestination.getDisplayName(context, id)
        Log.i(
            "NavigationUI",
            "Ignoring onNavDestinationSelected for MenuItem $name as it cannot be found " +
                    "from the current destination $currentDestination",
            e
        )
        false
    }
}