package interpreter.parser;

import exception.SyntaxErrorException;
import exception.VariableNotFoundException;
import exception.VariableTypeException;
import interpreter.lexer.Token;
import interpreter.lexer.Type;
import interpreter.parser.util.ParserUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CustomFunc extends SubParser{
    private String name; // 函数名
    private int argc_size; // 参数个数
    private int returns_cnt; // 返回值个数
    private List<Token> func_body = new ArrayList<>(); // 函数体
    private List<Token> argv; // 输入参数
    private Token[] result; // 运算结果

    public CustomFunc(Parser mainParser, String name, int argc_size, int returns_cnt, List<Token> func_body) {
        super(mainParser);
        // 去掉第一行的function xxx和最后一行的end
        int first_line = this.func_body.get(0).getLine(); // "function xxx = func(aaa,bbb)"所在的行标
        int last_line = this.func_body.get(this.func_body.size() - 1).getLine(); // 最后一行的行标
        List<Token> sub_parser_token = new ArrayList<>();
        for(Token t : this.func_body) {
            if (t.getLine() != first_line && t.getLine() != last_line)
                sub_parser_token.add(t.clone());
        }
        sub_parser_token.add(new Token(Type.end_of_file, "EOF", last_line)); // 需要加上EOF标志
        Parser selfParser = new Parser(sub_parser_token);
        this.selfParser = selfParser;
        this.name = name;
        this.argc_size = argc_size;
        this.returns_cnt = returns_cnt;
        for(Token item : func_body)
            this.func_body.add(item.clone());
        this.is_legal();
    }

    public boolean init(List<Token> argv) {
        if (this.argc_size == argv.size())
            this.argv = argv;
        else {
            String msg = "\n\t第" + argv.get(0).getLine() + "行：" + "参数个数不匹配！";
            throw new SyntaxErrorException(msg);
        }
        return true;
    }

    /* 运行函数 */
    public void run() {
        // 将字面量改成标识符类型
        for(Token t : argv) {
            if (t.isLiteMat())
                t.setType(Type.mat);
            if (t.isLiteNum())
                t.setType(Type.num);
            if (t.isLiteString())
                t.setType(Type.string);
        }
        // 设置输入参数
        String[] inputs = this.get_inputs_string();
        for(int i = 0; i < this.argc_size; i++) {
            Token t = argv.get(i);
            if (Pattern.matches("[+-]?\\d+(\\.\\d+)?", t.getContent()))
                this.selfParser.getVarPool().put(inputs[i], t);
            else if (Pattern.matches("\".*\"", t.getContent()))
                this.selfParser.getVarPool().put(inputs[i], t);
            else if (Pattern.matches("\\[(([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)([\\s]*;[\\s]*([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)*\\]", t.getContent()))
                this.selfParser.getVarPool().put(inputs[i], t);
            else if (t.isLiteString() || t.isLiteNum() || t.isLiteMat())
                this.selfParser.getVarPool().put(inputs[i], this.mainParser.getLite_varPool().get(t.getContent()));
            else if (t.isIdtf())
                this.selfParser.getVarPool().put(inputs[i], this.mainParser.getVarPool().get(t.getContent()));
        }


        // 运行
        this.selfParser.parse();

        // 获取计算结果
        String[] returns = this.get_returns_string();
        int string_pos = 0;
        int result_pos = 0;
        this.result = new Token[returns_cnt];
        for(String key : this.selfParser.getVarPool().keySet()) {
            if (key.equals(returns[string_pos])) {
                string_pos++;
                this.result[result_pos++] = this.selfParser.getVarPool().get(key);
            }
        }
    }

    public Token[] getResult() {
        return result;
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
    public void is_legal() {
        int first_line = this.func_body.get(0).getLine(); // "function xxx = func(aaa,bbb)"所在的行标
        int last_line = this.func_body.get(this.func_body.size() - 1).getLine(); // 最后一行的行标

        /* 1-检查返回值是否存在重名 */
        String[] returns = this.get_returns_string();
        if (returns.length > 1) {
            Set returns_set = new HashSet();
            for(String item : returns)
                returns_set.add(item);
            if (returns_set.size() != returns.length) {
                String msg = "\n\t第" + first_line + "行：" + "返回值存在重复命名！";
                throw new SyntaxErrorException(msg);
            }
        }

        /* 2-检查返回值是否被初始化 */
        int to_confirm_pos = 0; // 待确认的返回值的指针
        for(int idx = first_line + 1; idx < last_line; idx++) {
            List<Token> l_token = ParserUtil.get_line_tokens(this.func_body, idx); // 获取一行Token
            Parser.SentType type = Parser.setSentType(l_token);

            if (type == Parser.SentType.eval) {
                if (l_token.get(0).getContent().equals(returns[to_confirm_pos]))
                    to_confirm_pos++;
                else
                    continue;
            }

            if (idx < last_line && to_confirm_pos == this.returns_cnt)
                break;
            if (idx == last_line - 1 && to_confirm_pos < this.returns_cnt) {
                String msg = "\n\t第" + first_line + "行：函数" + this.name +"的返回值未被全部初始化！";
                throw new SyntaxErrorException(msg);
            }
        }
    }

    /**
     * 获取返回值的名字
     * @return
     */
    private String[] get_returns_string() {
        int first_line = this.func_body.get(0).getLine(); // "function xxx = func(aaa,bbb)"所在的行标
        StringBuffer returns_string = new StringBuffer();
        int pos = 1;
        while (!this.func_body.get(pos).getContent().equals("="))
            returns_string.append(func_body.get(pos++).getContent()); // 拼接"function xxx"所在的行
        returns_string = returns_string.deleteCharAt(returns_string.length() - 1).deleteCharAt(0); // 去掉"["、"]"
        String[] returns = returns_string.toString().split(",");
        return returns;
    }

    /**
     * 获取输入参数的名字
     * @return
     */
    private String[] get_inputs_string() {
        int first_line = this.func_body.get(0).getLine(); // "function xxx = func(aaa,bbb)"所在的行标
        StringBuffer inputs_string = new StringBuffer();
        int pos = 1;
        while (!this.func_body.get(pos).getContent().equals("="))
            pos++;
        pos = pos + 3;
        while (!this.func_body.get(pos).isEOL())
            inputs_string.append(this.func_body.get(pos++).getContent());
        inputs_string = inputs_string.deleteCharAt(inputs_string.length() - 1);
        String[] inputs = inputs_string.toString().split(",");
        return inputs;
    }
}
