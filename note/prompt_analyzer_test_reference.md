# PromptAnalyzer 테스트 데이터셋 참조

> 작성일: 2026-06-10  
> 테스트 총계: **79개 (failures 0 / errors 0 / skipped 0)**  
> 실행시간: 0.068초

---

## 탐지 로직 요약

### 임계값 & 트리거 조건

| 탐지 유형 | 조건 A | 조건 B | 조건 C |
|-----------|--------|--------|--------|
| **REDUNDANCY** | 같은 토큰(길이≥2) **3회 이상** | 같은 문장 **2회 이상** | — |
| **VERBOSITY** | filler 단어 **2개 이상** | filler 밀도 **≥ 0.15** (전체 토큰 대비) | connector 종류 **2가지 이상** |
| **LACK_OF_SCOPE** | action/format/constraint/context 중 **2개 이상 누락** | `isShortButPrecise` 조건 **미충족** | — |

### isShortButPrecise 조건
```
hasAction == true
AND hasOutputFormat == true
AND rawTokens.size in 3..8
```
→ 위 세 조건 모두 만족 시 LACK_OF_SCOPE 탐지 면제

### hasContext 자동 충족 조건
- context 키워드 포함 **OR** rawTokens.size **≥ 8**

### 점수 계산
```
score = 100 - (REDUNDANCY × 12) - (VERBOSITY × 8) - (LACK_OF_SCOPE × 15)
최솟값 0, 최댓값 100
```

| 이슈 조합 | 점수 |
|-----------|------|
| 없음 | 100 |
| REDUNDANCY만 | 88 |
| VERBOSITY만 | 92 |
| LACK_OF_SCOPE만 | 85 |
| R + V | 80 |
| R + S | 73 |
| V + S | 77 |
| R + V + S | **65** |

---

## 키워드 사전 전체 목록

### Filler Phrases (장황 탐지용) — 15개
| 그룹 | 키워드 |
|------|--------|
| commonFillers | 정말, 정말로, 매우, 진짜, 되게, 아주 |
| politeFillers | 가능하면, 혹시 가능하면, 괜찮다면, 되도록이면 |
| explanatoryFillers | 어떻게 보면, 상당히, 굳이 말하면, 약간, 조금은 |

### Connector Keywords — 5개
`그리고`, `또한`, `그리고 또`, `그 다음`, `추가로`

> ✅ **수정된 동작 (2026-06-10)**: longest-match 중복 제거 적용. `"그리고 또"` 포함 시 더 긴 `"그리고 또"`가 우선 채택되고 `"그리고"`는 집계에서 제외 → connectorCount=1 → VERBOSITY 미트리거. `"그리고"`와 `"또한"` 같이 포함 시(서로 포함 관계 아님) → count=2 → VERBOSITY 트리거.

### Action Keywords — 40개
| 그룹 | 키워드 |
|------|--------|
| commonActions | 분석, 정리, 설명, 작성, 비교, 추천, 요약, 수정, 변환 |
| technicalActions | 구현, 리팩터링, 디버깅, 최적화, 테스트, 설계, 점검, 검토 |
| writingActions | 초안, 번역, 교정, 재작성, 압축, 확장 |
| planningActions | 우선순위, 분류, 계획, 도출, 추출 |
| colloquialActions | **알려**, **보여**, **만들어**, **찾아**, **제안**, **생성**, **나열** |

> ℹ️ colloquialActions는 substring 매칭 기반: `"알려"` → `알려줘`, `알려주세요` 모두 포착. `"보여"` → `보여줘`, `보여주세요` 포착.

### Output Format Keywords — 19개
| 그룹 | 키워드 |
|------|--------|
| commonOutputFormats | 표, 리스트, 목록, 단계, 문단, 예시 |
| technicalOutputFormats | 코드, json, markdown, xml, api, 함수, 클래스 |
| writingOutputFormats | 개요, 요약문, 체크리스트, 보고서, 초안, bullet |

