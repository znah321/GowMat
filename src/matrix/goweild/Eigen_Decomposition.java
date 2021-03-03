package matrix.goweild;

import matrix.goweild.util.CreateMatrix;

public class Eigen_Decomposition {
    private double[] eig_value;
    private double[][] eig_vector;

    public Eigen_Decomposition(Matrix m) {
        double[][] t_mat = Matrix.copy(m.getMat());
        int n = m.getRow();
        this.eig_value = new double[n];
        Matrix eig_vector = CreateMatrix.eye(n);
        Matrix A_new = null;
        // 基于Jacobi迭代法
        int gen = 0;
        for(; gen < 20; gen++) {
            // 找到除主对角线上的绝对值最大的元素
            double max_elem = Math.abs(t_mat[0][1]);
            int p = 0, q = 1;
            for(int i = 0; i < n; i++) {
                for(int j = 0; j < n; j++) {
                    if (Math.abs(t_mat[i][j]) > Math.abs(max_elem) && j != i) {
                        max_elem = t_mat[i][j];
                        p = i;
                        q = j;
                    }
                }
            }
            // 构造正交矩阵
            double[][] Q = CreateMatrix.eye(n).getMat();
            double s = (t_mat[q][q] - t_mat[p][p]) / (2 * t_mat[p][q]);
            double t;
            if (s == 0)
                t = 1;
            else {
                double t_1 = -s - Math.pow(s*s+1, 0.5);
                double t_2 = -s + Math.pow(s*s+1, 0.5);
                if (Math.abs(t_1) > Math.abs(t_2))
                    t = t_2;
                else
                    t = t_1;
            }
            double c = 1.0 / Math.sqrt(1.0 + t*t);
            double d = t / Math.sqrt(1.0 + t*t);
            Q[p][p] = c;
            Q[p][q] = d;
            Q[q][p] = -d;
            Q[q][q] = c;
            // 计算: A_(k+1) = Q' * A_(k) * Q
            Matrix A = new Matrix(t_mat);
            Matrix Q_mat = new Matrix(Q);
            A_new = Q_mat.trans().times(A).times(Q_mat);
            // 更新A、t_mat
            t_mat = Matrix.copy(A_new.getMat());
            A = A_new;
            eig_vector = eig_vector.times(new Matrix((Q)));
        }
        for(int i = 0; i < n; i++)
            this.eig_value[i] = A_new.getMat()[i][i];
        this.eig_vector = eig_vector.getMat();
    }

    public double[] getEig_value() {
        return eig_value;
    }

    public double[][] getEig_vector() {
        return eig_vector;
    }
}
