package de.unijena.bioinf.cmlDesign;

@FunctionalInterface
public interface CMLEvaluator {

    double evaluate(int[][] bbMasses);

}