> ℹ️ `초안`은 writingActions에도 중복 등재됨 → `초안`만으로 hasAction과 hasOutputFormat 동시 충족 가능

### Constraint Keywords — 18개
| 그룹 | 키워드 |
|------|--------|
| commonConstraints | 이내, 제한, 형식, 반드시, 제외, 포함 |
| formattingConstraints | 글자, 줄, 길이, 문장, 단어, 분량 |
| qualityConstraints | 정확, 간단, 자세, 친절, 테스트, 검증 |

> ℹ️ `테스트`는 technicalActions에도 중복 등재됨 → 동시 충족 가능

### Context Keywords — 22개
| 그룹 | 키워드 |
|------|--------|
| commonContexts | 대상, 상황, 배경, 맥락, 프로젝트, 데이터 |
| technicalContexts | 앱, 안드로이드, 코드, 서비스, 서버, 클라이언트, 버그, 로그인, api, db |
| businessContexts | 사용자, 고객, 기획, 운영, 마케팅, 문서 |

> ℹ️ `코드`는 technicalContexts와 technicalOutputFormats에 동시 등재됨 → hasContext와 hasOutputFormat 동시 충족

### 한글 접미사 제거 목록 (중복 탐지용) — 14개
`으로`, `에서`, `에게`, `까지`, `부터`, `보다`, `처럼`, `하다`, `하고`, `적인`, `적인지`, `입니다`, `해주세요`, `해줘`

---

## 테스트 케이스 전체 목록

### 🔴 REDUNDANCY 탐지 확인 (탐지 O)

| 프롬프트 | 탐지 이유 |
|----------|-----------|
| `"이 앱 앱 앱 분석 분석 분석 결과를 정리해 줘"` | 앱×3, 분석×3 |
| `"파이썬 파이썬 파이썬 코드 파이썬 예제 보여줘"` | 파이썬×4 |
| `"코드 리뷰해줘. 코드 리뷰해줘."` | 동일 문장 ×2 |
| `"정리해줘. 정리해줘. 정리해줘."` | 동일 문장 ×3 |
| `"코드 코드 코드 설명해줘"` | 코드×3 |
| `"공부하다 공부하고 공부하다 이유가 뭔지 설명해줘"` | 접미사 제거 후 공부×3 |
| `"GPT GPT GPT 활용법을 리스트로 알려줘"` | gpt×3 (영문 소문자 변환 후), score=88 |
| `"아이 아이 아이 관련 코드를 단계로 설명해줘"` | 아이×3 (2글자 경계값) |
| `"Python 코드에서 파이썬 파이썬 파이썬 변수명을 리스트로 정리해줘"` | 파이썬×3, score=88 |
| `"정말 정말 매우 진짜 이 코드 코드 코드를 설명 설명 설명해줘"` | 코드×3, 설명×3 + VERBOSITY |

### 🟢 REDUNDANCY 미탐지 확인 (탐지 X)

| 프롬프트 | 미탐지 이유 |
|----------|-------------|
| `"코드 코드 설명해줘"` | count=2 < threshold 3 |
| `"이 이 이 이 이 코드 설명해줘"` | '이'는 1글자 → 필터됨 |
| `"분석해줘 정리해줘 요약해줘"` | 접미사 제거 후 모두 다른 토큰 |
| `"분석하다 분석하고 결과를 단계로 설명해줘"` | 접미사 제거 후 분석×2 < 3 |
| `"123 456 123 456 코드 설명해줘"` | 숫자 각 2회 < 3 |
| `"Review this code and list three performance issues with suggested fixes"` | 반복 없음 |

---

### 🔴 VERBOSITY 탐지 확인 (탐지 O)

