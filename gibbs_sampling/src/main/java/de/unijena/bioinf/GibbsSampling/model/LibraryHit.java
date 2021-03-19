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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.GibbsSampling.LibraryHitQuality;

public class LibraryHit {
    private final Ms2Experiment queryExperiment;
    private final MolecularFormula molecularFormula;
    private final String structure;
    private final double cosine;
    private final PrecursorIonType ionType;
    private final int sharedPeaks;
    private final LibraryHitQuality quality;
    private final double precursorMz;

    public LibraryHit(Ms2Experiment queryExperiment, MolecularFormula molecularFormula, String structure, PrecursorIonType ionType, double cosine, int sharedPeaks, LibraryHitQuality quality, double libPrecursorMz) {
        this.queryExperiment = queryExperiment;
        this.molecularFormula = molecularFormula;
        this.structure = structure;
        this.ionType = ionType;
        this.cosine = cosine;
        this.sharedPeaks = sharedPeaks;
        this.quality = quality;
        this.precursorMz = libPrecursorMz;
    }

    public Ms2Experiment getQueryExperiment() {
        return this.queryExperiment;
    }

    public MolecularFormula getMolecularFormula() {
        return this.molecularFormula;
    }

    public String getStructure() {
        return this.structure;
    }

    public double getCosine() {
        return this.cosine;
    }

    public PrecursorIonType getIonType() {
        return this.ionType;
    }

    public int getSharedPeaks() {
        return this.sharedPeaks;
    }

    public LibraryHitQuality getQuality() {
        return this.quality;
    }

    public double getPrecursorMz() {
        return precursorMz;
    }
}
