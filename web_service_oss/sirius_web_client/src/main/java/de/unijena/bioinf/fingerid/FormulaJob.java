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
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.rest.NetUtils;

import java.util.List;

/**
 * retrieves {@link FingerprintCandidate}s for a given {@link MolecularFormula}
 */
public class FormulaJob extends BasicJJob<WebWithCustomDatabase.CandidateResult> {

    protected final MolecularFormula formula;
    protected final WebWithCustomDatabase searchDatabase;
    protected final List<CustomDataSources.Source> dbs;
    protected final PrecursorIonType ionType;
    protected final boolean includeRestAllDb;
    protected final long fakeFilterBits;


    public FormulaJob(MolecularFormula formula, WebWithCustomDatabase searchDatabase, List<CustomDataSources.Source> dbs, PrecursorIonType precursorIonType, boolean includeRestAllDb) {
        this(formula, searchDatabase, dbs, precursorIonType, includeRestAllDb, 0);
    }

    public FormulaJob(MolecularFormula formula, WebWithCustomDatabase searchDatabase, List<CustomDataSources.Source> dbs, PrecursorIonType precursorIonType, boolean includeRestAllDb, long fakeFilterBits) {
        super(JobType.WEBSERVICE);
        this.formula = formula;
        this.searchDatabase = searchDatabase;
        this.dbs = dbs;
        this.ionType = precursorIonType;
        this.includeRestAllDb = includeRestAllDb;
        this.fakeFilterBits = fakeFilterBits;
    }

    @Override
    protected WebWithCustomDatabase.CandidateResult compute() throws Exception {
        return NetUtils.tryAndWait(() -> {
            final WebWithCustomDatabase.CandidateResult result = searchDatabase.loadCompoundsByFormula(formula, dbs, includeRestAllDb);
            result.addToRequestFilter(fakeFilterBits);
            return result;
        }, this::checkForInterruption);
    }
}
