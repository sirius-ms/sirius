//import org.openjfx.gradle.JavaFXPlatform;
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//@Slf4j
public enum SiriusPlatform {
    WIN_x86_64("win", "win32-x86-64", "win-x86-64", "windows-amd64"),
    WIN_x86_32(null, "win32-x86", null, null),
    WIN_ARM64(null, null, "win-arm64", "windows-aarch64"),
    WIN_ARM32(null, null, null, null),
    LINUX_x86_64("linux", "linux-x86-64", "linux-x86-64", "linux-amd64"),
    LINUX_x86_32(null, "linux-x86", null, null),
    LINUX_ARM64("linux-aarch64", "linux-aarch64", "mac-arm64", "linux-aarch64"),
    LINUX_ARM32(null, "linux-arm", null, null),
    MAC_x86_64("mac", "darwin-x86-64", "mac-x86-64", "macos-amd64"),
    MAC_ARM64("mac-aarch64", "darwin-aarch64", "mac-arm64", "macos-aarch64"),;

    private final String jfxClassifier;
    private final String inchiClassifier;
    private final String jenaClassifier;
    private final String bellClassifier;

    SiriusPlatform(String jfxClassifier, String inchiClassifier, String jenaClassifier, String bellClassifier) {
        this.jfxClassifier = jfxClassifier;
        this.inchiClassifier = inchiClassifier;
        this.jenaClassifier = jenaClassifier;
        this.bellClassifier = bellClassifier;
    }

    public boolean isMac() {
        return this == MAC_x86_64 || this == MAC_ARM64;

    }

    public boolean isLinux() {
        return this == LINUX_x86_64 || this == LINUX_x86_32 || this == LINUX_ARM32 || this == LINUX_ARM64;
    }

    public boolean isWin() {
        return this == WIN_x86_64 || this == WIN_x86_32 || this == WIN_ARM32 || this == WIN_ARM64;
    }

    public Optional<String> jfxClassifier() {
//        if (jfxClassifier == null || jfxClassifier.isBlank())
//            throw new IllegalArgumentException(name() + " is a supported by JavaFX");
        return Optional.ofNullable(jfxClassifier);
    }

    public Optional<String> inchiClassifier() {
//        if (inchiClassifier == null || inchiClassifier.isBlank())
//            throw new IllegalArgumentException(name() + " is a supported by JNAinchi");
        return Optional.ofNullable(inchiClassifier);
    }

    public Optional<String> jenaClassifier() {
//        if (jenaClassifier == null || jenaClassifier.isBlank())
//            throw new IllegalArgumentException(name() + " is a supported by Jena bioinf native libs.");
        return Optional.ofNullable(jenaClassifier);
    }

    public Optional<String> bellsoftClassifier() {
//        if (jenaClassifier == null || jenaClassifier.isBlank())
//            throw new IllegalArgumentException(name() + " is a supported by Jena bioinf native libs.");
        return Optional.ofNullable(bellClassifier);
    }

    public static List<SiriusPlatform> allBut(SiriusPlatform exclude) {
        return Arrays.stream(SiriusPlatform.values()).filter(it -> it != exclude).toList();
    }

    public static SiriusPlatform fromDescriptor(String platformDescriptor) {
        LoggerFactory.getLogger(SiriusPlatform.class).info("Parsing following descriptor: " + platformDescriptor);
        System.out.println("----> Parsing following platform descriptor: " + platformDescriptor);
        platformDescriptor = platformDescriptor.toLowerCase();

        if (platformDescriptor.contains("win")) {
            if (is32Bit(platformDescriptor))
                if (isARM(platformDescriptor))
                    return SiriusPlatform.WIN_ARM32;
                else
                    return SiriusPlatform.WIN_x86_32;
            else if (isARM(platformDescriptor))
                return SiriusPlatform.WIN_ARM64;
            return SiriusPlatform.WIN_x86_64;

        } else if (platformDescriptor.contains("darwin") || platformDescriptor.contains("mac") || platformDescriptor.contains("osx")) {
            if (isARM(platformDescriptor))
                return SiriusPlatform.MAC_ARM64;
            return SiriusPlatform.MAC_x86_64;
        } else {
            if (is32Bit(platformDescriptor))
                if (isARM(platformDescriptor))
                    return SiriusPlatform.LINUX_ARM32;
                else
                    return SiriusPlatform.LINUX_x86_32;
            else if (isARM(platformDescriptor))
                return SiriusPlatform.LINUX_ARM64;
            return SiriusPlatform.LINUX_x86_64;
        }
    }

    private static boolean is32Bit(String descriptor) {
        return descriptor.contains("32") || descriptor.contains("i386");
    }

    private static boolean isARM(String descriptor) {
        return descriptor.contains("arm") || descriptor.contains("aarch");

    }
}