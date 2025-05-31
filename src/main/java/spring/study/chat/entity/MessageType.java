package spring.study.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {
    ENTER("enter"), TALK("talk"), IMAGE("image"), QUIT("quit");

    private String value;
}
