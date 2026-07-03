/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.model.core.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class FoldChangeAndTagGroupCompatibilityTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testFoldChangeOldDeserialization() throws IOException {
        // Construct JSON representing an old FoldChange record (no left/right abundances, but has foldChange)
        String oldFoldChangeJson = "{" +
                "\"@class\":\"de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange$CompoundFoldChange\"," +
                "\"foldChange\":4.5," +
                "\"compoundId\":123" +
                "}";

        FoldChange.CompoundFoldChange fc = mapper.readValue(oldFoldChangeJson, FoldChange.CompoundFoldChange.class);

        // Verify deserialized state
        Assert.assertEquals(123, fc.getCompoundId());
        Assert.assertEquals(0.0, fc.getLeftAbundance(), 0.0001);
        Assert.assertEquals(0.0, fc.getRightAbundance(), 0.0001);

        // Crucial check: getFoldChange() must return the old foldChange value, not 1.0 or 0.0
        Assert.assertEquals(4.5, fc.getFoldChange(), 0.0001);
    }

    @Test
    public void testFoldChangeNewSerializationAndForwardCompatibility() throws IOException {
        // Construct a new FoldChange record with left and right abundance
        FoldChange.CompoundFoldChange fc = FoldChange.CompoundFoldChange.builder()
                .compoundId(456)
                .leftAbundance(15.0)
                .rightAbundance(3.0)
                .build();

        // Verify initial calculated state
        Assert.assertEquals(5.0, fc.getFoldChange(), 0.0001);

        // Serialize to JSON
        String json = mapper.writeValueAsString(fc);

        // Verify that the serialized JSON contains both abundances AND the foldChange field
        // so that older software can parse the foldChange value successfully.
        Assert.assertTrue(json.contains("\"leftAbundance\":15.0"));
        Assert.assertTrue(json.contains("\"rightAbundance\":3.0"));
        Assert.assertTrue(json.contains("\"foldChange\":5.0"));

        // Deserialize back and verify
        FoldChange.CompoundFoldChange deserialized = mapper.readValue(json, FoldChange.CompoundFoldChange.class);
        Assert.assertEquals(456, deserialized.getCompoundId());
        Assert.assertEquals(15.0, deserialized.getLeftAbundance(), 0.0001);
        Assert.assertEquals(3.0, deserialized.getRightAbundance(), 0.0001);
        Assert.assertEquals(5.0, deserialized.getFoldChange(), 0.0001);
    }

    @Test
    public void testTagGroupOldDeserialization() throws IOException {
        // Old TagGroup JSON without 'editable' field
        String oldTagGroupJson = "{" +
                "\"groupName\":\"OldCustomGroup\"," +
                "\"luceneQuery\":\"tags.sample:sample\"," +
                "\"groupType\":\"CUSTOM\"" +
                "}";

        TagGroup group = mapper.readValue(oldTagGroupJson, TagGroup.class);

        // Verify that 'editable' defaults to true, enabling editing of legacy groups
        Assert.assertTrue(group.isEditable());
        Assert.assertTrue(group.getEditable());
    }

    @Test
    public void testTagGroupNewSerialization() throws IOException {
        // Predefined group that should be immutable (editable = false)
        TagGroup predefinedGroup = TagGroup.builder()
                .groupName("PredefinedGroup")
                .luceneQuery("tags.type:blank")
                .groupType("PREDEFINED")
                .editable(false)
                .build();

        Assert.assertFalse(predefinedGroup.isEditable());
        Assert.assertFalse(predefinedGroup.getEditable());

        String json = mapper.writeValueAsString(predefinedGroup);

        // Verify serialized JSON contains "editable":false
        Assert.assertTrue(json.contains("\"editable\":false"));

        // Deserialize and check
        TagGroup deserialized = mapper.readValue(json, TagGroup.class);
        Assert.assertFalse(deserialized.isEditable());
        Assert.assertFalse(deserialized.getEditable());
    }
}
