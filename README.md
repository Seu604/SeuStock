# SeuStock

SeuStock은 개인 또는 소규모 팀이 공간, 선반, 박스 단위로 물품과 재고를 관리할 수 있도록 만드는 웹 기반 재고 관리 애플리케이션입니다. Spring Boot와 Thymeleaf, HTMX를 기반으로 서버 렌더링 UI를 구성하고, MyBatis로 PostgreSQL/H2 데이터베이스에 접근합니다.

## 프로젝트 사양

| 구분 | 내용 |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| View | Thymeleaf, HTMX, Tailwind CSS |
| Persistence | MyBatis |
| Database | PostgreSQL, H2(Test) |
| AI | Spring AI Ollama, Gemma, YOLO 연동 준비 |
| Image | Multipart upload, local file storage, client-side analysis image resize |
| QR | ZXing 기반 QR 코드 생성 |
| Build | Gradle Wrapper |
| Test | JUnit 5, Spring Boot Test, MyBatis Test |

## 주요 기능

### 사용자

- 회원가입, 로그인, 로그아웃
- 아이디 중복 확인
- 세션 기반 로그인 사용자별 데이터 접근 제어
- 비밀번호 해시 저장을 위한 Spring Security Crypto 사용

### 공간 관리

- 사용자의 보관 공간 생성, 조회, 수정, 삭제
- 공간 상세 화면에서 하위 선반과 재고 현황 확인
- HTMX 기반 모달/부분 갱신 UI

### 선반 및 박스 관리

- 공간 하위에 선반 생성, 이름 수정, 삭제
- 선반 하위에 박스 생성, 이름 수정, 삭제
- 선반/박스 선택 시 해당 위치의 재고 패널 갱신
- 선반과 박스에 대한 QR 코드 생성 및 QR URL 진입 처리

### 품목 관리

- 품목 생성, 조회, 수정, 삭제
- 품목명과 설명 관리
- 품목 이미지 업로드 및 대표 이미지 표시
- 품목별 위치별 재고 현황 조회
- 품목별 입출고 이력 조회

### 재고 관리

- 공간, 선반, 박스 위치 기준 재고 조회
- 기존 품목을 선택해 재고 등록
- 새 품목과 재고를 한 번에 등록하는 빠른 재고 등록
- 재고 수량 단위 입고/출고 처리
- 재고별 시리얼 번호, 로트 번호, 유통기한, 메모 수정
- 개별 재고 row 단위 수정 및 삭제
- 재고 상태 관리: `IN_STOCK`, `DISPATCHED`, `LOST`, `DAMAGED`, `DISPOSED`
- 재고 트랜잭션 기록: `IN`, `OUT`, `MOVE`, `ADJUST`

### 이미지 및 AI 분석

- 이미지 파일 업로드 및 사용자별 이미지 조회
- 이미지 중복 관리를 위한 content hash 저장
- Spring AI Ollama/Gemma 기반 이미지 분석
- 저장용 원본 이미지와 AI 분석용 최적화 이미지를 분리
- 모바일 클라이언트에서 AI 분석용 이미지를 리사이징하고, 일정 크기 초과 시 WebP로 변환
- 로컬 YOLO 전처리 결과를 Gemma 프롬프트에 포함해 품목명과 설명 후보 정확도 개선 준비
- 업로드 이미지에서 품목명과 설명 후보 자동 생성
- 분석 재시도 시 이전 결과와 다른 표현을 유도하는 retry 파라미터 지원

### QR 코드

- 선반/박스 QR 코드 모달 제공
- QR 이미지 생성 API 제공
- QR 진입 시 대상 위치의 재고 화면으로 연결
- `app.qr-base-url` 설정으로 QR에 포함할 기준 URL 분리

## 프로젝트 구조

```text
src/main/java/com/seu/seustock
├── configuration   # MVC 설정, 인터셉터, 예외 처리, 타입 핸들러
├── controller      # 화면/API 요청 처리
├── mapper          # MyBatis Mapper 인터페이스
├── model
│   ├── dto         # 화면과 서비스에서 사용하는 DTO
│   └── form        # 요청 폼 및 검증 모델
└── service         # 비즈니스 로직

src/main/resources
├── mapper          # MyBatis XML 매핑
├── static          # 정적 이미지, JavaScript
└── templates       # Thymeleaf 페이지와 fragments

src/test
├── java            # JUnit, Mapper, Service, Controller 테스트
└── resources       # H2 테스트 스키마와 테스트 설정
```

## 데이터 모델 요약

