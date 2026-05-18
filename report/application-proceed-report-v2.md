# 물품관리 애플리케이션 개발 진행 보고서 (v2)

**작성일:** 2026-05-18  
**이전 버전:** application-proceed-report-v1.md  
**변경 배경:** 렌더링 방식을 REST API + 프론트엔드에서 **Thymeleaf SSR + HTMX 부분 인터랙션** 방식으로 변경

---

## 1. 프로젝트 개요

- **프로젝트 명**: SeuStock (물품 및 재고 관리 시스템)
- **주요 목적**: 사용자가 공간(Space) > 선반(Shelf) > 박스(Box)로 구성된 계층적 위치에 품목(Item)의 재고(Stock)를 등록·관리하고, 입출고 이력(Stock Transaction)을 추적하는 시스템
- **목표 범위**: MVP (Minimum Viable Product)

### 1.1 기술 스택

| 계층 | 기술 |
|---|---|
| 언어 / 런타임 | Java 25, Spring Boot 4.0.6 |
| 빌드 | Gradle (Kotlin DSL) |
| 데이터베이스 | PostgreSQL (운영), H2 인메모리 (테스트) |
| ORM | MyBatis (XML 매퍼) |
| 뷰 | Thymeleaf (SSR) |
| 부분 인터랙션 | HTMX (htmx-spring-boot-thymeleaf 5.1.0) |
| 테스트 | JUnit 5, `@MybatisTest` |

### 1.2 렌더링 전략 (v1 대비 변경)

**v1 계획 (폐기):** REST API + 별도 프론트엔드 프레임워크  
**v2 채택:** Thymeleaf SSR 기반, HTMX는 아래 인터랙션에만 한정 적용

| HTMX 적용 대상 | HTMX 미적용 (전체 페이지 이동) |
|---|---|
| 모달 열기/닫기 | 페이지 간 네비게이션 |
| 인라인 폼 제출 결과 반영 | 목록 페이지 최초 진입 |
| 삭제 확인 후 행(row) 제거 | 상세/등록 페이지 이동 |
| 취소 버튼으로 폼 되돌리기 | 로그인/회원가입 |

> **원칙**: 전체 화면 재로딩 없이 UX가 명확히 개선되는 경우에만 HTMX를 사용한다. 단순 페이지 이동은 일반 `<a>` 링크로 처리한다.

---

## 2. 현재 개발 완료 현황

### 2.1 인프라 및 프로젝트 설정 (완료)

- Docker Compose 기반 PostgreSQL 환경 (포트 5433)
- `spring-boot-docker-compose`로 `bootRun` 시 자동 컨테이너 기동
- 데이터베이스 스키마 정의 (`schema/schema-v1.sql`)
- Gradle 의존성 구성 완료

### 2.2 Persistence 레이어 (완료)

**Mapper 인터페이스 7개** — Optional 적용 완료

| Mapper | 단건 조회 반환 타입 | 목록 조회 |
|---|---|---|
| `UserMapper` | `Optional<UserDTO>` | — |
| `SpaceMapper` | `Optional<SpaceDTO>` | `List<SpaceDTO>` |
| `ShelfMapper` | `Optional<ShelfDTO>` | `List<ShelfDTO>` |
| `BoxMapper` | `Optional<BoxDTO>` | `List<BoxDTO>` |
| `ItemMapper` | `Optional<ItemDTO>` | `List<ItemDTO>` |
| `StockMapper` | `Optional<StockDTO>` | `List<StockDTO>` |
| `StockTransactionMapper` | `Optional<StockTransactionDTO>` | `List<StockTransactionDTO>` |

**XML 매퍼 7개** — `src/main/resources/mapper/` 작성 완료

**UUID TypeHandler** — MyBatis 기본 미지원으로 직접 구현 및 등록

**테스트** — `@MybatisTest` 기반 34개 테스트 전체 통과

---

## 3. MVP 개발 로드맵

### Phase 1: 사용자 인증 (1주)

**목표:** 로그인/로그아웃만 구현. Spring Security 완전 연동은 MVP 이후로 보류.

#### 3.1.1 백엔드

