package swyp12.team9.server.api.reference.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;

@Builder
@Schema(description = "레퍼런스 뷰 폴더 응답 객체")
public record ReferenceResponse(

        @Schema(description = "레퍼런스 아이디", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "레퍼런스 제목", example = "경제", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "레퍼런스 설명", example = "경제 관련 레퍼런스 뷰 폴더입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "레퍼런스 공개여부", example = "공개/비공개", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean isPublic

) {

    public static ReferenceResponse from(Reference reference) {
        return ReferenceResponse.builder()
                .id(reference.getId())
                .title(reference.getTitle())
                .description(reference.getDescription())
                .isPublic(reference.getIsPublic())
                .build();
    }

//    public static ReferenceResponse from(Reference reference) {
//        return new ReferenceResponse(
//                reference.getId(),
//                reference.getTitle(),
//                reference.getDescription(),
//                reference.getIsPublic()
//        );
//    }
}