- `users`: 사용자 계정
- `spaces`: 사용자별 보관 공간
- `shelves`: 공간 하위 선반
- `boxes`: 선반 하위 박스
- `items`: 품목 마스터 정보
- `stocks`: 물리적 재고 단위
- `stock_transactions`: 재고 입고, 출고, 이동, 조정 이력
- `images`: 업로드 이미지 메타데이터
- `item_images`: 품목과 이미지 연결
- `stock_images`: 재고와 이미지 연결

## 실행 방법

### 1. PostgreSQL 실행

```bash
docker compose up -d postgres
```

개발용 PostgreSQL은 `compose.yaml` 기준으로 `localhost:5433`에 노출됩니다.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 접속 주소는 `http://localhost:8080`입니다.

### 3. 테스트 실행

```bash
./gradlew test
```

테스트는 H2와 `src/test/resources/schema-test.sql`을 사용합니다.

## 주요 설정

`src/main/resources/application.properties`

```properties
server.port=8080
seustock.upload-dir=uploads/images
app.base-url=http://localhost:8080
app.qr-base-url=${app.base-url}
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma3:4b
seustock.ai.yolo.enabled=false
seustock.ai.yolo.base-url=http://localhost:8000
seustock.ai.yolo.image-size=640
seustock.ai.yolo.confidence-threshold=0.25
seustock.ai.yolo.iou-threshold=0.7
```

AI 이미지 분석을 사용하려면 로컬 Ollama 서버와 설정된 Gemma 모델이 준비되어 있어야 합니다. YOLO 전처리를 사용하려면 `seustock.ai.yolo.enabled=true`로 바꾸고 `/detect` 엔드포인트를 제공하는 로컬 YOLO HTTP 서비스를 실행해야 합니다.

## 구현 예정

### spaces, items, stocks 목록 사용성 개선

- `spaces`, `items`, `stocks` 화면 전반에 페이지네이션을 추가할 예정입니다.
- 데이터가 많아져도 목록 화면이 안정적으로 동작하도록 페이지 크기, 현재 페이지, 전체 개수 정보를 함께 관리할 예정입니다.
- 각 목록 화면에 검색 기능을 추가할 예정입니다.
- `spaces`는 공간명 기준 검색을 우선 지원할 예정입니다.
- `items`는 품목명, 설명, 이미지 AI 분석 결과를 활용한 검색 확장을 검토할 예정입니다.
- `stocks`는 품목명, 공간/선반/박스 위치, 시리얼 번호, 로트 번호, 유통기한, 상태, 메모 기준 검색을 지원할 예정입니다.
- 각 목록 화면에 정렬 기능을 추가할 예정입니다.
- 기본 정렬은 생성일 기준 최신순으로 유지하고, 이름순, 수정일/등록일순, 상태순, 유통기한순 등 화면 성격에 맞는 정렬 옵션을 제공할 예정입니다.
- 검색, 정렬, 페이지 상태는 HTMX 부분 갱신과 쿼리 파라미터를 함께 사용해 새로고침이나 URL 공유 시에도 가능한 범위에서 유지되도록 설계할 예정입니다.

### 개인 페이지 기반 보유 품목 AI 평가

- 개인 페이지를 추가해 사용자가 이 앱을 어떤 목적으로 사용하는지 입력할 수 있도록 할 예정입니다.
- 예: 취미 장비 관리, 생활용품 정리, 사무실 비품 관리, 소규모 판매 재고 관리, 공구/부품 관리 등
- 입력된 사용 목적은 사용자 프로필 성격의 데이터로 저장하고, AI 평가 시 기준 문맥으로 사용할 예정입니다.
- 사용자의 전체 품목과 재고 데이터를 DB에서 조회해 목적 대비 보유량과 품목 구성이 적절한지 분석하는 기능을 추가할 예정입니다.
- AI 평가는 "목적에 비해 과하게 보유한 품목", "부족해 보이는 품목군", "중복 가능성이 높은 품목", "정리 또는 처분을 검토할 품목", "계속 보유할 근거가 있는 품목"처럼 실용적인 관점으로 제공할 예정입니다.
- 분석 대상 데이터는 `items`, `stocks`, `stock_transactions`, `spaces`, `shelves`, `boxes`, `stocks.memo`를 함께 조회해 구성할 예정입니다.
- 단순히 품목명만 전달하지 않고 품목 설명, 총 보유 수량, 위치 분포, 상태, 입출고 이력, 유통기한, 메모를 요약한 뒤 AI에 전달할 예정입니다.
- 사용자의 전체 원본 데이터를 그대로 프롬프트에 넣기보다 서비스 계층에서 요약 데이터를 만들고, 품목 수가 많은 경우 페이지 단위/카테고리 단위로 나누어 분석하도록 설계할 예정입니다.
- 개인 페이지에는 사용 목적 입력/수정, 최근 분석 결과, 분석 재실행 버튼, 평가 기준 안내를 제공할 예정입니다.
- DB 변경으로는 사용자 목적을 저장할 `user_profiles` 테이블 또는 `users` 테이블의 profile 컬럼 확장을 검토할 예정입니다.
- 서비스 구현은 `UserService` 또는 별도 `UserProfileService`, 품목/재고 요약 조회용 Mapper, AI 평가 전용 Service로 분리할 예정입니다.
- AI 평가 결과는 매번 실시간으로 생성할 수도 있지만, 비용과 응답 시간을 고려해 분석 결과 저장 테이블을 두고 최근 결과를 캐싱하는 방식을 검토할 예정입니다.
- 평가 결과는 참고용 제안으로 표시하고, 사용자에게 실제 삭제/처분을 강제하지 않도록 UI 문구를 구성할 예정입니다.

