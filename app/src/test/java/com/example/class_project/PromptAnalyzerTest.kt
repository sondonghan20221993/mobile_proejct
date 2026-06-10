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

    // ── 중복(REDUNDANCY) ─────────────────────────────────────────────────────

    @Test
    fun `naturally repeated keyword four times triggers redundancy`() {
        // 실제 유저가 같은 주제를 여러 번 쓰는 패턴
        val result = analyzer.analyze("파이썬 파이썬 파이썬 코드 파이썬 예제 보여줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `duplicated sentence triggers redundancy`() {
        val result = analyzer.analyze("코드 리뷰해줘. 코드 리뷰해줘.")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── 장황함(VERBOSITY) ────────────────────────────────────────────────────

    @Test
    fun `two polite filler phrases trigger verbosity`() {
        // "혹시 가능하면", "괜찮다면" → fillerPhrases 2개 이상
        val result = analyzer.analyze("혹시 가능하면 괜찮다면 이 내용 좀 요약해줄 수 있을까요")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `two distinct connector keywords trigger verbosity`() {
        // "그리고", "또한" → connectorKeywords 2종류 이상
        val result = analyzer.analyze("설치 방법 알려줘 그리고 사용법도 알려줘 그리고 예제도 줘 또한 에러 해결법도 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── 범위 부족(LACK_OF_SCOPE) ─────────────────────────────────────────────

    @Test
    fun `single-word vague request triggers scope issue with short message`() {
        val result = analyzer.analyze("도와줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `ambiguous short prompt without action or format triggers scope issue`() {
        val result = analyzer.analyze("뭔가 좋은 거 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `context-only prompt without action verb triggers scope issue`() {
        // "코드" → hasContext=true, 하지만 action·outputFormat 없음 → missing 3개
        val result = analyzer.analyze("코드 봐줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 좋은 프롬프트 ─────────────────────────────────────────────────────────

    @Test
    fun `technical analysis with step format is fully clean`() {
        // action: "분석", outputFormat: "단계", context: tokens >= 8
        val result = analyzer.analyze("이 파이썬 함수의 시간복잡도를 분석하고 최적화 방법을 3단계로 알려줘")

        assertTrue(result.issues.isEmpty())
        assertEquals(100, result.efficiencyScore)
    }

    @Test
    fun `writing prompt with character constraint avoids scope issue`() {
        // action: "작성", constraint: "이내", context: tokens >= 8
        val result = analyzer.analyze("스타트업 투자 유치 이메일을 200자 이내 한국어로 작성해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `list format summary prompt is fully clean`() {
        // action: "정리", outputFormat: "목록", context: tokens >= 8
        val result = analyzer.analyze("React 컴포넌트 성능 최적화 방법을 초보자 기준으로 단계별 목록으로 정리해줘")

        assertTrue(result.issues.isEmpty())
        assertEquals(100, result.efficiencyScore)
    }

    // ── 점수 검증 ─────────────────────────────────────────────────────────────

    @Test
    fun `redundancy only gives score 88`() {
        // 좋은 구조(action+format+context) 유지하면서 파이썬만 3번 반복
        val result = analyzer.analyze("Python 코드에서 파이썬 파이썬 파이썬 변수명을 리스트로 정리해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertEquals(88, result.efficiencyScore)
    }

    @Test
    fun `redundancy and verbosity combined drops score to 80 or below`() {
        val result = analyzer.analyze("정말 정말 매우 진짜 이 코드 코드 코드를 설명 설명 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
        assertTrue(result.efficiencyScore <= 80)
    }

    // ── 중복 임계값 경계 ──────────────────────────────────────────────────────

    @Test
    fun `token appearing exactly twice does not trigger redundancy`() {
        // count 2 < threshold 3
        val result = analyzer.analyze("코드 코드 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `token appearing exactly three times triggers redundancy`() {
        val result = analyzer.analyze("코드 코드 코드 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `single character tokens do not count toward redundancy`() {
        // "이"는 1글자 → normalizedTokens length < 2 필터로 제외
        val result = analyzer.analyze("이 이 이 이 이 코드 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `suffix-stripped tokens accumulate for redundancy`() {
        // "공부하다" → "공부", "공부하고" → "공부", "공부하다" → "공부" → count 3
        val result = analyzer.analyze("공부하다 공부하고 공부하다 이유가 뭔지 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `해줘 suffix is stripped before redundancy count`() {
        // "분석해줘" → "분석", "정리해줘" → "정리" (서로 다른 토큰이므로 중복 없음)
        val result = analyzer.analyze("분석해줘 정리해줘 요약해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── 장황함 경계 ───────────────────────────────────────────────────────────

    @Test
    fun `single filler in long prompt does not trigger verbosity`() {
        // foundFillers.size=1 < 2, density=1/8=0.125 < 0.15
        val result = analyzer.analyze("정말 좋은 안드로이드 앱 개발 방법을 단계로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `single filler word alone triggers verbosity via density`() {
        // density = 1/1 = 1.0 >= 0.15 → 단어 하나여도 filler면 장황 탐지
        val result = analyzer.analyze("정말")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `그리고 또 phrase is deduplicated from 그리고 and counts as one connector`() {
        // longest-match deduplication: "그리고 또"가 "그리고"를 포함하므로 1개로만 집계 → count=1 < 2 → no VERBOSITY
        val result = analyzer.analyze("설치 방법 보여줘 그리고 또 사용법도 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `알려줘 is recognized as an action verb`() {
        // "알려" 키워드 → hasAction=true, "리스트"=outputFormat → isShortButPrecise 만족 → no LACK_OF_SCOPE
        val result = analyzer.analyze("머신러닝의 종류를 리스트로 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `보여줘 with output format avoids scope issue`() {
        // "보여"=action, "예시"=outputFormat → isShortButPrecise 조건 만족 → no LACK_OF_SCOPE
        val result = analyzer.analyze("파이썬 기초 문법을 예시로 보여줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `찾아줘 is recognized as an action verb`() {
        // "찾아" 키워드 → hasAction=true, "리스트"=outputFormat → isShortButPrecise 만족 → no LACK_OF_SCOPE
        val result = analyzer.analyze("딥러닝 논문을 리스트로 찾아줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `되도록이면 and 가능하면 together trigger verbosity`() {
        val result = analyzer.analyze("되도록이면 가능하면 짧게 요약해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── isShortButPrecise 경계 ────────────────────────────────────────────────

    @Test
    fun `3-token prompt with action and format avoids scope issue`() {
        // isShortButPrecise: hasAction=true, hasOutputFormat=true, rawTokens=3 in 3..8
        val result = analyzer.analyze("이걸 리스트로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `7-token prompt with action and format avoids scope issue via isShortButPrecise`() {
        // context 키워드 없고 7토큰 → hasContext=false, 하지만 isShortButPrecise로 보호
        val result = analyzer.analyze("이거 리스트로 정리 요약 압축 비교해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `2-token prompt with action and format still triggers scope issue`() {
        // rawTokens.size=2, 3..8 범위 밖 → isShortButPrecise=false
        val result = analyzer.analyze("리스트로 정리해줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 토큰 수 기반 hasContext 경계 ──────────────────────────────────────────

    @Test
    fun `7-token prompt without context keyword triggers scope issue`() {
        // rawTokens=7 < 8, context 키워드 없음 → hasContext=false
        val result = analyzer.analyze("그냥 뭔가 재미있는 이야기를 하나만 짧게 해줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `8-token prompt auto-satisfies hasContext`() {
        // rawTokens.size >= 8 → hasContext=true 자동
        val result = analyzer.analyze("좋은 코드 작성 습관 열 가지를 목록으로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 점수 정밀 검증 ─────────────────────────────────────────────────────────

    @Test
    fun `lack of scope only gives score 85`() {
        // -15점, 다른 이슈 없음
        val result = analyzer.analyze("이거 해줘")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertEquals(85, result.efficiencyScore)
    }

    @Test
    fun `verbosity only gives score 92`() {
        // filler 2개, context/action/format 충분 → VERBOSITY만 -8
        val result = analyzer.analyze("정말 매우 이 안드로이드 코드를 단계별로 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertEquals(92, result.efficiencyScore)
    }

    @Test
    fun `all three issues give score 65`() {
        // REDUNDANCY(-12) + VERBOSITY(-8) + LACK_OF_SCOPE(-15) = 65
        val result = analyzer.analyze("정말 매우 이거 이거 이거")

        assertEquals(65, result.efficiencyScore)
    }

    // ── 토큰·비용 기본 속성 검증 ──────────────────────────────────────────────

    @Test
    fun `any non-empty prompt produces positive token counts and cost`() {
        val result = analyzer.analyze("코드 설명해줘")

        assertTrue(result.inputTokens > 0)
        assertTrue(result.outputTokens > 0)
        assertTrue(result.totalTokens >= result.inputTokens + result.outputTokens)
        assertTrue(result.estimatedCostUsd > 0.0)
    }

    @Test
    fun `longer prompt has more input tokens than shorter prompt`() {
        val short = analyzer.analyze("설명해줘")
        val long = analyzer.analyze("이 안드로이드 앱의 Room 데이터베이스 마이그레이션 과정을 단계별로 자세히 설명해줘")

        assertTrue(long.inputTokens > short.inputTokens)
    }

    @Test
    fun `wasted tokens never exceed total tokens`() {
        val prompts = listOf(
            "정말 정말 매우 진짜 이거 이거 이거",
            "이거 해줘",
            "코드 코드 코드 설명해줘",
            "안드로이드 앱을 단계별로 목록으로 정리해줘"
        )
        prompts.forEach { prompt ->
            val result = analyzer.analyze(prompt)
            assertTrue("wastedTokens > totalTokens for: $prompt",
                result.wastedTokens <= result.totalTokens)
        }
    }

    @Test
    fun `efficiency score is always between 0 and 100`() {
        val prompts = listOf(
            "정말 정말 매우 진짜 아주 정말로 되게 이거 이거 이거 이거",
            "a",
            "코드 분석해줘",
            "완전 좋은 프롬프트를 목록으로 정리해줘",
            "도와줘",
            ""
        )
        prompts.forEach { prompt ->
            val result = analyzer.analyze(prompt)
            assertTrue("score out of range for '$prompt': ${result.efficiencyScore}",
                result.efficiencyScore in 0..100)
        }
    }

    // ── 영문·혼용 프롬프트 ────────────────────────────────────────────────────

    @Test
    fun `english-only technical prompt has no redundancy`() {
        val result = analyzer.analyze("Review this code and list three performance issues with suggested fixes")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `mixed Korean English prompt with action and format is clean`() {
        val result = analyzer.analyze("Python 코드의 memory leak 원인을 분석하고 수정 코드 예시를 보여줘")

        assertTrue(result.issues.isEmpty())
    }

    // ── 도메인별 좋은 프롬프트 ────────────────────────────────────────────────

    @Test
    fun `business email prompt with constraint is clean`() {
        // action: 작성, constraint: 이내, context: 고객
        val result = analyzer.analyze("신규 고객 대상 서비스 소개 이메일 초안을 200자 이내로 작성해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `data analysis prompt with table format is clean`() {
        // action: 분석+정리, outputFormat: 표, context: 데이터
        val result = analyzer.analyze("월별 매출 데이터를 분석해서 상위 3개 원인을 표로 정리해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `code refactoring prompt is clean`() {
        val result = analyzer.analyze("이 함수를 리팩터링하고 변경된 이유를 주석으로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `translation prompt with constraint is clean`() {
        // action: 번역, constraint: 이내, context: 문서
        val result = analyzer.analyze("이 기술 문서를 영어로 번역하고 500자 이내로 요약해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `debug prompt with code context is clean`() {
        // action: 디버깅, context: 버그+코드, outputFormat: 단계
        val result = analyzer.analyze("이 안드로이드 앱 버그를 디버깅하고 원인을 3단계로 설명해줘")

        assertTrue(result.issues.isEmpty())
    }

    // ── 특수·경계 입력 ────────────────────────────────────────────────────────

    @Test
    fun `numeric tokens do not cause redundancy with two occurrences`() {
        val result = analyzer.analyze("123 456 123 456 코드 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `prompt with all issues has model name set`() {
        val result = analyzer.analyze("정말 매우 이거 이거 이거")

        assertTrue(result.modelName.isNotBlank())
    }

    @Test
    fun `clean prompt has zero wasted tokens`() {
        val result = analyzer.analyze("안드로이드 앱 로그인 오류 원인을 3단계로 요약하고 해결 코드를 예시로 보여줘")

        assertEquals(0, result.wastedTokens)
    }

    // ── REDUNDANCY 추가 경계 ──────────────────────────────────────────────────

    @Test
    fun `same sentence three times triggers redundancy`() {
        val result = analyzer.analyze("정리해줘. 정리해줘. 정리해줘.")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `english token repeated three times triggers redundancy`() {
        // "gpt"×3 → length=3 ≥ 2, no Korean suffix → REDUNDANCY
        val result = analyzer.analyze("GPT GPT GPT 활용법을 리스트로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `two-char Korean token repeated three times triggers redundancy`() {
        // "아이"×3 → length=2 (경계값), no suffix → REDUNDANCY
        val result = analyzer.analyze("아이 아이 아이 관련 코드를 단계로 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `suffix-variant tokens appearing exactly twice do not trigger redundancy`() {
        // "분석하다"→"분석", "분석하고"→"분석" → count=2 < 3 → no REDUNDANCY
        val result = analyzer.analyze("분석하다 분석하고 결과를 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `english repeated token with good format gives redundancy-only score 88`() {
        // REDUNDANCY만 발생, VERBOSITY/SCOPE 없음 → score = 100 - 12 = 88
        val result = analyzer.analyze("GPT GPT GPT 활용법을 리스트로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertEquals(88, result.efficiencyScore)
    }

    // ── VERBOSITY 추가 경계 ───────────────────────────────────────────────────

    @Test
    fun `상당히 and 약간 together trigger verbosity`() {
        // explanatoryFillers 2개 → foundFillers.size ≥ 2 → VERBOSITY
        val result = analyzer.analyze("상당히 약간 복잡한 내용을 단계로 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `그 다음 and 추가로 connectors trigger verbosity`() {
        // connectorKeywords 2종 매칭, 긴 쪽이 짧은 쪽 포함 아님 → count=2 → VERBOSITY
        val result = analyzer.analyze("설치해줘 그 다음 설정해줘 추가로 테스트해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `filler density 1 in 6 tokens triggers verbosity`() {
        // density = 1/6 ≈ 0.167 ≥ 0.15, foundFillers.size=1 < 2 → density 조건으로만 VERBOSITY
        val result = analyzer.analyze("정말 안드로이드 코드 로그인 버그를 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `filler density 1 in 7 tokens does not trigger verbosity`() {
        // density = 1/7 ≈ 0.143 < 0.15, foundFillers.size=1 < 2, connectors=0 → no VERBOSITY
        val result = analyzer.analyze("정말 안드로이드 코드 로그인 버그를 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `굳이 말하면 multi-word filler triggers verbosity via density`() {
        // "굳이 말하면" = 1 filler, rawTokens=4 → density=0.25 ≥ 0.15 → VERBOSITY
        val result = analyzer.analyze("굳이 말하면 이게 맞아")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── LACK_OF_SCOPE — 나머지 구어체 동사 ──────────────────────────────────

    @Test
    fun `만들어줘 with output format avoids scope issue`() {
        val result = analyzer.analyze("React 컴포넌트를 리스트로 만들어줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `생성해줘 with output format avoids scope issue`() {
        val result = analyzer.analyze("테스트 데이터를 리스트로 생성해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `나열해줘 with output format avoids scope issue`() {
        val result = analyzer.analyze("Python 특징을 리스트로 나열해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `제안해줘 with output format avoids scope issue`() {
        val result = analyzer.analyze("UI 개선안을 리스트로 제안해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 새 도메인 — 좋은 프롬프트 ────────────────────────────────────────────

    @Test
    fun `json output format prompt is clean`() {
        // action: 정리, outputFormat: json, hasContext: rawTokens ≥ 8
        val result = analyzer.analyze("사용자 API 응답 데이터를 json 형식으로 정리해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `server context keyword satisfies hasContext`() {
        // "서버" in technicalContexts → hasContext=true → no SCOPE
        val result = analyzer.analyze("서버 API 설계 방법을 단계로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `마케팅 context keyword satisfies hasContext`() {
        // "마케팅" in businessContexts → hasContext=true → no SCOPE
        val result = analyzer.analyze("신규 마케팅 전략을 단계로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `맥락 context keyword satisfies hasContext for short prompt`() {
        // "맥락" in commonContexts → hasContext=true → no SCOPE
        val result = analyzer.analyze("이 맥락에서 핵심 포인트를 리스트로 요약해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `bullet format prompt is clean`() {
        // action: 정리, outputFormat: bullet, context: rawTokens ≥ 8
        val result = analyzer.analyze("좋은 코드 작성 습관 다섯 가지를 bullet 포인트로 정리해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `class design prompt avoids scope issue`() {
        // action: 설계, outputFormat: 클래스
        val result = analyzer.analyze("사용자 인증 기능을 클래스로 설계해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 점수 추가 검증 ────────────────────────────────────────────────────────

    @Test
    fun `verbosity and scope combined score is 77`() {
        // VERBOSITY(-8) + LACK_OF_SCOPE(-15) = 77, REDUNDANCY 없음
        val result = analyzer.analyze("정말 약간 이거 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertEquals(77, result.efficiencyScore)
    }

    @Test
    fun `redundancy and scope combined score is 73`() {
        // REDUNDANCY(-12) + LACK_OF_SCOPE(-15) = 73, VERBOSITY 없음
        val result = analyzer.analyze("이거 이거 이거 도와줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
        assertEquals(73, result.efficiencyScore)
    }

    // ── Issue 품질 검증 ───────────────────────────────────────────────────────

    @Test
    fun `redundancy issue has non-blank description`() {
        val result = analyzer.analyze("코드 코드 코드 설명해줘")
        val issue = result.issues.first { it.type == IssueType.REDUNDANCY }

        assertTrue(issue.description.isNotBlank())
        assertTrue(issue.suggestedFix.isNotBlank())
    }

    @Test
    fun `verbosity issue has non-blank description and fix`() {
        val result = analyzer.analyze("정말 매우 설명해줘")
        val issue = result.issues.first { it.type == IssueType.VERBOSITY }

        assertTrue(issue.description.isNotBlank())
        assertTrue(issue.suggestedFix.isNotBlank())
    }

    @Test
    fun `scope issue has non-blank description and fix`() {
        val result = analyzer.analyze("이거 해줘")
        val issue = result.issues.first { it.type == IssueType.LACK_OF_SCOPE }

        assertTrue(issue.description.isNotBlank())
        assertTrue(issue.suggestedFix.isNotBlank())
    }

    // ── Fix 1: 조건부 filler ─────────────────────────────────────────────────

    @Test
    fun `가능하면 followed by format keyword is not counted as filler`() {
        // "가능하면 JSON으로" → conditional use → no VERBOSITY
        val result = analyzer.analyze("가능하면 JSON으로 출력해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `가능하면 followed by list format is not counted as filler`() {
        val result = analyzer.analyze("가능하면 리스트로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `가능하면 followed by constraint keyword is not counted as filler`() {
        // "이내" = commonConstraints → conditional → no VERBOSITY
        val result = analyzer.analyze("가능하면 200자 이내로 작성해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `괜찮다면 followed by format keyword is not counted as filler`() {
        val result = analyzer.analyze("괜찮다면 표로 보여줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `혹시 가능하면 followed by format keyword is not counted as filler`() {
        val result = analyzer.analyze("혹시 가능하면 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `가능하면 without format or constraint keyword remains a filler`() {
        // "짧게" is not a format/constraint keyword → still a filler
        val result = analyzer.analyze("가능하면 짧게 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `two non-conditional polite fillers still trigger verbosity`() {
        // 둘 다 format/constraint 없이 쓰임 → count=2 → VERBOSITY
        val result = analyzer.analyze("혹시 가능하면 괜찮다면 이 내용 좀 요약해줄 수 있을까요")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── Fix 2: 동의어 정규화 ──────────────────────────────────────────────────

    @Test
    fun `Python and 파이썬 normalize to same token and trigger redundancy`() {
        // "python" → "파이썬", "파이썬" → "파이썬" → count=3 → REDUNDANCY
        val result = analyzer.analyze("Python 파이썬 파이썬 코드를 리스트로 정리해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `GPT and ChatGPT normalize to same token and trigger redundancy`() {
        // "chatgpt" → "gpt", "gpt" → "gpt" × 3 → REDUNDANCY
        val result = analyzer.analyze("GPT ChatGPT gpt 차이를 표로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `Kotlin and 코틀린 normalize to same token and trigger redundancy`() {
        val result = analyzer.analyze("Kotlin 코틀린 코틀린 문법을 리스트로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `Android and 안드로이드 twice do not trigger redundancy`() {
        // 동의어 정규화 후 "안드로이드"×2 < 3 → no REDUNDANCY
        val result = analyzer.analyze("Android 안드로이드 앱 개발 방법 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── Fix 3: suggestedFix 구체화 ────────────────────────────────────────────

    @Test
    fun `redundancy suggestedFix mentions the repeated word`() {
        // 반복 단어 '코드'가 suggestedFix에 언급되어야 함
        val result = analyzer.analyze("코드 코드 코드 설명해줘")
        val issue = result.issues.first { it.type == IssueType.REDUNDANCY }

        assertTrue(issue.suggestedFix.contains("코드"))
    }

    @Test
    fun `verbosity suggestedFix mentions the detected filler word`() {
        // '정말' filler → suggestedFix에 '정말' 언급
        val result = analyzer.analyze("정말 매우 설명해줘")
        val issue = result.issues.first { it.type == IssueType.VERBOSITY }

        assertTrue(issue.suggestedFix.contains("정말") || issue.suggestedFix.contains("매우"))
    }

    @Test
    fun `scope suggestedFix mentions action when action is missing`() {
        // action 없음 → suggestedFix에 "분석·요약·설명" 같은 힌트
        val result = analyzer.analyze("이거 해줘")
        val issue = result.issues.first { it.type == IssueType.LACK_OF_SCOPE }

        assertTrue(issue.suggestedFix.contains("분석") || issue.suggestedFix.contains("해달라"))
    }

    @Test
    fun `scope suggestedFix mentions format when format is missing`() {
        // format 없음 → suggestedFix에 "리스트·표·단계별" 힌트
        val result = analyzer.analyze("이거 해줘")
        val issue = result.issues.first { it.type == IssueType.LACK_OF_SCOPE }

        assertTrue(issue.suggestedFix.contains("리스트") || issue.suggestedFix.contains("형식"))
    }

    @Test
    fun `connector-only verbosity suggestedFix suggests compression`() {
        // filler 없이 connector만 → "압축" 언급
        val result = analyzer.analyze("설명해줘 그리고 요약해줘 또한 번역해줘")
        val issue = result.issues.first { it.type == IssueType.VERBOSITY }

        assertTrue(issue.suggestedFix.contains("압축"))
    }

    // ── 새 실전 데이터셋 ──────────────────────────────────────────────────────

    @Test
    fun `논문 context keyword satisfies hasContext`() {
        // "논문" in businessContexts → hasContext=true
        // action(요약) + constraint(줄) + context(논문) → missingSignals=[format]=1 < 2 → no SCOPE
        val result = analyzer.analyze("이 논문을 3줄로 요약해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `multi-request with two connectors triggers verbosity`() {
        // "그리고" + "또한" → connector 2종 → VERBOSITY
        val result = analyzer.analyze("요약해줘. 그리고 번역도 해줘. 또한 예시도 들어줘.")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `초안 as both action and format satisfies isShortButPrecise`() {
        // "초안" → writingActions(action=true) + writingOutputFormats(format=true), 4토큰 → no SCOPE
        val result = analyzer.analyze("단편 소설 초안을 써줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `개조식 is recognized as output format`() {
        // "개조식" 추가된 writingOutputFormats → hasOutputFormat=true
        val result = analyzer.analyze("하기 내용을 개조식으로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `질문형 프롬프트 triggers scope due to missing action`() {
        // "머신러닝이 뭔가요" → 2토큰, action 없음 → SCOPE (알려진 동작: 질문형 미처리)
        val result = analyzer.analyze("머신러닝이 뭔가요")

        assertTrue(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `number list format prompt avoids scope issue`() {
        // action(정리), format(목록), 3토큰 → isShortButPrecise → no SCOPE
        val result = analyzer.analyze("10가지를 목록으로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `코드 keyword satisfies both hasContext and hasOutputFormat simultaneously`() {
        // "코드" = technicalContexts(context) + technicalOutputFormats(format) 동시 충족
        // action(설명) + format(코드) + context(코드) → missingSignals=[constraint]=1 < 2 → no SCOPE
        val result = analyzer.analyze("print hello 이 코드 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `qualityConstraint 간단 satisfies hasConstraint`() {
        // action(설명) + context(코드) + constraint(간단) → missingSignals=[format]=1 < 2 → no SCOPE
        val result = analyzer.analyze("코드를 최대한 간단하게 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `multiple output formats in one prompt are all recognized`() {
        // json + markdown 둘 다 포함 → hasOutputFormat=true → no SCOPE
        val result = analyzer.analyze("결과를 json과 markdown으로 동시에 보여줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `JavaScript and JS normalize to same token and trigger redundancy`() {
        val result = analyzer.analyze("JavaScript JS js 문법을 리스트로 정리해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `very long clean prompt has score 100`() {
        val result = analyzer.analyze(
            "우리 안드로이드 앱에서 Room 데이터베이스를 사용할 때 발생하는 " +
            "마이그레이션 오류의 원인을 분석하고, 각 원인별 해결 방법을 단계별 목록으로 정리해줘"
        )

        assertEquals(100, result.efficiencyScore)
    }

    @Test
    fun `React and 리액트 normalize to same token and trigger redundancy`() {
        val result = analyzer.analyze("React 리액트 리액트 컴포넌트를 리스트로 만들어줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── 추가 filler (informalFillers) ────────────────────────────────────────

    @Test
    fun `그냥 alone triggers verbosity via density`() {
        // density=1/2=0.5 ≥ 0.15 → VERBOSITY
        val result = analyzer.analyze("그냥 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `제발 alone triggers verbosity via density`() {
        val result = analyzer.analyze("제발 도와줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `살짝 alone triggers verbosity via density`() {
        val result = analyzer.analyze("살짝 수정해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `정말 and 그냥 together trigger verbosity via count`() {
        // commonFiller(정말) + informalFiller(그냥) → count=2 → VERBOSITY
        val result = analyzer.analyze("정말 그냥 편하게 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `그냥 in long prompt does not trigger verbosity`() {
        // density=1/8=0.125 < 0.15, count=1 < 2, connectors=0 → no VERBOSITY
        val result = analyzer.analyze("그냥 안드로이드 코드 버그 로그인 오류를 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── 추가 connector (게다가, 아울러) ──────────────────────────────────────

    @Test
    fun `게다가 and 아울러 connectors trigger verbosity`() {
        val result = analyzer.analyze("설명해줘 게다가 예시도 보여줘 아울러 번역도 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `게다가 alone does not trigger verbosity`() {
        // connector 1개 < 2 → no VERBOSITY
        val result = analyzer.analyze("이 코드를 설명해줘 게다가 최적화 방법도 리스트로 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── 추가 action 키워드 (analysisActions) ─────────────────────────────────

    @Test
    fun `시각화 is recognized as action verb`() {
        val result = analyzer.analyze("월별 매출 데이터를 그래프로 시각화해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `계산 is recognized as action verb`() {
        val result = analyzer.analyze("프로젝트 예산을 단계로 계산해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `예측 is recognized as action verb`() {
        val result = analyzer.analyze("내년 매출을 데이터 기반으로 표로 예측해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `그려 is recognized as action verb for diagram prompts`() {
        val result = analyzer.analyze("시스템 구조를 다이어그램으로 그려줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `평가 is recognized as action verb`() {
        val result = analyzer.analyze("이 코드의 성능을 단계로 평가해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 추가 output format (다이어그램, 그래프, 순서도) ────────────────────

    @Test
    fun `다이어그램 is recognized as output format`() {
        // action(그려) + format(다이어그램) → no SCOPE
        val result = analyzer.analyze("배포 프로세스를 다이어그램으로 그려줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `그래프 is recognized as output format`() {
        val result = analyzer.analyze("매출 추이를 그래프로 보여줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `순서도 is recognized as output format`() {
        val result = analyzer.analyze("업무 흐름을 순서도로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 추가 context 키워드 (논문, 회사, 팀) ─────────────────────────────────

    @Test
    fun `회사 context keyword satisfies hasContext`() {
        val result = analyzer.analyze("회사 보고서를 단계로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `팀 context keyword satisfies hasContext`() {
        val result = analyzer.analyze("팀 업무 현황을 표로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 추가 synonym 테스트 ────────────────────────────────────────────────────

    @Test
    fun `TypeScript and TS normalize to same token and trigger redundancy`() {
        val result = analyzer.analyze("TypeScript TS ts 문법을 리스트로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `database and 데이터베이스 normalize to same token and trigger redundancy`() {
        val result = analyzer.analyze("database 데이터베이스 데이터베이스 설계를 표로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── 새 suffix 형태 (합니다, 했어, 한다, 하면, 하여, 해서, 하는) ──────────────

    @Test
    fun `합니다 suffix stripped to same root triggers redundancy`() {
        // "설명합니다"→"설명", "설명하고"→"설명", "설명합니다"→"설명" → count=3
        val result = analyzer.analyze("설명합니다 설명하고 설명합니다 구조를 단계로 정리해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `했어 suffix stripped to same root triggers redundancy`() {
        // "분석했어"→"분석", "분석하고"→"분석", "분석했어"→"분석" → count=3
        val result = analyzer.analyze("분석했어 분석하고 분석했어 결과를 단계로 요약해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `한다 suffix stripped to same root triggers redundancy`() {
        // "정리한다"→"정리", "정리하고"→"정리", "정리한다"→"정리" → count=3
        val result = analyzer.analyze("정리한다 정리하고 정리한다 결과를 요약해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `하면 suffix stripped to same root triggers redundancy`() {
        // "적용하면"→"적용" × 2 + "적용하고"→"적용" → count=3
        val result = analyzer.analyze("적용하면 적용하고 적용하면 방법을 단계로 설명해줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `하여 해서 하는 three forms of same root trigger redundancy`() {
        // "구현하여"→"구현", "구현해서"→"구현", "구현하는"→"구현" → count=3
        val result = analyzer.analyze("구현하여 구현해서 구현하는 방법을 단계로 알려줘")

        assertTrue(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    @Test
    fun `same root in only two suffix forms does not trigger redundancy`() {
        // "분석합니다"→"분석", "분석하고"→"분석" → count=2 < 3 → no REDUNDANCY
        val result = analyzer.analyze("분석합니다 분석하고 결과를 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.REDUNDANCY })
    }

    // ── 미커버 action 키워드 ──────────────────────────────────────────────────

    @Test
    fun `파악 is recognized as action verb`() {
        // action(파악), format(단계), rawTokens=5 → isShortButPrecise → no SCOPE
        val result = analyzer.analyze("문제 원인을 파악하고 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 미커버 context 키워드 ────────────────────────────────────────────────

    @Test
    fun `상황 context keyword satisfies hasContext for short prompt`() {
        // "상황" in commonContexts → hasContext=true
        val result = analyzer.analyze("이 상황을 리스트로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `배경 context keyword satisfies hasContext`() {
        // "배경" in commonContexts → hasContext=true
        val result = analyzer.analyze("프로젝트 배경을 표로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `운영 context keyword satisfies hasContext`() {
        // "운영" in businessContexts → hasContext=true
        val result = analyzer.analyze("서비스 운영 현황을 단계로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    @Test
    fun `db context keyword satisfies hasContext`() {
        // "db" in technicalContexts → hasContext=true
        val result = analyzer.analyze("db 스키마를 코드로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 미커버 connector 케이스 ──────────────────────────────────────────────

    @Test
    fun `아울러 alone does not trigger verbosity`() {
        // connector count=1 < 2 → no VERBOSITY
        val result = analyzer.analyze("이 코드를 설명해줘 아울러 최적화 방법도 리스트로 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `추가로 alone does not trigger verbosity`() {
        // connector count=1 < 2 → no VERBOSITY
        val result = analyzer.analyze("이 코드를 최적화하고 추가로 리스트로 알려줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `그리고 and 게다가 together trigger verbosity`() {
        // 두 connector 각각 포함관계 없음 → count=2 → VERBOSITY
        val result = analyzer.analyze("설명해줘 그리고 예시도 줘 게다가 번역도 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── 미커버 filler 케이스 ─────────────────────────────────────────────────

    @Test
    fun `그냥 and 살짝 together trigger verbosity`() {
        // informalFiller 2개 → count=2 → VERBOSITY
        val result = analyzer.analyze("그냥 살짝 수정해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `살짝 in long prompt does not trigger verbosity`() {
        // density=1/8=0.125 < 0.15, count=1 < 2 → no VERBOSITY
        val result = analyzer.analyze("살짝 안드로이드 코드 버그 로그인 오류를 단계로 설명해줘")

        assertFalse(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    @Test
    fun `되도록이면 alone triggers verbosity via density`() {
        // density=1/2=0.5 ≥ 0.15 → VERBOSITY
        val result = analyzer.analyze("되도록이면 해줘")

        assertTrue(result.issues.any { it.type == IssueType.VERBOSITY })
    }

    // ── 미커버 output format ─────────────────────────────────────────────────

    @Test
    fun `보고서 is recognized as output format`() {
        // "보고서" in writingOutputFormats → hasOutputFormat=true
        val result = analyzer.analyze("월별 마케팅 성과를 보고서로 정리해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }

    // ── 도메인 실전 프롬프트 (추가) ─────────────────────────────────────────────

    @Test
    fun `db query optimization prompt is clean`() {
        // action(최적화), format(단계), context(db) → no issues
        val result = analyzer.analyze("db 쿼리 성능을 최적화하는 방법을 단계로 알려줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `education step-by-step prompt is clean`() {
        // action(설명), format(단계), isShortButPrecise(7토큰) → no issues
        val result = analyzer.analyze("머신러닝 학습 방법을 초보자 기준으로 단계별로 설명해줘")

        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `api json design prompt avoids scope issue`() {
        // action(설계), format(json), context(api, 사용자)
        val result = analyzer.analyze("사용자 인증 api를 json 형식으로 설계해줘")

        assertFalse(result.issues.any { it.type == IssueType.LACK_OF_SCOPE })
    }
}
