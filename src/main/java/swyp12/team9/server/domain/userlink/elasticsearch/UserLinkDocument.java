package swyp12.team9.server.domain.userlink.elasticsearch;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * ============================================================ 3단계: Elasticsearch Document
 * ============================================================
 * <p>
 * 사전 요구사항: - build.gradle에 spring-boot-starter-data-elasticsearch 추가 - Elasticsearch 7.x 이상 실행 중 - nori 플러그인 설치
 */
@Document(indexName = "userlinks")
@Setting(settingPath = "/elasticsearch/userlinks-settings.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLinkDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Long)
    private Long linkId;

    // ==================== 검색 대상 필드 ====================

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori_search")
    private String why;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori_search")
    private String memo;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori_search")
    private String title;

    @Field(type = FieldType.Text, analyzer = "nori", searchAnalyzer = "nori_search")
    private String aiSummary;

    @Field(type = FieldType.Keyword)
    private String url;

    // ==================== 필터링/정렬용 필드 ====================

//    @Field(type = FieldType.Boolean)
//    private Boolean isPublic;

    @Field(type = FieldType.Long)
    private Long viewCount;

    @Field(type = FieldType.Keyword, index = false)
    private String faviconUrl;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    //  자동완성용 필드
    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    private String titleAutocomplete;

    public static UserLinkDocument from(UserLink userLink) {
        return UserLinkDocument.builder()
                .id(userLink.getId())
                .userId(userLink.getUser().getId())
                .linkId(userLink.getLink().getId())
                .why(userLink.getWhy())
                .memo(userLink.getMemo())
                .title(userLink.getLink().getTitle())
                .aiSummary(userLink.getLink().getAiSummary())
                .url(userLink.getLink().getUrl())
                .viewCount(userLink.getViewCount())
                .faviconUrl(userLink.getLink().getFaviconUrl())
                .createdAt(userLink.getCreatedAt())
                .updatedAt(userLink.getUpdatedAt())
                .titleAutocomplete(userLink.getLink().getTitle())
                .build();
    }
}