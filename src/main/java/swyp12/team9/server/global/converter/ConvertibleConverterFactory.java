package swyp12.team9.server.global.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import java.util.Arrays;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ConvertibleConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return new ConvertibleConverter(targetType);
    }

    private static class ConvertibleConverter<T extends Enum> implements Converter<String, T> {
        private final Class<T> enumType;

        public ConvertibleConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isBlank()) return null;

            return (T) Arrays.stream(enumType.getEnumConstants())
                    .filter(e -> {
                        // 1. Convertible 인터페이스를 구현했다면 설정한 값(all, latest 등)으로 비교
                        if (e instanceof Convertible convertible) {
                            return convertible.getParameterValue().equalsIgnoreCase(source.trim());
                        }
                        // 2. 구현하지 않은 일반 Enum이라면 Enum 이름(ALL, PUBLIC 등)으로 비교
                        return e.name().equalsIgnoreCase(source.trim());
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("요청하신 값이 올바르지 않습니다: " + source));
        }
    }
}