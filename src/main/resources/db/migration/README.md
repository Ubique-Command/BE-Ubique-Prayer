# Flyway 마이그레이션 작성 안내

이 폴더에는 DB 스키마를 변경하는 버전별 SQL 파일을 추가합니다.
현재는 ERD 확정 전이므로 업무용 마이그레이션 SQL은 없습니다.
최초 실행에서는 Flyway 관리용 `flyway_schema_history` 테이블을 준비합니다.

## 파일 이름

```text
V1__initial_schema.sql
V2__add_new_columns.sql
```

- `V` 다음에 순서대로 증가하는 버전을 사용합니다.
- 버전과 설명 사이에는 밑줄 두 개(`__`)를 사용합니다.
- SQL 파일은 UTF-8로 저장합니다.
- 처음 업무 스키마를 만들 때 ERD를 기준으로 `V1__initial_schema.sql`을 작성합니다.
- 이미 DB에 적용된 SQL 파일은 수정하지 않고, 다음 버전 파일을 추가합니다.
  Flyway가 저장된 체크섬과 파일 내용을 비교하기 때문입니다.
- 파일은 Spring Boot 시작 시 자동 적용되며, JPA 초기화보다 먼저 실행됩니다.

## 스키마 관리 원칙

- JPA의 `ddl-auto=validate`를 유지합니다.
- 스키마 변경은 이 폴더의 Flyway SQL로 관리합니다.
- 별도의 `schema.sql`, `data.sql`, Docker 초기화 스크립트로 같은 업무 스키마를 만들지 않습니다.
- 기존 테이블이 있는 DB에 처음 도입할 때는 팀과 기준 버전 도입 방식을 합의합니다.
  현재 `baseline-on-migrate=false`여서 기존 스키마를 자동으로 기준 버전 처리하지 않습니다.
- `clean-disabled=true`로 Flyway의 스키마 일괄 삭제 기능을 비활성화했습니다.

## 적용 이력 확인

서버를 실행한 뒤 PostgreSQL에서 조회합니다.

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

아직 업무용 SQL을 추가하지 않은 현재 단계에서는 버전 마이그레이션 이력이 비어 있는 것이 정상입니다.
