package com.vocar.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocar.app.domain.Diagram
import com.vocar.app.domain.DiagramKind
import com.vocar.app.domain.Dictionary
import com.vocar.app.domain.EconBadges
import com.vocar.app.domain.EconProgress
import com.vocar.app.domain.EconQuestion
import com.vocar.app.domain.EconQuiz
import com.vocar.app.domain.EconTerm
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val TOPIC_ALL = "전체"
private const val TOPIC_REVIEW = "다시 볼 개념"

/**
 * 경제 상식 탭: 오늘의 퀘스트(하루 3문제, 답하면 바로 해설)와 경제 사전(개념 · 쉬운 설명 · 그림).
 * 차 안이라 키보드와 검색창은 두지 않는다. 문제 풀기는 주차 중에만 된다.
 */
@Composable
internal fun EconScreen(c: AppController, wide: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = if (wide) 6.dp else 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Segmented(
            EconSection.entries.map { it to it.label },
            c.econSection,
            { c.econSection = it; c.dictionaryTerm = null },
            Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Appear(c.econSection to c.dictionaryTerm) {
                when (c.econSection) {
                    EconSection.QUEST -> QuestView(c, wide)
                    EconSection.DICT -> {
                        val term = c.dictionaryTerm?.let { Dictionary.find(it) }
                        if (term != null) TermDetail(c, term, wide) else TermList(c, wide)
                    }
                }
            }
        }
    }
}

// ── 연속 기록 ────────────────────────────────────────────

/** 🔥 연속 N일 꼬리표. 오늘 아직 안 했으면 회색으로 "오늘 하면 N+1일". */
@Composable
private fun StreakPill(progress: EconProgress, today: Long) {
    val p = LocalPalette.current
    val streak = progress.currentStreak(today)
    val doneToday = progress.done
    val text = when {
        doneToday -> "🔥 연속 ${streak}일"
        streak > 0 -> "🔥 오늘 하면 ${streak + 1}일 연속"
        else -> "🔥 오늘부터 연속 시작"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (doneToday) p.retrySoft else p.tile)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { T(text, 14, color = if (doneToday) p.retry else p.textDim, weight = FontWeight.Bold) }
}

/** 같은 폭으로 나눈 진행 막대. 끝낸 칸은 초록, 지금 칸은 진하게, 남은 칸은 옅게. */
@Composable
private fun QuestBar(total: Int, done: Int, current: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .then(
                        when {
                            i < done -> Modifier.background(p.accentBrush)
                            i == current -> Modifier.background(p.text)
                            else -> Modifier.background(p.line)
                        },
                    ),
            )
        }
    }
}

// ── 오늘의 퀘스트 ─────────────────────────────────────────

@Composable
private fun QuestView(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val progress = c.econ
    val qs = progress.questions()
    val shown = c.questShown
    if (shown >= qs.size) {
        QuestResult(c, wide)
        return
    }
    val q = qs[shown]
    val picked = progress.picks.getOrNull(shown)
    TwoPane(
        wide,
        leftWeight = 1.05f,
        left = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T("${progress.dayNumber()}일차 · 오늘의 경제 상식 퀴즈", 15, color = p.textDim, modifier = Modifier.weight(1f))
                StreakPill(progress, c.today)
            }
            QuestBar(qs.size, progress.picks.size, shown, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag("문제 ${shown + 1} / ${qs.size}")
                q.term?.let { Tag(it.topic) }
            }
            T(q.prompt, 22, weight = FontWeight.Bold)
            q.choices.forEachIndexed { i, text ->
                ChoiceRow(
                    text = text,
                    state = when {
                        picked == null -> ChoiceState.OPEN
                        i == q.answer -> ChoiceState.CORRECT
                        i == picked -> ChoiceState.WRONG
                        else -> ChoiceState.DIM
                    },
                    enabled = picked == null && c.gate.parked,
                    onClick = { c.answerQuest(i) },
                )
            }
            if (picked == null && !c.gate.parked) GateChip(c.gate.state)
            // 답한 뒤 다음으로 가는 버튼은 보기 바로 아래에 둔다(오른쪽 해설이 길어도 항상 보이게).
            if (picked != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigButton(
                        if (shown == qs.size - 1) "결과 보기" else "다음 문제 →",
                        { c.nextQuest() },
                        Modifier.weight(1.4f),
                        kind = ButtonKind.PRIMARY,
                        minHeight = 60,
                        textSize = 18,
                    )
                    q.term?.let { t ->
                        BigButton("사전 보기", { c.openTerm(t.id) }, Modifier.weight(1f), kind = ButtonKind.SECONDARY, minHeight = 60, textSize = 16)
                    }
                }
            }
        },
        right = {
            if (picked == null) {
                QuestHint(progress, c.today)
            } else {
                Explanation(q, picked)
            }
        },
    )
}

