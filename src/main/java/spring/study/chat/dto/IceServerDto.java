package spring.study.chat.dto;

import java.util.List;

public record IceServerDto(List<String> urls, String username, String credential) {
    public static IceServerDto stun(String url) {
        return new IceServerDto(List.of(url), null, null);
    }

    public static IceServerDto turn(List<String> urls, String username, String credential) {
        return new IceServerDto(urls, username, credential);
    }
}
