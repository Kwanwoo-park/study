package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;

public final class ClientIpResolver {
    private static final int MAX_IP_LENGTH = 45;

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";

        String remoteAddress = normalize(request.getRemoteAddr());
        if (remoteAddress == null) return "UNKNOWN";
        if (!isTrustedProxy(remoteAddress)) return limit(remoteAddress);

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String resolved = resolveForwardedFor(forwardedFor);
            if (resolved != null) return limit(resolved);
        }

        String realIp = request.getHeader("X-Real-IP");
        String normalizedRealIp = normalize(realIp);
        if (normalizedRealIp != null) return limit(normalizedRealIp);

        return limit(remoteAddress);
    }

    private static String resolveForwardedFor(String forwardedFor) {
        String[] addresses = forwardedFor.split(",");
        String leftmostValidAddress = null;
        for (int index = addresses.length - 1; index >= 0; index--) {
            String candidate = normalize(addresses[index]);
            if (candidate == null) continue;
            leftmostValidAddress = candidate;
            if (!isTrustedProxy(candidate)) return candidate;
        }
        return leftmostValidAddress;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = sanitize(value);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (!isIpLiteral(normalized)) return null;
        try {
            InetAddress.getByName(normalized);
            return normalized;
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isIpLiteral(String value) {
        if (value.indexOf(':') >= 0) return value.matches("[0-9a-fA-F:]+");
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("\\d{1,3}")) return false;
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    private static boolean isTrustedProxy(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || isUniqueLocalIpv6(address);
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private static String limit(String value) {
        return value.length() <= MAX_IP_LENGTH ? value : value.substring(0, MAX_IP_LENGTH);
    }
}
