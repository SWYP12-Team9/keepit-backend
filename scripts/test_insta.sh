#!/bin/bash



echo "� 1. DB 초기화 및 기초 데이터 주입 (Docker MySQL)..."
# init_data.sh의 SQL 주입 로직 복사
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
(2, 'dev_bot', '\$2a\$10\$s7tEgnL5YQhdGTqxuRQZCuyegv2IZTJjt8zMLrzItv2eF/aAgBz6.', '개발봇', 'DevBot', 'dev@bot.com', 0, 0, 'USER', NOW(), NOW());

INSERT INTO reference (reference_id, user_id, title, description, is_public, created_at, updated_at) VALUES 
(1, 1, '테스터의 학습 폴더', '열공 화이팅', 1, NOW(), NOW()),
(2, 2, '백엔드/AI 추천 자료', '개발자 필수 링크', 1, NOW(), NOW());
EOF

echo "✅ DB 초기화 완료."

# 설정
API_URL="http://localhost:8080"
USERNAME="dev_bot"
PASSWORD="1234"  # init_data.sh의 bcrypt 해시와 매칭되는 비밀번호

echo "🔹 [2] 로그인 시도 (dev_bot)..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\", \"password\":\"$PASSWORD\"}")

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)


if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
  echo "❌ 최종 로그인 실패! 서버 응답: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ 로그인 성공! (Token 획득)"

# 테스트할 인스타그램 URL
INSTA_URL="https://www.instagram.com/p/DTxSH-vk91Z/?utm_source=ig_web_copy_link&igsh=NTc4MTIwNjQ2YQ=="

# init_data.sh의 페이로드 형식에 맞춤 (referenceId 등 포함)
# 단, referenceId가 1번(테스터 폴더)이 있다고 가정. 없으면 에러 날 수 있으니 0이나 1 시도.
# 안전하게 referenceId 없이 보내보거나(옵셔널이면), 1번을 보냄.
# DB가 비어있으면 referenceId FK 에러 날 수 있으므로, reference도 하나 만듦.

echo "🔹 [2] Reference(폴더) 생성 (혹시 없을 경우를 대비)"
# 폴더 생성 API가 있다고 가정 (init_data.sh에는 SQL로 넣어서 API 모름)
# 모르면 그냥 link 추가 시도 (referenceId=1)

echo "🔹 [3] 인스타그램 링크 추가 테스트: $INSTA_URL"
echo "   (Apify 스크래핑 진행 중...)"

# init_data.sh는 /api/v1/links 사용
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/links" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "referenceId": 2,
    "url": "'"$INSTA_URL"'",
    "purpose": "테스트",
    "why": "인스타 스크래핑 확인",
    "memo": "잘 되나?"
  }')

echo "🔹 [4] 결과 확인"
if command -v jq &> /dev/null; then
    echo $RESPONSE | jq .
else
    echo $RESPONSE
fi
