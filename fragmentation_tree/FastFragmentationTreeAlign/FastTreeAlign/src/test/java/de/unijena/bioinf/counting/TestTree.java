
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
