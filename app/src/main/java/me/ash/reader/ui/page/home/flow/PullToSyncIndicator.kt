package me.ash.reader.ui.page.home.flow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.ash.reader.ui.page.home.reading.PullToLoadDefaults
import me.ash.reader.ui.page.home.reading.PullToLoadState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.PullToSyncIndicator(
    pullToLoadState: PullToLoadState,
    modifier: Modifier = Modifier,
    isSyncing: Boolean,
    progress: Int? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val animateOffsetFraction = remember { Animatable(if (isSyncing) 1f else 0f) }
    val animateAlpha = remember { Animatable(if (isSyncing) 1f else 0f) }
    val animateScale = remember { Animatable(if (isSyncing) 1f else .2f) }

    val progressFlow = remember(pullToLoadState) { snapshotFlow { pullToLoadState.progress } }

    val offsetFractionFlow =
        remember(pullToLoadState) { snapshotFlow { pullToLoadState.offsetFraction } }

    var showIndeterminateIndicator by remember { mutableStateOf(isSyncing) }

    // 显示用百分比：向真实进度平滑爬升；真实进度长时间停滞时缓慢兜底爬升
    // （约 0.7s +1%，封顶 96），避免同步中段停住不动、结束时瞬间冲到 100
    var displayedPercent by remember { mutableStateOf(progress ?: 0) }
    val currentProgress by rememberUpdatedState(progress)
    LaunchedEffect(isSyncing) {
        if (!isSyncing) return@LaunchedEffect
        // 新一轮同步开始，把上一轮残留的高值压回当前真实进度
        val target = currentProgress
        if (target != null && target in 0..100) {
            if (displayedPercent > target) displayedPercent = target
        } else {
            displayedPercent = 0
        }
        while (true) {
            val real = currentProgress
            if (real != null && real in 0..100 && displayedPercent < real) {
                val step = ((real - displayedPercent) / 4).coerceAtLeast(1)
                displayedPercent = (displayedPercent + step).coerceAtMost(real)
                delay(70)
            } else if (displayedPercent < 96) {
                displayedPercent += 1
                delay(700)
            } else {
                delay(300)
            }
        }
    }

    val isSyncingFlow = snapshotFlow { isSyncing }

    val offsetSpec = remember {
        spring<Float>(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    }

    val scaleSpec = remember {
        spring<Float>(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    }

    val alphaSpec = remember {
        spring<Float>(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        )
    }

    LaunchedEffect(isSyncingFlow) {
        isSyncingFlow.collect { isSyncing ->
            if (isSyncing) {
                showIndeterminateIndicator = true
                animateAlpha.snapTo(1f)
                animateScale.snapTo(1f)
                launch {
                    animateOffsetFraction.animateTo(0f, offsetSpec)
                }
            } else {
                animateAlpha.animateTo(0f, alphaSpec)
                animateScale.snapTo(0f)
                showIndeterminateIndicator = false
            }
        }
    }

    LaunchedEffect(progressFlow) {
        progressFlow.collect { progress ->
            if (!showIndeterminateIndicator) {
                animateScale.snapTo(progress.fastCoerceAtMost(1f))
                animateAlpha.snapTo(progress.fastCoerceAtMost(1f))
            }
        }
    }

    LaunchedEffect(offsetFractionFlow) {
        offsetFractionFlow.collect {
            if (!showIndeterminateIndicator) {
                animateOffsetFraction.snapTo(it.fastCoerceAtMost(3f))
            }
        }
    }

    LaunchedEffect(progressFlow) {
        progressFlow.map { it > 1f }.distinctUntilChanged().collect {
            if (it && !showIndeterminateIndicator) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }
        }
    }


    val fraction by remember { derivedStateOf { animateOffsetFraction.value } }


    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 72.dp)
            .align(Alignment.TopCenter)
            .offset {
                IntOffset(
                    x = 0,
                    y = (fraction * PullToLoadDefaults.ContentOffsetMultiple).dp.roundToPx()
                )
            }
            .graphicsLayer {
                this.alpha = animateAlpha.value
                this.scaleX = animateScale.value
                this.scaleY = animateScale.value
            },
        color = MaterialTheme.colorScheme.primaryFixedDim,
        shape = MaterialTheme.shapes.extraLarge) {
        if (showIndeterminateIndicator) {
            val scale = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                scale.animateTo(1.2f, animationSpec = scaleSpec)
            }
            val percent = progress
            if (percent != null && percent in 0..100) {
                // 同步中显示转圈 + 百分比
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoadingIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale.value)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$displayedPercent%",
                        color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .scale(scale.value)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(
                    progress = { fraction },
                    color = MaterialTheme.colorScheme.onPrimaryFixedVariant,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}