# Space 상세 화면 UX 구조 설계 리포트 (v1)

**작성일:** 2026-05-18  
**대상 범위:** `/spaces/{id}` 진입 이후 Shelf → Box → Stock 계층 관리 화면  
**기술 전제:** Thymeleaf SSR + HTMX 선택적 적용 (application-proceed-report-v2 기준)

---

## 1. 설계 목표

Space 목록(`/spaces`)에서 특정 공간을 선택한 후 진입하는 상세 화면에서 아래 세 가지를 동시에 달성한다.

1. **계층 가시성** — Shelf → Box 구조를 한 화면에서 파악 가능
2. **재고 맥락** — 선택한 위치에 어떤 품목이 얼마나 있는지 즉시 확인 가능
3. **조작 편의성** — Shelf/Box 추가, 재고 입출고를 최소한의 화면 전환으로 처리

---

## 2. 화면 흐름 (페이지 네비게이션)

```
/spaces                    공간 목록
    │
    └─ (공간명 클릭) ──────▶  /spaces/{spaceId}         Space 상세
                                    │
                                    ├─ (선반 클릭, HTMX) ──▶  오른쪽 패널 갱신 (박스 목록)
                                    │
                                    └─ (박스 클릭, HTMX) ──▶  오른쪽 패널 갱신 (재고 목록)
```

**전체 페이지 이동은 Space 목록 → Space 상세 한 번뿐이다.**  
이후 Shelf 선택, Box 선택, 재고 조회는 모두 HTMX 부분 업데이트로 처리한다.

---

## 3. Space 상세 페이지 레이아웃

```
┌─────────────────────────────────────────────────────────────────┐
│  nav: SeuStock           내 공간 | 사용자명 | 로그아웃           │
├─────────────────────────────────────────────────────────────────┤
│  내 공간 > 창고 A                                [+ 선반 추가]   │
├────────────────────────┬────────────────────────────────────────┤
│  [왼쪽] 위치 트리       │  [오른쪽] 재고 패널                    │
│                        │                                        │
│  ▼ 1번 선반    [+][x]  │  위치: 창고 A > 1번 선반 > A 박스      │
│    ▶ A 박스    [+][x]  │                                        │
│    ▶ B 박스    [+][x]  │  ┌─────────────────────────────────┐  │
│    📦 선반 직접 재고    │  │ 품목명    수량  입고  출고  삭제 │  │
│                        │  ├─────────────────────────────────┤  │
│  ▶ 2번 선반    [+][x]  │  │ 테이프    12    [+]  [-]  [x]   │  │
│                        │  │ 마커펜    5     [+]  [-]  [x]   │  │
│  ▶ 3번 선반    [+][x]  │  └─────────────────────────────────┘  │
│                        │                                        │
│  📦 공간 직접 재고      │  [+ 재고 추가]                         │
│                        │                                        │
└────────────────────────┴────────────────────────────────────────┘
```

### 3.1 왼쪽: 위치 트리 패널

- **Shelf 목록**이 최초 렌더링 시 서버에서 SSR로 전달된다.
- **Shelf 행 클릭** → 해당 Shelf의 Box 목록을 토글(accordion)로 펼침/접힘.  
  Box 목록이 아직 로드되지 않았으면 HTMX로 첫 번째 클릭 시 서버에서 받아온다.
- **Box 행 클릭** → 오른쪽 재고 패널을 해당 Box의 재고 목록으로 교체(HTMX).
- **"📦 선반 직접 재고"** — box_id 없이 shelf_id만 가진 재고를 보는 진입점.
- **"📦 공간 직접 재고"** — space_id만 가진 재고 (shelf_id, box_id 없음)를 보는 진입점.
- `[+]` 버튼 — Shelf 옆: Box 추가 모달. Box 옆: 재고 추가 모달.
- `[x]` 버튼 — 삭제 확인 후 해당 행 제거.

### 3.2 오른쪽: 재고 패널

- 기본 상태(초기 진입) — 공간 전체의 직접 재고(shelf_id = null) 또는 안내 문구 표시.
- 위치 트리에서 Box 또는 Shelf 클릭 시 해당 위치의 재고 목록으로 교체된다.
- 패널 상단 **브레드크럼** — "창고 A > 1번 선반 > A 박스" 형식으로 현재 위치 명시.
- **"+ 재고 추가"** 버튼 — 현재 선택된 위치에 재고를 등록하는 모달.

---

## 4. 주요 HTMX 인터랙션 정의