- `UserService` — `insertUser`, `findByUsername` 활용한 회원가입·로그인 로직
- Spring Security 설정 (현재 주석 처리된 의존성 활성화)
  - `SecurityFilterChain` — 인증 필요 경로 설정 (`/spaces/**`, `/items/**` 등)
  - `UserDetailsService` — `UserMapper.findByUsername` 연동
  - 비밀번호 `BCryptPasswordEncoder` 적용
- `UserController` — `/login`, `/register`, `/logout` 처리

#### 3.1.2 화면

| 경로 | 템플릿 | 설명 |
|---|---|---|
| `/login` | `auth/login.html` | 로그인 폼 (Thymeleaf) |
| `/register` | `auth/register.html` | 회원가입 폼 |

**HTMX 미사용** — 인증 플로우는 전통적인 폼 submit + 리다이렉트

---

### Phase 2: 위치 체계 관리 (1.5주)

**목표:** Space → Shelf → Box 계층 구조를 한 화면에서 관리.

#### 3.2.1 백엔드

- `SpaceService`, `ShelfService`, `BoxService`
- `SpaceController`, `ShelfController`, `BoxController`
- 요청 흐름: `HTTP → Controller → Service → Mapper → DB`

**엔드포인트 (Thymeleaf 뷰 반환 기준)**

| 메소드 | 경로 | 동작 |
|---|---|---|
| GET | `/spaces` | 공간 목록 페이지 |
| POST | `/spaces` | 공간 등록 → 목록으로 리다이렉트 |
| POST | `/spaces/{id}/delete` | 공간 삭제 |
| GET | `/spaces/{id}/shelves` | 해당 공간의 선반 목록 |
| POST | `/spaces/{id}/shelves` | 선반 등록 |
| GET | `/shelves/{id}/boxes` | 해당 선반의 박스 목록 |
| POST | `/shelves/{id}/boxes` | 박스 등록 |

**HTMX 적용 포인트**

```
[공간 목록 페이지]
- "공간 추가" 버튼 → 모달 열기 (hx-get="/spaces/form" hx-target="#modal")
- 모달 내 취소 버튼 → 모달 닫기 (hx-get="/spaces/empty" hx-target="#modal")
- 삭제 버튼 → 삭제 확인 후 해당 행 제거 (hx-delete, hx-target="closest tr")
```

#### 3.2.2 화면

| 템플릿 | 설명 |
|---|---|
| `spaces/list.html` | 공간 목록 + 선반 트리 |
| `spaces/form-fragment.html` | HTMX용 모달 폼 프래그먼트 |
| `shelves/form-fragment.html` | HTMX용 선반 추가 모달 |
| `boxes/form-fragment.html` | HTMX용 박스 추가 모달 |

---

### Phase 3: 품목 관리 (1주)

**목표:** 아이템 마스터 등록 및 목록 관리.

#### 3.3.1 백엔드

- `ItemService`, `ItemController`

**엔드포인트**

| 메소드 | 경로 | 동작 |
|---|---|---|
| GET | `/items` | 품목 목록 페이지 |
| POST | `/items` | 품목 등록 → 목록으로 리다이렉트 |
| GET | `/items/{id}` | 품목 상세 |
| POST | `/items/{id}` | 품목 수정 (이름, 설명) |
| POST | `/items/{id}/delete` | 품목 삭제 |

**HTMX 적용 포인트**

```
[품목 목록 페이지]
- "품목 추가" 버튼 → 모달 폼
- 인라인 수정: "수정" 클릭 → 해당 행이 편집 가능 입력 필드로 교체
  (hx-get="/items/{id}/edit-row" hx-target="closest tr")
- 수정 취소 → 원래 행으로 복원
  (hx-get="/items/{id}/view-row" hx-target="closest tr")
```

#### 3.3.2 화면

| 템플릿 | 설명 |
|---|---|
| `items/list.html` | 품목 목록 |
| `items/form-fragment.html` | HTMX용 등록 모달 |
| `items/edit-row-fragment.html` | HTMX용 인라인 수정 행 |
| `items/view-row-fragment.html` | HTMX용 수정 취소 후 복원 행 |

