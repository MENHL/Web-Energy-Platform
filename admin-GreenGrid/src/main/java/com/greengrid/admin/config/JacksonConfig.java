package com.greengrid.admin.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 时间序列化 / 反序列化配置。
 * <p>序列化：日期 yyyy-MM-dd、时间 yyyy-MM-dd HH:mm:ss（避免默认 ISO 格式带 T）。</p>
 * <p>反序列化：容忍空字符串（前端未选日期时提交 {@code ""}），统一转成 null，避免 400。</p>
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .serializers(
                        new LocalDateTimeSerializer(DATETIME_FORMATTER),
                        new LocalDateSerializer(DATE_FORMATTER))
                .deserializers(
                        new LenientLocalDateTimeDeserializer(),
                        new LenientLocalDateDeserializer());
    }

    /** 时间反序列化器：空串 / null 转 null，否则按 yyyy-MM-dd HH:mm:ss 解析 */
    public static class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public Class<?> handledType() {
            return LocalDateTime.class;
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            return LocalDateTime.parse(text.trim(), DATETIME_FORMATTER);
        }
    }

    /** 日期反序列化器：空串 / null 转 null，否则按 yyyy-MM-dd 解析 */
    public static class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public Class<?> handledType() {
            return LocalDate.class;
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        }
    }
}
