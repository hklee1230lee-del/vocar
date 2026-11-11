package com.arrivaltalk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arrivaltalk.app.domain.ActivityKind
import com.arrivaltalk.app.domain.Choice
import com.arrivaltalk.app.domain.Content
import com.arrivaltalk.app.domain.Engine
import com.arrivaltalk.app.domain.Mode
import com.arrivaltalk.app.domain.Question
import com.arrivaltalk.app.domain.Session
import com.arrivaltalk.app.domain.SessionStatus
import kotlinx.coroutines.delay

// ── UI-03 문제 · UI-04 말하기 · UI-05 피드백 · UI-07 일시정지 ─────────────

/**
 * 문제 한 개, 답안 최대 3개. '끝내기'는 항상 오른쪽 위, 주 행동은 항상 오른쪽(좁은 창은 아래) 단에 둔다.
 * 시간 제한·카운트다운이 없다. 응답 속도에 맞춰 진행한다.
 */
@Composable
internal fun LessonScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val session = c.session
    val question = c.question
    if (session == null || question == null) {
        // 세션을 찾지 못하면 빈 문제로 진행하지 않고 홈으로 돌아갈 길만 보여준다.
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            T("학습을 불러오지 못했어요", 24, weight = FontWeight.Bold)
            BigButton("오늘 화면으로", { c.finishNow() })
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepDots(session.items.size, session.done, c.index)
                T(
                    "${modeLabel(session.mode)} ${session.length.minutes}분 · ${c.index + 1}/${session.items.size}",
                    14,
                    color = p.textDim,
                )
            }
            BigButton("끝내기", { c.finishNow() }, kind = ButtonKind.OUTLINE, minHeight = 52, textSize = 17)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                c.pause != null -> PausePanel(c, session)
                c.phase == Phase.ASK -> AskBody(c, question, wide)
                else -> FeedbackBody(c, session, question, wide)
            }
        }
    }
}

@Composable
private fun ExpressionBlock(en: String, ko: String, size: Int = 34) {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T(en, size, weight = FontWeight.SemiBold, serif = true)
        T(ko, 18, color = p.textDim)
    }
}

@Composable
private fun AudioRow(c: AppController, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton(
            if (c.speaker.speaking) "듣는 중…" else "다시 듣기",
            { c.listen(text) },
            kind = ButtonKind.SECONDARY, minHeight = 54, textSize = 17,
        )
        BigButton("천천히 듣기", { c.listen(text, slow = true) }, kind = ButtonKind.OUTLINE, minHeight = 54, textSize = 17)
    }
}

