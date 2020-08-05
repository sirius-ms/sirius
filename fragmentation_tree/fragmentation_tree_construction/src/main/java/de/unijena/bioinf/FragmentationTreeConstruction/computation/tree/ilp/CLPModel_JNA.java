/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.ilp;

import java.util.Stack;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

public class CLPModel_JNA {
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
    static int INT_SIZE = Native.getNativeSize(Integer.class);

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("CLPModelWrapper_JNA", CLibrary.class);

        Pointer CLPModel_ctor(int ncols, int obj_sense); // obj_sense: ObjectiveSense

        void CLPModel_dtor(Pointer self);

        double CLPModel_getInfinity(Pointer self);

        void CLPModel_setObjective(Pointer self, Memory objective, int len);

        void CLPModel_setTimeLimit(Pointer self, double seconds);

        void CLPModel_setColBounds(Pointer self, Memory col_lb, Memory col_ub, int len);

        void CLPModel_setColStart(Pointer self, double start[], int length);

        void CLPModel_addFullRow(Pointer self, double row[], int len, double lb, double ub);

        void CLPModel_addSparseRow(Pointer self, Memory elems, Memory indices, int len, double lb, double ub);

        void CLPModel_addSparseRows(Pointer self, int numrows, Memory rowstarts, Memory elems, Memory indices, int len, Memory lb, Memory ub);

        int CLPModel_solve(Pointer self); // returns ReturnStatus

        Pointer CLPModel_getColSolution(Pointer self);

        double CLPModel_getScore(Pointer self);
    }

    private Pointer self;
    private int ncols;
    // NOTE: for some reason, Memory still gets GCed.
    // this should ensure that the variables are GCed with this instance
    private Memory obj_mem;
    private Memory mem_col_lb;
    private Memory mem_col_ub;
    private Memory mem_row_lb;
    private Memory mem_row_ub;
    private Memory mem_row_starts;
    private Memory mem_row_elems;
    private Memory mem_row_indices;
    private Stack<Double> row_elems = null;
    private Stack<Integer> row_indices = null;
    private Stack<Double> row_lb = null;
    private Stack<Double> row_ub = null;
    private Stack<Integer> row_starts = null;

    CLPModel_JNA(int ncols, int obj_sense) {
        this.ncols = ncols;
        self = CLibrary.INSTANCE.CLPModel_ctor(ncols, obj_sense);
    }

    double getInfinity() {
        return CLibrary.INSTANCE.CLPModel_getInfinity(self);
    }

    void setObjective(double objective[]) {
        obj_mem = new Memory(DOUBLE_SIZE * objective.length);
        for (int i = 0; i < objective.length; ++i)
            obj_mem.setDouble(i * DOUBLE_SIZE, objective[i]);
        CLibrary.INSTANCE.CLPModel_setObjective(self, obj_mem, objective.length);
        // TODO: make sure memory is freed up!!
    }

    void setTimeLimit(double seconds){
        CLibrary.INSTANCE.CLPModel_setTimeLimit(self, seconds);
    }

    void setColBounds(double col_lb[], double col_ub[]) {
        assert col_lb.length == col_ub.length;
        int len = col_lb.length;
        mem_col_lb = new Memory(DOUBLE_SIZE * len);
        for (int i = 0; i < len; ++i)
            mem_col_lb.setDouble(i * DOUBLE_SIZE, col_lb[i]);
        mem_col_ub = new Memory(DOUBLE_SIZE * len);
        for (int i = 0; i < len; ++i)
            mem_col_ub.setDouble(i * DOUBLE_SIZE, col_ub[i]);
        CLibrary.INSTANCE.CLPModel_setColBounds(self, mem_col_lb, mem_col_ub, len);
    }

    void setColStart(double start[]) {
        CLibrary.INSTANCE.CLPModel_setColStart(self, start, start.length);
    }

    void addFullRow(double row[], double lb, double ub) {
        CLibrary.INSTANCE.CLPModel_addFullRow(self, row, row.length, lb, ub);
    }

    void addSparseRow(double elems[], int indices[], double lb, double ub) {
        assert elems.length == indices.length;
        int len = elems.length;
        Memory mem_elems = new Memory(DOUBLE_SIZE * len);
        Memory mem_indices = new Memory(INT_SIZE * len);
        for (int i = 0; i < len; ++i){
            mem_elems.setDouble(i * DOUBLE_SIZE, elems[i]);
            mem_indices.setInt(i * INT_SIZE, indices[i]);
        }
        CLibrary.INSTANCE.CLPModel_addSparseRow(self, mem_elems, mem_indices, len, lb, ub);

    }

    void addSparseRows(int numrows, Memory rowstarts, Memory elems, Memory indices, int len, Memory lb, Memory ub){
        CLibrary.INSTANCE.CLPModel_addSparseRows(self, numrows, rowstarts, elems, indices, len, lb, ub);
    }

    int solve() {
        if (row_elems != null){
            int numrows = row_lb.size();
            int numelems = row_elems.size();
            mem_row_lb = new Memory(DOUBLE_SIZE * numrows);
            mem_row_ub = new Memory(DOUBLE_SIZE * row_ub.size());
            mem_row_starts = new Memory(INT_SIZE * row_starts.size());
            mem_row_elems = new Memory(DOUBLE_SIZE * numelems);
            mem_row_indices = new Memory(INT_SIZE * row_indices.size());
            mem_row_starts.setInt(numrows * INT_SIZE, row_starts.pop());
            for (int i = numrows - 1; i >= 0; --i){
                mem_row_lb.setDouble(i * DOUBLE_SIZE, row_lb.pop());
                mem_row_ub.setDouble(i * DOUBLE_SIZE, row_ub.pop());
                mem_row_starts.setInt(i * INT_SIZE, row_starts.pop());
            }
            for (int i = numelems - 1; i >= 0; --i){
                mem_row_elems.setDouble(i * DOUBLE_SIZE, row_elems.pop());
                mem_row_indices.setInt(i * INT_SIZE, row_indices.pop());
            }
            addSparseRows(numrows, mem_row_starts, mem_row_elems, mem_row_indices, numelems, mem_row_lb, mem_row_ub);
        }
        int return_code = CLibrary.INSTANCE.CLPModel_solve(self);
        return return_code;
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

    /////////////////////////
    // JAVA only functions //
    /////////////////////////

    // does not call CLibrary addSparseRow everytime
    // instead, stores rows and calls addSparseRows() add the end
    void addSparseRowCached(double elems[], int indices[], double lb, double ub){
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
        row_starts.push(row_starts.peek() + elems.length);
    }
}
