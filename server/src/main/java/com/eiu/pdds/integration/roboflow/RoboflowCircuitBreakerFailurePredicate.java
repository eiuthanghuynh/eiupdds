package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Resilience4j failure predicate that records only upstream AI provider/network failures,
 * ignoring caller validation errors and circuit-open rejections.
 */
public class RoboflowCircuitBreakerFailurePredicate implements Predicate<Throwable> {

    public RoboflowCircuitBreakerFailurePredicate() {
    }

    @Override
    public boolean test(Throwable throwable) {
        if (throwable == null || throwable instanceof CallNotPermittedException) {
            return false;
        }
        if (throwable instanceof BusinessException be) {
            ErrorCode code = be.getErrorCode();
            return code == ErrorCode.ROBOFLOW_API_ERROR
                    || (code != null && code.getHttpStatus().is5xxServerError());
        }
        return throwable instanceof RestClientException
                || throwable instanceof IOException
                || throwable instanceof TimeoutException;
    }
}
