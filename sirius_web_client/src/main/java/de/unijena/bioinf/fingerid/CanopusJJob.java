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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.canopus.CanopusResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CanopusJJob extends FingerprintDependentJJob<CanopusResult> {

    private final Canopus canopus;

    public CanopusJJob(@NotNull Canopus canopus) {
        this(canopus, null, null);
    }

    public CanopusJJob(@NotNull Canopus canopus, @Nullable ProbabilityFingerprint fp, @Nullable MolecularFormula formula) {
        super(JobType.CPU, fp, formula, null); //todo what do we need here??
        this.canopus = canopus;
    }


    @Override
    protected CanopusResult compute() throws Exception {
        progressInfo("Predict compound categories for " + formula + ": \nid\tname\tprobability");
        final ProbabilityFingerprint fingerprint = canopus.predictClassificationFingerprint(formula, fp);
        for (FPIter category : fingerprint.iterator()) {
            if (category.getProbability() >= 0.333) {
                ClassyfireProperty prop = ((ClassyfireProperty) category.getMolecularProperty());
                progressInfo(prop.getChemontIdentifier() + "\t" + prop.getName() + "\t" + ((int) Math.round(100d * category.getProbability())) + " %");
            }
        }

        return new CanopusResult(fingerprint);
    }
}