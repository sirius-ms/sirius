package de.unijena.bioinf.ChemistryBase.ms.ft;

import com.google.common.base.Joiner;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecalibrationFunction {

    private double[] polynomialTerms;

    public RecalibrationFunction(double[] polynomialTerms) {
        this.polynomialTerms = polynomialTerms;
    }

    public double[] getPolynomialTerms() {
        return polynomialTerms;
    }

    public void setPolynomialTerms(double[] polynomialTerms) {
        this.polynomialTerms = polynomialTerms;
    }

    public String toString() {
        final List<String> terms = new ArrayList<String>(polynomialTerms.length);
        for (int k=polynomialTerms.length-1; k >= 0; --k) {
            if (polynomialTerms[k]==0) continue;
            terms.add(k==0 ? String.valueOf(polynomialTerms[k]) : (k == 1 ? polynomialTerms[k]+"x" : polynomialTerms[k]+"x^"+k));
        }
        return Joiner.on(" + ").join(terms);
    }

    private static Pattern FMATCH = Pattern.compile("(\\d+\\.\\d+)(x(?:^(\\d+))?)?");
    public static RecalibrationFunction fromString(String s) {
        final Matcher m = FMATCH.matcher(s);
        final TDoubleArrayList list = new TDoubleArrayList();
        while (m.find()) {
            final double constTerm = Double.parseDouble(m.group(1));
            final int degree;
            final String x = m.group(2);
            if (x==null || x.isEmpty()) {
                degree=0;
            } else {
                final String y = m.group(3);
                if (y==null || y.isEmpty()) {
                    degree=1;
                } else degree = Integer.parseInt(y);
            }
            if (degree > 1000) throw new IllegalArgumentException("maximal allowed polynomial is 1000");
            while (list.size() <= degree) list.add(0d);
            list.set(degree, constTerm);
        }
        return new RecalibrationFunction(list.toArray());
    }
}
