package com.vocar.app.domain

/**
 * 경제 상식 문제 하나. 차 안에서 고르기 쉽도록 보기는 3개로 고정한다.
 * [termId]는 사전의 개념을 가리키고, 틀리면 그 개념이 "다시 볼 개념"에 들어간다.
 * 해설에 붙일 그림은 [diagram]이 없으면 개념의 그림을 쓴다.
 */
data class EconQuestion(
    val id: String,
    val termId: String,
    val prompt: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
    val diagram: Diagram? = null,
) {
    val term: EconTerm? get() = Dictionary.find(termId)
    val shownDiagram: Diagram? get() = diagram ?: term?.diagram
}

/**
 * 오늘의 경제 상식 퀘스트: 하루 [PER_DAY]문제. 같은 날에는 언제 열어도 같은 문제이고,
 * 날이 바뀌면 다음 문제 묶음으로 넘어간다. 문제는 형식만 신문사 경제 상식 시험을 참고했고 내용은 직접 만들었다.
 */
object EconQuiz {
    const val PER_DAY = 3

    private fun q(id: String, term: String, prompt: String, choices: List<String>, answer: Int, explanation: String, diagram: Diagram? = null) =
        EconQuestion(id, term, prompt, choices, answer, explanation, diagram)

    val bank: List<EconQuestion> = listOf(
        q(
            "q-rate-hike-effect", "rate-hike",
            "중앙은행이 기준금리를 올리면 일반적으로 어떤 일이 생길까요?",
            listOf("대출 이자 부담이 커진다", "물가가 곧바로 크게 오른다", "예금 이자가 줄어든다"), 0,
            "금리가 오르면 돈을 빌린 사람이 내야 할 이자가 늘어요. 그래서 소비와 투자가 줄고, 물가가 진정되는 쪽으로 움직여요. 예금 이자는 보통 함께 올라요.",
        ),
        q(
            "q-inflation-def", "inflation",
            "물건과 서비스 값이 전체적으로 계속 오르는 현상을 무엇이라고 할까요?",
            listOf("디플레이션", "인플레이션", "경기 침체"), 1,
            "인플레이션은 물가가 전체적으로 꾸준히 오르는 현상이에요. 반대로 계속 내리는 것은 디플레이션이에요.",
        ),
        q(
            "q-purchasing-power", "purchasing-power",
            "물가가 오르면 내가 가진 돈의 구매력은 어떻게 될까요?",
            listOf("늘어난다", "변하지 않는다", "줄어든다"), 2,
            "같은 돈으로 살 수 있는 물건의 양이 구매력이에요. 값이 오르면 같은 돈으로 살 수 있는 양이 줄어요.",
        ),
        q(
            "q-supply-demand", "price",
            "사려는 사람은 그대로인데 물건이 갑자기 적어지면 가격은 어떻게 움직이기 쉬울까요?",
            listOf("오르기 쉽다", "내리기 쉽다", "전혀 변하지 않는다"), 0,
            "공급이 줄었는데 수요가 그대로면, 물건을 사려는 경쟁이 생겨 가격이 오르기 쉬워요.",
        ),
        q(
            "q-weak-won", "exchange-rate",
            "원/달러 환율이 오르면(원화 약세) 수출 기업에는 대체로 어떤 영향이 있을까요?",
            listOf("해외에서 우리 물건값이 비싸져 불리하다", "해외에서 우리 물건값이 싸져 유리하다", "아무 영향이 없다"), 1,
            "환율이 오르면 같은 달러로 원화를 더 많이 바꿀 수 있어요. 해외에서 우리 물건이 상대적으로 싸져 수출에 유리하고, 대신 수입품은 비싸져요.",
        ),
        q(
            "q-trade-deficit", "trade-deficit",
            "일정 기간 수입한 금액이 수출한 금액보다 많은 상태는?",
            listOf("무역 흑자", "재정 적자", "무역 적자"), 2,
            "수입이 수출보다 많으면 무역 적자, 수출이 더 많으면 무역 흑자예요. 재정 적자는 정부의 지출이 수입보다 많은 상태예요.",
        ),
        q(
            "q-tariff", "tariff",
            "수입품이 국경을 넘어 들어올 때 나라가 걷는 세금은?",
            listOf("관세", "부가가치세", "소득세"), 0,
            "관세는 수입품에 붙는 세금이에요. 관세가 붙으면 수입품 값이 오르고 수입이 줄어드는 쪽으로 움직여요.",
        ),
        q(
            "q-profit", "profit",
            "회사의 이익을 바르게 설명한 것은?",
            listOf("물건을 팔아 번 돈 전체", "번 돈에서 쓴 돈을 뺀 나머지", "은행에서 빌린 돈"), 1,
            "물건을 팔아 번 돈 전체는 매출이에요. 매출에서 재료비·월급 같은 비용을 빼고 남는 것이 이익이에요.",
        ),
        q(
            "q-earnings", "earnings",
            "뉴스에서 '실적 발표'라고 할 때 실적은 무엇을 뜻할까요?",
            listOf("회사의 직원 수", "회사의 주가 목표", "회사가 일정 기간 벌고 남긴 성적표"), 2,
            "실적은 회사가 정해진 기간 동안 얼마나 팔고(매출) 얼마나 남겼는지(이익)를 보여 주는 성적표예요.",
        ),
        q(
            "q-dividend", "dividend",
            "회사가 이익의 일부를 주주에게 나누어 주는 돈은?",
            listOf("배당", "이자", "보조금"), 0,
            "배당은 회사의 주인인 주주에게 이익을 나누어 주는 돈이에요. 이자는 돈을 빌려준 대가로 받는 돈이에요.",
        ),
        q(
            "q-gdp", "gdp",
            "한 나라 안에서 일정 기간 새로 만들어진 물건과 서비스의 가치를 모두 더한 값은?",
            listOf("물가 지수", "국내총생산(GDP)", "무역 수지"), 1,
            "GDP는 나라 안에서 새로 만들어진 가치를 모두 더한 값으로, 경제의 크기를 재는 대표 지표예요.",
        ),
        q(
            "q-recession", "recession",
            "경제 활동이 한동안 눈에 띄게 줄어드는 시기를 무엇이라고 할까요?",
            listOf("호황", "인플레이션", "경기 침체"), 2,
            "경기 침체 때는 생산과 소비가 줄고 일자리도 줄기 쉬워요. 경기는 호황과 침체를 오가며 순환해요.",
        ),
        q(
            "q-cycle", "economic-growth",
            "경기 순환에서 '저점'을 지난 다음에 오는 단계는?",
            listOf("회복", "후퇴", "정점"), 0,
            "경기는 호황(정점) → 후퇴 → 불황(저점) → 회복을 되풀이해요. 저점을 지나면 회복이 시작돼요.",
        ),
        q(
            "q-unemployment", "unemployment",
            "실업 상태를 바르게 설명한 것은?",
            listOf("일할 생각이 없어 쉬는 상태", "일할 의사와 능력이 있는데 일자리를 못 구한 상태", "학교에 다니는 상태"), 1,
            "실업은 일하고 싶고 일할 수 있는데도 일자리를 구하지 못한 상태예요. 일할 생각이 없는 사람은 실업자로 세지 않아요.",
        ),
        q(
            "q-bond-rate", "bond",
            "금리가 오르면 이미 발행된 채권의 가격은 대체로 어떻게 될까요?",
            listOf("오른다", "변하지 않는다", "내린다"), 2,
            "새로 나오는 채권이 더 높은 이자를 주면, 이자가 낮은 예전 채권은 인기가 떨어져 값이 내려가요. 금리와 채권 가격은 반대로 움직여요.",
        ),
        q(
            "q-stock", "stock",
            "주식을 산다는 것은 무엇을 뜻할까요?",
            listOf("회사의 주인 자격 일부를 갖는 것", "회사에 돈을 빌려주는 것", "회사 물건을 미리 사 두는 것"), 0,
            "주식은 회사의 주인 자격을 잘게 나눈 것이에요. 돈을 빌려주고 이자를 받는 약속은 채권이에요.",
        ),
        q(
            "q-central-bank", "central-bank",
            "'은행의 은행'이라고도 불리며 나라의 기준금리를 정하는 곳은?",
            listOf("증권사", "중앙은행", "보험사"), 1,
            "중앙은행은 돈의 양과 기준금리를 조절해 물가와 경기를 안정시키는 일을 해요.",
        ),
        q(
            "q-rate-cut", "rate-cut",
            "경기가 나빠질 때 중앙은행이 경기를 살리려고 흔히 쓰는 방법은?",
            listOf("금리 인상", "세금 인상", "금리 인하"), 2,
            "금리를 내리면 돈을 빌리기 쉬워져 소비와 투자가 늘고, 경기가 살아나는 쪽으로 움직여요.",
        ),
        q(
            "q-deflation", "deflation",
            "물가가 계속 내려갈 때 생길 수 있는 문제는?",
            listOf("사람들이 '더 싸지겠지' 하며 소비를 미룬다", "모든 회사의 이익이 늘어난다", "실업이 반드시 사라진다"), 0,
            "물가가 계속 내리면 값이 더 내릴 거라 기대하며 소비를 미루고, 회사 매출이 줄어 경기가 더 나빠질 수 있어요.",
        ),
        q(
            "q-loan", "loan",
            "대출을 받을 때 반드시 함께 갚아야 하는 것은?",
            listOf("배당", "이자", "관세"), 1,
            "대출은 돈을 빌리는 것이고, 빌린 대가로 이자를 더해 갚아야 해요.",
        ),
        q(
            "q-budget", "budget",
            "들어올 돈과 쓸 돈을 미리 계획해 적어 둔 것은?",
            listOf("결산", "매출", "예산"), 2,
            "예산은 미리 세우는 계획이고, 결산은 실제로 쓰고 번 돈을 나중에 정리하는 거예요.",
        ),
        q(
            "q-tax", "tax",
            "세금에 대한 설명으로 알맞은 것은?",
            listOf("공공 서비스를 위해 나라와 지방 정부가 걷는 돈", "회사만 내는 돈", "은행이 주는 이자"), 0,
            "세금은 도로·학교·안전 같은 공공 서비스를 위해 국민과 회사로부터 걷는 돈이에요.",
        ),
        q(
            "q-demand", "demand",
            "겨울이 되면 따뜻한 외투를 사려는 사람이 늘어요. 이것은 무엇이 늘어난 것일까요?",
            listOf("공급", "수요", "비용"), 1,
            "사려는 마음과 양이 수요예요. 수요가 늘면 같은 공급에서 가격이 오르기 쉬워요.",
        ),
        q(
            "q-monetary", "monetary-policy",
            "중앙은행이 금리와 돈의 양을 조절해 물가와 경기를 안정시키려는 정책은?",
            listOf("재정 정책", "무역 정책", "통화 정책"), 2,
            "통화 정책은 중앙은행이 금리와 돈의 양으로 펼치는 정책이에요. 정부가 세금과 지출로 하는 정책은 재정 정책이에요.",
        ),
    )

