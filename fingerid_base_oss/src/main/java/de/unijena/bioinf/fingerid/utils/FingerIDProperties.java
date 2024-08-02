/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.utils;


import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All version numbers are directly taken from the build. So there is no more redundant data. Note that version a.b.c-SNAPSHOT lower than a.b.c
 */
public class FingerIDProperties {

    public static String fingeridFullVersion() {
        return PropertyManager.getProperty("de.unijena.bioinf.fingerid.version");
    }

    public static String fingeridMinorVersion() {
        return toMinorVersion(fingeridFullVersion());
    }
    public static String toMinorVersion(@NotNull String version) {
        String[] splits = version.split("[.]");
        return splits[0] + "." + splits[1];
    }

    public static String fingeridMajorVersion() {
        return toMajorVersion(fingeridFullVersion());
    }
    public static String toMajorVersion(@NotNull String version) {
        return version.split("[.]")[0];
    }

    public static String siriusFallbackWebHostContextPath() {
        return "v" + FingerIDProperties.fingeridMinorVersion();
    }

    public static String siriusFallbackWebHost() {
        return PropertyManager.getProperty("de.unijena.bioinf.fingerid.web.host",null ,"http://localhost:8080");
    }

    public static String siriusVersion() {
        return PropertyManager.getProperty("de.unijena.bioinf.sirius.version");
    }

    public static String sirius_guiVersion() {
        return  PropertyManager.getProperty("de.unijena.bioinf.siriusFrontend.version");
    }

    public static String gcsChemDBFlavor() {
        return PropertyManager.getProperty("de.unijena.bioinf.chemdb.gcs.flavor", null, "default");
    }

    @Nullable
    public static Integer gcsChemDBFpId() {
        return PropertyManager.getInteger("de.unijena.bioinf.chemdb.fingerprint.id", null, null);
    }

    public static String defaultChemDBBucket(){
        return PropertyManager.getProperty("de.unijena.bioinf.stores.chemdb.bucket");
    }
    public static String chemDBStorePropertyPrefix(){
        return "de.unijena.bioinf.stores.chemdb";
    }

    public static String customDBStorePropertyPrefix(){
        return "de.unijena.bioinf.stores.customdb";
    }

}
