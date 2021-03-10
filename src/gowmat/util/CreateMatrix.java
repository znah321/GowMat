package GowMat.util;
import GowMat.Matrix;

public class CreateMatrix {

    /**
     * 生成一个n阶的单位矩阵
     * @param n 阶数
     * @return 单位矩阵
     */
    public static Matrix eye(int n) {
        double[][] I = new double[n][n];
        for(int i = 0; i < n; i++)
            I[i][i] = 1;
        return new Matrix(I, n, n);
    }

    /**
     * 生成一个n阶的零矩阵
     * @param n
     * @return
     */
    public static Matrix zeros(int n) {
        double[][] mat = new double[n][n];
        return new Matrix(mat, n, n);
    }

    /**
     * 生成一个n行m列的零矩阵
     * @param n
     * @param m
     */
    public static Matrix zeros(int n, int m) {
        double[][] mat = new double[n][m];
        return new Matrix(mat, n, m);
    }

    /**
     * 随机生成一个n阶矩阵
     * @param n
     * @param lb 元素下界
     * @param ub 元素上界
     * @return
     */
    public static Matrix random(int n, double lb, double ub) {
        double[][] mat = new double[n][n];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
                mat[i][j] = Math.random() * (ub - lb) + lb;
        return new Matrix(mat, n, n);
    }

    /**
     * 随机生成一个n行m列的矩阵
     * @param n
     * @param m
     * @param lb
     * @param ub
     * @return
     */
    public static Matrix random(int n, int m, double lb, double ub) {
        double[][] mat = new double[n][m];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < m; j++)
                mat[i][j] = Math.random() * (ub - lb) + lb;
        return new Matrix(mat, n, m);
    }

    /**
     * 随机生成一个n阶的矩阵，元素均为整数
     * @param n
     * @param lb
     * @param ub
     * @return
     */
    public static Matrix randi(int n, int lb, int ub) {
        double[][] mat = new double[n][n];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n;j++)
                mat[i][j] = Math.floor(Math.random() * (ub - lb) + lb);
        return new Matrix(mat, n, n);
    }

    /**
     *  随机生成一个n行m列的矩阵，元素均为整数
     * @param n
     * @param m
     * @param lb
     * @param ub
     * @return
     */
    public static Matrix randi(int n, int m, int lb, int ub) {
        double[][] mat = new double[n][m];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < m;j++)
                mat[i][j] = Math.floor(Math.random() * (ub - lb) + lb);
        return new Matrix(mat, n, m);
    }

    /**
     * 创建对角矩阵
     * @param elem 主对角线上的元素
     * @return
     */
    public static Matrix diag(double[] elem) {
        int n = elem.length;
        double[][] mat = new double[n][n];
        for(int i = 0; i < n; i++)
            mat[i][i] = elem[i];
        return new Matrix(mat);
    }

}