    private val byId = bank.associateBy { it.id }

    fun find(id: String): EconQuestion? = byId[id]

    /** 그날의 문제 [PER_DAY]개. 문제 은행을 차례로 돌며, 날이 바뀌면 다음 묶음이 나온다. */
    fun questsFor(epochDay: Long): List<EconQuestion> {
        val n = bank.size
        val start = Math.floorMod(epochDay * PER_DAY, n.toLong()).toInt()
        return (0 until PER_DAY).map { bank[(start + it) % n] }
    }
}

/**
 * 오늘의 퀘스트 진행. [picks]는 그날 문제 순서대로 고른 보기 번호이고,
 * [review]는 틀린 개념 id(최근 것이 앞)로 날이 바뀌어도 남는다.
 */
data class EconProgress(
    val day: Long,
    val picks: List<Int>,
    val review: List<String>,
) {
    fun forDay(epochDay: Long): EconProgress = if (day == epochDay) this else copy(day = epochDay, picks = emptyList())

    fun questions(): List<EconQuestion> = EconQuiz.questsFor(day)

    val done: Boolean get() = picks.size >= EconQuiz.PER_DAY

    val correct: Int get() = questions().zip(picks).count { (q, p) -> q.answer == p }

    /** 오늘 문제에 답한다. 이미 끝났거나 보기 번호가 틀리면 그대로 둔다. */
    fun answer(choice: Int): EconProgress {
        if (done) return this
        val q = questions()[picks.size]
        if (choice !in q.choices.indices) return this
        val nextReview = if (choice == q.answer) {
            review
        } else {
            (listOf(q.termId) + review.filter { it != q.termId }).take(MAX_REVIEW)
        }
        return copy(picks = picks + choice, review = nextReview)
    }

    /** 사전에서 다시 확인한 개념을 목록에서 뺀다. */
    fun reviewed(termId: String): EconProgress = copy(review = review.filter { it != termId })

    companion object {
        const val MAX_REVIEW = 20
        fun empty(epochDay: Long) = EconProgress(epochDay, emptyList(), emptyList())
    }
}