### 재고 개별 KEEP 및 출고 제외

- 개별 재고 단위로 KEEP 여부를 지정할 수 있도록 할 예정입니다.
- KEEP된 재고는 실제 보유 재고에는 남아 있지만, 출고 가능 수량 계산에서는 제외할 예정입니다.
- 출고 처리 시 `IN_STOCK` 상태이면서 KEEP되지 않은 재고만 출고 후보로 조회하도록 변경할 예정입니다.
- 위치/품목별 재고 화면에서는 전체 보유 수량과 출고 가능 수량을 구분해 표시하는 방식을 검토할 예정입니다.
- KEEP 설정/해제는 재고 상세 row 또는 상세 모달에서 수행하고, HTMX 부분 갱신으로 화면에 즉시 반영할 예정입니다.

### 재고별 이미지 업로드 및 AI 통합

- 품목 이미지 중심으로 동작하는 현재 이미지 업로드/AI 분석 흐름을 재고 개별 단위까지 확장할 예정입니다.
- `stock_images` 연결 테이블을 활용해 재고별 대표 이미지와 추가 이미지를 관리할 예정입니다.
- 재고 이미지 업로드 시 AI 분석 결과를 재고 메모, 상태 판단 보조, 품목 설명 보강 등에 활용할 수 있도록 통합할 예정입니다.

### YOLO + Gemma 이미지 분석 정확도 개선

- 기존 Gemma 단독 이미지 분석 흐름을 `ImageAnalysisService` 인터페이스 기반으로 분리하고, 구현체에서 리사이징 → YOLO 전처리 → Gemma 최종 추론 순서로 처리할 예정입니다.
- 저장용 업로드 FormData는 원본 이미지를 유지하고, `/images/analyze` 요청에만 클라이언트에서 만든 분석용 리사이징 이미지를 전송합니다.
- 모바일 환경에서 업로드 이미지가 일정 크기를 초과하면 분석용 이미지를 WebP로 변환하고, WebP 변환이 실패하면 JPEG 또는 원본 분석으로 fallback합니다.
- YOLO는 로컬 HTTP 서비스로 연동하며, 객체 label, confidence, bounding box를 Gemma 프롬프트의 보조 단서로 전달합니다.
- YOLO 서비스가 비활성화되어 있거나 호출에 실패하면 기존 Gemma 단독 분석으로 fallback합니다.

## 구현 예정 기능 구현 가능성 평가

현재 코드베이스를 분석한 결과를 바탕으로, 구현 예정 기능의 구현 가능성을 평가합니다.

### spaces / items / stocks 목록 사용성 개선 (페이지네이션·검색·정렬)

**평가: ✅ 구현 가능 — 난이도: 중간**

- `SpaceMapper`, `ItemMapper`, `StockMapper` 모두 현재는 단순 `findByUserId(Long userId)` 형태로 전체 조회만 지원한다.
- `StockMapper`에는 이미 `searchDetails` 형태의 다중 조건 검색 메서드가 존재하며, 이것이 페이지네이션/검색 확장의 선례가 된다.
- MyBatis XML에서 `LIMIT #{pageSize} OFFSET #{offset}` 구문과 `<if test="keyword != null">` 조건 분기를 추가하는 방식이 이미 팀이 익숙한 패턴이다.
- HTMX 기반 부분 갱신 UI가 이미 spaces/items/stocks 화면에 적용되어 있어, 검색/정렬/페이지 버튼에 `hx-get` + 쿼리 파라미터를 붙이는 방식으로 자연스럽게 확장된다.

