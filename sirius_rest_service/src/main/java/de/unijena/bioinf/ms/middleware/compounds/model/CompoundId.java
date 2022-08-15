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

package de.unijena.bioinf.ms.middleware.compounds.model;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import lombok.Getter;
import lombok.Setter;

/**
 * The CompoundId contains the ID of a compound together with some read-only information that might be displayed in
 * some summary view.
 */
@Getter
@Setter
public class CompoundId {

    // identifier
    protected String id;

    // identifier source //todo obsolete?
    protected String name;
    protected long index;

    // additional attributes
    protected Double ionMass;
    protected String ionType;

    protected Double rtStartSeconds;
    protected Double rtEndSeconds;

    //Summary of the results of the compounds
    protected CompoundAnnotation topAnnotation;
    protected MsData msData;

    //todo handle computing flag
    protected boolean computing = false;

    public static CompoundId of(CompoundContainerId cid) {
        final CompoundId id = new CompoundId();
        id.setId(cid.getDirectoryName());
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
