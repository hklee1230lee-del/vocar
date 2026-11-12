package com.vocar.app.domain

/**
 * 경제 용어 하나. 설명은 중학생도 이해할 수 있게 쓰고, 수치·기업명·종목·투자 권유는 담지 않는다
 * (시간이 지나도 틀리지 않는 일반 설명만). 관련 용어는 [Dictionary.terms]의 id를 가리킨다.
 */
data class EconTerm(
    val id: String,
    val ko: String,
    val en: String,
    val topic: String,
    val plain: String,
    val example: String,
    val exampleKo: String,
    val related: List<String> = emptyList(),
)

/**
 * 설치에 포함되는 경제 사전. 네트워크 없이 전부 동작하고, 학습 엔진(Engine)과는 독립이다.
 * 용어를 고치거나 늘리면 [VERSION]을 올린다.
 */
object Dictionary {
    const val VERSION = 1

    private const val MONEY = "금리·통화"
    private const val PRICE = "물가·소비"
    private const val TRADE = "환율·무역"
    private const val FIRM = "기업·실적"
    private const val LIFE = "생활 속 경제"
    private const val MARKET = "경기·시장"

    val terms: List<EconTerm> = listOf(
        // ── 금리·통화 ──────────────────────────────────────────
        EconTerm(
            "interest-rate", "금리", "interest rate", MONEY,
            "돈을 빌리거나 맡길 때 붙는 값이에요. 빌린 사람은 빌린 돈에 더해서 갚고, 맡긴 사람은 맡긴 돈에 더해서 받아요.",
            "A lower interest rate makes loans cheaper.",
            "금리가 낮아지면 대출이 더 싸져요.",
            listOf("central-bank", "loan", "rate-hike", "rate-cut"),
        ),
        EconTerm(
            "central-bank", "중앙은행", "central bank", MONEY,
            "한 나라의 돈의 양과 금리를 관리하는 은행이에요. 일반 은행들이 돈을 맡기고 빌리는 '은행의 은행'이기도 해요.",
            "The central bank raised interest rates.",
            "중앙은행이 금리를 올렸어요.",
            listOf("interest-rate", "monetary-policy", "inflation"),
        ),
        EconTerm(
            "rate-hike", "금리 인상", "rate hike", MONEY,
            "기준이 되는 금리를 올리는 거예요. 빌린 돈의 부담은 커지고, 사람들이 쓰는 돈은 줄어드는 쪽으로 움직여요.",
            "The rate hike surprised many people.",
            "금리 인상은 많은 사람을 놀라게 했어요.",
            listOf("interest-rate", "rate-cut", "inflation"),
        ),
        EconTerm(
            "rate-cut", "금리 인하", "rate cut", MONEY,
            "기준이 되는 금리를 내리는 거예요. 돈을 빌리기 쉬워져서 쓰는 돈과 투자가 늘어나는 쪽으로 움직여요.",
            "A rate cut can help the economy grow.",
            "금리 인하는 경제가 자라는 데 도움이 될 수 있어요.",
            listOf("interest-rate", "rate-hike", "economic-growth"),
        ),
        EconTerm(
            "monetary-policy", "통화 정책", "monetary policy", MONEY,
            "중앙은행이 금리와 돈의 양을 조절해서 물가와 경기를 안정시키려는 정책이에요.",
            "Monetary policy aims to keep prices stable.",
            "통화 정책은 물가를 안정시키는 것을 목표로 해요.",
            listOf("central-bank", "inflation", "interest-rate"),
        ),
        EconTerm(
            "currency", "통화", "currency", MONEY,
            "한 나라에서 공식적으로 쓰는 돈이에요. 우리나라는 원, 미국은 달러를 써요.",
            "Each country has its own currency.",
            "나라마다 자기만의 통화가 있어요.",
            listOf("exchange-rate", "inflation"),
        ),

        // ── 물가·소비 ──────────────────────────────────────────
        EconTerm(
            "inflation", "물가 상승(인플레이션)", "inflation", PRICE,
            "물건과 서비스 값이 전체적으로 계속 오르는 현상이에요. 같은 돈으로 살 수 있는 것이 줄어들어요.",
            "Inflation makes everyday items more expensive.",
            "물가가 오르면 일상 물건이 더 비싸져요.",
            listOf("deflation", "purchasing-power", "central-bank"),
        ),
        EconTerm(
            "deflation", "물가 하락(디플레이션)", "deflation", PRICE,
            "물건과 서비스 값이 전체적으로 계속 내리는 현상이에요. 값이 더 내릴 거라 생각해서 사람들이 쓰기를 미루기도 해요.",
            "Deflation can slow down spending.",
            "물가가 내리면 소비가 느려질 수 있어요.",
            listOf("inflation", "consumer-spending", "recession"),
        ),
        EconTerm(
            "price", "가격", "price", PRICE,
            "어떤 물건이나 서비스를 얻기 위해 내야 하는 돈의 값이에요. 수요와 공급이 만나서 정해져요.",
            "The price depends on supply and demand.",
            "가격은 수요와 공급에 따라 달라져요.",
            listOf("demand", "supply", "inflation"),
        ),
        EconTerm(
            "cost-of-living", "생활비", "cost of living", PRICE,
            "먹고 자고 이동하는 등 평소 생활을 이어가는 데 드는 돈이에요.",
            "The cost of living is higher in big cities.",
            "큰 도시일수록 생활비가 더 많이 들어요.",
            listOf("rent", "income", "inflation"),
        ),
        EconTerm(
            "consumer-spending", "소비 지출", "consumer spending", PRICE,
            "사람들이 물건과 서비스를 사는 데 쓰는 돈이에요. 한 나라 경제에서 아주 큰 부분을 차지해요.",
            "Consumer spending keeps the economy moving.",
            "소비 지출은 경제를 움직이게 해요.",
            listOf("demand", "economic-growth", "income"),
        ),
        EconTerm(
            "purchasing-power", "구매력", "purchasing power", PRICE,
            "가진 돈으로 살 수 있는 물건의 양이에요. 물가가 오르면 같은 돈의 구매력은 줄어요.",
            "Higher prices reduce our purchasing power.",
            "물가가 오르면 구매력이 줄어요.",
            listOf("inflation", "income", "price"),
        ),
        EconTerm(
            "demand", "수요", "demand", PRICE,
            "사람들이 어떤 물건을 사고 싶어 하는 마음과 양이에요. 사려는 사람이 많으면 값이 오르기 쉬워요.",
            "Demand for warm coats rises in winter.",
            "겨울에는 따뜻한 외투 수요가 늘어요.",
            listOf("supply", "price", "consumer-spending"),
        ),
        EconTerm(
            "supply", "공급", "supply", PRICE,
            "시장에 팔려고 내놓는 물건의 양이에요. 물건이 적은데 사려는 사람이 많으면 값이 오르기 쉬워요.",
            "A small supply can push prices up.",
            "공급이 적으면 가격이 오를 수 있어요.",
            listOf("demand", "price"),
        ),

        // ── 환율·무역 ──────────────────────────────────────────
        EconTerm(
            "exchange-rate", "환율", "exchange rate", TRADE,
            "한 나라 돈을 다른 나라 돈으로 바꿀 때의 비율이에요. 여행이나 수입품 값에 영향을 줘요.",
            "The exchange rate changes every day.",
            "환율은 매일 바뀌어요.",
            listOf("currency", "import", "export"),
        ),
        EconTerm(
            "export", "수출", "export", TRADE,
            "우리나라에서 만든 물건이나 서비스를 다른 나라에 파는 거예요.",
            "The country exports cars and electronics.",
            "그 나라는 자동차와 전자제품을 수출해요.",
            listOf("import", "trade-deficit", "exchange-rate"),
        ),
        EconTerm(
            "import", "수입", "import", TRADE,
            "다른 나라에서 만든 물건이나 서비스를 사 오는 거예요.",
            "We import oil from other countries.",
            "우리는 다른 나라에서 석유를 수입해요.",
            listOf("export", "tariff", "exchange-rate"),
        ),
        EconTerm(
            "trade-deficit", "무역 적자", "trade deficit", TRADE,
            "일정 기간 동안 수출한 것보다 수입한 것이 더 많은 상태예요. 반대는 무역 흑자예요.",
            "A trade deficit means imports are greater than exports.",
            "무역 적자는 수입이 수출보다 많다는 뜻이에요.",
            listOf("export", "import"),
        ),
        EconTerm(
            "tariff", "관세", "tariff", TRADE,
            "수입품이 국경을 넘어 들어올 때 나라가 걷는 세금이에요. 수입품 값이 오르는 원인이 되기도 해요.",
            "A tariff makes imported goods more expensive.",
            "관세가 붙으면 수입품이 더 비싸져요.",
            listOf("import", "tax", "price"),
        ),

        // ── 기업·실적 ──────────────────────────────────────────
        EconTerm(
            "revenue", "매출", "revenue", FIRM,
            "회사가 물건과 서비스를 팔아서 번 돈의 전체예요. 아직 비용을 빼기 전의 금액이에요.",
            "The company's revenue grew this year.",
            "그 회사의 매출이 올해 늘었어요.",
            listOf("cost", "profit", "earnings"),
        ),
        EconTerm(
            "cost", "비용", "cost", FIRM,
            "물건을 만들고 팔고 회사를 꾸려가는 데 드는 돈이에요. 재료비, 월급, 임대료 등이 있어요.",
            "Rising costs can reduce profit.",
            "비용이 오르면 이익이 줄 수 있어요.",
            listOf("revenue", "profit", "loss"),
        ),
        EconTerm(
            "profit", "이익", "profit", FIRM,
            "번 돈(매출)에서 쓴 돈(비용)을 뺀 나머지예요. 남는 돈이 있다는 뜻이에요.",
            "The shop made a small profit last month.",
            "그 가게는 지난달에 작은 이익을 냈어요.",
            listOf("revenue", "cost", "loss", "earnings"),
        ),
        EconTerm(
            "loss", "손실", "loss", FIRM,
            "쓴 돈(비용)이 번 돈(매출)보다 더 많아서 모자라는 상태예요.",
            "The company reported a loss this quarter.",
            "그 회사는 이번 분기에 손실을 냈다고 발표했어요.",
            listOf("profit", "cost", "earnings"),
        ),
        EconTerm(
            "earnings", "실적", "earnings", FIRM,
            "회사가 일정 기간 동안 얼마나 벌고 얼마나 남겼는지를 보여 주는 성적표예요.",
            "The company will announce its earnings on Friday.",
            "그 회사는 금요일에 실적을 발표할 거예요.",
            listOf("revenue", "profit", "loss"),
        ),
        EconTerm(
            "dividend", "배당", "dividend", FIRM,
            "회사가 번 이익의 일부를 주식을 가진 사람들에게 나누어 주는 돈이에요.",
            "Some companies pay a dividend to their owners.",
            "어떤 회사는 주인인 주주에게 배당을 줘요.",
            listOf("profit", "stock"),
        ),
        EconTerm(
            "startup", "스타트업", "startup", FIRM,
            "새로운 아이디어로 막 시작한 젊은 회사예요. 빠르게 커지려고 하지만 실패할 가능성도 커요.",
            "She works at a small startup.",
            "그녀는 작은 스타트업에서 일해요.",
            listOf("revenue", "loss"),
        ),

        // ── 생활 속 경제 ───────────────────────────────────────
        EconTerm(
            "rent", "월세", "rent", LIFE,
            "남의 집이나 공간을 빌려 쓰는 대가로 정해진 날에 내는 돈이에요.",
            "I pay my rent on the first day of the month.",
            "저는 매달 첫날에 월세를 내요.",
            listOf("cost-of-living", "income", "budget"),
        ),
        EconTerm(
            "tax", "세금", "tax", LIFE,
            "나라와 지방 정부가 공공 서비스에 쓰려고 국민과 회사로부터 걷는 돈이에요.",
            "Taxes pay for roads and schools.",
            "세금은 도로와 학교를 짓는 데 쓰여요.",
            listOf("income", "tariff"),
        ),
        EconTerm(
            "income", "소득", "income", LIFE,
            "일을 하거나 이자와 같은 방법으로 들어오는 돈이에요.",
            "Her income comes from her job.",
            "그녀의 소득은 직장에서 나와요.",
            listOf("tax", "budget", "savings"),
        ),
        EconTerm(
            "budget", "예산", "budget", LIFE,
            "들어올 돈과 쓸 돈을 미리 계획해서 적어 둔 거예요. 가정, 회사, 나라 모두 예산을 세워요.",
            "We made a budget for the trip.",
            "우리는 여행 예산을 짰어요.",
            listOf("income", "savings", "cost-of-living"),
        ),
        EconTerm(
            "savings", "저축", "savings", LIFE,
            "번 돈 가운데 바로 쓰지 않고 남겨 두는 돈이에요. 갑자기 돈이 필요할 때를 대비해요.",
            "Regular savings help in an emergency.",
            "꾸준한 저축은 비상시에 도움이 돼요.",
            listOf("income", "budget", "interest-rate"),
        ),
        EconTerm(
            "loan", "대출", "loan", LIFE,
            "은행 같은 곳에서 돈을 빌리는 거예요. 약속한 기간 안에 이자를 더해 갚아야 해요.",
            "He took out a loan to buy a house.",
            "그는 집을 사려고 대출을 받았어요.",
            listOf("interest-rate", "income", "budget"),
        ),

        // ── 경기·시장 ──────────────────────────────────────────
        EconTerm(
            "economy", "경제", "economy", MARKET,
            "사람들이 물건과 서비스를 만들고, 사고팔고, 쓰는 활동 전체예요.",
            "The economy affects our daily lives.",
            "경제는 우리의 일상에 영향을 줘요.",
            listOf("gdp", "market", "economic-growth"),
        ),
        EconTerm(
            "gdp", "국내총생산(GDP)", "GDP", MARKET,
            "한 나라 안에서 일정 기간 동안 새로 만들어진 물건과 서비스의 가치를 모두 더한 값이에요. 나라 경제의 크기를 재는 대표 지표예요.",
            "GDP shows the size of a country's economy.",
            "GDP는 한 나라 경제의 크기를 보여 줘요.",
            listOf("economy", "economic-growth", "recession"),
        ),
        EconTerm(
            "economic-growth", "경제 성장", "economic growth", MARKET,
            "한 나라가 만들어 내는 물건과 서비스가 시간이 지나면서 늘어나는 거예요.",
            "Economic growth can create new jobs.",
            "경제가 성장하면 새 일자리가 생길 수 있어요.",
            listOf("gdp", "recession", "unemployment"),
        ),
        EconTerm(
            "recession", "경기 침체", "recession", MARKET,
            "경제 활동이 한동안 눈에 띄게 줄어드는 시기예요. 일자리가 줄고 사람들이 쓰는 돈도 줄기 쉬워요.",
            "A recession often leads to job losses.",
            "경기 침체가 오면 일자리가 줄기도 해요.",
            listOf("economic-growth", "unemployment", "gdp"),
        ),
        EconTerm(
            "unemployment", "실업", "unemployment", MARKET,
            "일할 의사와 능력이 있는데도 일자리를 구하지 못한 상태예요.",
            "Unemployment rises when companies cut jobs.",
            "회사들이 일자리를 줄이면 실업이 늘어요.",
            listOf("recession", "economic-growth", "income"),
        ),
        EconTerm(
            "market", "시장", "market", MARKET,
            "사는 사람과 파는 사람이 만나 거래하는 곳이에요. 시장에서 수요와 공급이 만나 가격이 정해져요.",
            "The market is busy on weekends.",
            "시장은 주말에 붐벼요.",
            listOf("demand", "supply", "price"),
        ),
        EconTerm(
            "stock", "주식", "stock", MARKET,
            "회사의 주인 자격을 잘게 나눈 증서예요. 주식을 가진 사람을 주주라고 해요.",
            "A stock represents a small share of a company.",
            "주식은 회사의 작은 한 몫을 나타내요.",
            listOf("dividend", "market", "bond"),
        ),
        EconTerm(
            "bond", "채권", "bond", MARKET,
            "정부나 회사가 돈을 빌리면서 '정해진 날에 이자와 함께 갚겠다'고 약속해 쓴 증서예요.",
            "A bond is a promise to repay borrowed money.",
            "채권은 빌린 돈을 갚겠다는 약속이에요.",
            listOf("stock", "interest-rate", "loan"),
        ),
    )

    private val byId: Map<String, EconTerm> = terms.associateBy { it.id }

    /** 주제 목록(등장 순서 유지). */
    val topics: List<String> = terms.map { it.topic }.distinct()

    fun find(id: String): EconTerm? = byId[id]

    fun byTopic(topic: String): List<EconTerm> = terms.filter { it.topic == topic }

    /** 같은 날에는 언제 물어도 같은 용어. 날이 바뀌면 다음 용어로 넘어간다. */
    fun termOfDay(epochDay: Long): EconTerm =
        terms[Math.floorMod(epochDay, terms.size.toLong()).toInt()]
}
