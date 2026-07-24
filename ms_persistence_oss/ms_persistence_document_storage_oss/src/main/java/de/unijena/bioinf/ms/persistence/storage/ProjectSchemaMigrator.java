/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.storage.db.nosql.Database;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One-time, in-place conversion of older ("pre-index") SIRIUS projects to the current project-data schema.
 * <p>
 * Newer SIRIUS versions rely on fields that older projects do not carry (they were introduced together with the
 * Lucene search index). Since the GUI feature list is served exclusively from the index, and the index is built
 * from these fields, an old project must be upgraded <b>before</b> the index is (re)built or the default filter
 * hides everything until a manual filter reset.
 * <p>
 * The migration is gated by {@link SiriusProjectDocumentDatabase#findProjectSchemaVersion()}: projects already at
 * {@link #CURRENT_SCHEMA_VERSION} are skipped in O(1). For older/absent versions each conversion step still
 * re-checks whether its data is actually missing, so a low version whose data is already present is a near no-op
 * (no {@link MSData} is loaded). This is deliberate: a version mismatch triggers checks, never blind writes.
 */
@Slf4j
public class ProjectSchemaMigrator {

    /**
     * Current project-data schema version. Bump when a new derivable field/backfill is added here; projects with a
     * lower (or absent) {@link SiriusProjectDocumentDatabase#PROJECT_SCHEMA_VERSION_KEY} are migrated on open.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private ProjectSchemaMigrator() {
    }

    public static boolean computeHasMs1(@Nullable MSData msData) {
        return msData != null && msData.getMergedMs1Spectrum() != null;
    }

    public static boolean computeHasMsMs(@Nullable MSData msData) {
        return msData != null && ((msData.getMsnSpectra() != null && !msData.getMsnSpectra().isEmpty())
                || msData.getMergedMSnSpectrum() != null);
    }

    /**
     * Upgrades the given project to {@link #CURRENT_SCHEMA_VERSION} if needed. Safe to call on every open.
     *
     * @return {@code true} if index-affecting feature data was rewritten (i.e. the MS flags were backfilled), so
     * the caller should (re)build the search index from scratch; {@code false} if nothing index-relevant changed.
     */
    public static boolean migrateIfNeeded(@NotNull SiriusProjectDocumentDatabase<? extends Database<?>> project) throws IOException {
        if (project.findProjectSchemaVersion().orElse(0) >= CURRENT_SCHEMA_VERSION)
            return false;

        Database<?> storage = project.getStorage();

        // hasMs1/hasMsMs (feature document): only (re)compute from MSData when the field is genuinely absent.
        // A persisted default (stored 'false') must not trigger a needless MSData pass, so we probe a single
        // representative feature document at the raw-document level rather than trusting the deserialized primitive.
        boolean flagsMissing = !storage.isFieldPresent("hasMsMs", AlignedFeatures.class);

        // project-level detected adducts (drives the GUI adduct filter): backfill the union of the feature-level
        // detected adducts when the project property is absent. The adducts are on the feature document, so this
        // needs no MSData.
        boolean adductsMissing = project.findDetectedAdducts().isEmpty();

        if (flagsMissing || adductsMissing) {
            Set<PrecursorIonType> adductUnion = new HashSet<>();
            List<AlignedFeatures> features = storage.findAllStr(AlignedFeatures.class).toList();
            for (AlignedFeatures feature : features) {
                // Collect the real detected adducts, mirroring what an import records via addToDetectedAdducts.
                // Features without detected adducts contribute nothing (they are exposed with the unknown-adduct
                // fallback, exactly as in a freshly imported project), so an old project ends up consistent with
                // a new one.
                if (adductsMissing && feature.getDetectedAdducts() != null)
                    adductUnion.addAll(feature.getDetectedAdducts().getAllAdducts());

                if (flagsMissing) {
                    project.fetchMsData(feature);
                    MSData msData = feature.getMSData().orElse(null);
                    feature.setHasMs1(computeHasMs1(msData));
                    feature.setHasMsMs(computeHasMsMs(msData));
                    storage.upsert(feature);
                }
            }
            if (adductsMissing && !adductUnion.isEmpty())
                project.addToDetectedAdducts(adductUnion);
        }

        project.upsertProjectSchemaVersion(CURRENT_SCHEMA_VERSION);
        log.info("Migrated project '{}' to schema version {} (backfilled MS flags: {}, detected adducts: {}).",
                storage.location(), CURRENT_SCHEMA_VERSION, flagsMissing, adductsMissing);
        return flagsMissing;
    }
}