| 프롬프트 | 탐지 이유 |
|----------|-----------|
| `"정말 정말 매우 진짜 자세하고 길게 설명해 줘"` | filler 3개 이상 |
| `"혹시 가능하면 괜찮다면 이 내용 좀 요약해줄 수 있을까요"` | polite filler 2개 이상 |
| `"설치 방법 알려줘 그리고 사용법도 알려줘 그리고 예제도 줘 또한 에러 해결법도 알려줘"` | connector 2종류(그리고·또한) |
| `"설치해줘 그 다음 설정해줘 추가로 테스트해줘"` | connector 2종류(그 다음·추가로), 포함 관계 아님 |
| `"정말"` | density=1.0 ≥ 0.15 (1토큰 전체가 filler) |
| `"정말 안드로이드 코드 로그인 버그를 설명해줘"` | density=1/6≈0.167 ≥ 0.15, filler 1개(count 미달이지만 density로 트리거) |
| `"상당히 약간 복잡한 내용을 단계로 설명해줘"` | explanatory filler 2개(상당히·약간) |
| `"굳이 말하면 이게 맞아"` | 다중어 filler 1개, density=1/4=0.25 ≥ 0.15 |
| `"되도록이면 가능하면 짧게 요약해줘"` | polite filler 2개 |
| `"정말 매우 이 안드로이드 코드를 단계별로 설명해줘"` | filler 2개, score=92 |
| `"정말 약간 이거 해줘"` | filler 2개 → VERBOSITY + SCOPE, score=77 |
| `"정말 정말 매우 진짜 이거 이거 이거"` | filler 4개 + REDUNDANCY + SCOPE, score=65 |

### 🟢 VERBOSITY 미탐지 확인 (탐지 X)

| 프롬프트 | 미탐지 이유 |
|----------|-------------|
| `"정말 좋은 안드로이드 앱 개발 방법을 단계로 정리해줘"` | filler 1개, density=1/8=0.125 < 0.15 |
| `"정말 안드로이드 코드 로그인 버그를 단계로 설명해줘"` | filler 1개, density=1/7≈0.143 < 0.15 (경계값) |
| `"설치 방법 보여줘 그리고 또 사용법도 알려줘"` | longest-match 제거: "그리고 또"가 "그리고" 흡수 → count=1 < 2 |

> ℹ️ **density 경계**: filler 1개일 때 6토큰 이하(≥0.167) → VERBOSITY, 7토큰 이상(≤0.143) → 안전

---

### 🔴 LACK_OF_SCOPE 탐지 확인 (탐지 O)

| 프롬프트 | 누락 신호 |
|----------|-----------|
| `"이거 해줘"` | 전부 누락, score=85 |
| `"도와줘"` | 전부 누락 |
| `"뭔가 좋은 거 알려줘"` | format·constraint·context 누락 (알려=action은 충족) |
| `"코드 봐줘"` | action·format·constraint 누락 (context=코드 충족) |
| `"리스트로 정리해줘"` | rawTokens=2 → isShortButPrecise 불충족 |
| `"그냥 뭔가 재미있는 이야기를 하나만 짧게 해줘"` | rawTokens=7 < 8, context 키워드 없음 |
| `"이거 이거 이거 도와줘"` | 전부 누락 + REDUNDANCY, score=73 |

### 🟢 LACK_OF_SCOPE 미탐지 확인 (탐지 X)

