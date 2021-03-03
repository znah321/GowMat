import matrix.goweild.Cholesky_Decomposition;
import matrix.goweild.Eigen_Decomposition;
import matrix.goweild.Matrix;
import matrix.goweild.SingularValue_Decomposition;

public class TestClass {
    public static void main(String[] args) {
        Matrix A = new Matrix("[4,-1,1;-1,4.25,2.75;1,2.75,3.5]");
        Cholesky_Decomposition cd = new Cholesky_Decomposition(A);
        Matrix.print_tdarray(cd.getL(), "L");
        Matrix.print_tdarray(cd.getTransL(), "transL");
    }
}
