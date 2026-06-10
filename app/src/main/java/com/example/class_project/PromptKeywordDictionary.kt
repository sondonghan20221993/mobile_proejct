package com.example.class_project

data class PromptKeywordDictionary(
    val fillerPhrases: List<String>,
    val outputFormatKeywords: List<String>,
    val constraintKeywords: List<String>,
    val contextKeywords: List<String>,
    val actionKeywords: List<String>,
    val connectorKeywords: List<String>,
    val synonymGroups: Map<String, String> = emptyMap(),
    val conditionalFillerPhrases: Set<String> = emptySet()
) {
    companion object {
        fun default(): PromptKeywordDictionary {
            return PromptKeywordDictionary(
                fillerPhrases = commonFillers + politeFillers + explanatoryFillers + informalFillers,
                outputFormatKeywords = commonOutputFormats + technicalOutputFormats + writingOutputFormats,
                constraintKeywords = commonConstraints + formattingConstraints + qualityConstraints,
                contextKeywords = commonContexts + technicalContexts + businessContexts + professionalContexts,
                actionKeywords = commonActions + technicalActions + writingActions + planningActions + colloquialActions + analysisActions,
                connectorKeywords = commonConnectors,
                synonymGroups = defaultSynonyms,
                conditionalFillerPhrases = politeFillers.toSet()
            )
        }

        private val commonFillers = listOf(
            "정말",
            "정말로",
            "매우",
            "진짜",
            "되게",
            "아주"
        )

        private val politeFillers = listOf(
            "가능하면",
            "혹시 가능하면",
            "괜찮다면",
            "되도록이면"
        )

        private val explanatoryFillers = listOf(
            "어떻게 보면",
            "상당히",
            "굳이 말하면",
            "약간",
            "조금은"
        )

        private val informalFillers = listOf(
            "그냥",  // 그냥 해줘
            "제발",  // 제발 도와줘
            "살짝"   // 살짝 수정해줘
        )

        private val commonOutputFormats = listOf(
            "표",
            "리스트",
            "목록",
            "단계",
            "문단",
            "예시"
        )

        private val technicalOutputFormats = listOf(
            "코드",
            "json",
            "markdown",
            "xml",
            "api",
            "함수",
            "클래스",
            "다이어그램",
            "그래프",
            "순서도"
        )

        private val writingOutputFormats = listOf(
            "개요",
            "요약문",
            "요약본",
            "체크리스트",
            "보고서",
            "초안",
            "bullet",
            "개조식",
            "슬라이드",   // PPT/발표 자료
            "타임라인"    // 시간 순서 정리
        )

        private val commonConstraints = listOf(
            "이내",
            "제한",
            "형식",
            "반드시",
            "제외",
            "포함"
        )

        private val formattingConstraints = listOf(
            "글자",
            "줄",
            "길이",
            "문장",
            "단어",
            "분량"
        )

        private val qualityConstraints = listOf(
            "정확",
            "간단",
            "자세",
            "친절",
            "테스트",
            "검증"
        )

        private val commonContexts = listOf(
            "대상",
            "상황",
            "배경",
            "맥락",
            "프로젝트",
            "데이터"
        )

        private val technicalContexts = listOf(
            "앱",
            "안드로이드",
            "코드",
            "서비스",
            "서버",
            "클라이언트",
            "버그",
            "로그인",
            "api",
            "db"
        )

        private val businessContexts = listOf(
            "사용자",
            "고객",
            "기획",
            "운영",
            "마케팅",
            "문서",
            "논문",
            "회사",
            "팀"
        )

        private val professionalContexts = listOf(
            "법",      // 법률 관련
            "법률",
            "계약",
            "의료",    // 의료/헬스케어
            "진료",
            "환자",
            "금융",    // 금융/투자
            "투자",
            "주식",
            "보안",    // 사이버보안
            "취약점",
            "게임",    // 게임 개발/콘텐츠
            "교육",    // 교육
            "학생",
            "수업"
        )

        private val commonActions = listOf(
            "분석",
            "정리",
            "설명",
            "작성",
            "비교",
            "추천",
            "요약",
            "수정",
            "변환"
        )

        private val technicalActions = listOf(
            "구현",
            "리팩터링",
            "디버깅",
            "최적화",
            "테스트",
            "설계",
            "점검",
            "검토"
        )

        private val writingActions = listOf(
            "초안",
            "번역",
            "교정",
            "재작성",
            "압축",
            "확장"
        )

        private val planningActions = listOf(
            "우선순위",
            "분류",
            "계획",
            "도출",
            "추출",
            "구분",    // "항목을 구분해줘"
            "분리",    // "역할별로 분리해줘"
            "결합",    // "두 데이터를 결합해줘"
            "검색",    // "관련 정보를 검색해줘"
            "학습"     // "모델을 학습시켜줘" / "내용을 학습해줘"
        )

        private val analysisActions = listOf(
            "시각화",  // 데이터를 시각화해줘
            "계산",    // 비용을 계산해줘
            "예측",    // 매출을 예측해줘
            "평가",    // 성능을 평가해줘
            "파악",    // 원인을 파악해줘
            "그려"     // 다이어그램을 그려줘
        )

        private val colloquialActions = listOf(
            "알려",   // 알려줘, 알려주세요
            "보여",   // 보여줘, 보여주세요
            "만들어", // 만들어줘, 만들어주세요
            "찾아",   // 찾아줘, 찾아봐줘
            "제안",   // 제안해줘
            "생성",   // 생성해줘
            "나열"    // 나열해줘
        )

        private val defaultSynonyms = mapOf(
            "python" to "파이썬", "py" to "파이썬", "파이썬" to "파이썬",
            "javascript" to "자바스크립트", "js" to "자바스크립트", "자바스크립트" to "자바스크립트",
            "typescript" to "타입스크립트", "ts" to "타입스크립트", "타입스크립트" to "타입스크립트",
            "kotlin" to "코틀린", "코틀린" to "코틀린",
            "java" to "자바", "자바" to "자바",
            "android" to "안드로이드", "안드로이드" to "안드로이드",
            "gpt" to "gpt", "chatgpt" to "gpt",
            "ml" to "머신러닝", "머신러닝" to "머신러닝",
            "dl" to "딥러닝", "딥러닝" to "딥러닝",
            "ai" to "ai",
            "react" to "react", "리액트" to "react",
            "vue" to "vue", "뷰" to "vue",
            "css" to "css",
            "html" to "html",
            "swift" to "swift",
            "flutter" to "flutter",
            "database" to "데이터베이스", "데이터베이스" to "데이터베이스"
        )

        private val commonConnectors = listOf(
            "그리고",
            "또한",
            "그리고 또",
            "그 다음",
            "추가로",
            "게다가",
            "아울러",
            "더불어"   // "더불어 예시도 들어줘"
        )
    }
}
