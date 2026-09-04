package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.readingOpenLinkFab
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalReadingOpenLinkFab =
    compositionLocalOf<ReadingOpenLinkFabPreference> {
        ReadingOpenLinkFabPreference.default
    }

/**
 * 阅读页右下角"打开原文链接"悬浮按钮开关。
 * 开启时点击该按钮等同于点击文章标题区，按打开链接偏好调起浏览器。
 */
sealed class ReadingOpenLinkFabPreference(val value: Boolean) : Preference() {
    /** 显示悬浮按钮 */
    object ON : ReadingOpenLinkFabPreference(true)
    /** 隐藏悬浮按钮 */
    object OFF : ReadingOpenLinkFabPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(DataStoreKey.readingOpenLinkFab, value)
        }
    }

    companion object {

        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (
                preferences[DataStoreKey.keys[readingOpenLinkFab]?.key as Preferences.Key<Boolean>]
            ) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun ReadingOpenLinkFabPreference.not(): ReadingOpenLinkFabPreference =
    when (value) {
        true -> ReadingOpenLinkFabPreference.OFF
        false -> ReadingOpenLinkFabPreference.ON
    }
