# Prompt Diet 프로젝트 작업 현황 및 목표

## 📅 최종 업데이트: 2026-06-10 (분석 엔진 3차 개선 + 테스트 107개)

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

## 🎯 향후 작업 목표
- 추가 UI 개선 또는 기능 확장 (필요 시 논의)
