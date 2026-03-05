package swyp12.team9.server.domain.terms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.terms.exception.InvalidTermsTypeException;

import java.util.Arrays;

/**
 * 약관 타입
 */
@Getter
@RequiredArgsConstructor
public enum TermsType {

    SERVICE("서비스 이용약관", "service-terms.txt"),
    PRIVACY("개인정보 처리방침", "privacy-policy.txt");

    private final String title;
    private final String fileName;

    public static TermsType from(String value) {
        return Arrays.stream(TermsType.values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(InvalidTermsTypeException::new);
    }
}
