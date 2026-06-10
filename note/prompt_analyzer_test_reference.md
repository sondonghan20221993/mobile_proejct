# PromptAnalyzer 테스트 데이터셋 참조

> 최종 업데이트: 2026-06-10  
> 테스트 총계: **186개 (failures 0 / errors 0 / skipped 0)**  
> 실행시간: 0.102초

---

## 탐지 로직 요약

### 임계값 & 트리거 조건

| 탐지 유형 | 조건 A | 조건 B | 조건 C |
|-----------|--------|--------|--------|
| **REDUNDANCY** | 같은 토큰(길이≥2) **3회 이상** | 같은 문장 **2회 이상** | — |
| **VERBOSITY** | filler 단어 **2개 이상** | filler 밀도 **≥ 0.15** (전체 토큰 대비) | 독립 connector 종류 **2가지 이상** |
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

### Filler Phrases (장황 탐지용) — 18개

| 그룹 | 키워드 | 특이사항 |
|------|--------|---------|
| commonFillers | 정말, 정말로, 매우, 진짜, 되게, 아주 | — |
| politeFillers | 가능하면, 혹시 가능하면, 괜찮다면, 되도록이면 | 조건부 filler (아래 참조) |
| explanatoryFillers | 어떻게 보면, 상당히, 굳이 말하면, 약간, 조금은 | — |
| informalFillers | **그냥**, **제발**, **살짝** | — |

> ℹ️ **조건부 filler 규칙 (politeFillers만 적용)**: 해당 filler 뒤 20자 이내에 출력형식/제약 키워드가 오면 조건문으로 판단 → filler 카운트 제외.  
> 단, 키워드 앞에 공백 또는 문자열 시작이어야 함 (부분 문자열 오매칭 방지).  
> 예) `"가능하면 JSON으로"` → 조건부 → 제외. `"가능하면 짧게"` → 키워드 없음 → filler로 집계.

### Connector Keywords — 8개

`그리고`, `또한`, `그리고 또`, `그 다음`, `추가로`, `게다가`, `아울러`, `더불어`

> ℹ️ **longest-match 중복 제거**: `foundConnectors`를 길이 내림차순 정렬 후, 더 긴 connector에 포함되는 짧은 항목은 집계 제외.  
> 예) `"그리고 또"` 매칭 시 더 짧은 `"그리고"`는 제외 → connectorCount=1 → VERBOSITY 미트리거.  
> 예) `"그리고"` + `"또한"` 동시 포함 시(서로 포함관계 없음) → count=2 → VERBOSITY 트리거.

### Action Keywords — 46개

| 그룹 | 키워드 |
|------|--------|
| commonActions (9) | 분석, 정리, 설명, 작성, 비교, 추천, 요약, 수정, 변환 |
| technicalActions (8) | 구현, 리팩터링, 디버깅, 최적화, 테스트, 설계, 점검, 검토 |
| writingActions (6) | 초안, 번역, 교정, 재작성, 압축, 확장 |
| planningActions (10) | 우선순위, 분류, 계획, 도출, 추출, **구분**, **분리**, **결합**, **검색**, **학습** |
| colloquialActions (7) | **알려**, **보여**, **만들어**, **찾아**, **제안**, **생성**, **나열** |
| analysisActions (6) | **시각화**, **계산**, **예측**, **평가**, **파악**, **그려** |

> ℹ️ colloquialActions/analysisActions는 substring 매칭: `"알려"` → `알려줘`, `알려주세요` 포착. `"그려"` → `그려줘`, `그려주세요` 포착.  
> ℹ️ `초안`은 writingActions에도 등재 → hasAction + hasOutputFormat 동시 충족 가능.

### Output Format Keywords — 26개

