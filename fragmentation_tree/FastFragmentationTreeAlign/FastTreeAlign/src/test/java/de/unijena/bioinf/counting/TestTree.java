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
package de.unijena.bioinf.counting;

import de.unijena.bioinf.graphUtils.tree.TreeAdapter;
import de.unijena.bioinf.graphUtils.tree.TreeType;
import de.unijena.bioinf.treealign.scoring.SimpleEqualityScoring;

import java.util.ArrayList;
import java.util.List;

public class TestTree implements TreeType<TestTree> {

    public ArrayList<TestTree> children;
    private String label;

    public TestTree(String label) {
        this.children = new ArrayList<TestTree>();
        this.label = label;
    }

    public static TestTree node(String label, TestTree... nodes) {
        final TestTree t = new TestTree(label);
        for (TestTree u : nodes) t.children.add(u);
        return t;
    }

    public static SimpleEqualityScoring<TestTree> getScoring() {
        return new SimpleEqualityScoring<TestTree>() {
            @Override
            public boolean isMatching(TestTree left, TestTree right) {
                return left.label.equals(right.label);
            }
        };
    }

    public static TreeAdapter<TestTree> getAdapter() {
        return new Adapter<TestTree>();
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int degree() {
        return children().size();
    }

    @Override
    public List<TestTree> children() {
        return children;
    }
}
