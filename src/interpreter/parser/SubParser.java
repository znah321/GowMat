package interpreter.parser;

import GowMat.Matrix;
import exception.VariableNotFoundException;
import exception.VariableTypeException;
import interpreter.lexer.Token;
import interpreter.lexer.Type;

public abstract class SubParser {
    protected Parser mainParser;
    protected Parser selfParser;

    public SubParser(Parser mainParser) {
        this.mainParser = mainParser;
    }

    // 取值
    public String getVarValue(Token t) {
        if (this.mainParser.getVarPool().containsKey(t.getContent()))
            return this.mainParser.getVarValue(t);
        else {
            if (t.isLiteNum()) {
                if (this.selfParser.getLite_varPool().containsKey(t.getContent()))
                    return this.selfParser.getLite_varPool().get(t.getContent()).getContent();
                else
                    return t.getContent();
            } else if (t.isLiteMat())
                return this.selfParser.getLite_varPool().get(t.getContent()).getContent();
            else if (t.isNum())
                return t.getContent();
            else if (t.isMat())
                return t.getContent();
            else if (t.isIdtf()) {
                if (this.selfParser.getVarPool().containsKey(t.getContent()))
                    return this.selfParser.getVarPool().get(t.getContent()).getContent();
                else {
                    String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "不存在，请检查输入！";
                    throw new VariableNotFoundException(msg);
                }
            } else if (t.isLiteString() && this.selfParser.getLite_varPool().containsKey(t.getContent()))
                return this.selfParser.getLite_varPool().get(t.getContent()).getContent();
            else if (t.isLiteString() && !this.selfParser.getLite_varPool().containsKey(t.getContent()))
                return t.getContent();
            else {
                String msg = "\n\t第" + t.getLine() + "行：" + "变量" + t.getContent() + "类型错误，请检查输入！";
                throw new VariableTypeException(msg);
            }
        }
    }

    // 赋值
    public void eval(Token var_token, Token res) {
        String varName = var_token.getContent();
        if (this.mainParser.getVarPool().containsKey(varName))
            this.mainParser.eval(var_token, res);
        else {

            if (this.selfParser.getVarPool().containsKey(varName)) {
                this.selfParser.getVarPool().get(varName).setContent(this.getVarValue(res));
                if (res.isLiteNum()) {
                    this.selfParser.getVarPool().get(varName).setType(Type.num);
                } else
                    this.selfParser.getVarPool().get(varName).setType(Type.mat);
            } else {
                Token t;
                if (res.isIdtf()) {
                    Type type = this.selfParser.getVarPool().get(res.getContent()).getType();
                    t = new Token(type, this.getVarValue(res), var_token.getLine());
                } else if (res.isLiteNum() || res.isNum())
                    t = new Token(Type.num, this.getVarValue(res), var_token.getLine());
                else if (res.isLiteMat() || res.isMat())
                    t = new Token(Type.mat, this.getVarValue(res), var_token.getLine());
                else
                    t = new Token(Type.string, this.getVarValue(res), var_token.getLine());
                this.selfParser.getVarPool().put(varName, t);
                ///////////////
                System.out.print(varName + " = ");
                if (this.selfParser.getVarPool().get(varName).isNum())
                    System.out.println(t.getContent());
                else if (this.selfParser.getVarPool().get(varName).isMat()) {
                    System.out.println();
                    new Matrix(t.getContent()).display();
                }
            }
        }
    }

}
