
package de.unijena.bioinf.counting;

import org.junit.Test;

import static de.unijena.bioinf.counting.TestTree.node;
import static org.junit.Assert.assertEquals;

public class WeightCounterTest {

    @Test
    public void testUnitCount() {
        TestTree t =
        node("r",
                node("a",
                    node("b"),
                    node("c"),
                    node("d",
                            node("e"),
                            node("f")
                    )
                )
        );
        assertEquals(26, (int)alignUnit(t, t));
        t = node("a", node("b", node("c", node("d"))));
        assertEquals(12, (int)alignUnit(t, t));


    }

    private double alignUnit(TestTree a , TestTree b) {
        final Weighting<TestTree> weights = new Weighting<TestTree>() {
            @Override
            public double weight(TestTree u, TestTree v) {
                return 1;
            }
        };
        return new WeightedPathCounting<TestTree>(TestTree.getScoring(), weights, a, b, TestTree.getAdapter()).compute();
    }

}
