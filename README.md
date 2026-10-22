# Ubique Prayer Backend

유비크 기도 서비스의 Spring Boot 기반 백엔드 프로젝트이다.
웹 서버와 PostgreSQL·Flyway 연동, Docker 실행 환경, 테스트용 DB 분리, 보안·입력 검증, API 문서와 CI 기본 설정을 포함한다.

## 팀 공유 문서

| 문서 | 역할 |
| --- | --- |
| [README](README.md) | 현재 개발 환경과 실행·테스트 방법 |
| [설계 기록](docs/design.md) | 대화로 결정한 요구사항·아키텍처, 제안과 미결정 정책 |
| [DB 상세 명세](docs/database.md) | 11개 테이블의 필드·관계·제약, 인증 흐름과 검토 항목 |
| [AI 공통 개발 규칙](AGENTS.md) | AI 코딩 도구가 작업할 때 준수할 공통 원칙 |

설계 문서의 확정 여부와 실제 구현 상태는 구분한다. 설계 변경 시 관련 코드·마이그레이션과 문서를 함께 갱신한다.

## 개발 환경

| 항목 | 버전 / 구성 |
| --- | --- |
| Java | JDK 21 |
| Spring Boot | 4.1.1 |
| Gradle | 9.8.1 (Wrapper 포함) |
| PostgreSQL | 18.6 (Docker Compose / Testcontainers) |
| API 문서 | springdoc-openapi 3.1.1 / Swagger UI |
| 테스트 | JUnit·Testcontainers (Spring Boot BOM 관리) |
| 기본 패키지 | `com.ubique.prayer` |
| 기본 포트 | 8080 |

Docker로 백엔드와 DB를 함께 실행하려면 Docker Desktop이 필요하다.
로컬 터미널이나 IDE에서 백엔드를 직접 실행하거나 테스트하려면 JDK 21도 필요하다.
Gradle은 포함된 Wrapper가 지정 버전을 내려받으므로 별도 설치가 필요하지 않다.
첫 실행 시에는 Docker 이미지, Gradle, 의존성을 내려받기 위한 인터넷 연결이 필요하다.

## 로컬 PostgreSQL 실행

로컬 PostgreSQL은 Docker Compose로 실행한다. Mac에 PostgreSQL을 별도로 설치할 필요는 없다.
실행 전에는 Docker Desktop을 시작한다. 로컬 이미지는 `postgres:18.6-bookworm`을 사용한다.
PostgreSQL 18은 초기 배포 대상인 Render에서도 지원하는 메이저 버전이다.

### 1. 개인 환경변수 준비

`.env` 파일이 이미 있으면 복사 단계를 건너뛴다.

macOS / Linux:

```sh
cp -n .env.example .env
```

Windows PowerShell:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

새로 복사한 `.env`에서 `POSTGRES_PASSWORD`를 개인 개발용 비밀번호로 바꾼다.
자동 구성 과정에서 생성한 `.env`에는 임의의 비밀번호가 이미 설정되어 있다.
`.env`는 Git에서 제외하며, 팀에는 항목과 예시만 담긴 `.env.example`을 공유한다.
Docker Compose와 Spring Boot는 같은 `.env`를 사용한다. Spring Boot는 `spring.config.import`로
파일을 읽어 `POSTGRES_*` 값을 DB 접속 정보로 사용한다. 배포 환경에 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`가 설정되어 있으면 해당 값을 우선 사용한다.
`DB_URL`은 `jdbc:postgresql://호스트:포트/DB이름` 형식이다. 실제 비밀번호는 공통 설정 파일에 작성하지 않는다.

### 2. 설정 확인과 DB 시작

프로젝트 폴더에서 다음 명령을 실행한다.

```sh
docker compose config --quiet
docker compose up -d --wait postgres
docker compose ps
```

`config --quiet`는 비밀번호를 출력하지 않고 설정 문법을 확인한다.
`up -d --wait`는 DB를 백그라운드에서 실행하고 상태 확인이 성공할 때까지 기다린다.
`ps`에서 `postgres` 서비스가 `healthy`이면 DB가 접속을 받을 준비가 된 상태이다.