| 프롬프트 | 충족 경로 |
|----------|-----------|
| `"버그 원인 3개 리스트로 정리"` | action+format+context(버그) |
| `"이걸 리스트로 정리해줘"` | isShortButPrecise (3토큰) |
| `"이거 리스트로 정리 요약 압축 비교해줘"` | isShortButPrecise (7토큰) |
| `"좋은 코드 작성 습관 열 가지를 목록으로 정리해줘"` | rawTokens=8 → hasContext 자동 |
| `"스타트업 투자 유치 이메일을 200자 이내 한국어로 작성해줘"` | action+constraint+rawTokens≥8 |
| `"안드로이드 로그인 버그를 디버깅하고 xml 예시로 수정 방향을 보여줘"` | 기술 도메인 키워드 다수 |
| `"기획 문서를 초안 형태의 체크리스트로 재작성해줘"` | writingAction+format |
| `"머신러닝의 종류를 리스트로 알려줘"` | colloquial action(알려)+format → isShortButPrecise |
| `"파이썬 기초 문법을 예시로 보여줘"` | colloquial action(보여)+format → isShortButPrecise |
| `"딥러닝 논문을 리스트로 찾아줘"` | colloquial action(찾아)+format → isShortButPrecise |
| `"React 컴포넌트를 리스트로 만들어줘"` | colloquial action(만들어)+format → isShortButPrecise |
| `"테스트 데이터를 리스트로 생성해줘"` | colloquial action(생성)+format → isShortButPrecise |
| `"Python 특징을 리스트로 나열해줘"` | colloquial action(나열)+format → isShortButPrecise |
| `"UI 개선안을 리스트로 제안해줘"` | colloquial action(제안)+format → isShortButPrecise |
| `"서버 API 설계 방법을 단계로 정리해줘"` | context(서버)+action+format |
| `"신규 마케팅 전략을 단계로 정리해줘"` | context(마케팅)+action+format |
| `"이 맥락에서 핵심 포인트를 리스트로 요약해줘"` | context(맥락)+action+format |
| `"사용자 인증 기능을 클래스로 설계해줘"` | context(사용자)+action(설계)+format(클래스) |

---

### ✅ 100점 클린 프롬프트 예시

| 프롬프트 | 충족 신호 |
|----------|-----------|
| `"안드로이드 앱 로그인 오류 원인을 3단계로 요약하고 해결 코드를 예시로 보여줘"` | action+format+context 다수, rawTokens≥8 |
| `"이 파이썬 함수의 시간복잡도를 분석하고 최적화 방법을 3단계로 알려줘"` | action(분석·알려)+format(단계)+rawTokens≥8 |
| `"React 컴포넌트 성능 최적화 방법을 초보자 기준으로 단계별 목록으로 정리해줘"` | action+format+rawTokens≥8 |
| `"Python 코드의 memory leak 원인을 분석하고 수정 코드 예시를 보여줘"` | action(분석·보여)+format(예시·코드)+context(코드) |
| `"신규 고객 대상 서비스 소개 이메일 초안을 200자 이내로 작성해줘"` | action+format(초안)+constraint+context(고객) |
| `"월별 매출 데이터를 분석해서 상위 3개 원인을 표로 정리해줘"` | action+format(표)+context(데이터) |
| `"이 기술 문서를 영어로 번역하고 500자 이내로 요약해줘"` | action+constraint+context(문서) |
| `"이 안드로이드 앱 버그를 디버깅하고 원인을 3단계로 설명해줘"` | action+format+context(앱·버그) |
| `"사용자 API 응답 데이터를 json 형식으로 정리해줘"` | action+format(json)+context(사용자·데이터)+rawTokens≥8 |
| `"좋은 코드 작성 습관 다섯 가지를 bullet 포인트로 정리해줘"` | action+format(bullet)+rawTokens≥8 |

---

## 속성 불변 검증 (Property-based)

