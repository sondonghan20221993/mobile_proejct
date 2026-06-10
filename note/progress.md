# Prompt Diet 프로젝트 작업 현황 및 목표

## 📅 최종 업데이트: 2026-06-11 (입력창 안내 문구 추가)

## ✅ 완료된 작업

### 1. 프로젝트 환경 설정
- ViewBinding 활성화
- Material Design 및 ConstraintLayout 기반 UI 환경 구축

### 2. 리소스 정의
- 앱 고유 컬러셋(`header_blue`, `box_grey`, `score_good`, `score_medium`, `score_bad` 등) 정의
- 주요 텍스트 리소스(`strings.xml`) 및 메뉴 구조(`bottom_nav_menu.xml`) 작성

### 3. 레이아웃 구현
- 메인 화면: 하단 내비게이션 바 및 프래그먼트 컨테이너 구조
- 분석 화면: 채팅 텍스트 입력부(clear 버튼, 글자 수 카운터 포함), 분석 시작 버튼, 결과 표시 영역(ScrollView)
- 통계 화면: 프로필, 상위 원인, 세부 원인 버튼 대시보드 구조 + IssueBarChartView 연동
- 세부 통계 화면: 상세 로그 데이터 RecyclerView (슬라이드업 레이아웃 애니메이션 적용)

### 4. 기능 로직 및 분석 엔진
- 하단 탭 전환 기능 (Analysis ↔ Statistics)
- '세부 추정 원인' 클릭 시 상세 화면 이동 로직
- **PromptAnalyzer 엔진**: 중복(Redundancy), 장황(Verbosity), 범위 부족(Lack of Scope) 탐지, 가상 토큰 계산 및 효율성 점수 산정

### 5. 통계 데이터 시각화
- `IssueBarChartView`: 커스텀 막대 차트 (중복/장황/범위 이슈 분포 표시)
  - 데이터 로드 시 막대 성장 애니메이션 (600ms, DecelerateInterpolator)
  - 숫자 카운트 페이드인 효과
- `StatisticsViewModel`이 `chartEntries` 계산 → LiveData로 Fragment에 전달
- 총 분석 수, 평균/최고 점수, 낭비 토큰, 누적 비용, 최근 추세 표시

### 6. 데이터 저장 및 관리 (Room)
- `PromptHistoryEntity`, `PromptIssueEntity` (1:N 관계)
- `PromptHistoryRepository`를 통한 분석 결과 자동 저장
- 전체 기록 삭제 기능 (AlertDialog 확인 포함)

### 7. UI/UX 고도화
- **분석 화면**: 입력창 clear 버튼(`endIconMode="clear_text"`), 글자 수 카운터(최대 1500자), 결과 텍스트 가독성 개선(15sp, lineSpacingExtra 3dp)
- **기록 카드**: 좌측 점수 컬러 스트라이프 (초록/주황/빨강)로 한눈에 점수 파악
- **HistoryAdapter**: `ListAdapter` + `DiffUtil` 전환 → 항목 추가/삭제 시 부드러운 애니메이션
- **시맨틱 컬러**: `score_good`, `score_medium`, `score_bad` 색상 리소스 추가
- **애니메이션**: `slide_up.xml`, `layout_anim_slide_up.xml` 추가 → 세부 통계 목록 등장 효과
- **입력창 안내 문구**: hint → "AI에게 보낼 프롬프트를 입력하세요", helperText → "단일 요청문만 입력하세요 (대화 로그·코드 전체 제외)" 추가

### 8. 빌드 환경 정비
- KSP `2.1.20-1.0.32` → `2.2.10-2.0.2` (Kotlin 2.2.10 대응)
- Room `2.6.1` → `2.7.2` (KSP2 호환)
- `android.disallowKotlinSourceSets=false` 추가 (AGP 9.x built-in Kotlin 대응)

