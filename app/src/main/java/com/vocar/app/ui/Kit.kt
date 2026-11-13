package com.vocar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * VOCAR의 모양: 밝은 바탕 위의 옅은 회색 타일, 그 안의 흰 알약 모양 조작부,
 * 강조는 검정 카드와 초록 그러데이션 하나. 상태는 색만이 아니라 기호·글자·위치로도 함께 알린다.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    /** 화면 바탕 */
    val bg: Color,
    /** 묶음을 담는 옅은 타일 */
    val tile: Color,
    /** 타일 안의 조작부·카드 */
    val card: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    /** 강조 카드(검정)와 그 위의 글자 */
    val hero: Color,
    val onHero: Color,
    val onHeroDim: Color,
    /** 초록 그러데이션의 양 끝 */
    val accentA: Color,
    val accentB: Color,
    val onAccent: Color,
    val accentSoft: Color,
    /** 바탕 위에서 읽을 수 있는 초록 글자 */
    val good: Color,
    val retry: Color,
    val retrySoft: Color,
) {
    val accentBrush: Brush get() = Brush.horizontalGradient(listOf(accentA, accentB))
}

val DayPalette = Palette(
    dark = false,
    bg = Color(0xFFFAFAF8),
    tile = Color(0xFFF0F0ED),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFE2E2DE),
    text = Color(0xFF141414),
    textDim = Color(0xFF62625E),
    hero = Color(0xFF141414),
    onHero = Color(0xFFFFFFFF),
    onHeroDim = Color(0xFFA6A6A2),
    accentA = Color(0xFF3BDB62),
    accentB = Color(0xFFB4F03C),
    onAccent = Color(0xFF0A2A12),
    accentSoft = Color(0xFFDDF6DF),
    good = Color(0xFF117A37),
    retry = Color(0xFFA5500A),
    retrySoft = Color(0xFFFFEBD2),
)

val NightPalette = Palette(
    dark = true,
    bg = Color(0xFF0B0B0B),
    tile = Color(0xFF171717),
    card = Color(0xFF242424),
    line = Color(0xFF333333),
    text = Color(0xFFF4F4F2),
    textDim = Color(0xFFA8A8A4),
    hero = Color(0xFF000000),
    onHero = Color(0xFFFFFFFF),
    onHeroDim = Color(0xFFA6A6A2),
    accentA = Color(0xFF3BDB62),
    accentB = Color(0xFFB4F03C),
    onAccent = Color(0xFF0A2A12),
    accentSoft = Color(0xFF143220),
    good = Color(0xFF6BE58A),
    retry = Color(0xFFFFB866),
    retrySoft = Color(0xFF3A2710),
)

val LocalPalette = staticCompositionLocalOf { DayPalette }

@Composable
fun VocarTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) NightPalette else DayPalette
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accentA, onPrimary = palette.onAccent,
            background = palette.bg, onBackground = palette.text,
            surface = palette.card, onSurface = palette.text,
        )
    } else {
        lightColorScheme(
            primary = palette.accentA, onPrimary = palette.onAccent,
            background = palette.bg, onBackground = palette.text,
            surface = palette.card, onSurface = palette.text,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** 앱의 모든 글자. 크기는 기준 화면(가로 790 단위) 값이며 창 크기에 맞춰 함께 커진다. */
@Composable
fun T(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.text,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = (size * 1.3f).sp,
    )
}

/** 누르는 동안 살짝 작아지는 반응. 누르면 즉시 줄고, 떼면 튀지 않고 돌아온다. */
@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        if (pressed) snap() else tween(160, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
        label = "press",
    )
    return scale
}

enum class ButtonKind {
    /** 초록 그러데이션: 그 화면의 주 행동 */
    PRIMARY,

    /** 검정 알약: 다음 단계로 넘어가는 행동 */
    DARK,

    /** 흰 알약: 보조 행동 */
    SECONDARY,

    /** 테두리만: 가장 조용한 행동 */
    OUTLINE,
}

/** 알약 모양의 큰 버튼 + 라벨. */
@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    sub: String? = null,
    minHeight: Int = 66,
    textSize: Int = 19,
    sidePadding: Int = 22,
) {
    val p = LocalPalette.current
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(50)
    val fg = when {
        !enabled -> p.textDim
        kind == ButtonKind.PRIMARY -> p.onAccent
        kind == ButtonKind.DARK -> p.onHero
        else -> p.text
    }
    val fill = when {
        !enabled -> Modifier.background(p.tile).border(1.dp, p.line, shape)
        kind == ButtonKind.PRIMARY -> Modifier.background(p.accentBrush)
        kind == ButtonKind.DARK -> Modifier.background(if (p.dark) p.card else p.hero)
        kind == ButtonKind.SECONDARY -> Modifier.background(p.card).border(1.dp, p.line, shape)
        else -> Modifier.border(1.5.dp, p.line, shape)
    }
    Box(
        modifier = modifier
            .scale(pressScale(source))
            .heightIn(min = minHeight.dp)
            .clip(shape)
            .then(fill)
            .clickable(
                interactionSource = source,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = sidePadding.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(label, textSize, color = fg, weight = FontWeight.SemiBold, align = TextAlign.Center)
            if (sub != null) T(sub, 13, color = fg.copy(alpha = 0.72f), align = TextAlign.Center)
        }
    }
}

