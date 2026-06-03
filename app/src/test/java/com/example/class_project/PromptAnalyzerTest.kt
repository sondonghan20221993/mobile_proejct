package com.example.class_project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptAnalyzerTest {
    private val analyzer = PromptAnalyzer()

    @Test
    fun `repeated words trigger redundancy issue`() {
        val result = analyzer.analyze("이 앱 앱 앱 분석 분석 분석 결과를 정리해 줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `filler-heavy prompt triggers verbosity issue`() {
        val result = analyzer.analyze("정말 정말 매우 진짜 자세하고 길게 설명해 줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `short but explicit prompt is not treated as lack of scope`() {
        val result = analyzer.analyze("버그 원인 3개 리스트로 정리")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `underspecified prompt triggers lack of scope issue`() {
        val result = analyzer.analyze("이거 해줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `clear prompt keeps high efficiency score`() {
        val result = analyzer.analyze("안드로이드 앱 로그인 오류 원인을 3단계로 요약하고 해결 코드를 예시로 보여줘")

        assertTrue(result.issues.isEmpty())
        assertEquals(100, result.efficiencyScore)
        assertTrue(result.inputTokens > 0)
        assertTrue(result.outputTokens > 0)
        assertTrue(result.totalTokens >= result.inputTokens + result.outputTokens)
        assertTrue(result.estimatedCostUsd > 0.0)
    }

    @Test
    fun `technical domain keywords count as valid action and output signals`() {
        val result = analyzer.analyze("안드로이드 로그인 버그를 디버깅하고 xml 예시로 수정 방향을 보여줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `writing domain keywords count as valid action and output signals`() {
        val result = analyzer.analyze("기획 문서를 초안 형태의 체크리스트로 재작성해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `custom dictionary can extend recognized action keywords`() {
        val customDictionary = PromptKeywordDictionary.default().copy(
            actionKeywords = PromptKeywordDictionary.default().actionKeywords + "브레인스토밍"
        )
        val customAnalyzer = PromptAnalyzer(customDictionary)

        val result = customAnalyzer.analyze("신규 서비스 아이디어를 브레인스토밍 표로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }
}
