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

package de.unijena.bioinf.ms.persistence.model.core.feature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import jakarta.persistence.Id;
import lombok.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Deprecated
public class Feature {


    /**
     * ID of this feature
     */
    @Id
    private long featureId;
    /**
     * ID of the aligned feature this feature belongs to
     */
    protected Long alignedFeatureId;

    protected double ionMass;
    protected PrecursorIonType ionType;
    protected boolean blank;
    //todo do we need a list of alternative ionTypes? we probably want to reference the ion network?

    /**
     * ID of the apexScan
     */
    protected long apexScanId;
    /**
     * MS1 spectrum that defines the Apex (max intensity)
     * Foreign Field by apexScanId
     */
    @JsonIgnore
    @ToString.Exclude
    protected Scan apexScan;

    public Optional<Scan> getApexScan() {
        return Optional.ofNullable(apexScan);
    }

    protected SimpleSpectrum getApexSpectrum() {
        return getApexScan().map(Scan::getPeaks).orElse(null);
    }

    /**
     * Extracted isotope pattern of this feature
     */
    @ToString.Exclude
    protected SimpleSpectrum isotopePattern; //artificial spectrum -> no scan

    /**
     * MS/MS spectra belonging to this feature, referenced by the feature id
     * Foreign Field by featureId
     */
    @JsonIgnore
    @ToString.Exclude
    protected List<MSMSScan> msms;

    public Optional<List<MSMSScan>> getMsms() {
        return Optional.ofNullable(msms);
    }

    public Stream<MutableMs2Spectrum> getMSMSSpectra(){
        return getMsms().map(l -> l.stream().map(scan ->
                        new MutableMs2Spectrum(scan.getPeaks(), ionMass, scan.getCollisionEnergy(), scan.getMsLevel())))
                .orElse(null);
    }

    /**
     * Traces of this feature (mono-isotopic + isotope peaks)
     * Optional Field
     */
    @ToString.Exclude
    protected List<AbstractTrace> traces;
}