| 그룹 | 키워드 |
|------|--------|
| commonOutputFormats (6) | 표, 리스트, 목록, 단계, 문단, 예시 |
| technicalOutputFormats (10) | 코드, json, markdown, xml, api, 함수, 클래스, **다이어그램**, **그래프**, **순서도** |
| writingOutputFormats (10) | 개요, 요약문, **요약본**, 체크리스트, 보고서, 초안, bullet, **개조식**, **슬라이드**, **타임라인** |

### Constraint Keywords — 18개

| 그룹 | 키워드 |
|------|--------|
| commonConstraints | 이내, 제한, 형식, 반드시, 제외, 포함 |
| formattingConstraints | 글자, 줄, 길이, 문장, 단어, 분량 |
| qualityConstraints | 정확, 간단, 자세, 친절, 테스트, 검증 |

### Context Keywords — 40개

| 그룹 | 키워드 |
|------|--------|
| commonContexts (6) | 대상, 상황, 배경, 맥락, 프로젝트, 데이터 |
| technicalContexts (10) | 앱, 안드로이드, 코드, 서비스, 서버, 클라이언트, 버그, 로그인, api, db |
| businessContexts (9) | 사용자, 고객, 기획, 운영, 마케팅, 문서, **논문**, **회사**, **팀** |
| professionalContexts (15) | **법**, **법률**, **계약**, **의료**, **진료**, **환자**, **금융**, **투자**, **주식**, **보안**, **취약점**, **게임**, **교육**, **학생**, **수업** |

> ℹ️ `코드`는 technicalContexts + technicalOutputFormats 동시 등재 → hasContext + hasOutputFormat 동시 충족.

### 동의어 정규화 그룹 (synonymGroups) — 주요 매핑

| 입력(lowercase) | 정규화 결과 |
|----------------|-----------|
| python, py | 파이썬 |
| javascript, js | 자바스크립트 |
| typescript, ts | 타입스크립트 |
| kotlin | 코틀린 |
| java | 자바 |
| android | 안드로이드 |
| gpt, chatgpt | gpt |
| ml | 머신러닝 |
| dl | 딥러닝 |
| ai | ai |
| react, 리액트 | react |
| vue, 뷰 | vue |
| css | css |
| html | html |
| swift | swift |
| flutter | flutter |
| database | 데이터베이스 |

> ℹ️ 정규화는 suffix 제거 후 적용. `"python했어"` → 접미사 `"했어"` 제거 → `"python"` → synonym `"파이썬"`.

### 한글 접미사 제거 목록 (중복 탐지용) — 29개

긴 것 우선 적용 (firstOrNull 방식):

| 그룹 | 접미사 |
|------|--------|
| 정중 요청 | 해주세요, 해줘 |
| 과거형 | 했어요, 했어 |
| 공식 현재형 | 합니다, 한다 |
| 동사 활용형 | 해요, 해서, 하면, 하여, 하는, 하고, 하다 |
| 형용사형 | 적인지, 적인 |
| 계사 | 입니다 |
| 격조사 | 으로, 에서, 에게, 까지, 부터, 보다, 처럼 |
| 주격·목적격·보조사 | **을, 를, 이, 가, 은, 는** |

