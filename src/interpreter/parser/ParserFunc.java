package interpreter.parser;

import GowMat.Matrix;
import com.sun.xml.internal.ws.api.ha.StickyFeature;
import exception.SyntaxErrorException;
import interpreter.lexer.Lexer;
import interpreter.lexer.Token;
import interpreter.lexer.Type;

import java.util.List;

// Gowmat内置函数类
public class ParserFunc {
    private Parser parser;

    public ParserFunc(Parser parser) {
        this.parser = parser;
    }

    // 屏幕打印函数
    protected void print() {

    }

    // 求和函数
    protected Token sum(Token key, List<Token> argv) {
        Matrix _t_mat = null;
        double _t_num = 0;
        String msg = null;

        if (key.getContent().equals("sum")) { // sum函数
            Token mat = argv.get(0);
            Token argc = argv.get(1);
            /* 1-检查输入参数的合法性  */
            if (argv.size() < 2) {
                msg = "\n\t第" + key.getLine() + "行：sum函数输入的参数不足" + "！";
                throw new SyntaxErrorException(msg);
            } else if (argv.size() > 2) {
                msg = "\n\t第" + key.getLine() + "行：sum函数输入的参数过多" + "！";
                throw new SyntaxErrorException(msg);
            } else if (!parser.getVarPool().containsKey(argv.get(0).getContent())){
                msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "变量不存在" + "！";
                throw new SyntaxErrorException(msg);
            } else if (!argc.getContent().equals("1") && !argc.getContent().equals("2") && !argc.getContent().equals("3")) {
                msg = "\n\t第" + key.getLine() + "行：sum函数的参数输入错误" + "！";
                throw new SyntaxErrorException(msg);
            }

            // 待求和的矩阵是数字
            if (mat.isNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getVarPool().put(name, t);
                return new Token(Type.num_literals, name, key.getLine());
            }
            if (mat.isLiteNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getVarPool().put(name, t);
                return new Token(Type.num_literals, name, key.getLine());
            }

            Matrix m = new Matrix(parser.getVarValue(mat));
            int sum_argc = Integer.parseInt(argc.getContent());
            /* 1-按行求和    2-按列求和    3-所有元素求和 */
            if (sum_argc == 1)
                _t_mat = m.sum_by_row();
            else if (sum_argc == 2)
                _t_mat = m.sum_by_column();
            else
                _t_num = m.sum();
        }
        String name = parser.geneName();
        if (_t_mat == null) {
            Token t = new Token(Type.num_literals, String.valueOf(_t_num), key.getLine());
            parser.getLite_varPool().put(name, t);
            return new Token(Type.num_literals, name, key.getLine());
        } else {
            Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
            parser.getLite_varPool().put(name, t);
            return new Token(Type.mat_literals, name, key.getLine());
        }
    }
}
