package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;
import org.junit.Test;

public class ParameterTest {

    @Parameter class Algo {
        @Parameter public double alpha;
        @Parameter public int beta;
        @Parameter @Called("γ") public Complex gamma;
        @Parameter public Special f;
        public String noParam = "foo";
        public int noParam2 = 1;
    }

    @Parameter class Complex {
        @Parameter public int a;
        @Parameter public int b;

        @Parameter public String getText() {return "foo";}

        public int c = 3;
    }

    @Parameter(formatter = SpecialFormatter.class) class Special {

        @Parameter int a;
        @Parameter int b;
    }

    static class SpecialFormatter implements ParameterFormatter {

        @Override
        public <G, D, L> G format(DataDocument<G, D, L> document, Object val) {
            final Special value = (Special)val;
            return document.wrap("f(x) = " + value.a + "*x + " + value.b );
        }
    }

    @Test
    public void testParameter() {
        final Algo myAlgo = new Algo();
        myAlgo.alpha = 5.3;
        myAlgo.beta = 2;
        myAlgo.gamma = new Complex();
        myAlgo.gamma.a = 12;
        myAlgo.gamma.b = 8;
        myAlgo.f = new Special();
        myAlgo.f.a = 3;
        myAlgo.f.b = 2;

        System.out.println(new ObjectFormatter().format(new JDKDocument(), myAlgo));

    }

}
