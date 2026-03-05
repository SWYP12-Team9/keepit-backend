package swyp12.team9.server.domain.stat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.stat.dto.UserStatResponse;
import swyp12.team9.server.domain.stat.service.UserStatService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

/**
 * Stat Controller
 * 사용자 통계 API 구현
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserStatController implements UserStatApi {

    private final UserStatService userStatService;

    /**
     * 마이페이지 사용자 통계 조회
     * @param userId 사용자 ID
     * @return 사용자 통계 응답
     */
    @Override
    public ApiResponse<UserStatResponse> getUserStat(@CurrentUserId Long userId) {
        UserStatResponse response = userStatService.getUserStat(userId);

        return ApiResponse.ok(response);
    }
}