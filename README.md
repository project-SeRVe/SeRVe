# SeRVe Backend

**Secure Collaborative Repository with Envelope Encryption & Federated Model**

Spring Boot 기반의 Zero-Trust 협업 저장소 백엔드 서버입니다.

## 핵심 기능

### 1. Envelope Encryption (엔벨로프 암호화)
- **DEK (Data Encryption Key)**: 문서별 대칭키로 실제 데이터 암호화
- **KEK (Key Encryption Key)**: 팀 키로 DEK를 래핑하여 보호
- **사용자별 암호화**: 각 멤버의 공개키로 팀 키를 암호화하여 저장
- **Zero-Trust**: 서버는 평문 데이터나 개인키를 절대 보지 못함

### 2. Federated Model (역할 기반 권한 분리)
- **ADMIN (Key Master)**:
  - 저장소 생성자 (Creator)
  - 멤버 초대/강퇴 권한
  - **메타데이터만 조회 가능** (암호화된 데이터 접근 불가)
  - 보안 강화: 관리자도 실제 데이터를 복호화할 수 없음

- **MEMBER (Data Owner)**:
  - **데이터 업로드/동기화 권한**
  - 암호화된 팀 키로 DEK 복호화 가능
  - 실제 데이터 읽기/쓰기 가능

### 3. 청크 기반 증분 동기화
- 문서를 청크 단위로 분할하여 저장
- 버전 관리 (`@Version`) 자동 증가
- `lastVersion` 기반 증분 동기화 (변경된 청크만 전송)
- 네트워크 효율성 및 충돌 방지

---

## 데이터베이스 구조

### Entity 설명

#### 1. `User` (users 테이블)
사용자 계정 정보
```java
- userId (PK)              : UUID 기반 사용자 ID
- email (UNIQUE)           : 로그인용 이메일
- hashedPassword           : BCrypt 해시 비밀번호
- publicKey                : RSA/ECIES 공개키 (서버 저장)
- encryptedPrivateKey      : 암호화된 개인키 (사용자 비밀번호로 암호화, 서버 복호화 불가)
```

**보안 특징**:
- 개인키는 사용자 비밀번호로 암호화되어 서버에 저장됨
- 서버는 개인키를 복호화할 수 없음 (Zero-Trust)

#### 2. `Team` (teams 테이블)
저장소/팀 정보
```java
- teamId (PK)              : UUID 기반 팀 ID
- name                     : 저장소 이름
- description              : 설명
- ownerId                  : 생성자 User ID
- type                     : TEAM | PRIVATE
- createdAt                : 생성 일시
```

#### 3. `RepositoryMember` (repository_members 테이블)
팀 멤버십 (M:N 관계)
```java
- id (복합키)              : {teamId, userId}
- role                     : ADMIN | MEMBER
- encryptedTeamKey         : 멤버별로 암호화된 팀 키 (사용자 공개키로 암호화)
```

**Envelope Encryption 핵심**:
- 각 멤버가 자신의 공개키로 암호화된 팀 키를 받음
- 자신의 개인키로만 팀 키 복호화 가능
- 팀 키로 DEK를 복호화하여 실제 데이터 접근

#### 4. `Document` (documents 테이블)
문서 메타데이터
```java
- id (PK, AUTO_INCREMENT)  : DB 내부 ID
- documentId (UNIQUE)      : UUID 기반 외부 식별자
- teamId (FK)              : 소속 팀
- uploaderId (FK)          : 업로더 User ID
- originalFileName         : 원본 파일명
- encryptedDEK             : 암호화된 DEK (팀 키로 래핑)
- uploadedAt               : 업로드 일시
```

**Envelope Encryption**:
- `encryptedDEK`: 문서별 DEK를 팀 키로 암호화하여 저장
- 실제 청크 데이터는 DEK로 암호화됨

#### 5. `VectorChunk` (vector_chunks 테이블)
청크 단위 암호화 데이터 저장
```java
- chunkId (PK)             : UUID
- documentId               : 문서 ID
- teamId                   : 팀 ID (빠른 조회용)
- chunkIndex               : 청크 순서 (0부터 시작)
- encryptedBlob            : 암호화된 청크 데이터 (LONGBLOB)
- version                  : 낙관적 락 버전 (@Version 자동 증가)
- isDeleted                : 삭제 플래그 (Soft Delete)
- createdAt, updatedAt     : 생성/수정 일시
```

**인덱스**:
- `idx_document_chunk`: (document_id, chunk_index) - 문서별 청크 조회
- `idx_team_version`: (team_id, version) - 증분 동기화
- `idx_document_deleted`: (document_id, is_deleted) - 활성 청크 필터링

