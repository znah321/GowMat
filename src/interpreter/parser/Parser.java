package interpreter.parser;

import GowMat.*;
import GowMat.util.MatrixMath;
import exception.ArithmeticException;
import exception.SyntaxErrorException;
import exception.VariableNotFoundException;
import exception.VariableTypeException;
import interpreter.lexer.Lexer;
import interpreter.lexer.Token;
import interpreter.lexer.Type;

import java.util.*;
import java.util.regex.Pattern;

public class Parser {
    private Lexer lexer = null;
    private Map<String, Token> varPool = new HashMap<>(); // 变量池，key为变量名，value为变量值
    private Map<String, Token> lite_varPool = new HashMap<>(); // 字面量变量池
    private Map<String, Token> t_varPool = new HashMap<>(); // 域变量池
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
        int start_ind = 0; // 起始行标
        boolean reach_end = false; // 到达文件结尾的标志
        int start_pos = 0;
        while (!reach_end) {
            /* 1-获取一行的Token */
            List<Token> l_token = new ArrayList<Token>(); // 当前行的Token列表
            for (int i = start_pos; i < tokenList.size(); i++) {
                if (tokenList.get(i).getLine() == start_ind + 1)
                    l_token.add(tokenList.get(i).clone());
                else if (tokenList.get(i).isEOF())
                    reach_end = true;
                else
                    break;
            }

            /* 2-确定语句类型 */
            SentType type = setSentType(this.lexer.getSource_code().get(start_ind));
            System.out.println("--------------------------------第" + (start_ind + 1) + "行---------------------------");
            start_ind++;
            start_pos += l_token.size();

            /* 3-处理这一行的Token（赋值语句的情况） */
            if (type == SentType.eval) {
                // 找到"="的位置
                int eval_pos = 0;
                for (int i = 0; i < l_token.size(); i++) {
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
                /* 将中缀表达式转换成后缀表达式，并计算 */
                List<Token> infix_expr = new ArrayList<Token>();
                for (int i = eval_pos + 1; i < l_token.size(); i++)
                    infix_expr.add(l_token.get(i).clone());
                List<Token> suffix_expr = this.infix_to_suffix(infix_expr);
                Token res = this.cal_suffix(suffix_expr);
                /* 将计算结果赋给变量 */
                String varName = l_token.get(0).getContent();
                if (this.varPool.containsKey(varName))
                    this.varPool.get(varName).setContent(res.getContent());
                else {
                    Token t;
                    if (res.isLiteNum())
                        t = new Token(Type.num, this.getVarValue(res), l_token.get(0).getLine());
                    else
                        t = new Token(Type.mat, this.getVarValue(res), l_token.get(0).getLine());
                    this.varPool.put(varName, t);
                    ///////////////
                    System.out.print(varName + " = ");
                    if (this.varPool.get(varName).isNum())
                        System.out.println(t.getContent());
                    else {
                        System.out.println();
                        new Matrix(t.getContent()).display();
                    }
                }
                // 清空字面量变量池
                this.lite_varPool.clear();
            }
        }
        ///////////////////////////////
        System.out.println("calculate over");
    }

    // 设置语句类型
    public static SentType setSentType(String sentence) {
        if (sentence.contains("=") && !sentence.contains("=="))
            return SentType.eval;
        return SentType.other;
    }

