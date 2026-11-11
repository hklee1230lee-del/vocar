package com.arrivaltalk.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arrivaltalk.app.audio.VoiceState
import com.arrivaltalk.app.domain.Bundle
import com.arrivaltalk.app.domain.Content
import com.arrivaltalk.app.domain.Engine
import com.arrivaltalk.app.domain.Goal
import com.arrivaltalk.app.domain.Level
import com.arrivaltalk.app.domain.Mastery
import com.arrivaltalk.app.domain.Mode
import com.arrivaltalk.app.domain.SessionLength
import com.arrivaltalk.app.domain.Stats
import com.arrivaltalk.app.vehicle.GateState

internal fun modeLabel(mode: Mode) = if (mode == Mode.MORNING) "출근 · 준비" else "퇴근 · 회상"

internal fun gateMessage(state: GateState) = when (state) {
    GateState.PARKED -> "P · 주차 확인"
    GateState.CHECKING -> "차량 상태를 확인하고 있어요"
    GateState.RESTRICTED -> "주차 후 이어할 수 있어요"
    GateState.UNKNOWN -> "차량 상태를 확인한 뒤 시작할 수 있어요"
}

/**
 * 창 크기에 맞춰 화면 전체를 같은 비율로 키운다(가로 790 × 세로 560 단위 기준).
 * 넓은 창은 좌우 2단, 좁은 창은 한 줄 흐름으로 바뀌며 문제 수는 늘리지 않는다.
 */