---

### Phase 4: 재고 관리 및 입출고 (1.5주)

**목표:** 핵심 비즈니스 로직. 재고 등록, 입고/출고 처리, 이력 조회.

#### 3.4.1 비즈니스 규칙

- `stocks.quantity`는 `CHECK (quantity >= 0)` 제약 존재 → 출고 시 재고 부족 검증 필수
- 재고 수량 변경은 반드시 `stock_transactions`에 이력 기록 후 `stocks.quantity` 업데이트 (원자적 처리)
- `box_id`가 있으면 `shelf_id`도 반드시 존재해야 함 (계층 무결성)

#### 3.4.2 백엔드

- `StockService`
  - `registerStock(StockDTO)` — 위치에 재고 등록
  - `processIn(Long stockId, int quantity, String memo)` — 입고: 트랜잭션 기록 + quantity 증가
  - `processOut(Long stockId, int quantity, String memo)` — 출고: 재고 검증 + 트랜잭션 기록 + quantity 감소
- `StockController`

**엔드포인트**

| 메소드 | 경로 | 동작 |
|---|---|---|
| GET | `/stocks` | 전체 재고 현황 목록 |
| POST | `/stocks` | 재고 등록 (위치 + 품목 지정) |
| POST | `/stocks/{id}/in` | 입고 처리 |
| POST | `/stocks/{id}/out` | 출고 처리 |
| GET | `/stocks/{id}/transactions` | 해당 재고의 입출고 이력 |

**HTMX 적용 포인트**

```
[재고 현황 페이지]
- 입고/출고 버튼 → 수량 입력 모달
  (hx-get="/stocks/{id}/in-form" hx-target="#modal")
- 모달 내 취소 → 모달 닫기
- 처리 완료 후 해당 행의 수량 셀만 갱신
  (hx-post, hx-target="closest tr" hx-swap="outerHTML")
```

#### 3.4.3 화면

| 템플릿 | 설명 |
|---|---|
| `stocks/list.html` | 재고 현황 목록 (품목명, 위치, 수량) |
| `stocks/in-form-fragment.html` | HTMX용 입고 처리 모달 |
| `stocks/out-form-fragment.html` | HTMX용 출고 처리 모달 |
| `stocks/transactions.html` | 이력 조회 페이지 |

---

### Phase 5: 공통 레이아웃 및 마무리 (0.5주)

- 공통 네비게이션 레이아웃 (`layout/base.html`, Thymeleaf Layout Dialect 또는 `th:replace`)
- 글로벌 모달 컨테이너 (`<div id="modal">`) — HTMX 응답 수신 대상
- 입력 유효성 검증 오류 메시지 표시 (Spring Validation + `BindingResult`)
- 전역 예외 처리 (`@ControllerAdvice`)

---

## 4. HTMX 공통 패턴 정의

MVP 전반에 걸쳐 일관되게 사용할 HTMX 패턴을 아래와 같이 정의한다.

### 4.1 모달 열기/닫기

```html
<!-- 트리거 버튼 -->
<button hx-get="/spaces/form"
        hx-target="#modal"
        hx-swap="innerHTML">
  공간 추가
</button>

<!-- 모달 컨테이너 (layout에 1개만 존재) -->
<div id="modal"></div>

<!-- 서버가 반환하는 모달 프래그먼트 -->
<dialog open th:fragment="modal-form">
  <form hx-post="/spaces" hx-target="#space-list" hx-swap="outerHTML">
    ...
    <button type="button"
            hx-get="/empty"
            hx-target="#modal"
            hx-swap="innerHTML">취소</button>
  </form>
</dialog>
```

### 4.2 인라인 수정 (행 교체)

```html
<!-- 뷰 행 -->
<tr th:fragment="view-row" id="item-row-{id}">
  <td th:text="${item.name}"></td>
  <td>
    <button hx-get="/items/{id}/edit-row"
            hx-target="closest tr"
            hx-swap="outerHTML">수정</button>
  </td>
</tr>

<!-- 편집 행 (서버 반환) -->
<tr th:fragment="edit-row">
  <td><input type="text" name="name" th:value="${item.name}"/></td>
  <td>
    <button hx-post="/items/{id}"
            hx-target="closest tr"
            hx-swap="outerHTML">저장</button>
    <button hx-get="/items/{id}/view-row"
            hx-target="closest tr"
            hx-swap="outerHTML">취소</button>
  </td>
</tr>
```

