
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ftalign.analyse;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.dot.FTDotReader;
import de.unijena.bioinf.babelms.json.FTJsonReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Kai Dührkop
 */
public class FTDataElement {

    private final String name;
    private final URL source;
    private final FTree tree;
    private final int maxDegree;
    private final int maxDepth;
    private final int size;

    public FTDataElement(URL source, String name, FTree tree) {
        this.name = name;
        this.source = source;
        this.tree = tree;
        this.maxDegree = tree.getCursor().maxDegree();
        this.maxDepth = tree.getCursor().maxDegree();
        this.size = tree.numberOfVertices();
    }

    public FTDataElement(URL source) throws IOException {
        this(source, extractName(source), parseTree(source));
    }

    public static List<FTDataElement> parseDotFilesFromDirectories(List<File> dirs) throws IOException {
        final ArrayList<FTDataElement> list = new ArrayList<FTDataElement>();
        for (File f : dirs) {
            list.addAll(parseDotFilesFromDirectory(f));
        }
        list.trimToSize();
        Collections.sort(list, new Comparator<FTDataElement>() {
            @Override
            public int compare(FTDataElement o1, FTDataElement o2) {
                return o1.getName().length() == o2.getName().length() ? o1.getName().compareTo(o2.getName())
                        : (o1.getName().length() - o2.getName().length());
            }
        });
        return list;
    }

    public static List<FTDataElement> parseDotFilesFromDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            if (dir.getName().endsWith(".dot") || dir.getName().endsWith(".json")) {
                return Collections.singletonList(new FTDataElement(dir.toURI().toURL()));
            } else {
                return Collections.emptyList();
            }
        }
        final ArrayList<FTDataElement> list = new ArrayList<FTDataElement>();
        for (File f : dir.listFiles()) {
            if (f.isFile() && (f.getName().endsWith(".dot") || f.getName().endsWith(".json"))) {
                list.add(new FTDataElement(f.toURI().toURL()));
            }
        }
        return list;
    }

    private static String extractName(URL source) {
        final String pathname = source.getPath();
        final String filename = pathname.substring(pathname.lastIndexOf('/') + 1);
        return filename.substring(0, filename.lastIndexOf("."));
    }

    private static FTree parseTree(URL source) throws IOException {
        BufferedReader reader = null;
        try {
            reader = FileUtils.ensureBuffering(new InputStreamReader(source.openStream()));
            return new GenericParser<FTree>(source.getPath().endsWith(".json") ? new FTJsonReader() : new FTDotReader()).parse(reader);
        } catch (Throwable e) {
            final IOException newExc = new IOException("Error while parsing '" + source + "' :\n" + e.toString());
            newExc.setStackTrace(e.getStackTrace());
            throw newExc;
        } finally {
            if (reader != null) reader.close();
        }
    }

    public int getMaxDegree() {
        return maxDegree;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public URL getSource() {
        return source;
    }

    public FTree getTree() {
        return tree;
    }

}
