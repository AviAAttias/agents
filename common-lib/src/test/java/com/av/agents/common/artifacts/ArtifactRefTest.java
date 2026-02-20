package com.av.agents.common.artifacts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactRefTest {

    @Test
    void parse_fileUri() {
        ArtifactRef ref = ArtifactRef.parse("file:///tmp/sample.pdf");
        assertThat(ref.scheme()).isEqualTo("file");
        assertThat(ref.id()).isEqualTo("/tmp/sample.pdf");
    }

    @Test
    void parse_customScheme() {
        ArtifactRef ref = ArtifactRef.parse("text-artifact://42");
        assertThat(ref.scheme()).isEqualTo("text-artifact");
        assertThat(ref.id()).isEqualTo("//42");
    }


    @Test
    void parse_approvalScheme() {
        ArtifactRef ref = ArtifactRef.parse("appr:123");
        assertThat(ref.scheme()).isEqualTo("appr");
        assertThat(ref.id()).isEqualTo("123");
    }

    @Test
    void parse_unsupportedSchemeFails() {
        assertThatThrownBy(() -> ArtifactRef.parse("pdf:abcd"))
                .isInstanceOf(ArtifactResolutionException.class)
                .hasMessageContaining("Unsupported artifact reference scheme");
    }
}
