package com.docint.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure JUnit 5 unit test — no Spring, no mocks. HashUtil has only static methods
 * with no dependencies, so we just call them and assert on the output.
 */
class HashUtilTest {

    @Test
    @DisplayName("same bytes always produce the same hash (deterministic)")
    void sameBytesProduceSameHash() {
        byte[] data = "hello world".getBytes();

        String hash1 = HashUtil.sha256(data);
        String hash2 = HashUtil.sha256(data);

        assertThat(hash1).isEqualTo(hash2);                 // deterministic
        assertThat(hash1).hasSize(64);                      // SHA-256 hex = 64 chars
    }

    @Test
    @DisplayName("different bytes produce different hashes")
    void differentBytesProduceDifferentHashes() {
        String hashA = HashUtil.sha256("document A".getBytes());
        String hashB = HashUtil.sha256("document B".getBytes());

        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    @DisplayName("known input matches known SHA-256 (sanity check vs. the algorithm)")
    void knownVector() {
        // SHA-256 of empty string is a well-known constant
        String hash = HashUtil.sha256(new byte[0]);
        assertThat(hash).isEqualTo(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
