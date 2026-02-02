package swyp12.team9.server.global.annotation;

import org.springframework.http.HttpStatus;
import swyp12.team9.server.global.exception.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 응답 명세를 정의하는 어노테이션
 * 성공 응답과 에러 응답을 함께 지정하여 Swagger 문서를 자동 생성합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSpec {

    /**
     * 성공 응답 HTTP 상태 코드
     * @return HttpStatus (기본값: 200 OK)
     */
    HttpStatus status() default HttpStatus.OK;

    /**
     * 발생 가능한 에러 코드 목록
     * @return ErrorCode 배열
     */
    ErrorCode[] errors() default {};

    /**
     * 응답 헤더 목록
     * @return ResponseHeader 배열
     */
    ResponseHeader[] headers() default {};

    /**
     * 응답 헤더 정의
     */
    @interface ResponseHeader {
        /**
         * 헤더 이름
         */
        String name();

        /**
         * 헤더 설명
         */
        String description() default "";

        /**
         * 헤더 예시 값
         */
        String example() default "";
    }
}
