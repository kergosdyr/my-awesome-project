package challenge.commerce.support;

public class BusinessException extends RuntimeException {
    public enum Reason {
        NOT_FOUND,
        CONFLICT,
        INVALID_INPUT
    }

    private final Reason reason;

    private BusinessException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(Reason.INVALID_INPUT, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(Reason.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(Reason.CONFLICT, message);
    }
}
