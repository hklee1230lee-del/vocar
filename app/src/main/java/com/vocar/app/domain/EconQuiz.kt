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
 * 주제별 심화 퀴즈 팩. 결제로 막지 않고 누구나 무료로 풀어 볼 수 있다.
 */
data class EconQuizPack(
    val id: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val questions: List<EconQuestion>,
)

/**
 * 오늘의 경제 상식 퀘스트(하루 [PER_DAY]문제) + 심화 퀴즈 팩(사회초년생 패키지 등).
 * 같은 날에는 언제 열어도 같은 문제이고, 날이 바뀌면 다음 문제 묶음으로 넘어간다.
 */
object EconQuiz {
    const val PER_DAY = 3

    private fun q(
        id: String,
        term: String,
        prompt: String,
        choices: List<String>,
        answer: Int,
        explanation: String,
        diagram: Diagram? = null,
    ) = EconQuestion(id, term, prompt, choices, answer, explanation, diagram)

    /** 기본 문제 은행 (24문항) */
    val coreBank: List<EconQuestion> = listOf(
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
            Diagrams.SUPPLY_DOWN,
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
            Diagrams.DEMAND_UP,
        ),
        q(
            "q-monetary", "monetary-policy",
            "중앙은행이 금리와 돈의 양을 조절해 물가와 경기를 안정시키려는 정책은?",
            listOf("재정 정책", "무역 정책", "통화 정책"), 2,
            "통화 정책은 중앙은행이 금리와 돈의 양으로 펼치는 정책이에요. 정부가 세금과 지출로 하는 정책은 재정 정책이에요.",
        ),
    )

    /** 심화 퀴즈 팩 1: 사회초년생 패키지 (6문항) */
    val starterPack = EconQuizPack(
        id = "pack-starter",
        icon = "💼",
        title = "사회초년생 패키지",
        subtitle = "첫 월급·세금·예산·월세·대출·비상금 필수 상식",
        questions = listOf(
            q(
                "sq-starter-income", "income",
                "근로계약서의 연봉(세전 소득)과 통장에 실제로 찍히는 월급(실수령액)이 다른 가장 큰 이유는?",
                listOf("세금과 사회보험료를 먼저 떼고 주기 때문", "은행이 계좌 수수료를 가져가기 때문", "물가가 매달 바뀌기 때문"), 0,
                "급여를 받을 때는 소득세와 국민연금·건강보험 같은 사회보험료를 원천징수(미리 떼어 납부)한 뒤 나머지를 받아요.",
            ),
            q(
                "sq-starter-budget", "budget",
                "사회초년생이 첫 월급을 관리할 때 가장 먼저 세우면 좋은 습관은?",
                listOf("남는 돈이 생기면 그때 저축하기", "고정 지출과 저축·생활비 예산을 미리 나누기", "한꺼번에 한 종목에 모두 투자하기"), 1,
                "월세·통신비 같은 고정 지출과 저축할 금액을 예산으로 먼저 나누어 두면 과소비를 막고 꾸준히 돈을 모을 수 있어요.",
            ),
            q(
                "sq-starter-savings", "savings",
                "갑자기 아플 때나 이직 공백기처럼 예상치 못한 지출에 대비해 언제든 꺼내 쓸 수 있게 모아 두는 돈은?",
                listOf("관세", "배당금", "비상 예비 자금(비상금 저축)"), 2,
                "예기치 못한 상황에 대출을 받지 않도록 몇 달 치 생활비를 안전한 저축 통장에 비상금으로 마련해 두는 것이 좋아요.",
            ),
            q(
                "sq-starter-rent", "rent",
                "집을 구할 때 보증금 외에 매달 집주인에게 정해진 날 내는 주거 비용은?",
                listOf("월세", "배당", "관세"), 0,
                "월세는 매달 빠져나가는 대표적인 고정 생활비예요. 내 소득에서 주거비가 차지하는 비중을 살펴보고 결정해야 해요.",
            ),
            q(
                "sq-starter-loan", "loan",
                "전세 자금이나 학자금 대출을 받을 때 이자 부담을 줄이려면 무엇을 가장 먼저 비교해야 할까요?",
                listOf("은행 건물의 크기", "대출 금리와 상환 조건", "은행 직원의 수"), 1,
                "같은 금액을 빌려도 금리(이자율)와 상환 방식에 따라 갚아야 할 총이자가 크게 달라져요.",
                Diagrams.RATE_HIKE_FLOW,
            ),
            q(
                "sq-starter-real-wage", "purchasing-power",
                "내 월급이 조금 올랐는데도 장바구니 물가와 생활비가 더 크게 올랐다면 실제 살림살이는 어떻게 될까요?",
                listOf("살 수 있는 양이 크게 늘어난다", "아무 변화가 없다", "실제 구매력은 오히려 줄어든다"), 2,
                "명목 소득이 올라도 물가가 그보다 더 빨리 오르면 같은 월급으로 살 수 있는 물건의 양(실질 구매력)은 줄어들어요.",
                Diagrams.PURCHASING_POWER,
            ),
        ),
    )

    /** 심화 퀴즈 팩 2: 금리·환율 심화 팩 (6문항) */
    val macroPack = EconQuizPack(
        id = "pack-macro",
        icon = "📈",
        title = "금리·환율 심화 팩",
        subtitle = "기준금리·채권 시소·환율과 수출입·물가 흐름 읽기",
        questions = listOf(
            q(
                "sq-macro-real-rate", "interest-rate",
                "예금 금리보다 물가 상승률이 더 높을 때, 예금에 넣어 둔 돈의 실제 가치는 어떻게 될까요?",
                listOf("이자를 받아도 실제 구매력은 줄어든다", "구매력이 두 배로 늘어난다", "물가와 상관없이 가치가 늘어난다"), 0,
                "명목 금리에서 물가 상승률을 뺀 값을 실질 금리라고 해요. 물가가 이자보다 더 많이 오르면 돈의 실제 가치는 줄어들어요.",
                Diagrams.PURCHASING_POWER,
            ),
            q(
                "sq-macro-bond-cut", "bond",
                "반대로 시중 금리가 내려가면, 예전에 높은 고정 이자를 주기로 하고 발행된 채권의 인기와 가격은 어떻게 될까요?",
                listOf("인기가 떨어져 가격이 내린다", "인기가 높아져 채권 가격이 오른다", "채권이 주식으로 바뀐다"), 1,
                "새로 나오는 채권 이자가 낮아지면, 예전에 높은 이자를 약속한 채권의 가치가 올라가서 채권 가격이 상승해요.",
                Diagrams.RATE_VS_BOND,
            ),
            q(
                "sq-macro-import-price", "import",
                "원/달러 환율이 크게 오르면(원화 가치 하락) 원유나 원자재를 해외에서 사 오는 수입 기업의 부담은?",
                listOf("같은 달러를 갚는 데 필요한 원화가 줄어든다", "전혀 달라지지 않는다", "같은 물건을 들여오는 데 더 많은 원화가 든다"), 2,
                "환율이 오르면 해외에 달러로 대금을 치를 때 더 많은 원화가 필요해요. 그래서 수입 물가와 국내 물가도 오르기 쉬워요.",
                Diagrams.WEAK_WON_FLOW,
            ),
            q(
                "sq-macro-stagflation", "recession",
                "경기가 침체되어 일자리가 줄어드는데 물가는 오히려 오르는 까다로운 상황을 가리키는 현상은?",
                listOf("불황 속 물가 상승(스태그플레이션)", "호황 속 물가 안정", "무역 흑자 확대"), 0,
                "보통 경기가 나쁘면 물가가 내리지만, 원자재 값이 급등해 공급이 줄면 경기 침체와 물가 상승이 동시에 나타날 수 있어요.",
                Diagrams.SUPPLY_DOWN,
            ),
            q(
                "sq-macro-tightening", "monetary-policy",
                "물가가 너무 빠르게 오를 때 중앙은행이 시중의 돈줄을 죄어 물가를 잡으려는 통화 정책 방향은?",
                listOf("완화 정책(금리 인하)", "긴축 정책(금리 인상)", "관세 폐지"), 1,
                "물가 과열을 식히기 위해 기준금리를 올리고 시중 통화량을 줄이는 것을 긴축 통화 정책이라고 해요.",
                Diagrams.RATE_HIKE_FLOW,
            ),
            q(
                "sq-macro-tariff-consumer", "tariff",
                "특정 외국산 물건에 높은 관세를 매기면 국내 소비자에게 나타날 수 있는 영향은?",
                listOf("수입품 가격이 무조건 내려간다", "세금이 모두 사라진다", "수입품과 관련 제품 가격이 올라 선택 폭이 좁아질 수 있다"), 2,
                "관세는 국내 산업을 보호하는 효과가 있지만, 수입품 가격을 올려 소비자의 물가 부담을 키울 수도 있어요.",
                Diagrams.TARIFF_FLOW,
            ),
        ),
    )

    /** 심화 퀴즈 팩 3: 기업·시장 심화 팩 (6문항) */
    val marketPack = EconQuizPack(
        id = "pack-market",
        icon = "🏢",
        title = "기업·시장 심화 팩",
        subtitle = "매출과 이익률·배당·주식과 채권·경기 순환 심화",
        questions = listOf(
            q(
                "sq-market-margin", "earnings",
                "두 회사의 매출이 같아도 실제 남긴 이익이 크게 다른 이유는 무엇 때문일까요?",
                listOf("원가·인건비·임대료 같은 비용 구조가 다르기 때문", "회사 이름의 글자 수가 다르기 때문", "주주 수가 같기 때문"), 0,
                "매출이 아무리 커도 비용이 많이 들면 남는 이익이 적어요. 실적 뉴스를 볼 때는 매출과 함께 영업이익을 꼭 함께 봐야 해요.",
                Diagrams.PROFIT_FLOW,
            ),
            q(
                "sq-market-cost-up", "cost",
                "제품 판매 가격은 그대로인데 원재료비와 전기료 같은 생산 비용이 크게 오르면 기업의 수익성은?",
                listOf("이익이 늘어난다", "이익이 줄어들거나 손실이 날 수 있다", "매출이 자동으로 두 배가 된다"), 1,
                "번 돈(매출)이 그대로인데 쓴 돈(비용)이 늘어나면 그만큼 남는 돈(이익)이 줄어들어요.",
                Diagrams.PROFIT_FLOW,
            ),
            q(
                "sq-market-stock-vs-bond", "stock",
                "회사가 큰 이익을 냈을 때 주주와 채권 보유자가 받는 보상의 차이를 바르게 설명한 것은?",
                listOf("채권 보유자만 배당금을 받는다", "주주는 정해진 이자만 받고 배당은 못 받는다", "채권은 약속된 이자를 받고 주주는 실적에 따라 배당과 주가 상승을 기대한다"), 2,
                "채권은 빌려준 돈이라 정해진 이자를 받는 대신 안정적이고, 주식은 주인 자격이라 실적에 따라 배당이나 가치가 크게 바뀔 수 있어요.",
            ),
            q(
                "sq-market-supply-up", "supply",
                "기술이 발전해 공장에서 물건을 훨씬 싸고 많이 만들 수 있게 되면(공급 증가) 시장 가격은 보통 어떻게 될까요?",
                listOf("내려가기 쉽다", "무조건 치솟는다", "거래가 완전히 멈춘다"), 0,
                "수요가 그대로인데 시장에 나오는 물건(공급)이 늘어나면 가격은 내려가고 거래되는 양은 늘어나기 쉬워요.",
                Diagrams.SUPPLY_DEMAND,
            ),
            q(
                "sq-market-boom-risk", "economic-growth",
                "경기가 아주 좋은 호황(정점) 시기에 흔히 함께 주의해야 하는 경제 현상은?",
                listOf("실업률 급등과 디플레이션", "수요 과열로 인한 물가 상승(인플레이션) 압력", "모든 생산 활동의 중단"), 1,
                "경기가 빠르게 달아오르면 소비와 투자가 크게 늘어 물가가 함께 오르는 과열이 생기기 쉬워요.",
                Diagrams.BUSINESS_CYCLE,
            ),
            q(
                "sq-market-startup", "startup",
                "초기 스타트업이 당장 이익을 내지 못하더라도 투자자들이 자금을 투자하는 주된 이유는?",
                listOf("정해진 예금 이자를 보장받기 위해서", "세금을 대신 내 주기 위해서", "앞으로 시장이 커질 때 회사가 빠르게 성장할 가능성을 보고"), 2,
                "스타트업은 초기 개발비와 마케팅 비용 때문에 손실이 나기 쉽지만, 미래 성장 가치를 보고 투자를 유치해요.",
            ),
        ),
    )

    /** 심화 퀴즈 팩 전체 목록 (3종 × 6문항 = 18문항) */
    val packs: List<EconQuizPack> = listOf(starterPack, macroPack, marketPack)

    /** 전체 문제 은행 (기본 24문항 + 심화 18문항 = 42문항, 3의 배수) */
    val bank: List<EconQuestion> = coreBank + packs.flatMap { it.questions }

    private val byId = bank.associateBy { it.id }
    private val packById = packs.associateBy { it.id }

    fun find(id: String): EconQuestion? = byId[id]

    fun findPack(id: String): EconQuizPack? = packById[id]

    /** 그날의 문제 [PER_DAY]개. 문제 은행을 차례로 돌며, 날이 바뀌면 다음 묶음이 나온다. */
    fun questsFor(epochDay: Long): List<EconQuestion> {
        val n = bank.size
        val start = Math.floorMod(epochDay * PER_DAY, n.toLong()).toInt()
        return (0 until PER_DAY).map { bank[(start + it) % n] }
    }
}

