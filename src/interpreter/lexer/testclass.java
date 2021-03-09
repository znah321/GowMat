package interpreter.lexer;

import interpreter.parser.Parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class testclass {
    public static void main(String[] args) {

        String regex = "a";
        System.out.println(Pattern.matches("\".*", "\"aaa "));
        Lexer lexer = new Lexer("code");
        lexer.analyze();
//        lexer.save_result();
        Parser parser = new Parser(lexer);
        parser.parse();

         /*
        String str = "B = C([1:2:5],[2,3]) + [1,2,3;4,5,6;7,8,9].^2 + sum(A,1)";
        Pattern p = Pattern.compile("\\[(([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)([\\s]*;[\\s]*([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)*\\]");
        Matcher m = p.matcher(str);
        m.find();
        System.out.println(m.group(0));
        System.out.println(m.group(1));

          */
    }
}
