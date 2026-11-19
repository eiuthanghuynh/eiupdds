package com.eiu.pdds.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves the effective client IP address from an {@link HttpServletRequest},
 * honoring {@code X-Forwarded-For} only when the immediate peer IP belongs to
 * the configured trusted proxy set.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    static final String UNKNOWN_IP = "unknown";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");
    private static final Pattern IPV6_ZONE_ID = Pattern.compile("^[0-9a-zA-Z._-]+$");

    private final RateLimitProperties properties;

    /**
     * Resolves and canonicalizes the client IP literal, falling back to {@code "unknown"}
     * when {@code remoteAddr} is missing or malformed.
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }

        String remoteIp = normalizeIpLiteral(request.getRemoteAddr());
        if (remoteIp == null) {
            return UNKNOWN_IP;
        }

        Set<String> trustedProxies = properties.trustedProxySet().stream()
                .map(this::normalizeIpLiteral)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        if (!trustedProxies.contains(remoteIp)) {
            return remoteIp;
        }

        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] tokens = forwardedFor.split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    String clientIp = normalizeIpLiteral(trimmed);
                    return clientIp != null ? clientIp : remoteIp;
                }
            }
        }

        return remoteIp;
    }

    /**
     * Parses and canonicalizes a literal IPv4 or IPv6 address without performing DNS lookups.
     */
    String normalizeIpLiteral(String raw) {
        if (raw == null) {
            return null;
        }
        String candidate = raw.trim();
        if (candidate.isEmpty()) {
            return null;
        }

        if (candidate.startsWith("[") && candidate.endsWith("]") && candidate.contains(":")) {
            candidate = candidate.substring(1, candidate.length() - 1);
            if (candidate.isEmpty()) {
                return null;
            }
        }

        if (candidate.contains(":")) {
            int zoneIdx = candidate.indexOf('%');
            if (zoneIdx >= 0) {
                if (zoneIdx == 0) {
                    return null;
                }
                String zone = candidate.substring(zoneIdx + 1);
                if (!IPV6_ZONE_ID.matcher(zone).matches()) {
                    return null;
                }
                candidate = candidate.substring(0, zoneIdx);
            }
        }

        byte[] addressBytes = candidate.contains(":")
                ? parseIpv6Bytes(candidate)
                : parseIpv4Bytes(candidate);
        if (addressBytes == null) {
            return null;
        }

        try {
            return InetAddress.getByAddress(addressBytes).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static byte[] parseIpv4Bytes(String candidate) {
        if (!IPV4_LITERAL.matcher(candidate).matches()) {
            return null;
        }
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int value = Integer.parseInt(octets[i]);
            if (value > 255) {
                return null;
            }
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    private static byte[] parseIpv6Bytes(String candidate) {
        if (!candidate.contains(":") || !IPV6_LITERAL.matcher(candidate).matches()) {
            return null;
        }
        int doubleColonIndex = candidate.indexOf("::");
        boolean hasDoubleColon = doubleColonIndex >= 0;
        if (hasDoubleColon && doubleColonIndex != candidate.lastIndexOf("::")) {
            return null;
        }
        if (candidate.contains(":::")) {
            return null;
        }

        String leftPart = hasDoubleColon ? candidate.substring(0, doubleColonIndex) : candidate;
        String rightPart = hasDoubleColon ? candidate.substring(doubleColonIndex + 2) : "";

        List<Integer> leftGroups = new ArrayList<>(8);
        List<Integer> rightGroups = new ArrayList<>(8);

        if (!leftPart.isEmpty() && !parseIpv6Part(leftPart, leftGroups, !hasDoubleColon)) {
            return null;
        }
        if (!rightPart.isEmpty() && !parseIpv6Part(rightPart, rightGroups, true)) {
            return null;
        }

        int totalGroups = leftGroups.size() + rightGroups.size();
        if (hasDoubleColon ? totalGroups >= 8 : totalGroups != 8) {
            return null;
        }

        int zeroFillGroups = 8 - totalGroups;
        byte[] bytes = new byte[16];
        int byteIdx = 0;
        for (int group : leftGroups) {
            bytes[byteIdx++] = (byte) (group >>> 8);
            bytes[byteIdx++] = (byte) group;
        }
        byteIdx += zeroFillGroups * 2;
        for (int group : rightGroups) {
            bytes[byteIdx++] = (byte) (group >>> 8);
            bytes[byteIdx++] = (byte) group;
        }
        return bytes;
    }

    private static boolean parseIpv6Part(String part, List<Integer> target, boolean allowTrailingIpv4) {
        String[] segments = part.split(":", -1);
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                return false;
            }
            if (segment.contains(".")) {
                if (!allowTrailingIpv4 || i != segments.length - 1) {
                    return false;
                }
                byte[] ipv4 = parseIpv4Bytes(segment);
                if (ipv4 == null) {
                    return false;
                }
                target.add(((ipv4[0] & 0xFF) << 8) | (ipv4[1] & 0xFF));
                target.add(((ipv4[2] & 0xFF) << 8) | (ipv4[3] & 0xFF));
            } else {
                if (segment.length() > 4) {
                    return false;
                }
                for (int c = 0; c < segment.length(); c++) {
                    if (Character.digit(segment.charAt(c), 16) < 0) {
                        return false;
                    }
                }
                target.add(Integer.parseInt(segment, 16));
            }
        }
        return true;
    }
}
