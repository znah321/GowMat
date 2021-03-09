package matrix.goweild;

public class LU_Decomposition {
    private double[][] L; //
    private double[][] U;
    private int n;

    public LU_Decomposition(Matrix m) {
        // 输入合法性检查
        if (m.getColumn() != m.getRow()) {
            System.out.println("矩阵必须是方阵！");
            System.exit(-1);
        }

        // 复制一份作为副本，不改变原矩阵的值
        double[][] t_mat = Matrix.copy(m.getMat());
        int size_n = t_mat.length;

        double[][] U = new double[size_n][size_n];
        double[][] L = new double[size_n][size_n];
        for(int i = 0; i < size_n; i++) {
            U[0][i] = t_mat[0][i];
            L[i][0] = t_mat[i][0] / U[0][0];
        }
        for(int r = 1; r < size_n; r++) {
            for(int i = r; i < size_n; i++) {
                U[r][i] = t_mat[r][i] - sum_Lrk_Ukj(L, U, r, i);
                if (i == r)
                    L[r][r] = 1;
                else if (r == size_n)
                    L[size_n][size_n] = 1;
                else
                    L[i][r] = (t_mat[i][r] - sum_Lik_Ukr(L, U, i, r)) / U[r][r];
            }
        }

        this.L = L;
        this.U = U;
        this.n = size_n;
    }

    private static double sum_Lrk_Ukj(double[][] L, double[][] U, int r, int j) {
        double res = 0;
        for(int k = 0; k < r; k++)
            res += L[r][k]*U[k][j];
        return res;
    }
    private static double sum_Lik_Ukr(double[][] L, double[][] U, int i, int r) {
        double res = 0;
        for(int k = 0; k < r; k++)
            res += L[i][k]*U[k][r];
        return res;
    }

    public double[][] getL() {
        return L;
    }

    public double[][] getU() {
        return U;
    }

    public int getN() {
        return n;
    }
}
