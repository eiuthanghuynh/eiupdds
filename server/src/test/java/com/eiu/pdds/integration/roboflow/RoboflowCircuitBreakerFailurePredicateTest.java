package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class RoboflowCircuitBreakerFailurePredicateTest {

    private final RoboflowCircuitBreakerFailurePredicate predicate = new RoboflowCircuitBreakerFailurePredicate();

    @Test
    @DisplayName("Records upstream RestClientException, IOException, TimeoutException, and ROBOFLOW_API_ERROR/5xx BusinessException")
    void test_upstreamFailures_returnTrue() {
        assertThat(predicate.test(new ResourceAccessException("Connection refused"))).isTrue();
        assertThat(predicate.test(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Bad Gateway"))).isTrue();
        assertThat(predicate.test(new IOException("Socket closed"))).isTrue();
        assertThat(predicate.test(new TimeoutException("Timed out"))).isTrue();
        assertThat(predicate.test(new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Upstream failed"))).isTrue();
        assertThat(predicate.test(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Server failure"))).isTrue();
    }

    @Test
    @DisplayName("Ignores client validation errors, CallNotPermittedException, non-upstream exceptions, and null")
    void test_clientErrorsAndCallNotPermitted_returnFalse() {
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("test");
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(breaker);

        assertThat(predicate.test(new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Empty image"))).isFalse();
        assertThat(predicate.test(new BusinessException(ErrorCode.INVALID_FILE_TYPE, "Bad image"))).isFalse();
        assertThat(predicate.test(new BusinessException(ErrorCode.NO_DETECTION_FOUND, "Blurry image"))).isFalse();
        assertThat(predicate.test(callNotPermitted)).isFalse();
        assertThat(predicate.test(new IllegalArgumentException("Bad caller arg"))).isFalse();
        assertThat(predicate.test(null)).isFalse();
    }
}