| 트리거 | HTMX 요청 | 서버 응답 | swap 대상 |
|---|---|---|---|
| Shelf 행 클릭 (첫 번째) | `hx-get="/spaces/{id}/shelves/{shelfId}/boxes"` | `shelves/fragments/box-list :: box-list` | `#box-list-{shelfId}` |
| Shelf 행 클릭 (이후) | JS로 토글 (서버 재요청 없음) | — | — |
| Box 행 클릭 | `hx-get="/spaces/{id}/shelves/{shelfId}/boxes/{boxId}/stocks"` | `stocks/fragments/panel :: stock-panel` | `#stock-panel` |
| "선반 직접 재고" 클릭 | `hx-get="/spaces/{id}/shelves/{shelfId}/stocks"` | `stocks/fragments/panel :: stock-panel` | `#stock-panel` |
| "공간 직접 재고" 클릭 | `hx-get="/spaces/{id}/stocks"` | `stocks/fragments/panel :: stock-panel` | `#stock-panel` |
| `[+ 선반 추가]` 클릭 | `hx-get="/spaces/{id}/shelves/new"` | `shelves/fragments/modal :: modal` | `#modal` |
| `[+ Box 추가]` 클릭 | `hx-get="/spaces/{id}/shelves/{shelfId}/boxes/new"` | `boxes/fragments/modal :: modal` | `#modal` |
| `[+ 재고 추가]` 클릭 | `hx-get="/stocks/new?spaceId=&shelfId=&boxId="` | `stocks/fragments/modal :: modal` | `#modal` |
| 재고 `[+]` 입고 클릭 | `hx-get="/stocks/{stockId}/in-form"` | `stocks/fragments/in-modal :: modal` | `#modal` |
| 재고 `[-]` 출고 클릭 | `hx-get="/stocks/{stockId}/out-form"` | `stocks/fragments/out-modal :: modal` | `#modal` |
| Shelf `[x]` 삭제 | `hx-delete="/spaces/{id}/shelves/{shelfId}"` | 빈 문자열 | `closest li` (outerHTML 제거) |
| Box `[x]` 삭제 | `hx-delete="/shelves/{shelfId}/boxes/{boxId}"` | 빈 문자열 | `closest li` (outerHTML 제거) |
| 재고 `[x]` 삭제 | `hx-delete="/stocks/{stockId}"` | 빈 문자열 | `closest tr` (outerHTML 제거) |
| 모달 취소 버튼 | `hx-get="/empty"` | `""` | `#modal` |

> **원칙:** Box 목록은 Shelf 클릭 시 첫 로드 후 DOM에 남긴다. 토글(펼침/접힘)은 CSS `hidden` 클래스를 JS로 교체해 서버 재요청을 방지한다.

---

## 5. URL 라우팅 설계

| 메서드 | 경로 | 반환 | 설명 |
|---|---|---|---|
| GET | `/spaces/{spaceId}` | 전체 페이지 (`spaces/detail.html`) | Space 상세 최초 진입 |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/boxes` | 프래그먼트 | Box 목록 (트리 패널용) |
| GET | `/spaces/{spaceId}/stocks` | 프래그먼트 | 공간 직접 재고 패널 |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/stocks` | 프래그먼트 | 선반 직접 재고 패널 |
| GET | `/spaces/{spaceId}/shelves/{shelfId}/boxes/{boxId}/stocks` | 프래그먼트 | 박스 재고 패널 |
| POST | `/spaces/{spaceId}/shelves` | redirect or 프래그먼트 | 선반 등록 |
| DELETE | `/spaces/{spaceId}/shelves/{shelfId}` | `""` | 선반 삭제 |
| POST | `/shelves/{shelfId}/boxes` | redirect or 프래그먼트 | 박스 등록 |
| DELETE | `/shelves/{shelfId}/boxes/{boxId}` | `""` | 박스 삭제 |
| GET | `/stocks/new` | 프래그먼트 | 재고 등록 모달 (쿼리파라미터로 위치 전달) |
| POST | `/stocks` | 프래그먼트 | 재고 등록 → 패널 갱신 |
| DELETE | `/stocks/{stockId}` | `""` | 재고 삭제 |
| GET | `/stocks/{stockId}/in-form` | 프래그먼트 | 입고 모달 |
| POST | `/stocks/{stockId}/in` | 프래그먼트 (tr) | 입고 처리 → 해당 행 갱신 |
| GET | `/stocks/{stockId}/out-form` | 프래그먼트 | 출고 모달 |
| POST | `/stocks/{stockId}/out` | 프래그먼트 (tr) | 출고 처리 → 해당 행 갱신 |

---

## 6. 템플릿 파일 구조

