package interpreter.parser.util;

import interpreter.lexer.Token;
import interpreter.lexer.Type;
import interpreter.parser.Parser;
import interpreter.parser.ParserFunc;

import java.util.ArrayList;
import java.util.List;

public class ParserUtil {
    private ParserUtil() {}

    // 获取一行Token
    public static List<Token> get_line_tokens(List<Token> tokenList, int line) {
        int last_line = tokenList.get(tokenList.size() - 2).getLine();
        List<Token> tokens = new ArrayList<>();
        if (line <= last_line) {
            for (Token item : tokenList) {
                if (item.getLine() == line)
                    tokens.add(item.clone());
            }
            return tokens;
        } else {
            tokens.add(new Token(Type.end_of_file, "EOF", -1));
            return tokens;
        }
    }

    // 把一行Token拼接成一个字符串
    public static String getString(List<Token> tokenList) {
        String res = "";
        for(Token t : tokenList)
            res += t.getContent();
        return res;
    }

    public static String getString(List<Token> tokenList, int startIndex, int endIndex) {
        String res = "";
        for(int i = startIndex; i < endIndex; i++)
            res += tokenList.get(i).getContent();
        return res;
    }

    public static int getIndex(List<Token> tokenList, Token t) {
        for(int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).getContent().equals(t.getContent()) && tokenList.get(i).getType() == t.getType() && t.getLine() == tokenList.get(i).getLine())
                return i;
        }
        return -1;
    }

    public static int getIndex(List<Token> tokenList, String str) {
        for(int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).getContent().equals(str))
                return i;
        }
        return -1;
    }
}