/**
 * 오늘의 퀘스트 진행과 매일 하는 습관·배지·심화 팩·사전 열람 기록.
 * - [picks]: 그날 문제 순서대로 고른 보기 번호(날이 바뀌면 비운다)
 * - [review]: 틀린 개념 id(최근 것이 앞). 날이 바뀌어도 남는다
 * - [streak]: 연속으로 퀘스트를 끝낸 날 수, [lastDone]: 마지막으로 끝낸 날
 * - [freezes]: 연속 보호권. 하루를 놓쳐도 다음 날 끝내면 연속이 이어진다(7일 연속마다 1개 무료 지급 + 추가 구매 가능)
 * - [newBadges]: 방금 받은 새 배지 id(축하 표시용)
 * - [solvedPackQuestions]: 심화 퀴즈 팩에서 맞힌 문제 id 목록
 * - [seenTerms]: 경제 사전에서 열어 본 개념 id 목록
 * - [purchasedFreezes]: 앱 내 결제로 추가 구매한 연속 보호권 누적 수
 */
data class EconProgress(
    val day: Long,
    val picks: List<Int>,
    val review: List<String>,
    val streak: Int = 0,
    val best: Int = 0,
    val lastDone: Long = NEVER,
    val totalDays: Int = 0,
    val perfectDays: Int = 0,
    val freezes: Int = 0,
    val newBadges: List<String> = emptyList(),
    val solvedPackQuestions: List<String> = emptyList(),
    val seenTerms: List<String> = emptyList(),
    val purchasedFreezes: Int = 0,
) {
    fun forDay(epochDay: Long): EconProgress =
        if (day == epochDay) this else copy(day = epochDay, picks = emptyList(), newBadges = emptyList())

    fun questions(): List<EconQuestion> = EconQuiz.questsFor(day)

    val done: Boolean get() = picks.size >= EconQuiz.PER_DAY

    val correct: Int get() = questions().zip(picks).count { (q, p) -> q.answer == p }

    /** 지금 보여 줄 연속 일수. 어제까지 이어졌거나, 하루를 놓쳤어도 보호권이 있으면 아직 살아 있다. */
    fun currentStreak(today: Long): Int = when {
        lastDone == today || lastDone == today - 1 -> streak
        lastDone == today - 2 && freezes > 0 -> streak
        else -> 0
    }

    /** 오늘이 몇 번째 학습일인지(오늘 끝냈으면 그날 포함). */
    fun dayNumber(): Int = if (done) totalDays else totalDays + 1

    /** 오늘 문제에 답한다. 마지막 문제에 답하면 연속 기록과 배지를 갱신한다. */
    fun answer(choice: Int): EconProgress {
        if (done) return this
        val q = questions()[picks.size]
        if (choice !in q.choices.indices) return this
        val nextReview = if (choice == q.answer) {
            review
        } else {
            (listOf(q.termId) + review.filter { it != q.termId }).take(MAX_REVIEW)
        }
        val next = copy(picks = picks + choice, review = nextReview)
        return if (next.done) next.completeDay() else next
    }

    private fun completeDay(): EconProgress {
        val usesFreeze = lastDone == day - 2 && freezes > 0
        val nextStreak = when {
            lastDone == day - 1 -> streak + 1
            usesFreeze -> streak + 1
            else -> 1
        }
        var nextFreezes = if (usesFreeze) freezes - 1 else freezes
        if (nextStreak % 7 == 0) nextFreezes = minOf(nextFreezes + 1, maxOf(MAX_FREEZES, nextFreezes))
        val after = copy(
            streak = nextStreak,
            best = maxOf(best, nextStreak),
            lastDone = day,
            totalDays = totalDays + 1,
            perfectDays = perfectDays + if (correct == picks.size) 1 else 0,
            freezes = nextFreezes,
        )
        return withUpdatedBadges(after)
    }

    /** 심화 퀴즈 팩 문제에 답한다. 맞히면 정복 목록에 넣고, 틀리면 다시 볼 개념에 넣는다. */
    fun answerPackQuestion(q: EconQuestion, choice: Int): EconProgress {
        if (choice !in q.choices.indices) return this
        val isCorrect = choice == q.answer
        val nextSolved = if (isCorrect && q.id !in solvedPackQuestions) {
            solvedPackQuestions + q.id
        } else {
            solvedPackQuestions
        }
        val nextReview = if (isCorrect) {
            review
        } else {
            (listOf(q.termId) + review.filter { it != q.termId }).take(MAX_REVIEW)
        }
        val after = copy(solvedPackQuestions = nextSolved, review = nextReview)
        return withUpdatedBadges(after)
    }

    /** 사전에서 개념 상세를 열었을 때 기록하고, 다시 볼 개념이면 목록에서 뺀다. */
    fun reviewed(termId: String): EconProgress {
        val nextSeen = if (termId in seenTerms || Dictionary.find(termId) == null) seenTerms else seenTerms + termId
        val after = copy(review = review.filter { it != termId }, seenTerms = nextSeen)
        return withUpdatedBadges(after)
    }

    /** 앱 내 결제로 연속 보호권 1개를 추가 구매한다(최대 [MAX_PURCHASED_FREEZES]개 보유 가능). */
    fun buyFreeze(): EconProgress {
        if (freezes >= MAX_PURCHASED_FREEZES) return this
        val after = copy(freezes = freezes + 1, purchasedFreezes = purchasedFreezes + 1)
        return withUpdatedBadges(after)
    }

    fun packSolvedCount(pack: EconQuizPack): Int =
        pack.questions.count { it.id in solvedPackQuestions }

    fun isPackCompleted(pack: EconQuizPack): Boolean =
        pack.questions.all { it.id in solvedPackQuestions }

    private fun withUpdatedBadges(after: EconProgress): EconProgress {
        val earnedBefore = EconBadges.earned(this).map { it.id }.toSet()
        val newlyEarned = EconBadges.earned(after).map { it.id }.filter { it !in earnedBefore }
        return if (newlyEarned.isEmpty()) after else after.copy(newBadges = (after.newBadges + newlyEarned).distinct())
    }

    companion object {
        const val MAX_REVIEW = 20
        const val MAX_FREEZES = 2
        const val MAX_PURCHASED_FREEZES = 5
        const val NEVER = -100L
        fun empty(epochDay: Long) = EconProgress(epochDay, emptyList(), emptyList())
    }
}