/** 묶음을 담는 옅은 타일. 안에는 흰 카드·알약 조작부가 들어간다. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.tile,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** 검정 강조 카드: 지금 가장 중요한 한 가지(오늘의 표현, 주간 진행). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(p.hero)
            .then(if (p.dark) Modifier.border(1.dp, p.line, shape) else Modifier)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** 작은 꼬리표. on이면 초록으로 채우고 ✓를 붙인다. */
@Composable
fun Tag(text: String, on: Boolean = false, fg: Color? = null, bg: Color? = null) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .then(
                when {
                    on -> Modifier.background(p.accentBrush)
                    bg != null -> Modifier.background(bg)
                    else -> Modifier.background(p.card).border(1.dp, p.line, shape)
                },
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        T(
            if (on) "✓ $text" else text,
            13,
            color = if (on) p.onAccent else fg ?: p.textDim,
            weight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** 초록 체크 동그라미. 완료·선택 표시. */
@Composable
fun CheckDot(on: Boolean, size: Int = 24) {
    val p = LocalPalette.current
    val grow by animateFloatAsState(
        if (on) 1f else 0.75f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check",
    )
    Box(
        Modifier
            .size(size.dp)
            .scale(grow)
            .clip(CircleShape)
            .then(if (on) Modifier.background(p.accentBrush) else Modifier.background(p.line)),
        contentAlignment = Alignment.Center,
    ) {
        if (on) T("✓", (size * 0.55f).toInt(), color = p.onAccent, weight = FontWeight.Bold)
    }
}

/** 회색 홈 안에서 흰 알약이 선택된 쪽에 놓이는 선택 칸. */
@Composable
fun <V> Segmented(
    options: List<Pair<V, String>>,
    selected: V,
    onSelect: (V) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = LocalPalette.current.tile,
) {
    val p = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(track)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            val fill by animateColorAsState(if (on) p.card else track, tween(180), label = "segment")
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fill)
                    .clickable(role = Role.Tab, onClick = { onSelect(value) })
                    .semantics { stateDescription = if (on) "선택됨" else "선택 안 됨" }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(p.accentBrush))
                    Spacer(Modifier.width(8.dp))
                }
                T(
                    label,
                    16,
                    color = if (on) p.text else p.textDim,
                    weight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    align = TextAlign.Center,
                )
            }
        }
    }
}

/** 켜기/끄기 한 줄. 손잡이의 위치와 색이 함께 바뀐다. */
@Composable
fun ToggleRow(
    title: String,
    detail: String?,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val knob by animateDpAsState(if (on) 24.dp else 0.dp, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium), label = "knob")
    val track by animateColorAsState(if (on) p.accentA else p.line, tween(180), label = "track")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.line, shape)
            .clickable(role = Role.Switch, onClick = { onChange(!on) })
            .semantics { stateDescription = if (on) "켜짐" else "꺼짐" }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T(title, 17, weight = FontWeight.SemiBold)
            if (detail != null) T(detail, 13, color = p.textDim)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(CircleShape)
                .background(track)
                .padding(4.dp),
        ) {
            Box(
                Modifier
                    .offset { IntOffset(knob.roundToPx(), 0) }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** 진행 단계: 끝난 단계는 초록, 지금 단계는 길게. */
@Composable
fun StepDots(total: Int, done: Int, current: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val filled = i < done
            val width by animateDpAsState(if (i == current) 44.dp else 22.dp, tween(220), label = "step")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) Modifier.background(p.accentBrush)
                        else Modifier.background(if (i == current) p.text else p.line),
                    ),
            )
        }
    }
}

/** 초록 진행 고리. 값이 바뀌면 부드럽게 차오른다. */
@Composable
fun ProgressRing(fraction: Float, label: String, diameter: Int = 92) {
    val p = LocalPalette.current
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(Modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter.dp)) {
            val stroke = size.minDimension * 0.11f
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color(0xFF303030),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke),
            )
            if (shown > 0f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(p.accentA, p.accentB)),
                    startAngle = -90f, sweepAngle = 360f * shown, useCenter = false,
                    topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        T(label, (diameter * 0.25f).toInt(), color = p.onHero, weight = FontWeight.Bold)
    }
}

/** 음성 메모 모양의 막대. 재생 중에는 초록으로 차오른다. */
@Composable
fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val level by animateFloatAsState(if (active) 1f else 0f, tween(if (active) 1500 else 200), label = "wave")
    val heights = remember { listOf(6, 12, 20, 9, 16, 24, 13, 8, 18, 26, 14, 10, 22, 12, 7, 17, 23, 11, 15, 8, 19, 9, 13, 6) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        heights.forEachIndexed { i, h ->
            val lit = i < (heights.size * level).toInt()
            Box(
                Modifier
                    .size(width = 4.dp, height = h.dp)
                    .clip(CircleShape)
                    .background(if (lit) p.accentA else Color(0xFF4A4A4A)),
            )
        }
    }
}

/** 검정 카드 위의 동그란 흰 버튼(글자 기호 + 읽어 주는 이름). */
@Composable
fun RoundButton(symbol: String, label: String, onClick: () -> Unit, diameter: Int = 54) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .scale(pressScale(source))
            .size(diameter.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = source,
                indication = ripple(),
                onClickLabel = label,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        T(symbol, (diameter * 0.36f).toInt(), color = Color(0xFF141414), weight = FontWeight.Bold)
    }
}
