package interpreter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private List<Token> tokens = new ArrayList<Token>(); // Token列表
    private List<String> key = new ArrayList<String>(); // 关键字列表
    private static List<String> var = new ArrayList<String>(); // 变量列表
    public static void main(String[] args) {
        String code; // 一行源代码
        String code_name = "code.gmt";
        Lexer lexer = new Lexer();
        lexer.init_keyList(); // 初始化关键字列表

        /* 定义正则表达式 */
        String[] regexPat = new String[5];
        regexPat[0] = "[a-zA-Z_][a-zA-Z_0-9]*"; // identifier标识符
        regexPat[1] = "";
        for(int i = 0; i < lexer.key.size(); i++) {
            regexPat[1] = regexPat[1] + lexer.key.get(i);
            if (i != lexer.key.size() - 1)
                regexPat[1] += "|";
        } // key关键字
        regexPat[2] = "\\+|-|\\*|/|%|\\^|\\.\\^|\\./|\\.\\*|\\(|\\)|\\[|\\]|>|<|=|>=|<=|==|\\|\\||&&|\\||&|:"; // operator操作符
        regexPat[3] = "\".*\""; // str_literals字符串字面量
        regexPat[4] = "[+-]?\\d+(\\.\\d+)?"; // num_literals数字字面量

        code = lexer.read(code_name);

        /*
        String regex = "";
        regex += regexPat[0];
        for(int i = 1; i < regexPat.length; i++)
            regex = regex + "|" + regexPat[i];
        System.out.println(regex);
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher("[1,2,3;4,5,6;7,8,9]");
        if (m.lookingAt()) {
            for(int i = 0; i < 7; i++)
                System.out.println(m.group(i));
        }

         */
        System.out.print("输入一行代码: ");
        Scanner s = new Scanner(System.in);
        String tt = s.nextLine();
        System.out.println("词法分析结果：");
        List<String> _t_list= lexer.cut(tt, regexPat);
        for(int i = 0; i < _t_list.size(); i++)
            System.out.print(_t_list.get(i) + "|");
        /* 开始切割 */
        int line_num = 0;
        while ((code = lexer.read(code_name)) != null) {
            // 根据正则表达式进行匹配
            code = lexer.read(code_name);
            lexer.cut(code, regexPat);
        }
    }

    // 读一行代码
    public String read(String fileName) {
        FileReader fr = null;
        String code = null;
        try {
            fr = new FileReader("code.gmt");
            BufferedReader reader = new BufferedReader(fr);
            code = reader.readLine();
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
        return code;
    }

    // 初始化关键字列表
    public void init_keyList() {
        FileReader fr = null;
        try {
            fr = new FileReader("keyword");
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

    public List<String> cut(String code, String[] regexPat) {
        List<String> res = new ArrayList<String>();
        code = code.trim();
        char[] _ch_code = code.toCharArray();
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        while (code != null) {
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
                    System.out.println(t_str);
                    code = code.substring(pos + 2).trim();
                    _ch_code = code.toCharArray();
                    pos = 0;
                    // 再判断是不是一元操作符或分隔符（逗号、分号）
                } else if (Pattern.matches(regexPat[2], String.valueOf(_ch_code[pos])) || _ch_code[pos] == ',' || _ch_code[pos] == ';') {
                    // 当前字符是运算符或分隔符（逗号、分号）
                    if (sb.length() != 0) {
                        res.add(sb.toString());
                        System.out.println(sb.toString());
                    }
                    res.add(String.valueOf(_ch_code[pos]));
                    System.out.println(_ch_code[pos]);
                    sb.delete(0, sb.length()); // 清空sb
                    code = code.substring(pos + 1).trim();
                    _ch_code = code.toCharArray();
                    pos = 0;
                } else if (_ch_code[pos] == ' ') { // 当前字符是空格
                    // 如果sb里面的字符串是关键字，就存入res，并清空sb，然后跳过这个空格
                    if (Pattern.matches(regexPat[1], sb.toString())) {
                        System.out.println(sb.toString());
                        res.add(sb.toString());
                        sb.delete(0, sb.length()); // 清空delete
                        code = code.substring(pos + 1).trim();
                        _ch_code = code.toCharArray();
                        pos = 0;
                    // 如果sb里面的字符串不是关键字，而是字符串（没有右边的引号），则把空格存入sb
                    } else if (Pattern.matches("\".*", sb.toString())){
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
                System.out.println(_ch_code[pos]);
                res.add(String.valueOf(_ch_code[pos]));
                break;
            } else {
                // _ch_code中不止一个字符，但已经到达_ch_code的末尾
                System.out.println(sb.toString());
                res.add(sb.toString());
                break;
            }
        }
        return res;
    }

    public void addToken(Token t) {
        this.tokens.add(t);
    }

    public Token getToken(int index) {
        return this.tokens.get(index);
    }
}
