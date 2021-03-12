package exception;

public class VariableTypeException extends RuntimeException{
    public VariableTypeException() {
        super();
    }

    public VariableTypeException(String msg) {
        super(msg);
    }
}
