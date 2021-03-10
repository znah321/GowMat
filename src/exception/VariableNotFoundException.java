package exception;

public class VariableNotFoundException extends RuntimeException{
    public VariableNotFoundException() {
        super();
    }

    public VariableNotFoundException(String msg) {
        super(msg);
    }
}
