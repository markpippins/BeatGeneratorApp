package com.angrysurfer.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SmokeTestHelperTest {
    @Test
    void smokeHelloDoesNotThrow() {
        assertDoesNotThrow(() -> SmokeTestHelper.hello());
    }
}