**버전 관리**:
- `@Version` 자동 증가 (JPA Optimistic Locking)
- 청크 수정/삭제 시마다 version++
- 클라이언트는 `lastVersion` 이후 변경된 청크만 가져옴

#### 6. `EncryptedData` (encrypted_data 테이블)
문서별 암호화 데이터 (레거시, VectorChunk로 대체됨)
```java
- dataId (PK)              : UUID
- documentId (FK)          : Document와 1:1 관계
- encryptedBlob            : 암호화된 데이터
- version                  : 버전 관리
```

#### 7. `EdgeNode` (edge_nodes 테이블)
엣지 서버/로봇 디바이스 정보
```java
- nodeId (PK)              : UUID
- ownerId (FK)             : 소유자 User ID
- certificateData          : X.509 인증서 (인증용)
- registeredAt             : 등록 일시
```

---

## API 엔드포인트

### 인증 (AuthController)
- `POST /auth/signup` - 회원가입 (공개키 등록)
- `POST /auth/login` - 로그인 (JWT 발급)
- `POST /auth/robot/login` - 로봇 인증서 기반 로그인
- `POST /auth/reset-password` - 비밀번호 재설정
- `GET /auth/public-key` - 사용자 공개키 조회
- `DELETE /auth/me` - 계정 삭제

### 저장소 관리 (RepoController)
- `POST /api/repositories` - 저장소 생성 (생성자는 자동으로 ADMIN)
- `GET /api/repositories` - 내 저장소 목록 조회
- `GET /api/repositories/{teamId}/keys` - 암호화된 팀 키 조회
- `DELETE /api/repositories/{teamId}` - 저장소 삭제 (ADMIN만)

### 멤버 관리 (MemberController)
- `POST /api/teams/{teamId}/members` - 멤버 초대 (ADMIN만)
- `GET /api/teams/{teamId}/members` - 멤버 목록 조회
- `DELETE /api/teams/{teamId}/members/{userId}` - 멤버 강퇴 (ADMIN만)
- `PUT /api/teams/{teamId}/members/{userId}` - 역할 변경 (ADMIN만)
- `POST /api/teams/{teamId}/members/rotate-keys` - 키 로테이션 (ADMIN만)

### 문서 관리 (DocumentController)
- `POST /api/teams/{teamId}/documents` - 문서 업로드 (MEMBER만)
- `GET /api/teams/{teamId}/documents` - 문서 목록 조회 (메타데이터)
- `DELETE /api/teams/{teamId}/documents/{docId}` - 문서 삭제
- `POST /api/teams/{teamId}/documents/reencrypt-keys` - DEK 재암호화

### 청크 업로드/동기화 (ChunkController)
- `POST /api/teams/{teamId}/chunks` - 청크 배치 업로드 (MEMBER만)
- `DELETE /api/teams/{teamId}/chunks/{chunkIndex}` - 청크 삭제
- `GET /api/sync/chunks?teamId={teamId}&lastVersion={version}` - 증분 동기화

**Federated Model 권한 체크**:
- ChunkService.uploadChunks()에서 ADMIN 업로드 차단:
  ```java
  if (member.getRole() == Role.ADMIN) {
      throw new SecurityException("ADMIN은 데이터 업로드가 불가능합니다. MEMBER만 업로드할 수 있습니다.");
  }
  ```

### 동기화 (SyncController)
- `GET /api/sync/documents?teamId={teamId}&lastVersion={version}` - 변경된 문서 동기화

### 보안 (SecurityController)
- `POST /api/security/handshake` - 클라이언트 공개키 등록

### 엣지 노드 (EdgeNodeController)
- `POST /edge-nodes/register` - 엣지 노드 등록
- `GET /edge-nodes/{nodeId}/team-key` - 엣지 노드용 팀 키 조회

---

## 주요 클래스 설명

### Service Layer

#### 1. `ChunkService`
청크 기반 데이터 업로드/동기화 핵심 로직

**주요 메서드**:
- `uploadChunks()`: 청크 배치 업로드
  - **ADMIN 차단**: MEMBER만 업로드 가능
  - Document 찾기/생성
  - encryptedDEK 저장 (Envelope Encryption)
  - VectorChunk 저장 (UPDATE or INSERT)

- `syncChunks()`: 증분 동기화
  - `lastVersion` 이후 변경된 청크만 반환
  - **ADMIN 차단**: MEMBER만 동기화 가능
  - 버전 기반 효율적 전송

