package com.eiu.pdds.auth.security;

import com.eiu.pdds.auth.model.GoogleUserPayload;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Adapter implementing GoogleTokenVerifier via Google API Client SDK.
 */
@Slf4j
@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierImpl(@Value("${security.oauth2.google.client-id:}") String clientId) {
        GoogleIdTokenVerifier.Builder builder = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        );
        if (clientId != null && !clientId.isBlank()) {
            builder.setAudience(Collections.singletonList(clientId));
        }
        this.verifier = builder.build();
    }

    @Override
    public GoogleUserPayload verify(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Google ID token must not be null or empty");
        }

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID token verification failed: invalid signature or audience");
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (!emailVerified) {
                log.warn("Google account email is not verified: email={}", email);
                throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Google account email is not verified");
            }

            return new GoogleUserPayload(subject, email, emailVerified, name, pictureUrl);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to verify Google ID token cryptographically", e);
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED, "Failed to verify Google token: " + e.getMessage());
        }
    }
}
