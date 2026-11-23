package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;

public final class ClientIpResolver {
    public static final String RESOLVED_ADDRESS = ClientIpResolver.class.getName() + ".resolvedAddress";

    public static String resolve(HttpServletRequest request) {
        return resolve(request, ClientIpResolver::isTrustedProxy);
    }

    public static String resolve(HttpServletRequest request, Predicate<String> trustedProxy) {
        if (request == null) return "UNKNOWN";
        if (request.getAttribute(RESOLVED_ADDRESS) instanceof String resolvedAddress) return resolvedAddress;

        String remoteAddress = canonicalAddress(request.getRemoteAddr());

        if (remoteAddress == null)
            return "UNKNOWN";
        else if (!trustedProxy.test(remoteAddress))
            return remoteAddress;

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String resolved = resolveForwardedFor(forwardedFor, trustedProxy);

            if (resolved != null)
                return resolved;
        }

        String normalizedRealIp = canonicalAddress(request.getHeader("X-Real-IP"));

        return normalizedRealIp == null ? remoteAddress : normalizedRealIp;
    }

    private static String resolveForwardedFor(String forwardedFor, Predicate<String> trustedProxy) {
        String[] addresses = forwardedFor.split(",");
        String leftmostValidAddress = null;

        for (int index = addresses.length - 1; index >= 0; index--) {
            String candidate = canonicalAddress(addresses[index]);

            if (candidate == null)
                continue;

            leftmostValidAddress = candidate;

            if (!trustedProxy.test(candidate))
                return candidate;
        }

        return leftmostValidAddress;
    }

    public static String canonicalAddress(String value) {
        if (value == null || value.isBlank())
            return null;

        String normalized = value.strip();

        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (!isIpLiteral(normalized)) return null;

        try {
            return InetAddress.getByName(normalized).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    private static boolean isIpLiteral(String value) {
        if (value.indexOf(':') >= 0)
            return value.matches("[0-9a-fA-F:.]+");

        String[] parts = value.split("\\.", -1);

        if (parts.length != 4)
            return false;

        for (String part : parts) {
            if (!part.matches("\\d{1,3}"))
                return false;

            if (Integer.parseInt(part) > 255)
                return false;
        }

        return true;
    }

    private static boolean isTrustedProxy(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);

            return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || isUniqueLocalIpv6(address);
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
