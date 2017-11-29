package de.unijena.bioinf.ChemistryBase.properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PropertyManagerTest {

    @Test
    public void testManager() {
        assertEquals("SUCCESSFUL",
                PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.test"));
    }
}
