package de.unijena.bioinf.sirius.gui.canopus;

import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.myxo.gui.tree.render.TreeRenderPanel;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class ClassyfireTreePanel extends TreeRenderPanel {

    protected static ClassyfireNode buildTreeFromClassifications(ClassyFireFingerprintVersion version, ProbabilityFingerprint fingerprint, double probabilityThreshold) {
        // first: find all nodes with probability above the threshold
        TObjectDoubleHashMap propertyMap = new TObjectDoubleHashMap<ClassyfireProperty>();
        for (FPIter i : fingerprint) {
            if (i.getProbability() >= probabilityThreshold) {
                // add fingerprint and all of its parents
                ClassyfireProperty prop = (ClassyfireProperty) i.getMolecularProperty();
                do {
                    propertyMap.put(prop, fingerprint.getProbability(version.))
                    prop = prop.getParent();
                } while (prop != null)
            }
        }


    }

    protected static class ClassyfireNode extends DefaultTreeNode {
        ClassyfireProperty underlyingProperty;

        public ClassyfireNode(ClassyfireProperty underlyingProperty) {
            this.underlyingProperty = underlyingProperty;
        }
    }

}
