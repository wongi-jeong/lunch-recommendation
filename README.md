# 🍱 lunch-recommendation

[![CI](https://github.com/wongi-jeong/lunch-recommendation/actions/workflows/ci.yml/badge.svg)](https://github.com/wongi-jeong/lunch-recommendation/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?logo=vite&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)

사용자의 **위치와 선호도**를 기반으로 점심 메뉴를 추천하는 풀스택 웹 애플리케이션입니다.
Google Places로 주변 식당을 찾고, 필터·룰렛·투표·즐겨찾기로 "오늘 뭐 먹지?"를 함께 정합니다.

> 🇯🇵 日本語版: [README.ja.md](README.ja.md) · 팀 프로젝트 (개발 2 · 디자인 2 · 기획/리서치 공동, 역할은 아래 [기여](#기여-팀-프로젝트) 참고)

## 기획 · 리서치

무작정 코딩부터 시작하지 않고, **문제 정의 → 사용자 설문 → 페르소나 → 니즈 우선순위 → IA 설계**를 거쳐 개발에 착수했습니다.

- **문제 정의** — "매일 점심 메뉴 고르기의 번거로움"
- **사용자 설문 (응답 47명)** — 연차와 무관하게 메뉴 선택에 어려움을 겪음 / 약 **60%**가 회사 근처에서 점심 해결 / 결정 방식은 *동료와 의논(64%) · 근처 단골(36%) · 즉흥 결정(32%)* → 각각 **투표 · 즐겨찾기 · 랜덤 추천** 기능으로 연결
- **페르소나 4종** — 실용적 1~3년차 직장인 / 고민 많은 신입사원 / 루틴 중시형 시니어 / 팀워크 중심 베테랑
- **니즈 우선순위 → 기능 정의** — 설문에서 도출한 니즈를 순위화해 우선순위가 높은 것부터 구현
  - 구현: 팀원 투표·다수결, 랜덤 추천, 즐겨찾기, 카테고리 필터
  - 디자인/기획 단계: 가격대 필터, 취향 기반 개인화
  - 범위 제외: 웨이팅·혼잡도, 예약·바로주문
- **IA(정보구조)** 설계로 화면 흐름을 확정한 뒤 디자인 → 개발로 이어감

| IA | 페르소나 | 설문 결과 | 니즈 우선순위 |
|---|---|---|---|
| ![IA](docs/research-ia.png) | ![페르소나](docs/research-persona.png) | ![설문](docs/research-survey.png) | ![니즈](docs/research-needs.png) |

## 디자인

- **디자이너 2인**이 Figma로 디자인 시스템과 화면·컴포넌트를 설계
- **Figma Dev Mode MCP**를 이용해 Figma 컴포넌트를 프론트엔드(Vue) 코드에 반영 — 디자인–개발 간 핸드오프를 일관되게 자동화
- 🎨 **디자인 파일**: [Diduga_Design (Figma)](https://www.figma.com/design/E64gTfIyewiOjxKSIDBCWk/Diduga_Design?node-id=0-1)

## 주요 기능

- **위치 기반 주변 식당 추천** — Google Places API로 현재 위치 주변 식당 조회
- **필터** — 카테고리(음식점/카페 등)·영업중(openNow)·반경 필터, 추천 카드 개별 새로고침
- **점심 룰렛** — 회전 애니메이션 + 당첨 결과 공유
- **투표** — 투표 생성/공유/종료, 결과 지도 표시, 종료 알림
- **즐겨찾기 · 저장 필터** — 즐겨찾는 식당과 자주 쓰는 검색 조건 저장(CRUD)
- **회원 · 인증** — 회원가입/로그인/마이페이지/프로필/탈퇴 (Spring Security + BCrypt)
- **지도** — Google Maps로 위치·경로·인포윈도우 표시

## 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Java 25, Spring Boot 3.5, Spring Security, Spring Data JPA, MySQL, Google Places API |
| Frontend | Vue 3, Vue Router, Vite |
| Tooling | Spotless (Java 포맷), GitHub Actions (CI) |

## 아키텍처

```
[Vue 3 / Vite]  --(/api 프록시)-->  [Spring Boot REST]  -->  [MySQL]
      |                                     |
  Google Maps JS                      Google Places API
 (VITE_GOOGLE_MAPS_API_KEY)          (GOOGLE_MAPS_API_KEY)
```

지도 표시는 클라이언트(브라우저)에서, 식당 검색은 백엔드에서 각각 Google API를 호출합니다. 프론트는 개발 시 `/api`를 백엔드(`localhost:8080`)로 프록시합니다(`vite.config.js`).

## 사전 요구사항

- **Docker로 실행(권장):** Docker Desktop / Docker Compose 만 있으면 됩니다. (MySQL·JDK·Node 설치 불필요)
- **로컬에서 직접 실행:** JDK 25, Node.js ≥ 22.12 (또는 20.19), MySQL 8+
- **Google Maps API 키는 선택** — 없으면 **데모 모드**(샘플 데이터)로 동작합니다. 실제 데이터·지도를 쓰려면 서버측(Places API)·클라이언트측(Maps JavaScript) 키를 발급하세요.

## Docker로 실행 (가장 쉬움)

MySQL 설치도, Google 키도 없이 **한 번에** 실행됩니다(데모 모드 기본).

```bash
docker compose up --build
```

MySQL·백엔드·프론트가 컨테이너로 함께 뜹니다. 이후:

1. 브라우저에서 **http://localhost:5173** 접속
2. **음식 종류를 하나 이상 선택**하고 **추천받기** 클릭
3. **위치 권한 허용** (거부해도 기본 위치로 동작) → **샘플 식당 추천 카드** 표시
4. 룰렛·투표·즐겨찾기도 동작 (즐겨찾기·마이페이지 등 개인화는 로그인 후)

> 데모 모드에선 실제 **지도 타일만** 표시되지 않습니다(클라이언트 키 필요). 추천 리스트·룰렛·투표 등은 정상입니다.

실제 Google 데이터/지도를 쓰려면 `.env`에 키를 넣고 다시 빌드하세요:

```bash
cp .env.example .env      # GOOGLE_MAPS_API_KEY / VITE_GOOGLE_MAPS_API_KEY 입력
docker compose up --build
```

> 아래 "실행 방법"은 Docker 없이 로컬에서 직접 구동하는 방법입니다.

## 실행 방법

### 1) 데이터베이스

```sql
CREATE DATABASE lunch_recommendation;
```

### 2) 백엔드

```bash
cd backend
cp src/main/resources/application.properties.example src/main/resources/application.properties
# application.properties 에 DB 계정과 GOOGLE_MAPS_API_KEY 를 입력
./gradlew bootRun          # http://localhost:8080
```

### 3) 프론트엔드

```bash
cd frontend
cp .env.example .env
# .env 에 VITE_GOOGLE_MAPS_API_KEY 를 입력
npm install
npm run dev                # http://localhost:5173 (/api → 백엔드로 프록시)
```

## 실제 데이터로 쓰기 (Google 키 — 선택)

데모 모드(샘플 데이터)로도 충분히 둘러볼 수 있지만, **실제 주변 식당·지도**를 쓰려면 Google Maps API 키를 발급해 넣으세요. [Google Cloud Console](https://console.cloud.google.com)에서:

- **Places API (New)** 활성화 → 서버측 키 → `GOOGLE_MAPS_API_KEY`
- **Maps JavaScript API** 활성화 → 클라이언트측 키 → `VITE_GOOGLE_MAPS_API_KEY` *(HTTP 리퍼러 제한 권장)*

Docker면 루트 `.env`에, 로컬 실행이면 각각 `application.properties`·프론트 `.env`에 넣습니다. 키를 넣은 뒤 `docker compose up --build`로 다시 빌드하면 실제 데이터와 지도가 표시됩니다. (저사용은 월 무료 한도 내라 사실상 무료)

## 환경변수

| 위치 | 변수 | 설명 |
|---|---|---|
| backend (`application.properties`) | `GOOGLE_MAPS_API_KEY` | 서버측 Google Places API 키. OS 환경변수로 설정해도 됨 |
| backend | `spring.datasource.*` | MySQL 접속 정보 |
| frontend (`.env`) | `VITE_GOOGLE_MAPS_API_KEY` | 클라이언트측 Google Maps JS 키 |

> ⚠️ **키 보안**: 클라이언트 지도 키는 브라우저에 노출되므로 Google Cloud에서 **HTTP 리퍼러 제한**을 걸고, 서버측 키와 **분리**하세요. `application.properties`·`.env`는 `.gitignore`로 커밋되지 않습니다.

## 데모

<!-- 스크린샷을 docs/ 에 저장한 뒤 아래 주석을 해제하세요.
![메인 · 주변 추천](docs/demo-main.png)
*위치 기반 주변 식당 추천 + 카테고리/영업중 필터*

![룰렛 · 투표](docs/demo-vote.png)
*점심 룰렛과 투표 결과 지도*
-->

## 기여 (팀 프로젝트)

**개발 (2인)**
- **정원기 ([@wongi-jeong](https://github.com/wongi-jeong))** — 백엔드 추천 로직 및 **Google Places 연동**, 식당/회원/투표 도메인·DB 설계, 프론트엔드 대부분(인증·룰렛·투표·즐겨찾기·지도·필터 UI) 주도
- **Heesun Hong ([@h3136514](https://github.com/h3136514))** — 초기 프로젝트 셋팅(프론트/백엔드), 저장소 관리

**디자인 (2인, Figma)**
- **김주희, 장유진** — 디자인 시스템·화면·컴포넌트 설계 (Figma Dev Mode MCP로 프론트엔드에 반영)

**기획 · 리서치** — 팀 공동 (문제 정의 · 경쟁사 조사 · 사용자 설문 · 페르소나)

> 개발 기여는 커밋 이력(`git shortlog -sne`, 브랜치별 변경 이력)에서 확인할 수 있습니다.
