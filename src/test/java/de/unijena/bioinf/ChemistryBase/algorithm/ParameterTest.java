package de.unijena.bioinf.ChemistryBase.algorithm;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;
import org.junit.Test;

import java.lang.reflect.Field;

public class ParameterTest {

    @Parameter class Algo {
        @Parameter public double alpha;
        @Parameter public int beta;
        @Parameter @Called("γ") public Complex gamma;
        public String noParam = "foo";
        public int noParam2 = 1;
    }

    @Parameter class Complex {
        @Parameter public int a;
        @Parameter public int b;

        @Parameter public String getText() {return "foo";}

        public int c = 3;
    }

    @Test
    public void testParameter() {
        final Algo myAlgo = new Algo();
        myAlgo.alpha = 5.3;
        myAlgo.beta = 2;
        myAlgo.gamma = new Complex();
        myAlgo.gamma.a = 12;
        myAlgo.gamma.b = 8;

        System.out.println(new ParameterFormatter().format(new JDKDocument(), myAlgo));

    }

}
