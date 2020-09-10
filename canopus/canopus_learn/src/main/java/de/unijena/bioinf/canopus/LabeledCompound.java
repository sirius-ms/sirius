package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import gnu.trove.map.hash.TObjectIntHashMap;

class LabeledCompound {
    protected final String inchiKey;
    protected final MolecularFormula formula;
    protected final ArrayFingerprint fingerprint;
    protected final ArrayFingerprint label;

    protected ArrayFingerprint npcLabel;

    protected final double[] formulaFeatures;

    protected final ArrayFingerprint learnableFp;
    public float[] formulaFeaturesF;

    // cached tanimoto to training data

    protected short[] closestTrainingPoints;

    LabeledCompound(String inchiKey, MolecularFormula formula, ArrayFingerprint fingerprint, ArrayFingerprint label, double[] formulaFeatures, ArrayFingerprint learnableFp) {
        this.inchiKey = inchiKey;
        this.formula = formula;
        this.fingerprint = fingerprint;
        this.label = label;
        if (formulaFeatures==null)
            throw new NullPointerException();
        this.formulaFeatures = formulaFeatures;
        this.learnableFp = learnableFp;
    }

    private static boolean isValidClassyfireFingerprint(String name, ArrayFingerprint cf) {
        final TObjectIntHashMap<ClassyfireProperty> props = new TObjectIntHashMap<ClassyfireProperty>(2000,0.75f,-2);
        for (FPIter iter : cf)  {
            ClassyfireProperty p = (ClassyfireProperty)iter.getMolecularProperty();
            props.put(p,iter.getIndex());
        }
        for (FPIter iter : cf.presentFingerprints())  {
            ClassyfireProperty p = (ClassyfireProperty)iter.getMolecularProperty();
            ClassyfireProperty q = p.getParent();
            while (q != null) {
                if (props.containsKey(q.getChemOntId()) && !cf.isSet(props.get(q))) {
                    System.err.println("Invalid property for " + name + ", " + p.getName() + " is set to 1 but " + q.getName() + " is set to 0.\n" + cf.toCommaSeparatedString());
                    return false;
                }
                q = q.getParent();
            }
        }
        return true;
    }
}
