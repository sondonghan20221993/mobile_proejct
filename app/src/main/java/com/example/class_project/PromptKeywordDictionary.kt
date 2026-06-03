package com.example.class_project

data class PromptKeywordDictionary(
    val fillerPhrases: List<String>,
    val outputFormatKeywords: List<String>,
    val constraintKeywords: List<String>,
    val contextKeywords: List<String>,
    val actionKeywords: List<String>,
    val connectorKeywords: List<String>
) {
    companion object {
        fun default(): PromptKeywordDictionary {
            return PromptKeywordDictionary(
                fillerPhrases = commonFillers + politeFillers + explanatoryFillers,
                outputFormatKeywords = commonOutputFormats + technicalOutputFormats + writingOutputFormats,
                constraintKeywords = commonConstraints + formattingConstraints + qualityConstraints,
                contextKeywords = commonContexts + technicalContexts + businessContexts,
                actionKeywords = commonActions + technicalActions + writingActions + planningActions,
                connectorKeywords = commonConnectors
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
            "bullet"
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

        private val commonConnectors = listOf(
            "그리고",
            "또한",
            "그리고 또",
            "그 다음",
            "추가로"
        )
    }
}
