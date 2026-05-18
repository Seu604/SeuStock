# Persistence Layer Proceed Report v1

**작성일:** 2026-05-18  
**작업 범위:** MyBatis Mapper 레이어 — Optional 적용 및 테스트 환경 구축

---

## 1. 작업 배경

SeuStock 프로젝트의 MyBatis Mapper 인터페이스에 대한 두 가지 개선 작업을 수행하였다.

1. 단건 조회 메소드에 `Optional<T>` 반환 타입 적용
2. H2 인메모리 데이터베이스 기반의 Mapper 통합 테스트 환경 구축

---

## 2. Optional 적용

### 2.1 배경 및 근거

기존 단건 조회 메소드는 모두 raw 타입을 반환하고 있었다. 조회 결과가 없을 경우 MyBatis는 `null`을 반환하므로, 호출부에서 null 체크를 누락하면 `NullPointerException` 발생 위험이 존재했다.

MyBatis 3.5.0 이상부터 `Optional<T>`를 공식 반환 타입으로 지원하며, null 결과를 자동으로 `Optional.empty()`로 래핑한다. XML 매퍼는 수정이 불필요하다.

**적용 기준:**

| 메소드 유형 | Optional 적용 | 이유 |
|---|---|---|
| `findById`, `findByUsername` 등 단건 | 적용 | 결과가 없을 수 있는 단건 조회 |
| `findByUserId`, `findBySpaceId` 등 목록 | 미적용 | `List`는 빈 리스트로 처리, `Optional<List>`는 안티패턴 |
| `insert`, `update`, `delete` | 미적용 | 반환값 없음 |

### 2.2 변경 내역

총 7개 Mapper 인터페이스의 단건 조회 메소드에 `Optional<T>` 적용.

| Mapper | 변경 전 | 변경 후 |
|---|---|---|
| `UserMapper` | `UserDTO findByUsername(String)` | `Optional<UserDTO> findByUsername(String)` |
| `ItemMapper` | `ItemDTO findById(Long)` | `Optional<ItemDTO> findById(Long)` |
| `StockMapper` | `StockDTO findById(Long)` | `Optional<StockDTO> findById(Long)` |
| `SpaceMapper` | `SpaceDTO findById(Long)` | `Optional<SpaceDTO> findById(Long)` |
| `ShelfMapper` | `ShelfDTO findById(Long)` | `Optional<ShelfDTO> findById(Long)` |
| `BoxMapper` | `BoxDTO findById(Long)` | `Optional<BoxDTO> findById(Long)` |
| `StockTransactionMapper` | `StockTransactionDTO findById(Long)` | `Optional<StockTransactionDTO> findById(Long)` |

### 2.3 향후 서비스 레이어 활용 패턴

```java
// 없으면 예외
UserDTO user = userMapper.findByUsername(username)
    .orElseThrow(() -> new NoSuchElementException("User not found"));

// 없으면 기본값
UserDTO user = userMapper.findByUsername(username)
    .orElse(defaultUser);

// 있을 때만 처리
userMapper.findByUsername(username)
    .ifPresent(u -> doSomething(u));
```

---

## 3. UUID TypeHandler 등록

### 3.1 문제 발견

테스트 환경 구축 중 MyBatis가 `java.util.UUID` 타입에 대한 TypeHandler를 기본 제공하지 않는다는 것을 발견하였다. XML 매퍼 파싱 시점에 아래 오류가 발생하였다.

```
java.lang.IllegalStateException: No typehandler found for property externalId
    at org.apache.ibatis.mapping.ResultMapping$Builder.validate(ResultMapping.java:153)
```

이는 MyBatis의 `TypeHandlerRegistry`가 `java.util.UUID`를 기본 등록 타입으로 포함하지 않기 때문이다. 프로덕션 환경(PostgreSQL)에서도 동일한 문제가 잠재하고 있었으며, 이번 테스트 작업을 통해 사전에 발견하였다.

### 3.2 조치 내용

`UUIDTypeHandler`를 신규 작성하고 MyBatis 설정에 등록하였다.

**신규 파일:** `src/main/java/com/seu/seustock/configuration/UUIDTypeHandler.java`

```java
@MappedTypes(UUID.class)
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {
    // UUID 객체 및 문자열 모두 처리 (H2, PostgreSQL 양쪽 호환)
}
```

**`application.properties` 추가:**
```properties
mybatis.type-handlers-package=com.seu.seustock.configuration
```

