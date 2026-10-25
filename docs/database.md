<!--
목적: 교회 공동체 앱의 PostgreSQL 테이블별 필드, 관계, 제약조건 및 인증 데이터 흐름을 공유한다.
대상: 백엔드·프런트엔드 개발자, 기획 담당자, AI 코딩 도구.
관리: 상세 DB 명세는 이 파일에서 관리하고 설계 배경은 design.md에 기록한다.
변경 시 관련 코드·Flyway SQL과 함께 갱신하며, 검토 중인 정책을 확정된 것으로 구현하지 않는다.
-->

# 데이터베이스 상세 명세

기준일: 2026-10-22 · DBMS: PostgreSQL · 현재 명세 대상: 11개 테이블

[설계 개요](design.md) · [개발 환경 안내](../README.md)

## 명세의 범위와 결정 상태

- 사용자가 제공한 11개 테이블 상세 명세와 인증 흐름을 기준으로 작성했다. 아직 실행 가능한 SQL이나 구현 완료 명세는 아니다.
- 기존에 확정한 이름에 맞춰 `groups`를 `cell`, `group_id`와 `primary_group_id`를 `cell_id`, `group_type`을 `cell_type`으로 통일했다.
- 역할 유형 `GROUP`, 공개 범위 `GROUP`·`GROUP_LEADER`는 제공된 코드 값을 유지한다. 여기서 그룹은 `cell`을 의미한다.
- 일반 로그인 테이블 이름은 `auth_credentials`로 통일한다. 과거 제안 이름 `password_credentials`는 사용하지 않는다.
- 아래 정책 중 명시적으로 권장 또는 검토 대상으로 표시한 항목은 확정값이 아니다.
- 달란트 지급과 첨부파일 메타데이터는 기존 요구사항으로 남아 있지만 이번 11개 테이블 명세에는 포함되지 않는다. 별도 설계가 필요하다.

### 해결이 필요한 충돌

**기도글 삭제 권한:** 기존 요구사항은 목회자의 전체 기도글 수정·삭제를 허용하지만 제공된 상세 명세는 작성자만 삭제 가능하다고 한다.
아래 상세 표는 제공된 내용을 유지한다. 구현 전에 이 충돌을 해결해야 한다.
권고안은 “작성자 또는 목회자가 삭제 가능하며, 응답 완료는 작성자만 가능”이다. 아직 이 권고를 확정 정책으로 간주하지 않는다.

# 공통 설계 기준

| **구분** | **설명** |
| --- | --- |
| `O` | 필수 (`NOT NULL`) |
| `X` | 선택 (`NULL` 허용) |
| `△` | 조건부 필수 |
| PK | 별도 표시가 없는 `bigint` PK는 자동 생성 |
| FK | 다른 테이블의 PK를 참조하는 외래 키 |
| `created_at` | 기본값 `CURRENT_TIMESTAMP` |
| `updated_at` | 애플리케이션에서 갱신 |
| 상태값 | `varchar` + `CHECK` 제약조건 적용 |
| 삭제 방식 | 사용자·기도 게시글·댓글은 Soft Delete 적용 |

# 1. 전체 테이블 목록

| **번호** | **테이블명** | **설명** | **주요 기능** |
| --- | --- | --- | --- |
| 1 | `users` | 사용자 관리 | 회원가입, 사용자 정보, 탈퇴 |
| 2 | `auth_credentials` | 아이디·비밀번호 일반 인증 | 아이디·비밀번호 로그인 |
| 3 | `social_accounts` | 소셜 로그인 인증 | 카카오·구글 로그인 |
| 4 | `roles` | 역할 정의 / 권한 관리 | 평신도·목회자·셀 리더·부리더 |
| 5 | `user_roles` | 그룹 내 추가 역할 / 권한 관리 | 평신도·목회자·셀 리더·부리더 |
| 6 | `clergy_profiles` | 목회자 상세 정보 / 목회자 관리 | 목회자 직책 및 사역 영역 |
| 7 | `cell` | 새신자 그룹·셀 / 그룹 관리 | 새신자 그룹 및 셀 관리 |
| 8 | `prayer_posts` | 기도 게시글 | 기도 게시글 등록·수정·삭제·응답 완료 |
| 9 | `prayer_comments` | 댓글 | 댓글 등록·수정·삭제·익명 처리 |
| 10 | `prayer_reactions` | 기도 반응 | 기도했습니다 등록·취소 |
| 11 | `refresh_tokens` | Refresh Token 관리, 인증 유지 | Access Token 재발급, 로그아웃 |

