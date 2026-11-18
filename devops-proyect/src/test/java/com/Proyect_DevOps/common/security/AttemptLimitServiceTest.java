package com.Proyect_DevOps.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttemptLimitServiceTest {

    @Test
    void blocksAccountAfterMaximumFailures() {
        AttemptLimitService service = new AttemptLimitService(3, 1);

        service.recordFailure("login:user@example.com");
        service.recordFailure("login:user@example.com");
        assertFalse(service.isBlocked("login:user@example.com"));

        service.recordFailure("login:user@example.com");

        assertTrue(service.isBlocked("login:user@example.com"));
        assertTrue(service.retryAfterSeconds("login:user@example.com") > 0);
    }

    @Test
    void resetClearsFailuresAfterSuccessfulLogin() {
        AttemptLimitService service = new AttemptLimitService(2, 1);

        service.recordFailure("login:user@example.com");
        service.recordFailure("login:user@example.com");
        assertTrue(service.isBlocked("login:user@example.com"));

        service.reset("login:user@example.com");

        assertFalse(service.isBlocked("login:user@example.com"));
    }

    @Test
    void keysAreCaseInsensitiveAndTrimmed() {
        AttemptLimitService service = new AttemptLimitService(1, 1);

        service.recordFailure("  LOGIN:User@Example.com  ");

        assertTrue(service.isBlocked("login:user@example.com"));
    }
}
