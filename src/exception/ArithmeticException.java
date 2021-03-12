package exception;

public class ArithmeticException extends RuntimeException{
    public ArithmeticException() {
        super();
    }

    public ArithmeticException(String msg) {
        super(msg);
    }
}