이 TypeHandler는 프로덕션(PostgreSQL)과 테스트(H2) 환경 모두에 적용된다.

---

## 4. 테스트 환경 구축

### 4.1 구성 개요

| 항목 | 내용 |
|---|---|
| 테스트 DB | H2 인메모리 (`MODE=PostgreSQL`) |
| 테스트 프레임워크 | `@MybatisTest` (mybatis-spring-boot-starter-test:4.0.1) |
| 트랜잭션 | 메소드마다 자동 롤백 (`@Transactional` 내장) |
| 스키마 초기화 | `@Sql("classpath:schema-test.sql")` — 메소드 실행 전 DDL 수행 |

### 4.2 신규 파일 목록

```
src/test/resources/
├── application-test.properties    # H2 datasource + MyBatis 설정
└── schema-test.sql                # H2 호환 테스트 스키마

src/test/java/com/seu/seustock/mapper/
├── UserMapperTest.java
├── SpaceMapperTest.java
├── ShelfMapperTest.java
├── BoxMapperTest.java
├── ItemMapperTest.java
├── StockMapperTest.java
└── StockTransactionMapperTest.java
```

### 4.3 application-test.properties

```properties
spring.datasource.url=jdbc:h2:mem:seustock_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

mybatis.mapper-locations=classpath:mapper/**/*.xml
mybatis.type-aliases-package=com.seu.seustock.model.dto
mybatis.type-handlers-package=com.seu.seustock.configuration
mybatis.configuration.map-underscore-to-camel-case=true
```

### 4.4 테스트 어노테이션 구성

```java
@MybatisTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("classpath:schema-test.sql")
```

| 어노테이션 | 역할 |
|---|---|
| `@MybatisTest` | MyBatis 슬라이스 컨텍스트 + `@Transactional` 내장 |
| `@ActiveProfiles("test")` | `application-test.properties` 활성화 |
| `@AutoConfigureTestDatabase(replace=NONE)` | 자동 datasource 교체 비활성화, 설정 파일의 H2 사용 |
| `@Sql("classpath:schema-test.sql")` | 각 테스트 메소드 전 `IF NOT EXISTS` DDL 실행 |

> **Spring Boot 4.x 변경사항:** `AutoConfigureTestDatabase`의 패키지가 `org.springframework.boot.test.autoconfigure.jdbc`에서 `org.springframework.boot.jdbc.test.autoconfigure`로 이동되었다.

### 4.5 테스트 케이스 목록

**UserMapperTest (4개)**

| 테스트 메소드 | 검증 내용 |
|---|---|
| `insertUser_thenFindByUsername` | 삽입 후 조회, id/externalId/username 검증 |
| `findByUsername_notFound_returnsEmpty` | 존재하지 않는 사용자 조회 → `Optional.empty()` |
| `updatePassword` | 비밀번호 변경 후 재조회 검증 |
| `deleteById` | 삭제 후 조회 → `Optional.empty()` |

**SpaceMapperTest (5개)**

| 테스트 메소드 | 검증 내용 |
|---|---|
| `insertSpace_thenFindById` | 삽입 후 조회, id/externalId/name/userId 검증 |
| `findById_notFound_returnsEmpty` | 존재하지 않는 ID 조회 → `Optional.empty()` |
| `findByUserId` | 사용자 ID로 목록 조회, 2개 반환 검증 |
| `updateSpace` | 이름 변경 후 재조회 검증 |
| `deleteById` | 삭제 후 조회 → `Optional.empty()` |

**ShelfMapperTest (5개)** — `BeforeEach`에서 User → Space 생성

**BoxMapperTest (5개)** — `BeforeEach`에서 User → Space → Shelf 생성

**ItemMapperTest (5개)** — `BeforeEach`에서 User 생성

**StockMapperTest (6개)** — `BeforeEach`에서 User → Item, User → Space 생성

| 테스트 메소드 | 검증 내용 |
|---|---|
| `insertStock_thenFindById` | 삽입 후 조회, 수량/위치 검증 |
| `findById_notFound_returnsEmpty` | 존재하지 않는 ID → `Optional.empty()` |
| `findByItemId` | 아이템 ID로 목록 조회 |
| `findBySpaceId` | 공간 ID로 목록 조회 |
| `updateStock` | 수량 변경 후 재조회 검증 |
| `deleteById` | 삭제 후 조회 → `Optional.empty()` |

