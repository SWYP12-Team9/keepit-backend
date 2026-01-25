#!/bin/bash

echo "🔄 1. DB 초기화 및 유저 생성 중..."
docker exec -i swyp-mysql mysql -uswyp -ppassword --default-character-set=utf8mb4 swyp_db <<EOF
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE reference_user_links;
TRUNCATE TABLE user_links;
TRUNCATE TABLE reference;
TRUNCATE TABLE links;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (user_id, username, password, name, nickname, email, is_lock, is_social, role_type, created_at, updated_at) VALUES 
(1, 'tester', '\$2a\$10\$s7tEgnL5YQhdGTqxuRQZCuyegv2IZTJjt8zMLrzItv2eF/aAgBz6.', '테스터', 'Tester', 'tester@test.com', 0, 0, 'USER', NOW(), NOW()),
(2, 'dev_bot', '\$2a\$10\$s7tEgnL5YQhdGTqxuRQZCuyegv2IZTJjt8zMLrzItv2eF/aAgBz6.', '개발봇', 'DevBot', 'dev@bot.com', 0, 0, 'USER', NOW(), NOW()),
(3, 'cook_bot', '\$2a\$10\$s7tEgnL5YQhdGTqxuRQZCuyegv2IZTJjt8zMLrzItv2eF/aAgBz6.', '요리봇', 'CookBot', 'cook@bot.com', 0, 0, 'USER', NOW(), NOW());

INSERT INTO reference (reference_id, user_id, title, description, is_public, created_at, updated_at) VALUES 
(1, 1, '테스터의 학습 폴더', '열공 화이팅', 1, NOW(), NOW()),
(2, 2, '백엔드/AI 추천 자료', '개발자 필수 링크', 1, NOW(), NOW()),
(3, 3, '주말 점심 메뉴', '간단 요리법', 1, NOW(), NOW());
EOF

echo "✅ DB 초기화 완료."
echo "🔄 2. 인덱스 재생성 (클린하게)..."
curl -X DELETE "http://localhost:9200/spring-ai-document-index" > /dev/null 2>&1
curl -X PUT "http://localhost:9200/spring-ai-document-index" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "embedding": {
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine"
      }
    }
  }
}' > /dev/null 2>&1

echo -e "\n✅ 인덱스 생성 완료."

echo "🔑 3. DevBot 로그인 중..."
LOGIN_RES=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"dev_bot", "password":"1234"}')

# JSON 파싱 수정 (accessToken)
TOKEN=$(echo $LOGIN_RES | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ 로그인 실패! 응답: $LOGIN_RES"
    exit 1
fi

echo "✅ 로그인 성공 (Token 획득)."

echo "🚀 4. DevBot 데이터 5개 입력 중 (ES 색인)..."

# 데이터 1: 도커
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 2,
    "url": "https://www.docker.com",
    "purpose": "도구",
    "why": "컨테이너 환경",
    "memo": "도커 필수"
  }' > /dev/null

# 데이터 4: 리액트
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 2,
    "url": "https://react.dev",
    "purpose": "프론트엔드",
    "why": "UI 라이브러리",
    "memo": "웹 개발"
  }' > /dev/null

# 데이터 5: 인스타그램 (Apify 테스트)
# (실제 존재하는 게시물 URL이어야 스크래핑이 성공함. 예시 URL 사용)
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 2,
    "url": "https://www.instagram.com/p/DTxSH-vk91Z/?utm_source=ig_web_copy_link&igsh=NTc4MTIwNjQ2YQ==", 
    "purpose": "주식",
    "why": "UI 레퍼런스",
    "memo": "인스타 테스트"
  }' > /dev/null

# 데이터 3: 카프카
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 2,
    "url": "https://kafka.apache.org",
    "purpose": "아키텍처",
    "why": "메시지 큐",
    "memo": "MSA 아키텍처"
  }' > /dev/null

echo "✅ DevBot 데이터 입력 완료."

echo "🔑 5. Tester(나) 로그인 및 내 데이터 입력..."
MY_LOGIN_RES=$(curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tester", "password":"1234"}')
  
MY_TOKEN=$(echo $MY_LOGIN_RES | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

# 내 관심사: "자바 공부"
curl -s -X POST http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $MY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 1,
    "url": "https://www.java.com",
    "purpose": "공부",
    "why": "자바 기초",
    "memo": "자바 마스터하기"
  }' > /dev/null

echo "✅ Tester 데이터 입력 완료."
echo "🎉 모든 준비 끝! 이제 추천 API를 호출해보세요."
echo "👉 토큰: Bearer $MY_TOKEN"
