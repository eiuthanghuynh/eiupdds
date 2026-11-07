package com.eiu.pdds.auth.security;

import com.eiu.pdds.auth.model.GoogleUserPayload;

/**
 * Port interface for cryptographic Google ID Token verification (DIP).
 */
public interface GoogleTokenVerifier {

    /**
     * Verifies cryptographic signature and audience of a Google ID Token.
     *
     * @param idTokenString raw base64 JWT ID token from Google Sign-In SDK
     * @return extracted verified claims
     */
    GoogleUserPayload verify(String idTokenString);
}
