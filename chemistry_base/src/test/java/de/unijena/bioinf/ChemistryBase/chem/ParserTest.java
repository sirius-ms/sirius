/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ChemistryBase.chem;

import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistributionBlueObeliskReader;
import de.unijena.bioinf.ChemistryBase.chem.utils.PeriodicTableBlueObeliskReader;
import de.unijena.bioinf.ChemistryBase.chem.utils.PeriodicTableJSONReader;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ParserTest {

    @Test
    public void testBlueObelisk() {
        PeriodicTable instance = PeriodicTable.push();
        try {
            new PeriodicTableBlueObeliskReader().readFromClasspath(instance);
            assertNotNull(instance.getByName("C"));
            assertEquals("Carbon", instance.getByName("C").getName());
            assertEquals(4, instance.getByName("C").getValence());
            assertEquals(3, instance.getByName("N").getValence());
            assertEquals(2, instance.getByName("O").getValence());
            assertEquals(1, instance.getByName("H").getValence());
            assertEquals(3, instance.getByName("P").getValence());
            assertEquals(2, instance.getByName("S").getValence());
            assertEquals(1.00782503214, instance.getByName("H").getMass(), 1e-6);
            assertEquals(31.9720706912, instance.getByName("S").getMass(), 1e-6);
            assertEquals(12, instance.getByName("C").getMass(), 1e-12);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PeriodicTable.pop();
    }

    @Test
    public void correctlyInitialized() {
        assertNotNull("periodic table should know additional elements like D", PeriodicTable.getInstance().getByName("D"));
        assertNotNull("periodic table should know standard elements like Cl", PeriodicTable.getInstance().getByName("Cl"));
    }

    @Test
    public void testTableStack() {
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
        PeriodicTable.pushCopy();
        PeriodicTable.getInstance().addElement("Quark mit Soße", "Quark", 777, 4);
        assertNotNull(PeriodicTable.getInstance().getByName("Quark"));
        assertEquals(13, MolecularFormula.parseOrThrow("Quark13").numberOf(PeriodicTable.getInstance().getByName("Quark")));
        assertNotNull(PeriodicTable.getInstance().getByName("C"));
        PeriodicTable.pop();
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
    }

    @Test
    public void testTableNewStack() {
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
        PeriodicTable.push();
        PeriodicTable.getInstance().addElement("Quark mit Soße", "Quark", 777, 4);
        assertNotNull(PeriodicTable.getInstance().getByName("Quark"));
        assertNull(PeriodicTable.getInstance().getByName("C"));
        assertEquals(13, MolecularFormula.parseOrThrow("Quark13").numberOf(PeriodicTable.getInstance().getByName("Quark")));
        PeriodicTable.pop();
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
    }

    @Test
    public void testThreadLocalStack() {
        final boolean[] valid = new boolean[]{true, true};
        PeriodicTable.setThreadLocal(true);
        final Thread a = new Thread(new Runnable() {
            @Override
            public void run() {
                PeriodicTable.push();
                PeriodicTable.getInstance().addElement("Quark mit Soße", "Quark", 777, 4);
                for (int i=0; i < 10; ++i) {
                    valid[0] &= PeriodicTable.getInstance().getByName("Quark") != null;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        assert false;
                    }
                }
                PeriodicTable.pop();
            }
        });
        a.start();
        final Thread b = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i=0; i < 10; ++i) {
                    valid[1] &= PeriodicTable.getInstance().getByName("Quark") == null;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        assert false;
                    }
                }
            }
        });
        b.start();
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        assertTrue(valid[0]);
        assertTrue(valid[1]);
        PeriodicTable.setThreadLocal(false);
        assertNull(PeriodicTable.getInstance().getByName("Quark"));
    }

    @Test
    public void testBlueObeliskIsotopes() {
        PeriodicTable instance = PeriodicTable.getInstance();
        try {
            instance.setDistribution(new IsotopicDistributionBlueObeliskReader().getFromClasspath());
        } catch (IOException e) {
            assert false;
        }
        IsotopicDistribution dist = instance.getDistribution();
        assertNotNull(dist.getIsotopesFor("C"));
        assertEquals(2, dist.getIsotopesFor("C").getNumberOfIsotopes());
    }

    @Test
    public void testLegacyParser() throws IOException {
        new PeriodicTableJSONReader(false).readFromClasspath(PeriodicTable.getInstance());
    }

}
