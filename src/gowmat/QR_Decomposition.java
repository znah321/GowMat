package matrix.goweild;

public class QR_Decomposition {
    private double[][] Q;
    private double[][] R;
    private int n;

    public QR_Decomposition(Matrix m) {
        // 复制一份作为副本，不改变m
        double[][] t_mat = Matrix.copy(m.getMat());
        Matrix A = new Matrix(t_mat);
        this.n = m.getRow();

        /*基于Doolittle分解的QR分解 */
        // Step 1-计算A的转置
        Matrix A_t = A.trans();

        // Step 2-计算A_t * A
        Matrix C = A_t.times(A);

        // Step 3-实现矩阵C的Doolittle分解式
        LU_Decomposition C_lu = new LU_Decomposition(C);
        double[][] L = C_lu.getL();
        double[][] U = C_lu.getU();

        // Step 4-计算L的逆矩阵
        double[][] L_inv = new double[this.n][this.n];
        for(int i = 0; i < this.n; i++) {
            L_inv[i][i] = 1 / L[i][i];
            for(int k = i + 1; k < this.n; k++) {
                for(int j = i; j < k; j++)
                    L_inv[k][i] -= L[k][j]*L_inv[j][i];
            }
        }

        // Step 5-计算M = L_inv * A_t
        Matrix M = (new Matrix(L_inv)).times(A_t);

        // Step 6-计算Q,R
        double[][] Q = new double[this.n][this.n];
        for(int i = 0; i < this.n; i++)
            for(int j = 0; j < this.n; j++)
                Q[i][j] = M.getMat()[j][i] / Math.pow(U[j][j], 0.5);
        double[][] R = new double[this.n][this.n];
        for(int i = 0; i < this.n; i++)
            for(int j = i; j < this.n; j++)
                R[i][j] = U[i][j] / Math.pow(U[j][j], 0.5);

        this.Q = Q;
        this.R = R;

        // 参考文献：[1]冯天祥,李世宏.矩阵的QR分解[J].西南民族学院学报(自然科学版),2001(04):418-421.
    }

    public double[][] getQ() {
        return Q;
    }

    public double[][] getR() {
        return R;
    }
}