> ℹ️ 조건: `token.length > suffix.length + 1` — 너무 짧은 토큰은 제거 안 함.

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
| `"GPT GPT GPT 활용법을 리스트로 알려줘"` | gpt×3, score=88 |
| `"아이 아이 아이 관련 코드를 단계로 설명해줘"` | 아이×3 (2글자 경계) |
| `"Python 코드에서 파이썬 파이썬 파이썬 변수명을 리스트로 정리해줘"` | 파이썬×3, score=88 |
| `"정말 정말 매우 진짜 이 코드 코드 코드를 설명 설명 설명해줘"` | 코드×3, 설명×3 + VERBOSITY |
| `"Python 파이썬 파이썬 코드를 리스트로 정리해줘"` | 동의어 정규화: python→파이썬×3 |
| `"GPT ChatGPT gpt 차이를 표로 알려줘"` | 동의어 정규화: chatgpt→gpt×3 |
| `"Kotlin 코틀린 코틀린 문법을 리스트로 알려줘"` | 동의어 정규화: kotlin→코틀린×3 |
| `"JavaScript JS js 문법을 리스트로 정리해줘"` | 동의어 정규화: js→자바스크립트×3 |
| `"TypeScript TS ts 문법을 리스트로 알려줘"` | 동의어 정규화: ts→타입스크립트×3 |
| `"database 데이터베이스 데이터베이스 설계를 표로 알려줘"` | 동의어 정규화: database→데이터베이스×3 |
| `"React 리액트 리액트 컴포넌트를 리스트로 만들어줘"` | 동의어 정규화: 리액트→react×3 |
| `"설명합니다 설명하고 설명합니다 구조를 단계로 정리해줘"` | 합니다 접미사 제거: 설명×3 |
| `"분석했어 분석하고 분석했어 결과를 단계로 요약해줘"` | 했어 접미사 제거: 분석×3 |
| `"정리한다 정리하고 정리한다 결과를 요약해줘"` | 한다 접미사 제거: 정리×3 |
| `"적용하면 적용하고 적용하면 방법을 단계로 설명해줘"` | 하면 접미사 제거: 적용×3 |
| `"구현하여 구현해서 구현하는 방법을 단계로 알려줘"` | 하여/해서/하는 제거: 구현×3 |
| `"python했어 파이썬하고 py한다 코드를 리스트로 정리해줘"` | 접미사 제거 + 동의어 복합: 파이썬×3 |
| `"css css css 스타일을 표로 정리해줘"` | css×3 (synonym self-map) |
| `"flutter flutter flutter 앱 개발 단계를 알려줘"` | flutter×3 |
| `"dl 딥러닝 딥러닝 차이를 표로 알려줘"` | dl→딥러닝×3 |
| `"swift swift swift 코드를 리스트로 설명해줘"` | swift×3 |
| `"파이썬을 파이썬으로 파이썬에서 리스트로 정리해줘"` | 조사 을/으로/에서 제거 → 파이썬×3 |
| `"코드를 코드는 코드가 단계로 설명해줘"` | 조사 를/은/가 제거 → 코드×3 |
| `"분석이 분석을 분석은 필요한 내용을 표로 정리해줘"` | 조사 이/을/은 제거 → 분석×3 |
| `"파이썬은 파이썬을 파이썬으로 뭘 할 수 있는지 리스트로 알려줘"` | 조사 은/을/으로 제거 → 파이썬×3 |
| `"python을 파이썬으로 py는 어떻게 다른지 표로 정리해줘"` | 조사 제거 + synonym 연쇄: 파이썬×3 |

### 🟢 REDUNDANCY 미탐지 확인 (탐지 X)

| 프롬프트 | 미탐지 이유 |
|----------|-------------|
| `"코드 코드 설명해줘"` | count=2 < threshold 3 |
| `"이 이 이 이 이 코드 설명해줘"` | '이'는 1글자 → 필터됨 |
| `"분석해줘 정리해줘 요약해줘"` | 접미사 제거 후 모두 다른 토큰 |
| `"분석하다 분석하고 결과를 단계로 설명해줘"` | 접미사 제거 후 분석×2 < 3 |
| `"123 456 123 456 코드 설명해줘"` | 숫자 각 2회 < 3 |
| `"Review this code and list three performance issues with suggested fixes"` | 반복 없음 |
| `"Android 안드로이드 앱 개발 방법 알려줘"` | 동의어 정규화 후 안드로이드×2 < 3 |
| `"분석합니다 분석하고 결과를 단계로 설명해줘"` | 접미사 제거 후 분석×2 < 3 |
| `"코드를 코드는 단계로 설명해줘"` | 조사 제거 후 코드×2 < 3 |
| `"머신러닝 ml 딥러닝 ai 트렌드를 리스트로 정리해줘"` | ml→머신러닝×2, 딥러닝×1 — 서로 다른 개념 구별 |

