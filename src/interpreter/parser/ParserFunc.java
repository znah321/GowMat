package interpreter.parser;

import GowMat.Matrix;
import GowMat.util.CreateMatrix;
import exception.SyntaxErrorException;
import interpreter.lexer.Token;
import interpreter.lexer.Type;
import org.omg.CORBA.SystemException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Gowmat内置函数类
public class ParserFunc {
    private Parser parser;
    private int returns_cnt;
    private Token[] result;

    public ParserFunc(Parser parser, int returns_cnt) {
        this.parser = parser;
        this.returns_cnt = returns_cnt;
    }

    public void run(Token key, Token argc) {
        switch (key.getContent()) {
            case "print":
                this.print(argc);
                break;
        }
    }

    public void run(Token key, List<Token> argv) {
        switch (key.getContent()) {
            case "sum":
                this.sum(key, argv);
                break;
            case "eye":
                this.eye(key, argv);
                break;
            case "zeros":
            case "ones":
            case "random":
            case "randi":
                this.gene_a_matrix(key, argv);
                break;
        }
    }

    public Token[] getResult() {
        return result;
    }

    /* 内置函数 */
    // 屏幕打印函数
    private void print(Token argc) {
        if (this.returns_cnt != 0) {
            String msg = "\n\t第" + argc.getLine() + "行：" + "print函数不需要返回值！";
            throw new SyntaxErrorException(msg);
        }
        // print里面只有一个标识符，且为矩阵
        if (argc.isIdtf()) {
            if (this.parser.getVarPool().get(argc.getContent()).isMat()) {
                new Matrix(this.parser.getVarValue(argc)).display();
                return;
            }
        }

        String str = this.parser.getVarValue(argc).substring(1, this.parser.getVarValue(argc).length()-1);
        char[] _ch_res = str.toCharArray();
        String res = "";
        for(int i = 0; i < _ch_res.length; i++) {
            if (i != _ch_res.length - 1) {
                if (_ch_res[i] == '\\' && (_ch_res[i + 1] == '\'' || _ch_res[i + 1] == '\"' || _ch_res[i + 1] == '\\'))
                    res += String.valueOf(_ch_res[++i]);
                else
                    res += String.valueOf(_ch_res[i]);
            } else
                res += String.valueOf(_ch_res[i]);
        }
        System.out.println(res);
    }

