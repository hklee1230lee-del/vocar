package com.vocar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.vocar.app.domain.Dictionary
import com.vocar.app.domain.EconTerm

private const val TOPIC_ALL = "전체"

private enum class DictSort(val label: String) {
    KO("가나다순"),
    EN("ABC순"),
}

/**
 * 경제 사전. 차 안이라 키보드와 검색창은 두지 않고, 주제 칩과 정렬만으로 찾는다.
 * 용어를 누르면 쉬운 설명·예문·관련 용어가 열린다.
 */
@Composable
internal fun DictionaryScreen(c: AppController, wide: Boolean) {
    val term = c.dictionaryTerm?.let { Dictionary.find(it) }
    if (term != null) TermDetail(c, term, wide) else TermList(c, wide)
}

@Composable
private fun TermList(c: AppController, wide: Boolean) {
    val p = LocalPalette.current
    var topic by remember { mutableStateOf(TOPIC_ALL) }
    var sort by remember { mutableStateOf(DictSort.KO) }
    val shown = remember(topic, sort) {
        val base = if (topic == TOPIC_ALL) Dictionary.terms else Dictionary.byTopic(topic)
        when (sort) {
            DictSort.KO -> base.sortedBy { it.ko }
            DictSort.EN -> base.sortedBy { it.en.lowercase() }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = if (wide) 6.dp else 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T("경제 사전", 28, weight = FontWeight.Bold)
        T("뉴스에 나오는 경제 용어를 쉬운 말로 확인해요. 용어 ${Dictionary.terms.size}개", 15, color = p.textDim)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf(TOPIC_ALL) + Dictionary.topics).forEach { name ->
                val on = topic == name
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
                        if (on) "✓ $name" else name,
                        15,
                        color = if (on) p.onAccent else p.textDim,
                        weight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        Segmented(DictSort.entries.map { it to it.label }, sort, { sort = it }, Modifier.fillMaxWidth())
        val rows = if (wide) shown.chunked(2) else shown.chunked(1)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rows, key = { row -> row.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { t -> TermCard(t, Modifier.weight(1f)) { c.dictionaryTerm = t.id } }
                    if (wide && row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TermCard(t: EconTerm, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Panel(modifier, onClick = onClick) {
        Tag(t.topic)
        T(t.ko, 21, weight = FontWeight.Bold)
        T(t.en, 15, color = p.textDim)
        T(t.plain, 14, color = p.textDim, maxLines = 2)
    }
}

@Composable
private fun TermDetail(c: AppController, t: EconTerm, wide: Boolean) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 6.dp else 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BigButton("← 목록", { c.dictionaryTerm = null }, kind = ButtonKind.SECONDARY, minHeight = 48, textSize = 15)
            Tag(t.topic)
        }
        T(t.ko, 30, weight = FontWeight.Bold)
        T(t.en, 18, color = p.textDim)
        Panel(Modifier.fillMaxWidth()) {
            T("쉬운 설명", 13, color = p.textDim)
            T(t.plain, 18)
        }
        Panel(Modifier.fillMaxWidth()) {
            T("예문", 13, color = p.textDim)
            T(t.example, 19, weight = FontWeight.SemiBold)
            T(t.exampleKo, 15, color = p.textDim)
            if (!c.silent) {
                BigButton(
                    "예문 듣기", { c.listen(t.example) },
                    kind = ButtonKind.DARK, enabled = c.gate.parked, minHeight = 52, textSize = 16,
                )
            }
        }
        val related = t.related.mapNotNull { Dictionary.find(it) }
        if (related.isNotEmpty()) {
            T("관련 용어", 14, color = p.textDim)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                related.forEach { r ->
                    Box(
                        Modifier
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(50))
                            .background(p.tile)
                            .clickable(role = Role.Button, onClick = { c.openTerm(r.id) })
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { T(r.ko, 15) }
                }
            }
        }
    }
}

/** 오늘 화면의 "오늘의 경제 용어" 카드. 같은 날에는 같은 용어이고, 누르면 사전 상세가 열린다. */
@Composable
internal fun TodayTermCard(c: AppController) {
    val p = LocalPalette.current
    val t = Dictionary.termOfDay(c.today)
    Panel(Modifier.fillMaxWidth(), onClick = { c.openTerm(t.id) }) {
        T("오늘의 경제 용어", 14, color = p.textDim)
        T(t.ko, 22, weight = FontWeight.Bold)
        T(t.en, 14, color = p.textDim)
        T(t.plain, 14, color = p.textDim, maxLines = 2)
    }
}
