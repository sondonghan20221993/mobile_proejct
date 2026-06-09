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
}
