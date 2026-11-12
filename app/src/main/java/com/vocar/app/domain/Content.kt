package com.vocar.app.domain

/**
 * 설치에 포함되는 4주 · 20개 학습 묶음 (v1).
 * 묶음마다 출근용 4개(듣고 뜻 고르기, 따라 말하기, 상황 고르기, 변형 A)와
 * 퇴근용 4개(떠올리기, 회상 확인, 변형 B, 다시 말하기) 활동이 만들어진다. → 기본 활동 160개.
 * 네트워크 없이 전부 동작한다. 문항을 고치면 CONTENT_VERSION을 올린다.
 */
object Content {
    const val CONTENT_VERSION = 1

    private fun bundle(
        id: String,
        week: Int,
        topic: String,
        headline: String,
        situation: String,
        en: String,
        ko: String,
        wrongMeanings: List<String>,
        wrongChoices: List<WrongChoice>,
        note: String,
        variationA: Variation,
        variationB: Variation,
        recallCue: String,
        accepted: List<String> = emptyList(),
    ) = Bundle(
        id, week, topic, headline, situation, en, ko, listOf(en) + accepted,
        wrongMeanings, wrongChoices, note, variationA, variationB, recallCue,
    )

    private const val W1 = "인사·확인·부탁"
    private const val W2 = "시간·일정·약속"
    private const val W3 = "업무 상태·도움 요청"
    private const val W4 = "일상·주문·계획"

