package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import java.util.Stack;

public class CLPModel_JNI {

    public static interface ObjectiveSense {
        public static final int MAXIMIZE = -1;
        public static final int MINIMIZE = 1;
    }

    public static interface ReturnStatus {
        public static final int OPTIMAL = 0;
        public static final int INFEASIBLE = 1;
        public static final int ABANDONED = 2;
        public static final int LIMIT_REACHED = 3;
        public static final int UNKNOWN = 4;
    }

    static {
        System.loadLibrary("CLPModelWrapper_JNI");
    }

    private Stack<Double> row_elems = null;
    private Stack<Integer> row_indices = null;
    private Stack<Double> row_lb = null;
    private Stack<Double> row_ub = null;
    private Stack<Integer> row_starts = null;

    public CLPModel_JNI(int ncols, int obj_sense){
        ctor(ncols, obj_sense);
    }

    native void ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

    native void dispose();

    native double getInfinity();

    native void setObjective(double[] objective);

    native void setTimeLimit(double seconds);

    native void setColBounds(double[] col_lb, double[] col_ub);

    native void setColStart(double start[]);

    native void addFullRow(double row[], double lb, double ub);

    native void addSparseRow(double[] elems, int[] indices, double lb, double ub);

    native void addSparseRowCached(double[] elems, int[] indices, double lb, double ub);

    native void addSparseRows(int numrows, int rowstarts[], double elems[], int indices[], double lb[], double ub[]);

    native int solve(); // returns ReturnStatus

    native double[] getColSolution();

    native double getScore();

    void j_addSparseRowCached(double elems[], int indices[], double lb, double ub){
        if (row_elems == null){
            row_elems = new Stack<Double>();
            row_indices = new Stack<Integer>();
            row_lb = new Stack<Double>();
            row_ub = new Stack<Double>();
            row_starts = new Stack<Integer>();
            row_starts.push(0);
        }

        for (int i = 0; i < elems.length; ++i){
            row_elems.push(elems[i]);
            row_indices.push(indices[i]);
        }

        row_lb.push(lb);
        row_ub.push(ub);
        row_starts.push(row_starts.lastElement() + elems.length);
    }

    int j_solve(){
        if (row_elems != null){
            int numrows = row_lb.size();
            int numelems = row_elems.size();
            double arr_row_lb[] = new double[row_lb.size()];
            double arr_row_ub[] = new double[row_ub.size()];
            int arr_row_starts[] = new int[row_starts.size()];
            double arr_row_elems[] = new double[row_elems.size()];
            int arr_row_indices[] = new int[row_indices.size()];
            arr_row_starts[numrows] = row_starts.pop();
            for (int i = numrows - 1; i >= 0; --i){
                arr_row_lb[i] = row_lb.pop();
                arr_row_ub[i] = row_ub.pop();
                arr_row_starts[i] = row_starts.pop();
            }
            for (int i = numelems - 1; i >= 0; --i){
                arr_row_elems[i] = row_elems.pop();
                arr_row_indices[i] = row_indices.pop();
            }
            addSparseRows(numrows, arr_row_starts, arr_row_elems, arr_row_indices,
                          arr_row_lb, arr_row_ub);
        }
        return solve();
    }
}
