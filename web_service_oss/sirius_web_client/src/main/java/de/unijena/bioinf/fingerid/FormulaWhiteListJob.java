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
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.rest.NetUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * retrieves a {@link Whiteset} of {@link MolecularFormula}s based on the given {@link SearchableDatabase}
 */
public class FormulaWhiteListJob extends BasicJJob<CandidateFormulas> {
    private final List<CustomDataSources.Source> dbToSearch;
    private final WebWithCustomDatabase searchDB;

    //experiment parameter
    private final double precursorMass;
    private final Deviation massDev;
    private final PrecursorIonType[] allowedIons;

    private FormulaWhiteListJob(WebWithCustomDatabase searchDB, List<CustomDataSources.Source> dbToSearch, double precursorMass, Deviation massDev, PrecursorIonType[] allowedIonTypes) {
        super(JobType.WEBSERVICE);
        this.massDev = massDev;
        this.dbToSearch = dbToSearch;
        this.precursorMass = precursorMass;
        this.allowedIons = allowedIonTypes;
        this.searchDB = searchDB;
    }

    public static FormulaWhiteListJob create(WebWithCustomDatabase searchDB, List<CustomDataSources.Source> dbToSearch, Ms2Experiment experiment) {
        final double precursorMass = experiment.getIonMass();
        final Deviation massDev = getMassDeviation(experiment);
        return new FormulaWhiteListJob(searchDB, dbToSearch, precursorMass, massDev, experiment.getPossibleAdductsOrFallback().getAdducts().toArray(PrecursorIonType[]::new));
    }

    /**
     * returns the larger of MS1 and MS2 mass dev. This should be on the safe side for the downstream workflow.
     */
    private static Deviation getMassDeviation(Ms2Experiment experiment) {
        MS1MassDeviation ms1Dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class);
        MS2MassDeviation ms2Dev = experiment.getAnnotationOrDefault(MS2MassDeviation.class);

        return new Deviation(
                Math.max(ms1Dev.allowedMassDeviation.getPpm(), ms2Dev.allowedMassDeviation.getPpm()),
                Math.max(ms1Dev.allowedMassDeviation.getAbsolute(), ms2Dev.allowedMassDeviation.getAbsolute()));
    }

    @Override
    protected CandidateFormulas compute() throws Exception {
        final Set<MolecularFormula> formulas = NetUtils.tryAndWait(() ->
                searchDB.loadMolecularFormulas(precursorMass, massDev, allowedIons, dbToSearch) //todo ElementFilter: I am not sure that every Database implementation only retrieves MFs that respect the adduct (and not just the mass)
                .stream().map(FormulaCandidate::getFormula).collect(Collectors.toSet()), this::checkForInterruption);
        return CandidateFormulas.fromSet(formulas, FormulaWhiteListJob.class);
    }

}