---

## 5. 요청 처리 흐름

```
[브라우저]
  │
  ├─ 일반 페이지 이동 (링크/폼 submit)
  │     └─ GET/POST → Controller → Service → Mapper → DB
  │                                                    │
  │                 Thymeleaf 전체 페이지 렌더링 ←────┘
  │
  └─ HTMX 인터랙션 (모달/인라인)
        └─ hx-get/hx-post → Controller → Service → Mapper → DB
                                                              │
                    Thymeleaf 프래그먼트만 렌더링 ←──────────┘
                    (#modal 또는 특정 row에 swap)
```

---

## 6. 디렉토리 구조 (목표)

```
src/main/
├── java/com/seu/seustock/
│   ├── configuration/
│   │   └── UUIDTypeHandler.java        ← 완료
│   ├── controller/
│   │   ├── IndexController.java        ← 존재
│   │   ├── UserController.java
│   │   ├── SpaceController.java
│   │   ├── ShelfController.java
│   │   ├── BoxController.java
│   │   ├── ItemController.java
│   │   └── StockController.java
│   ├── mapper/                         ← 완료
│   ├── model/
│   │   ├── dto/                        ← 완료
│   │   └── form/                       ← 입력 폼 객체 (Bean Validation 적용)
│   └── service/
│       ├── UserService.java
│       ├── SpaceService.java
│       ├── ShelfService.java
│       ├── BoxService.java
│       ├── ItemService.java
│       └── StockService.java           ← 재고 + 트랜잭션 통합
└── resources/
    ├── mapper/                         ← 완료
    └── templates/
        ├── layout/
        │   └── base.html               ← 공통 레이아웃
        ├── auth/
        │   ├── login.html
        │   └── register.html
        ├── spaces/
        ├── shelves/
        ├── boxes/
        ├── items/
        └── stocks/
```

---

## 7. MVP 범위 외 항목 (이후 단계)

아래 항목은 MVP 완성 후 다음 버전에서 구현한다.

| 항목 | 사유 |
|---|---|
| 그룹(Group) 기반 다중 사용자 협업 | 스키마 주석 처리됨, 요구사항 미확정 |
| 바코드/SKU 관리 | 스키마 주석 처리됨 |
| 대시보드 차트/시각화 | 핵심 기능 완성 후 추가 |
| 검색·필터링 고도화 | MVP는 단순 목록 조회로 충분 |
| 모바일 반응형 최적화 | 기본 레이아웃 이후 개선 |

---

## 8. 예상 소요 기간

| 단계 | 내용 | 기간 |
|---|---|---|
| ~~사전 작업~~ | ~~인프라, 스키마, Mapper, 테스트~~ | **완료** |
| Phase 1 | 사용자 인증 | 1주 |
| Phase 2 | 위치 체계 관리 (Space/Shelf/Box) | 1.5주 |
| Phase 3 | 품목 관리 (Item) | 1주 |
| Phase 4 | 재고 관리 + 입출고 처리 | 1.5주 |
| Phase 5 | 공통 레이아웃 및 마무리 | 0.5주 |
| **합계** | | **5.5주** |

---

## 9. 결론

v1 보고서의 REST API 방식을 **Thymeleaf SSR + HTMX 선택적 적용**으로 전환한다. 이 방식은:

- 서버 렌더링 중심으로 **Spring MVC의 기존 패턴을 그대로 활용**
- 별도 프론트엔드 빌드 파이프라인 불필요 → **개발 복잡도 감소**
- HTMX는 모달·인라인 수정·삭제 확인 등 **UX가 명확히 개선되는 지점에만 한정 적용**
- Persistence 레이어(Mapper + 테스트)가 이미 완성되어 **Service → Controller → View 순으로 빠르게 진행 가능**
