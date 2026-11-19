package com.eiu.pdds.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private RateLimitProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        resolver = new ClientIpResolver(properties);
    }

    @Test
    @DisplayName("Should return normalized remoteAddr when trusted-proxies is empty")
    void resolve_emptyTrustedProxies_returnsRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Should ignore forged X-Forwarded-For when remoteAddr is not a trusted proxy")
    void resolve_untrustedProxy_ignoresForwardedHeader() {
        properties.setTrustedProxies("10.0.0.1, 172.16.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.99");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.99");
    }

    @Test
    @DisplayName("Should extract single IP from X-Forwarded-For when remoteAddr is trusted")
    void resolve_trustedProxy_singleForwardedIp_extractsClientIp() {
        properties.setTrustedProxies("10.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Should extract first IP from comma-separated X-Forwarded-For chain when remoteAddr is trusted")
    void resolve_trustedProxy_forwardedChain_extractsFirstIp() {
        properties.setTrustedProxies("10.0.0.1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Should canonicalize IPv6 loopback variants identically when matching trusted proxies")
    void resolve_ipv6Canonicalization_matchesAcrossCompactAndExpandedForms() {
        properties.setTrustedProxies("::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        request.addHeader("X-Forwarded-For", "[2001:db8::1], 10.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8:0:0:0:0:0:1");

        MockHttpServletRequest directIpv6Request = new MockHttpServletRequest();
        directIpv6Request.setRemoteAddr("::1");
        assertThat(resolver.resolve(directIpv6Request)).isEqualTo("0:0:0:0:0:0:0:1");
    }

    @Test
    @DisplayName("Should strip IPv6 zone suffixes and match trusted proxies safely")
    void resolve_ipv6WithZoneSuffix_normalizesSafely() {
        properties.setTrustedProxies("[::1]");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("0:0:0:0:0:0:0:1%0");
        request.addHeader("X-Forwarded-For", "fe80::1%eth0");

        assertThat(resolver.resolve(request)).isEqualTo("fe80:0:0:0:0:0:0:1");
    }

    @Test
    @DisplayName("Should handle malformed X-Forwarded-For and blank entries safely without DNS lookups")
    void resolve_malformedOrBlankEntries_handledSafely() {
        properties.setTrustedProxies("10.0.0.1, invalid-proxy.example, 192.168.0.0/24");

        MockHttpServletRequest hostnameHeader = new MockHttpServletRequest();
        hostnameHeader.setRemoteAddr("10.0.0.1");
        hostnameHeader.addHeader("X-Forwarded-For", "evil.com");
        assertThat(resolver.resolve(hostnameHeader)).isEqualTo("10.0.0.1");

        MockHttpServletRequest hostnameBeforeIp = new MockHttpServletRequest();
        hostnameBeforeIp.setRemoteAddr("10.0.0.1");
        hostnameBeforeIp.addHeader("X-Forwarded-For", "evil.com, 203.0.113.5");
        assertThat(resolver.resolve(hostnameBeforeIp)).isEqualTo("10.0.0.1");

        MockHttpServletRequest outOfRangeOctets = new MockHttpServletRequest();
        outOfRangeOctets.setRemoteAddr("10.0.0.1");
        outOfRangeOctets.addHeader("X-Forwarded-For", "999.999.999.999");
        assertThat(resolver.resolve(outOfRangeOctets)).isEqualTo("10.0.0.1");

        MockHttpServletRequest ipWithPort = new MockHttpServletRequest();
        ipWithPort.setRemoteAddr("10.0.0.1");
        ipWithPort.addHeader("X-Forwarded-For", "203.0.113.5:8080");
        assertThat(resolver.resolve(ipWithPort)).isEqualTo("10.0.0.1");

        MockHttpServletRequest malformedColons = new MockHttpServletRequest();
        malformedColons.setRemoteAddr("10.0.0.1");
        malformedColons.addHeader("X-Forwarded-For", ":::::");
        assertThat(resolver.resolve(malformedColons)).isEqualTo("10.0.0.1");

        MockHttpServletRequest onlyBlanks = new MockHttpServletRequest();
        onlyBlanks.setRemoteAddr("10.0.0.1");
        onlyBlanks.addHeader("X-Forwarded-For", "  ,   ");
        assertThat(resolver.resolve(onlyBlanks)).isEqualTo("10.0.0.1");

        MockHttpServletRequest leadingBlankEntry = new MockHttpServletRequest();
        leadingBlankEntry.setRemoteAddr("10.0.0.1");
        leadingBlankEntry.addHeader("X-Forwarded-For", ", 203.0.113.5");
        assertThat(resolver.resolve(leadingBlankEntry)).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("Should return unknown when remoteAddr is null, blank, or invalid")
    void resolve_nullOrBlankRemoteAddr_returnsUnknown() {
        MockHttpServletRequest nullRemote = new MockHttpServletRequest();
        nullRemote.setRemoteAddr(null);
        assertThat(resolver.resolve(nullRemote)).isEqualTo("unknown");

        MockHttpServletRequest blankRemote = new MockHttpServletRequest();
        blankRemote.setRemoteAddr("   ");
        assertThat(resolver.resolve(blankRemote)).isEqualTo("unknown");

        MockHttpServletRequest invalidRemote = new MockHttpServletRequest();
        invalidRemote.setRemoteAddr("evil.com");
        assertThat(resolver.resolve(invalidRemote)).isEqualTo("unknown");

        assertThat(resolver.resolve(null)).isEqualTo("unknown");
    }
}
