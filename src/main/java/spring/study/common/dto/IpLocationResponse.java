package spring.study.common.dto;

public record IpLocationResponse(
        String country,
        String region,
        String city,
        String displayName,
        boolean available
) {
    public static IpLocationResponse internal() {
        return new IpLocationResponse(null, null, null, "내부 네트워크", false);
    }

    public static IpLocationResponse unknown() {
        return new IpLocationResponse(null, null, null, "지역 확인 불가", false);
    }
}