### 9. PromptAnalyzer 정확도 개선
- **구어체 동작 동사 추가** (`PromptKeywordDictionary.kt`): `알려`, `보여`, `만들어`, `찾아`, `제안`, `생성`, `나열`
  - "머신러닝 종류를 리스트로 알려줘" 같은 자연스러운 프롬프트에서 hasAction=false로 인한 오탐 수정
- **Connector 중복 집계 버그 수정** (`PromptAnalyzer.kt`): longest-match 중복 제거
  - "그리고 또"가 "그리고"와 "그리고 또" 둘 다 매칭 → count=2 → 오탐이었던 버그 수정
  - 더 긴 phrase에 포함되는 짧은 connector는 집계에서 제외
- 테스트 79개 전부 통과 (51 → 54 → 79개)
  - Fix 검증 3개: 구어체 동사 인식, 그리고 또 중복 제거
  - 데이터셋 확장 +25개: REDUNDANCY 경계(5), VERBOSITY 경계(5), 구어체 동사 완전 커버(4), 새 도메인 좋은 프롬프트(6), 점수 정밀(2), Issue 품질(3)

### 10. PromptAnalyzer 3차 개선
- **조건부 filler 오탐 수정** (`PromptAnalyzer.kt`): `가능하면`, `괜찮다면` 등 polite filler가 출력 형식·제약 키워드 앞에 오면 조건문으로 판단하여 장황 집계 제외
  - 단어 앞 공백 유무 검사로 `요약해줄`의 `줄`같은 부분 문자열 오매칭 방지
- **동의어 정규화** (`PromptKeywordDictionary.kt`, `PromptAnalyzer.kt`): `synonymGroups` 맵 추가
  - `python`=`파이썬`, `chatgpt`=`gpt`, `js`=`자바스크립트`, `리액트`=`react` 등 19쌍
  - `simplifyToken()`에서 suffix 제거 후 동의어 정규화 적용 → 언어 혼용 중복 탐지 가능
- **suggestedFix 구체화**: 탐지된 실제 키워드를 메시지에 포함
  - REDUNDANCY: `"'코드' 등 반복 단어를 한 번만 쓰거나 대명사로 대체하세요."`
  - VERBOSITY: `"'정말', '매우' 같은 수식어를 제거하고 핵심만 전달하세요."`
  - LACK_OF_SCOPE: 누락 항목별 힌트 (`무엇을 해달라는지`, `어떤 형식으로`, `어떤 상황인지 배경`)
- **`개조식` 출력 형식 추가**: writingOutputFormats에 등재
- **테스트 107개** (79 → 107): 조건부 filler(7), 동의어(5), suggestedFix(5), 실전 프롬프트(11)

### 11. PromptAnalyzer 키워드 사전 2차 확장 (107→126)
- **informalFillers 추가** (`PromptKeywordDictionary.kt`): `그냥`, `제발`, `살짝`
- **analysisActions 추가**: `시각화`, `계산`, `예측`, `평가`, `파악`, `그려`
- **commonConnectors 추가**: `게다가`, `아울러` (총 7개)
- **technicalOutputFormats 추가**: `다이어그램`, `그래프`, `순서도`
- **writingOutputFormats 추가**: `개조식`
- **businessContexts 추가**: `논문`, `회사`, `팀`
- **synonymGroups 초기 구성** (24쌍): python/파이썬, js/자바스크립트, ts/타입스크립트, kotlin/코틀린, java/자바, android/안드로이드, chatgpt→gpt, ml→머신러닝, react/리액트, vue/뷰, database/데이터베이스
- **conditionalFillerPhrases 도입**: politeFillers를 Set으로 관리, 조건부 판단 로직 연동
- **테스트 126개** (107→126): informalFiller(5), analysisActions(5), 새 connector(2), 새 format(3), 새 context(2), 새 synonym(2)