# 2. 테이블별 상세 정의서

## 2-1. **`users` — 사용자**

사용자의 기본 정보와 소속 그룹, 기본 역할을 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `user_id` | `bigint` | O | 사용자 PK |
| `name` | `varchar(100)` | O | 사용자 이름 |
| `email` | `varchar(320)` | O | 연락 및 계정 복구용 이메일. 대소문자를 구분하지 않는 UNIQUE |
| `birth` | `date` | O | 생년월일 |
| `profile_image_key` | `text` | X | R2에 저장된 프로필 이미지 Object Key |
| `base_role_id` | `bigint` | O | 기본 역할 FK → `roles.role_id`. `LAY` 또는 `CLERGY` |
| `cell_id` | `bigint` | △ | 주 소속 그룹 FK → `cell.cell_id`. 평신도 필수, 목회자 선택 |
| `status` | `varchar(20)` | O | 사용자 상태. `ACTIVE`, `INACTIVE`, `DELETED` |
| `created_at` | `timestamptz` | O | 가입 완료 시각 |
| `updated_at` | `timestamptz` | O | 사용자 정보 수정 시각 |
| `deleted_at` | `timestamptz` | X | 탈퇴 처리 시각 |

- **제약조건 및 정책**

| **항목** | **설명** |
| --- | --- |
| 이메일 | 대소문자 구분 없이 UNIQUE |
| 기본 역할 | `LAY`, `CLERGY`만 허용 |
| 평신도 소속 | 활성 새신자 그룹 또는 셀에 반드시 소속 |
| 목회자 소속 | 그룹 소속 선택 |
| 가입 시점 | 필수 정보 입력이 완료된 시점에 사용자 행 생성 |
| 탈퇴 | `status = 'DELETED'`, `deleted_at` 기록 |

## 2-2. **`auth_credentials` — 아이디·비밀번호 인증**

일반 로그인에 필요한 인증 정보를 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `user_id` | `bigint` | O | PK이자 FK → `users.user_id`. 자동 생성하지 않음 |
| `login_id` | `varchar(100)` | O | 로그인 아이디. 대소문자를 구분하지 않는 UNIQUE |
| `password_hash` | `varchar(255)` | O | 단방향 비밀번호 해시 |
| `failed_login_count` | `integer` | O | 연속 로그인 실패 횟수. 기본값 `0` |
| `locked_until` | `timestamptz` | X | 비밀번호 로그인 잠금 해제 시각 |
| `last_login_at` | `timestamptz` | X | 마지막 비밀번호 로그인 성공 시각 |
| `password_changed_at` | `timestamptz` | O | 비밀번호 설정 및 변경 시각 |
| `created_at` | `timestamptz` | O | 인증 정보 생성 시각 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 사용자 관계 | 사용자당 최대 1개 |
| 로그인 아이디 | 대소문자 구분 없이 UNIQUE |
| 비밀번호 | 평문 저장 금지, 단방향 해시 저장 |
| 로그인 실패 횟수 | `failed_login_count >= 0` |
| 계정 잠금 | 연속 로그인 실패 시 `locked_until` 설정 |
| 소셜 전용 사용자 | 해당 테이블에 행이 존재하지 않음 |

## 2-3. **`social_accounts` — 소셜 로그인 연결**

