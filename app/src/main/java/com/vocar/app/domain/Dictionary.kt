package com.vocar.app.domain

/** 해설·사전에 붙이는 그림의 종류. 그래프는 화면에서 직접 그리고, FLOW는 단계를 화살표로 잇는다. */
enum class DiagramKind { SUPPLY_DEMAND, RATE_VS_BOND, BUSINESS_CYCLE, PURCHASING_POWER, FLOW }

data class Diagram(
    val kind: DiagramKind,
    val caption: String,
    /** FLOW일 때 화살표로 이을 단계. 그래프일 때는 범례로 쓴다. */
    val steps: List<String> = emptyList(),
)

object Diagrams {
    val SUPPLY_DEMAND = Diagram(
        DiagramKind.SUPPLY_DEMAND,
        "사려는 양(수요)과 팔려는 양(공급)이 만나는 곳에서 가격이 정해져요.",
        listOf("수요", "공급", "가격이 정해지는 곳"),
    )
    val RATE_VS_BOND = Diagram(
        DiagramKind.RATE_VS_BOND,
        "금리가 오르면 이미 나와 있던 채권의 가격은 내려가요. 둘은 시소처럼 반대로 움직여요.",
        listOf("금리", "채권 가격"),
    )
    val BUSINESS_CYCLE = Diagram(
        DiagramKind.BUSINESS_CYCLE,
        "경기는 오르내리기를 되풀이해요. 정점 다음에는 후퇴, 저점 다음에는 회복이 와요.",
        listOf("호황", "후퇴", "불황", "회복"),
    )
    val PURCHASING_POWER = Diagram(
        DiagramKind.PURCHASING_POWER,
        "물가가 오를수록 같은 돈으로 살 수 있는 양이 줄어들어요.",
        listOf("물가", "살 수 있는 양"),
    )
    val RATE_HIKE_FLOW = Diagram(
        DiagramKind.FLOW,
        "금리를 올리면 돈을 빌리기 어려워지고, 쓰는 돈이 줄면서 물가가 진정되는 쪽으로 움직여요.",
        listOf("금리 인상", "대출 부담 증가", "소비·투자 감소", "물가 진정"),
    )
    val RATE_CUT_FLOW = Diagram(
        DiagramKind.FLOW,
        "금리를 내리면 돈을 빌리기 쉬워지고, 쓰는 돈이 늘면서 경기가 살아나는 쪽으로 움직여요.",
        listOf("금리 인하", "대출 부담 감소", "소비·투자 증가", "경기 회복"),
    )
    val WEAK_WON_FLOW = Diagram(
        DiagramKind.FLOW,
        "원화 가치가 내려가면(환율 상승) 우리 물건은 해외에서 싸지고, 수입품은 비싸져요.",
        listOf("환율 상승(원화 약세)", "수출품 가격 경쟁력 상승", "수입품 가격 상승"),
    )
    val TARIFF_FLOW = Diagram(
        DiagramKind.FLOW,
        "관세가 붙으면 수입품 값이 오르고, 수입이 줄어드는 쪽으로 움직여요.",
        listOf("관세 부과", "수입품 가격 상승", "수입 감소"),
    )
    val PROFIT_FLOW = Diagram(
        DiagramKind.FLOW,
        "번 돈(매출)에서 쓴 돈(비용)을 빼면 남는 돈(이익)이에요.",
        listOf("매출", "− 비용", "= 이익"),
    )
}

/**
 * 경제 개념 하나. 설명은 중학생도 이해할 수 있게 쓰고, 수치·기업명·종목·투자 권유는 담지 않는다
 * (시간이 지나도 틀리지 않는 일반 설명만). 관련 개념은 [Dictionary.terms]의 id를 가리킨다.
 */
data class EconTerm(
    val id: String,
    val name: String,
    val topic: String,
    val plain: String,
    val example: String,
    val related: List<String> = emptyList(),
    val diagram: Diagram? = null,
)

/**
 * 설치에 포함되는 경제 사전. 네트워크 없이 전부 동작하고, 영어 학습 엔진(Engine)과는 독립이다.
 * 개념을 고치거나 늘리면 [VERSION]을 올린다.
 */
object Dictionary {
    const val VERSION = 2

    private const val MONEY = "금리·통화"
    private const val PRICE = "물가·소비"
    private const val TRADE = "환율·무역"
    private const val FIRM = "기업·실적"
    private const val LIFE = "생활 속 경제"
    private const val MARKET = "경기·시장"

