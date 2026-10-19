package ru.arman.creditservice.infrastructure.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsTest {

    @Nested
    @DisplayName("toJson")
    class ToJson {

        @Test
        @DisplayName("should serialize simple record to JSON")
        void serializeSimpleRecord() {
            var record = Map.of("key", "value", "number", 42);
            var json = JsonUtils.toJson(record);

            assertThat(json).contains("\"key\"")
                    .contains("\"value\"")
                    .contains("\"number\"")
                    .contains("42");
        }

        @Test
        @DisplayName("should serialize object with Instant without timestamps")
        void serializeWithInstant() {
            var instant = Instant.parse("2025-01-15T10:30:00Z");
            var record = Map.of("timestamp", instant);
            var json = JsonUtils.toJson(record);

            assertThat(json).contains("2025-01-15")
                    .doesNotContain("\"type\"")
                    .doesNotContain("\"instant\"");
        }

        @Test
        @DisplayName("should serialize BigDecimal correctly")
        void serializeBigDecimal() {
            var record = Map.of("amount", new BigDecimal("12345.67"));
            var json = JsonUtils.toJson(record);

            assertThat(json).contains("12345.67");
        }
    }

    @Nested
    @DisplayName("fromJson")
    class FromJson {

        record Person(String name, int age) {
        }

        @Test
        @DisplayName("should deserialize JSON to record")
        void deserializeToRecord() {
            var json = """
                    {"name":"Alice","age":30}
                    """;

            var person = JsonUtils.fromJson(json, Person.class);

            assertThat(person.name()).isEqualTo("Alice");
            assertThat(person.age()).isEqualTo(30);
        }

        @Test
        @DisplayName("should throw IllegalStateException for invalid JSON")
        void deserializeInvalidJson() {
            assertThatThrownBy(() -> JsonUtils.fromJson("{invalid", String.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot deserialize");
        }
    }
}
