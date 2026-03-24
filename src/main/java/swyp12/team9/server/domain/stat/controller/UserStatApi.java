package swyp12.team9.server.domain.stat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swyp12.team9.server.domain.stat.dto.UserStatResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * Stat API 인터페이스
 * 사용자 통계 관련 API 스펙을 정의합니다.
 */
@Tag(name = "Stat", description = "사용자 통계 API")
@RequestMapping("/api/v1/users/stats")
public interface UserStatApi {

    @Operation(
            summary = "마이페이지 사용자 통계 조회",
            description = """
                    사용자의 링크 저장 통계를 조회합니다.

                    ## 응답 구조
                    - **topReferences**: 상위 레퍼런스 통계 (최대 6개)
                    - **readState**: 읽음/미열람 상태 분포
                    - **savePattern**: 저장 패턴 (요일별 저장 개수)

                    ---

                    ## 1. 상위 레퍼런스 통계 (topReferences)

                    **데이터 범위:**
                    - isDefault=true인 미지정 폴더는 제외됩니다.
                    - 링크 개수가 많은 순으로 최대 6개까지 표시됩니다.
                    - 폴더가 6개 미만이면 있는 만큼만 표시됩니다.

                    **인사이트 텍스트 (text):**
                    - 폴더 0개: "아직 레퍼런스 폴더가 없어요. 폴더를 만들고 링크를 모아보세요."
                    - 폴더 1개 이상 + 모든 폴더 비어있음: "만들어둔 폴더가 아직 비어있어요. 관심 있는 링크를 모아보세요."
                    - 폴더 1개 이상 + 1위 단독: "요즘 가장 많이 모아둔 폴더는 '{폴더명}'이에요. 링크가 총 XX개예요."
                    - 1위 폴더 동률(2개): "요즘 가장 많이 모아둔 폴더는 '{폴더A}'와 '{폴더B}'예요. 링크가 각각 XX개예요."
                    - 1위 폴더 동률(3개 이상): "요즘 가장 많이 모아둔 폴더가 여러 개예요. 상위 폴더들이 각각 XX개예요."

                    ---

                    ## 2. 읽음 상태 통계 (readState)

                    **데이터 범위:**
                    - isDefault=true인 미지정 폴더를 포함한 모든 링크를 대상으로 합니다.

                    **퍼센트 계산:**
                    - 정수 반올림으로 표시됩니다.
                    - 반올림 후 합이 100이 되도록 나머지로 보정됩니다.

                    **인사이트 텍스트 (text) - 미열람 중심:**
                    - 전체 링크 0개: "아직 저장한 링크가 없어요. 링크를 저장하고 열람 현황을 확인해보세요."
                    - 전체 링크 1개 이상 + 미열람 0개: "미열람 링크가 없어요. 저장한 링크를 모두 열람했어요."
                    - 전체 링크 1개 이상 + 미열람 1개 이상 + 미열람 최다 폴더 단독: "전체 링크의 XX%를 아직 열람하지 않았어요. 미열람 링크는 '{폴더명}'에 총 XX개로 가장 많아요."
                    - 미열람 최다 폴더 동률(2개): "전체 링크의 XX%를 아직 열람하지 않았어요. 미열람 링크는 '{폴더A}'와 '{폴더B}'에 각각 XX개로 가장 많아요."
                    - 미열람 최다 폴더 동률(3개 이상): "전체 링크의 XX%를 아직 열람하지 않았어요. 미열람 링크가 여러 폴더에 비슷하게 분포돼 있어요."
                    - 미열람 최다 폴더 없음 (폴더에 속하지 않은 링크만): "전체 링크의 XX%를 아직 열람하지 않았어요. 미열람 링크가 총 XX개예요."

                    ---

                    ## 3. 저장 패턴 통계 (savePattern)

                    **데이터 범위:**
                    - isDefault=true인 미지정 폴더를 포함한 모든 링크를 대상으로 합니다.
                    - 최대 최근 4주까지의 데이터를 사용합니다.
                    - 4주 미만이면 첫 저장일부터 현재까지 데이터를 사용합니다.

                    **peakDay 필드:**
                    - 단독 1위 요일이 있을 때만 요일 이름(예: "월")이 들어갑니다.
                    - 동률이거나 데이터가 없으면 null입니다.

                    **인사이트 텍스트 (text):**
                    - 링크가 없는 경우: "최근 4주 동안 새로 저장한 링크가 없어요."
                    - 링크 저장이 1주일 이하: "아직 링크를 저장한 지 일주일이 안 됐어요. 일주일이 지나면 패턴을 보여드릴게요."
                    - 최근 4주 동안 저장이 0개: "최근 4주 동안 새로 저장한 링크가 없어요."
                    - 링크 저장이 1주일 이상 + 최다 요일 단독: "'{요일}'에 링크를 가장 많이 저장했어요. 총 XX개예요."
                    - 링크 저장이 1주일 이상 + 최다 요일 동률(2개): "'{요일A}'과 '{요일B}'에 링크를 가장 많이 저장했어요. 각각 XX개예요."
                    - 링크 저장이 1주일 이상 + 요일별 저장이 비슷할 때: "요일에 상관없이 꾸준히 링크를 저장하고 있어요."
                      * 비슷함 판단 기준: 최근 4주 저장 링크가 14개 이상이고, (최다-최소 차이가 3개 이하 또는 최다 요일이 3개 이상 동률)
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND
            }
    )
    @GetMapping("")
    ApiResponse<UserStatResponse> getUserStat(
            @CurrentUserId Long userId
    );
}