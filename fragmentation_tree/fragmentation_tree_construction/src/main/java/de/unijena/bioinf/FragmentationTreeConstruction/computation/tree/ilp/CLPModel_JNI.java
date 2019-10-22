package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;
public class CLPModel_JNI {

    native void ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

    native void dispose();

    native double getInfinity();

    native void setObjective(double[] objective, int len);

    native void setTimeLimit(double seconds);

    native void setColBounds(double[] col_lb, double[] col_ub, int len);

    native void setColStart(double start[], int length);

    native void addRow(double row[], int len, double lb, double ub);

    native void addSparseRow(double[] elems, int[] indices, int len, double lb, double ub);

    native void addSparseRows(int numrows, int rowstarts[], double elems[], int indices[], int len, double lb[], double ub[]);

    native int solve(); // returns ReturnStatus

    native double[] getColSolution();

    native double getScore();
}