**StockTransactionMapperTest (4개)** — `BeforeEach`에서 User → Item → Space → Stock 생성

| 테스트 메소드 | 검증 내용 |
|---|---|
| `insertTransaction_thenFindById` | 삽입 후 조회, 타입/delta/memo 검증 |
| `findById_notFound_returnsEmpty` | 존재하지 않는 ID → `Optional.empty()` |
| `findByStockId_returnsAllTransactions` | 재고 ID로 전체 이력 조회, 순서 검증 |
| `findByStockId_noTransactions_returnsEmpty` | 이력 없는 재고 조회 → 빈 리스트 |

### 4.6 schema-test.sql 주요 변경 사항

프로덕션 스키마(`schema-v1.sql`)와 테스트 스키마의 차이:

| 항목 | 프로덕션 (PostgreSQL) | 테스트 (H2) |
|---|---|---|
| AUTO INCREMENT | `SERIAL` | `BIGINT GENERATED BY DEFAULT AS IDENTITY` |
| UUID 기본값 | `DEFAULT gen_random_uuid()` | `DEFAULT RANDOM_UUID()` |
| 멱등성 보장 | 없음 | `CREATE TABLE IF NOT EXISTS` |

---

## 5. 트러블슈팅 이력

| # | 발생 오류 | 원인 | 조치 |
|---|---|---|---|
| 1 | `cannot find symbol: AutoConfigureTestDatabase` | Spring Boot 4.x에서 패키지 변경 | import를 `org.springframework.boot.jdbc.test.autoconfigure`로 수정 |
| 2 | `No typehandler found for property externalId` | MyBatis 3.5.x 기본 UUID TypeHandler 미제공 | `UUIDTypeHandler` 작성 및 `mybatis.type-handlers-package` 등록 |
| 3 | `Syntax error: BIGINT AUTO_INCREMENT` | H2 PostgreSQL 모드에서 MySQL 문법 불가 | `GENERATED BY DEFAULT AS IDENTITY`로 변경 |

---

## 6. 최종 테스트 결과

```
BUILD SUCCESSFUL

34 tests completed, 0 failed
```

모든 Mapper 테스트 34개 통과.

---

## 7. 변경 파일 요약

| 파일 | 구분 | 내용 |
|---|---|---|
| `src/main/java/.../mapper/UserMapper.java` | 수정 | `Optional<UserDTO> findByUsername` |
| `src/main/java/.../mapper/ItemMapper.java` | 수정 | `Optional<ItemDTO> findById` |
| `src/main/java/.../mapper/StockMapper.java` | 수정 | `Optional<StockDTO> findById` |
| `src/main/java/.../mapper/SpaceMapper.java` | 수정 | `Optional<SpaceDTO> findById` |
| `src/main/java/.../mapper/ShelfMapper.java` | 수정 | `Optional<ShelfDTO> findById` |
| `src/main/java/.../mapper/BoxMapper.java` | 수정 | `Optional<BoxDTO> findById` |
| `src/main/java/.../mapper/StockTransactionMapper.java` | 수정 | `Optional<StockTransactionDTO> findById` |
| `src/main/java/.../configuration/UUIDTypeHandler.java` | 신규 | UUID MyBatis TypeHandler |
| `src/main/resources/application.properties` | 수정 | `mybatis.type-handlers-package` 추가 |
| `src/test/resources/application-test.properties` | 신규 | H2 datasource + MyBatis 테스트 설정 |
| `src/test/resources/schema-test.sql` | 신규 | H2 호환 테스트 스키마 |
| `src/test/java/.../mapper/UserMapperTest.java` | 신규 | UserMapper 테스트 (4개) |
| `src/test/java/.../mapper/SpaceMapperTest.java` | 신규 | SpaceMapper 테스트 (5개) |
| `src/test/java/.../mapper/ShelfMapperTest.java` | 신규 | ShelfMapper 테스트 (5개) |
| `src/test/java/.../mapper/BoxMapperTest.java` | 신규 | BoxMapper 테스트 (5개) |
| `src/test/java/.../mapper/ItemMapperTest.java` | 신규 | ItemMapper 테스트 (5개) |
| `src/test/java/.../mapper/StockMapperTest.java` | 신규 | StockMapper 테스트 (6개) |
| `src/test/java/.../mapper/StockTransactionMapperTest.java` | 신규 | StockTransactionMapper 테스트 (4개) |
