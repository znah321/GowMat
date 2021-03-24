package interpreter.lexer;

import java.util.regex.Pattern;

public class Token {
    protected static final String EOL = "\\n"; // 行末尾
    protected static final Token EOF = new Token(Type.end_of_file, "EOF", -1); // 文件末尾
    private Type type = Type.undefined; // token类型
    private String content; // token内容
    private int line; // 行标

    public Token(Type t, String content, int line) {
        this.type = t;
        this.content = content;
        this.line = line;
    }

    // 获取Token优先级
    public int priority() {
        if (this.isKey())
            return 1;
        else if (this.isSep())
            return 4;
        else if (Pattern.matches("\\*|/|%|\\^|\\.\\^|\\./|\\.\\*", this.content))
            return 2;
        else if (Pattern.matches("\\+|-", this.content))
            return 3;
        else if (this.content == "(" || this.content == ")")
            return 5;
        else
            return 6;
    }

    // 克隆
    public Token clone() {
        return new Token(this.type, this.content, this.line);
    }

    // 判断Token类型
    public boolean isIdtf() {
        return this.type == Type.identifier;
    }

    public boolean isKey() {
        return this.type == Type.key;
    }

    public boolean isOptr() {
        return this.type == Type.operator;
    }

    public boolean isLiteString() {
        return this.type == Type.str_literals;
    }

    public boolean isLiteNum() {
        return this.type == Type.num_literals;
    }

    public boolean isLiteMat() {return this.type == Type.mat_literals;}

    public boolean isUndef() {
        return this.type == Type.undefined;
    }

    public boolean isSep() {
        return this.type == Type.separator;
    }

    public boolean isNum() {
        return this.type == Type.num;
    }

    public boolean isMat() {
        return this.type == Type.mat;
    }

    public boolean isString() {
        return this.type == Type.string;
    }

    public boolean isEOL() {
        return this.type == Type.end_of_line;
    }

    public boolean isEOF() {
        return this.type == Type.end_of_file;
    }

    public boolean isCustFunc() {return this.type == Type.custom_func; }

    // Setter and Getter
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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
