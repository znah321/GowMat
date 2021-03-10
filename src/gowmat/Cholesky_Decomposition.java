package GowMat;

public class Cholesky_Decomposition {
    private double[][] L;
    private double[][] transL;

    public Cholesky_Decomposition(Matrix m) {
        int n = m.getRow();
        double[][] t_mat = Matrix.copy(m.getMat());
        this.L = new double[n][n];
        this.transL = new double[n][n];

        // Cholesky分解
        for(int k = 0; k < n; k++)
        {
            double sum = 0;
            for(int i = 0; i < k; i++)
                sum += this.L[k][i] * this.L[k][i];
            sum = t_mat[k][k] - sum;
            this.L[k][k] = Math.sqrt(sum > 0 ? sum : 0);
            for(int i = k + 1; i < n; i++)
            {
                sum = 0;
                for(int j = 0; j < k; j++)
                    sum += this.L[i][j] * this.L[k][j];
                this.L[i][k] = (t_mat[i][k] - sum) / this.L[k][k];
            }
            for(int j = 0; j < k; j++)
                this.L[j][k] = 0;
        }
        this.transL = new Matrix(this.L).trans().getMat();
        // https://blog.csdn.net/Hansry/article/details/102252365?utm_medium=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.control&dist_request_id=&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-BlogCommendFromMachineLearnPai2-1.control
    }

    public double[][] getL() {
        return L;
    }

    public double[][] getTransL() {
        return transL;
    }
}
