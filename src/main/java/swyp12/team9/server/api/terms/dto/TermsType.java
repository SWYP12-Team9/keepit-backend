package swyp12.team9.server.api.terms.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

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
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Terms type cannot be empty");
        }
        try {
            return TermsType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid terms type: " + value);
        }
    }
}
