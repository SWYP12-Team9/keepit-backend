package swyp12.team9.server.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Record 타입 캐싱(직렬화/역직렬화) 지원을 위한 커스텀 파서
 * NON_FINAL 규칙을 따르되, 레코드(isRecord)인 경우에도 다형성 식별자(@class)를 붙이도록 강제합니다.
 */
public class RecordTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {

    public RecordTypeResolverBuilder(ObjectMapper.DefaultTyping typing, PolymorphicTypeValidator ptv) {
        super(typing, ptv);
        this.init(JsonTypeInfo.Id.CLASS, null);
        this.inclusion(JsonTypeInfo.As.PROPERTY);
        this.typeProperty("@class");
    }

    @Override
    public boolean useForType(JavaType t) {
        if (t.getRawClass() != null && t.getRawClass().isRecord()) {
            return true;
        }
        return super.useForType(t);
    }
}