```
src/main/resources/templates/
├── spaces/
│   ├── list.html                          ← 기존 (공간 목록)
│   ├── detail.html                        ← 신규 (Space 상세 — 2패널 레이아웃)
│   └── fragments/
│       └── row.html                       ← 기존
│
├── shelves/
│   └── fragments/
│       ├── tree-item.html                 ← Shelf 행 + 하위 Box 컨테이너
│       ├── box-list.html                  ← Box 목록 (HTMX 첫 로드용)
│       └── modal.html                     ← Shelf 추가 모달
│
├── boxes/
│   └── fragments/
│       ├── tree-item.html                 ← Box 행 (트리 패널용)
│       └── modal.html                     ← Box 추가 모달
│
└── stocks/
    └── fragments/
        ├── panel.html                     ← 재고 패널 전체 (브레드크럼 + 테이블)
        ├── row.html                       ← 재고 테이블 행 (view/edit 모드)
        ├── in-modal.html                  ← 입고 처리 모달
        ├── out-modal.html                 ← 출고 처리 모달
        └── modal.html                     ← 재고 등록 모달
```

---

## 7. 컨트롤러 구성

기존 `SpaceController` 패턴을 그대로 따른다.

```
SpaceController    — /spaces/**       (목록, 상세, 선반 이하 재고 패널)
ShelfController    — /spaces/{id}/shelves/**, /shelves/**
BoxController      — /shelves/{id}/boxes/**, /boxes/**
StockController    — /stocks/**
```

각 컨트롤러는 HTMX 요청(`HX-Request` 헤더)과 일반 요청을 구분하지 않아도 된다. 프래그먼트를 반환하는 엔드포인트는 **항상 프래그먼트만** 반환하는 전용 경로로 분리한다.

---

## 8. 재고 패널 상세 구조 (stocks/fragments/panel.html)

```
┌─────────────────────────────────────────────────────────┐
│  위치: 창고 A  >  1번 선반  >  A 박스                    │  ← 브레드크럼
├──────────┬──────┬──────┬──────┬──────────────────────────┤
│  품목명  │ 수량 │ 입고 │ 출고 │ 삭제                     │
├──────────┼──────┼──────┼──────┼──────────────────────────┤
│  테이프  │  12  │ [+]  │ [-]  │ [x]                      │
│  마커펜  │   5  │ [+]  │ [-]  │ [x]                      │
├──────────┴──────┴──────┴──────┴──────────────────────────┤
│  등록된 재고가 없습니다.  (재고 없을 때)                  │
└─────────────────────────────────────────────────────────┘
[+ 이 위치에 재고 추가]
```

- 브레드크럼은 서버에서 쿼리 결과를 기반으로 렌더링한다. (Space name, Shelf name, Box name을 별도 조회하거나 DTO에 포함)
- `[+]` / `[-]` 클릭 → 모달 → 처리 완료 시 해당 `<tr>` 전체를 갱신 (`hx-swap="outerHTML"`).
- 재고가 없을 때는 빈 메시지 행을 표시한다.

---

## 9. 선택지 비교 — 이 구조를 선택한 이유

| 대안 | 장점 | 단점 | 결론 |
|---|---|---|---|
| **드릴다운 (별도 페이지)** `/spaces/{id}` → `/shelves/{id}` → `/boxes/{id}` | 구현 단순 | 페이지 이동마다 전체 렌더링, 계층 맥락 손실 | 탈락 |
| **전체 트리 한번에 렌더링** (Shelf+Box+Stock 모두 SSR) | 첫 화면 완성도 높음 | 재고가 많아지면 초기 로딩 비용 큼, 복잡한 중첩 루프 | 탈락 |
| **2패널 (위치 트리 + 재고 패널)** ← 채택 | 계층 탐색과 재고 확인을 한 화면에서 처리, HTMX에 최적 | 초기 구현 복잡도 약간 높음 | **채택** |
| **밀러 컬럼 (3열 Finder 방식)** | 계층 탐색에 직관적 | 모바일 부적합, 구현 복잡 | 탈락 |

---

## 10. 구현 우선순위 (Phase 2 세분화)

| 순서 | 작업 | 비고 |
|---|---|---|
| 1 | `spaces/detail.html` 레이아웃 (2패널 골격만) | SSR, 데이터 없어도 구조 확인 가능 |
| 2 | `ShelfService` + `ShelfController` (목록, 추가, 삭제) | 트리 패널 왼쪽 완성 |
| 3 | HTMX Box 목록 로드 (`box-list` 프래그먼트) | 아코디언 토글 |
| 4 | `BoxService` + `BoxController` (목록, 추가, 삭제) | 트리 패널 완성 |
| 5 | `stocks/fragments/panel.html` + 재고 패널 라우팅 | 위치별 재고 조회 |
| 6 | 재고 추가 모달 | `StockService` 연동 |
| 7 | 입고/출고 모달 + 행 갱신 | `StockService.processIn/Out` |
