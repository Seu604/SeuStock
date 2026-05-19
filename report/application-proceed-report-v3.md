# 물품관리 애플리케이션 개발 진행 보고서 (v3)

**작성일:** 2026-05-18  
**이전 버전:** application-proceed-report-v2.md  
**변경 배경:** Phase 2 (공간 상세 — 선반/박스/재고 2패널 레이아웃) 구현 완료 후 현황 업데이트

---

## 1. 프로젝트 개요

- **프로젝트 명**: SeuStock (물품 및 재고 관리 시스템)
- **주요 목적**: 공간(Space) > 선반(Shelf) > 박스(Box) 계층 위치에 품목(Item) 재고(Stock)를 등록·관리하고 입출고 이력(Stock Transaction)을 추적하는 시스템
- **목표 범위**: MVP

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

### 1.2 렌더링 전략

Thymeleaf SSR 기반 전체 페이지 렌더링 + HTMX 선택적 부분 인터랙션 (v2에서 확정, 유지).

| HTMX 적용 대상 | HTMX 미적용 |
|---|---|
| 모달 열기/닫기 | 페이지 간 네비게이션 |
| 인라인 행 수정/취소 | 목록 페이지 최초 진입 |
| 삭제 확인 후 행 제거 | 로그인/회원가입 |
| 재고 패널 위치별 교체 | — |
| 입출고 처리 후 행 수량 갱신 | — |

---

## 2. 현재 개발 완료 현황

### 2.1 인프라 및 사전 작업 (완료)

- Docker Compose 기반 PostgreSQL (포트 5433), `bootRun` 시 자동 기동
- 데이터베이스 스키마 (`schema/schema-v1.sql`) — **ON DELETE CASCADE 추가 완료**
  - `shelves → spaces`, `boxes → shelves`, `stocks → shelves/boxes`, `stock_transactions → stocks`
- 3개 스키마 파일 동기화: `schema/schema-v1.sql`, `docker/postgres/init/init.sql`, `src/test/resources/schema-test.sql`

### 2.2 Persistence 레이어 (완료)

**Mapper 인터페이스 7개** — 모든 단건 조회는 `Optional<T>` 반환

| Mapper | 주요 메서드 |
|---|---|
| `UserMapper` | `findByUsername`, `insertUser` |
| `SpaceMapper` | `findById`, `findByExternalId`, `findAllByUserId`, `insert`, `update`, `delete` |
| `ShelfMapper` | `findById`, `findByExternalId`, `findAllBySpaceId`, `insert`, `delete` |
| `BoxMapper` | `findById`, `findByExternalId`, `findAllByShelfId`, `insert`, `delete` |
| `ItemMapper` | `findById`, `findByExternalId`, `findAllByUserId`, `insert` |
| `StockMapper` | `findByExternalId`, `findByBoxId`, `findByShelfIdDirectOnly`, `findBySpaceIdDirectOnly`, `findPanelByBoxId`, `findPanelByShelfDirectOnly`, `findPanelBySpaceDirectOnly`, `insert`, `updateQuantity`, `delete` |
| `StockTransactionMapper` | `insert` |

**XML 매퍼 7개** (`src/main/resources/mapper/`) — `StockMapper.xml`에 `StockPanelResultMap` 및 JOIN 쿼리 포함

**UUID TypeHandler** — `configuration/UUIDTypeHandler.java` 등록 완료

**테스트** — `@MybatisTest` 기반 전체 통과 (`BUILD SUCCESSFUL`)

### 2.3 Session 기반 인증 (완료)

Spring Security 대신 세션 직접 관리 방식으로 구현.

- `UserService` — BCrypt 비밀번호 암호화, 회원가입/로그인 로직
- `UserController` — `GET/POST /login`, `GET/POST /register`, `GET /logout`
- `LoginCheckInterceptor` — 세션 미존재 시 `/login` 리다이렉트
- `WebMvcConfig` — `/spaces/**`, `/stocks/**`, `/shelves/**`, `/boxes/**` 경로에 인터셉터 적용
- 템플릿: `auth/login.html`, `auth/register.html` (Bean Validation 에러 표시 포함)
- 공통 레이아웃: `fragments/layout.html` (nav 포함)

### 2.4 공간(Space) 관리 (완료)

