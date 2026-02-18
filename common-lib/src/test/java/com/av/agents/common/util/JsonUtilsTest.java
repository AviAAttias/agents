package com.av.agents.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsTest {

    @Test
    void toJson_serializesObject() {
        JsonUtils utils = new JsonUtils(new ObjectMapper());

        String json = utils.toJson(Map.of("key", "value"));

        assertThat(json).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void toJson_wrapsJsonProcessingException() {
        ObjectMapper objectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("forced") {};
            }
        };
        JsonUtils utils = new JsonUtils(objectMapper);

        assertThatThrownBy(() -> utils.toJson(Map.of("k", "v")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JSON serialization failed");
    }
}