@Composable
fun AppRoot(c: AppController) {
    ArrivalTheme {
        val p = LocalPalette.current
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(p.bg)
                .safeDrawingPadding(),
        ) {
            val landscape = maxWidth > maxHeight * 1.15f
            val fit = if (landscape) {
                minOf(maxWidth / 790.dp, maxHeight / 560.dp)
            } else {
                minOf(maxWidth / 440.dp, maxHeight / 720.dp)
            }
            val scale = fit.coerceIn(0.8f, 2.4f)
            val wide = maxWidth / scale >= 700.dp
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density * scale, base.fontScale)) {
                BackHandler(enabled = c.screen == Screen.LESSON || c.screen == Screen.DONE || (c.screen == Screen.HOME && c.tab != Tab.TODAY)) {
                    c.back()
                }
                Column(Modifier.fillMaxSize()) {
                    if (c.saveFailed) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(p.retrySoft)
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                        ) {
                            T("방금 기록을 저장하지 못했어요. 저장 공간을 확인해 주세요.", 15, color = p.retry)
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (c.screen) {
                            Screen.ONBOARDING -> OnboardingScreen(c, wide)
                            Screen.HOME -> HomeScaffold(c, wide)
                            Screen.LESSON -> LessonScreen(c, wide)
                            Screen.DONE -> DoneScreen(c, wide)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GateChip(state: GateState, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val ok = state == GateState.PARKED
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (ok) p.goodSoft else p.retrySoft)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        T(gateMessage(state), 14, color = if (ok) p.good else p.retry, weight = FontWeight.Medium)
    }
}

// ── 공통 틀: 오늘 / 표현 보관함 / 내 기록 / 설정 ─────────────────────

@Composable
private fun HomeScaffold(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(168.dp)
                    .fillMaxHeight()
                    .background(p.surface)
                    .padding(horizontal = 12.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                T("도착한마디", 21, weight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                T("Arrival Talk", 14, color = p.textDim, serif = true, modifier = Modifier.padding(start = 10.dp))
                Spacer(Modifier.height(14.dp))
                Tab.entries.forEach { NavItem(it, c.tab == it, Modifier.fillMaxWidth()) { c.tab = it } }
                Spacer(Modifier.weight(1f))
                GateChip(c.gate.state)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { TabContent(c, true) }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { TabContent(c, false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(p.surface)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Tab.entries.forEach { NavItem(it, c.tab == it, Modifier.weight(1f), centered = true) { c.tab = it } }
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Boolean, modifier: Modifier, centered: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.accentSoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
    ) {
        if (!centered) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 22.dp)
                    .clip(CircleShape)
                    .background(if (selected) p.accent else Color.Transparent),
            )
            Spacer(Modifier.width(10.dp))
        }
        T(
            tab.label,
            if (centered) 15 else 17,
            color = if (selected) p.text else p.textDim,
            weight = if (selected) FontWeight.Bold else FontWeight.Normal,
            align = if (centered) TextAlign.Center else null,
        )
    }
}

@Composable
private fun TabContent(c: AppController, wide: Boolean) {
    when (c.tab) {
        Tab.TODAY -> TodayScreen(c, wide)
        Tab.LIBRARY -> LibraryScreen(c, wide)
        Tab.RECORD -> RecordScreen(c, wide)
        Tab.SETTINGS -> SettingsScreen(c, wide)
    }
}

/** 넓은 창은 두 단, 좁은 창은 위아래로 이어 붙인다. */
@Composable
internal fun TwoPane(
    wide: Boolean,
    modifier: Modifier = Modifier,
    leftWeight: Float = 1.25f,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    if (wide) {
        Row(modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(
                Modifier
                    .weight(leftWeight)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { left() }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { right() }
        }
    } else {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            left()
            right()
        }
    }
}

// ── UI-02 오늘 ─────────────────────────────────────────────

@Composable
private fun TodayScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val settings = c.data.settings
    val stats = Engine.stats(c.data, c.today)
    val parked = c.gate.parked
    val resumable = Engine.resumable(c.data)
    val resumeFirst = resumable != null && resumable.day == c.today
    val recommendation = Engine.recommend(c.data, c.mode, settings.length, c.today)
    val other = if (settings.length == SessionLength.THREE_MIN) SessionLength.ONE_MIN else SessionLength.THREE_MIN
    // 오늘 목표를 이미 채웠거나 이어할 학습이 있으면 새 학습을 앞세우지 않는다.
    val startKind = if (stats.studiedToday || resumeFirst) ButtonKind.SECONDARY else ButtonKind.PRIMARY

    TwoPane(
        wide,
        Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
        left = {
            if (stats.studiedToday) {
                T("오늘 목표를 채웠어요", 28, weight = FontWeight.Bold)
                T("더 하고 싶을 때만 가볍게 복습해요.", 16, color = p.textDim)
            } else {
                T("내리기 전, 오늘 쓸 한마디", 28, weight = FontWeight.Bold)
            }
            Segmented(
                listOf(Mode.MORNING to "출근 · 회사 도착", Mode.EVENING to "퇴근 · 집 도착"),
                c.mode,
                { c.mode = it },
                Modifier.fillMaxWidth(),
            )
            if (recommendation == null) {
                Panel(Modifier.fillMaxWidth()) {
                    T("준비된 학습이 없어요", 20, weight = FontWeight.SemiBold)
                    T("앱을 다시 설치하면 기본 학습을 받을 수 있어요.", 15, color = p.textDim)
                }
            } else {
                Panel(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(recommendation.topic)
                        if (recommendation.isNew) Tag("새 표현", p.accent, p.accentSoft) else Tag("복습")
                        if (recommendation.reviewCount > 0 && recommendation.isNew) Tag("복습 ${recommendation.reviewCount}개 포함")
                    }
                    T(recommendation.headline, 24, weight = FontWeight.SemiBold)
                    T(
                        if (c.mode == Mode.MORNING) "듣고, 뜻을 확인하고, 한 번 말해 봐요." else "보기 전에 먼저 떠올려 보고, 다시 말해 봐요.",
                        15,
                        color = p.textDim,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigButton(
                        "${settings.length.minutes}분 시작",
                        { c.start(settings.length) },
                        Modifier.weight(1.5f),
                        kind = startKind,
                        enabled = parked,
                        sub = "활동 ${settings.length.activityCount}개",
                        minHeight = 76,
                    )
                    BigButton(
                        if (other == SessionLength.ONE_MIN) "1분만 하기" else "3분 하기",
                        { c.start(other) },
                        Modifier.weight(1f),
                        kind = if (startKind == ButtonKind.PRIMARY) ButtonKind.SECONDARY else ButtonKind.OUTLINE,
                        enabled = parked,
                        sub = "활동 ${other.activityCount}개",
                        minHeight = 76,
                    )
                }
            }
            if (!parked) GateChip(c.gate.state)
        },
        right = {
            if (resumable != null) {
                Panel(Modifier.fillMaxWidth()) {
                    T(if (resumeFirst) "이어할 학습" else "지난번에 멈춘 학습", 15, color = p.textDim)
                    T(
                        "${modeLabel(resumable.mode)} ${resumable.length.minutes}분 · ${resumable.done}/${resumable.items.size} 저장됨",
                        18,
                        weight = FontWeight.Medium,
                    )
                    BigButton(
                        "이어하기",
                        { c.resumeSaved() },
                        Modifier.fillMaxWidth(),
                        kind = if (resumeFirst && !stats.studiedToday) ButtonKind.PRIMARY else ButtonKind.SECONDARY,
                        enabled = parked,
                        minHeight = 60,
                    )
                }
            }
            WeekPanel(stats, Modifier.fillMaxWidth())
            ToggleRow(
                "무음으로 학습",
                when {
                    c.speaker.state == VoiceState.UNAVAILABLE -> "이 기기에서는 음원을 재생할 수 없어 글자로 진행해요"
                    settings.silent -> "소리 없이 글자로 진행해요"
                    else -> "동승자가 있을 때 켜 두세요"
                },
                settings.silent,
                { on -> c.updateSettings { it.copy(silent = on) } },
            )
        },
    )
}

