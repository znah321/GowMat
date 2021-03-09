package interpreter.parser;

import com.sun.xml.internal.ws.api.ha.StickyFeature;
import interpreter.lexer.Token;

// Gowmat内置函数类
public class ParserFunc {
    private String func_name; // 函数名
    private String[] argv; // 函数参数

    // 屏幕打印函数
    protected static void print() {

    }

    // 求和函数
    protected static Token sum(Token t, int argv) {
        return null;
    }
}
