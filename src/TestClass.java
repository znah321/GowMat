import matrix.goweild.Eigen_Decomposition;
import matrix.goweild.Matrix;
import matrix.goweild.SingularValue_Decomposition;

public class TestClass {
    public static void main(String[] args) {
        Matrix A = new Matrix("[4,2,2;2,5,1;2,1,6]");
//        System.out.println(A.rank());
        Eigen_Decomposition eig = new Eigen_Decomposition(A);
        Matrix.print_array(eig.getEig_value(), "Value");
        Matrix.print_tdarray(eig.getEig_vector(), "Vector");
        SingularValue_Decomposition svd = new SingularValue_Decomposition(A);
        Matrix.print_tdarray(svd.getU(),"U");
        Matrix.print_tdarray(svd.getS(),"S");
        Matrix.print_tdarray(svd.getV(),"V");
    }
}
