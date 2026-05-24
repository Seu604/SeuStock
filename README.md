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
| AI | Spring AI Ollama |
| Image | Multipart upload, local file storage |
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
- 재고별 시리얼 번호, 로트 번호, 유통기한 수정
- 재고 상태 관리: `IN_STOCK`, `DISPATCHED`, `LOST`, `DAMAGED`, `DISPOSED`
- 재고 트랜잭션 기록: `IN`, `OUT`, `MOVE`, `ADJUST`

### 이미지 및 AI 분석

- 이미지 파일 업로드 및 사용자별 이미지 조회
- 이미지 중복 관리를 위한 content hash 저장
- Spring AI Ollama 기반 이미지 분석
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
spring.ai.ollama.chat.model=gemma4:e2b
```

AI 이미지 분석을 사용하려면 로컬 Ollama 서버와 설정된 모델이 준비되어 있어야 합니다.

## 구현 예정

### stocks 페이지 개별 재고 관리 강화

- 현재 위치/품목 기준으로 집계되는 재고 화면에서 개별 재고 단위 관리 기능을 강화할 예정입니다.
- 개별 재고 row 단위로 상세 조회, Update, Delete를 수행할 수 있도록 UI와 API를 정리할 예정입니다.
- 재고 삭제는 위치/품목 묶음 전체 삭제뿐 아니라 특정 재고 1건 삭제를 지원하도록 확장할 예정입니다.

### 재고별 이미지 업로드 및 AI 통합

- 품목 이미지 중심으로 동작하는 현재 이미지 업로드/AI 분석 흐름을 재고 개별 단위까지 확장할 예정입니다.
- `stock_images` 연결 테이블을 활용해 재고별 대표 이미지와 추가 이미지를 관리할 예정입니다.
- 재고 이미지 업로드 시 AI 분석 결과를 재고 메모, 상태 판단 보조, 품목 설명 보강 등에 활용할 수 있도록 통합할 예정입니다.

### stocks 테이블 memo 컬럼 추가

- 재고 개별 단위에 메모를 남길 수 있도록 `stocks` 테이블에 `memo` 컬럼을 추가할 예정입니다.
- 생산 스키마와 테스트 스키마를 함께 수정합니다.
- `StockDTO`, `StockDetailDTO`, `StockUpdateForm`, `StockMapper.xml`, 재고 상세 fragment에 memo 필드를 반영할 예정입니다.
- 기존 `stock_transactions.memo`는 입출고/조정 이력 메모로 유지하고, `stocks.memo`는 현재 재고 자체에 대한 고정 메모로 분리할 예정입니다.

## 개발 참고

- Gradle은 시스템 설치본이 아니라 `./gradlew`를 사용합니다.
- Java toolchain은 25로 설정되어 있습니다.
- DB 스키마 변경 시 `docker/postgres/init/init.sql`과 `src/test/resources/schema-test.sql`을 함께 갱신해야 합니다.
- Persistence 변경 시 관련 Mapper 테스트를 함께 수정하거나 추가합니다.
