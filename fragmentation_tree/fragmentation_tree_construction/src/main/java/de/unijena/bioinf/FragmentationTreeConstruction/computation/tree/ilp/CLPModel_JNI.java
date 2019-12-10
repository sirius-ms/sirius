package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // needed, because JNI libraries are loaded as static, multiple
    // instances are managed in the Wrapper class and accessed by index
    private int wrapper_index = 0;

    public CLPModel_JNI(int ncols, int obj_sense){
        wrapper_index = n_ctor(ncols, obj_sense);
    }

    native int n_ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

    native void n_dispose(int self);

    native double n_getInfinity(int self);

    native void n_setObjective(int self, double[] objective);

    native void n_setTimeLimit(int self, double seconds);

    native void n_setColBounds(int self, double[] col_lb, double[] col_ub);

    native void n_setColStart(int self, double start[]);

    native void n_addFullRow(int self, double row[], double lb, double ub);

    native void n_addSparseRow(int self, double[] elems, int[] indices, double lb, double ub);

    native void n_addSparseRowCached(int self, double[] elems, int[] indices, double lb, double ub);

    native void n_addSparseRows(int self, int numrows, int rowstarts[], double elems[], int indices[], double lb[], double ub[]);

    native int n_solve(int self); // returns ReturnStatus

    native double[] n_getColSolution(int self);

    native double n_getScore(int self);

    void dispose(){
        n_dispose(wrapper_index);
    }

    double getInfinity(){
        return n_getInfinity(wrapper_index);
    }

    void setObjective(double[] objective){
        n_setObjective(wrapper_index, objective);
    }

void setTimeLimit(double seconds){
        n_setTimeLimit(wrapper_index, seconds);
    }

void setColBounds(double[] col_lb, double[] col_ub){
        n_setColBounds(wrapper_index, col_lb, col_ub);
    }

void setColStart(double start[]){
        n_setColStart(wrapper_index, start);
    }

void addFullRow(double row[], double lb, double ub){
        n_addFullRow(wrapper_index, row, lb, ub);
    }

void addSparseRow(double[] elems, int[] indices, double lb, double ub){
        n_addSparseRow(wrapper_index, elems, indices, lb, ub);
    }

void addSparseRowCached(double[] elems, int[] indices, double lb, double ub){
        n_addSparseRowCached(wrapper_index, elems, indices, lb, ub);
    }

void addSparseRows(int numrows, int rowstarts[], double elems[], int indices[], double lb[], double ub[]){
        n_addSparseRows(wrapper_index, numrows, rowstarts, elems, indices, lb, ub);
    }

    int solve(){ // returns ReturnStatus
        return n_solve(wrapper_index);
    }

    double[] getColSolution(){
        return n_getColSolution(wrapper_index);
    }

    double getScore(){
        return n_getScore(wrapper_index);
    }
}