기본 접속 정보는 다음과 같다. `.env`에서 값을 변경했다면 변경한 값을 사용한다.

| 항목 | 기본값 |
| --- | --- |
| 호스트 | `127.0.0.1` |
| 포트 | `5432` |
| 데이터베이스 | `ubique_prayer` |
| 사용자 | `ubique` |
| 비밀번호 | 개인 `.env`의 `POSTGRES_PASSWORD` |

포트가 이미 사용 중이면 첫 실행 전에 `.env`의 `POSTGRES_PORT`를 다른 값으로 변경한다.
DB 포트는 `127.0.0.1`에 연결되어 같은 컴퓨터에서만 접근할 수 있다.

### 3. SQL 접속 확인

컨테이너에 포함된 `psql`을 사용하므로 Mac에 DB 클라이언트를 따로 설치할 필요가 없다.
다음 명령은 컨테이너의 환경변수에 설정된 비밀번호로 PostgreSQL에 TCP 접속한다.

```sh
docker compose exec postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" exec psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

접속 후 아래 SQL을 입력한다.

```sql
SELECT current_database(), current_user;
```

기본 설정에서는 `ubique_prayer`, `ubique`가 표시된다. 종료하려면 `\q`를 입력한다.

### 4. 종료와 데이터 보관

DB를 일시 정지하거나 다시 실행할 때는 다음 명령을 사용한다.

```sh
docker compose stop postgres
docker compose start --wait postgres
```

컨테이너를 종료하고 정리할 때는 다음 명령을 사용한다.

```sh
docker compose down
```

DB 데이터는 `postgres_data`라는 Docker 볼륨에 보관한다.
PostgreSQL 18 이미지의 데이터 보관 경로에 맞춰 `/var/lib/postgresql`에 연결한다.
`docker compose down` 이후 다시 `up -d --wait postgres`를 실행해도 데이터는 유지된다.
`down`에 `-v`를 붙이면 볼륨 데이터도 삭제되므로 일반 종료에는 사용하지 않는다.

DB 이름·사용자·비밀번호는 빈 볼륨을 처음 초기화할 때 반영된다.
DB가 이미 생성된 뒤 `.env`만 바꾸면 기존 계정이나 비밀번호가 자동 변경되지 않는다.

Spring Boot는 이 PostgreSQL에 JPA로 연결하며, 업무 테이블 생성과 변경은 ERD에 맞춰 Flyway 마이그레이션으로 관리한다.

## Docker로 백엔드와 DB 함께 실행

### 1. 실행 준비

Docker Desktop을 실행한 뒤, 개인 환경변수 준비 절차에 따라 `.env`를 준비한다.
이미 `.env`가 있으면 유지하고 필요한 항목만 추가한다.
`BACKEND_PORT`는 로컬 컴퓨터에서 백엔드에 접속할 때 사용하는 포트이다. 기본값은 `8080`이다.
Mac에서 실행한 `bootRun` 등이 8080을 사용 중이면 해당 서버를 `Ctrl+C`로 종료하거나
개인 `.env`에 `BACKEND_PORT=8081`을 설정한다. 컨테이너 내부 포트는 항상 8080이다.

### 2. 이미지 빌드와 실행

프로젝트 폴더에서 다음 명령을 실행한다.

```sh
docker compose config --quiet
docker compose up --build -d --wait --wait-timeout 180
docker compose ps
```

첫 이미지 빌드는 Java 이미지와 Gradle 의존성을 내려받으므로 시간이 걸릴 수 있다.
`Dockerfile`은 Java 21 JDK 단계에서 `bootJar`로 실행 JAR를 만들고,
Java 21 JRE 단계에 JAR만 복사하여 일반 사용자 `app`으로 실행한다.
이미지 빌드는 실행 JAR 생성까지 수행한다. 테스트는 아래의 테스트·빌드 절차에서 별도로 실행한다.
소스를 수정한 뒤에는 같은 `up --build` 명령으로 이미지를 다시 빌드한다.

Compose는 PostgreSQL이 `healthy`가 된 뒤 백엔드를 시작한다.
백엔드는 `postgres:5432`로 DB에 연결하며, 호스트용 `POSTGRES_PORT`와 구분된다.
DB 접속 정보는 Compose의 환경변수로 전달하고 `.env` 파일 자체는 이미지에 복사하지 않는다.
기존 DB 서비스와 `postgres_data` 볼륨을 함께 사용하므로 데이터는 유지된다.

### 3. 정상 실행 확인

`docker compose ps`에서 `backend`, `postgres` 모두 `healthy`인지 확인한다.
기본 포트에서는 <http://localhost:8080/actuator/health>로 접속한다.
`BACKEND_PORT=8081`로 설정했다면 <http://localhost:8081/actuator/health>로 접속한다.
HTTP 200 응답과 `"status":"UP"`을 확인한다.

백엔드 시작 과정과 DB 연결, Flyway 실행 로그는 다음 명령으로 확인한다.

```sh
docker compose logs --tail=100 backend
```

현재는 업무용 SQL이 없으므로 Flyway 마이그레이션 개수가 0으로 표시되는 것이 정상이다.
실행에 실패하면 로그와 `docker compose ps -a`를 확인한다.
`port is already allocated` 오류가 발생하면 `.env`의 `BACKEND_PORT`와 해당 포트를 사용 중인 프로세스를 확인한다.

### 4. 종료와 재실행

DB를 유지하면서 백엔드만 정지할 때는 다음 명령을 사용한다.

```sh
docker compose stop backend
```

소스 변경 없이 백엔드를 다시 실행할 때는 다음 명령을 사용한다.

```sh
docker compose up -d --wait backend
```

백엔드와 DB 컨테이너를 함께 종료할 때는 다음 명령을 사용한다.

```sh
docker compose down
```

일반 종료에는 `-v` 옵션을 사용하지 않는다. DB 데이터는 볼륨에 남는다.
Docker 빌드에서 제외할 파일은 `.dockerignore`에서 관리한다.

## Mac/PC에서 백엔드 직접 실행

먼저 `docker compose up -d --wait postgres`로 DB를 준비한다.
Docker 백엔드와 같은 8080 포트를 사용한다면 `docker compose stop backend`로 컨테이너를 멈춘 후 실행한다.

macOS / Linux:

```sh
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

