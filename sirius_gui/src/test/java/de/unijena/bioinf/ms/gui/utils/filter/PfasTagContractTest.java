/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The GUI mirrors the pfas tag field name as a literal ({@link FeatureFilterModel#FIELD_PFAS}) because it
 * must keep working against a REMOTE middleware and therefore cannot depend on the server-side tag
 * definitions.
 * <p>
 * This test is the safety net for that copy: it is the ONLY place where both sides meet, and it exists
 * so a change to the tag definitions fails the build here instead of silently turning the PFAS filter
 * into a query that matches nothing. It is a test-only dependency - nothing shipped in the GUI links
 * against {@link TagDefinitions}.
 */
public class PfasTagContractTest {

    @Test
    public void testTheQueriedFieldMatchesTheTagDefinition() {
        // the filter asks whether this field is present, so a renamed tag would silently match nothing
        assertEquals("tags." + TagDefinitions.PFAS_TYPE.getTagName(), FeatureFilterModel.FIELD_PFAS);
    }
}
