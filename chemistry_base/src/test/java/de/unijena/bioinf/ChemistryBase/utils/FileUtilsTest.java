package de.unijena.bioinf.ChemistryBase.utils;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static de.unijena.bioinf.ChemistryBase.utils.FileUtils.sizeToReadableString;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {
    private static final DecimalFormat f = new DecimalFormat("0.#");

    @Test
    void sizeToReadableStringTest() {
        assertEquals(f.format(0) + " B", sizeToReadableString(0));
        assertEquals(f.format(1) + " B", sizeToReadableString(1));
        assertEquals(f.format(999) + " B", sizeToReadableString(999));
        assertEquals(f.format(1) + " kB", sizeToReadableString(1000));
        assertEquals(f.format(1) + " kB", sizeToReadableString(1049));
        assertEquals(f.format(1.1) + " kB", sizeToReadableString(1050));
        assertEquals(f.format(1.2) + " kB", sizeToReadableString(1234));
        assertEquals(f.format(1.9) + " GB", sizeToReadableString(1900000000));
    }
}