카카오,구글 등 외부 인증 제공자와 연결된 계정 관리 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `social_account_id` | `bigint` | O | 소셜 계정 PK |
| `user_id` | `bigint` | O | 사용자 FK → `users.user_id` |
| `provider` | `varchar(30)` | O | 소셜 로그인 제공자. `KAKAO`, `GOOGLE`, `NAVER` |
| `provider_user_id` | `varchar(255)` | O | 제공자가 발급한 사용자 고유 식별자 |
| `provider_email` | `varchar(320)` | X | 소셜 제공자가 전달한 이메일 |
| `last_login_at` | `timestamptz` | X | 해당 소셜 계정의 마지막 로그인 성공 시각 |
| `created_at` | `timestamptz` | O | 소셜 계정 연결 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 소셜 계정 식별 | `UNIQUE(provider, provider_user_id)` |
| 사용자 관계 | 사용자 1명에게 여러 소셜 계정 연결 가능 |
| 계정 연결 | 이메일이 같다는 이유만으로 자동 연결하지 않음 |
| 소셜 인증 | 외부 제공자에서 인증한 사용자 식별자를 기반으로 처리 |

## 2-4. **`roles` — 역할 정의**

사용자의 기본 역할과 그룹 내 역할을 정의하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `role_id` | `bigint` | O | 역할 PK |
| `code` | `varchar(50)` | O | 역할 코드, UNIQUE |
| `role_type` | `varchar(20)` | O | 역할 유형. `BASE`, `GROUP` |
| `description` | `text` | X | 역할 설명 |
| `created_at` | `timestamptz` | O | 역할 생성 시각 |
| `updated_at` | `timestamptz` | O | 역할 수정 시각 |

- 초기 역할 데이터

| **code** | **role_type** | **설명** |
| --- | --- | --- |
| `LAY` | `BASE` | 평신도 |
| `CLERGY` | `BASE` | 목회자. 전체 기도 게시글 관리 가능 |
| `CELL_LEADER` | `GROUP` | 지정된 셀의 리더 |
| `CELL_ASSISTANT` | `GROUP` | 지정된 셀의 부리더 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 역할 코드 | UNIQUE |
| 기본 역할 | `users.base_role_id`에서 참조 |
| 그룹 역할 | `user_roles.role_id`에서 참조 |
| 권한 관리 | 초기에는 Spring Security 및 백엔드 서비스에서 관리 |
| 역할 유형 | `BASE`, `GROUP`만 허용 |

## 2-5. **`user_roles` — 그룹 내 추가 역할**

셀 리더 및 부리더 역할과 담당 그룹을 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `user_role_id` | `bigint` | O | 역할 배정 PK |
| `user_id` | `bigint` | O | 사용자 FK → `users.user_id` |
| `role_id` | `bigint` | O | 역할 FK → `roles.role_id`. `GROUP` 유형만 허용 |
| `cell_id` | `bigint` | O | 담당 그룹 FK → `cell.cell_id` |
| `assigned_at` | `timestamptz` | O | 역할 시작 시각 |
| `ended_at` | `timestamptz` | X | 역할 종료 시각. `NULL`이면 현재 활성 역할 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 역할 유형 | `CELL_LEADER`, `CELL_ASSISTANT` |
| 셀 리더 | 셀마다 활성 리더 최대 1명 |
| 셀 부리더 | 셀마다 활성 부리더 최대 1명 |
| 중복 역할 | 동일 사용자는 동일 셀에서 리더·부리더 동시 수행 불가 |
| 역할 담당자 | 해당 셀에 소속된 평신도 |
| 역할 종료 | `ended_at` 기록 |
| 셀 이동 | 기존 셀의 활성 리더·부리더 역할 종료 |

## 2-6. **`clergy_profiles` — 목회자 상세 정보**

목회자의 직책과 담당 사역 영역을 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `user_id` | `bigint` | O | PK이자 FK → `users.user_id`. 자동 생성하지 않음 |
| `position` | `varchar(30)` | O | 목회자 직책 |
| `ministry_area` | `varchar(50)` | O | 담당 사역 영역 |
| `created_at` | `timestamptz` | O | 목회자 정보 생성 시각 |
| `updated_at` | `timestamptz` | O | 목회자 정보 수정 시각 |

- 목회자 직책 CODE

