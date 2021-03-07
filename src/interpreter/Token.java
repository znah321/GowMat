package interpreter;

public class Token {
    protected enum tokenType {
        identifier, // 标识符：变量名之类的 a ab _aaa
        key, // 关键字：内置的函数名，以及if、else等
        operator, // 运算符：+ - * / % ^ .^ ./ .* ( ) [ ] , > < = >= <= || && | & :
        str_literals, // 字符串字面量 "hello gowmat"
        num_literals, // 数字字面量 1 2 34.5
        undefined, // 未定义
        separator, // 分隔符（逗号，分号）
        end_of_line, // 行末尾
        end_of_file; // 文件末尾
    }
    protected static final String EOL = "\\n"; // 行末尾
    protected static final Token EOF = new Token(tokenType.end_of_file, "EOF", -1); // 文件末尾
    private tokenType type = tokenType.undefined; // token类型
    private String content; // token内容
    private int line; // 行标

    public Token(tokenType t, String content, int line) {
        this.type = t;
        this.content = content;
        this.line = line;
    }

    public tokenType getType() {
        return type;
    }

    public void setType(tokenType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