    // 中缀表达式转后缀表达式
    public List<Token> infix_to_suffix(List<Token> expr){
        List<Token> suffix_expr = new ArrayList<Token>(); // 转换结果
        Stack<Token> s = new Stack<>();
        boolean flag = false; // false代表形式(1)，true代表形式(2)
        int line_idx = expr.get(0).getLine(); // 当前行标
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
                            String msg = "\n\t第" + line_idx + "行：" + "矩阵输入格式错误，请检查输入！";
                            throw new SyntaxErrorException(msg);
                        }
                    }
                    mat_expr += "]";
                    i = pos; // 从"]"的下一个位置开始扫描
                    // 把这个字面量添加到字面量变量池中
                    String name = this.geneName();
                    Token new_t = new Token(Type.mat_literals, mat_expr, line_idx);
                    this.lite_varPool.put(name, new_t);
                    suffix_expr.add(new Token(Type.mat_literals, name, line_idx));
                } else if (flag == true && t.getContent().equals("(")) {
                    /* 形式(2) */
                    flag = false;
                    String varName = expr.get(i-1).getContent();
                    String r_expr = "["; // 行的取值
                    String c_expr = "["; // 列的取值
                    int pos = i + 1;

                    /*
                        如果expr.get(pos)的Token对象是"["，说明是取子矩阵
                        如果expr.get(pos)的Token对象是数字，说明是取单个值
                     */
                    if (expr.get(pos).getContent().equals("[")) {
                        pos = i+2;
                        for(; ; pos++) {
                            if (expr.get(pos).getContent().equals("]"))
                                break;
                            else if (Pattern.matches(";|,|:|[+-]?\\d+(\\.\\d+)?", expr.get(pos).getContent()))
                                r_expr += expr.get(pos).getContent();
                            else {
                                String msg = "\n\t第" + line_idx + "行：" + "矩阵输入格式错误，请检查输入！";
                                throw new SyntaxErrorException(msg);
                            }
                        }
                        r_expr += "]";
                        pos = pos + 2; //跳过",["
                        for(;; pos++) {
                            if (expr.get(pos).getContent().equals("]") || expr.get(pos).getContent().equals(")"))
                                break;
                            else if (Pattern.matches(";|,|:|[+-]?\\d+(\\.\\d+)?", expr.get(pos).getContent()))
                                c_expr += expr.get(pos).getContent();
                            else {
                                String msg = "\n\t第" + line_idx + "行：" + "矩阵输入格式错误，请检查输入！";
                                throw new SyntaxErrorException(msg);
                            }
                        }
                        c_expr += "]";
                        if (expr.get(pos).getContent().equals(")"))
                            i = pos;
                        else
                            i = pos + 1; // 从")"的下一个位置开始扫描

                        // 赋值
                        /*
                            这一步是把后缀表达式末尾的元素删掉。
                            此时末尾的元素是一个变量名，由于这一步是取一个子矩阵，在取之前已经先把这把变量放到
                            了表达式里面，但实际要的不是这个变量，而是对应的子矩阵，因此要删掉
                         */
                        suffix_expr.remove(suffix_expr.size() - 1);
                        if (this.varPool.containsKey(varName)) {
                            Matrix m = (new Matrix(this.varPool.get(varName).getContent())).submatrix(r_expr, c_expr);
                            String name = this.geneName();
                            Token new_t = new Token(Type.mat_literals, m.toToken(), line_idx);
                            this.lite_varPool.put(name, new_t);
                            suffix_expr.add(new Token(Type.mat_literals, name, line_idx));
                        } else {
                            String msg = "\n\t第" + line_idx + "行：" + "变量" + varName + "不存在，请检查输入！";
                            throw new VariableNotFoundException(msg);
                        }
                    } else if (expr.get(pos).isLiteNum() || (expr.get(pos).isIdtf() && this.varPool.containsKey(getVarValue(expr.get(pos))))) {
                        pos = i + 1;
                        r_expr = expr.get(pos).getContent();
                        c_expr = expr.get(pos + 2).getContent();
                        i = pos + 3;

                        // 赋值
                        /*
                            这一步是把后缀表达式末尾的元素删掉。
                            此时末尾的元素是一个变量名，由于这一步是取一个子矩阵，在取之前已经先把这把变量放到
                            了表达式里面，但实际要的不是这个变量，而是对应的子矩阵，因此要删掉
                         */
                        suffix_expr.remove(suffix_expr.size() - 1);
                        if (this.varPool.containsKey(varName)) {
                            Matrix m = new Matrix(this.varPool.get(varName).getContent());
                            String name = this.geneName();
                            Token new_t = new Token(Type.num_literals, String.valueOf(m.elem(r_expr, c_expr)), line_idx);
                            this.lite_varPool.put(name, new_t);
                            suffix_expr.add(new Token(Type.num_literals, name, line_idx));
                        } else {
                            String msg = "\n\t第" + line_idx + "行：" + "矩阵输入格式错误，请检查输入！";
                            throw new SyntaxErrorException(msg);
                        }
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
                                if 栈顶元素的优先级 <= 当前元素的优先级
                                    栈顶元素出栈
                                end
                            end
                            当前元素入栈
                         */
                        while (!s.empty()) {
                            Token top = s.peek(); // 取栈顶元素
                            if (top.priority() <= t.priority())
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
        System.out.println(this.lexer.getSource_code().get(expr.get(0).getLine() - 1));
        System.out.println("转后缀表达式：");
        for(int i = 0; i < suffix_expr.size(); i++)
            System.out.print(suffix_expr.get(i).getContent() + " ");
        System.out.println();
        return suffix_expr;
    }

    // 后缀表达式求解
    public Token cal_suffix(List<Token> expr) {
        Token key = null; // 关键字Token（函数）
        List<Token> argv = new ArrayList<>(); // 函数的参数
        Stack<Object> optn_s = new Stack<>(); // 运算数的栈
        String t1 = null; // 左运算数
        String t2 = null; // 右运算数
        boolean has_comma = false; // 是否遇到了逗号，false代表没遇到，true代表遇到了
        List<Object> comma_res = new ArrayList<>(); // 逗号运算的结果
        int line_idx = expr.get(0).getLine();

        for(int i = 0; i < expr.size(); i++) {
            Token inst_t = expr.get(i).clone(); // 当前Token
            if (inst_t.isLiteNum() || inst_t.isLiteMat() || inst_t.isIdtf()) // 当前Token是数字/矩阵字面量、标识符（变量），直接入栈
                optn_s.push(inst_t);
            else if (inst_t.getContent().equals(",")) {
                // 当前Token是","，说明","前面的Token是函数的参数
                has_comma = true;
                if (optn_s.size() < 2) {
                    String msg = "\n\t第" + line_idx + "行：" + "请检查函数的参数！";
                    throw new SyntaxErrorException(msg);
                } else {
                    /*
                        弹出2个参数，放到comma_res中，再把comma_res放到栈里面
                     */
                    if (comma_res.isEmpty()) {
                        comma_res.add(optn_s.pop());
                        comma_res.add(0, optn_s.pop());
                    } else
                        comma_res.add(optn_s.pop());
                    optn_s.push(comma_res);
                }
            } else if (Pattern.matches("\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*", inst_t.getContent())) {
                /* 当前Token是九个运算符之一，弹出栈中两个元素进行计算 */
                // 操作数的赋值
                if (optn_s.size() < 2) {
                    String msg = "\n\t第" + line_idx + "行：" + "请检查运算符两边的运算数！";
                    throw new SyntaxErrorException(msg);
                } else {
                    if (optn_s.peek().getClass() != Token.class) {
                        String msg = "\n\t第" + line_idx + "行：" + "请检查运算符两边的运算数！";
                        throw new SyntaxErrorException(msg);
                    } else
                        t2 = this.getVarValue((Token) optn_s.pop());
                    if (optn_s.peek().getClass() != Token.class) {
                        String msg = "\n\t第" + line_idx + "行：" + "请检查运算符两边的运算数！";
                        throw new SyntaxErrorException(msg);
                    } else
                        t1 = this.getVarValue((Token) optn_s.pop());

                    Token _t_res = this.arithmetic(inst_t, t1, t2);
                    optn_s.push(_t_res); // 计算结果压入栈中
                }
            } else if (inst_t.isKey()) {
                if (has_comma) { // 有逗号，栈顶为ArrayList
                    if (!optn_s.empty()) {
                        if (optn_s.peek().getClass() == ArrayList.class) {
                            List _t_list = (ArrayList) optn_s.pop();
                            for (int j = 0; j < _t_list.size(); j++)
                                argv.add((Token) _t_list.get(j));
                        }
                    } else {
                        String msg = "\n\t第" + line_idx + "行：函数参数输入有误" + "！";
                        throw new VariableTypeException(msg);
                    }

                } else { // 没有逗号，栈顶为Token
                    if (optn_s.peek().getClass() != Token.class) {
                        String msg = "\n\t第" + line_idx + "行：函数参数输入有误" + "！";
                        throw new VariableTypeException(msg);
                    } else
                        argv.add((Token) optn_s.pop());
                }
                comma_res.clear();
                has_comma = false;

                // 调用函数计算
                Token res = null;
                ParserFunc func = new ParserFunc(this);
                String key_name = inst_t.getContent();
                switch (key_name) {
                    case "sum":
                        res = func.sum(inst_t, argv);
                        break;
                    case "eye":
                        res = func.eye(inst_t, argv);
                        break;
                    case "zeros":
                    case "ones":
                    case "random":
                    case "randi":
                        res = func.gene_a_matrix(inst_t, argv);
                        break;
                }
                argv.clear();
                optn_s.push(res);
            }
        }
        // 循环结束，计算结果就是栈顶的值
        if (optn_s.peek().getClass() == Token.class)
            return (Token) optn_s.pop();
        else {
            String msg = "\n\t第" + line_idx + "行：函数参数输入有误" + "！";
            throw new VariableTypeException(msg);
        }
    }

    // 获取变量值
    public String getVarValue(Token t) {
        if (t.isLiteNum()) {
            if (this.lite_varPool.containsKey(t.getContent()))
                return this.lite_varPool.get(t.getContent()).getContent();
            else
                return t.getContent();
        } else if (t.isLiteMat()) {
            return this.lite_varPool.get(t.getContent()).getContent();
        }
        else if (t.isIdtf()) {
            if (this.varPool.containsKey(t.getContent()))
                return this.varPool.get(t.getContent()).getContent();
            else {
                String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "不存在，请检查输入！";
                throw new VariableNotFoundException(msg);
            }
        } else {
            String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "类型错误，请检查输入！";
            throw new VariableTypeException(msg);
        }
    }

    /**
     * 基础运算
     * @param operator 运算符
     * @param t1 左运算数
     * @param t2 右运算数
     * @return 计算结果
     */
    public Token arithmetic(Token operator, String t1, String t2) {
        Token res = null;
        String msg = null;
        Object _t_res = null;
        boolean is_mat = false;
        if (t1.contains("[") && t2.contains("[")) { // 两个都是矩阵
            is_mat = true;
            String optr = operator.getContent(); // 运算符
            Matrix m1 = new Matrix(t1);
            Matrix m2 = new Matrix(t2);
            switch (optr) {
                case "+":
                    _t_res = m1.plus(m2);
                    break;
                case "-":
                    _t_res = m1.minus(m2);
                    break;
                case "*":
                    _t_res = m1.times(m2);
                    break;
                case  "/":
                    msg = "\n\t第" + operator.getLine() + "行：" + "不支持矩阵除法！";
                    throw new ArithmeticException(msg);
                case "%":
                    msg = "\n\t第" + operator.getLine() + "行：" + "不支持矩阵取余运算！";
                    throw new ArithmeticException(msg);
                case "^":
                    msg = "\n\t第" + operator.getLine() + "行：" + "\"^\"运算符使用错误！";
                    throw new ArithmeticException(msg);
                case ".*":
                    _t_res = m1.dot_times(m2);
                    break;
                case "./":
                    _t_res = m1.dot_divide(m2);
                    break;
                case ".^":
                    msg = "\n\t第" + operator.getLine() + "行：" + "\".^\"运算符使用错误！";
                    throw new ArithmeticException(msg);
            }
        } else if (t1.contains("[") || t2.contains("[")){ // 有一个是矩阵
            is_mat =  true;
            Matrix mat = null;
            double k = 0;
            if (t1.contains("[")) {
                mat = new Matrix(t1);
                k = Double.parseDouble(t2);
            } else {
                mat = new Matrix(t2);
                k = Double.parseDouble(t1);
            }
            switch (operator.getContent()) {
                case "+":
                    _t_res = mat.plus(k);
                    break;
                case "-":
                    _t_res = mat.minus(k).times(-1); // 顺序是反的，要加个负号
                    break;
                case "*":
                    _t_res = mat.times(k);
                    break;
                case "/":
                    if (t1.contains("[")) {
                        _t_res = mat.times(1 / k);
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "不能用一个数除以矩阵！";
                        throw new ArithmeticException(msg);
                    }
                case "^":
                    if (t1.contains("[")) {
                        Matrix _t_mat = (Matrix) mat.clone();
                        for(int i = 0; i < k - 1; k++)
                            _t_mat = _t_mat.times(mat);
                        _t_res = _t_mat;
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "\"^\"运算符使用错误！";
                        throw new ArithmeticException(msg);
                    }
                case "%":
                    msg = "\n\t第" + operator.getLine() + "行：" + "矩阵不支持取余运算！";
                    throw new ArithmeticException(msg);
                case ".*":
                    if (t1.contains("[")) {
                        _t_res = mat.times(k);
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "\".*\"运算符使用错误！";
                        throw new ArithmeticException(msg);
                    }
                case "./":
                    if (t1.contains("[")) {
                        _t_res = mat.times(1/k);
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "\"./\"运算符使用错误！";
                        throw new ArithmeticException(msg);
                    }
                case ".^":
                    if (t1.contains("[")) {
                        _t_res = MatrixMath.pow(mat, k);
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "\".^\"运算符使用错误！";
                        throw new ArithmeticException(msg);
                    }
            }
        } else { // 两个都是数字
            double num1 = Double.parseDouble(t1);
            double num2 = Double.parseDouble(t2);
            switch (operator.getContent()) {
                case "+":
                    _t_res = num1 + num2;
                    break;
                case "-":
                    _t_res = num1 - num2; // 顺序是反的，要加个负号
                    break;
                case "*":
                case ".*":
                    _t_res = num1 * num2;
                    break;
                case "/":
                case "./":
                    if (num2 != 0 && num2 >= 1e-10) {
                        _t_res = num1 / num2;
                        break;
                    } else {
                        msg = "\n\t第" + operator.getLine() + "行：" + "除数不能为0！";
                        throw new ArithmeticException(msg);
                    }
                case "^":
                case ".^":
                    _t_res = Math.pow(num1, num2);
                    break;
                case "%":
                    _t_res = num1 % num2;
                    break;
            }
        }
        // 构造返回值的Token对象
        String name = null;
        if (is_mat) {
            name = this.geneName();
            res = new Token(Type.mat_literals, ((Matrix) _t_res).toToken(), operator.getLine());
            this.lite_varPool.put(name, res);
            return new Token(Type.mat_literals, name, operator.getLine());
        } else {
            name = this.geneName();
            res = new Token(Type.num_literals, _t_res.toString(), operator.getLine());
            this.lite_varPool.put(name, res);
            return new Token(Type.num_literals, name, operator.getLine());
        }
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

    public Lexer getLexer() {
        return lexer;
    }

    public Map<String, Token> getVarPool() {
        return varPool;
    }

    public Map<String, Token> getLite_varPool() {
        return lite_varPool;
    }

    public Map<String, Token> getT_varPool() {
        return t_varPool;
    }
}
