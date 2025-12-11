# JWT 서명키 기반 인증 프로젝트

이 프로젝트는 JWT 서명키(secret)가 유출되었을 때 어떤 보안 문제가 발생하는지
Spring Boot 환경에서 실제로 재현하기 위한 데모 애플리케이션입니다.

HS256 기반 JWT는 서버가 가진 대칭키(secret) 만 알고 있으면 누구나
임의 사용자로 가장할 수 있는 위조 토큰을 생성할 수 있습니다.
이 프로젝트는 그 취약 구조를 직접 실험해볼 수 있도록 구성되어 있습니다.

# 주요 기능
- Spring Security + JWT 기반 로그인 / 인증 처리
- HS256(대칭키) 기반 Access Token 발급 및 검증
- sub = userId 구조를 통한 단순한 인증 매핑
- Node.js로 생성한 위조 토큰도 정상 토큰처럼 인증됨을 재현
- /user/me API 호출을 통해 인증 결과 확인 가능

# 실행 방법
http/example.http

***VSCode의 경우 Rest Client extension 필요***
### 로그인 요청
```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "user1",
  "password": "password1"
}
```

### 보호 API 호출
```
@token = eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NjU0MzE0OTgsImV4cCI6MTc2NTQzNTA5OH0.hWXAU_gPSHVLiztetG0HtaKSKCUC_W8RRMQPbA0ThjM

GET http://localhost:8080/user/me
Authorization: Bearer {token}
```

### 위조 토큰 생성 방법
```
cd generate-token
npm install
node generate-token.js
```

# 주의
이 예제는 보안 취약한 구조를 재현하기 위해 의도적으로 단순화되어 있습니다.
실제 서비스에서는 다음을 반드시 고려해야 합니다:
- 비대칭키(RS256) 기반 전환
- 키 로테이션 및 KMS/Vault 적용
- 관리자 권한 별도 검증
- 토큰 스코프 제한
- 이상 행위 탐지 및 토큰 블랙리스트 관리
