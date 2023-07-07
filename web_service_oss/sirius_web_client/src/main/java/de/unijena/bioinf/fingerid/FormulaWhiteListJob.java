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

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.rest.NetUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * retrieves a {@link Whiteset} of {@link MolecularFormula}s based on the given {@link SearchableDatabase}
 */
public class FormulaWhiteListJob extends BasicJJob<Whiteset> {
    private final List<SearchableDatabase> searchableDatabases;
    private final WebWithCustomDatabase searchDB;
    //job parameter
    private final boolean onlyOrganic;
    private final boolean annotate;

    //experiment parameter
    private final Ms2Experiment experiment;
    private final Deviation massDev;

    public FormulaWhiteListJob(WebWithCustomDatabase searchDB, List<SearchableDatabase> searchableDatabases, Ms2Experiment experiment, boolean onlyOrganic, boolean annotateResult) {
        this(searchDB, searchableDatabases, experiment, experiment.getAnnotationOrThrow(MS2MassDeviation.class).allowedMassDeviation, onlyOrganic, annotateResult);
    }

    public FormulaWhiteListJob(WebWithCustomDatabase searchDB, List<SearchableDatabase> searchableDatabases, Ms2Experiment experiment, Deviation massDev, boolean onlyOrganic, boolean annotateResult) {
        super(JobType.WEBSERVICE);
        this.massDev = massDev;
        this.searchableDatabases = searchableDatabases;
        this.experiment = experiment;
        this.annotate = annotateResult;
        this.searchDB = searchDB;
        this.onlyOrganic = onlyOrganic;
    }

    @Override
    protected Whiteset compute() throws Exception {
        PrecursorIonType ionType = experiment.getPrecursorIonType();
        PrecursorIonType[] allowedIons;
        if (ionType.isIonizationUnknown()) {
            allowedIons = Iterables.toArray(PeriodicTable.getInstance().getIonizations(ionType.getCharge()), PrecursorIonType.class);
        } else {
            allowedIons = new PrecursorIonType[]{ionType};
        }


        final Set<MolecularFormula> formulas = NetUtils.tryAndWait(() ->
                searchDB.loadMolecularFormulas(experiment.getIonMass(), massDev, allowedIons, searchableDatabases)
                .stream().map(FormulaCandidate::getFormula).filter(f -> !onlyOrganic || f.isCHNOPSBBrClFI())
                .collect(Collectors.toSet()), this::checkForInterruption);

        final Whiteset whiteset = Whiteset.ofNeutralizedFormulas(formulas);
        if (annotate)
            experiment.setAnnotation(Whiteset.class, whiteset);

        return whiteset;
    }
}
