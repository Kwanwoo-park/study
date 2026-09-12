package spring.study.chat.dto;

import java.util.List;

public record ChatRoomImagesResponse(List<ChatRoomImageResponse> images, Long nextCursor) { }