    // 求和函数
    private void sum(Token key, List<Token> argv) {
        Matrix _t_mat = null;
        double _t_num = 0;
        String msg = null;

        /* 1-检查输入参数的合法性  */
        if (argv.isEmpty()) {
            msg = "\n\t第" + key.getLine() + "行：sum函数输入的参数不足" + "！";
            throw new SyntaxErrorException(msg);
        } else if (argv.size() > 2) {
            msg = "\n\t第" + key.getLine() + "行：sum函数输入的参数过多" + "！";
            throw new SyntaxErrorException(msg);
        } else if (argv.get(0).isIdtf() && !parser.getVarPool().containsKey(argv.get(0).getContent())) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "变量不存在" + "！";
            throw new SyntaxErrorException(msg);
        } else if (this.returns_cnt < 1) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "sum函数的缺少返回值" + "！";
            throw new SyntaxErrorException(msg);
        } else if (this.returns_cnt > 3) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "sum函数的返回值过多" + "！";
            throw new SyntaxErrorException(msg);
        }

        if (argv.size() == 1) {
                /*
                    只输入一个参数，默认按行求和
                    如果矩阵只有一行或一列，则对这一行/一列求和
                    如果是数字，直接返回数字
                 */
            Token mat = argv.get(0);
            // 待求和的矩阵是数字
            if (mat.isIdtf() && this.parser.getVarPool().containsKey(mat.getContent()) && this.parser.getVarPool().get(mat.getContent()).isNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            }
            if (mat.isLiteNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            }
            // 矩阵只有一行或一列
            Matrix m = new Matrix(this.parser.getVarValue(mat));
            if (m.getColumn() == 1 || m.getRow() == 1) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, String.valueOf(m.sum()), key.getLine());
                this.parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            } else {
                // 默认按行求和
                String name = parser.geneName();
                Token t = new Token(Type.mat_literals, m.sum_by_row().toToken(), key.getLine());
                this.parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            }
        } else {
            if (!this.isNumerical(argv.get(1))) {
                msg = "\n\t第" + key.getLine() + "行：sum函数的参数输入错误" + "！";
                throw new SyntaxErrorException(msg);
            }
            Token mat = argv.get(0);
            Token argc = argv.get(1);
            // 待求和的矩阵是数字
            if (mat.isIdtf() && this.parser.getVarPool().containsKey(mat.getContent()) && this.parser.getVarPool().get(mat.getContent()).isNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            }
            if (mat.isLiteNum()) {
                String name = parser.geneName();
                Token t = new Token(Type.num_literals, parser.getVarValue(mat), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.num_literals, name, key.getLine());
                return;
            }

            Matrix m = new Matrix(parser.getVarValue(mat));
            int sum_argc = getInteger(this.getNum(argc), key);
            /* 1-按行求和    2-按列求和    3-所有元素求和 */
            ///////////////////////////////////////////////test
            if (sum_argc == 4) {
                this.result = new Token[2];

                String name = this.parser.geneName();
                _t_mat = m.sum_by_row();
                if (_t_mat == null) {
                    Token t = new Token(Type.num_literals, String.valueOf(_t_num), key.getLine());
                    parser.getLite_varPool().put(name, t);
                    this.result[0] = new Token(Type.mat_literals, name, key.getLine());
                } else {
                    Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
                    parser.getLite_varPool().put(name, t);
                    this.result[0] = new Token(Type.mat_literals, name, key.getLine());
                }

                name = this.parser.geneName();
                _t_mat = m.sum_by_column();
                if (_t_mat == null) {
                    Token t = new Token(Type.num_literals, String.valueOf(_t_num), key.getLine());
                    parser.getLite_varPool().put(name, t);
                    this.result[1] = new Token(Type.mat_literals, name, key.getLine());
                } else {
                    Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
                    parser.getLite_varPool().put(name, t);
                    this.result[1] = new Token(Type.mat_literals, name, key.getLine());
                }
                return;
                //////////////////////////////////////////
            }

            if (sum_argc == 1)
                _t_mat = m.sum_by_row();
            else if (sum_argc == 2)
                _t_mat = m.sum_by_column();
            else
                _t_num = m.sum();
            String name = parser.geneName();
            if (_t_mat == null) {
                Token t = new Token(Type.num_literals, String.valueOf(_t_num), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.mat_literals, name, key.getLine());
            } else {
                Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
                parser.getLite_varPool().put(name, t);
                this.result = new Token[1];
                this.result[0] = new Token(Type.mat_literals, name, key.getLine());
            }
        }
    }

    // 创建单位矩阵
    private void eye(Token key, List<Token> argv) {
        Matrix _t_mat = null;
        int n = 0;
        String msg = null;

        // 参数合法性检查
        if (argv.size() < 1) {
            msg = "\n\t第" + key.getLine() + "行：eye函数输入的参数不足" + "！";
            throw new SyntaxErrorException(msg);
        } else if (argv.size() > 2) {
            msg = "\n\t第" + key.getLine() + "行：eye函数输入的参数过多" + "！";
            throw new SyntaxErrorException(msg);
        } else if (argv.size() == 1) {
            if (!this.isAllNumerical(argv)) {
                msg = "\n\t第" + key.getLine() + "行：请检查eye函数输入的参数的类型" + "！";
                throw new SyntaxErrorException(msg);
            }
        } else if (this.returns_cnt < 1) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "eye函数的缺少返回值" + "！";
            throw new SyntaxErrorException(msg);
        } else if (this.returns_cnt > 1) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + "eye函数的返回值过多" + "！";
            throw new SyntaxErrorException(msg);
        }
        n = getInteger(this.getNum(argv.get(0)), key); // 判断参数是否为整数
        
        _t_mat = CreateMatrix.eye(n);
        String name = parser.geneName();
        Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
        this.parser.getLite_varPool().put(name, t);
        this.result = new Token[1];
        this.result[0] = new Token(Type.mat_literals, name, key.getLine());
    }

    // 生成一个矩阵（zeros，ones, random, randi)
    private void gene_a_matrix(Token key, List<Token> argv) {
        Matrix _t_mat = null;
        String msg = null;
        /* 检查参数合法性 */
        // zeros函数和ones函数
        if (Pattern.matches("zeros|ones", key.getContent())) {
            if (argv.size() < 1) {
                msg = "\n\t第" + key.getLine() + "行：" + key.getContent() + "函数输入的参数不足" + "！";
                throw new SyntaxErrorException(msg);
            }
            if (argv.size() > 2) {
                msg = "\n\t第" + key.getLine() + "行：" + key.getContent() + "函数输入的参数过多" + "！";
                throw new SyntaxErrorException(msg);
            }
        }
        // random函数和randi函数
        if (Pattern.matches("random|randi", key.getContent())) {
            if (argv.size() < 2) {
                msg = "\n\t第" + key.getLine() + "行：" + key.getContent() + "函数输入的参数不足" + "！";
                throw new SyntaxErrorException(msg);
            }
            if (argv.size() > 5) {
                msg = "\n\t第" + key.getLine() + "行：" + key.getContent() + "函数输入的参数过多" + "！";
                throw new SyntaxErrorException(msg);
            }
        }
        // 返回值个数
        if (this.returns_cnt < 1) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + key.getContent() + "函数的缺少返回值" + "！";
            throw new SyntaxErrorException(msg);
        } else if (this.returns_cnt > 1) {
            msg = "\n\t第" + key.getLine() + "行：" + argv.get(0).getContent() + key.getContent() + "函数的返回值过多" + "！";
            throw new SyntaxErrorException(msg);
        }

        // 参数类型
        if (!this.isAllNumerical(argv)) {
            msg = "\n\t第" + key.getLine() + "行：请检查" + key.getContent() + "函数输入的参数类型" + "！";
            throw new SyntaxErrorException(msg);
        }

        if (argv.size() == 1) {
            int n = getInteger(this.getNum(argv.get(0)), key);
            switch (key.getContent()) {
                case "zeros":
                    _t_mat = CreateMatrix.zeros(n);
                    break;
                case "ones":
                    _t_mat = CreateMatrix.ones(n);
                    break;
            }
        } else if (argv.size() == 2){
            int n = getInteger(this.getNum(argv.get(0)), key);
            int m = getInteger(this.getNum(argv.get(1)), key);
            switch (key.getContent()) {
                case "zeros":
                    _t_mat = CreateMatrix.zeros(n, m);
                    break;
                case "ones:":
                    _t_mat = CreateMatrix.ones(n, m);
                    break;
            }
            // 下面的是random函数和randi函数
        } else if (argv.size() == 3) {
            int n = 0, lb = 0, ub = 0;
            n = getInteger(this.getNum(argv.get(0)), key);
            lb = (int) this.getNum(argv.get(1));
            ub = (int) this.getNum(argv.get(2));
            switch (key.getContent()) {
                case "random":
                    _t_mat = CreateMatrix.random(n, lb, ub);
                    break;
                case "randi":
                    _t_mat = CreateMatrix.randi(n, lb, ub);
                    break;
            }
        } else {
            int n = 0, m = 0, lb = 0, ub = 0;
            n = getInteger(this.getNum(argv.get(0)), key);
            m = getInteger(this.getNum(argv.get(1)), key);
            lb = (int) this.getNum(argv.get(2));
            ub = (int) this.getNum(argv.get(3));
            switch (key.getContent()) {
                case "random":
                    _t_mat = CreateMatrix.random(n, m, lb, ub);
                    break;
                case "randi":
                    _t_mat = CreateMatrix.randi(n, m, lb, ub);
                    break;
            }
        }
        String name = parser.geneName();
        Token t = new Token(Type.mat_literals, _t_mat.toToken(), key.getLine());
        this.parser.getLite_varPool().put(name, t);
        this.result = new Token[1];
        this.result[0] = new Token(Type.mat_literals, name, key.getLine());
    }

    /* 工具函数 */
    // 获取一个类型为num/num_literals的Token对象对应的数值
    private double getNum(Token t) {
        if (t.isLiteNum() && !this.parser.getLite_varPool().containsKey(t.getContent()))
            return Double.parseDouble(t.getContent());
        else if (t.isLiteNum() && this.parser.getLite_varPool().containsKey(t.getContent()))
            return Double.parseDouble(this.parser.getVarValue(t));
        else {
            return Double.parseDouble(this.parser.getVarValue(t));
        }
    }

    // 判断一个Token对象的内容是否为数字
    private boolean isNumerical(Token t) {
        if (t.isLiteNum())
            return true;
        else if (t.isIdtf() && this.parser.getVarPool().get(t.getContent()).isNum())
            return true;
        else
            return false;
    }

    // 判断一个List里面的Token对象是否均为数字
    private boolean isAllNumerical(List<Token> list) {
        for(int i = 0; i < list.size(); i++) {
            if (!this.isNumerical(list.get(i)))
                return false;
        }
        return true;
    }
    
    // 判断double是否为整数并赋值
    private static int getInteger(double x, Token key) {
        if (Math.floor(x) == x)
            return (int) x;
        else {
            String msg = "\n\t第" + key.getLine() + "行：" + key.getContent() + "函数输入的参数必须为整数" + "！";
            throw new SyntaxErrorException(msg);
        }
    }
}
