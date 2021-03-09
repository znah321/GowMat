
package interpreter.lexer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Lexer {
    private String objfile;
    private List<String> source_code = new ArrayList<String>(); // 源代码List，按行储存
    private int code_pos = 0; // source_code的指针
    private List<Token> tokens = new ArrayList<Token>(); // Token列表
    private List<String> key = new ArrayList<String>(); // 关键字列表

    public Lexer(String objfile) {
        this.objfile = objfile;
    }

    // 进行词法分析
    public void analyze() {
        String code; // 一行源代码
        String file_name = this.objfile;
        file_name = file_name + ".gmt";
        this.init_keyList(); // 初始化关键字列表

        /* 定义正则表达式 */
        String[] regexPat = new String[6];
        regexPat[0] = "[a-zA-Z_][a-zA-Z_0-9]*"; // identifier标识符
        regexPat[1] = "";
        for (int i = 0; i < this.key.size(); i++) {
            regexPat[1] = regexPat[1] + this.key.get(i);
            if (i != this.key.size() - 1)
                regexPat[1] += "|";
        } // key关键字
        regexPat[2] = "\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*|\\(|\\)|\\[|\\]|>|<|=|>=|<=|==|\\|\\||&&|\\||&|:"; // operator操作符
        regexPat[3] = "\".*\""; // str_literals字符串字面量
        regexPat[4] = "[+-]?\\d+(\\.\\d+)?"; // num_literals数字字面量
        regexPat[5] = ",|;"; // separator分隔符

        /* 开始切割 */
        this.readCode(file_name); // 把文件中所有内容读到source_code里
        int line_num = 1; // 行标
        do {
            /* 根据正则表达式进行匹配 */
            code = this.getLineCode(); // 读一行代码
            List<String> _t_code_list = this.cut(code, regexPat); // 切割
            for (int i = 0; i < _t_code_list.size(); i++) {
                if (Pattern.matches(regexPat[0], _t_code_list.get(i)))
                    this.addToken(Type.identifier, _t_code_list.get(i), line_num);
                else if (Pattern.matches(regexPat[1], _t_code_list.get(i)))
                    this.addToken(Type.key, _t_code_list.get(i), line_num);
                else if (Pattern.matches(regexPat[2], _t_code_list.get(i)))
                    this.addToken(Type.operator, _t_code_list.get(i), line_num);
                else if (Pattern.matches(regexPat[3], _t_code_list.get(i)))
                    this.addToken(Type.str_literals, _t_code_list.get(i), line_num);
                else if (Pattern.matches(regexPat[4], _t_code_list.get(i)))
                    this.addToken(Type.num_literals, _t_code_list.get(i), line_num);
                else if (Pattern.matches(regexPat[5], _t_code_list.get(i)))
                    this.addToken(Type.separator, _t_code_list.get(i), line_num);
                else
                    this.addToken(Type.undefined, _t_code_list.get(i), line_num);
            }
            this.addToken(Type.end_of_line, Token.EOL, line_num); // 添加换行标志
            line_num++;
        } while (this.hasNext());
        this.addToken(Token.EOF);
    }

    // 把所有的代码读到source_code中
    protected void readCode(String fileName) {
        FileReader fr = null;
        String code = null;
        try {
            fr = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(fr);
            while ((code = reader.readLine()) != null)
                this.source_code.add(code);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 将一行代码进行分割
    protected List<String> cut(String code, String[] regexPat) {
        List<String> res = new ArrayList<String>();
        code = code.trim();
        char[] _ch_code = code.toCharArray();
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (code.length() != 0) {
            // 取当前字符和下一个字符
            if (_ch_code.length > 1 && pos != _ch_code.length) {
                String t_str = null;
                try {
                    t_str = String.valueOf(_ch_code[pos]) + String.valueOf(_ch_code[pos + 1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                }
                // 先判断是不是二元操作符
                if (pos != _ch_code.length - 1 && Pattern.matches(regexPat[2], t_str)) {
                    /*
                        前面这个条件是判断pos指针是否已经到达_ch_code数组的尾部，不加这个的话，当pos到达数组尾时matches方法会抛出
                        NullPointerException异常（因为t_str初始化成了null，而在第一步中取不了两个字符给t_str）
                        同时用的是"&&"而不是"&"，第一个条件已经是false了，避免检查第二个条件
                     */
                    res.add(t_str);
                    code = code.substring(pos + 2).trim();
                    _ch_code = code.toCharArray();
                    pos = 0;
                    // 再判断是不是一元操作符或分隔符（逗号、分号）
                } else if (Pattern.matches(regexPat[2], String.valueOf(_ch_code[pos])) || _ch_code[pos] == ',' || _ch_code[pos] == ';') {
                    // 当前字符是运算符或分隔符（逗号、分号）
                    if (sb.length() != 0)
                        res.add(sb.toString());
                    res.add(String.valueOf(_ch_code[pos]));
                    sb.delete(0, sb.length()); // 清空sb
                    code = code.substring(pos + 1).trim();
                    _ch_code = code.toCharArray();
                    pos = 0;
                } else if (_ch_code[pos] == ' ') { // 当前字符是空格
                    // 如果sb里面的字符串是关键字，就存入res，并清空sb，然后跳过这个空格
                    if (Pattern.matches(regexPat[1], sb.toString())) {
                        res.add(sb.toString());
                        sb.delete(0, sb.length()); // 清空delete
                        code = code.substring(pos + 1).trim();
                        _ch_code = code.toCharArray();
                        pos = 0;
                        // 如果sb里面的字符串不是关键字，而是字符串（没有右边的引号），则把空格存入sb
                    } else if (Pattern.matches("\".*", sb.toString())) {
                        sb.append(_ch_code[pos]);
                        pos++;
                    } else {
                        pos++;
                    }
                } else {
                    sb.append(_ch_code[pos]);
                    pos++;
                }
            } else if (_ch_code.length == 1) {
                // _ch_code中只剩下一个字符，直接返回res
                res.add(String.valueOf(_ch_code[pos]));
                break;
            } else {
                // _ch_code中不止一个字符，但已经到达_ch_code的末尾
                res.add(sb.toString());
                break;
            }
        }
        return res;
    }

    // 读一行代码
    protected String getLineCode() {
        return this.source_code.get(this.code_pos++);
    }

    // 判断是否还有代码
    protected boolean hasNext() {
        if (this.code_pos < this.source_code.size())
            return true;
        else
            return false;
    }

    // 初始化关键字列表
    protected void init_keyList() {
        FileReader fr = null;
        try {
            fr = new FileReader("src/config/keyword");
            BufferedReader reader = new BufferedReader(fr);
            String _key;
            while ((_key = reader.readLine()) != null)
                this.key.add(_key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 添加一个Token到Token列表中
    protected void addToken(Type t, String content, int line) {
        Token token = new Token(t, content, line);
        this.tokens.add(token);
    }

    protected void addToken(Token token) {
        this.tokens.add(token);
    }

    // 返回一个Token对象
    protected Token getToken(int index) {
        return this.tokens.get(index);
    }

    // 将词法分析的结果保存到文件中
    public void save_result() {
        String fileName = "lexical_result_of_" + this.objfile + ".txt";
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(fileName));
            ps.print("-----所在行标----- | ----------Token内容---------- | ----------Token类型----------");
            ps.print("\n");
            int _t1_len = 5 * 2 + 4 + 3;
            int _t2_len = _t1_len + 2 + 10 * 2 + 7;
            for (int i = 0; i < this.tokens.size(); i++) {
                Token t = this.getToken(i);
                if (t.getType() == Type.end_of_line) {
                    continue;
                } else if (t.getType() == Type.end_of_file) {
                    break;
                } else {
                    // 写行标
                    ps.print(String.valueOf(t.getLine()));
                    // 对齐
                    for (int j = String.valueOf(t.getLine()).length() - 1; j < _t1_len; j++)
                        ps.print(" ");
                    ps.print("| ");
                    // 写Token内容
                    ps.print(t.getContent());
                    // 对齐
                    for (int j = _t1_len + t.getContent().length(); j < _t2_len; j++)
                        ps.print(" ");
                    ps.print("| ");
                    // 写Token类型
                    ps.print(String.valueOf(t.getType()));
                }
                if (i != this.tokens.size() - 2)
                    ps.println();
            }
            File file = new File(fileName);
            System.out.println("分析结果已保存在：" + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getObjfile() {
        return objfile;
    }

    public List<String> getSource_code() {
        return source_code;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<String> getKey() {
        return key;
    }
}
