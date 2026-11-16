package com.jobtracker.dto;

import java.time.Instant;

/** Public view of a user. Deliberately has no password or hash field. */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Instant createdAt
) {
}
