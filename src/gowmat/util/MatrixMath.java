package GowMat.util;
import GowMat.Matrix;

public class MatrixMath {
    /**
     * 将矩阵中的元素取2为底的对数
     * @param m
     * @return
     */
    public static Matrix log2(Matrix m) {
        double[][] t_mat= Matrix.copy(m.getMat());
        for(int i = 0; i < m.getRow(); i++)
            for(int j = 0; j < m.getColumn(); j++)
                t_mat[i][j] = Math.log(t_mat[i][j]) / Math.log(2);
        return new Matrix(t_mat, m.getRow(), m.getColumn());
    }

    /**
     * 将矩阵中的元素取10为底的对数
     * @param m
     * @return
     */
    public static Matrix lg(Matrix m) {
        double[][] t_mat= Matrix.copy(m.getMat());
        for(int i = 0; i < m.getRow(); i++)
            for(int j = 0; j < m.getColumn(); j++)
                t_mat[i][j] = Math.log10(t_mat[i][j]);
        return new Matrix(t_mat, m.getRow(), m.getColumn());
    }

    /**
     * 将矩阵中的元素取e为底的对数
     * @param m
     * @return
     */
    public static Matrix ln(Matrix m) {
        double[][] t_mat= Matrix.copy(m.getMat());
        for(int i = 0; i < m.getRow(); i++)
            for(int j = 0; j < m.getColumn(); j++)
                t_mat[i][j] = Math.log(t_mat[i][j]);
        return new Matrix(t_mat, m.getRow(), m.getColumn());
    }

    /**
     * 将矩阵中的元素取a为底的对数
     * @param m
     * @param a 底数(a > 0)
     * @return
     */
    public static Matrix log(Matrix m, double a) {
        double[][] t_mat= Matrix.copy(m.getMat());
        for(int i = 0; i < m.getRow(); i++)
            for(int j = 0; j < m.getColumn(); j++)
                t_mat[i][j] = Math.log(t_mat[i][j]) / Math.log(a);
        return new Matrix(t_mat, m.getRow(), m.getColumn());
    }

    /**
     * 符号函数sgn(x)
     * @param x
     * @return x > 0 ==> 返回1    x < 0 ==> 返回-1    x = 0 ==> 返回0
     */
    public static double sgn(double x) {
        if (x > 0)
            return 1;
        else if (x == 0)
            return 0;
        else
            return -1;
    }

    /**
     * 矩阵的pow函数
     * @param m
     * @param x 指数
     * @return
     */
    public static Matrix pow(Matrix m, double x) {
        double[][] t_mat = Matrix.copy(m.getMat());
        for(int i = 0; i < m.getRow(); i++)
            for(int j = 0; j < m.getColumn(); j++)
                t_mat[i][j] = Math.pow(t_mat[i][j], x);
        return new Matrix(t_mat);
    }

    /**
     * 获取主对角线上的元素
     * @param m
     * @return
     */
    public static double[] diag(Matrix m) {
        int len = Math.min(m.getRow(), m.getColumn());
        double[] res = new double[len];
        for(int i = 0; i < len; i++)
            res[i] = m.getMat()[i][i];
        return res;
    }
}
