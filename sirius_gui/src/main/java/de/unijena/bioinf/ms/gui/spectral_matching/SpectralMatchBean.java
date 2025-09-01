/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.spectral_matching;

import de.unijena.bioinf.ms.frontend.core.SiriusPCS;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchSubtoolJob;
import io.sirius.ms.sdk.model.BasicSpectrum;
import io.sirius.ms.sdk.model.DBLink;
import io.sirius.ms.sdk.model.SpectralLibraryMatch;
import io.sirius.ms.sdk.model.SpectralLibraryMatchOptField;
import de.unijena.bioinf.projectspace.InstanceBean;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Getter
public class SpectralMatchBean implements SiriusPCS, Comparable<SpectralMatchBean> {

    private final MutableHiddenChangeSupport pcs = new MutableHiddenChangeSupport(this, true);

    private final SpectralLibraryMatch match;

    private String queryName;

    private InstanceBean instance = null;

    /**
     * Corresponds to top molecular structure database hit (of a similar hit based on MCES distance in approximate mode)
     */
    @Getter
    private boolean matchesTopStructureHit;

    public SpectralMatchBean(SpectralLibraryMatch match, InstanceBean instance) {
        this.match = match;
        try {
            if (instance != null) {
                this.instance = instance;
                BasicSpectrum query = match.getQuerySpectrumIndex() < 0
                        ? instance.getMsData().getMergedMs2()
                        : instance.getMsData().getMs2Spectra().get(match.getQuerySpectrumIndex());

                this.queryName = SpectraSearchSubtoolJob.getQueryName(
                        query.getMsLevel(),
                        query.getScanNumber(),
                        query.getCollisionEnergy(),
                        instance.getIonType().getIonization().toString(),
                        match.getQuerySpectrumIndex());
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).error("Error retrieving spectral matching data.", e);
        }
    }

    public synchronized Optional<BasicSpectrum> getReference() {
        BasicSpectrum spec = getMatch().getReferenceSpectrum();
        if (spec != null)
            return Optional.of(spec);
        if (instance == null)
            return Optional.empty();
        SpectralLibraryMatch tmpMatch = instance.withIds((pid, fid) -> instance.getClient().features()
                .getSpectralLibraryMatch(pid, fid, getMatch().getSpecMatchId(), List.of(SpectralLibraryMatchOptField.REFERENCE_SPECTRUM)));
        getMatch().setReferenceSpectrum(tmpMatch.getReferenceSpectrum());
        return Optional.ofNullable(getMatch().getReferenceSpectrum());
    }

    @Override
    public HiddenChangeSupport pcs() {
        return pcs;
    }

    @Override
    public int compareTo(@NotNull SpectralMatchBean o) {
        return Double.compare(o.getMatch().getSimilarity(), match.getSimilarity());
    }

    public DBLink getDBLink() {
        return new DBLink().name(getMatch().getDbName()).id(getMatch().getDbId());
    }

    public int getRank() {
        return Optional.ofNullable(getMatch().getRank()).orElse(0);
    }

    protected void setMatchesTopStructureHit(boolean matchesTopStructureHit) {
        boolean old = this.matchesTopStructureHit;
        this.matchesTopStructureHit = matchesTopStructureHit;
        pcs.firePropertyChange("matchesTopStructureHit", old, this.matchesTopStructureHit);
    }
}
