package exception;

public class UnknownFunctionException extends RuntimeException{
    public UnknownFunctionException() {
    }

    public UnknownFunctionException(String message) {
        super(message);
    }
}
