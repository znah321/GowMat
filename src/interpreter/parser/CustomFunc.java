package interpreter.parser;

import interpreter.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class CustomFunc {
    private String name; // 函数名
    private int argc_size; // 参数个数
    private List<Token> func_body = new ArrayList<>(); // 函数体
    private Token[] result; // 运算结果

    public CustomFunc(String name, int argc_size, List<Token> func_body) {
        this.name = name;
        this.argc_size = argc_size;
        this.func_body = func_body;
    }

    /* 运行函数 */
    public void run() {

    }

    /**
     * 判断是否为相同函数
     * 函数名、参数个数均相同，则认为是同一函数
     * @param func
     * @return
     */
    public boolean same_as(CustomFunc func) {
        if (this.name == func.name && this.argc_size == this.argc_size)
            return true;
        return false;
    }

    /**
     * 判断函数是否存在错误
     * @return
     */
    public boolean is_legal() {
        return false;
    }
}
