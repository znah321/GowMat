import interpreter.lexer.Lexer;
import matrix.goweild.Cholesky_Decomposition;
import matrix.goweild.Eigen_Decomposition;
import matrix.goweild.Matrix;
import matrix.goweild.SingularValue_Decomposition;

import java.util.regex.Pattern;

public class TestClass {
    public static void main(String[] args) {
        /*
        Matrix A = new Matrix("[4,-1,1;-1,4.25,2.75;1,2.75,3.5]");
        Cholesky_Decomposition cd = new Cholesky_Decomposition(A);
        Matrix.print_tdarray(cd.getL(), "L");
        Matrix.print_tdarray(cd.getTransL(), "transL");

         */
        String regexPat_1 = "[+-]?\\d+(\\.\\d+)?"; // num_literals数字字面量
        // ([+-]?\d+(\.\d+)?)([\s,][+-]?\d+(\.\d+)?)* 行矩阵
        // ([;]([+-]?\d+(\.\d+)?)([\s,][+-]?\d+(\.\d+)?)*)*
        // (([+-]?\d+(\.\d+)?)([\s,][+-]?\d+(\.\d+)?)*)([;]([+-]?\d+(\.\d+)?)([\s,][+-]?\d+(\.\d+)?))*
        String regexPat_2 = "\\[(([+-]?\\d+(\\.\\d+)?)([\\s,][+-]?\\d+(\\.\\d+)?)*)([;]([+-]?\\d+(\\.\\d+)?)([\\s,][+-]?\\d+(\\.\\d+)?)*)*\\]|\\[\\]"; // mat_literals矩阵字面量
        String t = "[1, 2,                3;4, 5,        6.7]";
        // new
        // \\[([+-]?\d+(\.\d+)?)([\s]*[,]?[\s]*[+-]?\d+(\.\d+)?)*\\]
        // (([+-]?\d+(\.\d+)?)([\s]*[,]?[\s]*[+-]?\d+(\.\d+)?)*)([\s]*;[\s]*([+-]?\d+(\.\d+)?)([\s]*[,]?[\s]*[+-]?\d+(\.\d+)?)*)*
        String regexPat = "\\[(([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)([\\s]*;[\\s]*([+-]?\\d+(\\.\\d+)?)([\\s]*[,]?[\\s]*[+-]?\\d+(\\.\\d+)?)*)*\\]";
        System.out.println(Pattern.matches(regexPat, t));
    }
}
