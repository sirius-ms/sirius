package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.MolecularPropertyBean;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a Canopus fingerprint (CompoundClasses) with PropertyChangeSupport, to be a GlazedLists compatible list element.
 */
public class ClassyfirePropertyBean extends MolecularPropertyBean<ClassyfireProperty> {
    public ClassyfirePropertyBean(ProbabilityFingerprint underlyingFingerprint, int absoluteIndex, double fscore, int numberOfTrainingExamples) {
        super(underlyingFingerprint, absoluteIndex, fscore, numberOfTrainingExamples);
    }

    public static List<ClassyfirePropertyBean> fromCanopusResult(@NotNull ProbabilityFingerprint canopusFP) {
        List<ClassyfirePropertyBean> list = new ArrayList<>(canopusFP.cardinality());
        canopusFP.forEach(cc -> list.add(new ClassyfirePropertyBean(
                canopusFP,
                cc.getIndex(),
                Double.NaN,
                0
        )));

        return list;
    }

    public static List<ClassyfirePropertyBean> fromCanopusResult(@NotNull CanopusResult res) {
        return fromCanopusResult(res.getCanopusFingerprint());
    }


}