| **값** | **설명** |
| --- | --- |
| `SENIOR_PASTOR` | 담임목사 |
| `PASTOR` | 목사 |
| `EVANGELIST` | 전도사 |

- 담당 사역 영역 CODE

| **값** | **설명** |
| --- | --- |
| `CHURCH_WIDE` | 교회 전체 |
| `YOUTH` | 청년부 |
| `CHILDREN` | 아동부 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 사용자 관계 | 사용자당 최대 1개 |
| 기본 역할 | `CLERGY`인 사용자만 등록 가능 |
| 역할 변경 | 목회자 역할 변경 시 프로필 정보도 함께 처리 |

## 2-7. **`cell` — 새신자 그룹·셀**

새신자 그룹과 일반 셀의 정보를 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `cell_id` | `bigint` | O | 그룹 PK |
| `cell_type` | `varchar(30)` | O | 그룹 유형. `NEWCOMER`, `CELL` |
| `name` | `varchar(100)` | O | 그룹 이름 |
| `cell_category` | `varchar(1)` | △ | 셀 유형. `S`, `A`, `B`, `C` |
| `description` | `text` | X | 그룹 소개 |
| `status` | `varchar(20)` | O | 그룹 상태. `ACTIVE`, `INACTIVE` |
| `created_at` | `timestamptz` | O | 그룹 생성 시각 |
| `updated_at` | `timestamptz` | O | 그룹 정보 수정 시각 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 새신자 그룹 | `cell_type = 'NEWCOMER'` |
| 일반 셀 | `cell_type = 'CELL'` |
| 셀 카테고리 | 일반 셀은 `S`, `A`, `B`, `C` 중 하나 필수 |
| 새신자 그룹 카테고리 | `NULL` |
| 그룹명 | 중복 허용 |
| 구성원 조회 | `users.cell_id` |
| 리더·부리더 조회 | `user_roles` |

## 2-8. **`prayer_posts` — 기도 게시글**

사용자가 등록한 기도 제목과 내용, 공개 범위 및 기도 응답 상태를 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `prayer_id` | `bigint` | O | 기도 게시글 PK |
| `author_id` | `bigint` | O | 작성자 FK → `users.user_id` |
| `cell_id` | `bigint` | △ | 게시 대상 그룹 FK → `cell.cell_id`. `GROUP`, `GROUP_LEADER` 공개 시 필수 |
| `title` | `varchar(200)` | O | 기도 제목 |
| `content` | `text` | O | 기도 내용 |
| `visibility` | `varchar(20)` | O | 공개 범위. `PUBLIC`, `GROUP`, `GROUP_LEADER`, `PRIVATE` |
| `status` | `varchar(20)` | O | 기도 게시물 상태. 등록 시 `OPEN`, 작성자가 응답 완료 버튼을 누르면 `ANSWERED`, 삭제 버튼을 누르면 `DELETED` |
| `answered_at` | `timestamptz` | X | 작성자가 응답 완료 버튼을 눌러 상태를 변경한 시각 |
| `created_at` | `timestamptz` | O | 기도 게시글 작성 시각 |
| `updated_at` | `timestamptz` | O | 기도 게시글 수정 시각 |
| `deleted_at` | `timestamptz` | X | 작성자가 삭제 버튼을 눌러 상태를 변경한 시각 |

- 공개 범위(visibility)

| **값** | **공개 범위** | **조회 가능한 사용자** |
| --- | --- | --- |
| `PUBLIC` | 전체 공개 | 활성 사용자 전체 |
| `GROUP` | 그룹 공개 | 해당 그룹 구성원, 작성자, 목회자 |
| `GROUP_LEADER` | 그룹 리더 공개 | 해당 그룹 리더·부리더, 작성자, 목회자 |
| `PRIVATE` | 비공개 | 작성자, 목회자 |

- 상태 (status)

