# Prompt Diet — 기술 스택 구현 기록

> 작성일: 2026-06-10  
> 구현된 항목만 기록. 추가될 때마다 해당 섹션에 업데이트.

---

## 1. UI 레이어

### 프레임워크
- **XML + ViewBinding** — `buildFeatures { viewBinding = true }`
- Jetpack Compose 미사용

### 레이아웃
- `ConstraintLayout` — 분석 화면, 통계 화면 메인 구조
- `LinearLayout` — 히스토리 카드 내부 (좌측 점수 스트라이프 + 콘텐츠 영역)

### Material Design 3 컴포넌트
- `TextInputLayout` — 프롬프트 입력창
  - `endIconMode="clear_text"` — 입력 초기화 버튼
  - `counterEnabled="true"`, `counterMaxLength="1500"` — 글자 수 카운터
- `MaterialButton` — 분석 시작 버튼
- `BottomNavigationView` — 분석 / 통계 탭 전환

### 리스트
- `RecyclerView` + `ListAdapter<PromptHistoryWithIssues, VH>(DiffUtil.ItemCallback)`
  - `areItemsTheSame`: `history.id` 비교
  - `areContentsTheSame`: `history == new.history && issues == new.issues` 비교
  - 항목 추가·삭제 시 자동 diff 애니메이션

### 커스텀 뷰
- `IssueBarChartView` — Canvas 직접 드로잉 막대 차트
  - `ValueAnimator.ofFloat(0f, 1f)` + `DecelerateInterpolator` — 막대 성장 애니메이션 (600ms)
  - count 텍스트 알파 페이드인 (`animatedFraction * 255`)
  - `RoundRect` 배경 + 컬러 막대

### Rich Text
- `SpannableStringBuilder` — 분석 결과 화면
  - `BackgroundColorSpan` — 점수 뱃지, 중복 단어 하이라이트
  - `ForegroundColorSpan` — 이슈 유형별 색상
  - `StyleSpan(Typeface.BOLD)` — 섹션 헤더

### 애니메이션
- `res/anim/fade_in.xml` — 분석 결과 영역 등장
- `res/anim/slide_up.xml` — 히스토리 개별 아이템 (translate Y + alpha, 280ms)
- `res/anim/layout_anim_slide_up.xml` — RecyclerView `layoutAnimation` (12% delay)
- `AnimationUtils.loadAnimation()` — Fragment에서 수동 적용

### 색상 리소스
- `score_good` `#1B7C4A`, `score_medium` `#B45309`, `score_bad` `#B91C1C` — 점수 컬러 스트라이프 및 결과 텍스트

---

## 2. 아키텍처 레이어

- **MVVM** 패턴
- `AnalysisViewModel` — 분석 결과 저장 요청
- `StatisticsViewModel` — 히스토리 LiveData 구독 → `StatisticsUiState` 변환
- `ViewModelProvider.Factory` 수동 구현 (`AnalysisViewModelFactory`, `StatisticsViewModelFactory`)
- `PromptHistoryRepository` — DAO 래핑, 저장·삭제·조회 인터페이스 제공

---

## 3. 데이터 레이어

### Room 2.7.2 + KSP2 (2.2.10-2.0.2)
- `@Entity` — `PromptHistoryEntity` (prompt_history 테이블), `PromptIssueEntity` (prompt_issues 테이블)
- 관계 — `PromptHistoryWithIssues`: 1(history) : N(issues), `@Relation` 사용
- `@Dao` — `PromptHistoryDao`: `insertHistory()`, `insertIssues()`, `observeAll()`, `deleteAll()`
- `@Database` — `AppDatabase` 싱글턴 (`getInstance(context)`)

### 네트워크 / 이미지 / 설정
- 미사용

---

## 4. 비동기 처리

- **Coroutines** — `viewModelScope.launch { }` (저장, 삭제)
- **LiveData** — Room DAO `@Query` 반환 → Repository → ViewModel → Fragment `observe()`
- **MediatorLiveData** — `statisticsState`: `history` LiveData를 소스로 받아 `StatisticsUiState`로 변환

---

## 5. 내비게이션

- **탭 전환** — `BottomNavigationView.setOnItemSelectedListener` → `FragmentManager.beginTransaction().replace()` 수동 처리
- **화면 이동** — `Intent(context, DetailStatisticsActivity::class.java)` 명시적 인텐트
- Navigation Component 미사용

---

## 6. DI (의존성 주입)

- **수동 DI** — `PromptDietApplication : Application()`
  ```kotlin
  val repository by lazy {
      PromptHistoryRepository(AppDatabase.getInstance(this).promptHistoryDao())
  }
  ```
- Fragment에서 `requireActivity().application as PromptDietApplication` 캐스팅 후 ViewModel Factory에 전달
- Hilt 미사용

---

## 7. 테스트

- **JUnit4** 단위 테스트 (`src/test/`) — 기기 없이 로컬 실행
- `PromptAnalyzerTest` — **179개**, 0.094초
  - REDUNDANCY / VERBOSITY / LACK_OF_SCOPE 경계값, 점수 정밀, 도메인별 좋은 프롬프트, Issue 품질, 속성 불변 검증
  - suffix 스트리핑, 동의어 정규화, 조건부 filler, connector 중복 제거, suggestedFix 구체화 각 검증
- Espresso instrumented test 미사용

---

## 8. 빌드 환경

| 항목 | 버전 |
|------|------|
| AGP | 9.1.1 |
| Kotlin (built-in) | 2.2.10 |
| KSP2 | 2.2.10-2.0.2 |
| compileSdk | 36 (minorApiLevel 1) |
| minSdk | 24 |
| targetSdk | 36 |
| Room | 2.7.2 |
| Lifecycle (ViewModel·LiveData) | 2.8.7 |
| Material | 1.10.0 |

- `android.disallowKotlinSourceSets=false` — AGP 9.x built-in Kotlin sourceSets 오류 대응

---

## 9. 외부 API

- 미사용 — 분석 엔진 전부 로컬 룰 기반 (`PromptAnalyzer`, `PromptKeywordDictionary`)
