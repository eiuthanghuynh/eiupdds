package com.eiu.pdds.auth.security;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleTokenVerifierImplTest {

    @Test
    @DisplayName("verify - When client IDs configuration is empty, throws BusinessException at runtime")
    void shouldThrowWhenNoClientIdsConfigured() {
        GoogleTokenVerifierImpl verifier = new GoogleTokenVerifierImpl("");

        assertThatThrownBy(() -> verifier.verify("dummy.jwt.token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(be.getMessage()).contains("Google login is not configured");
                });
    }

    @Test
    @DisplayName("verify - When null or whitespace token is passed, throws BusinessException")
    void shouldThrowOnNullOrBlankToken() {
        GoogleTokenVerifierImpl verifier = new GoogleTokenVerifierImpl("client-1.apps.googleusercontent.com,client-2.apps.googleusercontent.com");

        assertThatThrownBy(() -> verifier.verify(""))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_AUTH_FAILED);
                });
    }
}