- `SpaceService`, `SpaceController`
- `GET /spaces` — 공간 목록 (SSR)
- `POST /spaces` — 공간 등록 (Bean Validation, 에러 시 인라인 표시)
- `GET /spaces/{id}/edit` → `PUT /spaces/{id}` — HTMX 인라인 행 수정
- `GET /spaces/{id}/cancel` — 인라인 수정 취소 (원래 행 복원)
- `DELETE /spaces/{id}` — 행 삭제 (`@ResponseBody ""`)
- `GET /spaces/{id}` — **공간 상세 페이지** (Phase 2에서 추가)
- 템플릿: `spaces/list.html`, `spaces/fragments/row.html` (view/edit 프래그먼트, 공간명에 상세 링크 추가)

### 2.5 공간 상세 — Shelf/Box/Stock 2패널 레이아웃 (Phase 2, 완료)

이번 세션의 핵심 구현 내용.

#### 2.5.1 구현 구조

```
[spaces/detail.html — GET /spaces/{id}]
┌────────────────────────┬────────────────────────────────┐
│ ul#shelf-list          │ div#stock-panel                │
│ (Shelf 아코디언 SSR)   │ (위치별 재고 패널 HTMX 교체)  │
└────────────────────────┴────────────────────────────────┘
<div id="modal"></div>    ← 전역 모달 컨테이너
```

#### 2.5.2 선반(Shelf) 관리

- `ShelfService` — `findAllBySpaceId`, `findByExternalId`, `create`, `delete`
  - 소유권 검증: `space.userId == user.id` → `shelf.spaceId == space.id`
- `ShelfController`

| 메서드 | 경로 | 반환 |
|---|---|---|
| GET | `/spaces/{spaceId}/shelves/new` | `shelves/fragments/modal :: modal` |
| POST | `/spaces/{spaceId}/shelves` | `shelves/fragments/created :: created` (OOB 모달 닫기 포함) |
| DELETE | `/spaces/{spaceId}/shelves/{shelfId}` | `@ResponseBody ""` |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/boxes` | `shelves/fragments/box-list :: box-list` |

- 템플릿: `shelves/fragments/tree-item.html`, `shelves/fragments/created.html`, `shelves/fragments/box-list.html`, `shelves/fragments/modal.html`

#### 2.5.3 박스(Box) 관리

- `BoxService` — `findAllByShelfId`, `create`, `delete`
  - 소유권 검증: Space → Shelf 체인
- `BoxController`

| 메서드 | 경로 | 반환 |
|---|---|---|
| GET | `/spaces/{spaceId}/shelves/{shelfId}/boxes/new` | `boxes/fragments/modal :: modal` |
| POST | `/spaces/{spaceId}/shelves/{shelfId}/boxes` | `boxes/fragments/created :: created` (OOB 모달 닫기 포함) |
| DELETE | `/spaces/{spaceId}/shelves/{shelfId}/boxes/{boxId}` | `@ResponseBody ""` |

- 템플릿: `boxes/fragments/tree-item.html`, `boxes/fragments/created.html`, `boxes/fragments/modal.html`

#### 2.5.4 재고(Stock) 패널

- `StockService` — 패널 조회 3종, `create`, `delete`, `processIn`, `processOut`
  - `processIn`/`processOut`: `@Transactional`, 트랜잭션 행 INSERT → `stocks.quantity` UPDATE (CLAUDE.md 규칙)
  - `processOut` 재고 부족 검증: `if (newQty < 0) throw new IllegalArgumentException`
- `ItemService` — `findAllByUsername` (재고 추가 모달 드롭다운용)
- `StockController`

| 메서드 | 경로 | 반환 |
|---|---|---|
| GET | `/spaces/{spaceId}/stocks` | `stocks/fragments/panel :: stock-panel` |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/stocks` | `stocks/fragments/panel :: stock-panel` |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/boxes/{boxId}/stocks` | `stocks/fragments/panel :: stock-panel` |
| GET | `/stocks/new` | `stocks/fragments/modal :: modal` |
| POST | `/stocks` | `stocks/fragments/panel :: stock-panel-response` (OOB 모달 닫기 포함) |
| DELETE | `/stocks/{id}` | `@ResponseBody ""` |
| GET | `/stocks/{id}/in-form` | `stocks/fragments/in-modal :: modal` |
| POST | `/stocks/{id}/in` | `stocks/fragments/row :: view-response` (OOB 모달 닫기 포함) |
| GET | `/stocks/{id}/out-form` | `stocks/fragments/out-modal :: modal` |
| POST | `/stocks/{id}/out` | `stocks/fragments/row :: view-response` (OOB 모달 닫기 포함) |

- 템플릿: `stocks/fragments/panel.html` (3개 fragment: `stock-panel-response`, `stock-panel`, `panel-content`), `stocks/fragments/row.html` (`view-response`, `view`), `stocks/fragments/modal.html`, `stocks/fragments/in-modal.html`, `stocks/fragments/out-modal.html`

#### 2.5.5 HTMX 핵심 swap 패턴

| 트리거 | hx-target | hx-swap |
|---|---|---|
| Shelf Box 목록 첫 로드 | `#box-list-{shelfId}` | `innerHTML` |
| Space/Shelf/Box 클릭 → 재고 패널 | `#stock-panel` | `outerHTML` |
| 선반/박스 추가 submit | `#shelf-list` / `#box-list-{id}` | `beforeend` |
| 재고 추가 submit | `#stock-panel` | `outerHTML` |
| 입출고 submit | `#stock-{externalId}` | `outerHTML` |
| 삭제 (Shelf/Box/Stock) | `closest li` / `closest tr` | `outerHTML` |
| OOB 모달 닫기 | `#modal` (hx-swap-oob) | 빈 div |