    val bundles: List<Bundle> = listOf(
        // ── 1주: 인사·확인·간단한 부탁 ─────────────────────────────
        bundle(
            id = "w1d1", week = 1, topic = W1,
            headline = "오늘은 다시 말해 달라고 부탁해요",
            situation = "회의에서 상대의 말을 잘 듣지 못했어요.",
            en = "Could you say that again?",
            ko = "다시 한번 말씀해 주시겠어요?",
            wrongMeanings = listOf("조금 더 천천히 말씀해 주시겠어요?", "그게 무슨 뜻인가요?"),
            wrongChoices = listOf(
                WrongChoice("Could you speak more slowly?", "더 천천히 말해 달라는 부탁"),
                WrongChoice("What do you mean?", "무슨 뜻인지 묻는 말"),
            ),
            note = "다시 들려 달라고 할 때는 say that again을 써요.",
            variationA = Variation(
                "말이 너무 빨라요. 천천히 말해 달라고 부탁해요.",
                "Could you speak more slowly?", "조금 더 천천히 말씀해 주시겠어요?",
            ),
            variationB = Variation(
                "이름을 받아 적어야 해요. 철자를 불러 달라고 부탁해요.",
                "Could you spell that for me?", "철자를 불러 주시겠어요?",
            ),
            recallCue = "상대 말을 못 들었어요. 다시 말해 달라고 어떻게 부탁할까요?",
            accepted = listOf("Can you say that again?", "Could you repeat that?"),
        ),
        bundle(
            id = "w1d2", week = 1, topic = W1,
            headline = "오늘은 가볍게 안부를 물어요",
            situation = "복도에서 동료와 마주쳤어요.",
            en = "How's your day going?",
            ko = "오늘 하루 어떠세요?",
            wrongMeanings = listOf("오늘 뭐 하세요?", "어디 가세요?"),
            wrongChoices = listOf(
                WrongChoice("What are you doing today?", "오늘 할 일을 묻는 말"),
                WrongChoice("Where are you going?", "어디 가는지 묻는 말"),
            ),
            note = "How's your day going?은 지금 하루가 어떤지 가볍게 묻는 인사예요.",
            variationA = Variation(
                "월요일 아침이에요. 주말이 어땠는지 물어요.",
                "How was your weekend?", "주말 잘 보내셨어요?",
            ),
            variationB = Variation(
                "오랜만에 만났어요. 그동안 어떻게 지냈는지 물어요.",
                "How have you been?", "그동안 어떻게 지내셨어요?",
            ),
            recallCue = "동료와 마주쳤어요. 오늘 하루가 어떤지 어떻게 물을까요?",
            accepted = listOf("How is your day going?"),
        ),
        bundle(
            id = "w1d3", week = 1, topic = W1,
            headline = "오늘은 잠깐 시간이 되는지 물어요",
            situation = "동료에게 잠깐 물어볼 것이 있어요.",
            en = "Do you have a minute?",
            ko = "잠깐 시간 괜찮으세요?",
            wrongMeanings = listOf("지금 몇 시예요?", "시간이 얼마나 걸려요?"),
            wrongChoices = listOf(
                WrongChoice("What time is it?", "지금 시각을 묻는 말"),
                WrongChoice("How long does it take?", "걸리는 시간을 묻는 말"),
            ),
            note = "a minute은 정확히 1분이 아니라 ‘잠깐’이라는 뜻으로 써요.",
            variationA = Variation(
                "말을 걸기 전에 지금 바쁜지 먼저 확인해요.",
                "Are you busy right now?", "지금 바쁘세요?",
            ),
            variationB = Variation(
                "지금은 어려워 보여요. 나중에 이야기할 수 있는지 물어요.",
                "Can we talk later?", "나중에 이야기할 수 있을까요?",
            ),
            recallCue = "동료에게 물어볼 게 있어요. 잠깐 시간이 되는지 어떻게 물을까요?",
            accepted = listOf("Do you have a second?", "Have you got a minute?"),
        ),
        bundle(
            id = "w1d4", week = 1, topic = W1,
            headline = "오늘은 도와 달라고 부탁해요",
            situation = "혼자 하기 어려운 일이 있어요.",
            en = "Could you help me with this?",
            ko = "이것 좀 도와주시겠어요?",
            wrongMeanings = listOf("제가 도와드릴까요?", "이걸 저에게 주시겠어요?"),
            wrongChoices = listOf(
                WrongChoice("Can I help you?", "내가 돕겠다고 제안하는 말"),
                WrongChoice("Could you give this to me?", "물건을 달라는 부탁"),
            ),
            note = "help me with 뒤에 도움이 필요한 일을 붙여요.",
            variationA = Variation(
                "이번에는 반대로, 내가 돕겠다고 제안해요.",
                "Can I help you with that?", "그거 도와드릴까요?",
            ),
            variationB = Variation(
                "도움을 받았어요. 고마움을 전해요.",
                "Thanks for your help.", "도와주셔서 고마워요.",
            ),
            recallCue = "혼자 하기 어려운 일이 있어요. 도와 달라고 어떻게 부탁할까요?",
            accepted = listOf("Can you help me with this?"),
        ),
        bundle(
            id = "w1d5", week = 1, topic = W1,
            headline = "오늘은 확인해 보겠다고 말해요",
            situation = "바로 답하기 어려운 질문을 받았어요.",
            en = "Let me check.",
            ko = "확인해 볼게요.",
            wrongMeanings = listOf("잘 모르겠어요.", "생각해 볼게요."),
            wrongChoices = listOf(
                WrongChoice("I don't know.", "모른다고만 말하는 표현"),
                WrongChoice("Let me think.", "생각할 시간을 달라는 말"),
            ),
            note = "모를 때 I don't know로 끝내기보다 Let me check이 더 부드러워요.",
            variationA = Variation(
                "확인한 뒤 다시 알려 주겠다고 말해요.",
                "I'll get back to you.", "확인하고 다시 알려 드릴게요.",
            ),
            variationB = Variation(
                "지금 바로 확인하겠다고 말해요.",
                "Let me check right now.", "지금 바로 확인해 볼게요.",
            ),
            recallCue = "바로 답하기 어려운 질문을 받았어요. 확인해 보겠다고 어떻게 말할까요?",
            accepted = listOf("I'll check.", "Let me check on that."),
        ),

        // ── 2주: 시간·일정·약속 ─────────────────────────────────
        bundle(
            id = "w2d1", week = 2, topic = W2,
            headline = "오늘은 만날 시간을 제안해요",
            situation = "회의 시간을 정해야 해요.",
            en = "Could we meet at three?",
            ko = "3시에 만날 수 있을까요?",
            wrongMeanings = listOf("사흘 뒤에 만날 수 있을까요?", "세 시간 동안 만날 수 있을까요?"),
            wrongChoices = listOf(
                WrongChoice("Could we meet in three days?", "사흘 뒤에 만나자는 말"),
                WrongChoice("Could we meet for three hours?", "세 시간 동안 만나자는 말"),
            ),
            note = "시각 앞에는 at을 써요. at three는 ‘3시에’예요.",
            variationA = Variation(
                "3시는 어렵대요. 4시로 바꿔 제안해요.",
                "Could we meet at four?", "4시에 만날 수 있을까요?",
            ),
            variationB = Variation(
                "오늘은 어려워요. 내일 아침에 만나자고 제안해요.",
                "Could we meet tomorrow morning?", "내일 아침에 만날 수 있을까요?",
            ),
            recallCue = "회의 시간을 정해야 해요. 3시에 만나자고 어떻게 제안할까요?",
            accepted = listOf("Can we meet at three?", "Could we meet at 3?"),
        ),
        bundle(
            id = "w2d2", week = 2, topic = W2,
            headline = "오늘은 편한 시간을 물어요",
            situation = "약속을 잡으려고 상대의 일정을 확인해요.",
            en = "What time works for you?",
            ko = "몇 시가 괜찮으세요?",
            wrongMeanings = listOf("몇 시에 일하세요?", "지금 몇 시예요?"),
            wrongChoices = listOf(
                WrongChoice("What time do you work?", "근무 시간을 묻는 말"),
                WrongChoice("What time is it now?", "지금 시각을 묻는 말"),
            ),
            note = "여기서 work는 ‘일하다’가 아니라 ‘(일정이) 괜찮다’는 뜻이에요.",
            variationA = Variation(
                "시간 대신 편한 요일을 물어요.",
                "What day works for you?", "무슨 요일이 괜찮으세요?",
            ),
            variationB = Variation(
                "금요일이 괜찮은지 물어요.",
                "Does Friday work for you?", "금요일 괜찮으세요?",
            ),
            recallCue = "약속을 잡으려고 해요. 몇 시가 괜찮은지 어떻게 물을까요?",
            accepted = listOf("What time is good for you?"),
        ),
        bundle(
            id = "w2d3", week = 2, topic = W2,
            headline = "오늘은 조금 늦는다고 알려요",
            situation = "약속 시간에 조금 늦게 됐어요.",
            en = "I'm running a little late.",
            ko = "조금 늦을 것 같아요.",
            wrongMeanings = listOf("조금 일찍 도착했어요.", "오늘은 조금 바빠요."),
            wrongChoices = listOf(
                WrongChoice("I'm a little early.", "조금 일찍 왔다는 말"),
                WrongChoice("I'm a little busy today.", "오늘 바쁘다는 말"),
            ),
            note = "running late는 달리는 것이 아니라 ‘늦어지고 있다’는 뜻이에요.",
            variationA = Variation(
                "얼마나 늦는지 알려요. 10분 늦는다고 말해요.",
                "I'll be ten minutes late.", "10분 늦을 것 같아요.",
            ),
            variationB = Variation(
                "거의 다 왔다고 알려요.",
                "I'm almost there.", "거의 다 왔어요.",
            ),
            recallCue = "약속에 늦게 됐어요. 조금 늦는다고 어떻게 알릴까요?",
            accepted = listOf("I'm running late.", "I'm going to be a little late."),
        ),
        bundle(
            id = "w2d4", week = 2, topic = W2,
            headline = "오늘은 일정을 다시 잡자고 말해요",
            situation = "약속한 시간에 갈 수 없게 됐어요.",
            en = "Can we reschedule?",
            ko = "일정을 다시 잡을 수 있을까요?",
            wrongMeanings = listOf("일정을 취소할 수 있을까요?", "지금 시작할 수 있을까요?"),
            wrongChoices = listOf(
                WrongChoice("Can we cancel?", "아예 취소하자는 말"),
                WrongChoice("Can we start now?", "지금 시작하자는 말"),
            ),
            note = "reschedule은 취소가 아니라 날짜나 시간을 다시 정한다는 뜻이에요.",
            variationA = Variation(
                "다음 주로 옮기자고 제안해요.",
                "Can we move it to next week?", "다음 주로 옮길 수 있을까요?",
            ),
            variationB = Variation(
                "30분만 미루자고 제안해요.",
                "Can we push it back thirty minutes?", "30분만 미룰 수 있을까요?",
            ),
            recallCue = "약속한 시간에 갈 수 없어요. 일정을 다시 잡자고 어떻게 말할까요?",
            accepted = listOf("Could we reschedule?"),
        ),
        bundle(
            id = "w2d5", week = 2, topic = W2,
            headline = "오늘은 다음에 보자고 인사해요",
            situation = "금요일 퇴근길에 동료와 인사해요.",
            en = "See you on Monday.",
            ko = "월요일에 봬요.",
            wrongMeanings = listOf("월요일에 봤어요.", "월요일은 쉬어요."),
            wrongChoices = listOf(
                WrongChoice("I saw you on Monday.", "월요일에 봤다는 지난 일"),
                WrongChoice("I'm off on Monday.", "월요일에 쉰다는 말"),
            ),
            note = "요일 앞에는 on을 써요. on Monday는 ‘월요일에’예요.",
            variationA = Variation(
                "내일 다시 만나는 사람에게 인사해요.",
                "See you tomorrow.", "내일 봬요.",
            ),
            variationB = Variation(
                "주말 잘 보내라고 인사해요.",
                "Have a good weekend.", "주말 잘 보내세요.",
            ),
            recallCue = "금요일 퇴근길이에요. 월요일에 보자고 어떻게 인사할까요?",
            accepted = listOf("See you Monday."),
        ),

        // ── 3주: 업무 상태·도움 요청 ──────────────────────────────
        bundle(
            id = "w3d1", week = 3, topic = W3,
            headline = "오늘은 언제 보낼지 말해요",
            situation = "자료를 언제 줄 수 있는지 질문을 받았어요.",
            en = "I'll send it this afternoon.",
            ko = "오늘 오후에 보내 드릴게요.",
            wrongMeanings = listOf("오늘 오후에 이미 보냈어요.", "오늘 오후에 볼게요."),
            wrongChoices = listOf(
                WrongChoice("I sent it this afternoon.", "이미 보냈다는 지난 일"),
                WrongChoice("I'll see it this afternoon.", "오후에 보겠다는 말"),
            ),
            note = "앞으로 할 일을 약속할 때는 I'll(I will)로 시작해요.",
            variationA = Variation(
                "오늘은 어려워요. 내일 아침에 보내겠다고 말해요.",
                "I'll send it tomorrow morning.", "내일 아침에 보내 드릴게요.",
            ),
            variationB = Variation(
                "어떻게 보낼지 알려요. 이메일로 보내겠다고 말해요.",
                "I'll send it by email.", "이메일로 보내 드릴게요.",
            ),
            recallCue = "자료를 언제 줄 수 있냐고 물어요. 오늘 오후에 보내겠다고 어떻게 말할까요?",
            accepted = listOf("I will send it this afternoon."),
        ),
        bundle(
            id = "w3d2", week = 3, topic = W3,
            headline = "오늘은 하고 있다고 알려요",
            situation = "맡은 일이 어떻게 되어 가는지 질문을 받았어요.",
            en = "I'm working on it.",
            ko = "지금 하고 있어요.",
            wrongMeanings = listOf("이미 끝냈어요.", "아직 시작하지 않았어요."),
            wrongChoices = listOf(
                WrongChoice("I'm done with it.", "이미 끝냈다는 말"),
                WrongChoice("I haven't started yet.", "아직 시작하지 않았다는 말"),
            ),
            note = "work on은 ‘~을 진행하고 있다’는 뜻이에요.",
            variationA = Variation(
                "거의 다 됐다고 알려요.",
                "I'm almost done.", "거의 다 됐어요.",
            ),
            variationB = Variation(
                "방금 끝냈다고 알려요.",
                "I just finished it.", "방금 끝냈어요.",
            ),
            recallCue = "일이 어떻게 되어 가냐고 물어요. 지금 하고 있다고 어떻게 말할까요?",
            accepted = listOf("I am working on it."),
        ),
        bundle(
            id = "w3d3", week = 3, topic = W3,
            headline = "오늘은 한번 봐 달라고 부탁해요",
            situation = "작성한 문서를 검토받고 싶어요.",
            en = "Could you take a look at this?",
            ko = "이것 좀 봐 주시겠어요?",
            wrongMeanings = listOf("이것 좀 가져가 주시겠어요?", "이것 좀 찾아 주시겠어요?"),
            wrongChoices = listOf(
                WrongChoice("Could you take this?", "가져가 달라는 부탁"),
                WrongChoice("Could you look for this?", "찾아 달라는 부탁"),
            ),
            note = "take a look at은 ‘한번 살펴보다’예요. look for(찾다)와 달라요.",
            variationA = Variation(
                "급하지 않아요. 시간 될 때 봐 달라고 부탁해요.",
                "Could you take a look when you have time?", "시간 되실 때 봐 주시겠어요?",
            ),
            variationB = Variation(
                "보낸 이메일을 봐 달라고 부탁해요.",
                "Could you take a look at my email?", "제 이메일 좀 봐 주시겠어요?",
            ),
            recallCue = "문서를 검토받고 싶어요. 한번 봐 달라고 어떻게 부탁할까요?",
            accepted = listOf("Can you take a look at this?", "Could you have a look at this?"),
        ),
        bundle(
            id = "w3d4", week = 3, topic = W3,
            headline = "오늘은 짧게 질문하겠다고 말해요",
            situation = "회의 중에 짧게 물어보고 싶어요.",
            en = "I have a quick question.",
            ko = "간단한 질문이 하나 있어요.",
            wrongMeanings = listOf("질문이 없어요.", "빨리 대답해 주세요."),
            wrongChoices = listOf(
                WrongChoice("I have no questions.", "질문이 없다는 말"),
                WrongChoice("I have a quick answer.", "짧은 답이 있다는 말"),
            ),
            note = "quick을 붙이면 ‘오래 걸리지 않는다’는 배려가 담겨요.",
            variationA = Variation(
                "질문 대신 제안이 하나 있다고 말해요.",
                "I have a suggestion.", "제안이 하나 있어요.",
            ),
            variationB = Variation(
                "질문해도 되는지 먼저 물어요.",
                "Can I ask you something?", "뭐 하나 여쭤봐도 될까요?",
            ),
            recallCue = "회의 중에 짧게 물어보고 싶어요. 간단한 질문이 있다고 어떻게 말할까요?",
            accepted = listOf("I've got a quick question."),
        ),
        bundle(
            id = "w3d5", week = 3, topic = W3,
            headline = "오늘은 필요하면 알려 달라고 말해요",
            situation = "동료에게 자료를 건네며 마무리 인사를 해요.",
            en = "Let me know if you need anything.",
            ko = "필요한 게 있으면 알려 주세요.",
            wrongMeanings = listOf("뭐가 필요한지 모르겠어요.", "뭘 하셨는지 말해 주세요."),
            wrongChoices = listOf(
                WrongChoice("I don't know what you need.", "무엇이 필요한지 모르겠다는 말"),
                WrongChoice("Tell me what you did.", "무엇을 했는지 말해 달라는 말"),
            ),
            note = "Let me know는 ‘알려 주세요’를 부드럽게 말하는 표현이에요.",
            variationA = Variation(
                "질문이 있으면 알려 달라고 말해요.",
                "Let me know if you have any questions.", "질문 있으면 알려 주세요.",
            ),
            variationB = Variation(
                "끝나면 알려 달라고 말해요.",
                "Let me know when you're done.", "끝나면 알려 주세요.",
            ),
            recallCue = "자료를 건네며 인사해요. 필요한 게 있으면 알려 달라고 어떻게 말할까요?",
            accepted = listOf("Let me know if you need something."),
        ),

        // ── 4주: 일상·주문·계획 ─────────────────────────────────
        bundle(
            id = "w4d1", week = 4, topic = W4,
            headline = "오늘은 커피를 주문해요",
            situation = "카페 계산대 앞에 섰어요.",
            en = "I'd like a coffee, please.",
            ko = "커피 한 잔 주세요.",
            wrongMeanings = listOf("저는 커피를 좋아해요.", "커피 드시겠어요?"),
            wrongChoices = listOf(
                WrongChoice("I like coffee.", "커피를 좋아한다는 말"),
                WrongChoice("Would you like a coffee?", "상대에게 커피를 권하는 말"),
            ),
            note = "I'd like는 I want보다 공손한 주문 표현이에요. I like(좋아해요)와 달라요.",
            variationA = Variation(
                "주문을 차로 바꿔요.",
                "I'd like a tea, please.", "차 한 잔 주세요.",
            ),
            variationB = Variation(
                "물 한 잔을 부탁해요.",
                "I'd like a glass of water, please.", "물 한 잔 주세요.",
            ),
            recallCue = "카페에서 주문할 차례예요. 커피 한 잔을 어떻게 주문할까요?",
            accepted = listOf("I would like a coffee, please.", "I'd like a coffee."),
        ),
        bundle(
            id = "w4d2", week = 4, topic = W4,
            headline = "오늘은 포장해 달라고 말해요",
            situation = "주문한 음식을 가져가고 싶어요.",
            en = "Can I get this to go?",
            ko = "이거 포장해 주실 수 있나요?",
            wrongMeanings = listOf("여기서 먹고 갈게요.", "이건 어디에 두나요?"),
            wrongChoices = listOf(
                WrongChoice("I'll have it here.", "매장에서 먹겠다는 말"),
                WrongChoice("Where does this go?", "이것을 어디에 두는지 묻는 말"),
            ),
            note = "to go는 ‘가져갈 것’, 즉 포장이라는 뜻이에요.",
            variationA = Variation(
                "이번에는 매장에서 먹겠다고 말해요.",
                "For here, please.", "여기서 먹을게요.",
            ),
            variationB = Variation(
                "식사를 마쳤어요. 계산서를 부탁해요.",
                "Can I get the check, please?", "계산서 주시겠어요?",
            ),
            recallCue = "음식을 가져가고 싶어요. 포장해 달라고 어떻게 말할까요?",
            accepted = listOf("Can I have this to go?", "Could I get this to go?"),
        ),
        bundle(
            id = "w4d3", week = 4, topic = W4,
            headline = "오늘은 가격을 물어요",
            situation = "가게에서 마음에 드는 물건을 발견했어요.",
            en = "How much is this?",
            ko = "이거 얼마예요?",
            wrongMeanings = listOf("이게 뭐예요?", "이거 몇 개 있어요?"),
            wrongChoices = listOf(
                WrongChoice("What is this?", "이것이 무엇인지 묻는 말"),
                WrongChoice("How many are there?", "개수를 묻는 말"),
            ),
            note = "가격은 How much, 개수는 How many로 물어요.",
            variationA = Variation(
                "조금 떨어져 있는 물건을 가리키며 가격을 물어요.",
                "How much is that one?", "저건 얼마예요?",
            ),
            variationB = Variation(
                "여러 개를 가리키며 가격을 물어요.",
                "How much are these?", "이것들은 얼마예요?",
            ),
            recallCue = "마음에 드는 물건이 있어요. 가격을 어떻게 물을까요?",
            accepted = listOf("How much is it?", "How much does this cost?"),
        ),
        bundle(
            id = "w4d4", week = 4, topic = W4,
            headline = "오늘은 주말 계획을 말해요",
            situation = "동료가 주말에 뭐 하냐고 물었어요.",
            en = "I'm going to stay home this weekend.",
            ko = "이번 주말에는 집에 있을 거예요.",
            wrongMeanings = listOf("지난 주말에는 집에 있었어요.", "이번 주말에 집에 갈 거예요."),
            wrongChoices = listOf(
                WrongChoice("I stayed home last weekend.", "지난 주말 이야기"),
                WrongChoice("I'm going home this weekend.", "주말에 집으로 간다는 말"),
            ),
            note = "미리 정한 계획은 be going to로 말해요.",
            variationA = Variation(
                "주말에 친구를 만날 계획이라고 말해요.",
                "I'm going to meet a friend this weekend.", "이번 주말에 친구를 만날 거예요.",
            ),
            variationB = Variation(
                "주말이 아니라 오늘 저녁 계획이에요. 집에 있을 거라고 말해요.",
                "I'm going to stay home tonight.", "오늘 저녁에는 집에 있을 거예요.",
            ),
            recallCue = "주말 계획을 물어요. 집에 있을 거라고 어떻게 말할까요?",
            accepted = listOf("I'm staying home this weekend.", "I am going to stay home this weekend."),
        ),
        bundle(
            id = "w4d5", week = 4, topic = W4,
            headline = "오늘은 추천을 부탁해요",
            situation = "식당에서 메뉴를 고르기 어려워요.",
            en = "What do you recommend?",
            ko = "뭘 추천하세요?",
            wrongMeanings = listOf("뭘 주문하셨어요?", "뭘 원하세요?"),
            wrongChoices = listOf(
                WrongChoice("What did you order?", "무엇을 주문했는지 묻는 말"),
                WrongChoice("What do you want?", "무엇을 원하는지 묻는 말"),
            ),
            note = "고르기 어려울 때 직원에게 가볍게 쓸 수 있는 질문이에요.",
            variationA = Variation(
                "가장 인기 있는 메뉴가 무엇인지 물어요.",
                "What's the most popular dish?", "가장 인기 있는 메뉴가 뭐예요?",
            ),
            variationB = Variation(
                "추천받은 것으로 주문해요.",
                "I'll have that, please.", "그걸로 주세요.",
            ),
            recallCue = "메뉴를 고르기 어려워요. 추천해 달라고 어떻게 물을까요?",
            accepted = listOf("What would you recommend?"),
        ),
    )

    private val byId: Map<String, Bundle> = bundles.associateBy { it.id }

    fun find(id: String): Bundle? = byId[id]

    /** 목표에 따라 주차 순서만 바꾼다. 일상·여행은 주문·계획(4주차)을 앞당긴다. */
    fun courseOrder(goal: Goal): List<Bundle> {
        val weeks = when (goal) {
            Goal.DAILY -> listOf(1, 4, 2, 3)
            Goal.WORK -> listOf(1, 2, 3, 4)
        }
        return weeks.flatMap { w -> bundles.filter { it.week == w } }
    }
}
