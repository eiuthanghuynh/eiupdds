package com.eiu.pdds.common.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitPropertiesTest {

    @Test
    @DisplayName("Should expose expected default rate limit values")
    void defaults_matchSpecification() {
        RateLimitProperties props = new RateLimitProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getTrustedProxies()).isEmpty();
        assertThat(props.getDiagnosisGuestLimit()).isEqualTo(3);
        assertThat(props.getDiagnosisGuestWindowSeconds()).isEqualTo(3600L);
        assertThat(props.getDiagnosisUserLimit()).isEqualTo(10);
        assertThat(props.getDiagnosisUserWindowSeconds()).isEqualTo(60L);
        assertThat(props.getAuthIpLimit()).isEqualTo(10);
        assertThat(props.getAuthIpWindowSeconds()).isEqualTo(60L);
    }

    @Test
    @DisplayName("Should return empty unmodifiable set when trustedProxies is null or blank")
    void trustedProxySet_nullOrBlank_returnsEmptyUnmodifiableSet() {
        RateLimitProperties props = new RateLimitProperties();

        props.setTrustedProxies(null);
        assertThat(props.trustedProxySet()).isEmpty();

        props.setTrustedProxies("   ,  , ");
        Set<String> set = props.trustedProxySet();
        assertThat(set).isEmpty();
        assertThatThrownBy(() -> set.add("127.0.0.1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should parse, trim, deduplicate, and return unmodifiable trusted proxy set")
    void trustedProxySet_csvInput_trimsAndDeduplicates() {
        RateLimitProperties props = new RateLimitProperties();
        props.setTrustedProxies(" 10.0.0.1 , 172.16.0.1, , 10.0.0.1 , 192.168.1.0/24 ");

        Set<String> set = props.trustedProxySet();

        assertThat(set).containsExactlyInAnyOrder("10.0.0.1", "172.16.0.1", "192.168.1.0/24");
        assertThatThrownBy(() -> set.add("1.2.3.4"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
