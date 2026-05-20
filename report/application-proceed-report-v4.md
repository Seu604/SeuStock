# 물품관리 애플리케이션 개발 진행 보고서 (v4)

**작성일:** 2026-05-19  
**이전 버전:** application-proceed-report-v3.md  
**변경 배경:** 품목(Item) 관리 구현 진행 현황 반영 및 수량 기반/개별 추적 기반 재고 관리 설계 방향 정리

---

## 1. 프로젝트 개요

- **프로젝트 명**: SeuStock (물품 및 재고 관리 시스템)
- **주요 목적**: 공간(Space) > 선반(Shelf) > 박스(Box) 계층 위치에 품목(Item) 재고(Stock)를 등록·관리하고 입출고 이력(Stock Transaction)을 추적하는 시스템
- **목표 범위**: MVP + 품목별 재고 추적 정책 확장 기반

### 1.1 현재 핵심 모델

| 모델 | 역할 |
|---|---|
| `items` | 사용자가 등록하는 품목 마스터 정보 |
| `stocks` | 품목 + 위치별 재고 묶음 및 수량 |
| `stock_transactions` | 입고/출고 등 수량 변화 이력 |
| `item_specs` | 제품 상세 정보 저장 예정 |
| `stock_units` | 개별 추적 대상 실물 단위 저장 예정 |

현재 구현은 `items`, `stocks`, `stock_transactions`를 중심으로 수량 기반 재고 관리를 수행한다. v4 설계에서는 기존 수량 기반 흐름을 유지하면서, 품목별로 개별 제품 추적을 선택할 수 있는 구조를 추가한다.

---

## 2. 현재 git 변경사항 요약

v3에서 잔여 작업으로 남아 있던 품목(Item) 관리 기능이 현재 워킹트리에서 구현 진행 상태로 전환되었다.

### 2.1 품목 관리 백엔드

- `ItemController` 추가
  - `GET /items` — 품목 목록 페이지
  - `POST /items` — 품목 등록
  - `GET /items/{externalId}/edit` — HTMX 인라인 수정 행
  - `PUT /items/{externalId}` — 품목 수정
  - `GET /items/{externalId}/cancel` — 수정 취소
  - `DELETE /items/{externalId}` — 품목 삭제
- `ItemForm` 추가
  - 품목명 필수
  - 품목명 최대 100자
  - 설명 최대 500자
- `ItemService` 확장
  - `findByExternalId`, `create`, `update`, `delete` 추가
  - 세션 username 기준 사용자 조회 및 소유권 검증 추가
- `ItemMapper` / `ItemMapper.xml`
  - 기존 품목 조회 외에 insert/update/delete 흐름 사용

### 2.2 품목 관리 화면

- `templates/items/list.html` 추가
  - 품목 등록 폼
  - 품목 목록 테이블
  - 빈 목록 안내 문구
- `templates/items/fragments/row.html` 추가
  - 읽기 모드 행
  - 수정 모드 행
  - HTMX 기반 저장/취소/삭제

### 2.3 인증 및 내비게이션

- `WebMvcConfig`
  - 인증 인터셉터 대상에 `/items/**` 추가
- `fragments/layout.html`
  - 상단 내비게이션에 `내 품목` 링크 추가
- `CLAUDE.md`
  - UI 문구 한국어 사용 명시
  - Thymeleaf fragment 반환 규칙 정리
  - 세션 기반 인증 및 서비스 소유권 검증 패턴 보강

---

## 3. 수량 기반/개별 추적 기반 재고 관리 설계

### 3.1 설계 방향

품목마다 재고 관리 방식을 선택할 수 있도록 `items`에 추적 정책을 둔다.

```sql
tracking_type VARCHAR(30) NOT NULL DEFAULT 'QUANTITY'
```

권장 값은 다음 두 가지다.

| 값 | 의미 |
|---|---|
| `QUANTITY` | 개별 제품 추적 없이 `stocks.quantity`로 수량만 관리 |
| `UNIT` | 개별 제품 단위를 `stock_units`에 저장하고 추적 |

숫자 `0/1`보다 `QUANTITY/UNIT` 문자열이 DB와 코드에서 의미가 명확하다.

### 3.2 `item_specs` 역할

`item_specs`는 제품 상세 정보를 담당한다. 추적 여부 플래그는 스펙이 아니라 재고 관리 정책이므로 `item_specs`에 두지 않는다.

예상 필드:

- 제조사
- 모델명
- SKU
- 바코드
- 규격
- 무게
- 색상
- 재질
- 비고

`item_specs.item_id`는 `items.id`를 FK로 참조하며, MVP에서는 품목 1개당 상세 정보 1개를 갖는 1:1 구조가 적합하다.

### 3.3 `stocks` 역할

`stocks`는 계속 품목 + 위치별 재고 묶음을 담당한다.