**주요 작업:**
- 각 Mapper에 `count` 쿼리와 `findPage` 쿼리 추가 (XML 수정)
- 페이지네이션 정보를 담는 공통 `PageRequest`/`PageResult` DTO 생성
- 검색 키워드·정렬 기준을 받는 파라미터를 Controller → Service → Mapper로 전달
- Thymeleaf 목록 fragment에 페이지 버튼, 검색 입력창, 정렬 드롭다운 추가

**리스크:** `items` AI 분석 결과 기반 검색은 별도 인덱싱이나 LIKE 쿼리 한계가 있으나, 1단계에서는 이름/설명 LIKE 검색만으로도 충분히 시작 가능하다.

---

### 개인 페이지 기반 보유 품목 AI 평가

**평가: ✅ 구현 가능 — 난이도: 높음**

- `GemmaVisionClient`가 이미 `ChatClient` (Spring AI Ollama)를 사용해 프롬프트 구성, 온도 조절, retry 파라미터 처리, `BeanOutputConverter`로 구조화된 응답 파싱까지 수행한다. 동일한 패턴으로 텍스트 기반 AI 평가 서비스를 구현할 수 있다.
- `StockMapper.searchDetails`, `StockTransactionMapper`, `ItemMapper` 등 필요한 데이터 조회 인프라가 이미 갖춰져 있다.
- `users` 테이블 확장 또는 별도 `user_profiles` 테이블 추가는 기존 MyBatis 패턴으로 단순하게 구현 가능하다.

**주요 작업:**
- DB: `user_profiles` 테이블 또는 `users.purpose` 컬럼 추가 (schema 2곳 수정: `init.sql`, `schema-test.sql`)
- `UserProfileService` + `UserProfileMapper` 신규 생성
- 품목·재고 요약 데이터를 조립하는 `StockSummaryService` 또는 전용 Mapper 쿼리 추가
- `AiEvaluationService`: 요약 데이터를 프롬프트로 구성하고 Ollama 호출, 결과 파싱
- AI 평가 결과 캐싱용 `ai_evaluation_results` 테이블 추가 (선택)
- 개인 페이지 Controller + Thymeleaf 페이지 신규 추가

**리스크:**
- 품목이 많아질수록 프롬프트가 길어지는 토큰 제한 문제 → "페이지/카테고리 단위 분할" 전략이 필수
- Ollama 로컬 서버 의존성(latency)이 있으나 이미 프로젝트에서 수용된 조건이다

---

### 재고 개별 KEEP 및 출고 제외

**평가: ✅ 구현 가능 — 난이도: 중간**

- 현재 `stocks` 테이블은 개별 재고 row를 물리적 재고 단위로 관리하고 있어, KEEP 여부를 `stocks.is_kept` 같은 boolean 컬럼으로 추가하기에 구조가 적합하다.
- 출고 로직은 `StockService.dispatchUnits`에서 위치/품목 기준으로 `findInStockByItemAndBox/Shelf/Space`를 조회한 뒤 `subList(0, count)`로 출고 대상을 선택한다. 따라서 해당 조회 쿼리에 `AND is_kept = FALSE` 조건을 추가하면 KEEP된 재고를 출고 후보에서 제외할 수 있다.
- `updateStatusIfInStock`은 상태 변경 직전에 한 번 더 `status = 'IN_STOCK'` 조건을 확인하므로, KEEP 보호를 강화하려면 `AND is_kept = FALSE` 조건을 함께 추가하는 것이 적절하다.
- `StockDetailDTO`, `StockDTO`, `StockUpdateForm`에 KEEP 필드를 반영하면 기존 개별 재고 상세/수정 UI 패턴을 그대로 사용할 수 있다.

**주요 작업:**
- DB: `stocks` 테이블에 `is_kept BOOLEAN NOT NULL DEFAULT FALSE` 컬럼 추가 (`init.sql`, `schema-test.sql`)
- `StockDTO`, `StockDetailDTO`, `StockUpdateForm`: KEEP 필드 추가
- `StockMapper.xml`: 출고 후보 조회, 출고 상태 변경, 상세 조회, 목록/집계 쿼리에 KEEP 조건 또는 표시 필드 반영
- `StockService.dispatchUnits`: 출고 가능 수량 부족 메시지가 KEEP 제외 기준으로 계산되도록 검증
- 재고 상세 fragment/list row에 KEEP 토글 또는 체크박스 추가
- KEEP 설정/해제에 대한 Mapper/Service/Controller 메서드와 mapper/service 테스트 추가