/** 배지 도감 분류 (Garmin Connect 스타일 카테고리) */
enum class BadgeCategory(val label: String) {
    STREAK("연속 기록"),
    QUEST("누적 퀘스트"),
    ACCURACY("만점·정확도"),
    PACK("심화 퀴즈팩"),
    DICTIONARY("사전·수집"),
}

data class EconBadge(
    val id: String,
    val icon: String,
    val title: String,
    val detail: String,
    val category: BadgeCategory = BadgeCategory.STREAK,
    /** Garmin 배지 점수 체계 (1, 2, 4, 8 pt) */
    val points: Int = 1,
    val progressText: (EconProgress) -> String = { "" },
    val earnedBy: (EconProgress) -> Boolean,
)

/** 유저 레벨 정보 (Garmin Connect Lv.1 ~ Lv.10 참고) */
data class UserLevelInfo(
    val level: Int,
    val title: String,
    val totalPoints: Int,
    val currentLevelStart: Int,
    val nextLevelTarget: Int?,
) {
    val progressFraction: Float
        get() {
            val next = nextLevelTarget ?: return 1f
            val span = (next - currentLevelStart).coerceAtLeast(1)
            return ((totalPoints - currentLevelStart).toFloat() / span).coerceIn(0f, 1f)
        }
    val pointsToNext: Int get() = nextLevelTarget?.let { (it - totalPoints).coerceAtLeast(0) } ?: 0
}

