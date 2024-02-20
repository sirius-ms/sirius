/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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

package de.unijena.bioinf.ms.persistence.model.core.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class InstrumentConfigTest {

    @Test
    public void testICGetters() {
        Assert.assertTrue(Fragmentation.byHupoId("MS:1000133").filter(InstrumentConfigs.fragmentMap.get("MS:1000133")::equals).isPresent());
        Assert.assertTrue(Fragmentation.byValue("collision-induced").filter(InstrumentConfigs.fragmentMap.get("MS:1000133")::equals).isPresent());
        Assert.assertTrue(Fragmentation.byValue("LIFT").filter(InstrumentConfigs.fragmentMap.get("MS:1002000")::equals).isPresent());

        Assert.assertTrue(MassAnalyzer.byValue("fticr").filter(InstrumentConfigs.analyzerMap.get("MS:1000079")::equals).isPresent());
        Assert.assertTrue(MassAnalyzer.byValue("ft_icr").filter(InstrumentConfigs.analyzerMap.get("MS:1000079")::equals).isPresent());

        Assert.assertTrue(Ionization.byValue("penning ionization").filter(InstrumentConfigs.ionizationMap.get("MS:1000399")::equals).isPresent());
        Assert.assertTrue(Ionization.byValue("chemical").filter(InstrumentConfigs.ionizationMap.get("MS:1000071")::equals).isPresent());
        Assert.assertTrue(Ionization.byValue("chemi").filter(InstrumentConfigs.ionizationMap.get("MS:1000386")::equals).isPresent());

        Assert.assertFalse(MassAnalyzer.byHupoId("MS:1000133").isPresent());
    }

    @Test
    public void testICSerialization() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Fragmentation ic = Fragmentation.byHupoId("MS:1000133").orElseThrow();
        String json = mapper.writeValueAsString(ic);

        Assert.assertEquals("\"MS:1000133\"", json);
        Assert.assertEquals(ic, mapper.readValue(json, Fragmentation.class));
        Assert.assertNull(mapper.readValue(json, MassAnalyzer.class));
    }

}
