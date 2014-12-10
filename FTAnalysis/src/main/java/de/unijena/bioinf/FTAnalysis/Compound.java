package de.unijena.bioinf.FTAnalysis;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

class Compound {

    private static PredictedLoss[] EMPTY_ARRAY = new PredictedLoss[0];

    static Iterable<PredictedLoss> foreachLoss(final List<Compound> compounds) {
        return new Iterable<PredictedLoss>() {
            @Override
            public Iterator<PredictedLoss> iterator() {
                return Iterables.concat(Iterables.transform(compounds, new Function<Compound, Iterable<PredictedLoss>>() {
                    @Override
                    public Iterable<PredictedLoss> apply(Compound arg) {
                        return Arrays.asList(arg.losses);
                    }
                })).iterator();
            }
        };
    }

    MolecularFormula formula;
    File file;
    PredictedLoss[] losses;

    public Compound(MolecularFormula formula, File file) {
        this.formula = formula;
        this.file = file;
        this.losses = EMPTY_ARRAY;
    }
}