### 12. PromptAnalyzer 4차 개선
- **동사 suffix 스트리핑 확장** (`PromptAnalyzer.kt`): `simplifyToken()`에 9개 동사 활용형 추가
  - `합니다`, `한다` (공식 현재형)
  - `했어요`, `했어` (과거형, 긴 것 우선)
  - `해요`, `해서`, `하면`, `하여`, `하는` (기타 활용형)
  - 이제 "설명합니다", "설명했어", "설명한다" 등 다른 활용형도 "설명"으로 정규화 → 중복 탐지 정확도 향상
  - 긴 suffix 우선(`해주세요`>`해줘`, `했어요`>`했어`, `적인지`>`적인`) 정렬로 오탐 방지
- **테스트 147개** (126 → 147): 새 suffix 형태(6), 미커버 키워드(11), 실전 프롬프트(4)
  - suffix: `합니다/했어/한다/하면/하여해서하는` 각 형태별 중복 탐지, 2-form만으로는 미트리거
  - 미커버: `파악` action, `상황/배경/운영/db` context, `아울러/추가로` connector 단독, `그냥+살짝` filler pair, `살짝` density 경계, `되도록이면` density, `보고서` format
  - 실전: db 쿼리 최적화, 머신러닝 교육, API JSON 설계 등

### 13. PromptAnalyzer 5차 개선 — 도메인 확장
- **professionalContexts 추가** (`PromptKeywordDictionary.kt`): 15개 전문 도메인 키워드
  - 법/법률/계약 (법률), 의료/진료/환자 (의료), 금융/투자/주식 (금융)
  - 보안/취약점 (사이버보안), 게임, 교육/학생/수업
  - default()의 contextKeywords에 통합: SCOPE 오탐 감소 효과
- **planningActions 확장**: `구분`, `분리`, `결합`, `검색`, `학습` 5개 추가
- **writingOutputFormats 확장**: `요약본`, `슬라이드`, `타임라인` 3개 추가
- **commonConnectors 추가**: `더불어`
- **synonyms 확장** (18→24쌍): `py`→파이썬, `dl`→딥러닝, `ai`, `css`, `html`, `swift`, `flutter`
- **테스트 167개** (147→167): professionalContexts(5), 새 action(4), 새 format(3), 더불어 connector(2), 새 synonym(3), 실전(3)

### 14. 테스트 6차 확장 (167→179)
- **cross-script 복합 테스트**: suffix 스트리핑 + 동의어 정규화 동시 적용 검증
  - "python했어 파이썬하고 py한다" → 모두 "파이썬"으로 정규화 → REDUNDANCY
- **유사 ML 용어 구별**: ml→머신러닝(×2) vs 딥러닝(×1) → 별개 개념이므로 no REDUNDANCY
- **미커버 action**: `분리` 검증
- **새 connector 조합**: 또한+아울러, 게다가+더불어, 3개 동시 등 7가지 조합
- **새 synonym 중복**: css×3, flutter×3 → REDUNDANCY
- **전문 도메인 복합 프롬프트**: 법률 계약, 금융 포트폴리오, 교육 슬라이드 완전 clean
- **점수 검증**: 새 connector(또한+아울러)로 VERBOSITY만 → score=92

### 15. PromptAnalyzer 7차 개선 — 조사 suffix 추가 (179→186)
- **주격·목적격·보조사 6개 추가** (`PromptAnalyzer.kt`): `을`, `를`, `이`, `가`, `은`, `는`
  - 기존 length 체크(`token.length > suffix.length + 1`)로 안전하게 적용 — 1글자 단독 토큰은 제거 안 함
  - "파이썬을 파이썬으로 파이썬에서" 같이 조사만 다른 동일 어근이 3회 등장 시 REDUNDANCY 탐지 가능
  - suffix 제거 → synonym 정규화 연쇄 적용: "python을"→"python"→"파이썬", "py는"→"py"→"파이썬" 등
- **테스트 186개** (179→186): 조사별 REDUNDANCY 탐지(6개), 미트리거 경계(1개)

## 🎯 향후 작업 목표
- 추가 UI 개선 또는 기능 확장 (필요 시 논의)
