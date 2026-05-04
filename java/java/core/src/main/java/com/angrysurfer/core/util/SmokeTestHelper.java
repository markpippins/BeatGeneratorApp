package com.angrysurfer.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Minimal, safe executable change for Recovery Mode: a tiny smoke helper
public class SmokeTestHelper {
    private static final Logger logger = LoggerFactory.getLogger(SmokeTestHelper.class);

    public static void hello() {
        logger.info("SmokeTestHelper: hello");
    }
}
