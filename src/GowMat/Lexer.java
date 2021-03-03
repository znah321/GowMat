package GowMat;

import java.util.regex.*;

public class Lexer {
    private static final String matrixPattern = "^[a-zA-Z_][\\w]*$";
    private static final String operatorPattern = "^[-*/+^%=()]$";
    private static final String keywordPattern = "^$";
    private static final String eolPattern = "^\\n$";
    private static final String test = "^,$";

    public static void main(String[] args) {
        System.out.println(Pattern.matches(test, ","));
    }
}
