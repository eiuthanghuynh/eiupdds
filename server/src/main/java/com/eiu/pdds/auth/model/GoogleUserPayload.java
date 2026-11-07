package com.eiu.pdds.auth.model;

/**
 * Standardized claims extracted from a verified Google ID Token.
 */
public record GoogleUserPayload(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl
) {}