@Composable
private fun QuestHint(progress: EconProgress, today: Long) {
    val p = LocalPalette.current
    val streak = progress.currentStreak(today)
    val goal = EconBadges.nextStreakGoal(streak)
    HeroCard(Modifier.fillMaxWidth()) {
        T("하루 ${EconQuiz.PER_DAY}문제면 오늘 퀘스트 끝", 15, color = p.onHeroDim)
        T(if (streak > 0) "🔥 ${streak}일째 이어 가는 중" else "🔥 오늘부터 연속 기록 시작", 24, color = p.onHero, weight = FontWeight.Bold)
        if (goal != null) T("${goal}일 연속이면 배지를 받아요 · ${goal - streak}일 남음", 14, color = p.onHeroDim)
        if (progress.freezes > 0) T("🧊 연속 보호권 ${progress.freezes}개: 하루를 놓쳐도 연속이 이어져요", 14, color = p.onHeroDim)
    }
    Panel(Modifier.fillMaxWidth()) {
        T("보기를 고르면 바로 해설과 그림이 나와요", 16, weight = FontWeight.SemiBold)
        T("틀린 개념은 경제 사전의 '다시 볼 개념'에 모여요.", 14, color = p.textDim)
    }
}

private enum class ChoiceState { OPEN, CORRECT, WRONG, DIM }

@Composable
private fun ChoiceRow(text: String, state: ChoiceState, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val bg = when (state) {
        ChoiceState.CORRECT -> p.accentSoft
        ChoiceState.WRONG -> p.retrySoft
        else -> p.card
    }
    val border = when (state) {
        ChoiceState.CORRECT -> p.accentA
        ChoiceState.WRONG -> p.retry
        else -> p.line
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(
            text, 18,
            modifier = Modifier.weight(1f),
            color = if (state == ChoiceState.DIM) p.textDim else p.text,
            weight = if (state == ChoiceState.CORRECT) FontWeight.Bold else FontWeight.Normal,
        )
        when (state) {
            ChoiceState.CORRECT -> T("✓ 정답", 15, color = p.good, weight = FontWeight.Bold)
            ChoiceState.WRONG -> T("✗ 내 답", 15, color = p.retry, weight = FontWeight.Bold)
            else -> {}
        }
    }
}

@Composable
private fun Explanation(q: EconQuestion, picked: Int) {
    val p = LocalPalette.current
    val right = picked == q.answer
    Panel(Modifier.fillMaxWidth(), color = if (right) p.accentSoft else p.retrySoft) {
        T(
            if (right) "정답이에요!" else "아쉬워요. 정답은 '${q.choices[q.answer]}'",
            20, weight = FontWeight.Bold, color = if (right) p.good else p.retry,
        )
        T(q.explanation, 16)
    }
    q.shownDiagram?.let { DiagramPanel(it) }
}

