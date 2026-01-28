# 프로필 API 가이드

## 개요

소셜 로그인 후 프로필 완성 및 마이페이지에서 프로필을 관리하는 API입니다.

## 사용자 상태 흐름

```
소셜 로그인 → PENDING 상태 → 프로필 완성 → ACTIVE 상태
```

| 상태 | 설명 |
|------|------|
| `PENDING` | 소셜 로그인 완료, 프로필 미완성 |
| `ACTIVE` | 프로필 완성, 서비스 이용 가능 |

---

## API 목록

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| `POST` | `/api/v1/users/profile/complete` | 프로필 완성 | Required |
| `GET` | `/api/v1/users/info` | 프로필 조회 | Required |
| `PATCH` | `/api/v1/users/profile` | 프로필 수정 | Required |
| `DELETE` | `/api/v1/users/profile/image` | 프로필 이미지 삭제 | Required |
| `DELETE` | `/api/v1/users/profile/background` | 배경 이미지 삭제 | Required |
| `DELETE` | `/api/v1/users` | 회원 탈퇴 | Required |

---

## 1. 프로필 완성

소셜 로그인 후 최초 1회 프로필 정보를 입력합니다.

### Request

```http
POST /api/v1/users/profile/complete
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

#### Form Data

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `profile` | JSON | O | 프로필 정보 |
| `profileImage` | File | X | 프로필 이미지 |
| `backgroundImage` | File | X | 배경 이미지 |

#### profile JSON 구조

```json
{
  "nickname": "개발자준현",
  "introduction": "백엔드 개발자입니다"
}
```

| 필드 | 타입 | 필수 | 제약 조건 |
|------|------|------|-----------|
| `nickname` | String | O | 2~50자, 중복 불가 |
| `introduction` | String | X | 최대 300자 |

### Response

#### 성공 (200 OK)

```json
{
  "code": 200,
  "message": "프로필이 완성되었습니다.",
  "data": {
    "userId": 1,
    "nickname": "개발자준현",
    "introduction": "백엔드 개발자입니다",
    "profileImageUrl": "https://storage.example.com/profile/1.jpg",
    "backgroundImageUrl": "https://storage.example.com/background/1.jpg",
    "status": "ACTIVE"
  }
}
```

#### 실패

| 상태 | 코드 | 메시지 |
|------|------|--------|
| 400 | USR005 | 이미 프로필이 완성된 사용자입니다 |
| 401 | ATH001 | 인증이 필요합니다 |
| 409 | USR004 | 이미 사용 중인 닉네임입니다 |

---

## 2. 프로필 조회

로그인한 사용자의 프로필 정보를 조회합니다.

### Request

```http
GET /api/v1/users/info
Authorization: Bearer {accessToken}
```

### Response

#### 성공 (200 OK)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "nickname": "개발자준현",
    "introduction": "백엔드 개발자입니다",
    "profileImageUrl": "https://storage.example.com/profile/1.jpg",
    "backgroundImageUrl": "https://storage.example.com/background/1.jpg"
  }
}
```

---

## 3. 프로필 수정

프로필 정보 및 이미지를 수정합니다.

### Request

```http
PATCH /api/v1/users/profile
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

#### Form Data

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `profile` | JSON | O | 수정할 프로필 정보 |
| `profileImage` | File | X | 새 프로필 이미지 (기존 이미지 교체) |
| `backgroundImage` | File | X | 새 배경 이미지 (기존 이미지 교체) |

### Response

#### 성공 (200 OK)

```json
{
  "code": 200,
  "message": "프로필이 수정되었습니다.",
  "data": {
    "userId": 1,
    "nickname": "새닉네임",
    "introduction": "수정된 소개글",
    "profileImageUrl": "https://storage.example.com/profile/2.jpg",
    "backgroundImageUrl": "https://storage.example.com/background/2.jpg",
    "status": "ACTIVE"
  }
}
```

---

## 4. 프로필 이미지 삭제

프로필 이미지만 삭제합니다.

### Request

```http
DELETE /api/v1/users/profile/image
Authorization: Bearer {accessToken}
```

### Response

#### 성공 (204 No Content)

```json
{
  "code": 204,
  "message": "success",
  "data": null
}
```

---

## 5. 배경 이미지 삭제

배경 이미지만 삭제합니다.

### Request

```http
DELETE /api/v1/users/profile/background
Authorization: Bearer {accessToken}
```

### Response

#### 성공 (204 No Content)

```json
{
  "code": 204,
  "message": "success",
  "data": null
}
```

---

## 6. 회원 탈퇴

로그인한 사용자의 계정을 삭제합니다.

### Request

```http
DELETE /api/v1/users
Authorization: Bearer {accessToken}
```

### Response

#### 성공 (204 No Content)

```json
{
  "code": 204,
  "message": "success",
  "data": null
}
```

---

## 에러 코드

| HTTP 상태 | 코드 | 메시지 | 설명 |
|-----------|------|--------|------|
| 400 | USR003 | 잘못된 비밀번호입니다 | 비밀번호 불일치 |
| 400 | USR005 | 이미 프로필이 완성된 사용자입니다 | PENDING 상태가 아닐 때 프로필 완성 시도 |
| 401 | ATH001 | 인증이 필요합니다 | 토큰 없음 |
| 401 | ATH002 | 유효하지 않은 토큰입니다 | 토큰 검증 실패 |
| 403 | USR006 | 프로필 완성이 필요합니다 | PENDING 상태로 서비스 접근 시도 |
| 404 | USR001 | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 |
| 409 | USR004 | 이미 사용 중인 닉네임입니다 | 닉네임 중복 |
| 409 | USR007 | 이미 사용 중인 아이디입니다 | 아이디 중복 |

---

## 파일 구조

```
src/main/java/swyp12/team9/server/
├── api/user/
│   ├── UserApi.java                    # API 스펙 인터페이스 (Swagger)
│   ├── UserController.java             # 컨트롤러 구현체
│   └── dto/
│       ├── request/
│       │   └── ProfileCompleteRequest.java
│       └── response/
│           ├── ProfileCompleteResponse.java
│           └── ProfileResponse.java
├── domain/user/
│   ├── exception/
│   │   ├── NicknameDuplicateException.java
│   │   ├── ProfileAlreadyCompletedException.java
│   │   └── UsernameDuplicateException.java
│   ├── model/
│   │   ├── User.java
│   │   └── UserStatus.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       ├── ProfileService.java
│       └── UserService.java
└── global/
    ├── exception/
    │   └── ErrorCode.java
    └── handler/
        └── SocialSuccessHandler.java
```

---

## 시퀀스 다이어그램

### 프로필 완성 흐름

```
Client                    Server                    Storage
  |                          |                          |
  |--- POST /profile/complete -->|                      |
  |   (profile + images)     |                          |
  |                          |--- Check PENDING status  |
  |                          |--- Check nickname dup    |
  |                          |--- Upload images ------->|
  |                          |<-- Image URLs -----------|
  |                          |--- Update User (ACTIVE)  |
  |<-- 200 OK --------------|                          |
```

### 프로필 수정 흐름

```
Client                    Server                    Storage
  |                          |                          |
  |--- PATCH /profile ------>|                          |
  |   (profile + new images) |                          |
  |                          |--- Check nickname (skip self)|
  |                          |--- Delete old images --->|
  |                          |--- Upload new images --->|
  |                          |<-- New Image URLs -------|
  |                          |--- Update User           |
  |<-- 200 OK --------------|                          |
```