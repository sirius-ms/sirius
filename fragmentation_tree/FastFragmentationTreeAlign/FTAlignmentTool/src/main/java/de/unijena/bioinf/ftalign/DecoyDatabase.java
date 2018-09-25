/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ftalign.analyse.FTDataElement;
import de.unijena.bioinf.treealign.TreeAlignmentAlgorithm;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DecoyDatabase {

    private List<FTDataElement> query;
    private List<FTDataElement> database;
    private List<FTDataElement> decoy;
    private TreeAlignmentAlgorithm.Factory<FragmentationTree> factory;
    private double[][] matrix;

    private boolean leaveOneOut;

    public DecoyDatabase(boolean leaveOneOut) {
        this.leaveOneOut = leaveOneOut;
    }

    public DecoyDatabase() {
        this(false);
    }

    public void computeQValues() {
        for (int i=0; i < database.size(); ++i) {
            for (int j=0; j < database.size(); ++j) {
                compute(i, j);
            }
        }
    }

    public void computeQValuesInParallel() {
        final ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i=0; i < database.size(); ++i) {
            for (int j=0; j < database.size(); ++j) {
                if (j <= i && query==database) continue;
                final int xi=i, xj=j;
                exec.submit(new Runnable() {
                    @Override
                    public void run() {
                        compute(xi, xj);
                    }
                });
            }
        }
        exec.shutdown();
        try {
            exec.awaitTermination(1000, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void compute(int left, int right) {
        // search query in (leave-one-out) database
        final FTDataElement a = query.get(left);
        final FTDataElement b = query.get(left);
        if (a.equals(b) && leaveOneOut) return;
        matrix[left][right] = factory.create(a.getTree(), b.getTree()).compute();
        if (query==database) matrix[right][left]=matrix[left][right];
    }


    public TreeAlignmentAlgorithm.Factory<FragmentationTree> getFactory() {
        return factory;
    }

    public void setFactory(TreeAlignmentAlgorithm.Factory<FragmentationTree> factory) {
        this.factory = factory;
    }

    public List<FTDataElement> getQuery() {
        return query;
    }

    public void setQuery(List<FTDataElement> query) {
        this.query = query;
    }

    public List<FTDataElement> getDatabase() {
        return database;
    }

    public void setDatabase(List<FTDataElement> database) {
        this.database = database;
    }

    public List<FTDataElement> getDecoy() {
        return decoy;
    }

    public void setDecoy(List<FTDataElement> decoy) {
        this.decoy = decoy;
    }

    public boolean isLeaveOneOut() {
        return leaveOneOut;
    }

    public void setLeaveOneOut(boolean leaveOneOut) {
        this.leaveOneOut = leaveOneOut;
    }
}
             */
