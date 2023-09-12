/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.persistence.microstream.LazySupplier;
import lombok.*;
import one.microstream.reference.Lazy;

import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature {
    long featureId;

    protected double ionMass;
    protected PrecursorIonType ionType;
    //todo do we need alternative ionTypes?


    /**
     * MS1 spectrum that defines the Apex (max intensity)
     */
    protected Lazy<Scan> apexScan;
    protected Lazy<SimpleSpectrum> isotopePattern; //artificial spectrum -> no scan
    protected Lazy<List<MSMSScan>> msms;

    public Lazy<Stream<Ms2Spectrum<Peak>>> getMsmsSpectra(){ //todo maybe without supplier? but it ensures that we knwot that is is IO
        return LazySupplier.from(getMsms(),
                (msms) -> msms.stream().map(scan -> new MutableMs2Spectrum(scan.getPeaks(), ionMass, scan.getCollisionEnergy(), scan.getMsLevel())));
    }



    /**
     * Traces of this feature (mono-isotopic + isotope peaks)
     * OPTIONAL
     */
    protected Lazy<List<Trace>> traces;

    protected boolean blank;
}
