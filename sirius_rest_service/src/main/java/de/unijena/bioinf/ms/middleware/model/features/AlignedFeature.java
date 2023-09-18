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

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.features;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ms.middleware.model.features.annotations.Annotations;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * The AlignedFeature contains the ID of a featured (aligned over runs) together with some read-only information
 * that might be displayed in some summary view.
 */
@Getter
@Setter
public class AlignedFeature {
    public enum OptFields {topAnnotations, msData, qualityFlags, lcmsFeatureQuality}

    // identifier
    @NotNull
    protected String alignFeatureId;

    // identifier source //todo obsolete?
    protected String name;
    protected long index;

    // additional attributes
    protected Double ionMass;
    protected String ionType;

    protected Double rtStartSeconds;
    protected Double rtEndSeconds;

    //Summary of the results of the compounds
    protected Annotations topAnnotations;
    protected MsData msData;

    // Data and Result quality

    //todo revisit the quality stuff for the api v2
    /**
     * Contains all pre-computation quality information that belong to
     * this feature (aligned over runs), such as information about the quality of the peak shape, MS2 spectrum etc.,
     * see ({@link CompoundQuality.CompoundQualityFlag})
     * <p>
     * Each Feature has a Set of Quality assessment flags.
     */
    protected EnumSet<CompoundQuality.CompoundQualityFlag> qualityFlags;
    //todo we will add an additional MSQuality Object with many different quality checks produced by LCMS Compound// Summary.

    /**
     * LCMS feature-based quality information as also provided in the LCMS-view in the GUI
     */
    protected LCMSFeatureQuality lcmsFeatureQuality;

    //todo handle computing flag
    protected boolean computing = false;

    public static AlignedFeature of(CompoundContainerId cid) {
        final AlignedFeature id = new AlignedFeature();
        id.setAlignFeatureId(cid.getDirectoryName());
        id.setName(cid.getCompoundName());
        id.setIndex(cid.getCompoundIndex());
        id.setIonMass(cid.getIonMass().orElse(0d));
        cid.getIonType().map(PrecursorIonType::toString).ifPresent(id::setIonType);
        cid.getRt().ifPresent(rt -> {
            if (rt.isInterval()) {
                id.setRtStartSeconds(rt.getStartTime());
                id.setRtEndSeconds(rt.getEndTime());
            }else {
                id.setRtStartSeconds(rt.getRetentionTimeInSeconds());
                id.setRtEndSeconds(rt.getRetentionTimeInSeconds());
            }
        });
        return id;
    }
}