@Composable
internal fun WeekPanel(stats: Stats, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Panel(modifier) {
        T("이번 주 학습일", 15, color = p.textDim)
        Row(verticalAlignment = Alignment.Bottom) {
            T("${stats.studiedDaysThisWeek}", 34, weight = FontWeight.Bold)
            T(" / ${stats.weeklyGoalDays}일", 18, color = p.textDim, modifier = Modifier.padding(bottom = 5.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stats.week.forEach { d ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (d.studied) p.accent else p.surfaceHigh)
                            .then(
                                if (d.isToday) Modifier.border(2.dp, if (d.studied) p.text else p.accent, CircleShape)
                                else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (d.studied) T("✓", 15, color = p.onAccent, weight = FontWeight.Bold)
                    }
                    T(
                        d.label,
                        13,
                        color = if (d.isToday) p.text else p.textDim,
                        weight = if (d.isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        T(
            when {
                stats.studiedDaysThisWeek >= stats.weeklyGoalDays -> "이번 주 목표를 채웠어요."
                stats.studiedToday -> "오늘은 학습일로 기록했어요."
                stats.studiedDaysThisWeek == 0 -> "다시 시작하면 돼요. 한마디면 충분해요."
                else -> "하루 한 번이면 그날 목표는 끝이에요."
            },
            14,
            color = p.textDim,
        )
    }
}

// ── UI-09 표현 보관함 ──────────────────────────────────────

private const val FILTER_ALL = "전체"
private const val FILTER_RETRY = "다시 익힐 표현"
private const val FILTER_STAR = "즐겨찾기"

@Composable
private fun LibraryScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    var filter by remember { mutableStateOf(FILTER_ALL) }
    var opened by remember { mutableStateOf<String?>(null) }
    val topics = remember { Content.bundles.map { it.topic }.distinct() }
    val learned: List<Pair<Bundle, Mastery>> = c.data.mastery.values
        .sortedByDescending { it.introducedAtMs }
        .mapNotNull { m -> Content.find(m.bundleId)?.let { it to m } }
    val shown = learned.filter { (b, m) ->
        when (filter) {
            FILTER_ALL -> true
            FILTER_RETRY -> m.needsWork
            FILTER_STAR -> m.favorite
            else -> b.topic == filter
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        T("표현 보관함", 28, weight = FontWeight.Bold)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf(FILTER_ALL, FILTER_RETRY, FILTER_STAR) + topics).forEach { name ->
                val on = filter == name
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) p.accentSoft else p.surfaceHigh)
                        .then(if (on) Modifier.border(1.5.dp, p.accent, RoundedCornerShape(16.dp)) else Modifier)
                        .clickable(role = Role.Tab, onClick = { filter = name })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    T(name, 16, color = if (on) p.text else p.textDim, weight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        when {
            learned.isEmpty() -> EmptyNote("아직 배운 표현이 없어요", "첫 학습을 마치면 여기에 모여요.")
            shown.isEmpty() -> EmptyNote("여기에 해당하는 표현이 없어요", "다른 묶음을 골라 보세요.")
            else -> {
                val rows = if (wide) shown.chunked(2) else shown.chunked(1)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rows, key = { row -> row.first().first.id }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (b, m) ->
                                ExpressionCard(
                                    c, b, m,
                                    expanded = opened == b.id,
                                    onToggle = { opened = if (opened == b.id) null else b.id },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (wide && row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(title: String, detail: String) {
    val p = LocalPalette.current
    Panel(Modifier.fillMaxWidth()) {
        T(title, 20, weight = FontWeight.SemiBold)
        T(detail, 16, color = p.textDim)
    }
}

@Composable
internal fun StatusTag(m: Mastery) {
    val p = LocalPalette.current
    when {
        m.needsWork -> Tag("↻ 다시 익힐 표현", p.retry, p.retrySoft)
        m.recallConfirmedDay >= 0 -> Tag("✓ 회상 확인", p.good, p.goodSoft)
        else -> Tag("연습한 표현")
    }
}

@Composable
private fun ExpressionCard(
    c: AppController,
    b: Bundle,
    m: Mastery,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Panel(modifier, onClick = onToggle) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(b.topic)
            StatusTag(m)
        }
        T(b.en, 22, weight = FontWeight.SemiBold, serif = true)
        T(b.ko, 16, color = p.textDim)
        if (expanded) {
            T("상황 · ${b.situation}", 15, color = p.textDim)
            T(b.note, 15)
            T("바꿔 말하기", 14, color = p.textDim)
            T("${b.variationA.en}  ·  ${b.variationA.ko}", 15)
            T("${b.variationB.en}  ·  ${b.variationB.ko}", 15)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!c.silent) {
                BigButton(
                    "듣기", { c.listen(b.en) },
                    kind = ButtonKind.SECONDARY, enabled = c.gate.parked, minHeight = 50, textSize = 16,
                )
            }
            BigButton(
                if (m.favorite) "★ 즐겨찾기" else "☆ 즐겨찾기", { c.toggleFavorite(b.id) },
                kind = ButtonKind.OUTLINE, minHeight = 50, textSize = 16,
            )
            if (!expanded) {
                BigButton("자세히", onToggle, kind = ButtonKind.OUTLINE, minHeight = 50, textSize = 16)
            }
        }
    }
}

// ── UI-10 내 기록 ──────────────────────────────────────────

@Composable
private fun RecordScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val stats = Engine.stats(c.data, c.today)
    TwoPane(
        wide,
        Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
        leftWeight = 1f,
        left = {
            T("내 기록", 28, weight = FontWeight.Bold)
            if (c.data.attempts.isEmpty()) {
                T("아직 기록이 없어요. 첫 한마디부터 시작하면 돼요.", 16, color = p.textDim)
            }
            WeekPanel(stats, Modifier.fillMaxWidth())
            GoalStepper(c)
            Panel(Modifier.fillMaxWidth()) {
                T("이번 주 학습", 15, color = p.textDim)
                T(
                    "끝까지 마친 학습 ${stats.sessionsCompletedThisWeek}번 · 중간에 마친 학습 ${stats.sessionsPartialThisWeek}번",
                    17,
                )
                T("중간에 마쳐도 저장한 활동은 학습일로 인정돼요.", 14, color = p.textDim)
            }
        },
        right = {
            StatTile("연습한 표현", stats.practicedExpressions, "활동을 한 번 이상 해 본 표현")
            StatTile("회상 확인", stats.recallConfirmed, "힌트 없이 기억에서 꺼낸 표현", p.good)
            StatTile("다시 익힐 표현", stats.needsWork, "최근에 어려웠던 표현 · 다음 학습일에 다시 나와요", p.retry)
            T("연습은 참여, 회상 확인은 기억을 뜻해요. 한 번 맞혔다고 숙련으로 보지 않아요.", 14, color = p.textDim)
        },
    )
}

@Composable
private fun StatTile(title: String, value: Int, detail: String, color: Color = LocalPalette.current.text) {
    val p = LocalPalette.current
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T(title, 18, weight = FontWeight.SemiBold)
                T(detail, 14, color = p.textDim)
            }
            T("$value", 34, color = color, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalStepper(c: AppController) {
    val p = LocalPalette.current
    val days = c.data.settings.weeklyGoalDays
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                T("주간 목표", 18, weight = FontWeight.SemiBold)
                T("일주일에 며칠, 하루 한 번", 14, color = p.textDim)
            }
            BigButton(
                "−", { c.updateSettings { it.copy(weeklyGoalDays = (days - 1).coerceAtLeast(1)) } },
                Modifier.width(60.dp), kind = ButtonKind.SECONDARY, enabled = days > 1, minHeight = 54,
            )
            T("${days}일", 22, weight = FontWeight.Bold)
            BigButton(
                "+", { c.updateSettings { it.copy(weeklyGoalDays = (days + 1).coerceAtMost(7)) } },
                Modifier.width(60.dp), kind = ButtonKind.SECONDARY, enabled = days < 7, minHeight = 54,
            )
        }
    }
}

// ── UI-11 설정 ─────────────────────────────────────────────

@Composable
private fun SettingsScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val settings = c.data.settings
    var confirmErase by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    TwoPane(
        wide,
        Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
        leftWeight = 1f,
        left = {
            T("설정", 28, weight = FontWeight.Bold)
            T("학습", 15, color = p.textDim)
            Segmented(
                listOf(Goal.DAILY to "일상·여행", Goal.WORK to "가벼운 업무"),
                settings.goal,
                { goal -> c.updateSettings { it.copy(goal = goal) } },
                Modifier.fillMaxWidth(),
            )
            T("주제 순서만 바뀌어요. 이미 배운 표현은 그대로 남아요.", 14, color = p.textDim)
            Segmented(
                listOf(SessionLength.THREE_MIN to "3분 기본", SessionLength.ONE_MIN to "1분 한마디"),
                settings.length,
                { length -> c.updateSettings { it.copy(length = length) } },
                Modifier.fillMaxWidth(),
            )
            GoalStepper(c)
        },
        right = {
            T("소리", 15, color = p.textDim)
            ToggleRow("무음으로 학습", "소리 없이 글자로 진행해요", settings.silent, { on -> c.updateSettings { it.copy(silent = on) } })
            ToggleRow("천천히 듣기", "음원을 항상 느리게 들려줘요", settings.slowAudio, { on -> c.updateSettings { it.copy(slowAudio = on) } })
            T(
                when (c.speaker.state) {
                    VoiceState.READY -> "음원: 이 기기의 영어 음성으로 들려줘요. 마이크는 쓰지 않아요."
                    VoiceState.STARTING -> "음원을 준비하고 있어요."
                    VoiceState.UNAVAILABLE -> "이 기기에서는 영어 음원을 재생할 수 없어 글자로 진행해요. 마이크는 쓰지 않아요."
                },
                14,
                color = p.textDim,
            )
            T("데이터", 15, color = p.textDim)
            Panel(Modifier.fillMaxWidth()) {
                T("기록은 이 차량에만 저장돼요", 17, weight = FontWeight.SemiBold)
                T("계정·위치·이동 경로·연락처·음성을 수집하지 않고, 서버로 보내지 않아요.", 14, color = p.textDim)
                if (confirmErase) {
                    T("학습 기록과 설정을 모두 지워요. 되돌릴 수 없어요.", 15, color = p.retry)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton(
                            "지우기",
                            {
                                confirmErase = false
                                c.eraseEverything()
                            },
                            Modifier.weight(1f), kind = ButtonKind.SECONDARY, minHeight = 54, textSize = 17,
                        )
                        BigButton("취소", { confirmErase = false }, Modifier.weight(1f), kind = ButtonKind.OUTLINE, minHeight = 54, textSize = 17)
                    }
                } else {
                    BigButton("기록 모두 지우기", { confirmErase = true }, Modifier.fillMaxWidth(), kind = ButtonKind.OUTLINE, minHeight = 54, textSize = 17)
                }
            }
            T("도착한마디 $version · 콘텐츠 v${Content.CONTENT_VERSION} · 표현 ${Content.bundles.size}개", 13, color = p.textDim)
        },
    )
}

// ── UI-01 첫 안내·설정 ─────────────────────────────────────

@Composable
private fun OnboardingScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    var goal by remember { mutableStateOf(Goal.DAILY) }
    var level by remember { mutableStateOf(Level.BASIC) }
    var length by remember { mutableStateOf(SessionLength.THREE_MIN) }
    val parked = c.gate.parked
    TwoPane(
        wide,
        Modifier.padding(horizontal = 34.dp, vertical = 28.dp),
        leftWeight = 1f,
        left = {
            T("도착한마디", 20, color = p.accent, weight = FontWeight.Bold)
            T("주차 후,\n내리기 전 한마디", 40, weight = FontWeight.Bold)
            T("회사나 집에 도착해 주차한 뒤 1~3분.\n오늘 쓸 영어 표현 하나를 듣고, 말해 보고, 다시 떠올려요.", 17, color = p.textDim)
            T("주차 중에만 쓸 수 있어요. 언제든 한 번에 끝낼 수 있고, 가입 없이 바로 시작해요.", 15, color = p.textDim)
            GateChip(c.gate.state)
        },
        right = {
            T("어떤 말을 연습할까요?", 15, color = p.textDim)
            Segmented(listOf(Goal.DAILY to "일상·여행", Goal.WORK to "가벼운 업무"), goal, { goal = it }, Modifier.fillMaxWidth())
            T("지금 수준은요?", 15, color = p.textDim)
            Segmented(listOf(Level.BASIC to "기초부터", Level.SHORT_SENTENCES to "짧은 문장은 알아요"), level, { level = it }, Modifier.fillMaxWidth())
            T("한 번에 얼마나 할까요?", 15, color = p.textDim)
            Segmented(listOf(SessionLength.THREE_MIN to "3분 기본", SessionLength.ONE_MIN to "1분 한마디"), length, { length = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            BigButton(
                "첫 학습 시작",
                { c.finishOnboarding(goal, level, length, startNow = true) },
                Modifier.fillMaxWidth(),
                enabled = parked,
                sub = if (parked) "활동 ${length.activityCount}개 · 예상 ${length.minutes}분" else gateMessage(c.gate.state),
                minHeight = 76,
            )
            BigButton(
                "나중에 시작",
                { c.finishOnboarding(goal, level, length, startNow = false) },
                Modifier.fillMaxWidth(),
                kind = ButtonKind.OUTLINE,
                minHeight = 56,
                textSize = 17,
            )
        },
    )
}