- `item_id`
- `space_id`
- `shelf_id`
- `box_id`
- `quantity`

`QUANTITY` 방식에서는 `quantity`가 실제 관리 수량이다. `UNIT` 방식에서도 초기 구현에서는 조회 성능과 기존 화면 호환성을 위해 `quantity`를 유지하되, `stock_units` 개수와 같은 트랜잭션에서 동기화한다.

### 3.4 `stock_units` 역할

`stock_units`는 `tracking_type = 'UNIT'` 품목의 개별 실물 단위를 저장한다.

예상 필드:

- `stock_id`
- `serial_number`
- `lot_number`
- `expiration_date`
- `status`
- `created_at`

개별 제품마다 시리얼 번호, 로트 번호, 유통기한, 상태를 관리해야 할 때 사용한다.

---

## 4. 동작 정책 요약

### 4.1 수량 기반 품목 (`QUANTITY`)

- 재고 등록 시 같은 품목 + 같은 위치의 `stocks` row가 있으면 `quantity`를 증가시킨다.
- 기존 row가 없으면 `stocks` row를 1개 생성한다.
- 입고/출고는 `stock_transactions`에 이력을 남긴 뒤 `stocks.quantity`를 갱신한다.
- 출고 시 `stocks.quantity`보다 큰 수량은 거부한다.

### 4.2 개별 추적 품목 (`UNIT`)

- 재고 등록 또는 입고 시 입력 수량만큼 `stock_units` row를 생성한다.
- `stocks.quantity`는 생성된 `stock_units` 수량만큼 같은 트랜잭션에서 증가시킨다.
- 출고 시에는 대상 unit의 `status`를 변경하거나 출고 이력과 연결하는 방식이 필요하다.
- 개별 unit 선택 UI는 후속 구현에서 별도 화면 또는 모달로 추가한다.

### 4.3 공통 규칙

- URL과 화면에는 내부 `id`를 노출하지 않고 `external_id`를 사용한다.
- 테이블 간 FK는 내부 `id`를 사용한다.
- 입고/출고 처리는 반드시 `@Transactional`로 묶는다.
- `stock_transactions`는 두 방식 모두에서 수량 변화의 감사 로그 역할을 유지한다.

---

## 5. 다음 작업

| 단계 | 내용 | 상태 |
|---|---|---|
| Phase 3 | 품목(Item) CRUD 구현 반영 및 테스트 | 진행 중 |
| Phase 3.5 | `item_specs` 추가 및 품목 상세 정보 관리 | 예정 |
| Phase 4 | `items.tracking_type` 추가 및 재고 등록 분기 | 예정 |
| Phase 4.5 | `stock_units` 추가 및 개별 제품 추적 | 예정 |
| Phase 5 | 입출고 이력 조회, 글로벌 예외 처리 | 예정 |

### 5.1 스키마 작업

- `schema/schema-v1.sql`
- `docker/postgres/init/init.sql`
- `src/test/resources/schema-test.sql`

위 3개 스키마 파일을 동기화해야 한다.

추가 예정:

- `items.tracking_type`
- `item_specs`
- `stock_units`
- 필요한 `CHECK` 제약 및 FK

### 5.2 애플리케이션 작업

- `ItemSpecMapper`, `ItemSpecDTO`, `ItemSpecForm`, `ItemSpecService` 추가
- `StockUnitMapper`, `StockUnitDTO`, `StockUnitService` 추가
- 품목 등록/수정 화면에 추적 방식 선택 UI 추가
- 재고 등록/입고/출고 서비스에서 `tracking_type` 기준 분기
- `StockPanelDTO` 또는 화면 표시값에 추적 방식 노출 여부 검토

### 5.3 테스트 작업

- `ItemMapperTest` 또는 품목 CRUD 관련 테스트 추가
- `ItemSpecMapperTest` 추가
- `StockUnitMapperTest` 추가
- `QUANTITY` 품목 입고/출고 수량 갱신 검증
- `UNIT` 품목 입고 시 `stock_units` 생성 및 `stocks.quantity` 동기화 검증

---

## 6. 결론

v4 기준으로 SeuStock은 v3에서 예정했던 품목 관리 기능이 구현 진행 상태에 들어갔다. 다음 설계의 핵심은 제품 상세 정보와 재고 추적 정책을 분리하는 것이다.

- 제품 상세 정보는 `item_specs`
- 재고 관리 방식은 `items.tracking_type`
- 위치별 재고 묶음은 `stocks`
- 개별 실물 추적은 `stock_units`
- 입출고 감사 로그는 `stock_transactions`

이 구조를 따르면 기존 수량 기반 재고 관리를 유지하면서도, 시리얼 번호나 로트 번호처럼 개별 제품 추적이 필요한 품목만 선택적으로 확장할 수 있다.
