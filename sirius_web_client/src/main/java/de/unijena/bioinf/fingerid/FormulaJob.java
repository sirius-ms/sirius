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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.utils.NetUtils;

import java.util.List;

/**
 * retrieves {@link FingerprintCandidate}s for a given {@link MolecularFormula}
 */
public class FormulaJob extends BasicJJob<RestWithCustomDatabase.CandidateResult> {

    protected final MolecularFormula formula;
    protected final RestWithCustomDatabase searchDatabase;
    protected final List<SearchableDatabase> dbs;
    protected final PrecursorIonType ionType;
    protected final boolean includeRestAllDb;


    public FormulaJob(MolecularFormula formula, RestWithCustomDatabase searchDatabase, List<SearchableDatabase> dbs, PrecursorIonType precursorIonType, boolean includeRestAllDb) {
        super(JobType.WEBSERVICE);
        this.formula = formula;
        this.searchDatabase = searchDatabase;
        this.dbs = dbs;
        this.ionType = precursorIonType;
        this.includeRestAllDb = includeRestAllDb;
    }

    @Override
    protected RestWithCustomDatabase.CandidateResult compute() throws Exception {
        return NetUtils.tryAndWait(() -> {
            final RestWithCustomDatabase.CandidateResult result = searchDatabase.loadCompoundsByFormula(formula, dbs, includeRestAllDb);
            /* we no longer store protonated compounds as protonated compounds
            final CompoundCandidateChargeState chargeState = CompoundCandidateChargeState.getFromPrecursorIonType(ionType);
            if (chargeState != CompoundCandidateChargeState.NEUTRAL_CHARGE) {
                final MolecularFormula hydrogen = MolecularFormula.parseOrThrow("H");
                final RestWithCustomDatabase.CandidateResult protonated = searchDatabase.loadCompoundsByFormula(
                        ionType.getCharge() > 0 ? formula.subtract(hydrogen) : formula.add(hydrogen),
                        dbs, includeRestAllDb);

                result.merge(protonated);
            }
             */

            return result;
        }, this::checkForInterruption);
    }
}
