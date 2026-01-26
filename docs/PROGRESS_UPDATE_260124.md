# Progress Update (2026-01-25)

## 🎯 **Architectural Change: External Scraping Service**

기존 서버 내부의 자바 기반 스크래핑 로직(Jsoup, Apify)을 제거하고, **외부 파이썬 서버**로 이관하기로 결정했습니다.

### 🔄 **1. Changes Summary**

| Component        | Before (Internal)                                     | After (External / Mock)                                                                    |
| :--------------- | :---------------------------------------------------- | :----------------------------------------------------------------------------------------- |
| **Architecture** | Java Spring Boot 내부에서 `Jsoup` / `Apify` 직접 호출 | 별도 **Python 서버**가 스크래핑 전담 <br> -> Java 서버는 API로 결과(`title`, `img`)만 받음 |
| **Logic**        | URL 저장 요청 시 서버가 스크래핑 수행 (동기/비동기)   | 클라이언트(또는 Python 서버)가 **URL + Metadata**를 함께 전송                              |
| **Dependencies** | `jsoup`, `okhttp`, `spring-ai` (구버전 호환)          | `jsoup`, `okhttp` **삭제** <br> `spring-boot 3.4.1`, `java 21` (LTS 안정화)                |

### ✅ **2. Implementation Details**

#### **A. DTO Update (`SaveLinkRequest`)**

외부에서 스크래핑 된 데이터를 받아 저장하기 위해 필드를 추가했습니다.

```java
public record SaveLinkRequest(
    ...
    String title,       // 추가 (Optional)
    String description, // 추가 (Optional)
    String imageUrl     // 추가 (Optional)
) {}
```

이제 요청 시 제목과 이미지를 직접 넣어주면, 서버가 스크래핑 없이 그대로 DB에 저장합니다.

#### **B. Service Update (`LinkService`)**

- 스크래핑(`scraperService`) 로직 삭제.
- 파라미터로 받은 `title`이 있으면 그것을 사용, 없으면 "제목 없음"으로 저장.

#### **C. Test Scripts (`init_data.sh`)**

- 더 이상 서버가 자동으로 스크래핑하지 않으므로, 테스트 스크립트에서 **Mock Metadata**를 함께 전송하도록 수정했습니다.
- 예:
  ```bash
  curl -d '{
    "url": "https://www.instagram.com/p/...",
    "title": "Instagram Post Mock",
    "imageUrl": "https://example.com/image.jpg"
  }' ...
  ```

---

### 📝 **3. Troubleshooting Log (Resolved)**

1.  **Elasticsearch vs Spring Boot Version Conflict**
    - **문제**: macOS/Docker 환경에서 ES 8.16+ 실행 시 커널 충돌 발생 + Spring Boot 3.5.x(미래 버전)와 라이브러리 버전 불일치로 `NoSuchMethodError` 발생.
    - **해결**:
      - **Elasticsearch**: `8.15.5`로 다운그레이드 (Docker Image & Gradle Dependency)
      - **Spring Boot**: `3.4.1` (안정 버전)로 다운그레이드
      - **Java**: `21` (LTS)로 설정하여 Gradle/Lombok 호환성 확보

---

### 🚀 **Next Steps (User Action)**

- **Python 스크래핑 서버 구축**: 실제 운영을 위해 URL을 받아 메타데이터를 반환해 줄 파이썬 서버 개발 필요.
- **연동 테스트**: 프론트엔드 또는 파이썬 서버에서 Java API (`POST /api/v1/links`) 호출 시 데이터가 잘 들어가는지 확인.
