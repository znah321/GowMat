package interpreter.parser;

import GowMat.*;
import exception.SyntaxErrorException;
import exception.VariableNotFoundException;
import interpreter.lexer.Lexer;
import interpreter.lexer.Token;
import interpreter.lexer.Type;

import java.util.*;
import java.util.regex.Pattern;

public class Parser {
    private Lexer lexer = null;
    private Map<String, Object> varPool = new HashMap<String, Object>(); // 变量池，key为变量名，value为变量值
    private Map<String, Object> lite_varPool = new HashMap<String, Object>(); // 字面量变量池
    private Map<String, Object> t_varPool = new HashMap<String, Object>(); // 临时变量池
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
        List<Token> l_token = new ArrayList<Token>(); // 当前行的Token列表
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
                if (l_token.get(i).getContent().equals("=")) {
                    eval_pos = i;
                    break;
                }
            }
            /*
                然后处理等号后面的式子，使用逆波兰表示法来将已知的中缀表达式转成后缀表达式
                一、优先级设置
                    类型                           优先级
                    内置函数                         1
                    , ;                             2
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
            List<Token> infix_expr = new ArrayList<Token>();
            for(int i = eval_pos + 1; i < l_token.size(); i++)
                infix_expr.add(l_token.get(i).clone());
            List<Object> suffix_expr = this.infix_to_suffix(infix_expr);
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
    public List<Object> infix_to_suffix(List<Token> expr){
        List<Object> suffix_expr = new ArrayList<Object>(); // 转换结果
        Stack<Token> s = new Stack<>();
        boolean flag = false; // false代表形式(1)，true代表形式(2)
        for(int i = 0; i < expr.size(); i++) {
            Token t = expr.get(i).clone(); // 当前Token
            /* token是标识符（变量） */
            if (t.isIdtf()) {
                flag = true;
                suffix_expr.add(t);
                continue;
            } else if (t.isLiteNum() || t.isLiteString()) { ////////////////////////////////字符串重新写
                /* token是字面量 */
                suffix_expr.add(t);
                continue;
            } else {
                /* token是操作符/分隔符/关键字 */
                /* 处理矩阵字面量 */
                if (flag == false && t.getContent().equals("[")) {
                    /* 形式(1) */
                    // Step 1-找到"]"的位置
                    int pos = i+1;
                    String mat_expr = "[";
                    for(; ; pos++) {
                        if (expr.get(pos).getContent().equals("]"))
                            break;
                        else if (Pattern.matches(";|,|[+-]?\\d+(\\.\\d+)?", expr.get(pos).getContent()))
                            mat_expr += expr.get(pos).getContent();
                        else {
                            throw new SyntaxErrorException("矩阵输入格式错误！");
                        }
                    }
                    mat_expr += "]";
                    i = pos; // 从"]"的下一个位置开始扫描
                    // 把这个字面量添加到字面量变量池中
                    String name = this.geneName();
                    Token new_t = new Token(Type.mat_literals, mat_expr, expr.get(0).getLine());
                    this.lite_varPool.put(name, new_t);
                    suffix_expr.add(new_t);
                } else if (flag == true && t.getContent().equals("(")) {
                    /* 形式(2) */
                    flag = false;
                    String varName = expr.get(i-1).getContent();
                    String r_expr = "["; // 行的取值
                    String c_expr = "["; // 列的取值
                    int pos = i+2;
                    for(; ; pos++) {
                        if (expr.get(pos).getContent().equals("]"))
                            break;
                        else if (Pattern.matches(";|,|:|[+-]?\\d+(\\.\\d+)?", expr.get(pos).getContent()))
                            r_expr += expr.get(pos).getContent();
                        else {
                            String msg = "\n\t第" + expr.get(0).getLine() + "行：" + "矩阵输入格式错误，请检查输入！";
                            System.out.println(new SyntaxErrorException(msg));
                        }
                    }
                    r_expr += "]";
                    pos = pos + 3; //跳过",["
                    for(;; pos++) {
                        if (expr.get(pos).getContent().equals("]"))
                            break;
                        else if (Pattern.matches(";|,|:|[+-]?\\d+(\\.\\d+)?", expr.get(pos).getContent()))
                            c_expr += expr.get(pos).getContent();
                        else {
                            String msg = "\n\t第" + expr.get(0).getLine() + "行：" + "矩阵输入格式错误，请检查输入！";
                            System.out.println(new SyntaxErrorException(msg));
                        }
                    }
                    c_expr += "]";
                    i = pos + 2; // 从")"的下一个位置开始扫描
                    if (this.varPool.containsKey(varName)) {
                        Matrix m = ((Matrix) this.varPool.get(varName)).submatrix(r_expr, c_expr);
                        String name = this.geneName();
                        Token new_t = new Token(Type.mat_literals, m.toToken(), expr.get(0).getLine());
                        this.lite_varPool.put(name, new_t);
                        suffix_expr.add(new_t);
                    } else {
                        String msg = "\n\t第" + expr.get(0).getLine() + "行：" + "变量" + varName + "不存在，请检查输入！";
                        System.out.println(new VariableNotFoundException(msg));;
                    }
                } else if (Pattern.matches("\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*|\\(|\\)", t.getContent()) || t.isKey() || t.isSep()){
                    flag = false;
                    /* 按照优先级进行处理 */
                    if (t.getContent().equals("(")) // 是"("，直接入栈
                        s.push(t);
                    if (Pattern.matches("\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*", t.getContent()) || t.isKey() || t.isSep()) {
                        /*
                            是操作符：
                            while 栈非空
                                if 栈顶元素的优先级 < 当前元素的优先级
                                    栈顶元素出栈
                                end
                            end
                            当前元素入栈
                         */
                        while (!s.empty()) {
                            Token top = s.peek(); // 取栈顶元素
                            if (top.priority() < t.priority())
                                suffix_expr.add(s.pop());
                            else
                                break;
                        }
                        s.push(t);
                    }
                    if (t.getContent().equals(")")) { // 是")"，弹出"("前面所有的元素
                        while (!s.peek().getContent().equals("(") && !s.empty())
                            suffix_expr.add(s.pop());
                        /*
                            如果括号没对齐，就会少一个"("，导致一直执行pop操作，直至栈空
                            而对齐的时候，栈顶就正好是"("
                         */
                        if (s.empty()) { // 栈空就是没对齐的情况
                            String msg = "\n\t第" + expr.get(0).getLine() + "行：括号未对齐！";
                            throw new SyntaxErrorException(msg);
                        } else { // 栈非空就是正常的情况
                            s.pop(); // 弹出"("
                        }
                    }
                } else if (t.isEOL()) {
                    // 到达行的末尾，弹出栈中的所有元素（除了"("，")"）
                    int len = s.size();
                    for(int j = 0; j < len; j++)
                        suffix_expr.add(s.pop());
                    break; // 退出循环
                } else {
                    String msg = "\n\t第" + expr.get(0).getLine() + "行：存在未知的符号！";
                    throw new SyntaxErrorException(msg);
                }
            }
        }

        //test
        System.out.println(this.lexer.getSource_code().get(0));
        System.out.println("转后缀表达式：");
        for(int i = 0; i < suffix_expr.size(); i++)
            System.out.print(((Token) suffix_expr.get(i)).getContent() + " ");
        System.out.println("\ntranverse over");
        return suffix_expr;
    }

    // 后缀表达式求解
    public Token cal_suffix(List<Token> expr) {
        return null;
    }

    // 调用内置函数
    public static Token callFunc(Token key, Token[] argv) {
        return null;
    }

    // 生成字面量矩阵的Key
    public String geneName() {
        String name = "literals_" + String.valueOf(this.lite_varPool.size());
        return name;
    }

    /**
     * 添加一个变量到变量池varPool
     * @param t 变量名对应的Token
     * @param val 计算结果对应的字符串
     * @param val_type 计算结果对应的类型（string， num， mat）
     */
    public void add_varPool(Token t, String val, Type val_type) {
        Token new_t = null;
        if (val_type == Type.string) // 计算结果是字符串
            new_t = new Token(Type.num, val, t.getLine());
        else if (val_type == Type.num) // 计算结果是数字
            new_t = new Token(Type.string, val, t.getLine());
        else if (val_type == Type.mat) // 计算结果是矩阵
            new_t = new Token(Type.mat, val, t.getLine());
        this.varPool.put(t.getContent(), new_t);
    }


}
