/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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