| **값** | **상태명** | **설명** | **변경 조건** |
| --- | --- | --- | --- |
| `OPEN` | 기도 중 | 기도 게시물을 최초 등록했을 때의 기본 상태 | 게시물 등록 시 자동 설정 |
| `ANSWERED` | 응답 완료 | 작성자가 기도 응답을 받았다고 표시한 상태 | 작성자가 **응답 완료** 버튼 클릭 |
| `DELETED` | 삭제 | 작성자가 기도 게시물을 삭제한 상태 | 작성자가 **삭제** 버튼 클릭 |

- 상태 변경에 따른 DB처리

| **사용자 동작** | **변경 필드** | **처리 내용** |
| --- | --- | --- |
| 기도 게시물 등록 | `status` | `OPEN`으로 설정 |
| 응답 완료 버튼 클릭 | `status`, `answered_at`, `updated_at` | `ANSWERED`로 변경하고 응답 완료 시각 기록 |
| 삭제 버튼 클릭 | `status`, `deleted_at`, `updated_at` | `DELETED`로 변경하고 삭제 시각 기록 |
| 게시물 수정 | `updated_at` | 수정 시각 갱신 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 공개 범위 | `PUBLIC`, `GROUP`, `GROUP_LEADER`, `PRIVATE` |
| 그룹 지정 | `GROUP`, `GROUP_LEADER` 공개 시 `cell_id` 필수 |
| 최초 상태 | `OPEN` |
| 응답 완료 처리 | 게시물 작성자만 가능 |
| 삭제 처리 | 게시물 작성자만 가능 |
| 삭제 방식 | Soft Delete |
| 삭제된 게시물 | 일반 목록 및 상세 조회에서 제외 |
| 그룹 이동 | 작성자가 다른 셀로 이동해도 게시물의 기존 `cell_id` 유지 |

## 2-9. **`prayer_comments` — 댓글**

기도 게시글에 작성한 댓글과 익명 여부를 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `comment_id` | `bigint` | O | 댓글 PK |
| `prayer_id` | `bigint` | O | 기도 게시글 FK → `prayer_posts.prayer_id` |
| `author_id` | `bigint` | O | 실제 작성자 FK → `users.user_id` |
| `content` | `text` | O | 댓글 내용 |
| `is_anonymous` | `boolean` | O | 익명 여부. 기본값 `false` |
| `created_at` | `timestamptz` | O | 댓글 작성 시각 |
| `updated_at` | `timestamptz` | O | 댓글 수정 시각 |
| `deleted_at` | `timestamptz` | X | 댓글 삭제 시각 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 댓글 작성 | 접근 권한이 있는 기도 게시글에만 작성 가능 |
| 익명 댓글 | `is_anonymous = true` |
| 작성자 정보 | 익명 댓글도 실제 작성자 ID 저장 |
| 익명 정보 노출 | 일반 조회 API에서 작성자 ID·이름·프로필 이미지 비노출 |
| 댓글 삭제 | Soft Delete |
| 삭제 여부 | `deleted_at IS NULL`이면 정상 |

## 2-10. **`prayer_reactions` — 기도 반응**

사용자가 기도 게시글에 표시한 ‘기도했습니다’ 반응을 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `prayer_id` | `bigint` | O | PK, FK → `prayer_posts.prayer_id` |
| `user_id` | `bigint` | O | PK, FK → `users.user_id` |
| `created_at` | `timestamptz` | O | 반응 등록 시각 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| PK | `(prayer_id, user_id)` 복합 PK |
| 반응 등록 | 사용자가 ‘기도했습니다’ 버튼 클릭 시 행 생성 |
| 중복 반응 | 사용자당 게시글별 최대 1회 |
| 반응 취소 | 해당 행 삭제 |
| 접근 권한 | 조회 권한이 있는 기도 게시글에만 반응 가능 |

## 2-11. **`refresh_tokens` — Refresh Token 관리**

Access Token 재발급 및 사용자 로그인 세션을 관리하는 테이블

