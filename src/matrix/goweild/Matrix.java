package matrix.goweild;

import matrix.goweild.util.CreateMatrix;
import matrix.goweild.util.MatrixMath;

import java.lang.reflect.Array;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Matrix{
    private double[][] mat;
    private int row = 0;
    private int column = 0;

    /**
     * 构造函数
     * @param mat 矩阵的二维数组形式
     * @param row 行数
     * @param column 列数
     */
    public Matrix(double[][] mat, int row, int column) {
        this.mat = mat;
        this.row = row;
        this.column = column;
    }

    /**
     * 构造函数
     * @param mat 矩阵的二维数组形式
     */
    public Matrix(double[][] mat) {
        this.mat = mat;
        this.row = mat.length;
        this.column = mat[0].length;
    }

    /**
     * 生成一个新的矩阵
     * @param string 字符串形式的矩阵
     */
    public Matrix(String string) {
        // 去掉两边的"["和"]"
        string = string.trim().substring(1, string.length() - 1);

        // 录入数据
        String matrix[] = string.split(";");
        String line_matrix[] = matrix[0].split(",");
        this.row = matrix.length;
        this.column = line_matrix.length;

        double[][] data = new double[this.row][this.column];
        for(int i = 0; i < this.row; i++) {
            line_matrix = matrix[i].split(",");
            for(int j = 0; j < this.column; j++)
                data[i][j] = Double.parseDouble(line_matrix[j].trim());
        }
        this.mat = data;
//        this.display();
    }

    /**
     * 打印矩阵信息
     */
    public void display() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(4);
        for (int i = 0; i < this.row; i++) {
            System.out.print("[\t");
            for (int j = 0; j < this.column; j++) {
                if (Math.round(this.mat[i][j]) == this.mat[i][j])
                    System.out.print(Math.round(this.mat[i][j]));
                else
                    System.out.print(nf.format(this.mat[i][j]));
                //if (j != this.column - 1)
                    System.out.print("\t");
            }
            System.out.print("]");
            if (i != this.row - 1)
                System.out.println();
        }
        System.out.println();
    }

    /**
     * 复制一份作为副本
     * @param mat 矩阵的二维数组形式
     * @return 一个二维数组
     */
    public static double[][] copy(double[][] mat) {
        double[][] res = new double[mat.length][mat[0].length];
        for(int i = 0; i < mat.length; i++)
            res[i] = mat[i].clone();
        return res;
    }

    /**
     * 交换矩阵中(i,j)、(a,b)两个元素
     * @param tdarray 被用到的矩阵
     * @param i 第一个元素的行标
     * @param j 第一个元素的列标
     * @param a 第二个元素的行标
     * @param b 第二个元素的列标
     */
    public static void swap(double[][] tdarray, int i, int j, int a, int b) {
        double temp = tdarray[i][j];
        tdarray[i][j] = tdarray[a][b];
        tdarray[a][b] = temp;
    }

    /**
     * 交换矩阵中第i行和第j行的元素
     * @param tdarray
     * @param i
     * @param j
     */
    public static void swap_row(double[][] tdarray, int i, int j) {
        for(int k = 0; k < tdarray[0].length; k++)
            swap(tdarray, i, k, j, k);
    }

    /**
     * 交换矩阵中第i列和第j列的元素
     * @param tdarray
     * @param i
     * @param j
     */
    public static void swap_col(double[][] tdarray, int i, int j) {
        for(int k = 0; k < tdarray.length; k++)
            swap(tdarray, k, i, k, j);
    }

    /**
     * 获取矩阵的某个列向量
     * @param tdarray 二维数组
     * @param c 列标
     * @return 列向量
     */
    public static double[] get_column_vector(double[][] tdarray, int c) {
        double[] res = new double[tdarray.length];
        for (int i = 0; i < tdarray.length; i++)
            res[i] = tdarray[i][c];
        return res;
    }

    /**
     * 判断两个矩阵是否行、列数均相等
     * @param m1 矩阵1
     * @param m2 矩阵2
     * @return true==>相同,false==>不同
     */
    public boolean adjust_r_c_same(Matrix m1, Matrix m2) {
        if (m1.column == m2.column && m1.row == m2.row)
            return true;
        return false;
    }

    /**
     * 矩阵加法
     * @param m 矩阵
     * @return 新矩阵
     */
    public Matrix plus(Matrix m) {
        // 合法性检查
        if (!adjust_r_c_same(this, m)) {
            System.out.println("矩阵维度不同，无法相加！");
            System.exit(-1);
        }
        // 返回值初始化
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][this.column];
        // 计算
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                new_mat[i][j] = this.mat[i][j] + m.mat[i][j];

        new_matrix = new Matrix(new_mat, this.row, this.column);
        return new_matrix;
    }

    /**
     * 矩阵与元素相加
     * @param s 元素
     * @return 新矩阵
     */
    public Matrix plus(double s) {
        // 返回值初始化
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][this.column];
        // 计算
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                new_mat[i][j] = this.mat[i][j] + s;

        new_matrix = new Matrix(new_mat, this.row, this.column);
        return new_matrix;
    }

    /**
     * 矩阵减法
     * @param m 减去的矩阵
     * @return 新矩阵
     */
    public Matrix minus(Matrix m) {
        // 合法性检查
        if (!adjust_r_c_same(this, m)) {
            System.out.println("矩阵维度不同，无法相加！");
            System.exit(-1);
        }
        // 返回值初始化
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][this.column];
        // 计算
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                new_mat[i][j] = this.mat[i][j] - m.mat[i][j];

        new_matrix = new Matrix(new_mat, this.row, this.column);
        return new_matrix;
    }

    /**
     * 矩阵与元素相减
     * @param s 减数
     * @return 新矩阵
     */
    public Matrix minus(double s) {
        // 返回值初始化
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][this.column];
        // 计算
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                new_mat[i][j] = this.mat[i][j] - s;

        new_matrix = new Matrix(new_mat, this.row, this.column);
        return new_matrix;
    }

    /**
     * 矩阵乘法(右乘)
     * @param m 右边的矩阵
     * @return 新矩阵
     */
    public Matrix times(Matrix m) {
        // 合法性检查
        if (this.column != m.row) {
            System.out.println("请检查输入的矩阵维度！");
            System.exit(-1);
        }
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][m.column];
        for(int i = 0; i < this.row; i++) {
            for(int j = 0; j < m.column; j++) {
                double sum = 0;
                for(int k = 0; k < this.column; k++)
                    sum += this.mat[i][k]*m.mat[k][j];
                new_mat[i][j] = sum;
            }
        }
        new_matrix = new Matrix(new_mat, this.row, m.column);
        return new_matrix;
    }

    /**
     * 矩阵与元素相乘
     * @param s 乘数
     * @return 新矩阵
     */
    public Matrix times(double s) {
        // 返回值初始化
        Matrix new_matrix;
        double[][] new_mat = new double[this.row][this.column];
        // 计算
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                new_mat[i][j] = this.mat[i][j] * s;

        new_matrix = new Matrix(new_mat, this.row, this.column);
        return new_matrix;
    }

    /**
     * 求行列式
     * @return 行列式
     */
    public double det() {
        // 输入合法性检查：必须为方阵
        if (this.row != this.column) {
            System.out.println("该矩阵不是方阵，无法求行列式！");
            System.exit(-1);
        }
        int size_n = this.row; // 矩阵的阶数
        // 复制一份作为副本，不改变原矩阵的值
        double[][] t_mat = copy(this.mat);

        if (size_n == 1)
            return t_mat[0][0];
        else if (size_n == 2)
            return t_mat[0][0]*t_mat[1][1] - t_mat[1][0]*t_mat[0][1];
        else {
            double sum = 1;
            for(int i = 0; i < size_n - 1; i++) {
                // 将第i行的主元设置为非0元素
                int j = i;
                while (t_mat[j][i] == 0 && j < size_n) {
                    if (t_mat[j+1][i] != 0) {
                        for (int k = 0; k < size_n - 1; k++)
                            Matrix.swap(t_mat, i, k, j, k);
                        break;
                    }
                }
                // 消元
                double pivot = t_mat[i][i];
                for(int k = i + 1; k < size_n; k++) {
                    double multiplier = t_mat[k][i] / pivot; // 乘数
                    for(int l = 0; l < size_n; l++)
                        t_mat[k][l] = t_mat[k][l] - t_mat[i][l]*multiplier;
                }
            }
            for (int i = 0; i < size_n; i++)
                sum *= t_mat[i][i];
            return sum;
        }
    }

    /**
     * 矩阵求和
     * @return 矩阵中所有元素的和
     */
    public double sum() {
        double sum = 0;
        for(int i = 0; i < this.row; i++)
            for(int j = 0; j < this.column; j++)
                sum += this.mat[i][j];
        return sum;
    }

    /**
     * 按行求和
     * @return 一维数组
     */
    public double[] sum_by_row() {
        double[] res = new double[this.row];
        for(int i = 0; i < this.row; i++) {
            double temp = 0;
            for(int j = 0; j < this.column; j++)
                temp += this.mat[i][j];
            res[i] = temp;
        }
        return res;
    }

    /**
     * 按列求和
     * @return 一维数组
     */
    public double[] sum_by_column() {
        double[] res = new double[this.column];
        for(int i = 0; i < this.column; i++) {
            double temp = 0;
            for(int j = 0; j < this.row; j++)
                temp += this.mat[i][j];
            res[i] = temp;
        }
        return res;
    }

    /**
     * 求逆矩阵
     * @return 矩阵的逆
     */
    public Matrix inv() {
        // 检查逆矩阵是否存在
        if (this.row != this.column || this.det() == 0) {
            System.out.println("该矩阵的逆矩阵不存在！");
            System.exit(-1);
        }

        int size_n = this.row;
        Matrix new_matrix;
        double[][] new_mat = new double[size_n][size_n];
        // 基于LU分解求逆矩阵
        // Step 1-计算L、U矩阵
        double[][] U = new double[size_n][size_n];
        double[][] L = new double[size_n][size_n];
        LU_Decomposition lu = new LU_Decomposition(this);
        L = lu.getL();
        U = lu.getU();
        // Step 2-求L的逆矩阵
        double[][] L_inv = new double[size_n][size_n];
        for(int i = 0; i < size_n; i++) {
            L_inv[i][i] = 1 / L[i][i];
            for(int k = i + 1; k < size_n; k++) {
                for(int j = i; j < k; j++)
                    L_inv[k][i] -= L[k][j]*L_inv[j][i];
            }
        }
        // Step 3-求U的逆矩阵
        double[][] U_inv = new double[size_n][size_n];
        for(int i = 0; i < size_n; i++) {
            U_inv[i][i] = 1 / U[i][i];
            for(int k = i - 1; k >= 0; k--) {
                double s = 0;
                for(int j = k + 1; j <= i; j++)
                    s += U[k][j]*U_inv[j][i];
                U_inv[k][i] = -s / U[k][k];
            }
        }
        // Step 4-将L_inv、U_inv相乘，得到逆矩阵
        new_mat = new Matrix(U_inv).times(new Matrix(L_inv)).mat;
        new_matrix = new Matrix(new_mat, size_n, size_n);
        return new_matrix;
    }

    /**
     * 矩阵转置
     * @return 转置后的矩阵
     */
    public Matrix trans() {
        Matrix new_matrix;
        double[][] new_mat = new double[this.column][this.row];
        if (this.row == 1 && this.column == 1)
            return new Matrix(this.mat, 1, 1);
        for(int i = 0; i < this.column; i++)
            for(int j = 0; j < this.row; j++)
                new_mat[i][j] = this.mat[j][i];
        return new Matrix(new_mat, this.column, this.row);
    }

    /**
     * 返回一个子矩阵
     * @param r 子矩阵所在的行,语法同Matlab
     * @param c 子矩阵所在的列,语法同Matlab
     * @return 子矩阵
     */
    public Matrix submatrix(String r, String c) {
        // 去掉r、c两边的"["、"]"
        if (r.contains("["))
            r = r.trim().substring(1);
        if (r.contains("]"))
            r = r.trim().substring(0, r.length() - 1);
        if (c.contains("["))
            c = c.trim().substring(1);
        if (c.contains("]")) {
            c = c.trim().substring(0, c.length() - 1);
        }
        /* 判断模式:
            1-如果还有",",则是选取特定位置的值
            2-如果含有":",则是以等差数列的形式取值
         */
        double[][] new_mat = null;
        if (r.contains(",")) {
            String[] r_arr = r.trim().split(",");
            if (c.contains(",")) {
                // 情况1 ==> 均含有","
                String[] c_arr = c.trim().split(",");
                new_mat = new double[r_arr.length][c_arr.length];
                for(int i = 0; i < r_arr.length; i++) {
                    for (int j = 0; j < c_arr.length; j++) {
                        try {
                            new_mat[i][j] = this.mat[Integer.parseInt(r_arr[i].trim())][Integer.parseInt(c_arr[j].trim())];
                        } catch (Exception e) {
                            System.out.println("下标超出矩阵范围！");
                            System.exit(-1);
                        }
                    }
                }
            } else {
                // 情况2 ==> 行为特定位置,列为等差数列
                String[] c_arr = c.trim().split(":"); // c_arr[0]为起始位置,c_arr[1]为递增量,c_arr[2]为结束位置
                // 若c_arr的长度为2，则递增量为0(下标相差1)
                int start = 0, val = 0, end = 0;
                if (c_arr.length == 2) {
                    start = Integer.parseInt(c_arr[0].trim());
                    val = 1;
                    end = Integer.parseInt(c_arr[1].trim());
                } else {
                    start = Integer.parseInt(c_arr[0].trim());
                    val = Integer.parseInt(c_arr[1].trim()) + 1;
                    end = Integer.parseInt(c_arr[2].trim());
                }
                new_mat = new double[r_arr.length][(end - start) / val + 1];
                for(int i = 0; i < r_arr.length; i++) {
                    for(int j = 0; j < (end - start) / val + 1; j++) {
                        try {
                            new_mat[i][j] = this.mat[Integer.parseInt(r_arr[i].trim())][start + j*val];
                        } catch (Exception e) {
                            System.out.println("下标超出矩阵范围！");
                            System.exit(-1);
                        }
                    }
                }
            }
        } else {
            String[] r_arr = r.trim().split(":");
            int r_start = 0, r_val = 0, r_end = 0;
            if (r_arr.length == 2) {
                r_start = Integer.parseInt(r_arr[0].trim());
                r_val = 1;
                r_end = Integer.parseInt(r_arr[1].trim());
            } else {
                r_start = Integer.parseInt(r_arr[0].trim());
                r_val = Integer.parseInt(r_arr[1].trim()) + 1;
                r_end = Integer.parseInt(r_arr[2].trim());
            }
            if (c.contains(",")) {
                String[] c_arr = c.trim().split(",");
                new_mat = new double[(r_end - r_start) / r_val][c_arr.length];
                for(int i = 0; i < (r_end - r_start) / r_val + 1; i++) {
                    for(int j = 0; j < c_arr.length; j++) {
                        try {
                            new_mat[i][j] = this.mat[r_start + i*r_val][Integer.parseInt(c_arr[j].trim())];
                        } catch (Exception e) {
                            System.out.println("下标超出矩阵范围！");
                            System.exit(-1);
                        }
                    }
                }
            } else {
                String[] c_arr = c.trim().split(":"); // c_arr[0]为起始位置,c_arr[1]为递增量,c_arr[2]为结束位置
                int c_start = 0, c_val = 0, c_end = 0;
                if (c_arr.length == 2) {
                    c_start = Integer.parseInt(c_arr[0].trim());
                    c_val = 1;
                    c_end = Integer.parseInt(c_arr[1].trim());
                } else {
                    c_start = Integer.parseInt(c_arr[0].trim());
                    c_val = Integer.parseInt(c_arr[1].trim()) + 1;
                    c_end = Integer.parseInt(c_arr[2].trim());
                }
                new_mat = new double[(r_end - r_start) / r_val][(c_end - c_start) / c_val + 1];
                for(int i = 0; i < (r_end - r_start) / r_val + 1; i++) {
                    for(int j = 0; j < (c_end - c_start) / c_val + 1; j++) {
                        try {
                            new_mat[i][j] = this.mat[r_start + i*r_val][c_start + j*c_val];
                        } catch (Exception e) {
                            System.out.println("下标超出矩阵范围！");
                            System.exit(-1);
                        }
                    }
                }
            }
        }
        return new Matrix(new_mat);
    }

    /**
     * Gram-Schmidt正交化
     * @return 规范化的正交矩阵
     */
    public Matrix gs_orthogonalize() {
        // 复制一份作为副本，不改变原矩阵的值
        int size_r = this.row;
        int size_c = this.column;
        double[][] t_mat = copy(this.mat);
        // Gram-Schmidt正交化
        double[][] new_mat = new double[size_r][size_c];
        for(int i = 0; i < size_c; i++) {
            if (i == 0) {
                for(int j = 0; j < size_r; j++)
                    new_mat[j][i] = t_mat[j][i];
            } else {
                double[] t_col_vec = get_column_vector(t_mat, i); // beta_i
                for(int j = 0; j < i; j++) {
                    double[] t_col_vec_2 = get_column_vector(new_mat, j); // alpha_i
                    double factor = get_inner_product(get_column_vector(t_mat, i), t_col_vec_2) /
                            get_inner_product(t_col_vec_2, t_col_vec_2);
                    for(int k = 0; k < size_c; k++) {
                        t_col_vec_2[k] *= factor;
                        t_col_vec[k] -= t_col_vec_2[k];
                    }
                }
                // 赋值
                for(int j = 0; j < size_r; j++)
                    new_mat[j][i] = t_col_vec[j];
            }
        }
        // 规范化
        for(int i = 0; i < size_c; i++) {
            double norm = get_norm_p(get_column_vector(new_mat, i), 2);
            for(int j = 0; j < size_r;j++)
                new_mat[j][i] /= norm;
        }
        return new Matrix(new_mat);
    }

    /**
     * 求矩阵特征值
     * @return 方阵，主对角线上为特征值
     */
    public Matrix eigenVal() {
        Matrix A = new Matrix(copy(this.mat));
        Matrix A_new = null;
        QR_Decomposition qr;
        for(int i = 0; i < 20; i++) {
            qr = new QR_Decomposition(A);
            Matrix Q = new Matrix(qr.getQ());
            A_new = Q.trans().times(A).times(Q);
            A = A_new;
        }
        /*
        double[] eigen_val = new double[this.getRow()];
        for(int i = 0; i < this.getRow(); i++)
            eigen_val[i] = A_new.mat[i][i];

         */
        return A_new;
    }

    /**
     * 求矩阵的秩
     * @return
     */
    public int rank() {
        double[][] t_mat = copy(this.mat);
        for(int r = 0; r < this.row; r++) {
            // 将第i行的主元设置为非0元素
            int i = r;
            while (t_mat[i][r] == 0 && i + 1 < this.row) {
                if (i + 1 < this.row & t_mat[i+1][r] != 0) {
                    for (int k = 0; k < this.column - 1; k++)
                        Matrix.swap(t_mat, i, k, r, k);
                    break;
                }
                i++;
            }
            // 消元
            double pivot = t_mat[i][i];
            for(int k = i + 1; k < this.row; k++) {
                double multiplier = t_mat[k][i] / pivot; // 乘数
                for(int l = 0; l < this.column; l++)
                    t_mat[k][l] = t_mat[k][l] - t_mat[i][l]*multiplier;
            }
        }
        // 找到最后一个非0行所在的行标
        for(int r = 0; r < Math.min(this.row, this.column); r++) {
            double[] t = new double[this.column];
            if (Arrays.equals(t, t_mat[r]))
                return r;
        }
        return Math.min(this.row, this.column);
    }

    // p-范数
    public static double get_norm_p(double[] vector, int p) {
        double res = 0;
        for(int i = 0; i < vector.length; i++)
            res += Math.pow(vector[i], p);
        return Math.pow(res, 1/(double)p);
    }

    // 求向量内积
    public static double get_inner_product(double[] v1, double[] v2) {
        // 合法性检查
        if (v1.length != v2.length) {
            System.out.println("向量长度不相等，无法计算内积！");
            System.exit(-1);
        }
        double res = 0;
        for(int i = 0; i < v1.length; i++)
            res += v1[i]*v2[i];
        return res;
    }

    // Setter() and Getter()
    public double[][] getMat() {
        return mat;
    }

    public void setMat(double[][] mat) {
        this.mat = mat;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    // test function
    // 打印一个二维数组
    public static void print_tdarray(double[][] arr, String name) {
        int r = arr.length;
        int c = arr[0].length;
        System.out.println("----------------------");
        System.out.println(name + ":");
        for(int i = 0; i < r; i++) {
            for(int j = 0; j < c; j++)
                System.out.print(arr[i][j] + " ");
            System.out.println();
        }
    }

    public static void print_array(double[] arr, String name) {
        int len = arr.length;
        System.out.println("---------------------");;
        System.out.println(name + ":");
        for(int i = 0; i < len; i++)
            System.out.print(arr[i] + " ");
        System.out.println();
    }
}