@Composable
private fun AskBody(c: AppController, q: Question, wide: Boolean) {
    val p = LocalPalette.current
    val kind = q.item.kind
    val silent = c.silent
    val audio = q.audioText
    val shadow = kind == ActivityKind.SHADOW || kind == ActivityKind.SHADOW_RECAP
    val recall = kind == ActivityKind.RECALL
    val listen = kind == ActivityKind.LISTEN_MEANING

    TwoPane(
        wide,
        leftWeight = 1.1f,
        left = {
            T(q.instruction, 17, color = p.accent, weight = FontWeight.SemiBold)
            T(q.prompt, if (shadow) 18 else 24, color = if (shadow) p.textDim else p.text, weight = FontWeight.Medium)
            when {
                shadow -> ExpressionBlock(q.answerEn, q.answerKo)
                recall && c.revealed -> ExpressionBlock(q.answerEn, q.answerKo)
                // 무음일 때 듣기 문제는 문장을 읽고 푼다. 듣기 평가로 기록하지 않는다.
                listen && silent -> {
                    T(q.answerEn, 30, weight = FontWeight.SemiBold, serif = true)
                    T(
                        if (c.data.settings.silent) "무음이라 문장을 글자로 보여드려요." else "이 기기에서는 음원을 낼 수 없어 글자로 보여드려요.",
                        14,
                        color = p.textDim,
                    )
                }
            }
            if (c.hintShown && !(recall && c.revealed)) {
                Panel(Modifier.fillMaxWidth(), color = p.surfaceHigh) {
                    T(if (listen) "문장 · ${q.hint}" else if (recall) "힌트 · ${q.hint}" else q.hint, 17)
                }
            }
            if (audio != null && !silent && (listen || shadow || (recall && c.revealed))) {
                AudioRow(c, audio)
            }
            c.audioNote?.let { T(it, 14, color = p.retry) }
        },
        right = {
            when {
                q.choices.isNotEmpty() -> {
                    q.choices.forEach { choice ->
                        ChoiceButton(choice, englishFont = !listen) { c.choose(choice) }
                    }
                    val canHint = !c.hintShown && q.hint.isNotBlank() && !(listen && silent)
                    if (canHint) {
                        BigButton(
                            if (listen) "문장 보기" else "힌트 보기",
                            { c.showHint() },
                            Modifier.fillMaxWidth(),
                            kind = ButtonKind.OUTLINE, minHeight = 52, textSize = 17,
                        )
                    }
                }
                recall && !c.revealed -> {
                    T("먼저 소리 내어 말해 본 뒤 눌러요.", 15, color = p.textDim)
                    BigButton("정답 보기", { c.reveal() }, Modifier.fillMaxWidth(), minHeight = 76)
                    if (!c.hintShown) {
                        BigButton("힌트 보기", { c.showHint() }, Modifier.fillMaxWidth(), kind = ButtonKind.OUTLINE, minHeight = 52, textSize = 17)
                    }
                }
                recall -> {
                    T("떠올린 말과 같았나요?", 15, color = p.textDim)
                    BigButton("떠올렸어요", { c.selfCheck(true) }, Modifier.fillMaxWidth(), minHeight = 76)
                    BigButton("아직 어려워요", { c.selfCheck(false) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, minHeight = 64)
                    T("스스로 확인한 결과는 연습으로만 기록해요.", 14, color = p.textDim)
                }
                else -> {
                    BigButton(
                        if (silent) "읽어 봤어요" else "말해 봤어요",
                        { c.practiced() },
                        Modifier.fillMaxWidth(),
                        sub = "마이크를 쓰지 않아요 · 연습으로 기록",
                        minHeight = 84,
                    )
                    T(
                        if (silent) "입 모양으로 따라 읽어도 좋아요." else "듣고 나서 한 번 소리 내어 말해 봐요.",
                        15,
                        color = p.textDim,
                    )
                }
            }
        },
    )
}

@Composable
private fun ChoiceButton(choice: Choice, englishFont: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clip(shape)
            .background(p.surface)
            .border(1.5.dp, p.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        T(choice.text, 20, weight = FontWeight.Medium, serif = englishFont)
    }
}

@Composable
private fun FeedbackBody(c: AppController, session: Session, q: Question, wide: Boolean) {
    val p = LocalPalette.current
    val picked = c.picked
    val correct = picked?.correct ?: true
    val last = session.done >= session.items.size
    // 답을 고른 터치가 그대로 '다음'까지 누르지 않도록 잠깐 뒤에 활성화한다.
    var ready by remember(session.id, c.index) { mutableStateOf(false) }
    LaunchedEffect(session.id, c.index) {
        delay(350)
        ready = true
    }
    TwoPane(
        wide,
        leftWeight = 1.1f,
        left = {
            Panel(Modifier.fillMaxWidth(), color = if (correct) p.goodSoft else p.retrySoft) {
                T(
                    if (correct) "✓ 맞았어요" else "↻ 다시 익힐 표현이에요",
                    20,
                    color = if (correct) p.good else p.retry,
                    weight = FontWeight.Bold,
                )
                if (!correct && picked != null && picked.explain.isNotBlank()) T(picked.explain, 16)
            }
            ExpressionBlock(q.answerEn, q.answerKo, size = 30)
            T(q.note, 16, color = p.textDim)
            if (!correct) T("다음 학습일에 한 번 더 나와요.", 14, color = p.textDim)
            c.audioNote?.let { T(it, 14, color = p.retry) }
        },
        right = {
            BigButton(
                if (last) "마무리" else "다음",
                { c.next() },
                Modifier.fillMaxWidth(),
                enabled = ready,
                minHeight = 84,
            )
            if (!c.silent) {
                BigButton(
                    if (c.speaker.speaking) "듣는 중…" else "표현 듣기",
                    { c.listen(q.answerEn) },
                    Modifier.fillMaxWidth(),
                    kind = ButtonKind.SECONDARY, minHeight = 60,
                )
            }
        },
    )
}

@Composable
private fun PausePanel(c: AppController, session: Session) {
    val p = LocalPalette.current
    val parked = c.gate.parked
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Panel(Modifier.fillMaxWidth(0.82f)) {
            T("잠시 멈췄어요", 26, weight = FontWeight.Bold)
            T(
                when (c.pause) {
                    PauseReason.GATE -> "주차 상태가 확인되지 않아 학습을 멈췄어요."
                    PauseReason.AUDIO -> "다른 소리가 먼저 나와서 멈췄어요. 방해받은 문제는 기록하지 않았어요."
                    else -> "화면을 벗어나 있어서 멈췄어요."
                },
                17,
            )
            T(
                if (session.done > 0) "활동 ${session.done}개는 이미 저장했어요." else "아직 저장한 활동은 없어요.",
                15,
                color = p.textDim,
            )
            if (!parked) GateChip(c.gate.state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigButton("이어하기", { c.continueLesson() }, Modifier.weight(1.3f), enabled = parked)
                BigButton("여기서 마치기", { c.finishNow() }, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
            }
        }
    }
}

// ── UI-06 완료 ─────────────────────────────────────────────

/** '다음 레슨'이 아니라 '오늘은 여기까지'가 주 행동이다. 기록은 이 화면이 뜨기 전에 이미 저장되어 있다. */
@Composable
internal fun DoneScreen(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    val session = c.finishedSessionId?.let { Engine.session(c.data, it) }
    if (session == null) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            BigButton("오늘 화면으로", { c.closeDone() })
        }
        return
    }
    val complete = session.status == SessionStatus.COMPLETED
    val stats = Engine.stats(c.data, c.today)
    val bundles = session.items.take(session.done).map { it.bundleId }.distinct().mapNotNull { Content.find(it) }
    val firstEver = c.data.sessions.count { it.done > 0 } <= 1

    TwoPane(
        wide,
        Modifier.padding(horizontal = 30.dp, vertical = 24.dp),
        leftWeight = 1.2f,
        left = {
            T(
                when {
                    !complete -> "여기까지 저장했어요"
                    session.mode == Mode.EVENING -> "오늘 한마디를 다시 꺼냈어요"
                    else -> "오늘 표현을 연습했어요"
                },
                30,
                weight = FontWeight.Bold,
            )
            T(
                if (complete) "활동 ${session.done}개를 모두 마쳤어요. 여기서 마칠게요."
                else "활동 ${session.done}개를 저장했어요. 남은 활동은 다음에 이어할 수 있어요.",
                17,
                color = p.textDim,
            )
            bundles.forEach { b ->
                val m = c.data.mastery[b.id]
                Panel(Modifier.fillMaxWidth()) {
                    T(b.en, 24, weight = FontWeight.SemiBold, serif = true)
                    T(b.ko, 16, color = p.textDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (m != null) StatusTag(m)
                        BigButton(
                            if (m?.favorite == true) "★ 보관함에 표시함" else "☆ 표현 보관",
                            { c.toggleFavorite(b.id) },
                            kind = ButtonKind.OUTLINE, minHeight = 48, textSize = 16,
                        )
                    }
                }
            }
        },
        right = {
            WeekPanel(stats, Modifier.fillMaxWidth())
            T(
                when {
                    c.saveFailed -> "기록을 저장하지 못했어요."
                    firstEver -> "기록은 이 차량에만 저장돼요. 가입은 필요 없어요."
                    else -> "기록을 저장했어요."
                },
                14,
                color = p.textDim,
            )
            BigButton("오늘은 여기까지", { c.closeDone() }, Modifier.fillMaxWidth(), minHeight = 84)
            BigButton(
                "표현 보관함 보기",
                { c.closeDone(Tab.LIBRARY) },
                Modifier.fillMaxWidth(),
                kind = ButtonKind.OUTLINE, minHeight = 56, textSize = 17,
            )
        },
    )
}