---

### 🔴 VERBOSITY 탐지 확인 (탐지 O)

| 프롬프트 | 탐지 이유 |
|----------|-----------|
| `"정말 정말 매우 진짜 자세하고 길게 설명해 줘"` | filler 3개+ |
| `"혹시 가능하면 괜찮다면 이 내용 좀 요약해줄 수 있을까요"` | polite filler 2개 (조건부 아님) |
| `"설치 방법 알려줘 그리고 사용법도 알려줘 그리고 예제도 줘 또한 에러 해결법도 알려줘"` | connector 2종(그리고·또한) |
| `"설치해줘 그 다음 설정해줘 추가로 테스트해줘"` | connector 2종(그 다음·추가로) |
| `"정말"` | density=1.0 ≥ 0.15 |
| `"정말 안드로이드 코드 로그인 버그를 설명해줘"` | density=1/6≈0.167 ≥ 0.15 |
| `"상당히 약간 복잡한 내용을 단계로 설명해줘"` | explanatory filler 2개 |
| `"굳이 말하면 이게 맞아"` | 다중어 filler 1개, density=1/4=0.25 |
| `"되도록이면 가능하면 짧게 요약해줘"` | polite filler 2개 |
| `"정말 매우 이 안드로이드 코드를 단계별로 설명해줘"` | filler 2개, score=92 |
| `"정말 약간 이거 해줘"` | filler 2개 → VERBOSITY+SCOPE, score=77 |
| `"정말 정말 매우 진짜 이거 이거 이거"` | filler 4개+REDUNDANCY+SCOPE, score=65 |
| `"가능하면 짧게 해줘"` | "가능하면" 뒤 format/constraint 없음 → filler로 집계, density=1/3 |
| `"그냥 해줘"` | informalFiller density=1/2=0.5 |
| `"제발 도와줘"` | informalFiller density=1/2=0.5 |
| `"살짝 수정해줘"` | informalFiller density=1/2=0.5 |
| `"정말 그냥 편하게 설명해줘"` | filler(정말)+informalFiller(그냥)=2개 |
| `"되도록이면 해줘"` | polite filler 1개, density=1/2=0.5 |
| `"그냥 살짝 수정해줘"` | informalFiller 2개 |
| `"설치 방법 알려줘 그리고 또 사용법도 알려줘 또한 에러 해결법도 알려줘"` | connector 2종(그리고 또·또한) |
| `"설명해줘 게다가 예시도 보여줘 아울러 번역도 해줘"` | connector 2종(게다가·아울러) |
| `"설명해줘 그리고 예시도 줘 게다가 번역도 해줘"` | connector 2종(그리고·게다가) |
| `"설명해줘 그리고 예시도 줘 더불어 번역도 해줘"` | connector 2종(그리고·더불어) |
| `"분석해줘 또한 예시도 줘 아울러 번역도 해줘"` | connector 2종(또한·아울러) |
| `"설명해줘 게다가 요약도 줘 더불어 리스트도 만들어줘"` | connector 2종(게다가·더불어) |
| `"또한 게다가 추가로 단계로 설명해줘"` | connector 3종 |
| `"요약해줘. 그리고 번역도 해줘. 또한 예시도 들어줘."` | connector 2종(그리고·또한) |

### 🟢 VERBOSITY 미탐지 확인 (탐지 X)