| **필드명** | **데이터 타입** | **필수** | **설명** |
| --- | --- | --- | --- |
| `refresh_token_id` | `bigint` | O | Refresh Token PK |
| `user_id` | `bigint` | O | 사용자 FK → `users.user_id` |
| `token_hash` | `varchar(64)` | O | Refresh Token의 SHA-256 해시값, UNIQUE |
| `token_family_id` | `uuid` | O | Rotation 시 동일 로그인 세션을 식별하는 UUID |
| `expires_at` | `timestamptz` | O | Refresh Token 만료 시각 |
| `revoked_at` | `timestamptz` | X | 토큰 무효화 시각. `NULL`이면 별도로 무효화되지 않은 상태 |
| `last_used_at` | `timestamptz` | X | 마지막 토큰 사용 시각 |
| `created_at` | `timestamptz` | O | 토큰 발급 시각 |

- 제약조건 및 정책

| **항목** | **설명** |
| --- | --- |
| 사용자 관계 | 사용자 1명에게 여러 Refresh Token 허용 |
| 토큰 저장 | 원본 저장 금지, SHA-256 해시 저장 |
| 토큰 해시 | UNIQUE |
| 유효기간 | 초기 권장값 14일 |
| 토큰 재발급 | Refresh Token Rotation 적용 |
| 기존 토큰 | 재발급 성공 시 무효화 |
| 로그아웃 | 해당 로그인 세션의 토큰 무효화 |
| 전체 로그아웃 | 사용자의 모든 Refresh Token 무효화 |
| 만료 토큰 | 배치 작업을 통해 정리 |
| 재사용 탐지 | 무효화된 토큰 재사용 시 동일 Token Family 무효화 정책 적용 |
| 트랜잭션 | 기존 토큰 무효화 및 신규 토큰 생성은 원자적으로 처리 |

# 3. 테이블 관계 정의서

| **번호** | **부모 테이블** | **자식 테이블** | **관계** | **FK** |
| --- | --- | --- | --- | --- |
| 1 | `users` | `auth_credentials` | 1:0..1 | `user_id` |
| 2 | `users` | `social_accounts` | 1:N | `user_id` |
| 3 | `users` | `refresh_tokens` | 1:N | `user_id` |
| 4 | `roles` | `users` | 1:N | `base_role_id` |
| 5 | `roles` | `user_roles` | 1:N | `role_id` |
| 6 | `users` | `user_roles` | 1:N | `user_id` |
| 7 | `cell` | `user_roles` | 1:N | `cell_id` |
| 8 | `cell` | `users` | 1:N | `cell_id` |
| 9 | `users` | `clergy_profiles` | 1:0..1 | `user_id` |
| 10 | `users` | `prayer_posts` | 1:N | `author_id` |
| 11 | `cell` | `prayer_posts` | 1:N | `cell_id` |
| 12 | `prayer_posts` | `prayer_comments` | 1:N | `prayer_id` |
| 13 | `users` | `prayer_comments` | 1:N | `author_id` |
| 14 | `prayer_posts` | `prayer_reactions` | 1:N | `prayer_id` |
| 15 | `users` | `prayer_reactions` | 1:N | `user_id` |

# 4. 인증 데이터 흐름

## 일반 로그인

1. `auth_credentials`에서 로그인 ID와 잠금 상태를 확인한다.
2. `PasswordEncoder.matches()`로 비밀번호를 검증한다.
3. 연결된 `users`의 상태를 확인하고 로그인 허용 여부를 판단한다.
4. 서비스 Access Token과 Refresh Token을 발급한다.
5. Refresh Token의 해시를 `refresh_tokens`에 저장한다.

소셜 전용 사용자는 `auth_credentials` 행 없이 운영할 수 있다. 일반 로그인을 추가하면 기존 사용자 ID에 인증 행을 연결한다.

## 소셜 로그인

1. 제공자의 Authorization Code를 백엔드가 토큰으로 교환한다.
2. 제공자의 검증된 인증 결과에서 사용자 식별자를 확인한다.
3. `(provider, provider_user_id)`로 `social_accounts`를 조회하고 연결된 `users`를 확인한다.
4. 사용자 상태를 확인한 뒤 우리 서비스의 토큰을 발급하고 Refresh Token 해시를 저장한다.

