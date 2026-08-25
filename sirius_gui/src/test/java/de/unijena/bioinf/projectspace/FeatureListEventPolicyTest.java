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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.projectspace.FeatureListEventPolicy.Action;
import io.sirius.ms.sdk.model.ProjectEventType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureListEventPolicyTest {

    private static final Set<String> PRESENT = Set.of("f1", "f2", "f3");

    @Test
    void deleteOfDisplayedFeatureReloads() {
        assertEquals(Action.RELOAD, FeatureListEventPolicy.decideStructural(ProjectEventType.FEATURE_DELETED, "f2", PRESENT));
    }

    @Test
    void deleteOfNonDisplayedFeatureIsIgnored() {
        // The common external / bulk-delete case: deleting features that aren't shown cannot change the list.
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(ProjectEventType.FEATURE_DELETED, "absent", PRESENT));
    }

    @Test
    void createAlwaysReloadsEvenForAbsentId() {
        // A new feature is by definition not in the present set but may match the current filter.
        assertEquals(Action.RELOAD, FeatureListEventPolicy.decideStructural(ProjectEventType.FEATURE_CREATED, "brandNew", PRESENT));
    }

    @Test
    void updateAlwaysReloads() {
        // An update can make a currently-absent feature start (or stop) matching the filter.
        assertEquals(Action.RELOAD, FeatureListEventPolicy.decideStructural(ProjectEventType.FEATURE_UPDATED, "absent", PRESENT));
    }

    @Test
    void resultAndLifecycleEventsAreIgnored() {
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(ProjectEventType.RESULT_CREATED, "f1", PRESENT));
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(ProjectEventType.RESULT_UPDATED, "f1", PRESENT));
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(ProjectEventType.RESULT_DELETED, "f1", PRESENT));
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(ProjectEventType.PROJECT_CLOSED, "f1", PRESENT));
    }

    @Test
    void nullTypeIsIgnored() {
        assertEquals(Action.IGNORE, FeatureListEventPolicy.decideStructural(null, "f1", PRESENT));
    }

    @Test
    void deleteWithNullIdReloadsConservatively() {
        // An unidentifiable delete cannot be proven irrelevant, so we reload rather than risk a stale list.
        assertEquals(Action.RELOAD, FeatureListEventPolicy.decideStructural(ProjectEventType.FEATURE_DELETED, null, PRESENT));
    }
}
