package me.ash.reader.ui.page.home.feeds

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.eventFlow
import androidx.work.WorkInfo
import kotlin.collections.set
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.LocalFeedsFilterBarPadding
import me.ash.reader.infrastructure.preference.LocalFeedsFilterBarStyle
import me.ash.reader.infrastructure.preference.LocalFeedsFilterBarTonalElevation
import me.ash.reader.infrastructure.preference.LocalFeedsGroupListExpand
import me.ash.reader.infrastructure.preference.LocalFeedsGroupListTonalElevation
import me.ash.reader.infrastructure.preference.LocalFeedsTopBarTonalElevation
import me.ash.reader.infrastructure.preference.LocalNewVersionNumber
import me.ash.reader.infrastructure.preference.LocalSkipVersionNumber
import me.ash.reader.ui.component.FilterBar
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.scrollbar.drawVerticalScrollIndicator
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.currentAccountId
import me.ash.reader.ui.ext.findActivity
import me.ash.reader.ui.ext.getCurrentVersion
import me.ash.reader.ui.ext.surfaceColorAtElevation
import me.ash.reader.ui.page.common.RouteName
import me.ash.reader.ui.page.home.feeds.accounts.AccountsTab
import me.ash.reader.ui.page.home.feeds.drawer.feed.FeedOptionDrawer
import me.ash.reader.ui.page.home.feeds.drawer.group.GroupOptionDrawer
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeDialog
import me.ash.reader.ui.page.home.feeds.subscribe.SubscribeViewModel
import me.ash.reader.ui.page.home.flow.PullToSyncIndicator
import me.ash.reader.ui.page.home.reading.PullToLoadDefaults
import me.ash.reader.ui.page.home.reading.PullToLoadDefaults.ContentOffsetMultiple
import me.ash.reader.ui.page.home.reading.PullToLoadState
import me.ash.reader.ui.page.home.reading.pullToLoad
import me.ash.reader.ui.page.home.reading.rememberPullToLoadState
import me.ash.reader.ui.page.settings.accounts.AccountViewModel

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterialApi::class,
)
@Composable
fun FeedsPage(
    //    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    accountViewModel: AccountViewModel = hiltViewModel(),
    feedsViewModel: FeedsViewModel = hiltViewModel(),
    subscribeViewModel: SubscribeViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit,
    navigationToFlow: () -> Unit,
    navigateToAccountList: () -> Unit,
    navigateToAccountDetail: (Int) -> Unit,
) {
    var accountTabVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val topBarTonalElevation = LocalFeedsTopBarTonalElevation.current
    val groupListTonalElevation = LocalFeedsGroupListTonalElevation.current
    val groupListExpand = LocalFeedsGroupListExpand.current
    val filterBarStyle = LocalFeedsFilterBarStyle.current
    val filterBarPadding = LocalFeedsFilterBarPadding.current
    val filterBarTonalElevation = LocalFeedsFilterBarTonalElevation.current

    val accounts = accountViewModel.accounts.collectAsStateValue(initial = emptyList())

    val feedsUiState = feedsViewModel.feedsUiState.collectAsStateValue()
    val filterState = feedsViewModel.filterStateFlow.collectAsStateValue()
    val importantSum = feedsUiState.importantSum
    val groupWithFeedList = feedsViewModel.groupWithFeedsListFlow.collectAsStateValue()
    val groupsVisible: SnapshotStateMap<String, Boolean> = feedsUiState.groupsVisible
    val hasGroupVisible by
        remember(groupWithFeedList) {
            derivedStateOf { groupWithFeedList.fastAny { groupsVisible[it.group.id] == true } }
        }

    val newVersion = LocalNewVersionNumber.current
    val skipVersion = LocalSkipVersionNumber.current
    val currentVersion = remember { context.getCurrentVersion() }
    val listState =
        if (groupWithFeedList.isNotEmpty()) feedsUiState.listState else rememberLazyListState()

    val owner = LocalLifecycleOwner.current

    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf<Int?>(null) }
    val syncingScope = rememberCoroutineScope()
    val settleSpec = remember { spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy) }
    val doSync: () -> Unit = {
        isSyncing = true
        syncProgress = null
        syncingScope.launch { feedsViewModel.sync() }
    }

    DisposableEffect(owner) {
        scope.launch {
            owner.lifecycle.eventFlow.collect {
                when (it) {
                    Lifecycle.Event.ON_RESUME,
                    Lifecycle.Event.ON_PAUSE -> {
                        feedsViewModel.commitDiffs()
                    }

                    else -> {
                        /* no-op */
                    }
                }
            }
        }
        feedsViewModel.syncWorkLiveData.observe(owner) { workInfoList ->
            val running =
                workInfoList.firstOrNull { workInfo -> workInfo.state == WorkInfo.State.RUNNING }
            isSyncing = running != null
            syncProgress =
                running
                    ?.progress
                    ?.getInt("syncProgress", -1)
                    ?.takeIf { progress -> progress in 0..100 }
        }
        onDispose { feedsViewModel.syncWorkLiveData.removeObservers(owner) }
    }

    // 下拉刷新：与文章信息流页同一套 PullToLoad 机制与显示（同步中圆圈+百分比）
    var currentPullToLoadState: PullToLoadState? by remember { mutableStateOf(null) }
    val onPullToSync: (() -> Unit)? =
        if (isSyncing) null
        else {
            {
                doSync()
                currentPullToLoadState?.animateDistanceTo(
                    targetValue = 0f,
                    animationSpec = settleSpec,
                )
            }
        }

    val pullToLoadState =
        rememberPullToLoadState(
                key = listState,
                onLoadNext = null,
                onLoadPrevious = onPullToSync,
                loadThreshold = PullToLoadDefaults.loadThreshold(.1f),
            )
            .also { currentPullToLoadState = it }

    fun expandAllGroups() {
        groupWithFeedList.forEach { groupWithFeed -> groupsVisible[groupWithFeed.group.id] = true }
    }

    fun collapseAllGroups() {
        groupWithFeedList.forEach { groupWithFeed -> groupsVisible[groupWithFeed.group.id] = false }
    }

    val groupDrawerState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val feedDrawerState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)

    BackHandler(true) { context.findActivity()?.moveTaskToBack(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RYScaffold(
        topBarTonalElevation = topBarTonalElevation.value.dp,
        //        containerTonalElevation = groupListTonalElevation.value.dp,
        topBar = {
            TopAppBar(
                modifier =
                    Modifier.clickable(
                        onClick = {
                            scope.launch {
                                if (listState.firstVisibleItemIndex != 0) {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                title = {},
                navigationIcon = {
                    FeedbackIconButton(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                        showBadge = newVersion.whetherNeedUpdate(currentVersion, skipVersion),
                    ) {
                        navigateToSettings()
                    }
                },
                actions = {
                    if (subscribeViewModel.rssService.get().addSubscription) {
                        FeedbackIconButton(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.subscribe),
                            tint = MaterialTheme.colorScheme.onSurface,
                        ) {
                            subscribeViewModel.showDrawer()
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceColorAtElevation(
                                topBarTonalElevation.value.dp
                            )
                    ),
            )
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier =
                        Modifier.pullToLoad(
                                state = pullToLoadState,
                                enabled = true,
                                contentOffsetY = { fraction ->
                                    if (fraction > 0f) {
                                        (fraction * ContentOffsetMultiple * 1.5f)
                                            .dp
                                            .roundToPx()
                                    } else {
                                        (fraction * ContentOffsetMultiple * 2f)
                                            .dp
                                            .roundToPx()
                                    }
                                },
                            )
                            .fillMaxSize()
                            .drawVerticalScrollIndicator(listState),
                    state = listState,
                ) {
                    item {
                        DisplayText(text = feedsUiState.account?.name ?: "", desc = "") {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            accountTabVisible = true
                        }
                    }
                    item {
                        FeedsBanner(
                            filter = filterState.filter,
                            desc = importantSum.ifEmpty { stringResource(R.string.loading) },
                        ) {
                            feedsViewModel.changeFilter(filterState.copy(group = null, feed = null))
                            navigationToFlow()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 26.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.feeds),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            IconButton(
                                onClick = {
                                    if (hasGroupVisible) collapseAllGroups() else expandAllGroups()
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(28.dp),
                            ) {
                                Icon(
                                    imageVector =
                                        if (hasGroupVisible) Icons.Rounded.UnfoldLess
                                        else Icons.Rounded.UnfoldMore,
                                    contentDescription = stringResource(R.string.unfold_less),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    itemsIndexed(groupWithFeedList) { _, (group, feeds) ->
                        GroupWithFeedsContainer {
                            GroupItem(
                                isExpanded = {
                                    groupsVisible.getOrPut(group.id, groupListExpand::value)
                                },
                                group = group,
                                onExpanded = {
                                    groupsVisible[group.id] =
                                        groupsVisible
                                            .getOrPut(group.id, groupListExpand::value)
                                            .not()
                                },
                                onLongClick = { scope.launch { groupDrawerState.show() } },
                            ) {
                                feedsViewModel.changeFilter(
                                    filterState.copy(group = group, feed = null)
                                )
                                navigationToFlow()
                            }

                            feeds.forEachIndexed { index, feed ->
                                FeedItem(
                                    feed = feed,
                                    isLastItem = { index == feeds.lastIndex },
                                    isExpanded = {
                                        groupsVisible.getOrPut(feed.groupId, groupListExpand::value)
                                    },
                                    onClick = {
                                        feedsViewModel.changeFilter(
                                            filterState.copy(feed = feed, group = null)
                                        )
                                        navigationToFlow()
                                    },
                                    onLongClick = { scope.launch { feedDrawerState.show() } },
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(128.dp))
                        Spacer(
                            modifier =
                                Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                        )
                    }
                }
            }
        },
        bottomBar = {
            FilterBar(
                modifier =
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState("filterBar"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                filter = filterState.filter,
                filterBarStyle = filterBarStyle.value,
                filterBarFilled = true,
                filterBarPadding = filterBarPadding.dp,
                filterBarTonalElevation = filterBarTonalElevation.value.dp,
            ) {
                feedsViewModel.changeFilter(filterState.copy(filter = it))
            }
        },
    )

    // 下拉同步指示：与文章信息流页同款（下拉进度 / 同步中转圈+百分比），
    // 放在 Scaffold 外层 Box，按屏幕顶部定位，与信息流页位置一致
    currentPullToLoadState?.let {
        PullToSyncIndicator(
            pullToLoadState = it,
            isSyncing = isSyncing,
            progress = if (isSyncing) syncProgress else null,
        )
    }
}

    SubscribeDialog(subscribeViewModel = subscribeViewModel)

    GroupOptionDrawer(drawerState = groupDrawerState)
    FeedOptionDrawer(drawerState = feedDrawerState)

    val currentAccountId = feedsUiState.account?.id

    AccountsTab(
        visible = accountTabVisible,
        accounts = accounts,
        currentAccountId = currentAccountId,
        onAccountSwitch = { accountViewModel.switchAccount(it) { accountTabVisible = false } },
        onClickSettings = {
            accountTabVisible = false
            navigateToAccountDetail(currentAccountId!!)
        },
        onClickManage = {
            accountTabVisible = false
            navigateToAccountList()
        },
        onDismissRequest = { accountTabVisible = false },
    )
}
