package me.ash.reader.domain.model.feed

import androidx.room.*
import me.ash.reader.domain.model.group.Group

/**
 * TODO: Add class description
 */
@Entity(
    tableName = "feed",
    foreignKeys = [ForeignKey(
        entity = Group::class,
        parentColumns = ["id"],
        childColumns = ["groupId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE,
    )],
)
data class Feed(
    @PrimaryKey
    val id: String,
    @ColumnInfo
    val name: String,
    @ColumnInfo
    val icon: String? = null,
    @ColumnInfo
    val url: String,
    @ColumnInfo(index = true)
    var groupId: String,
    @ColumnInfo(index = true)
    val accountId: Int,
    @ColumnInfo
    val isNotification: Boolean = false,
    @ColumnInfo
    val isFullContent: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isBrowser: Boolean = false,
    /**
     * 阅读页是否显示文章标题（大标题区）。
     * 0 = 跟随全局默认设置，1 = 始终显示，2 = 始终隐藏。
     */
    @ColumnInfo(defaultValue = "0")
    val titleDisplayMode: Int = 0,
    @Ignore val important: Int = 0
) {
    constructor(
        id: String,
        name: String,
        icon: String?,
        url: String,
        groupId: String,
        accountId: Int,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
        titleDisplayMode: Int = 0
    ) : this(
        id = id,
        name = name,
        icon = icon,
        url = url,
        groupId = groupId,
        accountId = accountId,
        isNotification = isNotification,
        isFullContent = isFullContent,
        isBrowser = isBrowser,
        titleDisplayMode = titleDisplayMode,
        important = 0
    )

    companion object {
        /** 阅读页标题显示：跟随全局默认设置 */
        const val TITLE_DISPLAY_FOLLOW_DEFAULT = 0
        /** 阅读页标题显示：始终显示 */
        const val TITLE_DISPLAY_SHOW = 1
        /** 阅读页标题显示：始终隐藏 */
        const val TITLE_DISPLAY_HIDE = 2
    }
}
