package interpreter.parser;

import GowMat.Matrix;
import GowMat.util.MatrixMath;
import exception.ArithmeticException;
import exception.SyntaxErrorException;
import exception.VariableNotFoundException;
import exception.VariableTypeException;
import interpreter.lexer.Lexer;
import interpreter.lexer.Token;
import interpreter.lexer.Type;
import interpreter.parser.util.ParserUtil;

import java.sql.Struct;
import java.util.*;
import java.util.regex.Pattern;

public class Parser {
    private Lexer lexer;
    private List<Token> tokenList;
    private Map<String, Token> varPool = new HashMap<>(); // 变量池，key为变量名，value为变量值
    private Map<String, Token> lite_varPool = new HashMap<>(); // 字面量变量池
    private Map<String[], CustomFunc> customFunc = new HashMap<>(); // 自定义函数，key为[函数名，参数个数]

    private boolean reach_logic_loop; // 是否遇到了if、elseif、for、while
    protected enum SentType { // 语句类型
        eval, // 赋值语句，有"="且没有"=="
        other; // 其他语句
    }

    /**
     * 用于主函数的运行时构造Parser
     * @param lexer
     */
    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.tokenList = this.lexer.getTokens();
    }

    /**
     * 用于自定义函数运行时构造Parser
     * @param tokenList
     */
    public Parser(List<Token> tokenList) {
        this.tokenList = tokenList;
    }

    /**
     * 解释器
     */
    public void parse() {
        this.tokenList = this.scan_customFunc(this.tokenList);
        int start_ind = 1; // 起始行标
        boolean reach_end = false; // 到达文件结尾的标志
        while (!reach_end) {
            /* 1-获取一行的Token */
            List<Token> l_token = new ArrayList<>(); // 当前行的Token列表
            l_token = ParserUtil.get_line_tokens(this.tokenList, start_ind);
            /* 排除异常情况：l_token里面只有一个EOL或l_token为空
                导致这种情况的原因是出现了空行
             */
            if ((l_token.size() == 1 && l_token.get(0).isEOL()) || l_token.isEmpty()) {
                start_ind++;
                continue;
            }
            if (l_token.get(0).isEOF())
                reach_end = true;


            /* 2-确定语句类型 */
            SentType type = setSentType(l_token);
            System.out.println("--------------------------------第" + (start_ind) + "行---------------------------");
            start_ind++;

            /* 3-处理这一行的Token（赋值语句的情况） */
            String[] returns = null; // 用于处理多返回值的情况
            if (type == SentType.eval) {
                this.reach_logic_loop = false;
                // 找到"="的位置
                int eval_pos = 0;
                for (int i = 0; i < l_token.size(); i++) {
                    if (l_token.get(i).getContent().equals("=")) {
                        eval_pos = i;
                        break;
                    }
                }
                if (eval_pos == 0) {
                    String msg = "\n\t第" + l_token.get(0).getLine() + "行：" + "请检查等号的左边是否有变量！";
                    throw new SyntaxErrorException(msg);
                }

                // 统计返回值个数
                int returns_cnt = 0;
                if (eval_pos == 1)
                    returns_cnt = 1;
                else {
                    // Step 1-拼接"="前面的字符串
                    StringBuffer returns_string = new StringBuffer();
                    for(int i = 0; i < eval_pos; i++)
                        returns_string.append(l_token.get(i).getContent());
                    // Step 2-判断返回值格式是否符合语法。如果不符合就抛出异常
                    String regexPat = "\\[([a-zA-Z_][a-zA-Z_0-9]*)(,[\\s]*[a-zA-Z_][a-zA-Z_0-9]*)*\\]";
                    if (!Pattern.matches(regexPat, returns_string)) {
                        String msg = "\n\t第" + l_token.get(0).getLine() + "行：" + "返回值格式错误！";
                        throw new SyntaxErrorException(msg);
                    } else {
                        returns_string = returns_string.deleteCharAt(returns_string.length() - 1).deleteCharAt(0); // 去掉"["、"]"
                        returns = returns_string.toString().split(",");
                        returns_cnt = returns.length;
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
                Token[] res = this.cal_suffix(suffix_expr, SentType.eval, returns_cnt);
                /* 将计算结果赋给变量 */
                // 1-返回值为单个变量
                if (returns_cnt == 1) {
                    this.eval(l_token.get(0), res[0]);
                // 2-返回值为多个变量
                } else {
                    int cnt = 0;
                    for(int i = 0; i < eval_pos; i++) {
                        // 找到变量对应的Token位置
                        if (l_token.get(i).getContent().equals(returns[cnt])) {
                            this.eval(l_token.get(i), res[cnt]);
                            cnt++;
                        }
                        if (cnt == returns_cnt)
                            break;
                    }
                }
                // 清空字面量变量池
                this.lite_varPool.clear();

            } else {
                /* 其他类型语句的情况 */
                Token key = l_token.get(0); // 第一个Token一定是关键字
                ParserFunc func = new ParserFunc(this, 0);
                if (!l_token.get(0).equals("if") && !l_token.get(0).equals("elseif"))
                    this.reach_logic_loop = false;
                switch (key.getContent()) {
                    case "print":
                        List<Token> suffix_expr = this.infix_to_suffix(l_token);
                        Token res = this.cal_suffix(suffix_expr, SentType.other, 0)[0];
                        func.run(key, res);
                        break;
                    case "if":
                    case "elseif":
                        this.reach_logic_loop = true;
                        /*
                            Step-1: 检查end是否对齐
                                多出来的end会单独当做异常进行处理，因此不需要考虑end有多余的情况
                         */
                        int end_cnt = 0; // end的计数
                        int end_need = 1; // end需要的个数
                        int pos = ParserUtil.getIndex(this.tokenList, key) + 1;
                        while (pos < this.tokenList.size() && end_cnt != end_need) {
                            Token inst_t = this.tokenList.get(pos);
                            if (Pattern.matches("if|while|for", inst_t.getContent()))
                                end_need++;
                            if (inst_t.getContent().equals("end"))
                                end_cnt++;
                            pos++;
                        }
                        if (pos == this.tokenList.size()) {
                            String msg = "\n\t第" + l_token.get(0).getLine() + "行：" + "缺少end！";
                            throw new SyntaxErrorException(msg);
                        }

                        /* Step-2: 获取需要进行判断的条件 */
                        String logic_regex = "&&|&|\\||\\|\\|";
                        // 1-统计逻辑关系符的个数及类型
                        List<String> logic_optr = new ArrayList<>();
                        for(Token token : l_token) {
                            if (Pattern.matches(logic_regex, token.getContent()))
                                logic_optr.add(token.getContent());
                        }

                        // 2-根据逻辑关系符进行切割，获取条件
                        List<List<Token>> raw_conditions = new ArrayList<>();
                        for(int i = 1; i < l_token.size() - 1; i++) {
                            List<Token> t_condition = new ArrayList<>();
                            int j = i;
                            for (; j < l_token.size() - 1; j++) {
                                if (Pattern.matches(logic_regex, l_token.get(j).getContent()))
                                    break;
                                else
                                    t_condition.add(l_token.get(j).clone());
                            }
                            raw_conditions.add(t_condition);
                            i = j;
                        }


                        // 3-对条件进行加工，去掉多余的括号（直接进行切割的话，括号有可能没对齐，但一定是相差一个）
                        List<List<Token>> conditions = new ArrayList<>(); // 经处理后的条件
                        for(List<Token> cond : raw_conditions) {
                            String cond_string = ParserUtil.getString(cond);
                            int left_bracket_cnt = 0; // 左括号计数
                            int right_bracket_cnt = 0; // 右括号计数

                            left_bracket_cnt = cond_string.length() - cond_string.replaceAll("\\(", "").length();
                            right_bracket_cnt = cond_string.length() - cond_string.replaceAll("\\)", "").length();
                            if (Math.abs(left_bracket_cnt - right_bracket_cnt) > 1) {
                                String msg = "\n\t第" + l_token.get(0).getLine() + "行：" + "请检查括号是否对齐！";
                                throw new SyntaxErrorException(msg);
                            } else if (Math.abs(left_bracket_cnt - right_bracket_cnt) == 1) {
                                if (left_bracket_cnt < right_bracket_cnt) {
                                    cond.remove(cond.size() - 1);
                                    conditions.add(cond);
                                }
                                else {
                                    cond.remove(0);
                                    conditions.add(cond);
                                }
                            } else {
                                if (Math.abs(left_bracket_cnt - right_bracket_cnt) == 0)
                                    conditions.add(cond);
                            }
                        }

                        /* Step-3: 获取判断符，对每个条件根据判断符切割，分别计算两边的结果，判断是否成立 */
                        String success_regex = ">|<|=|>=|<=|==|~=";
                        boolean[] is_true = new boolean[conditions.size()]; // 每个条件的结果
                        pos = 0;
                        // 1-获取判断符
                        Token[] adjust_optr = new Token[conditions.size()];
                        for(Token token : l_token) {
                            if (Pattern.matches(success_regex, token.getContent()))
                                adjust_optr[pos++] = token;
                        }
                        pos = 0;

                        for(int i = 0; i < conditions.size(); i++) {
                            boolean logic_flag = false;
                            Token[] expr_res = new Token[2]; // 两边待计算的式子的结果
                            List<Token> cond = conditions.get(i);
                            int adjust_pos = ParserUtil.getIndex(cond, adjust_optr[i]); // 判断符出现的位置

                            // 2-拼接式子对应的Token列表
                            List<Token> left_expr_tokenlist = new ArrayList<>(); // 左边的
                            List<Token>  right_expr_tokenlist = new ArrayList<>(); // 右边的
                            for(int j = 0; j < cond.size(); j++) {
                                if (j < adjust_pos)
                                    left_expr_tokenlist.add(cond.get(j));
                                if (j > adjust_pos)
                                    right_expr_tokenlist.add(cond.get(j));
                            }

                            // 3-计算
                            expr_res[0] = this.cal_suffix(this.infix_to_suffix(left_expr_tokenlist), SentType.eval, 1)[0];
                            expr_res[1] = this.cal_suffix(this.infix_to_suffix(right_expr_tokenlist), SentType.eval, 1)[0];

                            // 4-判断当前成立
                            switch (adjust_optr[i].getContent()) {
                                case "==":
                                    if (this.equals(expr_res[0], expr_res[1]))
                                        is_true[i] = true;
                                    else
                                        is_true[i] = false;
                                    break;
                                case "~=":
                                    if (!this.equals(expr_res[0], expr_res[1]))
                                        is_true[i] = true;
                                    else
                                        is_true[i] = false;
                            }
                        }

                        /* Step-4: 计算最终结果 */
                        // 1-构造由true、false构成的中缀表达式
                        String infix = ParserUtil.getString(l_token, 1, l_token.size() - 1);
                        for(int i = 0; i < conditions.size(); i++) {
                            if (is_true[i])
                                infix = infix.replace(ParserUtil.getString(conditions.get(i)), "1");
                            else
                                infix = infix.replace(ParserUtil.getString(conditions.get(i)), "0");
                        }
                        infix = infix.replaceAll("&&", "&").replaceAll("\\|\\|", "\\|");

                        // 2-中缀表达式转后缀表达式
                        StringBuilder suffix = new StringBuilder();
                        Stack<Character> s = new Stack<>();
                        for(int i = 0; i < infix.length(); i++) {
                            if (infix.charAt(i) == '1' || infix.charAt(i) == '0')
                                suffix.append(infix.charAt(i));
                            else if (infix.charAt(i) == '(')
                                s.push(infix.charAt(i));
                             else if (infix.charAt(i) == ')') {
                                while (s.peek() != '(')
                                    suffix.append(s.pop());
                                s.pop();
                            } else {
                                 if (!s.empty() && s.peek() != '(')
                                    suffix.append(s.pop());
                                 s.push(infix.charAt(i));
                            }
                        }
                        while (!s.empty())
                            suffix.append(s.pop());

                        // 3-计算后缀表达式
                        boolean can_continue = false;
                        int c1, c2;
                        for(int i = 0; i < suffix.length(); i++) {
                            if (suffix.charAt(i) == '&' || suffix.charAt(i) == '|') {
                                c1 = suffix.charAt(i - 2) - 48;
                                c2 = suffix.charAt(i - 1) - 48;
                                switch (suffix.charAt(i)) {
                                    case '&':
                                        suffix = suffix.delete(0, i + 1);
                                        suffix.insert(0, c1 & c2);
                                    case '|':
                                        suffix = suffix.delete(0, i + 1);
                                        suffix.insert(0, c1 | c2);
                                }
                            }
                        }
                        if (suffix.charAt(0) == '0')
                            can_continue = false;
                        else
                            can_continue = true;

                        /* Step 5-判断是否可以继续运行 */
                        this.lite_varPool.clear();
                        for(List<Token> a :conditions)
                            System.out.println(ParserUtil.getString(a));
                        System.out.println("if_lalala");
                }
            }
        }
        ///////////////////////////////
        System.out.println("calculate over");
    }

    /**
     * 给一个变量赋值
     * @param var_token
     * @param res
     */
    public void eval(Token var_token, Token res) {
        String varName = var_token.getContent();
        if (this.varPool.containsKey(varName)) {
            this.varPool.get(varName).setContent(this.getVarValue(res));
            if (res.isLiteNum()) {
                this.varPool.get(varName).setType(Type.num);
            }
            else
                this.varPool.get(varName).setType(Type.mat);
        } else {
            Token t;
            if (res.isIdtf()) {
                Type type = this.varPool.get(res.getContent()).getType();
                t = new Token(type, this.getVarValue(res), var_token.getLine());
            }
            else if (res.isLiteNum() || res.isNum())
                t = new Token(Type.num, this.getVarValue(res), var_token.getLine());
            else if (res.isLiteMat() || res.isMat())
                t = new Token(Type.mat, this.getVarValue(res), var_token.getLine());
            else
                t = new Token(Type.string, this.getVarValue(res), var_token.getLine());
            this.varPool.put(varName, t);
            ///////////////
            System.out.print(varName + " = ");
            if (this.varPool.get(varName).isNum())
                System.out.println(t.getContent());
            else if (this.varPool.get(varName).isMat()){
                System.out.println();
                new Matrix(t.getContent()).display();
            }
        }
    }

    /**
     * 设置语句类型
      */
    public static SentType setSentType(List<Token> list) {
        if (list.get(0).isIdtf()) {
            if (list.get(1).getContent().equals("="))
                return SentType.eval;
            else {
                String msg = "\n\t第" + list.get(0).getLine() + "行：" + "语法错误！";
                throw new SyntaxErrorException(msg);
            }
        } else {
            for(Token item : list) {
                if (item.getContent().equals("="))
                    return SentType.eval;
            }
            return SentType.other;
        }
    }

    /**
     * 中缀表达式转后缀表达式
     * @param expr
     * @return
     */
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
                } else if (Pattern.matches("\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*|\\(|\\)", t.getContent()) || t.isKey() || t.isSep() || t.isCustFunc()){
                    flag = false;
                    /* 按照优先级进行处理 */
                    if (t.getContent().equals("(")) // 是"("，直接入栈
                        s.push(t);
                    if (Pattern.matches("\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*", t.getContent()) || t.isKey() || t.isSep() || t.isCustFunc()) {
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
        // 栈中可能还有元素（比如传入一个if的条件，是没有EOL的）
        while (!s.empty())
            suffix_expr.add(s.pop());

        //test
        if (this.lexer != null)
            System.out.println(this.lexer.getSource_code().get(expr.get(0).getLine() - 1));
        System.out.println("转后缀表达式：");
        for(int i = 0; i < suffix_expr.size(); i++)
            System.out.print(suffix_expr.get(i).getContent() + " ");
        System.out.println();
        return suffix_expr;
    }

    /**
     * 后缀表达式求解
     * @param expr
     * @param type
     * @return
     */
    public Token[] cal_suffix(List<Token> expr, SentType type, int returns_cnt) {
        Token key = null; // 关键字Token（函数）
        List<Token> argv = new ArrayList<>(); // 函数的参数
        Stack<Object> optn_s = new Stack<>(); // 运算数的栈
        String t1 = null; // 左运算数
        String t2 = null; // 右运算数
        boolean has_comma = false; // 是否遇到了逗号，false代表没遇到，true代表遇到了
        List<Object> comma_res = new ArrayList<>(); // 逗号运算的结果
        Token[] returns_token = null; // 返回值数组
        int line_idx = expr.get(0).getLine();

        for(int i = 0; i < expr.size(); i++) {
            Token inst_t = expr.get(i).clone(); // 当前Token
            if (inst_t.isLiteNum() || inst_t.isLiteMat() || inst_t.isIdtf()) // 当前Token是数字/矩阵，直接入栈
                optn_s.push(inst_t);
            else if (inst_t.isIdtf()) {
                /*
                    标识符要分情况处理：
                        --> 如果语句类型是eval，则不能出现字符串
                        --> 如果语句类型是other，则可以出现字符串，对字符串字面量同理
                 */
                if (type == SentType.eval) {
                    if (!this.varPool.get(inst_t.getContent()).isString())
                        optn_s.push(inst_t);
                    else if (type == SentType.eval && this.varPool.get(inst_t.getContent()).isString()) {
                        String msg = "\n\t第" + line_idx + "行：" + "不得出现字符串！";
                        throw new ArithmeticException(msg);
                    }
                } else
                    optn_s.push(inst_t);
            } else if (inst_t.isLiteString()) { // 字符串字面量的处理
                if (type == SentType.eval) {
                    String msg = "\n\t第" + line_idx + "行：" + "不得出现字符串！";
                    throw new ArithmeticException(msg);
                } else
                    optn_s.push(inst_t);
            } else if (inst_t.getContent().equals(",")) {
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

                    Token _t_res = null;
                    if (type == SentType.eval)
                        _t_res = this.arithmetic(inst_t, t1, t2);
                    if (type == SentType.other) {
                        if (inst_t.getContent().equals("+") && (Pattern.matches("\".*\"", t1) || Pattern.matches("\".*\"", t2))) {
                            // 去掉t1、t2两边的引号
                            if (t1.startsWith("\"") && t1.endsWith("\""))
                                t1 = t1.substring(1, t1.length() - 1);
                            if (t2.startsWith("\"") && t2.endsWith("\""))
                                t2 = t2.substring(1, t2.length() - 1);
                            String name = this.geneName();
                            _t_res = new Token(Type.str_literals, name, line_idx);
                            this.lite_varPool.put(name, new Token(Type.str_literals, "\"" + t1 + t2 + "\"", line_idx)); // 再把字符串拼接结果两边的引号加上去
                        } else {
                            if (!Pattern.matches("\".*\"", t1) && !Pattern.matches("\".*\"", t2))
                                _t_res = this.arithmetic(inst_t, t1, t2);
                            else {
                                String msg = "\n\t第" + line_idx + "行：" + "请检查运算符两边的运算数！";
                                throw new SyntaxErrorException(msg);
                            }
                        }
                    }
                    optn_s.push(_t_res); // 计算结果压入栈中
                }
            } else if (inst_t.isKey() ||inst_t.isCustFunc()) {
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
                Token t_res = null;

                String key_name = inst_t.getContent();
                /*
                    出现多个返回值的情况，那么后缀表达式末尾的Token一定是一个函数
                 */
                if (i != expr.size() - 1) {
                    if (inst_t.isKey())
                        t_res = this.callFunc(inst_t, argv, 0)[0];
                    else {
                        Token[] temp = this.callCustomFunc(inst_t, argv);
                        if (temp.length != 1) {
                            String msg = "\n\t第" + line_idx + "行：函数" + inst_t.getContent() +"的返回值过多！";
                            throw new SyntaxErrorException(msg);
                        } else
                            t_res = temp[0];
                    }
                    argv.clear();
                    optn_s.push(t_res);
                } else {
                    String regexPat = "print"; ///////////////////////////////////// 待补充
                    if (Pattern.matches(regexPat, inst_t.getContent())) {
                        returns_token = new Token[1];
                        returns_token[0] = argv.get(0);
                        return returns_token;
                    } else {
                        if (inst_t.isKey())
                            returns_token = this.callFunc(inst_t, argv, returns_cnt);
                        else
                            returns_token = this.callCustomFunc(inst_t, argv);
                        return returns_token;
                    }
                }
            }
        }
        // 循环结束，计算结果就是栈顶的值
        if (optn_s.peek().getClass() == Token.class) {
            returns_token = new Token[1];
            returns_token[0] = (Token) optn_s.pop();
            return returns_token;
        }
        else {
            String msg = "\n\t第" + line_idx + "行：函数参数输入有误" + "！";
            throw new VariableTypeException(msg);
        }
    }

    /**
     * 扫描代码中的自定义函数
     * @param origin_expr 经过词法分析的token列表
     * @return 删除自定义函数部分的token列表，用于正式的计算
     */
    public List<Token> scan_customFunc(List<Token> origin_expr) {
        int end_cnt = 0; // 统计"end"关键字出现的次数
        int end_need = 1; // "end"关键字需要出现的次数
        int line = 1;
        boolean flag = false;
        List<String> func_name = new ArrayList<>(); // 记录已经扫描到的函数名
        List<Token> func_expr = new ArrayList<>();
        List<Token> new_expr = new ArrayList<>();
        /* 扫描函数 */
        for(Token t : origin_expr) {
            if (t.getContent().equals("function")) {
                flag = true;
                line = t.getLine();
            }
            if (t.isEOF() && end_cnt != end_need && flag) {
                String msg = "\n\t第" + line + "行：自定义函数缺少end！";
                throw new SyntaxErrorException(msg);
            }

            if (flag) {
                if (end_cnt == end_need) {
                    flag = false;
                    func_expr.add(t);
                    // 把扫描到的函数放到custom_Func里面
                    /* 统计返回值个数 */
                    int returns_cnt = 0; // 返回值个数
                    StringBuffer returns_string = new StringBuffer();
                    int pos = 1;
                    while (!func_expr.get(pos).getContent().equals("=")) {
                        // 处理缺少等号的情况
                        if (func_expr.get(pos).isEOL()) {
                            String msg = "\n\t第" + line + "行：自定义函数缺少\"=\"！";
                            throw new SyntaxErrorException(msg);
                        }
                        returns_string.append(func_expr.get(pos++).getContent());
                    }
                    if (pos == 2) // 单返回值
                        returns_cnt = 1;
                    else { // 多返回值
                        String regexPat = "\\[([a-zA-Z_][a-zA-Z_0-9]*)(,[\\s]*[a-zA-Z_][a-zA-Z_0-9]*)*\\]";
                        if (!Pattern.matches(regexPat, returns_string)) {
                            String msg = "\n\t第" + func_expr.get(0).getLine() + "行：" + "返回值格式错误！";
                            throw new SyntaxErrorException(msg);
                        } else {
                            returns_string = returns_string.deleteCharAt(returns_string.length() - 1).deleteCharAt(0); // 去掉"["、"]"
                            returns_cnt = returns_string.toString().split(",").length;
                        }
                    }

                    // 检查定义的函数名是否和内置函数名冲突
                    String name = null;
                    if (!func_expr.get(pos+1).isKey()) {
                        name = func_expr.get(pos+1).getContent();
                    } else {
                        String msg = "\n\t第" + line + "行：" + func_expr.get(3).getContent() +"是内置函数，不能被定义为自定义函数！";
                        throw new SyntaxErrorException(msg);
                    }
                    func_name.add(name);

                    /* 统计参数个数 */
                    String argv_string = "";
                    int argc_size = 0;
                    for(int i = pos + 3; i < func_expr.size(); i++) {
                        if (func_expr.get(i).getContent().equals(")")) {
                            if (func_expr.get(i+2).getLine() == line) { // ")"后面就不能有任何字符了，减1是为了去掉每行末尾的换行符
                                String msg = "\n\t第" + line + "行：" + "自定义函数存在非法字符！";
                                throw new SyntaxErrorException(msg);
                            } else
                                break;
                        }
                        else
                            argv_string += func_expr.get(i).getContent();
                    }
                    String[] argv = argv_string.split(",");
                    // 判断一下参数是否符合输入格式
                    for (String argc : argv) {
                        if (!Pattern.matches("[a-zA-Z_][a-zA-Z_0-9]*", argc)) {
                            String msg = "\n\t第" + line + "行：" + "参数的形式不合法，存在非法字符！";
                            throw new SyntaxErrorException(msg);
                        }
                    }
                    argc_size = argv.length;

                    // 判断该函数是否已被定义
                    String[] inst_key = {name, String.valueOf(argc_size)};
                    if (containsKey(inst_key)) {
                        String msg = "\n\t第" + line + "行：" + "函数" + name + "已被定义！";
                        throw new SyntaxErrorException(msg);
                    } else {
                        CustomFunc func = new CustomFunc(this, name, argc_size, returns_cnt, func_expr);
                        this.customFunc.put(inst_key, func);
                    }

                    // 清空func_expr、end_cnt、end_need，用于检测下一个自定义函数
                    func_expr.clear();
                    end_cnt = 0;
                    end_need = 1;
                    continue;
                }

                if (Pattern.matches("if|while|for", t.getContent()))
                    end_need++;
                else if (t.getContent().equals("end"))
                    end_cnt++;
                func_expr.add(t);
            } else
                new_expr.add(t);
        }
        /*
            此时new_expr中，自定义函数的类型还是identifier，需要改成custom_func
         */
        for(Token t : new_expr) {
            if (func_name.contains(t.getContent()))
                t.setType(Type.custom_func);
        }
        return new_expr;
    }

    private boolean containsKey(String[] inst_key) {
        for(String[] key : this.customFunc.keySet()) {
            if (Arrays.equals(key, inst_key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取变量值
     *
     * @param t
     * @return
     */
    public String getVarValue(Token t) {
        if (t.isLiteNum()) {
            if (this.lite_varPool.containsKey(t.getContent()))
                return this.lite_varPool.get(t.getContent()).getContent();
            else
                return t.getContent();
        } else if (t.isLiteMat())
            return this.lite_varPool.get(t.getContent()).getContent();
        else if (t.isNum())
            return t.getContent();
        else if (t.isMat())
            return t.getContent();
        else if (t.isIdtf()) {
            if (this.varPool.containsKey(t.getContent()))
                return this.varPool.get(t.getContent()).getContent();
            else {
                String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "不存在，请检查输入！";
                throw new VariableNotFoundException(msg);
            }
        } else if (t.isLiteString() && this.lite_varPool.containsKey(t.getContent()))
            return this.lite_varPool.get(t.getContent()).getContent();
        else if (t.isLiteString() && !this.lite_varPool.containsKey(t.getContent()))
            return t.getContent();
        else {
            String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "类型错误，请检查输入！";
            throw new VariableTypeException(msg);
        }
    }

    /**
     * 调用内置函数
     * @param key 函数Token
     * @param argv 参数列表
     * @param returns_cnt 该行的返回值个数，0代表当前函数不是后缀表达式中最末尾那个，不能出现多返回值的情况
     * @return 计算结果
     */
    public Token[] callFunc(Token key, List<Token> argv, int returns_cnt) {
        ParserFunc func = new ParserFunc(this, returns_cnt);
        String key_name = key.getContent();
        Token[] returns_token = null;
        if (returns_cnt == -1) {
            returns_token = null;
            func.run(key, argv);
            if (func.getResult().length != 1) {
                String msg = "\n\t第" + key.getLine() + "行：返回值过多，无法计算" + "！";
                throw new VariableTypeException(msg);
            } else
                returns_token = func.getResult();
        } else {
            String single_returns = "rank|det|trans|eye|zeros|ones|random|randi|diag|log|log2|ln|lg|sum"; // 只可能出现单返回值的函数
            func.run(key, argv);
            if (func.getResult().length != returns_cnt) {
                String msg = "\n\t第" + key.getLine() + "行：返回值个数不匹配" + "！";
                throw new VariableTypeException(msg);
            } else
                returns_token = func.getResult();
        }
        return returns_token;
    }

    /**
     * 调用自定义函数
     * @param key 函数名
     * @param argv 参数
     * @return 计算结果
     */
    public Token[] callCustomFunc(Token key, List<Token> argv) {
        String[] func_key = {key.getContent(), String.valueOf(argv.size())}; // 构造key
        CustomFunc func = null;
        for(String[] origin_key : this.customFunc.keySet()) {
            if (Arrays.equals(origin_key, func_key)) {
                func = this.customFunc.get(origin_key);
                break;
            }
        }
        if (func.init(argv))
            func.run();
        return func.getResult();
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

    /**
     * 生成字面量的Key
     * @return
     */
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

    /**
     * 判断两个Token是否相等
     * @param t1
     * @param t2
     * @return
     */
    public boolean equals(Token t1, Token t2) {
        String string_regex = "\".*\""; // 字符串
        String num_regex = "[+-]?\\d+(\\.\\d+)?"; // 数字
        String mat_regex = "\\[(([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)" +
                "([\\s]*;[\\s]*([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)*\\]"; // 矩阵
        String content_1 = this.getVarValue(t1);
        String content_2 = this.getVarValue(t2);

        if (Pattern.matches(num_regex, content_1) && Pattern.matches(num_regex, content_2)) {
            double var1 = Double.parseDouble(this.getVarValue(t1));
            double var2 = Double.parseDouble(this.getVarValue(t2));
            if (var1 == var2)
                return true;
            else
                return false;
        }
        if (Pattern.matches(mat_regex, content_1) && Pattern.matches(mat_regex, content_2)) {
            Matrix m1 = new Matrix(this.getVarValue(t1));
            Matrix m2 = new Matrix(this.getVarValue(t2));
            if (m1.equals(m2))
                return true;
            else
                return false;
        }
        if (Pattern.matches(string_regex, content_1) && Pattern.matches(string_regex, content_2)) {
            String str1 = this.getVarValue(t1);
            String str2 = this.getVarValue(t2);
            if (str1.equals(str2))
                return true;
            else
                return false;
        }
        return false;
    }


    // getter()
    public Lexer getLexer() {
        return lexer;
    }

    public Map<String, Token> getVarPool() {
        return varPool;
    }

    public Map<String, Token> getLite_varPool() {
        return lite_varPool;
    }
}