| 프롬프트 | 미탐지 이유 |
|----------|-------------|
| `"정말 좋은 안드로이드 앱 개발 방법을 단계로 정리해줘"` | filler 1개, density=1/8=0.125 < 0.15 |
| `"정말 안드로이드 코드 로그인 버그를 단계로 설명해줘"` | filler 1개, density=1/7≈0.143 < 0.15 (경계값) |
| `"설치 방법 보여줘 그리고 또 사용법도 알려줘"` | longest-match: "그리고 또"가 "그리고" 흡수 → count=1 |
| `"가능하면 JSON으로 출력해줘"` | "가능하면" 뒤 format(JSON) → 조건부 → 제외 |
| `"가능하면 리스트로 정리해줘"` | "가능하면" 뒤 format(리스트) → 조건부 → 제외 |
| `"가능하면 200자 이내로 작성해줘"` | "가능하면" 뒤 constraint(이내) → 조건부 → 제외 |
| `"괜찮다면 표로 보여줘"` | "괜찮다면" 뒤 format(표) → 조건부 → 제외 |
| `"혹시 가능하면 단계로 설명해줘"` | "혹시 가능하면" 뒤 format(단계) → 조건부 → 제외 |
| `"그냥 안드로이드 코드 버그 로그인 오류를 단계로 설명해줘"` | density=1/8=0.125 < 0.15, count=1 |
| `"살짝 안드로이드 코드 버그 로그인 오류를 단계로 설명해줘"` | density=1/8=0.125 < 0.15, count=1 |
| `"이 코드를 설명해줘 게다가 최적화 방법도 리스트로 알려줘"` | connector 1종만 |
| `"이 코드를 설명해줘 아울러 최적화 방법도 리스트로 알려줘"` | connector 1종만 |
| `"이 코드를 최적화하고 추가로 리스트로 알려줘"` | connector 1종만 |
| `"이 코드 설명해줘 더불어 예시도 리스트로 알려줘"` | connector 1종만 |

> ℹ️ **density 경계**: filler 1개, 6토큰 이하(density≥0.167) → VERBOSITY; 7토큰 이상(density≤0.143) → 안전

---

### 🔴 LACK_OF_SCOPE 탐지 확인 (탐지 O)

| 프롬프트 | 누락 신호 |
|----------|-----------|
| `"이거 해줘"` | 전부 누락, score=85 |
| `"도와줘"` | 전부 누락 |
| `"뭔가 좋은 거 알려줘"` | format·constraint·context 누락 |
| `"코드 봐줘"` | action·format·constraint 누락 |
| `"리스트로 정리해줘"` | rawTokens=2 → isShortButPrecise 불충족 |
| `"그냥 뭔가 재미있는 이야기를 하나만 짧게 해줘"` | rawTokens=7 < 8, context 없음 |
| `"이거 이거 이거 도와줘"` | 전부 누락 + REDUNDANCY, score=73 |
| `"머신러닝이 뭔가요"` | 질문형, rawTokens=2 < 3 → isShortButPrecise 불충족 |

### 🟢 LACK_OF_SCOPE 미탐지 확인 (탐지 X)