**리스크:** 현재 화면의 재고 수량은 대부분 `status = 'IN_STOCK'` 기준으로 집계된다. KEEP을 "보유 수량에는 포함, 출고 가능 수량에는 제외"로 정의하면 기존 집계 쿼리 중 어떤 화면이 전체 보유 수량을 보여야 하는지, 어떤 화면이 출고 가능 수량을 보여야 하는지 명확히 나눠야 한다.

---

### 재고별 이미지 업로드 및 AI 통합

**평가: ✅ 구현 가능 — 난이도: 중간**

- `StockImageMapper`가 이미 존재하며(`insertStockImage`, `unsetPrimaryByStockId`), `stock_images` 테이블도 DB 스키마에 이미 정의되어 있다.
- 품목 이미지(`ItemImageMapper`, `item_images`)와 완전히 동일한 구조가 재고용으로도 준비된 상태다.
- `ImageStorageService`와 `ImageAnalysisService` 인터페이스는 `MultipartFile`을 받아 처리하는 범용 흐름으로 재사용 가능하다.

**주요 작업:**
- `StockImageMapper`에 조회 메서드 추가 (`findByStockId`, `findPrimaryByStockId` 등, 현재는 insert만 있음)
- `StockService`에 이미지 업로드 + AI 분석 + 재고 메모/상태 반영 로직 추가
- 재고 상세 fragment에 이미지 업로드 UI 추가 (품목 이미지 업로드 fragment 참고)

**리스크:** `StockImageMapper`에 조회 메서드가 없어 XML과 인터페이스를 같이 추가해야 하지만, 기존 `ItemImageMapper` 패턴을 그대로 복사·응용하면 된다.

---

### YOLO + Gemma 이미지 분석 정확도 개선

**평가: ✅ 구현 가능 — 난이도: 중간**

- `ImageAnalysisService`가 인터페이스로 분리되어 컨트롤러는 분석 구현체에 직접 의존하지 않는다.
- `YoloGemmaImageAnalysisService`는 서버 최종 리사이징, YOLO 전처리, Gemma 최종 추론을 orchestration한다.
- `image-upload.js`는 저장용 원본 이미지와 분석용 최적화 이미지를 분리해 모바일 업로드 비용을 줄인다.
- YOLO는 로컬 HTTP 서비스로 두어 Spring Boot 앱 내부에 Python/모델 런타임을 직접 포함하지 않는다.

**주요 작업:**
- YOLO 응답 포맷을 실제 로컬 서비스와 맞춰 확정
- YOLO 탐지 결과가 많은 경우 상위 N개만 Gemma 프롬프트에 전달
- 모바일 브라우저별 WebP 변환 지원 여부와 fallback 확인
- 실제 물품 이미지 샘플로 Gemma 단독 분석 대비 정확도 비교

---

### 종합 평가 요약

| 기능 | 구현 가능성 | 난이도 | 기존 인프라 활용도 |
| --- | --- | --- | --- |
| 목록 페이지네이션·검색·정렬 | ✅ 가능 | 중간 | MyBatis XML 확장, HTMX 패턴 재사용 |
| AI 개인 평가 페이지 | ✅ 가능 | 높음 | `GemmaVisionClient` ChatClient 패턴 재사용 |
| 재고 개별 KEEP 및 출고 제외 | ✅ 가능 | 중간 | 개별 재고 row, 출고 후보 조회 로직 재사용 |
| 재고별 이미지·AI 통합 | ✅ 가능 | 중간 | `StockImageMapper`, `ImageStorageService` 기존 존재 |
| YOLO + Gemma 이미지 분석 정확도 개선 | ✅ 가능 | 중간 | 클라이언트 리사이징, Spring AI, 로컬 YOLO HTTP 연동 |

구현 예정 기능은 모두 현재 코드베이스의 설계·패턴 범위 안에서 구현 가능합니다. `stocks.memo`와 개별 재고 관리는 이미 구현되어 있으므로, 다음 우선순위는 **YOLO + Gemma 이미지 분석 정확도 개선 → 재고 개별 KEEP 및 출고 제외 → 목록 사용성 개선 → 재고별 이미지·AI 통합 → AI 개인 평가 페이지** 입니다.

## 개발 참고

- Gradle은 시스템 설치본이 아니라 `./gradlew`를 사용합니다.
- Java toolchain은 25로 설정되어 있습니다.
- DB 스키마 변경 시 `docker/postgres/init/init.sql`과 `src/test/resources/schema-test.sql`을 함께 갱신해야 합니다.
- Persistence 변경 시 관련 Mapper 테스트를 함께 수정하거나 추가합니다.
