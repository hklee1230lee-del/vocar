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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vocar.app.domain.Diagram
import com.vocar.app.domain.DiagramKind
import com.vocar.app.domain.Dictionary
import com.vocar.app.domain.EconQuestion
import com.vocar.app.domain.EconQuiz
import com.vocar.app.domain.EconTerm
import kotlin.math.PI
import kotlin.math.sin

private const val TOPIC_ALL = "전체"
private const val TOPIC_REVIEW = "다시 볼 개념"

/**
 * 경제 상식 탭: 오늘의 퀘스트(하루 3문제, 답하면 바로 아래 해설)와 경제 사전(개념 · 쉬운 설명 · 그림).
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

// ── 오늘의 퀘스트 ─────────────────────────────────────────

@Composable
private fun QuestView(c: AppController, wide: Boolean) {
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
        leftWeight = 1.1f,
        left = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T("오늘의 경제 상식 퀴즈", 15, color = LocalPalette.current.textDim)
                StepDots(qs.size, progress.picks.size, shown)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                q.term?.let { Tag(it.topic) }
                Tag("문제 ${shown + 1} / ${qs.size}")
            }
            T(q.prompt, 24, weight = FontWeight.Bold)
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
        },
        right = {
            if (picked == null) {
                Panel(Modifier.fillMaxWidth()) {
                    T("하루 ${EconQuiz.PER_DAY}문제", 20, weight = FontWeight.SemiBold)
                    T("보기를 고르면 바로 아래에 해설이 나와요. 틀린 개념은 사전의 '다시 볼 개념'에 모여요.", 15, color = LocalPalette.current.textDim)
                }
            } else {
                Explanation(c, q, picked, last = shown == qs.size - 1)
            }
        },
    )
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
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
private fun Explanation(c: AppController, q: EconQuestion, picked: Int, last: Boolean) {
    val p = LocalPalette.current
    val right = picked == q.answer
    Panel(Modifier.fillMaxWidth(), color = if (right) p.accentSoft else p.retrySoft) {
        T(if (right) "정답이에요!" else "아쉬워요. 정답은 '${q.choices[q.answer]}'", 20, weight = FontWeight.Bold, color = if (right) p.good else p.retry)
        T("해설", 13, color = p.textDim)
        T(q.explanation, 16)
    }
    q.shownDiagram?.let { DiagramPanel(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        q.term?.let { t ->
            BigButton("사전에서 '${t.name}' 보기", { c.openTerm(t.id) }, kind = ButtonKind.SECONDARY, minHeight = 54, textSize = 15)
        }
        BigButton(if (last) "결과 보기" else "다음 문제", { c.nextQuest() }, kind = ButtonKind.PRIMARY, minHeight = 54, textSize = 16)
    }
}

@Composable
private fun QuestResult(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val progress = c.econ
    val qs = progress.questions()
    TwoPane(
        wide,
        left = {
            HeroCard(Modifier.fillMaxWidth()) {
                T("오늘의 퀘스트 완료", 15, color = p.onHeroDim)
                T("${qs.size}문제 중 ${progress.correct}개 맞혔어요", 26, color = p.onHero, weight = FontWeight.Bold)
                T("내일 새 문제가 나와요. 오늘은 여기까지면 충분해요.", 15, color = p.onHeroDim)
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
            Panel(Modifier.fillMaxWidth()) {
                T("다시 볼 개념", 20, weight = FontWeight.SemiBold)
                if (progress.review.isEmpty()) {
                    T("틀린 개념이 없어요. 사전에서 다른 개념을 둘러봐도 좋아요.", 15, color = p.textDim)
                } else {
                    T("눌러서 확인하면 목록에서 빠져요.", 14, color = p.textDim)
                    ChipRow(progress.review.mapNotNull { Dictionary.find(it) }) { c.openTerm(it.id) }
                }
            }
            BigButton("경제 사전 둘러보기", { c.econSection = EconSection.DICT }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, minHeight = 58)
        },
    )
}

/** 오늘 화면의 "오늘의 경제 상식 퀴즈" 카드. 누르면 퀘스트로 간다. */
@Composable
internal fun TodayQuestCard(c: AppController) {
    val p = LocalPalette.current
    val progress = c.econ
    val total = EconQuiz.PER_DAY
    Panel(Modifier.fillMaxWidth(), onClick = { c.openQuest() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("오늘의 경제 상식 퀴즈", 17, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (progress.done) Tag("✓ 완료", on = true) else Tag("${progress.picks.size} / $total")
        }
        T(
            when {
                progress.done -> "${total}문제 중 ${progress.correct}개 맞혔어요"
                progress.picks.isEmpty() -> "하루 ${total}문제, 답하면 바로 해설을 보여 줘요"
                else -> "이어서 풀기 · ${total - progress.picks.size}문제 남았어요"
            },
            14, color = p.textDim, maxLines = 1,
        )
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

@Composable
internal fun DiagramPanel(d: Diagram) {
    val p = LocalPalette.current
    Panel(Modifier.fillMaxWidth(), color = p.card) {
        T("그림으로 보기", 13, color = p.textDim)
        when (d.kind) {
            DiagramKind.FLOW -> FlowSteps(d.steps)
            else -> {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                ) { drawChart(d.kind, p) }
                Legend(d)
            }
        }
        T(d.caption, 14, color = p.textDim)
    }
}

/** 그래프 색: 첫 번째 선은 초록, 두 번째 선은 주황, 표시점은 글자색. 범례도 같은 순서다. */
private fun legendColors(p: Palette) = listOf(p.accentA, p.retry, p.text, p.textDim)

@Composable
private fun Legend(d: Diagram) {
    val p = LocalPalette.current
    val colors = legendColors(p)
    if (d.kind == DiagramKind.BUSINESS_CYCLE) {
        T(d.steps.joinToString("  →  "), 14, weight = FontWeight.SemiBold)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        d.steps.forEachIndexed { i, label ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colors[i % colors.size]))
                Spacer(Modifier.width(6.dp))
                T(label, 14)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChart(kind: DiagramKind, p: Palette) {
    val w = size.width
    val h = size.height
    val pad = 12f
    val line = 7f
    val left = pad * 2
    val bottom = h - pad
    // 축
    drawLine(p.line, Offset(left, pad), Offset(left, bottom), strokeWidth = 3f)
    drawLine(p.line, Offset(left, bottom), Offset(w - pad, bottom), strokeWidth = 3f)
    when (kind) {
        DiagramKind.SUPPLY_DEMAND -> {
            val a = Offset(left + w * 0.08f, pad + h * 0.05f)
            val b = Offset(w - pad * 3, bottom - h * 0.08f)
            val c = Offset(left + w * 0.08f, bottom - h * 0.08f)
            val d = Offset(w - pad * 3, pad + h * 0.05f)
            drawLine(p.accentA, a, b, strokeWidth = line, cap = StrokeCap.Round) // 수요: 값이 오를수록 덜 산다
            drawLine(p.retry, c, d, strokeWidth = line, cap = StrokeCap.Round) // 공급: 값이 오를수록 더 판다
            val mid = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
            drawLine(p.textDim, Offset(left, mid.y), mid, strokeWidth = 2f)
            drawCircle(p.text, radius = 10f, center = mid)
        }
        DiagramKind.RATE_VS_BOND -> {
            drawLine(p.retry, Offset(left + 10, bottom - h * 0.12f), Offset(w - pad * 3, pad + h * 0.08f), strokeWidth = line, cap = StrokeCap.Round)
            drawLine(p.accentA, Offset(left + 10, pad + h * 0.12f), Offset(w - pad * 3, bottom - h * 0.1f), strokeWidth = line, cap = StrokeCap.Round)
        }
        DiagramKind.BUSINESS_CYCLE -> {
            val path = Path()
            val span = w - left - pad * 2
            val steps = 80
            var peak = Offset.Zero
            var trough = Offset.Zero
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val x = left + pad + span * t
                val y = h / 2 - sin(t * 2 * PI * 1.25 + 0.2).toFloat() * h * 0.32f - t * h * 0.08f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                if (i == 0 || y < peak.y) peak = Offset(x, y)
                if (i == 0 || y > trough.y) trough = Offset(x, y)
            }
            drawPath(path, p.accentA, style = Stroke(width = line, cap = StrokeCap.Round))
            drawCircle(p.text, radius = 9f, center = peak)
            drawCircle(p.retry, radius = 9f, center = trough)
        }
        DiagramKind.PURCHASING_POWER -> {
            // 막대: 같은 돈으로 살 수 있는 양(줄어든다) · 선: 물가(오른다)
            val n = 4
            val gap = (w - left - pad * 2) / n
            for (i in 0 until n) {
                val bh = (h - pad * 2) * (0.85f - i * 0.17f)
                val x = left + gap * i + gap * 0.25f
                drawRect(p.accentA, topLeft = Offset(x, bottom - bh), size = androidx.compose.ui.geometry.Size(gap * 0.5f, bh))
            }
            drawLine(p.retry, Offset(left + gap * 0.5f, bottom - h * 0.2f), Offset(left + gap * (n - 0.5f), pad + h * 0.05f), strokeWidth = line, cap = StrokeCap.Round)
        }
        DiagramKind.FLOW -> Unit // 단계 상자로 그린다(FlowSteps)
    }
}

@Composable
private fun FlowSteps(steps: List<String>) {
    val p = LocalPalette.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        steps.forEachIndexed { i, s ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (i == 0) p.accentSoft else p.tile)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) { T(s, 15, weight = if (i == 0) FontWeight.Bold else FontWeight.SemiBold) }
            if (i < steps.size - 1) T("→", 18, color = p.textDim)
        }
    }
}

