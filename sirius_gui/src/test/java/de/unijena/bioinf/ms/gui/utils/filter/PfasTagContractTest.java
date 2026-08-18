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

import de.unijena.bioinf.ms.gui.utils.filter.PfasFilter.PfasEvidence;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The GUI mirrors the pfas tag name and its values as literals ({@link PfasFilter}) because it must keep
 * working against a REMOTE middleware and therefore cannot depend on the server-side tag definitions.
 * <p>
 * This test is the safety net for that copy: it is the ONLY place where both sides meet, and it exists
 * so a change to the tag definitions fails the build here instead of silently turning the PFAS filter
 * into a query that matches nothing. It is a test-only dependency - nothing shipped in the GUI links
 * against {@link TagDefinitions}.
 */
public class PfasTagContractTest {

    @Test
    public void testGuiLiteralsMatchTheTagDefinition() {
        assertEquals(TagDefinitions.PFAS_TYPE.getTagName(), PfasFilter.TAG_NAME);
        assertEquals("tags." + TagDefinitions.PFAS_TYPE.getTagName(), FeatureFilterModel.FIELD_PFAS);

        assertEquals(TagDefinitions.PFAS_TYPE_0, PfasEvidence.POTENTIAL.getTagValue());
        assertEquals(TagDefinitions.PFAS_TYPE_1, PfasEvidence.MOLECULAR_FORMULA.getTagValue());
        assertEquals(TagDefinitions.PFAS_TYPE_2, PfasEvidence.MOLECULAR_STRUCTURE.getTagValue());
    }

    @Test
    public void testTheScaleCoversEveryDefinedTagValueInOrder() {
        // a value added to the definition without a matching evidence level would be unfilterable
        // the definition keeps its values in an ordered set, the scale in ordinal order
        assertEquals(List.copyOf(TagDefinitions.PFAS_TYPE.getValueDefinition().getPossibleValues()),
                List.of(PfasEvidence.POTENTIAL.getTagValue(), PfasEvidence.MOLECULAR_FORMULA.getTagValue(),
                        PfasEvidence.MOLECULAR_STRUCTURE.getTagValue()));
    }
}
