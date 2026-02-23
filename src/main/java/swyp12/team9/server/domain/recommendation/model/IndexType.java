package swyp12.team9.server.domain.recommendation.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Elasticsearch 인덱스 타입 구분
 * - CHATBOT: 챗봇용 개인 링크 인덱스 (사용자별 검색)
 * - RECOMMENDATION: 추천용 공개 링크 인덱스 (전체 검색)
 */
@Getter
@RequiredArgsConstructor
public enum IndexType {
    CHATBOT("chatbot"),
    RECOMMENDATION("recommendation");

    private final String value;
}