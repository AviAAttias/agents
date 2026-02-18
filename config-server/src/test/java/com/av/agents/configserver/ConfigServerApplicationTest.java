package com.av.agents.configserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConfigServerApplicationTest {

    @Test
    void main_runsWithoutThrowing() {
        assertThatCode(() -> ConfigServerApplication.main(new String[]{})).doesNotThrowAnyException();
    }
}