@Composable
private fun QuestResult(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val progress = c.econ
    val qs = progress.questions()
    val streak = progress.currentStreak(c.today)
    TwoPane(
        wide,
        left = {
            HeroCard(Modifier.fillMaxWidth()) {
                T("${progress.totalDays}일차 퀘스트 완료", 15, color = p.onHeroDim)
                T("🔥 연속 ${streak}일", 30, color = p.onHero, weight = FontWeight.Black)
                T("${qs.size}문제 중 ${progress.correct}개 맞혔어요 · 내일 새 문제가 나와요", 15, color = p.onHeroDim)
                EconBadges.nextStreakGoal(streak)?.let { goal ->
                    T("다음 배지까지 ${goal - streak}일 · ${goal}일 연속", 14, color = p.onHeroDim)
                }
            }
            if (progress.newBadges.isNotEmpty()) {
                Panel(Modifier.fillMaxWidth(), color = p.accentSoft) {
                    T("새 배지를 받았어요!", 18, weight = FontWeight.Bold, color = p.good)
                    progress.newBadges.mapNotNull { EconBadges.find(it) }.forEach { b ->
                        T("${b.icon}  ${b.title} · ${b.detail}", 16, weight = FontWeight.SemiBold)
                    }
                }
            }
            qs.zip(progress.picks).forEach { (q, pick) ->
                Panel(Modifier.fillMaxWidth(), onClick = { q.term?.let { c.openTerm(it.id) } }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (pick == q.answer) Tag("✓ 맞힘", on = true) else Tag("✗ 틀림", fg = p.retry, bg = p.retrySoft)
                        q.term?.let { T(it.name, 15, color = p.textDim) }
                    }
                    T(q.prompt, 16, weight = FontWeight.SemiBold, maxLines = 2)
                    T("정답: ${q.choices[q.answer]}", 14, color = p.textDim)
                }
            }
        },
        right = {
            BadgeShelf(progress)
            Panel(Modifier.fillMaxWidth()) {
                T("다시 볼 개념", 18, weight = FontWeight.SemiBold)
                if (progress.review.isEmpty()) {
                    T("틀린 개념이 없어요. 사전에서 다른 개념을 둘러봐도 좋아요.", 15, color = p.textDim)
                } else {
                    T("눌러서 확인하면 목록에서 빠져요.", 14, color = p.textDim)
                    ChipRow(progress.review.mapNotNull { Dictionary.find(it) }) { c.openTerm(it.id) }
                }
            }
        },
    )
}

/** 배지 진열장. 받은 배지는 진하게, 아직인 배지는 흐리게 보여 다음 목표를 알려 준다. */
@Composable
private fun BadgeShelf(progress: EconProgress) {
    val p = LocalPalette.current
    val earned = EconBadges.earned(progress).map { it.id }.toSet()
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("배지", 18, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            T("${earned.size} / ${EconBadges.all.size}", 14, color = p.textDim)
        }
        EconBadges.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { b ->
                    val on = b.id in earned
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (on) p.card else p.tile)
                            .border(1.dp, if (on) p.accentA else p.line, RoundedCornerShape(18.dp))
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        T(if (on) b.icon else "🔒", 24)
                        T(b.title, 13, color = if (on) p.text else p.textDim, weight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (progress.freezes > 0) T("🧊 연속 보호권 ${progress.freezes}개 (7일 연속마다 1개)", 14, color = p.textDim)
    }
}