| 속성 | 검증 내용 | 결과 |
|------|-----------|------|
| wastedTokens ≤ totalTokens | 4가지 프롬프트 전체 | ✅ |
| efficiencyScore in 0..100 | 극단적 프롬프트 6가지 포함 | ✅ |
| inputTokens > 0 | 비어있지 않은 프롬프트 | ✅ |
| outputTokens > 0 | 비어있지 않은 프롬프트 | ✅ |
| totalTokens ≥ inputTokens + outputTokens | — | ✅ |
| 긴 프롬프트 inputTokens > 짧은 프롬프트 | — | ✅ |
| 클린 프롬프트 wastedTokens = 0 | — | ✅ |
| modelName.isNotBlank() | 모든 분석 결과 | ✅ |
| REDUNDANCY issue.description.isNotBlank() | 탐지 시 설명 비어있지 않음 | ✅ |
| REDUNDANCY issue.suggestedFix.isNotBlank() | 탐지 시 수정 제안 비어있지 않음 | ✅ |
| VERBOSITY issue.description.isNotBlank() | 탐지 시 설명 비어있지 않음 | ✅ |
| VERBOSITY issue.suggestedFix.isNotBlank() | 탐지 시 수정 제안 비어있지 않음 | ✅ |
| LACK_OF_SCOPE issue.description.isNotBlank() | 탐지 시 설명 비어있지 않음 | ✅ |
| LACK_OF_SCOPE issue.suggestedFix.isNotBlank() | 탐지 시 수정 제안 비어있지 않음 | ✅ |

---

## 알려진 동작 특이사항

### 1. `"그리고 또"` 이중 매칭 → **수정 완료 (2026-06-10)**
- **구 동작 (버그)**: `"그리고 또"` 하나가 `"그리고"` + `"그리고 또"` 두 항목에 동시 매칭 → connectorCount=2 → VERBOSITY 오탐
- **현재 동작 (수정)**: longest-match 중복 제거 적용. `foundConnectors`를 길이 내림차순 정렬 후, 더 긴 connector에 포함되는 짧은 항목은 집계 제외 → connectorCount=1 → VERBOSITY 미트리거

### 2. 단어 하나짜리 Filler
- **현상**: `"정말"` 한 단어 입력 시 density=1.0/1=1.0 ≥ 0.15 → VERBOSITY 트리거
- **영향**: 실질적으로 의미 없는 입력이지만 장황으로 판단됨 (의도된 동작)

### 3. `코드`, `초안`, `테스트` 키워드 중복 등재
- `코드`: technicalOutputFormats + technicalContexts (hasOutputFormat + hasContext 동시 충족)
- `초안`: writingActions + writingOutputFormats (hasAction + hasOutputFormat 동시 충족)
- `테스트`: technicalActions + qualityConstraints (hasAction + hasConstraint 동시 충족)

### 4. `isShortButPrecise` 최소 토큰 경계
- rawTokens=2인 경우 action+format이 있어도 `3..8` 범위 밖 → LACK_OF_SCOPE 탐지됨
- `"리스트로 정리해줘"` (2토큰) → 탐지 O
- `"이걸 리스트로 정리해줘"` (3토큰) → 탐지 X

### 5. density 1/6 vs 1/7 경계
- filler 1개, 6토큰: density=0.167 ≥ 0.15 → VERBOSITY 트리거
- filler 1개, 7토큰: density=0.143 < 0.15 → VERBOSITY 미트리거
- filler 1개, count=1 < 2 이므로 density 조건이 유일한 트리거

### 6. colloquialActions substring 매칭 주의사항
- `"알려"` → `"알려줘"`, `"알려주세요"`, `"알려진"` 모두 매칭 (substring 기반)
- `"알려진 문제"` 같은 수동형 표현도 hasAction=true로 처리됨 (허용 가능 범위로 판단)

---

## 향후 테스트 확장 방향

추가하면 의미 있는 영역 (현재 미커버):
- 각 filler 키워드 15개 개별 density 테스트 (~30개)
- 각 action 키워드 40개 단독 인식 테스트 (~40개)
- 각 outputFormat 키워드 19개 단독 인식 테스트 (~19개)
- 각 context 키워드 22개 단독 인식 테스트 (~22개)
- 각 constraint 키워드 18개 단독 인식 테스트 (~18개)
- 접미사 14개 각각에 대한 strip 동작 테스트 (~14개)
- filler 2개 조합(15C2=105개) 중 대표 케이스
- connector 2개 조합(5C2=10개) 전부
- **추정 추가 가능 총계: 약 150~200개**

현재 79개 + 추가 시 총 **약 250~280개** 도달 가능