#### 2.5.6 아코디언 로딩 전략

첫 클릭 시 HTMX `hx-trigger="click once"`로 박스 목록 지연 로드 → 이후 클릭은 JS `toggleBoxList(shelfId, el)`로 `hidden` 클래스 토글 (서버 재요청 없음).

---

## 3. 현재 파일 구조

```
src/main/java/com/seu/seustock/
├── configuration/
│   ├── UUIDTypeHandler.java          ✅
│   ├── LoginCheckInterceptor.java    ✅
│   └── WebMvcConfig.java             ✅
├── controller/
│   ├── IndexController.java          ✅ (/empty 엔드포인트 포함)
│   ├── UserController.java           ✅
│   ├── SpaceController.java          ✅
│   ├── ShelfController.java          ✅
│   ├── BoxController.java            ✅
│   └── StockController.java          ✅
├── mapper/
│   ├── UserMapper.java               ✅
│   ├── SpaceMapper.java              ✅
│   ├── ShelfMapper.java              ✅
│   ├── BoxMapper.java                ✅
│   ├── ItemMapper.java               ✅
│   ├── StockMapper.java              ✅
│   └── StockTransactionMapper.java   ✅
├── model/
│   ├── dto/
│   │   ├── UserDTO.java              ✅
│   │   ├── SpaceDTO.java             ✅
│   │   ├── ShelfDTO.java             ✅
│   │   ├── BoxDTO.java               ✅
│   │   ├── ItemDTO.java              ✅
│   │   ├── StockDTO.java             ✅
│   │   ├── StockPanelDTO.java        ✅ (items JOIN 결과)
│   │   └── StockTransactionDTO.java  ✅
│   └── form/
│       ├── SpaceForm.java            ✅
│       ├── ShelfForm.java            ✅
│       ├── BoxForm.java              ✅
│       ├── StockForm.java            ✅
│       └── StockInOutForm.java       ✅
└── service/
    ├── UserService.java              ✅
    ├── SpaceService.java             ✅
    ├── ShelfService.java             ✅
    ├── BoxService.java               ✅
    ├── ItemService.java              ✅ (findAllByUsername만 구현)
    └── StockService.java             ✅

src/main/resources/
├── mapper/
│   ├── UserMapper.xml                ✅
│   ├── SpaceMapper.xml               ✅
│   ├── ShelfMapper.xml               ✅
│   ├── BoxMapper.xml                 ✅
│   ├── ItemMapper.xml                ✅
│   ├── StockMapper.xml               ✅ (StockPanelResultMap, JOIN 쿼리 포함)
│   └── StockTransactionMapper.xml    ✅
└── templates/
    ├── fragments/
    │   └── layout.html               ✅
    ├── auth/
    │   ├── login.html                ✅
    │   └── register.html             ✅
    ├── spaces/
    │   ├── list.html                 ✅
    │   ├── detail.html               ✅
    │   └── fragments/
    │       └── row.html              ✅ (공간명 링크 포함)
    ├── shelves/
    │   └── fragments/
    │       ├── tree-item.html        ✅
    │       ├── box-list.html         ✅
    │       ├── created.html          ✅
    │       └── modal.html            ✅
    ├── boxes/
    │   └── fragments/
    │       ├── tree-item.html        ✅
    │       ├── created.html          ✅
    │       └── modal.html            ✅
    └── stocks/
        └── fragments/
            ├── panel.html            ✅
            ├── row.html              ✅
            ├── modal.html            ✅
            ├── in-modal.html         ✅
            └── out-modal.html        ✅
```

---

## 4. 잔여 작업 (Phase 3)

### 4.1 품목(Item) 관리

현재 `ItemService.findAllByUsername`은 구현되어 있으나 ItemController와 화면이 없다.

