package de.unijena.bioinf.ChemistryBase.utils;

import org.junit.jupiter.api.Test;

import static de.unijena.bioinf.ChemistryBase.utils.FileUtils.sizeToReadableString;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

    @Test
    void sizeToReadableStringTest() {
        assertEquals("0 B", sizeToReadableString(0));
        assertEquals("1 B", sizeToReadableString(1));
        assertEquals("999 B", sizeToReadableString(999));
        assertEquals("1 kB", sizeToReadableString(1000));
        assertEquals("1 kB", sizeToReadableString(1049));
        assertEquals("1.1 kB", sizeToReadableString(1050));
        assertEquals("1.2 kB", sizeToReadableString(1234));
        assertEquals("1.9 GB", sizeToReadableString(1900000000));
    }
}