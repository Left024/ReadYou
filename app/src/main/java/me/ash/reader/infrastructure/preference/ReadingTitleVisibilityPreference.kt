package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.readingTitleVisibility
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalReadingTitleVisibility =
    compositionLocalOf<ReadingTitleVisibilityPreference> {
        ReadingTitleVisibilityPreference.default
    }

/**
 * 阅读页文章大标题的全局默认显示设置。
 * 单个订阅源/分组可通过 titleDisplayMode 覆盖该默认值。
 */
sealed class ReadingTitleVisibilityPreference(val value: Boolean) : Preference() {
    /** 显示文章标题 */
    object SHOW : ReadingTitleVisibilityPreference(true)
    /** 隐藏文章标题，只显示正文 */
    object HIDE : ReadingTitleVisibilityPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(DataStoreKey.readingTitleVisibility, value)
        }
    }

    companion object {

        val default = SHOW
        val values = listOf(SHOW, HIDE)

        fun fromPreferences(preferences: Preferences) =
            when (
                preferences[
                    DataStoreKey.keys[readingTitleVisibility]?.key as Preferences.Key<Boolean>
                ]
            ) {
                true -> SHOW
                false -> HIDE
                else -> default
            }
    }
}

operator fun ReadingTitleVisibilityPreference.not(): ReadingTitleVisibilityPreference =
    when (value) {
        true -> ReadingTitleVisibilityPreference.HIDE
        false -> ReadingTitleVisibilityPreference.SHOW
    }
