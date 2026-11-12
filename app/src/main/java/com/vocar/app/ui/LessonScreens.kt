package com.vocar.app.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.vocar.app.domain.ActivityKind
import com.vocar.app.domain.Choice
import com.vocar.app.domain.Content
import com.vocar.app.domain.Engine
import com.vocar.app.domain.Mode
import com.vocar.app.domain.Question
import com.vocar.app.domain.Session
import com.vocar.app.domain.SessionStatus
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
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepDots(session.items.size, session.done, c.index)
                T(
                    "${modeLabel(session.mode)} ${session.length.minutes}분 · ${c.index + 1}/${session.items.size}",
                    13,
                    color = p.textDim,
                )
            }
            BigButton("끝내기", { c.finishNow() }, kind = ButtonKind.SECONDARY, minHeight = 50, textSize = 16)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // 문제가 바뀌거나 답을 확정할 때마다 내용이 아래에서 올라오며 바뀐다.
            Appear(Triple(c.index, c.phase, c.pause)) {
                when {
                    c.pause != null -> PausePanel(c, session)
                    c.phase == Phase.ASK -> AskBody(c, question, wide)
                    else -> FeedbackBody(c, session, question, wide)
                }
            }
        }
    }
}

/** 검정 카드에 담은 오늘의 표현. 음원을 낼 수 있으면 재생 버튼과 파형을 함께 둔다. */
@Composable
private fun ExpressionHero(c: AppController, en: String?, ko: String?, audio: String?) {
    val p = LocalPalette.current
    HeroCard(Modifier.fillMaxWidth()) {
        if (en != null) T(en, 30, color = p.onHero, weight = FontWeight.Bold)
        if (ko != null) T(ko, 16, color = p.onHeroDim)
        if (audio != null) {
            if (en != null) Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val speaking = c.speaker.speaking
                RoundButton(
                    if (speaking) "■" else "▶",
                    if (speaking) "멈추기" else "다시 듣기",
                    { if (speaking) c.stopAudio() else c.listen(audio) },
                )
                Waveform(speaking, Modifier.weight(1f))
                BigButton("천천히", { c.listen(audio, slow = true) }, kind = ButtonKind.SECONDARY, minHeight = 44, textSize = 14)
            }
        }
    }
}

