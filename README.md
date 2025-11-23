# SeRVe

간단 설명

- Spring Boot + Gradle 기반의 Java 웹 애플리케이션입니다.
- Java 17과 Gradle Wrapper(`gradlew.bat`)을 사용합니다.

빠른 시작

필수 사전 준비:
- JDK 17 설치
- (선택) Git, GitHub CLI(`gh`) 설치

로컬 빌드 및 실행 (PowerShell):

```powershell
cd C:\Users\user\Desktop\SeRVe\SeRVe
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

테스트 실행:

```powershell
.\gradlew.bat test
```

프로젝트 구조 (주요 패키지와 역할)

아래는 이 프로젝트의 핵심 패키지 구조와 설명입니다. DTO는 나중에 구현될 예정이며, 각 기능에 필요한 DTO 타입은 dto 하위에 정리될 것입니다.

├── config                  # Security(JWT), Swagger, CORS 설정
├── controller              # [API 진입점]
│   ├── AuthController.java
│   ├── RepoController.java        # /repositories 관련 (생성, 조회, 이름변경)
│   ├── MemberController.java      # /repositories/{id}/members 관련 (초대, 강퇴)
│   └── DocumentController.java    # /documents 관련
├── service                 # [비즈니스 로직] (트랜잭션 관리)
│   ├── AuthService.java
│   ├── RepoService.java           # 팀 저장소 생성 시 Owner를 Member로 등록하는 로직 포함
│   ├── MemberService.java         # 초대 시 암호화된 키 처리 로직
│   └── DocumentService.java
├── repository              # [DB 접근 계층] (Spring Data JPA)
│   ├── UserRepository.java
│   ├── TeamRepoRepository.java    # 도메인 Repository 엔티티 관리
│   ├── MemberRepository.java
│   ├── DocumentRepository.java
│   └── EncryptedDataRepository.java
├── entity                  # [DB 테이블 매핑]
│   ├── User.java
│   ├── TeamRepository.java        # (테이블: repositories) *중요
│   ├── RepositoryMember.java      # (테이블: repository_members, 복합키)
│   ├── RepositoryMemberId.java
│   ├── Document.java
│   └── EncryptedData.java
└── dto                     # [데이터 전송 객체]
    ├── auth
    ├── repo                # CreateRepoRequest, RepoResponse 등 (추후 구현)
    ├── member              # InviteMemberRequest 등 (추후 구현)
    └── document            # Document 관련 DTO들 (추후 구현)

DTO 관련 안내

- 현재 `dto` 패키지는 설계상 포함되어 있으며, 실제 DTO 클래스들은 향후 구현될 예정입니다.
- 예시로 다음과 같은 DTO를 만들 계획입니다:
  - `dto.repo.CreateRepoRequest` (저장소 생성 요청)
  - `dto.repo.RepoResponse` (저장소 응답 데이터)
  - `dto.member.InviteMemberRequest` (회원 초대 요청)
  - `dto.auth.*` (로그인/회원가입 관련 요청/응답 DTO)
  - `dto.document.*` (문서 생성/조회 관련 DTO)

민감정보 및 로컬 설정

- `src/main/resources/application.properties` 또는 `application.yml`에 민감한 값(DB 비밀번호, API 키 등)을 직접 커밋하지 마세요.
- 로컬 설정은 `.gitignore`에 추가되어 있으며, CI/배포 시에는 GitHub Secrets나 환경 변수를 사용하세요.

추가 문서

- 개발 규칙, 도움말 및 스크립트는 `HELP.md` 또는 `CONTRIBUTING.md` (존재 시)를 참고하세요.

라이선스

- 이 저장소의 라이선스는 루트의 `LICENSE` 파일을 따릅니다.

문의

- 프로젝트 관련 문의는 이 저장소의 이슈 트래커를 이용해 주세요.