우리 서버는 소셜 사용자의 비밀번호를 저장하거나 검증하지 않는다. 클라이언트가 단순 제출한 식별자를 인증 증거로 신뢰하지 않는다.
제공자의 토큰과 우리 서비스의 토큰은 구분한다. 처음 연결하는 소셜 계정의 가입·연결 절차는 별도 구현이 필요하다.

## Access Token 재발급

- Access Token은 클라이언트 메모리, Refresh Token은 클라이언트 HttpOnly 쿠키에 보관한다.
- 서버의 인증 정책은 `STATELESS`이며 Refresh Token 해시는 PostgreSQL에 저장한다.
- 재발급 시 `auth_credentials`나 `social_accounts`를 다시 조회해 인증하지 않는다.
- Refresh Token의 해시, 만료 시각, 무효화 여부와 사용자 상태를 확인한다. `ACTIVE` 사용자만 재발급한다.
- 기존 토큰 무효화와 같은 `token_family_id`의 신규 토큰 저장을 하나의 트랜잭션으로 처리한다.
- 새 Access Token을 반환하고 새 Refresh Token으로 쿠키를 교체한다.
- 행 잠금 등으로 동시 요청을 제어해 동일 토큰에서 여러 유효한 후속 토큰이 발급되지 않게 한다.
- 무효화된 토큰 재사용 시 같은 Token Family를 무효화한다. 정상 중복 요청과 네트워크 재시도에 대한 처리 방침은 구현 전에 정한다.

# 5. 추가 결정 및 구현 검토 항목

| 항목 | 현재 상태 / 확인할 내용 |
| --- | --- |
| Refresh Token 유효기간 | 14일은 권장값. 최종 유효기간과 회전 시 만료 연장 여부는 미결정 |
| Access Token | 형식·서명 방식·유효기간 미결정. 역할을 포함한다면 재발급 시 현재 권한 반영 필요 |
| 로그아웃 의미 | Refresh Token 무효화로 재발급을 차단한다. 기존 Access Token의 즉시 차단 여부·방법은 별도 결정 |
| 비밀번호 변경 | 기존 세션 전체 무효화 여부 미결정 |
| 소셜 연결 해제 | 인증 수단별 세션 참조가 없어 해당 수단으로 로그인한 세션만 골라 폐기할 수 없음. 관련 기능 도입 여부 미결정 |
| 마지막 인증 수단 해제 | 계정 접근 수단을 잃지 않도록 해제 허용 조건 결정 필요 |
| 재사용 탐지와 정리 | 폐기 토큰을 즉시 삭제하면 재사용 식별이 어려워지므로 탐지에 필요한 이력 보존 기간과 배치 정리 기준 결정 필요 |
| 쿠키·브라우저 정책 | 실제 배포 도메인에 맞춘 Secure·SameSite·Path·Domain, CORS·CSRF 정책 결정 필요 |
| 가입 | 필수 정보 완료 후 users 행 생성. 제공자가 주지 않는 필드의 입력과 셀 배정 절차 필요 |
| 역할·소속 제약 | 역할 유형, 소속된 평신도 여부 등 다른 테이블을 참조하는 규칙의 강제 방법 검토 필요 |
| 활성 역할 중복 | 활성 리더·부리더 정원 및 동일 셀 내 역할 중복 금지를 구현할 제약 검토 필요 |
| 목회자 정보 | CLERGY 사용자와 clergy_profiles의 생성·변경을 일관되게 처리할 방법 결정 필요 |
| Soft Delete | 상태와 deleted_at의 일관성, 삭제된 사용자 이메일·로그인 ID 재사용 여부 결정 필요 |
| 관리자 권한 | 기도글 삭제 권한 충돌 해결, 익명 작성자 관리자 열람·감사 기록 정책 결정 필요 |
| 누락된 기능 명세 | 달란트 거래와 첨부파일 메타데이터의 테이블 및 제약 별도 설계 필요 |

이 문서를 바탕으로 FK·UNIQUE·CHECK·인덱스·트랜잭션 경계를 구체화한 뒤 Flyway SQL을 작성한다.