@Composable
private fun Instruction(text: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(p.accentBrush))
        T(text, 16, color = p.textDim, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun AskBody(c: AppController, q: Question, wide: Boolean) {
    val p = LocalPalette.current
    val kind = q.item.kind
    val silent = c.silent
    val audio = q.audioText?.takeIf { !silent }
    val shadow = kind == ActivityKind.SHADOW || kind == ActivityKind.SHADOW_RECAP
    val recall = kind == ActivityKind.RECALL
    val listen = kind == ActivityKind.LISTEN_MEANING

    TwoPane(
        wide,
        leftWeight = 1.1f,
        left = {
            Instruction(q.instruction)
            T(q.prompt, if (shadow) 18 else 24, color = if (shadow) p.textDim else p.text, weight = FontWeight.Bold)
            when {
                shadow -> ExpressionHero(c, q.answerEn, q.answerKo, audio)
                recall && c.revealed -> ExpressionHero(c, q.answerEn, q.answerKo, audio)
                // 듣기 문제: 소리가 나면 재생 카드만, 무음이면 문장을 글자로 보여준다(듣기 평가로 기록하지 않는다).
                listen && !silent -> ExpressionHero(c, null, null, audio)
                listen -> {
                    ExpressionHero(c, q.answerEn, null, null)
                    T(
                        if (c.data.settings.silent) "무음이라 문장을 글자로 보여드려요." else "이 기기에서는 음원을 낼 수 없어 글자로 보여드려요.",
                        13,
                        color = p.textDim,
                    )
                }
            }
            if (c.hintShown && !(recall && c.revealed)) {
                Panel(Modifier.fillMaxWidth()) {
                    T(if (listen) "문장 · ${q.hint}" else if (recall) "힌트 · ${q.hint}" else q.hint, 16, weight = FontWeight.SemiBold)
                }
            }
            c.audioNote?.let { T(it, 13, color = p.retry) }
        },
        right = {
            when {
                q.choices.isNotEmpty() -> {
                    Panel(Modifier.fillMaxWidth()) {
                        q.choices.forEach { choice -> ChoiceButton(choice) { c.choose(choice) } }
                    }
                    val canHint = !c.hintShown && q.hint.isNotBlank() && !(listen && silent)
                    if (canHint) {
                        BigButton(
                            if (listen) "문장 보기" else "힌트 보기",
                            { c.showHint() },
                            Modifier.fillMaxWidth(),
                            kind = ButtonKind.OUTLINE, minHeight = 50, textSize = 16,
                        )
                    }
                }
                recall && !c.revealed -> {
                    T("먼저 소리 내어 말해 본 뒤 눌러요.", 14, color = p.textDim)
                    BigButton("정답 보기", { c.reveal() }, Modifier.fillMaxWidth(), minHeight = 74)
                    if (!c.hintShown) {
                        BigButton("힌트 보기", { c.showHint() }, Modifier.fillMaxWidth(), kind = ButtonKind.OUTLINE, minHeight = 50, textSize = 16)
                    }
                }
                recall -> {
                    T("떠올린 말과 같았나요?", 14, color = p.textDim)
                    BigButton("떠올렸어요", { c.selfCheck(true) }, Modifier.fillMaxWidth(), minHeight = 74)
                    BigButton("아직 어려워요", { c.selfCheck(false) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, minHeight = 62)
                    T("스스로 확인한 결과는 연습으로만 기록해요.", 13, color = p.textDim)
                }
                else -> {
                    BigButton(
                        if (silent) "읽어 봤어요" else "말해 봤어요",
                        { c.practiced() },
                        Modifier.fillMaxWidth(),
                        sub = "마이크를 쓰지 않아요 · 연습으로 기록",
                        minHeight = 82,
                    )
                    T(
                        if (silent) "입 모양으로 따라 읽어도 좋아요." else "듣고 나서 한 번 소리 내어 말해 봐요.",
                        14,
                        color = p.textDim,
                    )
                }
            }
        },
    )
}

/** 흰 카드 한 줄 + 왼쪽의 빈 동그라미. 누르면 바로 제출된다. */
@Composable
private fun ChoiceButton(choice: Choice, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clip(shape)
            .background(p.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, p.line, CircleShape))
        T(choice.text, 19, weight = FontWeight.SemiBold)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (correct) {
                    CheckDot(true, 34)
                } else {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(p.retrySoft), contentAlignment = Alignment.Center) {
                        T("↻", 18, color = p.retry, weight = FontWeight.Bold)
                    }
                }
                T(if (correct) "맞았어요" else "다시 익힐 표현이에요", 24, weight = FontWeight.Bold)
            }
            if (!correct && picked != null && picked.explain.isNotBlank()) {
                Panel(Modifier.fillMaxWidth(), color = p.retrySoft) { T(picked.explain, 15, color = p.retry, weight = FontWeight.SemiBold) }
            }
            ExpressionHero(c, q.answerEn, q.answerKo, q.answerEn.takeIf { !c.silent })
            T(q.note, 15, color = p.textDim)
            if (!correct) T("다음 학습일에 한 번 더 나와요.", 13, color = p.textDim)
            c.audioNote?.let { T(it, 13, color = p.retry) }
        },
        right = {
            BigButton(
                if (last) "마무리" else "다음",
                { c.next() },
                Modifier.fillMaxWidth(),
                kind = ButtonKind.DARK,
                enabled = ready,
                minHeight = 82,
            )
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
                16,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckDot(session.done > 0, 20)
                T(
                    if (session.done > 0) "활동 ${session.done}개는 이미 저장했어요." else "아직 저장한 활동은 없어요.",
                    14,
                    color = p.textDim,
                )
            }
            if (!parked) GateChip(c.gate.state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
        leftWeight = 1.2f,
        left = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CheckDot(true, 36)
                T(
                    when {
                        !complete -> "여기까지 저장했어요"
                        session.mode == Mode.EVENING -> "오늘 한마디를 다시 꺼냈어요"
                        else -> "오늘 표현을 연습했어요"
                    },
                    28,
                    weight = FontWeight.Bold,
                )
            }
            T(
                if (complete) "활동 ${session.done}개를 모두 마쳤어요. 여기서 마칠게요."
                else "활동 ${session.done}개를 저장했어요. 남은 활동은 다음에 이어할 수 있어요.",
                16,
                color = p.textDim,
            )
            bundles.forEach { b ->
                val m = c.data.mastery[b.id]
                Panel(Modifier.fillMaxWidth()) {
                    T(b.en, 23, weight = FontWeight.Bold)
                    T(b.ko, 15, color = p.textDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (m != null) StatusTag(m)
                        BigButton(
                            if (m?.favorite == true) "★ 보관함에 표시함" else "☆ 표현 보관",
                            { c.toggleFavorite(b.id) },
                            kind = ButtonKind.SECONDARY, minHeight = 46, textSize = 15,
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
                13,
                color = p.textDim,
            )
            BigButton("오늘은 여기까지", { c.closeDone() }, Modifier.fillMaxWidth(), minHeight = 82)
            BigButton(
                "표현 보관함 보기",
                { c.closeDone(Tab.LIBRARY) },
                Modifier.fillMaxWidth(),
                kind = ButtonKind.SECONDARY, minHeight = 54, textSize = 16,
            )
        },
    )
}
