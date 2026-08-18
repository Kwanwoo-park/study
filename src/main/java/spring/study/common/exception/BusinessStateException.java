package spring.study.common.exception;

public class BusinessStateException extends IllegalStateException {
    public BusinessStateException(String message) {
        super(message);
    }
}
