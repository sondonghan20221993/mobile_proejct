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
                fillerPhrases = commonFillers + politeFillers + explanatoryFillers,
                outputFormatKeywords = commonOutputFormats + technicalOutputFormats + writingOutputFormats,
                constraintKeywords = commonConstraints + formattingConstraints + qualityConstraints,
                contextKeywords = commonContexts + technicalContexts + businessContexts,
                actionKeywords = commonActions + technicalActions + writingActions + planningActions + colloquialActions,
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
            "클래스"
        )

        private val writingOutputFormats = listOf(
            "개요",
            "요약문",
            "체크리스트",
            "보고서",
            "초안",
            "bullet",
            "개조식"
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
            "문서"
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
            "추출"
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
            "python" to "파이썬",
            "파이썬" to "파이썬",
            "javascript" to "자바스크립트",
            "js" to "자바스크립트",
            "자바스크립트" to "자바스크립트",
            "kotlin" to "코틀린",
            "코틀린" to "코틀린",
            "java" to "자바",
            "자바" to "자바",
            "android" to "안드로이드",
            "안드로이드" to "안드로이드",
            "gpt" to "gpt",
            "chatgpt" to "gpt",
            "ml" to "머신러닝",
            "머신러닝" to "머신러닝",
            "react" to "react",
            "리액트" to "react",
            "vue" to "vue",
            "뷰" to "vue"
        )

        private val commonConnectors = listOf(
            "그리고",
            "또한",
            "그리고 또",
            "그 다음",
            "추가로"
        )
    }
}