/** 오늘 화면의 "오늘의 경제 상식 퀴즈" 카드. 연속 기록을 함께 보여 주고, 누르면 퀘스트로 간다. */
@Composable
internal fun TodayQuestCard(c: AppController) {
    val progress = c.econ
    val total = EconQuiz.PER_DAY
    Panel(Modifier.fillMaxWidth(), onClick = { c.openQuest() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("오늘의 경제 상식 퀴즈", 17, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (progress.done) Tag("✓ 완료", on = true) else Tag("${progress.picks.size} / $total")
        }
        StreakPill(progress, c.today)
        QuestBar(total, progress.picks.size, if (progress.done) -1 else progress.picks.size, Modifier.fillMaxWidth())
    }
}

// ── 경제 사전 ────────────────────────────────────────────

@Composable
private fun TermList(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val review = c.econ.review
    var topic by remember { mutableStateOf(if (review.isNotEmpty()) TOPIC_REVIEW else TOPIC_ALL) }
    val filters = buildList {
        add(TOPIC_ALL)
        if (review.isNotEmpty()) add(TOPIC_REVIEW)
        addAll(Dictionary.topics)
    }
    val shown = when (topic) {
        TOPIC_ALL -> Dictionary.terms.sortedBy { it.name }
        TOPIC_REVIEW -> review.mapNotNull { Dictionary.find(it) }
        else -> Dictionary.byTopic(topic).sortedBy { it.name }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filters.forEach { name ->
                val on = topic == name
                val label = if (name == TOPIC_REVIEW) "↻ $name ${review.size}" else name
                Box(
                    Modifier
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(50))
                        .then(if (on) Modifier.background(p.accentBrush) else Modifier.background(p.tile))
                        .clickable(role = Role.Tab, onClick = { topic = name })
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    T(
                        if (on) "✓ $label" else label,
                        15,
                        color = if (on) p.onAccent else if (name == TOPIC_REVIEW) p.retry else p.textDim,
                        weight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        if (shown.isEmpty()) {
            Panel(Modifier.fillMaxWidth()) { T("여기에 해당하는 개념이 없어요", 18, weight = FontWeight.SemiBold) }
        } else {
            val rows = if (wide) shown.chunked(2) else shown.chunked(1)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rows, key = { row -> row.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { t -> TermCard(t, t.id in review, Modifier.weight(1f)) { c.openTerm(t.id) } }
                        if (wide && row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TermCard(t: EconTerm, needsReview: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Panel(modifier, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag(t.topic)
            if (t.diagram != null) Tag("그림")
            if (needsReview) Tag("↻ 다시 보기", fg = p.retry, bg = p.retrySoft)
        }
        T(t.name, 21, weight = FontWeight.Bold)
        T(t.plain, 14, color = p.textDim, maxLines = 2)
    }
}

@Composable
private fun TermDetail(c: AppController, t: EconTerm, wide: Boolean) {
    val p = LocalPalette.current
    val related = t.related.mapNotNull { Dictionary.find(it) }
    val quiz = EconQuiz.bank.filter { it.termId == t.id }
    TwoPane(
        wide,
        left = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BigButton("← 목록", { c.dictionaryTerm = null }, kind = ButtonKind.SECONDARY, minHeight = 48, textSize = 15)
                Tag(t.topic)
            }
            T(t.name, 30, weight = FontWeight.Bold)
            Panel(Modifier.fillMaxWidth()) {
                T("쉬운 설명", 13, color = p.textDim)
                T(t.plain, 18)
            }
            Panel(Modifier.fillMaxWidth()) {
                T("이렇게 쓰여요", 13, color = p.textDim)
                T(t.example, 17, weight = FontWeight.SemiBold)
            }
            if (related.isNotEmpty()) {
                T("관련 개념", 14, color = p.textDim)
                ChipRow(related) { c.openTerm(it.id) }
            }
        },
        right = {
            t.diagram?.let { DiagramPanel(it) }
            if (quiz.isNotEmpty()) {
                Panel(Modifier.fillMaxWidth()) {
                    T("이 개념이 나오는 문제", 13, color = p.textDim)
                    quiz.forEach { q ->
                        T(q.prompt, 15, weight = FontWeight.SemiBold)
                        T("정답: ${q.choices[q.answer]}", 14, color = p.textDim)
                    }
                }
            }
        },
    )
}

@Composable
private fun ChipRow(terms: List<EconTerm>, onClick: (EconTerm) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        terms.forEach { r ->
            Box(
                Modifier
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(p.card)
                    .border(1.dp, p.line, RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = { onClick(r) })
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { T(r.name, 15) }
        }
    }
}

// ── 그림(도식) ──────────────────────────────────────────
// 경제 신문 퀴즈 해설처럼 축 이름, 곡선 이름, 이동 화살표, 균형점을 그림 안에 직접 적는다.

@Composable
internal fun DiagramPanel(d: Diagram) {
    val p = LocalPalette.current
    Panel(Modifier.fillMaxWidth(), color = p.card) {
        T("그림으로 보기", 13, color = p.textDim)
        if (d.kind == DiagramKind.FLOW) {
            FlowSteps(d.steps)
        } else {
            val tm = rememberTextMeasurer()
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            ) { drawChart(d.kind, p, tm) }
            if (d.kind == DiagramKind.BUSINESS_CYCLE) T(d.steps.joinToString("  →  "), 14, weight = FontWeight.SemiBold)
        }
        T(d.caption, 14, color = p.textDim)
    }
}

/** 그래프 영역 안의 점. fx, fy는 0..1(왼쪽 아래가 0,0). */
private class Plot(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun at(fx: Float, fy: Float) = Offset(left + (right - left) * fx, bottom - (bottom - top) * fy)
}

private fun DrawScope.label(
    tm: TextMeasurer,
    text: String,
    at: Offset,
    color: Color,
    sizeSp: Int = 12,
    bold: Boolean = false,
    alignEnd: Boolean = false,
    above: Boolean = false,
) {
    val layout = tm.measure(text, TextStyle(color = color, fontSize = sizeSp.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val x = (if (alignEnd) at.x - w else at.x).coerceIn(0f, maxOf(0f, size.width - w))
    val y = (if (above) at.y - h else at.y - h / 2).coerceIn(0f, maxOf(0f, size.height - h))
    drawText(layout, topLeft = Offset(x, y))
}

private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, width: Float = 5f) {
    drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    val angle = atan2(to.y - from.y, to.x - from.x)
    val head = 16f
    listOf(angle + 2.6f, angle - 2.6f).forEach { a ->
        drawLine(color, to, Offset(to.x + head * cos(a), to.y + head * sin(a)), strokeWidth = width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.axes(tm: TextMeasurer, p: Palette, plot: Plot, yName: String, xName: String) {
    drawLine(p.textDim, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), strokeWidth = 3f)
    drawLine(p.textDim, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), strokeWidth = 3f)
    label(tm, yName, Offset(plot.left + 8f, plot.top), p.textDim, bold = true)
    label(tm, xName, Offset(plot.right, plot.bottom - 6f), p.textDim, bold = true, alignEnd = true, above = true)
}

/** 수요·공급 곡선을 그리고 균형점과 점선 안내선을 표시한다. 곡선은 fy = a ± fx 꼴의 직선이다. */
private fun DrawScope.supplyDemand(tm: TextMeasurer, p: Palette, plot: Plot, kind: DiagramKind) {
    val line = 7f
    val faded = p.line
    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
    // 기준: 수요 fy = 1.0 - fx, 공급 fy = fx + 0.05 (fx 0.1..0.85) → 균형 (0.475, 0.525)
    val demand = plot.at(0.1f, 0.9f) to plot.at(0.85f, 0.15f)
    val supply = plot.at(0.1f, 0.15f) to plot.at(0.85f, 0.9f)
    val e0 = plot.at(0.475f, 0.525f)
    fun guide(e: Offset, color: Color) {
        drawLine(color, Offset(plot.left, e.y), e, strokeWidth = 2.5f, pathEffect = dash)
        drawLine(color, Offset(e.x, plot.bottom), e, strokeWidth = 2.5f, pathEffect = dash)
    }
    when (kind) {
        DiagramKind.SUPPLY_DOWN -> {
            // 공급 감소: fy = fx + 0.25 (fx 0.05..0.65) → 새 균형 (0.375, 0.625)
            drawLine(faded, supply.first, supply.second, strokeWidth = line, cap = StrokeCap.Round)
            val s1 = plot.at(0.05f, 0.30f) to plot.at(0.65f, 0.90f)
            drawLine(p.accentA, demand.first, demand.second, strokeWidth = line, cap = StrokeCap.Round)
            drawLine(p.retry, s1.first, s1.second, strokeWidth = line, cap = StrokeCap.Round)
            arrow(plot.at(0.80f, 0.78f), plot.at(0.62f, 0.78f), p.retry)
            val e1 = plot.at(0.375f, 0.625f)
            guide(e0, faded)
            guide(e1, p.text)
            drawCircle(faded, 9f, e0)
            drawCircle(p.text, 11f, e1)
            label(tm, "수요", demand.second + Offset(-6f, -18f), p.accentA, bold = true, alignEnd = true)
            label(tm, "공급(전)", supply.second + Offset(0f, 22f), p.textDim, alignEnd = true)
            label(tm, "공급(후)", s1.second + Offset(-12f, 0f), p.retry, bold = true, alignEnd = true)
            label(tm, "가격 ↑", Offset(plot.left + 6f, e1.y - 4f), p.text, bold = true, above = true)
        }
        DiagramKind.DEMAND_UP -> {
            // 수요 증가: fy = 1.2 - fx (fx 0.3..0.95) → 새 균형 (0.575, 0.625)
            drawLine(faded, demand.first, demand.second, strokeWidth = line, cap = StrokeCap.Round)
            val d1 = plot.at(0.3f, 0.9f) to plot.at(0.95f, 0.25f)
            drawLine(p.retry, supply.first, supply.second, strokeWidth = line, cap = StrokeCap.Round)
            drawLine(p.accentA, d1.first, d1.second, strokeWidth = line, cap = StrokeCap.Round)
            arrow(plot.at(0.20f, 0.72f), plot.at(0.40f, 0.72f), p.accentA)
            val e1 = plot.at(0.575f, 0.625f)
            guide(e0, faded)
            guide(e1, p.text)
            drawCircle(faded, 9f, e0)
            drawCircle(p.text, 11f, e1)
            label(tm, "수요(전)", demand.second + Offset(0f, -18f), p.textDim, alignEnd = true)
            label(tm, "수요(후)", d1.second + Offset(0f, -20f), p.accentA, bold = true, alignEnd = true)
            label(tm, "공급", supply.second + Offset(-12f, 4f), p.retry, bold = true, alignEnd = true)
            label(tm, "가격 ↑", Offset(plot.left + 6f, e1.y - 4f), p.text, bold = true, above = true)
        }
        else -> {
            drawLine(p.accentA, demand.first, demand.second, strokeWidth = line, cap = StrokeCap.Round)
            drawLine(p.retry, supply.first, supply.second, strokeWidth = line, cap = StrokeCap.Round)
            guide(e0, p.text)
            drawCircle(p.text, 11f, e0)
            label(tm, "수요", demand.second + Offset(-6f, -18f), p.accentA, bold = true, alignEnd = true)
            label(tm, "공급", supply.second + Offset(-12f, 4f), p.retry, bold = true, alignEnd = true)
            label(tm, "균형 가격", Offset(plot.left + 6f, e0.y - 4f), p.text, bold = true, above = true)
            label(tm, "균형점", e0 + Offset(16f, 0f), p.text)
        }
    }
}

private fun DrawScope.drawChart(kind: DiagramKind, p: Palette, tm: TextMeasurer) {
    val plot = Plot(left = 18f, top = 26f, right = size.width - 12f, bottom = size.height - 26f)
    val line = 7f
    when (kind) {
        DiagramKind.SUPPLY_DEMAND, DiagramKind.SUPPLY_DOWN, DiagramKind.DEMAND_UP -> {
            axes(tm, p, plot, "가격", "수량")
            supplyDemand(tm, p, plot, kind)
        }
        DiagramKind.RATE_VS_BOND -> {
            axes(tm, p, plot, "높음", "시간 →")
            val rate = plot.at(0.05f, 0.2f) to plot.at(0.9f, 0.85f)
            val bond = plot.at(0.05f, 0.85f) to plot.at(0.9f, 0.2f)
            drawLine(p.retry, rate.first, rate.second, strokeWidth = line, cap = StrokeCap.Round)
            drawLine(p.accentA, bond.first, bond.second, strokeWidth = line, cap = StrokeCap.Round)
            label(tm, "금리 ↑", rate.second + Offset(0f, 22f), p.retry, bold = true, alignEnd = true)
            label(tm, "채권 가격 ↓", bond.second + Offset(0f, -24f), p.accentA, bold = true, alignEnd = true)
            label(tm, "시소처럼 반대로", plot.at(0.5f, 0.5f) + Offset(16f, 0f), p.textDim)
        }
        DiagramKind.BUSINESS_CYCLE -> {
            axes(tm, p, plot, "경기", "시간 →")
            val path = Path()
            val steps = 90
            var peak = Offset.Zero
            var trough = Offset.Zero
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val fy = 0.5f + 0.32f * sin(t * 2 * PI * 1.15 + 0.3).toFloat() + 0.06f * t
                val pt = plot.at(0.04f + 0.92f * t, fy)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                if (i == 0 || pt.y < peak.y) peak = pt
                if (i == 0 || pt.y > trough.y) trough = pt
            }
            drawPath(path, p.accentA, style = Stroke(width = line, cap = StrokeCap.Round))
            drawCircle(p.text, 10f, peak)
            drawCircle(p.retry, 10f, trough)
            label(tm, "정점(호황)", peak + Offset(-30f, -14f), p.text, bold = true, above = true)
            label(tm, "저점(불황)", trough + Offset(-30f, 16f), p.retry, bold = true)
            label(tm, "후퇴", Offset((peak.x + trough.x) / 2 + 12f, (peak.y + trough.y) / 2), p.textDim)
        }
        DiagramKind.PURCHASING_POWER -> {
            axes(tm, p, plot, "많음", "시간 →")
            val n = 4
            val gap = (plot.right - plot.left) / n
            for (i in 0 until n) {
                val top = plot.at(0f, 0.8f - i * 0.17f).y
                val x = plot.left + gap * i + gap * 0.25f
                drawRect(p.accentA, topLeft = Offset(x, top), size = Size(gap * 0.5f, plot.bottom - top))
            }
            val priceFrom = plot.at(0.12f, 0.25f)
            val priceTo = plot.at(0.88f, 0.95f)
            drawLine(p.retry, priceFrom, priceTo, strokeWidth = line, cap = StrokeCap.Round)
            label(tm, "물가 ↑", priceTo + Offset(-4f, 20f), p.retry, bold = true, alignEnd = true)
            label(tm, "같은 돈으로 살 수 있는 양 ↓", plot.at(0.98f, 0.04f), p.good, bold = true, alignEnd = true, above = true)
        }
        DiagramKind.FLOW -> Unit // 단계 상자로 그린다(FlowSteps)
    }
}

/** 원인 → 결과 흐름. 차 화면 폭이 좁아도 잘리지 않게 위에서 아래로 번호를 붙여 잇는다. */
@Composable
private fun FlowSteps(steps: List<String>) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == 0) p.accentBrush else SolidColor(p.tile)),
                    contentAlignment = Alignment.Center,
                ) { T("${i + 1}", 13, weight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (i == 0) p.accentSoft else p.tile)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) { T(s, 15, weight = if (i == 0) FontWeight.Bold else FontWeight.SemiBold) }
            }
            if (i < steps.size - 1) T("↓", 16, color = p.textDim, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