/**
 * Garmin 스타일 배지 도감(총 30개 배지) 및 유저 레벨 계산기.
 * 연속 기록은 가장 길었던 기록([EconProgress.best]) 기준이라 끊겨도 받은 배지는 영구 보존된다.
 */
object EconBadges {
    private fun packDone(p: EconProgress, pack: EconQuizPack) = p.packSolvedCount(pack)

    val all: List<EconBadge> = listOf(
        // 1. 연속 기록 (8개)
        EconBadge(
            "first", "🌱", "첫 퀘스트", "오늘의 퀘스트를 처음 끝냈어요",
            BadgeCategory.STREAK, points = 1,
            progressText = { "${minOf(it.totalDays, 1)} / 1일" },
        ) { it.totalDays >= 1 },
        EconBadge(
            "streak3", "🔥", "3일 연속", "사흘 연속 퀘스트 완료",
            BadgeCategory.STREAK, points = 1,
            progressText = { "${minOf(it.best, 3)} / 3일" },
        ) { it.best >= 3 },
        EconBadge(
            "streak7", "📅", "일주일 연속", "7일 연속 퀘스트 완료 (보호권 지급)",
            BadgeCategory.STREAK, points = 2,
            progressText = { "${minOf(it.best, 7)} / 7일" },
        ) { it.best >= 7 },
        EconBadge(
            "streak14", "💪", "2주 연속", "14일 연속 퀘스트 완료",
            BadgeCategory.STREAK, points = 2,
            progressText = { "${minOf(it.best, 14)} / 14일" },
        ) { it.best >= 14 },
        EconBadge(
            "streak30", "🏆", "한 달 연속", "30일 연속 퀘스트 완료",
            BadgeCategory.STREAK, points = 4,
            progressText = { "${minOf(it.best, 30)} / 30일" },
        ) { it.best >= 30 },
        EconBadge(
            "streak60", "💎", "두 달 연속", "60일 연속 퀘스트 완료",
            BadgeCategory.STREAK, points = 4,
            progressText = { "${minOf(it.best, 60)} / 60일" },
        ) { it.best >= 60 },
        EconBadge(
            "streak100", "👑", "100일 연속", "100일 연속 퀘스트 완료",
            BadgeCategory.STREAK, points = 8,
            progressText = { "${minOf(it.best, 100)} / 100일" },
        ) { it.best >= 100 },
        EconBadge(
            "freeze_keeper", "🧊", "연속 수호자", "연속 보호권을 보유해 기록을 지킬 준비 완료",
            BadgeCategory.STREAK, points = 2,
            progressText = { "보호권 ${it.freezes}개" },
        ) { it.freezes >= 1 || it.purchasedFreezes >= 1 },

        // 2. 누적 퀘스트 (6개)
        EconBadge(
            "total3", "🚗", "주차장 단골", "누적 3일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 1,
            progressText = { "${minOf(it.totalDays, 3)} / 3일" },
        ) { it.totalDays >= 3 },
        EconBadge(
            "total7", "🛣️", "출퇴근 파트너", "누적 7일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 1,
            progressText = { "${minOf(it.totalDays, 7)} / 7일" },
        ) { it.totalDays >= 7 },
        EconBadge(
            "total15", "📘", "보름의 축적", "누적 15일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 2,
            progressText = { "${minOf(it.totalDays, 15)} / 15일" },
        ) { it.totalDays >= 15 },
        EconBadge(
            "total30", "📚", "누적 30일", "모두 30일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 4,
            progressText = { "${minOf(it.totalDays, 30)} / 30일" },
        ) { it.totalDays >= 30 },
        EconBadge(
            "total50", "🏛️", "오십일의 내공", "누적 50일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 4,
            progressText = { "${minOf(it.totalDays, 50)} / 50일" },
        ) { it.totalDays >= 50 },
        EconBadge(
            "total100", "🌍", "백일의 지식인", "누적 100일 동안 퀘스트 완료",
            BadgeCategory.QUEST, points = 8,
            progressText = { "${minOf(it.totalDays, 100)} / 100일" },
        ) { it.totalDays >= 100 },

        // 3. 만점·정확도 (6개)
        EconBadge(
            "perfect1", "⭐", "첫 만점", "하루 3문제를 모두 맞혔어요",
            BadgeCategory.ACCURACY, points = 1,
            progressText = { "${minOf(it.perfectDays, 1)} / 1회" },
        ) { it.perfectDays >= 1 },
        EconBadge(
            "perfect3", "✨", "만점 3회", "하루 퀘스트 만점을 3번 받았어요",
            BadgeCategory.ACCURACY, points = 1,
            progressText = { "${minOf(it.perfectDays, 3)} / 3회" },
        ) { it.perfectDays >= 3 },
        EconBadge(
            "perfect5", "🌟", "만점 5회", "하루 퀘스트 만점을 5번 받았어요",
            BadgeCategory.ACCURACY, points = 2,
            progressText = { "${minOf(it.perfectDays, 5)} / 5회" },
        ) { it.perfectDays >= 5 },
        EconBadge(
            "perfect10", "🏅", "만점 10회", "만점을 10번 받았어요",
            BadgeCategory.ACCURACY, points = 2,
            progressText = { "${minOf(it.perfectDays, 10)} / 10회" },
        ) { it.perfectDays >= 10 },
        EconBadge(
            "perfect25", "🎖️", "만점 25회", "만점을 25번 받았어요",
            BadgeCategory.ACCURACY, points = 4,
            progressText = { "${minOf(it.perfectDays, 25)} / 25회" },
        ) { it.perfectDays >= 25 },
        EconBadge(
            "perfect50", "🔱", "만점 50회", "만점을 50번 받았어요",
            BadgeCategory.ACCURACY, points = 8,
            progressText = { "${minOf(it.perfectDays, 50)} / 50회" },
        ) { it.perfectDays >= 50 },

        // 4. 심화 퀴즈팩 (5개)
        EconBadge(
            "pack_first", "🎯", "심화 첫 도전", "심화 퀴즈 팩 문제를 처음 맞혔어요",
            BadgeCategory.PACK, points = 1,
            progressText = { "${minOf(it.solvedPackQuestions.size, 1)} / 1문제" },
        ) { it.solvedPackQuestions.isNotEmpty() },
        EconBadge(
            "pack_starter", "💼", "사회초년생 졸업", "사회초년생 패키지 6문제를 모두 맞혔어요",
            BadgeCategory.PACK, points = 2,
            progressText = { "${packDone(it, EconQuiz.starterPack)} / 6문제" },
        ) { it.isPackCompleted(EconQuiz.starterPack) },
        EconBadge(
            "pack_macro", "📈", "거시·금리 분석가", "금리·환율 심화 팩 6문제를 모두 맞혔어요",
            BadgeCategory.PACK, points = 2,
            progressText = { "${packDone(it, EconQuiz.macroPack)} / 6문제" },
        ) { it.isPackCompleted(EconQuiz.macroPack) },
        EconBadge(
            "pack_market", "🏢", "기업·시장 분석가", "기업·시장 심화 팩 6문제를 모두 맞혔어요",
            BadgeCategory.PACK, points = 2,
            progressText = { "${packDone(it, EconQuiz.marketPack)} / 6문제" },
        ) { it.isPackCompleted(EconQuiz.marketPack) },
        EconBadge(
            "pack_all", "🎓", "심화 그랜드슬램", "심화 퀴즈 팩 3종(18문제)을 모두 정복했어요",
            BadgeCategory.PACK, points = 8,
            progressText = { "${it.solvedPackQuestions.size} / 18문제" },
        ) { EconQuiz.packs.all { pack -> it.isPackCompleted(pack) } },

        // 5. 사전·수집 (5개)
        EconBadge(
            "dict1", "🔍", "호기심의 시작", "경제 사전 개념을 처음 열어 봤어요",
            BadgeCategory.DICTIONARY, points = 1,
            progressText = { "${minOf(it.seenTerms.size, 1)} / 1개" },
        ) { it.seenTerms.isNotEmpty() },
        EconBadge(
            "dict5", "📖", "개념 탐험가", "경제 사전 개념 5개를 열어 봤어요",
            BadgeCategory.DICTIONARY, points = 1,
            progressText = { "${minOf(it.seenTerms.size, 5)} / 5개" },
        ) { it.seenTerms.size >= 5 },
        EconBadge(
            "dict15", "🗂️", "용어 수집가", "경제 사전 개념 15개를 열어 봤어요",
            BadgeCategory.DICTIONARY, points = 2,
            progressText = { "${minOf(it.seenTerms.size, 15)} / 15개" },
        ) { it.seenTerms.size >= 15 },
        EconBadge(
            "dict30", "🧠", "경제 백과사전", "경제 사전 개념 30개를 열어 봤어요",
            BadgeCategory.DICTIONARY, points = 4,
            progressText = { "${minOf(it.seenTerms.size, 30)} / 30개" },
        ) { it.seenTerms.size >= 30 },
        EconBadge(
            "dict_all", "🏛️", "사전 마스터", "경제 사전의 모든 개념(40개)을 열어 봤어요",
            BadgeCategory.DICTIONARY, points = 8,
            progressText = { "${minOf(it.seenTerms.size, Dictionary.terms.size)} / ${Dictionary.terms.size}개" },
        ) { it.seenTerms.size >= Dictionary.terms.size },
    )