| 프롬프트 | 충족 경로 |
|----------|-----------|
| `"버그 원인 3개 리스트로 정리"` | action+format+context(버그) |
| `"이걸 리스트로 정리해줘"` | isShortButPrecise (3토큰) |
| `"이거 리스트로 정리 요약 압축 비교해줘"` | isShortButPrecise (7토큰) |
| `"좋은 코드 작성 습관 열 가지를 목록으로 정리해줘"` | rawTokens=8 → hasContext 자동 |
| `"스타트업 투자 유치 이메일을 200자 이내 한국어로 작성해줘"` | action+constraint+rawTokens≥8 |
| `"안드로이드 로그인 버그를 디버깅하고 xml 예시로 수정 방향을 보여줘"` | 기술 키워드 다수 |
| `"기획 문서를 초안 형태의 체크리스트로 재작성해줘"` | writingAction+format |
| `"머신러닝의 종류를 리스트로 알려줘"` | colloquial action(알려)+format → isShortButPrecise |
| `"파이썬 기초 문법을 예시로 보여줘"` | colloquial action(보여)+format → isShortButPrecise |
| `"딥러닝 논문을 리스트로 찾아줘"` | colloquial action(찾아)+format + context(논문) |
| `"React 컴포넌트를 리스트로 만들어줘"` | colloquial action(만들어)+format → isShortButPrecise |
| `"테스트 데이터를 리스트로 생성해줘"` | colloquial action(생성)+format → isShortButPrecise |
| `"Python 특징을 리스트로 나열해줘"` | colloquial action(나열)+format → isShortButPrecise |
| `"UI 개선안을 리스트로 제안해줘"` | colloquial action(제안)+format → isShortButPrecise |
| `"월별 매출 데이터를 그래프로 시각화해줘"` | analysis action(시각화)+format(그래프)+context(데이터) |
| `"프로젝트 예산을 단계로 계산해줘"` | analysis action(계산)+format |
| `"내년 매출을 데이터 기반으로 표로 예측해줘"` | analysis action(예측)+format+context |
| `"시스템 구조를 다이어그램으로 그려줘"` | analysis action(그려)+format(다이어그램) |
| `"이 코드의 성능을 단계로 평가해줘"` | analysis action(평가)+format+context(코드) |
| `"문제 원인을 파악하고 단계로 설명해줘"` | analysis action(파악)+format → isShortButPrecise |
| `"서버 API 설계 방법을 단계로 정리해줘"` | context(서버)+action+format |
| `"신규 마케팅 전략을 단계로 정리해줘"` | context(마케팅)+action+format |
| `"이 맥락에서 핵심 포인트를 리스트로 요약해줘"` | context(맥락)+action+format |
| `"사용자 인증 기능을 클래스로 설계해줘"` | context(사용자)+action+format(클래스) |
| `"이 논문을 3줄로 요약해줘"` | context(논문)+action+constraint(줄) |
| `"회사 보고서를 단계로 정리해줘"` | context(회사)+action+format |
| `"팀 업무 현황을 표로 정리해줘"` | context(팀)+action+format |
| `"이 상황을 리스트로 정리해줘"` | context(상황)+action+format → isShortButPrecise |
| `"프로젝트 배경을 표로 정리해줘"` | context(배경)+action+format → isShortButPrecise |
| `"서비스 운영 현황을 단계로 정리해줘"` | context(서비스·운영)+action+format |
| `"db 스키마를 코드로 설명해줘"` | context(db)+action+format(코드) |
| `"계약서의 불리한 조항을 법률 측면에서 리스트로 분석해줘"` | context(법률·계약)+action+format |
| `"환자 진료 기록 작성 방법을 단계로 설명해줘"` | context(환자·진료)+action+format |
| `"주식 포트폴리오 리밸런싱 방법을 단계로 정리해줘"` | context(주식)+action+format |
| `"웹 앱 보안 취약점을 리스트로 정리해줘"` | context(보안·취약점·앱)+action+format |
| `"학생 수업 자료를 단계로 정리해줘"` | context(학생·수업)+action+format |
| `"관련 자료를 리스트로 검색해줘"` | action(검색)+format → isShortButPrecise |
| `"항목을 유형별로 구분해서 리스트로 정리해줘"` | action(구분·정리)+format |
| `"딥러닝 모델 학습 방법을 단계로 알려줘"` | action(학습·알려)+format → isShortButPrecise |
| `"두 데이터를 결합해서 표로 정리해줘"` | action(결합·정리)+format |
| `"프론트엔드와 백엔드를 분리해서 코드로 설명해줘"` | action(분리·설명)+format(코드) → isShortButPrecise |
| `"이 내용을 슬라이드로 정리해줘"` | action+format(슬라이드) → isShortButPrecise |
| `"프로젝트 일정을 타임라인으로 정리해줘"` | action+format(타임라인) → isShortButPrecise |
| `"회의 내용을 요약본으로 작성해줘"` | action+format(요약본) → isShortButPrecise |
| `"배포 프로세스를 다이어그램으로 그려줘"` | action(그려)+format(다이어그램) |
| `"매출 추이를 그래프로 보여줘"` | action(보여)+format(그래프) |
| `"업무 흐름을 순서도로 정리해줘"` | action+format(순서도) |
| `"하기 내용을 개조식으로 정리해줘"` | action+format(개조식) → isShortButPrecise |
| `"단편 소설 초안을 써줘"` | action(초안)+format(초안) 이중 충족 |