**Rate Limiting**:
- 악의적 대량 업로드 방지
- RateLimitService 연동

#### 2. `RepoService`
저장소 생성/관리

**주요 메서드**:
- `createRepository()`: 저장소 생성
  - 팀 키(DEK) 생성
  - 생성자를 **ADMIN**으로 자동 등록
  - 생성자 공개키로 팀 키 암호화하여 저장

#### 3. `MemberService`
멤버 초대/강퇴

**주요 메서드**:
- `inviteMember()`: 멤버 초대
  - 초대자 공개키로 팀 키 암호화
  - RepositoryMember 생성 (기본: MEMBER)

- `kickMember()`: 멤버 강퇴
  - ADMIN만 실행 가능
  - 마지막 ADMIN 강퇴 방지

#### 4. `AuthService`
사용자 인증/회원가입

**주요 메서드**:
- `signup()`: 회원가입
  - 공개키/암호화된 개인키 저장
  - 비밀번호 BCrypt 해싱

- `login()`: 로그인
  - JWT 토큰 발급
  - UserDetails 연동

#### 5. `SyncService`
문서/청크 동기화 조율

### Security Layer

#### 1. `JwtTokenProvider`
JWT 토큰 생성/검증

#### 2. `JwtAuthenticationFilter`
JWT 기반 인증 필터 (Spring Security)

#### 3. `CryptoManager`
암호화 유틸리티 (Google Tink 연동)

#### 4. `KeyExchangeService`
키 교환 프로토콜 (Diffie-Hellman 등)

---

## 실행 방법

### 사전 요구사항
- Java 17+
- MariaDB 또는 H2 (개발용)

### 1. 데이터베이스 설정 (Docker)

```bash
cd SeRVe-Backend
docker compose up -d
```

**접속 정보**:
- Host: localhost:3306
- Database: serve_db
- User: serve_user
- Password: serve_pass

### 2. 초기 설정 (처음 실행 시)

**중요**: Git clone 후 반드시 설정 파일을 생성해야 합니다!

```bash
# 1. resources 디렉토리 생성
mkdir -p src/main/resources

# 2. 설정 파일 복사
cp application.properties.example src/main/resources/application.properties

# 또는 직접 복사
cp application.properties src/main/resources/
```

### 3. 애플리케이션 실행

```bash
# 빌드 및 테스트
./gradlew build

# 서버 실행
./gradlew bootRun
```

**접속**:
- Backend API: http://localhost:8080
- Actuator Health: http://localhost:8080/actuator/health

### 4. 설정 파일

`application.properties` (프로젝트 루트):
```properties
# 데이터베이스
spring.datasource.url=jdbc:mariadb://localhost:3306/serve_db
spring.datasource.username=serve_user
spring.datasource.password=serve_pass

# JWT
jwt.secret=your-secret-key-here
jwt.expiration=86400000

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## 보안 고려사항

### 1. Zero-Trust Architecture
- 서버는 평문 데이터나 개인키를 절대 보지 못함
- 모든 데이터는 클라이언트에서 암호화된 후 전송
- 서버는 암호문만 저장 (Envelope Encryption)

### 2. Federated Model
- **ADMIN**: Key Master 역할만 (멤버 관리, 메타데이터 조회)
- **MEMBER**: 실제 데이터 소유자 (업로드/동기화 권한)
- 관리자도 데이터를 복호화할 수 없어 보안 강화

### 3. 키 관리
- **사용자 공개키**: 서버 저장 (암호화용)
- **사용자 개인키**: 사용자 비밀번호로 암호화되어 저장 (서버 복호화 불가)
- **팀 키 (KEK)**: 각 멤버 공개키로 암호화하여 RepositoryMember에 저장
- **문서 키 (DEK)**: 팀 키로 암호화하여 Document에 저장

### 4. Rate Limiting
- 악의적 대량 업로드 방지
- IP 기반 요청 제한
- RateLimitService 자동 체크

---

## 테스트

```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests "horizon.SeRVe.service.RepoServiceTest"

# 특정 테스트 메서드
./gradlew test --tests "horizon.SeRVe.service.ChunkServiceTest.testAdminCannotUpload"
```

---

## 기술 스택

- **Framework**: Spring Boot 3.4.0
- **Language**: Java 17
- **Build**: Gradle 8.x
- **Database**: MariaDB (prod) / H2 (dev)
- **Security**: Spring Security + JWT (JJWT 0.11.5)
- **Encryption**: Google Tink 1.15.0
- **ORM**: Spring Data JPA (Hibernate)

---

## 라이센스

MIT License
