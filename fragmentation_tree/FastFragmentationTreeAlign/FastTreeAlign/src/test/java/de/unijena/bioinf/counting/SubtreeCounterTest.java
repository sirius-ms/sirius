
package de.unijena.bioinf.counting;

import org.junit.Test;

import static de.unijena.bioinf.counting.TestTree.node;
import static org.junit.Assert.assertEquals;

public class SubtreeCounterTest {

    @Test
    public void testSelfPath() {
        TestTree s, t;
        s = t = node("R",
                    node("A"),
                    node("B")
        );
        assertEquals(3, align(s, t));
        s = node("R",
                node("A",
                        node("B")));
        t = node("R",
                node("B",
                        node("A")));
        assertEquals(2, align(s, t));
        t = s;
        assertEquals(3, align(s, t));
        s = node("R",
                node("a"),
                node("b",
                        node("c")
                )
        );
        t = s;
        // TODO: Nachrechnen =(
        assertEquals(6, align(s, t));

        s = node("R",
                node("a"),
                node("b",
                        node("a")
                )
        );
        t = s;
        assertEquals(8, align(s, t));




    }

    private long align(TestTree a , TestTree b) {
        return new DPSubtreeCounter<TestTree>(TestTree.getScoring(), a, b, TestTree.getAdapter()).compute();
    }

}
