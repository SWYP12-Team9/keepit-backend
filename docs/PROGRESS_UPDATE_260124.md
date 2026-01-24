# Progress Update (2026-01-24)

## 🎯 **Achievement: Web Scraping Pipeline Stabilization**

스크래핑 모듈이 다양한 플랫폼(Instagram, Naver News, General Web)에 대해 안정적으로 동작하도록 구조를 개선하고 구현을 완료했습니다.

### ✅ **1. Scraper Implementation Status**

| Strategy                       | Target                | Status     | Tech Stack    | Key Features                                                                                                                                                                                                                                |
| :----------------------------- | :-------------------- | :--------- | :------------ | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **`InstagramScraperStrategy`** | Instagram Posts/Reels | **Active** | **Apify API** | - `apify/instagram-post-scraper` Actor 연동<br>- **URL 정규화**: `utm_source` 등 파라미터 자동 제거 후 전송<br>- **Payload 최적화**: `username` 필드를 사용한 독특한 스키마 준수<br>- **Priority: 10** (최우선 순위, Default로 빠지지 않음) |
| **`NewsScraperStrategy`**      | Naver News            | **Active** | **Jsoup**     | - **Content Cleaning**: 기자 정보, 무단 전재 문구 등 불필요 텍스트 제거<br>- **Smart Selection**: PC/Mobile/Old 버전 선택자 모두 지원<br>- **Priority: 10**                                                                                 |
| **`DefaultScraperStrategy`**   | General Web           | **Active** | **Jsoup**     | - Open Graph / Meta Tag 기본 지원<br>- **Priority: 999** (Fallback)                                                                                                                                                                         |

#### 📂 **File Structure (`domain/scraper`)**

```
src/main/java/swyp12/team9/server/domain/scraper/
├── dto/
│   └── ScrapedContent.java       # 스크래핑 결과 객체 (Title, Content, ImageUrl)
├── exception/
│   └── ScrapingException.java    # 스크래핑 전용 예외
├── factory/
│   └── ScraperFactory.java       # URL에 맞는 전략을 찾아주는 Factory (Priority 기반)
├── service/
│   └── ScraperService.java       # 상위 서비스 (비동기, 재시도 로직 포함 필요 시 확장)
└── strategy/                     # [핵심] 전략 패턴 구현체들
    ├── ScraperStrategy.java      # 인터페이스 (scrape, supports, priority)
    ├── DefaultScraperStrategy.java
    ├── InstagramScraperStrategy.java
    └── NewsScraperStrategy.java
```

---

### 🛠 **2. Testing & Scripts**

스크래핑/링크 기능을 테스트하기 위한 쉘 스크립트가 준비되었습니다.

#### **A. `scripts/test_insta.sh` (추천 ⭐)**

> **"DB 초기화부터 인스타그램 스크래핑 검증까지 한 방에!"**

- **기능**: Docker DB 초기화 -> 기초 데이터(Users/Folders) 주입 -> 로그인 -> 인스타그램 링크 추가 요청(Apify 호출) -> 결과 JSON 출력
- **용도**: 서버 재시작 직후 인스타그램 연동이 잘 되는지 빠르게 확인할 때.
- **실행**:
  ```bash
  chmod +x scripts/test_insta.sh
  ./scripts/test_insta.sh
  ```

#### **B. `scripts/init_data.sh`**

> **"전체 시나리오 데이터 셋업"**

- **기능**: DB 초기화, 모든 봇 계정 생성, 다양한 샘플 데이터(Docker, React, Kafka, News 등) 대량 주입.
- **용도**: 앱 전체 기능을 E2E로 테스트하거나 프론트엔드 연동 확인할 때.
- **실행**:
  ```bash
  ./scripts/init_data.sh
  ```
  _(주의: 실행 전 서버가 켜져 있어야 하며, Apify 과금을 아끼려면 인스타 링크 부분은 주석 처리 가능)_

---

### 📝 **3. Troubleshooting Log (Resolved)**

1.  **Instagram 403 Forbidden / Login Redirect**
    - **원인**: Jsoup이나 일반 HTTP 요청으로는 인스타그램의 보안 벽을 뚫을 수 없음.
    - **해결**: **Apify**를 사용하여 실제 브라우저/모바일 환경을 모사한 스크래핑 대행 방식으로 전환.

2.  **Apify "Bad Request (400)" Error**
    - **원인 1**: URL에 `utm_source` 등 지저분한 파라미터가 있어 Actor가 처리를 거부하거나 데이터 없음 반환.
      - -> **해결**: `normalizeInstagramUrl` 메서드로 `?` 뒤 파라미터 제거 및 `/` 보장.
    - **원인 2**: `directUrls` / `startUrls` 등 일반적인 필드가 아닌, 해당 Actor만의 독자적인 스키마 사용.
      - -> **해결**: Documentation 확인 후 Payload를 `{"username": ["URL"]}` 형태로 수정하여 해결.

3.  **Reference ID Null Error (400)**
    - **원인**: 테스트 스크립트에서 폴더 ID 없이 링크 추가를 요청하여 검증 실패.
    - **해결**: `test_insta.sh`에 DB 초기화 및 `referenceId: 2` 명시적 주입 로직 추가.

---

### 🚀 **Next Steps**

- [ ] **유튜브 스크래핑 (`YoutubeScraperStrategy`)**: `oEmbed`를 사용하거나 YouTube Data API 연동 고려.
- [ ] **비동기 처리 고도화**: 스크래핑 시간이 길어질 경우(특히 Apify) 유저 경험 개선을 위해 SSE/WebSocket 알림 검토.
