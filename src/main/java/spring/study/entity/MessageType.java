package spring.study.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {
    ENTER("enter"), TALK("talk"), QUIT("quit");

    private String value;
}
