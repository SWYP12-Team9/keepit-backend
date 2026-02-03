package swyp12.team9.server.domain.terms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import swyp12.team9.server.api.terms.dto.TermsType;
import swyp12.team9.server.api.terms.dto.response.TermsResponse;
import swyp12.team9.server.domain.terms.exception.TermsFileReadException;
import swyp12.team9.server.domain.terms.exception.TermsNotFoundException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TermsService {

    private static final String TERMS_PATH = "terms/";
    private static final String EFFECTIVE_DATE_SERVICE = "2026년 2월 8일";
    private static final String EFFECTIVE_DATE_PRIVACY = "2026년 2월 8일";

    /**
     * 서비스 이용약관 조회
     */
    public TermsResponse getServiceTerms() {
        return getTerms(TermsType.SERVICE, EFFECTIVE_DATE_SERVICE);
    }

    /**
     * 개인정보 처리방침 조회
     */
    public TermsResponse getPrivacyPolicy() {
        return getTerms(TermsType.PRIVACY, EFFECTIVE_DATE_PRIVACY);
    }

    /**
     * 약관 조회 공통 메서드
     */
    private TermsResponse getTerms(TermsType type, String effectiveDate) {
        String content = readTermsFile(type.getFileName());
        return TermsResponse.of(type, effectiveDate, content);
    }

    /**
     * 약관 파일 읽기
     */
    private String readTermsFile(String fileName) {
        String filePath = TERMS_PATH + fileName;

        try {
            ClassPathResource resource = new ClassPathResource(filePath);

            if (!resource.exists()) {
                log.error("약관 파일을 찾을 수 없습니다: {}", filePath);
                throw new TermsNotFoundException();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }

        } catch (TermsNotFoundException e) {
            throw e;
        } catch (IOException e) {
            log.error("약관 파일 읽기 실패: {}", filePath, e);
            throw new TermsFileReadException();
        }
    }
}
