package matrix.goweild;

public class SingularValue_Decomposition {
    private double[][] U;
    private double[][] S;
    private double[][] V;
    private int r;
    private int c;

    public SingularValue_Decomposition(Matrix m) {
        this.r = m.getRow();
        this.c = m.getColumn();
        Matrix A = new Matrix(Matrix.copy(m.getMat()));
        Matrix A_t = A.trans();
        this.V = new Eigen_Decomposition(A_t.times(A)).getEig_vector();
        this.U = new Eigen_Decomposition(A.times(A_t)).getEig_vector();
        this.S = new double[this.r][this.c];
        for(int i = 0; i < Math.min(this.r, this.c); i++)
            this.S[i][i] = Math.sqrt(A.times(A_t).eigenVal().getMat()[i][i]);
    }

    public double[][] getU() {
        return U;
    }

    public double[][] getS() {
        return S;
    }

    public double[][] getV() {
        return V;
    }
}