    private val levelTable: List<Pair<Int, String>> = listOf(
        0 to "새내기 운전자",
        4 to "경제 입문자",
        10 to "뉴스 독자",
        18 to "생활경제러",
        28 to "시장 관찰자",
        42 to "흐름 분석가",
        60 to "재테크 탐구자",
        85 to "거시경제 통찰자",
        115 to "금융 전략가",
        150 to "경제 마스터",
    )

    fun earned(p: EconProgress): List<EconBadge> = all.filter { it.earnedBy(p) }

    fun byCategory(category: BadgeCategory): List<EconBadge> = all.filter { it.category == category }

    fun find(id: String): EconBadge? = all.firstOrNull { it.id == id }

    /** 다음으로 노릴 연속 기록 목표(없으면 null). */
    fun nextStreakGoal(current: Int): Int? = listOf(3, 7, 14, 30, 60, 100).firstOrNull { it > current }

    /** 배지 포인트 + 퀘스트/심화/사전 활동 포인트를 합산한 총 포인트 */
    fun totalPoints(p: EconProgress): Int {
        val badgePts = earned(p).sumOf { it.points }
        val activityPts = p.totalDays * 2 + p.solvedPackQuestions.size + (p.seenTerms.size / 3)
        return badgePts + activityPts
    }

    /** 현재 유저 레벨(Lv.1 ~ Lv.10)과 진행도 정보 */
    fun levelInfo(p: EconProgress): UserLevelInfo {
        val pts = totalPoints(p)
        var idx = 0
        for (i in levelTable.indices) {
            if (pts >= levelTable[i].first) idx = i else break
        }
        val (startPts, title) = levelTable[idx]
        val nextPts = levelTable.getOrNull(idx + 1)?.first
        return UserLevelInfo(
            level = idx + 1,
            title = title,
            totalPoints = pts,
            currentLevelStart = startPts,
            nextLevelTarget = nextPts,
        )
    }
}