---

### ✅ 100점 클린 프롬프트 예시

| 프롬프트 | 충족 경로 |
|----------|-----------|
| `"안드로이드 앱 로그인 오류 원인을 3단계로 요약하고 해결 코드를 예시로 보여줘"` | 다수 신호, rawTokens≥8 |
| `"이 파이썬 함수의 시간복잡도를 분석하고 최적화 방법을 3단계로 알려줘"` | action+format+rawTokens≥8 |
| `"React 컴포넌트 성능 최적화 방법을 초보자 기준으로 단계별 목록으로 정리해줘"` | action+format+rawTokens≥8 |
| `"Python 코드의 memory leak 원인을 분석하고 수정 코드 예시를 보여줘"` | action+format+context |
| `"신규 고객 대상 서비스 소개 이메일 초안을 200자 이내로 작성해줘"` | action+format+constraint+context |
| `"월별 매출 데이터를 분석해서 상위 3개 원인을 표로 정리해줘"` | action+format+context |
| `"이 기술 문서를 영어로 번역하고 500자 이내로 요약해줘"` | action+constraint+context |
| `"이 안드로이드 앱 버그를 디버깅하고 원인을 3단계로 설명해줘"` | action+format+context |
| `"사용자 API 응답 데이터를 json 형식으로 정리해줘"` | action+format+context+rawTokens≥8 |
| `"좋은 코드 작성 습관 다섯 가지를 bullet 포인트로 정리해줘"` | action+format+rawTokens≥8 |
| `"계약서에서 법률적으로 불리한 조항을 리스트로 정리해줘"` | action+format+context(법률·계약) |
| `"주식 투자 포트폴리오를 리스크 수준별로 표로 분류해줘"` | action(분류)+format+context(주식·투자) |
| `"db 쿼리 성능을 최적화하는 방법을 단계로 알려줘"` | action+format+context(db) |
| `"머신러닝 학습 방법을 초보자 기준으로 단계별로 설명해줘"` | action+format → isShortButPrecise(7토큰) |
| `"환자 진료 기록 작성 방법을 단계로 설명해줘"` | action+format+context(환자·진료) |
| `"웹 앱 보안 취약점을 유형별로 리스트로 분석해줘"` | action+format+context(보안·취약점) |

---

## 속성 불변 검증 (Property-based)

| 속성 | 검증 내용 | 결과 |
|------|-----------|------|
| wastedTokens ≤ totalTokens | 4가지 프롬프트 | ✅ |
| efficiencyScore in 0..100 | 극단적 프롬프트 6가지 포함 | ✅ |
| inputTokens > 0 | 비어있지 않은 프롬프트 | ✅ |
| outputTokens > 0 | — | ✅ |
| totalTokens ≥ inputTokens + outputTokens | — | ✅ |
| 긴 프롬프트 inputTokens > 짧은 프롬프트 | — | ✅ |
| 클린 프롬프트 wastedTokens = 0 | — | ✅ |
| modelName.isNotBlank() | 모든 분석 결과 | ✅ |
| REDUNDANCY/VERBOSITY/SCOPE issue description·suggestedFix 비어있지 않음 | — | ✅ |

### suggestedFix 구체화 검증

| 이슈 유형 | 검증 내용 |
|-----------|-----------|
| REDUNDANCY | `suggestedFix`에 탐지된 반복 단어 포함 (예: `'코드'`) |
| VERBOSITY | `suggestedFix`에 탐지된 filler 단어 포함 (예: `'정말'`, `'매우'`) |
| VERBOSITY(connector만) | `suggestedFix`에 "압축" 포함 |
| LACK_OF_SCOPE(action 없음) | `suggestedFix`에 "분석" 등 action 힌트 포함 |
| LACK_OF_SCOPE(format 없음) | `suggestedFix`에 "리스트" 등 format 힌트 포함 |