서버가 시작되면 브라우저에서 <http://localhost:8080/actuator/health>를 연다.
응답의 `status`가 `UP`이면 정상이다. 응답 예시는 다음과 같다.

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

상태 확인 API는 Spring Boot Actuator가 제공한다.
서버를 종료하려면 실행 중인 터미널에서 `Ctrl+C`를 누른다.

## Flyway로 DB 변경 관리

Flyway와 PostgreSQL 지원 모듈은 Spring Boot BOM이 버전을 관리한다.
서버를 시작하면 `src/main/resources/db/migration`의 버전 SQL을 확인하고
미적용 파일을 JPA 초기화 전에 실행한다.

현재는 ERD 확정 전이므로 업무 테이블을 생성하는 SQL이 없는 상태이다.
Flyway 관리 테이블인 `flyway_schema_history`만 준비하며,
업무용 첫 SQL은 ERD에 맞춰 `V1__initial_schema.sql`로 추가한다.
서버 시작 시 마이그레이션 파일이 없다는 로그가 표시될 수 있으며, 초기 구성 단계에서는 정상이다.

현재 설정은 다음과 같다.

| 설정 | 역할 |
| --- | --- |
| `spring.jpa.hibernate.ddl-auto=validate` | JPA는 테이블을 자동 변경하지 않고 구조를 검증 |
| `spring.sql.init.mode=never` | 별도 `schema.sql`·`data.sql` 초기화 방지 |
| `spring.flyway.validate-on-migrate=true` | 시작 시 적용 이력과 SQL 체크섬 검증 |
| `spring.flyway.validate-migration-naming=true` | 마이그레이션 파일 이름 형식 검증 |
| `spring.flyway.baseline-on-migrate=false` | 기존 스키마를 자동으로 기준 버전 처리하지 않음 |
| `spring.flyway.clean-disabled=true` | 스키마 일괄 삭제 기능 비활성화 |

적용 이력은 SQL 접속 후 다음 쿼리로 확인한다.

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

