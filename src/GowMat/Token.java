package GowMat;

public class Token {
    public static final Token EOF = new Token(-1); // 结束标志
    public static final String EOL = "\\n"; // 行末尾标志
    private int lineNumber; // 行标
    private boolean isMatrix;
    private boolean isOperator;
    private boolean isKeyword;
    private boolean isEndOfLine;


    protected Token(int line) {
        this.lineNumber = line;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isMatrix() {
        return isMatrix;
    }

    public void setMatrix(boolean matrix) {
        isMatrix = matrix;
    }

    public boolean isOperator() {
        return isOperator;
    }

    public void setOperator(boolean operator) {
        isOperator = operator;
    }

    public boolean isKeyword() {
        return isKeyword;
    }

    public void setKeyword(boolean keyword) {
        isKeyword = keyword;
    }

    public boolean isEndOfLine() {
        return isEndOfLine;
    }

    public void setEndOfLine(boolean endOfLine) {
        isEndOfLine = endOfLine;
    }
}
