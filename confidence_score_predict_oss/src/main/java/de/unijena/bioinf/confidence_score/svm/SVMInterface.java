package de.unijena.bioinf.confidence_score.svm;




import java.util.Arrays;
import java.util.List;

/**
 * Created by Marcus Ludwig on 09.03.16.
 */
public interface SVMInterface<node extends SVMInterface.svm_node, problem extends SVMInterface.svm_problem<node>,  M> {
//    model extends SVMInterface.svm_model,

    node createSVM_Node(int index, double value);

    problem createSVM_Problem();

    SVMInterface.svm_model<M> svm_train(problem problem, svm_parameter parameter);

    double svm_predict(SVMInterface.svm_model<M> model, List<node> nodes);
//
Predictor getPredictor(SVMInterface.svm_model<M> model, double probA, double probB);


    abstract class svm_node{
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

    abstract class svm_problem<node>{
        public abstract void setL(int l);
        public abstract int getL();

        public abstract void setX(List<List<node>> svm_nodes);
        public abstract List<List<node>> getX();

        public abstract void setY(double[] y);
        public abstract double[] getY();
    }



    abstract class svm_model<M>{
        public abstract M getModel();
    }

    class svm_parameter{
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
        public int kernel_type = LINEAR;
        public int degree;
        public double gamma;
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

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("svm_parameter{");
            sb.append("svm_type=").append(svm_type);
            sb.append(", kernel_type=").append(kernel_type);
            sb.append(", degree=").append(degree);
            sb.append(", gamma=").append(gamma);
            sb.append(", cache_size=").append(cache_size);
            sb.append(", eps=").append(eps);
            sb.append(", C=").append(C);
            sb.append(", nr_weight=").append(nr_weight);
            sb.append(", weight_label=");
            if (weight_label == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < weight_label.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(weight_label[i]);
                sb.append(']');
            }
            sb.append(", weight=");
            if (weight == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < weight.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(weight[i]);
                sb.append(']');
            }
            sb.append(", p=").append(p);
            sb.append(", shrinking=").append(shrinking);
            sb.append(", probability=").append(probability);
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof svm_parameter)) return false;

            svm_parameter that = (svm_parameter) o;

            if (svm_type != that.svm_type) return false;
            if (kernel_type != that.kernel_type) return false;
            if (degree != that.degree) return false;
            if (Double.compare(that.gamma, gamma) != 0) return false;
            if (Double.compare(that.cache_size, cache_size) != 0) return false;
            if (Double.compare(that.eps, eps) != 0) return false;
            if (Double.compare(that.C, C) != 0) return false;
            if (nr_weight != that.nr_weight) return false;
            if (Double.compare(that.p, p) != 0) return false;
            if (shrinking != that.shrinking) return false;
            if (probability != that.probability) return false;
            if (!Arrays.equals(weight_label, that.weight_label)) return false;
            return Arrays.equals(weight, that.weight);

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = svm_type;
            result = 31 * result + kernel_type;
            result = 31 * result + degree;
            temp = Double.doubleToLongBits(gamma);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(cache_size);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(eps);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(C);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + nr_weight;
            result = 31 * result + Arrays.hashCode(weight_label);
            result = 31 * result + Arrays.hashCode(weight);
            temp = Double.doubleToLongBits(p);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + shrinking;
            result = 31 * result + probability;
            return result;
        }
    }
}