첫 업무 마이그레이션을 추가하기 전에는 이력 조회 결과가 비어 있는 것이 정상이다.
파일 이름과 작성 규칙은 `src/main/resources/db/migration/README.md`에 정리되어 있다.
이미 적용한 SQL은 유지하고 변경 사항을 새 버전 파일로 추가한다.

## 테스트와 빌드

macOS / Linux:

```sh
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

테스트에는 JDK 21과 실행 중인 Docker Desktop이 필요하다.
개인 `.env`와 Compose 개발 DB는 테스트에 필요하지 않다.
`PostgresTestConfiguration`이 `postgres:18.6-bookworm` 임시 컨테이너를 만들고,
Spring Boot의 `@ServiceConnection`이 JPA와 Flyway를 해당 DB에 연결한다.
테스트에서는 `.env`를 읽지 않고 개인 DB 접속 설정도 사용하지 않는다. 대신 임시 DB에서 제공하는 접속 정보를 사용한다.
테스트 컨테이너는 임의 포트를 사용하고 Spring 테스트 컨텍스트 종료 시 정리된다.
개발 DB 볼륨을 연결하지 않으며 테스트 데이터를 개발 DB에 저장하지 않는다.

통합 테스트는 다음 항목을 확인한다.

- 임시 PostgreSQL 접속과 테스트 DB 이름
- Flyway 변경 이력 테이블 생성과 마이그레이션 검증
- 인증 없이 상태 확인 API 조회
- OpenAPI JSON 생성과 Swagger UI 정적 파일 제공
- 비공개 경로의 익명 요청 차단과 초기 보안 정책 적용
- Jakarta Validation의 이메일·빈 값 검증

빌드한 JAR 파일은 `build/libs/`에 생성된다. 테스트 결과는 `build/reports/tests/test/index.html`에서 확인할 수 있다.
Docker가 중지되어 있으면 테스트가 실패한다. Docker Desktop을 실행한 뒤 다시 빌드한다.

## 공통 보안·입력 검증·API 문서

### Spring Security 초기 정책

`config/SecurityConfig.java`에서 GET 상태 확인(`/actuator/health`, 하위 경로),
OpenAPI(`/v3/api-docs`, 하위 경로, YAML), Swagger UI 조회를 허용한다.
그 외의 요청은 기본적으로 차단한다. 인증되지 않은 사용자가 비공개 경로에 접근하면 HTTP 401로 응답한다.
현재는 로그인·JWT·사용자 인증 제공자가 없는 상태이며, Spring Boot의 기본 사용자 자동 생성도 비활성화되어 있다.
폼 로그인·HTTP Basic·로그아웃·요청 캐시는 비활성화되어 있으며, 세션을 사용하지 않는다.
CSRF 기본 보호는 유지한다.
인증 기능을 구현할 때 `anyRequest().denyAll()`을 도메인별 권한 규칙으로 교체한다.

### Jakarta Validation

입력값 검증에는 `spring-boot-starter-validation`을 사용한다.
향후 요청 DTO에 `@NotBlank`, `@Email` 등의 제약을 선언하고 컨트롤러에서 `@Valid`를 사용한다.
메서드 검증이 필요한 서비스에는 `@Validated`를 사용한다.
업무 DTO나 공통 오류 응답 형식은 기능 설계에 맞춰 추가한다.

### OpenAPI / Swagger UI

`config/OpenApiConfig.java`에서 문서 제목과 버전을 설정한다.
컨트롤러를 추가하면 API 명세가 자동으로 생성된다. 현재는 업무 API가 없어 목록이 비어 있을 수 있다.

| 용도 | 기본 주소 |
| --- | --- |
| 상태 확인 | <http://localhost:8080/actuator/health> |
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

Docker의 `BACKEND_PORT=8081`을 사용하면 위 주소의 포트도 8081로 바꾼다.
`.env` 또는 실행 환경에 `API_DOCS_ENABLED=false`를 설정하면 JSON과 UI를 함께 끈다.
Docker는 `docker compose up -d --wait backend`로 변경한 환경변수를 반영한다.
직접 실행 중인 `bootRun`은 종료 후 다시 실행한다.

## GitHub Actions

`.github/workflows/ci.yml`은 push, pull request, 수동 실행 시 다음 작업을 수행한다.

1. 소스를 받고 Java 21·Gradle 환경을 준비한다.
2. Gradle Wrapper를 검증하고 `./gradlew build`로 임시 PostgreSQL 통합 테스트와 JAR 빌드를 수행한다.
3. `docker build`로 백엔드 이미지가 빌드되는지 확인한다.

GitHub 호스팅 Linux 러너의 Docker를 사용한다. 개인 `.env` 파일이나 DB 비밀번호용 GitHub Secrets는 필요하지 않다.
사용하는 Actions의 버전은 릴리스의 전체 커밋 SHA로 고정되어 있다.
이미지 푸시나 배포는 수행하지 않는다.
실제 GitHub 실행 결과는 팀과 커밋·푸시한 뒤 저장소 Actions에서 확인한다.

## 팀 공유 전 확인

프로젝트를 처음 실행하는 팀원은 다음 순서로 환경을 확인한다.

1. Docker Desktop을 실행한다. 직접 실행·테스트를 하려면 JDK 21도 준비한다.
2. `.env.example`을 `.env`로 복사하고 개인 DB 비밀번호를 설정한다.
3. `docker compose config --quiet`와 `docker compose up --build -d --wait --wait-timeout 180`을 실행한다.
4. 두 컨테이너가 `healthy`인지, 상태 확인 응답이 `UP`인지 확인한다.
5. Swagger UI와 OpenAPI JSON이 열리는지 확인한다.
6. `./gradlew build`로 개발 DB와 분리된 테스트를 실행한다.
7. `.env`가 Git 대상에서 제외되었는지 확인한 뒤 팀과 변경 사항을 검토하고 커밋·푸시한다.

로컬 환경 설정에는 기본 실행 환경, DB·Flyway 연동, Docker 구성, 테스트용 DB 분리, 공통 라이브러리 설정과 CI 파일 준비가 포함된다.
GitHub CI의 첫 실행 확인은 푸시 후 진행한다.
ERD 기반 업무 마이그레이션, 로그인·JWT, 업무 API와 클라우드 배포는 후속 기능·배포 작업이다.

## macOS에서 한글 경로 관련 오류가 발생할 때

터미널의 `LC_ALL` 값이 `C`이면 Gradle이 한글 경로를 ASCII로 처리해
테스트에서 `ClassNotFoundException`이 발생할 수 있다.
이 경우 해당 명령을 UTF-8 설정으로 실행한다.

```sh
LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 ./gradlew build
LC_ALL=en_US.UTF-8 LANG=en_US.UTF-8 ./gradlew bootRun
```

## 공통 설정 관리

- 소스 코드, Gradle Wrapper, 공통 설정은 Git으로 공유한다.
- 실제 비밀번호나 API 키가 담긴 `.env` 파일은 Git에서 제외한다.
- 백엔드 이미지는 `Dockerfile`, 빌드 제외 항목은 `.dockerignore`에서 관리한다.
- 백엔드와 PostgreSQL 실행은 `compose.yaml`, 개인 접속 값은 `.env`, 공유 예시는 `.env.example`로 관리한다.
- Spring Boot DB 연결과 Flyway 설정은 `application.properties`에서 관리하며 실제 접속 값은 환경변수로 받는다.

## 설계 기준

- [아키텍처 및 스택 확정 (FE/BE/APP)](https://app.notion.com/p/FE-BE-APP-3fcc21308ffb80d2be47e1942a038da4)
- [Spring Initializr](https://start.spring.io/)

- [PostgreSQL 공식 Docker 이미지](https://hub.docker.com/_/postgres)
- [Render PostgreSQL 지원 버전](https://render.com/docs/postgresql-upgrading#version-support)

- [Spring Boot Flyway 설정 안내](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)

- [Docker 멀티 단계 빌드](https://docs.docker.com/build/building/multi-stage/)
- [Docker Compose 서비스 시작 순서](https://docs.docker.com/compose/how-tos/startup-order/)

- [Spring Boot Testcontainers 연결](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Spring Security 설정](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
- [Spring Boot Validation](https://docs.spring.io/spring-boot/reference/io/validation.html)
- [springdoc-openapi](https://springdoc.org/)