    val terms: List<EconTerm> = listOf(
        EconTerm(
            "interest-rate", "금리", MONEY,
            "돈을 빌리거나 맡길 때 붙는 값이에요. 빌린 사람은 빌린 돈에 더해서 갚고, 맡긴 사람은 맡긴 돈에 더해서 받아요.",
            "금리가 낮아지면 대출이 더 싸져요.",
            listOf("central-bank", "loan", "rate-hike", "rate-cut"),
            Diagrams.RATE_VS_BOND,
        ),
        EconTerm(
            "central-bank", "중앙은행", MONEY,
            "한 나라의 돈의 양과 금리를 관리하는 은행이에요. 일반 은행들이 돈을 맡기고 빌리는 '은행의 은행'이기도 해요.",
            "중앙은행이 금리를 올렸어요.",
            listOf("interest-rate", "monetary-policy", "inflation"),
            null,
        ),
        EconTerm(
            "rate-hike", "금리 인상", MONEY,
            "기준이 되는 금리를 올리는 거예요. 빌린 돈의 부담은 커지고, 사람들이 쓰는 돈은 줄어드는 쪽으로 움직여요.",
            "금리 인상은 많은 사람을 놀라게 했어요.",
            listOf("interest-rate", "rate-cut", "inflation"),
            Diagrams.RATE_HIKE_FLOW,
        ),
        EconTerm(
            "rate-cut", "금리 인하", MONEY,
            "기준이 되는 금리를 내리는 거예요. 돈을 빌리기 쉬워져서 쓰는 돈과 투자가 늘어나는 쪽으로 움직여요.",
            "금리 인하는 경제가 자라는 데 도움이 될 수 있어요.",
            listOf("interest-rate", "rate-hike", "economic-growth"),
            Diagrams.RATE_CUT_FLOW,
        ),
        EconTerm(
            "monetary-policy", "통화 정책", MONEY,
            "중앙은행이 금리와 돈의 양을 조절해서 물가와 경기를 안정시키려는 정책이에요.",
            "통화 정책은 물가를 안정시키는 것을 목표로 해요.",
            listOf("central-bank", "inflation", "interest-rate"),
            Diagrams.RATE_HIKE_FLOW,
        ),
        EconTerm(
            "currency", "통화", MONEY,
            "한 나라에서 공식적으로 쓰는 돈이에요. 우리나라는 원, 미국은 달러를 써요.",
            "나라마다 자기만의 통화가 있어요.",
            listOf("exchange-rate", "inflation"),
            null,
        ),
        EconTerm(
            "inflation", "물가 상승(인플레이션)", PRICE,
            "물건과 서비스 값이 전체적으로 계속 오르는 현상이에요. 같은 돈으로 살 수 있는 것이 줄어들어요.",
            "물가가 오르면 일상 물건이 더 비싸져요.",
            listOf("deflation", "purchasing-power", "central-bank"),
            Diagrams.PURCHASING_POWER,
        ),
        EconTerm(
            "deflation", "물가 하락(디플레이션)", PRICE,
            "물건과 서비스 값이 전체적으로 계속 내리는 현상이에요. 값이 더 내릴 거라 생각해서 사람들이 쓰기를 미루기도 해요.",
            "물가가 내리면 소비가 느려질 수 있어요.",
            listOf("inflation", "consumer-spending", "recession"),
            null,
        ),
        EconTerm(
            "price", "가격", PRICE,
            "어떤 물건이나 서비스를 얻기 위해 내야 하는 돈의 값이에요. 수요와 공급이 만나서 정해져요.",
            "가격은 수요와 공급에 따라 달라져요.",
            listOf("demand", "supply", "inflation"),
            Diagrams.SUPPLY_DEMAND,
        ),
        EconTerm(
            "cost-of-living", "생활비", PRICE,
            "먹고 자고 이동하는 등 평소 생활을 이어가는 데 드는 돈이에요.",
            "큰 도시일수록 생활비가 더 많이 들어요.",
            listOf("rent", "income", "inflation"),
            null,
        ),
        EconTerm(
            "consumer-spending", "소비 지출", PRICE,
            "사람들이 물건과 서비스를 사는 데 쓰는 돈이에요. 한 나라 경제에서 아주 큰 부분을 차지해요.",
            "소비 지출은 경제를 움직이게 해요.",
            listOf("demand", "economic-growth", "income"),
            null,
        ),
        EconTerm(
            "purchasing-power", "구매력", PRICE,
            "가진 돈으로 살 수 있는 물건의 양이에요. 물가가 오르면 같은 돈의 구매력은 줄어요.",
            "물가가 오르면 구매력이 줄어요.",
            listOf("inflation", "income", "price"),
            Diagrams.PURCHASING_POWER,
        ),
        EconTerm(
            "demand", "수요", PRICE,
            "사람들이 어떤 물건을 사고 싶어 하는 마음과 양이에요. 사려는 사람이 많으면 값이 오르기 쉬워요.",
            "겨울에는 따뜻한 외투 수요가 늘어요.",
            listOf("supply", "price", "consumer-spending"),
            Diagrams.SUPPLY_DEMAND,
        ),
        EconTerm(
            "supply", "공급", PRICE,
            "시장에 팔려고 내놓는 물건의 양이에요. 물건이 적은데 사려는 사람이 많으면 값이 오르기 쉬워요.",
            "공급이 적으면 가격이 오를 수 있어요.",
            listOf("demand", "price"),
            Diagrams.SUPPLY_DEMAND,
        ),
        EconTerm(
            "exchange-rate", "환율", TRADE,
            "한 나라 돈을 다른 나라 돈으로 바꿀 때의 비율이에요. 여행이나 수입품 값에 영향을 줘요.",
            "환율은 매일 바뀌어요.",
            listOf("currency", "import", "export"),
            Diagrams.WEAK_WON_FLOW,
        ),
        EconTerm(
            "export", "수출", TRADE,
            "우리나라에서 만든 물건이나 서비스를 다른 나라에 파는 거예요.",
            "그 나라는 자동차와 전자제품을 수출해요.",
            listOf("import", "trade-deficit", "exchange-rate"),
            Diagrams.WEAK_WON_FLOW,
        ),
        EconTerm(
            "import", "수입", TRADE,
            "다른 나라에서 만든 물건이나 서비스를 사 오는 거예요.",
            "우리는 다른 나라에서 석유를 수입해요.",
            listOf("export", "tariff", "exchange-rate"),
            null,
        ),
        EconTerm(
            "trade-deficit", "무역 적자", TRADE,
            "일정 기간 동안 수출한 것보다 수입한 것이 더 많은 상태예요. 반대는 무역 흑자예요.",
            "무역 적자는 수입이 수출보다 많다는 뜻이에요.",
            listOf("export", "import"),
            null,
        ),
        EconTerm(
            "tariff", "관세", TRADE,
            "수입품이 국경을 넘어 들어올 때 나라가 걷는 세금이에요. 수입품 값이 오르는 원인이 되기도 해요.",
            "관세가 붙으면 수입품이 더 비싸져요.",
            listOf("import", "tax", "price"),
            Diagrams.TARIFF_FLOW,
        ),
        EconTerm(
            "revenue", "매출", FIRM,
            "회사가 물건과 서비스를 팔아서 번 돈의 전체예요. 아직 비용을 빼기 전의 금액이에요.",
            "그 회사의 매출이 올해 늘었어요.",
            listOf("cost", "profit", "earnings"),
            Diagrams.PROFIT_FLOW,
        ),
        EconTerm(
            "cost", "비용", FIRM,
            "물건을 만들고 팔고 회사를 꾸려가는 데 드는 돈이에요. 재료비, 월급, 임대료 등이 있어요.",
            "비용이 오르면 이익이 줄 수 있어요.",
            listOf("revenue", "profit", "loss"),
            Diagrams.PROFIT_FLOW,
        ),
        EconTerm(
            "profit", "이익", FIRM,
            "번 돈(매출)에서 쓴 돈(비용)을 뺀 나머지예요. 남는 돈이 있다는 뜻이에요.",
            "그 가게는 지난달에 작은 이익을 냈어요.",
            listOf("revenue", "cost", "loss", "earnings"),
            Diagrams.PROFIT_FLOW,
        ),
        EconTerm(
            "loss", "손실", FIRM,
            "쓴 돈(비용)이 번 돈(매출)보다 더 많아서 모자라는 상태예요.",
            "그 회사는 이번 분기에 손실을 냈다고 발표했어요.",
            listOf("profit", "cost", "earnings"),
            null,
        ),
        EconTerm(
            "earnings", "실적", FIRM,
            "회사가 일정 기간 동안 얼마나 벌고 얼마나 남겼는지를 보여 주는 성적표예요.",
            "그 회사는 금요일에 실적을 발표할 거예요.",
            listOf("revenue", "profit", "loss"),
            null,
        ),
        EconTerm(
            "dividend", "배당", FIRM,
            "회사가 번 이익의 일부를 주식을 가진 사람들에게 나누어 주는 돈이에요.",
            "어떤 회사는 주인인 주주에게 배당을 줘요.",
            listOf("profit", "stock"),
            null,
        ),
        EconTerm(
            "startup", "스타트업", FIRM,
            "새로운 아이디어로 막 시작한 젊은 회사예요. 빠르게 커지려고 하지만 실패할 가능성도 커요.",
            "그녀는 작은 스타트업에서 일해요.",
            listOf("revenue", "loss"),
            null,
        ),
        EconTerm(
            "rent", "월세", LIFE,
            "남의 집이나 공간을 빌려 쓰는 대가로 정해진 날에 내는 돈이에요.",
            "저는 매달 첫날에 월세를 내요.",
            listOf("cost-of-living", "income", "budget"),
            null,
        ),
        EconTerm(
            "tax", "세금", LIFE,
            "나라와 지방 정부가 공공 서비스에 쓰려고 국민과 회사로부터 걷는 돈이에요.",
            "세금은 도로와 학교를 짓는 데 쓰여요.",
            listOf("income", "tariff"),
            null,
        ),
        EconTerm(
            "income", "소득", LIFE,
            "일을 하거나 이자와 같은 방법으로 들어오는 돈이에요.",
            "그녀의 소득은 직장에서 나와요.",
            listOf("tax", "budget", "savings"),
            null,
        ),
        EconTerm(
            "budget", "예산", LIFE,
            "들어올 돈과 쓸 돈을 미리 계획해서 적어 둔 거예요. 가정, 회사, 나라 모두 예산을 세워요.",
            "우리는 여행 예산을 짰어요.",
            listOf("income", "savings", "cost-of-living"),
            null,
        ),
        EconTerm(
            "savings", "저축", LIFE,
            "번 돈 가운데 바로 쓰지 않고 남겨 두는 돈이에요. 갑자기 돈이 필요할 때를 대비해요.",
            "꾸준한 저축은 비상시에 도움이 돼요.",
            listOf("income", "budget", "interest-rate"),
            null,
        ),
        EconTerm(
            "loan", "대출", LIFE,
            "은행 같은 곳에서 돈을 빌리는 거예요. 약속한 기간 안에 이자를 더해 갚아야 해요.",
            "그는 집을 사려고 대출을 받았어요.",
            listOf("interest-rate", "income", "budget"),
            null,
        ),
        EconTerm(
            "economy", "경제", MARKET,
            "사람들이 물건과 서비스를 만들고, 사고팔고, 쓰는 활동 전체예요.",
            "경제는 우리의 일상에 영향을 줘요.",
            listOf("gdp", "market", "economic-growth"),
            null,
        ),
        EconTerm(
            "gdp", "국내총생산(GDP)", MARKET,
            "한 나라 안에서 일정 기간 동안 새로 만들어진 물건과 서비스의 가치를 모두 더한 값이에요. 나라 경제의 크기를 재는 대표 지표예요.",
            "GDP는 한 나라 경제의 크기를 보여 줘요.",
            listOf("economy", "economic-growth", "recession"),
            Diagrams.BUSINESS_CYCLE,
        ),
        EconTerm(
            "economic-growth", "경제 성장", MARKET,
            "한 나라가 만들어 내는 물건과 서비스가 시간이 지나면서 늘어나는 거예요.",
            "경제가 성장하면 새 일자리가 생길 수 있어요.",
            listOf("gdp", "recession", "unemployment"),
            Diagrams.BUSINESS_CYCLE,
        ),
        EconTerm(
            "recession", "경기 침체", MARKET,
            "경제 활동이 한동안 눈에 띄게 줄어드는 시기예요. 일자리가 줄고 사람들이 쓰는 돈도 줄기 쉬워요.",
            "경기 침체가 오면 일자리가 줄기도 해요.",
            listOf("economic-growth", "unemployment", "gdp"),
            Diagrams.BUSINESS_CYCLE,
        ),
        EconTerm(
            "unemployment", "실업", MARKET,
            "일할 의사와 능력이 있는데도 일자리를 구하지 못한 상태예요.",
            "회사들이 일자리를 줄이면 실업이 늘어요.",
            listOf("recession", "economic-growth", "income"),
            null,
        ),
        EconTerm(
            "market", "시장", MARKET,
            "사는 사람과 파는 사람이 만나 거래하는 곳이에요. 시장에서 수요와 공급이 만나 가격이 정해져요.",
            "시장은 주말에 붐벼요.",
            listOf("demand", "supply", "price"),
            Diagrams.SUPPLY_DEMAND,
        ),
        EconTerm(
            "stock", "주식", MARKET,
            "회사의 주인 자격을 잘게 나눈 증서예요. 주식을 가진 사람을 주주라고 해요.",
            "주식은 회사의 작은 한 몫을 나타내요.",
            listOf("dividend", "market", "bond"),
            null,
        ),
        EconTerm(
            "bond", "채권", MARKET,
            "정부나 회사가 돈을 빌리면서 '정해진 날에 이자와 함께 갚겠다'고 약속해 쓴 증서예요.",
            "채권은 빌린 돈을 갚겠다는 약속이에요.",
            listOf("stock", "interest-rate", "loan"),
            Diagrams.RATE_VS_BOND,
        ),
    )

    private val byId: Map<String, EconTerm> = terms.associateBy { it.id }

    /** 주제 목록(등장 순서 유지). */
    val topics: List<String> = terms.map { it.topic }.distinct()

    fun find(id: String): EconTerm? = byId[id]

    fun byTopic(topic: String): List<EconTerm> = terms.filter { it.topic == topic }
}
