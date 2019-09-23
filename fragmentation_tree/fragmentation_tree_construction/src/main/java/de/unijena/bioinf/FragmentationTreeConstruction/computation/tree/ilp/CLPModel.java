package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

public class CLPModel {
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

    static int DOUBLE_SIZE = Native.getNativeSize(Double.class);

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("CLPModelWrapper", CLibrary.class);

        Pointer CLPModel_ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

        void CLPModel_dtor(Pointer self);

        double CLPModel_getInfinity(Pointer self);

        void CLPModel_setObjective(Pointer self, Memory objective, int len);

        void CLPModel_setColBounds(Pointer self, Memory col_lb, Memory col_ub, int len);

        void CLPModel_setColStart(Pointer self, double start[], int length);

        void CLPModel_addRow(Pointer self, double row[], int len, double lb, double ub);

        void CLPModel_addSparseRow(Pointer self, double elems[], int indices[], int len, double lb, double ub);

        int CLPModel_solve(Pointer self); // returns ReturnStatus

        Pointer CLPModel_getColSolution(Pointer self);

        double CLPModel_getScore(Pointer self);
    }

    private Pointer self;
    private int ncols;

    CLPModel(int ncols, int obj_sense) {
        this.ncols = ncols;
        self = CLibrary.INSTANCE.CLPModel_ctor(ncols, obj_sense);
    }

    double getInfinity() {
        return CLibrary.INSTANCE.CLPModel_getInfinity(self);
    }

    void setObjective(double objective[]) {
        Memory mem = new Memory(DOUBLE_SIZE * objective.length);
        for (int i = 0; i < objective.length; ++i)
            mem.setDouble(i * DOUBLE_SIZE, objective[i]);
        CLibrary.INSTANCE.CLPModel_setObjective(self, mem, objective.length);
        // TODO: make sure memory is freed up!!
    }

    void setColBounds(double col_lb[], double col_ub[]) {
        assert col_lb.length == col_ub.length;
        int len = col_lb.length;
        Memory mem_lb = new Memory(DOUBLE_SIZE * len);
        for (int i = 0; i < len; ++i)
            mem_lb.setDouble(i * DOUBLE_SIZE, col_lb[i]);
        Memory mem_ub = new Memory(DOUBLE_SIZE * len);
        for (int i = 0; i < len; ++i)
            mem_ub.setDouble(i * DOUBLE_SIZE, col_ub[i]);
        CLibrary.INSTANCE.CLPModel_setColBounds(self, mem_lb, mem_ub, len);
    }

    void setColStart(double start[]) {
        CLibrary.INSTANCE.CLPModel_setColStart(self, start, start.length);
    }

    void addRow(double row[], double lb, double ub) {
        CLibrary.INSTANCE.CLPModel_addRow(self, row, row.length, lb, ub);
    }

    void addSparseRow(double elems[], int indices[], double lb, double ub) {
        assert elems.length == indices.length;
        int len = elems.length;
        CLibrary.INSTANCE.CLPModel_addSparseRow(self, elems, indices, len, lb, ub);
    }

    int solve() {
        return CLibrary.INSTANCE.CLPModel_solve(self);
    }

    double[] getColSolution() {
        return CLibrary.INSTANCE.CLPModel_getColSolution(self).getDoubleArray(0, ncols);
    }

    double getScore() {
        return CLibrary.INSTANCE.CLPModel_getScore(self);
    }

    void dispose() {
        // is it necessary to clean up Memory objects?
        CLibrary.INSTANCE.CLPModel_dtor(self);
    }
}
