package interpreter.parser;

import gowmat.*;
import interpreter.lexer.Lexer;
import interpreter.lexer.Token;

import java.util.Stack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private Lexer lexer = null;
    private static Map<String, Matrix> varPool = new HashMap<String, Matrix>(); // 变量池
    private static Map<String, Object> t_varPool = new HashMap<String, Object>(); // 临时变量池
    private enum SentType { // 语句类型
        eval, // 赋值语句，有"="且没有"=="
        other; // 其他语句
    }

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    // 解释器
    public void parse() {
        List<Token> tokenList = this.lexer.getTokens();
        int start_ind = 1; // 起始行标
        /* 1-获取一行的Token */
        List<Token> l_token = null; // 当前行的Token列表
        for(int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).getLine() == start_ind)
                l_token.add(tokenList.get(i).clone());
            else
                break;
        }

        /* 2-确定语句类型 */
        SentType type = setSentType(this.lexer.getSource_code().get(start_ind-1));

        /* 3-处理这一行的Token（赋值语句的情况） */
        if (type == SentType.eval) {
            // 找到"="的位置
            int eval_pos = 0;
            for(int i = 0; i < l_token.size(); i++) {
                if (l_token.get(i).getContent() == "=") {
                    eval_pos = i;
                    break;
                }
            }
            /*
                然后处理等号后面的式子，使用逆波兰表示法来将已知的中缀表达式转成后缀表达式
                一、优先级设置
                    类型                           优先级
                    , ;                             1
                    内置函数                         2
                    * / % ^ .* ./ .^                3
                    + -                             4
                    括号( )                         5
                    其他                            6
                    由于矩阵字面量涉及到了"["、"]"，遇到这两个符号时，下面额外进行说明
                二、矩阵字面量的处理
                    我这里把矩阵字面量定义为：除去有且仅有矩阵名的情况之外的所有情况，有两种形式：
                        (1) [1,2,3;4,5,6]    ==>   给出一个矩阵的完整形式
                        (2) A([1:2:8],[2,3]) ==>   矩阵的取值/取子矩阵形式
                        (3) A(A是一个矩阵)    ==>   这里只有一个矩阵名A，没有涉及别的操作，不认为是矩阵字面量，认为是普通变量，跟数字字面量是类似的
                    最开始打算用正则表达式来匹配，但形式(1)和形式(2)显然是区分不了的。
                    由于扫描一个表达式是从左到右进行扫描的，因此当程序扫描到"["时，前面扫描到的数据会影响到后面怎么处理。
                    如果我先扫描到了一个变量名，下一个扫描到的是"("，那么就只能是形式(2)了。
                    要想"["对应的是形式(1)，"["前面就必须是运算符。

                    因此处理流程是这样的：
                        if 当前Token是标识符（变量名）
                            if 下一个Token是"("
                                // 形式(2)
                                设置一个字符串，存储"("到")"之间的Token内容（Matrix类中有对应取子矩阵函数)
                                取值，转换成普通变量
                            else if 下一个Token是运算符
                                按照正常流程来处理（判断优先级）
                            else
                                抛出异常(语法错误)
             */
            List<Token> infix_token = null;
            for(int i = eval_pos; i < l_token.size(); i++)
                infix_token.add(l_token.get(i).clone());
            List<Token> suffix_token = infix_to_suffix(infix_token);
        }

        ///////////////////////////////
        System.out.println("over");
    }

    // 设置语句类型
    public static SentType setSentType(String sentence) {
        if (sentence.contains("=") && !sentence.contains("=="))
            return SentType.eval;
        return SentType.other;
    }

    // 中缀表达式转后缀表达式
    public static List<Token> infix_to_suffix(List<Token> expr) {

    }

    // 后缀表达式求解
    public static List<Token> cal_suffix(List<Token> expr) {

    }

    // 调用内置函数
    public static Token callFunc(Token key, Token[] argv) {

    }


}
