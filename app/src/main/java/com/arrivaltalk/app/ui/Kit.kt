package com.arrivaltalk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 차분한 바탕 + 강조색 하나(살구빛). 결과는 색만으로 구분하지 않고 항상 글자·기호를 함께 쓴다.
 * 주간/야간 모두 본문 대비 4.5:1 이상이 되도록 고른 값이다.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val line: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val good: Color,
    val goodSoft: Color,
    val retry: Color,
    val retrySoft: Color,
)

val NightPalette = Palette(
    dark = true,
    bg = Color(0xFF0E1316),
    surface = Color(0xFF161D22),
    surfaceHigh = Color(0xFF212B31),
    line = Color(0xFF33404A),
    text = Color(0xFFF2EFEA),
    textDim = Color(0xFFA9B3B9),
    accent = Color(0xFFFFB07A),
    onAccent = Color(0xFF24130A),
    accentSoft = Color(0xFF3A2A20),
    good = Color(0xFF7FE0C4),
    goodSoft = Color(0xFF16332C),
    retry = Color(0xFF9CCBFF),
    retrySoft = Color(0xFF1B2E44),
)

val DayPalette = Palette(
    dark = false,
    bg = Color(0xFFF5F2EC),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEBE6DC),
    line = Color(0xFFD3CBBD),
    text = Color(0xFF1D2327),
    textDim = Color(0xFF566067),
    accent = Color(0xFFA84614),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF6E3D6),
    good = Color(0xFF0E6B55),
    goodSoft = Color(0xFFDDF1EA),
    retry = Color(0xFF1F5E9E),
    retrySoft = Color(0xFFDDE9F6),
)

val LocalPalette = staticCompositionLocalOf { NightPalette }

@Composable
fun ArrivalTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) NightPalette else DayPalette
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent, onPrimary = palette.onAccent,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
        )
    } else {
        lightColorScheme(
            primary = palette.accent, onPrimary = palette.onAccent,
            background = palette.bg, onBackground = palette.text,
            surface = palette.surface, onSurface = palette.text,
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
    serif: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = if (serif) FontFamily.Serif else FontFamily.Default,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = (size * 1.32f).sp,
    )
}

enum class ButtonKind { PRIMARY, SECONDARY, OUTLINE }

/** 큰 터치 영역 + 라벨. 눌림은 리플로 즉시 보인다. */
@Composable
fun BigButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    sub: String? = null,
    minHeight: Int = 68,
    textSize: Int = 20,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    val bg = when {
        !enabled -> p.surfaceHigh
        kind == ButtonKind.PRIMARY -> p.accent
        kind == ButtonKind.SECONDARY -> p.surfaceHigh
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> p.textDim
        kind == ButtonKind.PRIMARY -> p.onAccent
        else -> p.text
    }
    Box(
        modifier = modifier
            .heightIn(min = minHeight.dp)
            .clip(shape)
            .background(bg)
            .then(if (kind == ButtonKind.OUTLINE) Modifier.border(1.5.dp, p.line, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(label, textSize, color = fg, weight = FontWeight.SemiBold, align = TextAlign.Center)
            if (sub != null) T(sub, 14, color = fg.copy(alpha = 0.78f), align = TextAlign.Center)
        }
    }
}

/** 카드 한 장. 테두리로 구분해 주야간 모두 경계가 보이게 한다. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.dp, p.line, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** 작은 꼬리표: 주제, 상태 등. */
@Composable
fun Tag(text: String, fg: Color = LocalPalette.current.textDim, bg: Color = LocalPalette.current.surfaceHigh) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        T(text, 14, color = fg, weight = FontWeight.Medium, maxLines = 1)
    }
}

/** 둘 이상 중 하나를 고르는 큰 선택 칸. 선택 여부는 색 + 굵기 + ● 표시로 함께 알린다. */
@Composable
fun <V> Segmented(
    options: List<Pair<V, String>>,
    selected: V,
    onSelect: (V) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    val outer = RoundedCornerShape(20.dp)
    Row(
        modifier
            .clip(outer)
            .background(p.surfaceHigh)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) p.surface else Color.Transparent)
                    .then(if (on) Modifier.border(1.5.dp, p.accent, RoundedCornerShape(16.dp)) else Modifier)
                    .clickable(enabled = enabled, role = Role.Tab, onClick = { onSelect(value) })
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                T(
                    if (on) "● $label" else label,
                    17,
                    color = if (on) p.text else p.textDim,
                    weight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    align = TextAlign.Center,
                )
            }
        }
    }
}

/** 켜기/끄기 한 줄. 상태를 "켜짐/꺼짐" 글자로도 보여준다. */
@Composable
fun ToggleRow(
    title: String,
    detail: String?,
    on: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(p.surface)
            .border(1.dp, p.line, shape)
            .clickable(role = Role.Switch, onClick = { onChange(!on) })
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T(title, 18, weight = FontWeight.Medium)
            if (detail != null) T(detail, 14, color = p.textDim)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (on) p.accent else p.surfaceHigh)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            T(if (on) "켜짐" else "꺼짐", 15, color = if (on) p.onAccent else p.textDim, weight = FontWeight.SemiBold)
        }
    }
}

/** 진행 단계 점: 끝난 단계는 채우고, 지금 단계는 테두리로 표시한다. */
@Composable
fun StepDots(total: Int, done: Int, current: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val filled = i < done
            Box(
                Modifier
                    .size(width = 34.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(if (filled) p.accent else p.surfaceHigh)
                    .then(if (i == current && !filled) Modifier.border(1.5.dp, p.accent, CircleShape) else Modifier),
            )
        }
    }
}
