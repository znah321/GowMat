package interpreter.lexer;

enum Type {
    identifier, // 标识符：变量名之类的 a ab _aaa
    key, // 关键字：内置的函数名，以及if、else等
    operator, // 运算符：+ - * / % ^ .^ ./ .* ( ) [ ] , > < = >= <= || && | & :
    str_literals, // 字符串字面量 "hello gowmat"
    num_literals, // 数字字面量 1 2 34.5
    mat_literals, // 矩阵字面量
    undefined, // 未定义
    separator, // 分隔符（逗号，分号）
    end_of_line, // 行末尾
    end_of_file; // 文件末尾
}