---

## 알려진 동작 특이사항

### 1. `"그리고 또"` 이중 매칭 → **수정 완료 (2026-06-10)**
- **구 동작**: `"그리고 또"` 하나가 `"그리고"` + `"그리고 또"` 두 항목에 동시 매칭 → count=2 → VERBOSITY 오탐
- **현재 동작**: longest-match 중복 제거 → `"그리고 또"` 포함 시 `"그리고"` 제외 → count=1 → 미트리거

### 2. 단어 하나짜리 Filler
- `"정말"` 단독 입력 시 density=1.0 ≥ 0.15 → VERBOSITY 트리거 (의도된 동작)

### 3. 키워드 중복 등재
- `코드`: technicalOutputFormats + technicalContexts → hasOutputFormat + hasContext 동시 충족
- `초안`: writingActions + writingOutputFormats → hasAction + hasOutputFormat 동시 충족
- `테스트`: technicalActions + qualityConstraints → hasAction + hasConstraint 동시 충족

### 4. `isShortButPrecise` 최소 토큰 경계
- rawTokens=2: `3..8` 범위 밖 → action+format 있어도 SCOPE 탐지
- rawTokens=3: action+format 있으면 SCOPE 면제
- `"리스트로 정리해줘"` (2토큰) → 탐지 O
- `"이걸 리스트로 정리해줘"` (3토큰) → 탐지 X

### 5. density 1/6 vs 1/7 경계
- filler 1개, 6토큰: density=0.167 ≥ 0.15 → VERBOSITY
- filler 1개, 7토큰: density=0.143 < 0.15 → 미트리거

### 6. colloquialActions substring 매칭 주의
- `"알려"` → `알려줘`, `알려주세요`, `알려진` 모두 매칭 (수동형도 hasAction=true 처리됨)

### 7. 조건부 polite filler 판단 로직 (2026-06-10 추가)
- politeFillers만 적용, informalFillers/commonFillers에는 미적용
- 판단 창: filler 뒤 20자 이내 window에서 출력형식/제약 키워드 검색
- 공백 앞 체크: keyword 앞에 공백 또는 문자열 시작 위치여야 인정 (부분 문자열 오매칭 방지)
- 예) `"요약해줄 수 있을까요"`에서 `"줄"` ≠ constraint → 조건 불충족

### 8. cross-script suffix+synonym 복합 처리 (2026-06-10 추가)
- `"python했어"` → suffix `"했어"` 제거 → `"python"` → synonym `"파이썬"`
- 라틴+한글 혼합 토큰도 정상 처리됨

### 9. `"그리고 또한"` substring 주의
- `"그리고 또한"` 원문에는 `"그리고 또"`가 substring으로 포함됨 → foundConnectors에 포함될 수 있음
- longest-match dedup으로 `"그리고 또"` 선택 시 `"그리고"` 제외, `"또한"`은 별도 계산

---

## 향후 테스트 확장 방향

추가하면 의미 있는 영역 (현재 미커버):
- 각 filler 18개 개별 density 테스트 (~36개)
- 각 action 46개 단독 인식 테스트 (~46개)
- 각 outputFormat 26개 단독 인식 테스트 (~26개)
- 각 context 40개 단독 인식 테스트 (~40개)
- connector 8개 조합(8C2=28개) 중 미검증 케이스
- 접미사 23개 각각 strip 동작 (~23개)
- 영문+한글 혼용 suffix+synonym 복합 케이스 (~10개)
- professionalContexts 15개 개별 단독 테스트 (~15개)

**현재 179개 + 추가 시 총 약 350~400개 도달 가능**
