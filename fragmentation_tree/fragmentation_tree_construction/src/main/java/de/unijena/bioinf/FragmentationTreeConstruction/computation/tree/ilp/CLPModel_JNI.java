package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

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
        try {
            System.loadLibrary("CLPModelWrapper_JNI");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // needed, because JNI libraries are loaded as static, multiple
    // instances are managed in the Wrapper class and accessed by index
    private long wrapper_ptr;

    public CLPModel_JNI(int ncols, int obj_sense){
        wrapper_ptr = n_ctor(ncols, obj_sense);
    }

    native long n_ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

    native void n_dispose(long self);

    native double n_getInfinity(long self);

    native void n_setObjective(long self, double[] objective);

    native void n_setTimeLimit(long self, double seconds);

    native void n_setColBounds(long self, double[] col_lb, double[] col_ub);

    native void n_setColStart(long self, double start[]);

    native void n_addFullRow(long self, double row[], double lb, double ub);

    native void n_addSparseRow(long self, double[] elems, int[] indices, double lb, double ub);

    native void n_addSparseRowCached(long self, double[] elems, int[] indices, double lb, double ub);

    native void n_addSparseRows(long self, int numrows, int rowstarts[], double elems[], int indices[], double lb[], double ub[]);

    native int n_solve(long self); // returns ReturnStatus

    native double[] n_getColSolution(long self);

    native double n_getScore(long self);

    void dispose(){
        n_dispose(wrapper_ptr);
    }

    double getInfinity(){
        return n_getInfinity(wrapper_ptr);
    }

    void setObjective(double[] objective){
        n_setObjective(wrapper_ptr, objective);
    }

void setTimeLimit(double seconds){
        n_setTimeLimit(wrapper_ptr, seconds);
    }

void setColBounds(double[] col_lb, double[] col_ub){
        n_setColBounds(wrapper_ptr, col_lb, col_ub);
    }

void setColStart(double start[]){
        n_setColStart(wrapper_ptr, start);
    }

void addFullRow(double row[], double lb, double ub){
        n_addFullRow(wrapper_ptr, row, lb, ub);
    }

void addSparseRow(double[] elems, int[] indices, double lb, double ub){
        n_addSparseRow(wrapper_ptr, elems, indices, lb, ub);
    }

void addSparseRowCached(double[] elems, int[] indices, double lb, double ub){
        n_addSparseRowCached(wrapper_ptr, elems, indices, lb, ub);
    }

void addSparseRows(int numrows, int rowstarts[], double elems[], int indices[], double lb[], double ub[]){
        n_addSparseRows(wrapper_ptr, numrows, rowstarts, elems, indices, lb, ub);
    }

    int solve(){ // returns ReturnStatus
        return n_solve(wrapper_ptr);
    }

    double[] getColSolution(){
        return n_getColSolution(wrapper_ptr);
    }

    double getScore(){
        return n_getScore(wrapper_ptr);
    }
}
