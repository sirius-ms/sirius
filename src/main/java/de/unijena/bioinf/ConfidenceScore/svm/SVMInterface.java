package de.unijena.bioinf.ConfidenceScore.svm;


import java.util.List;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public interface SVMInterface<node extends SVMInterface.svm_node, problem extends SVMInterface.svm_problem<node>,  M> {
//    model extends SVMInterface.svm_model,

    public node createSVM_Node(int index, double value);

    public problem createSVM_Problem();

    public SVMInterface.svm_model<M> svm_train(problem problem, svm_parameter parameter);

    public double svm_predict(SVMInterface.svm_model<M> model, List<node> nodes);
//
    public LinearSVMPredictor getPredictor(SVMInterface.svm_model<M> model, double probA, double probB);


    public abstract class svm_node{
        protected int index;
        protected double value;

        public svm_node(int index, double value){
            this.index = index;
            this.value = value;
        }
//        public abstract void setIndex(int i);
        public abstract void setValue(double v);

        public abstract int getIndex();
        public abstract double getValue();
    }

    public abstract class svm_problem<node>{
        public abstract void setL(int l);
        public abstract int getL();

        public abstract void setX(List<List<node>> svm_nodes);
        public abstract List<List<node>> getX();

        public abstract void setY(double[] y);
        public abstract double[] getY();
    }



    public abstract class svm_model<M>{
        public abstract M getModel();
    }

    public class svm_parameter{
        public static final int C_SVC = 0;
//        public static final int NU_SVC = 1;
//        public static final int ONE_CLASS = 2;
//        public static final int EPSILON_SVR = 3;
//        public static final int NU_SVR = 4;
        public static final int LINEAR = 0;
        public static final int POLY = 1;
        public static final int RBF = 2;
//        public static final int SIGMOID = 3;
//        public static final int PRECOMPUTED = 4;
        public final int svm_type = C_SVC;
        public final int kernel_type = LINEAR;
//        public int degree;
//        public double gamma;
//        public double coef0;
        public double cache_size;
        public double eps;
        public double C;
        public int nr_weight;
        public int[] weight_label;
        public double[] weight;
//        public double nu;
        public double p;
        public int shrinking;
        public int probability;

        public svm_parameter() {
        }

    }
}
