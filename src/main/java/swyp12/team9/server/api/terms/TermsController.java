package swyp12.team9.server.api.terms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.terms.dto.response.TermsResponse;
import swyp12.team9.server.domain.terms.service.TermsService;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController implements TermsApi {

    private final TermsService termsService;

    @Override
    public ApiResponse<TermsResponse> getServiceTerms() {
        TermsResponse response = termsService.getServiceTerms();
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<TermsResponse> getPrivacyPolicy() {
        TermsResponse response = termsService.getPrivacyPolicy();
        return ApiResponse.ok(response);
    }
}