#### 4.1.1 백엔드

- `ItemController` — `SpaceController` 패턴 그대로 적용
  - `GET /items` — 품목 목록 (SSR)
  - `POST /items` — 품목 등록
  - `GET /items/{id}/edit` → `PUT /items/{id}` — HTMX 인라인 행 수정
  - `GET /items/{id}/cancel` — 수정 취소
  - `DELETE /items/{id}` — 품목 삭제
- `ItemService` — `create`, `update`, `delete`, `findByExternalId` 추가
- `ItemMapper` + `ItemMapper.xml` — `insert`, `update`, `delete` 추가

#### 4.1.2 화면

| 템플릿 | 설명 |
|---|---|
| `items/list.html` | 품목 목록 + 인라인 추가/수정 |
| `items/fragments/row.html` | view/edit 프래그먼트 (SpaceController row 패턴) |
| `items/fragments/modal.html` | 품목 추가 모달 (필요 시) |

#### 4.1.3 내비게이션 링크

`fragments/layout.html` nav에 `/items` 링크 추가 필요.

### 4.2 글로벌 예외 처리

- `@ControllerAdvice` 미구현
- 현재 미등록 UUID 경로 접근, 재고 부족 등에서 500 에러 발생 가능
- 최소 구현: 404/403/500 에러 페이지 + `IllegalArgumentException` → 400 처리

### 4.3 입출고 이력 조회

- `stock_transactions` 테이블에 이력이 쌓이나 조회 화면 미구현
- `StockTransactionMapper.findByStockId` 추가 후 `GET /stocks/{id}/transactions` 구현

---

## 5. MVP 로드맵 현황

| 단계 | 내용 | 상태 |
|---|---|---|
| 사전 작업 | 인프라, 스키마, Mapper, 테스트 | **완료** |
| Phase 1 | 사용자 인증 (세션 기반) | **완료** |
| Phase 1.5 | Space CRUD + HTMX 인라인 수정 | **완료** |
| Phase 2 | 공간 상세: Shelf/Box 트리 + Stock 패널 | **완료** |
| **Phase 3** | **품목(Item) CRUD** | **진행 예정** |
| Phase 4 | 글로벌 예외 처리, 입출고 이력 조회 | 잔여 |

---

## 6. 구현 중 발견된 주요 기술 결정 사항

### 6.1 OOB(Out-of-Band) 모달 닫기 패턴

HTMX `hx-swap="beforeend"`로 새 행을 추가하면서 동시에 모달을 닫아야 하는 경우, `:: fragment` 선택으로는 fragment 외부의 OOB div가 렌더링되지 않는다. 해결책: `created.html` 별도 파일에 `<li>` + `<div id="modal" hx-swap-oob="true"></div>`를 형제 요소로 배치하여 HTMX가 두 요소를 각각 처리하도록 구성.

### 6.2 재고 패널 복합 Fragment

POST `/stocks` 응답에서 모달 닫기(OOB) + 패널 전체 교체를 동시에 처리하기 위해 `<th:block th:fragment="stock-panel-response">`로 두 요소를 하나의 fragment로 묶음. `stock-panel`과 내용을 공유하기 위해 `panel-content` fragment를 별도 분리하고 `th:replace="~{:: panel-content}"`로 자기 참조.

### 6.3 아코디언 + HTMX 첫 로드 조합

박스 목록을 첫 클릭 시에만 서버에서 가져오고(`hx-trigger="click once"`), 이후 토글은 JS로 처리(`toggleBoxList`). 단순 `hidden` 클래스 토글로 불필요한 서버 재요청을 차단.

### 6.4 StockPanelDTO N+1 방지

재고 패널 표시 시 품목명(`item_name`)을 별도 조회하지 않고 `stocks JOIN items` 쿼리로 한 번에 가져옴. `StockPanelResultMap`에 `item_name` 칼럼 매핑.

### 6.5 DB 레벨 CASCADE 삭제

Service 코드에서 하위 엔티티를 순서대로 삭제하는 대신 스키마에 `ON DELETE CASCADE`를 추가. Shelf 삭제 한 번으로 하위 Box, Stock, StockTransaction이 모두 정리됨.

---

## 7. 결론

Phase 2 완료로 SeuStock의 핵심 인터랙션(공간 → 선반 아코디언 → 박스 → 재고 패널 → 입출고)이 모두 동작하는 상태다. 잔여 작업은 품목 관리(Item CRUD)와 예외 처리에 집중되어 있으며, 이를 완료하면 MVP 기능이 충족된다.
