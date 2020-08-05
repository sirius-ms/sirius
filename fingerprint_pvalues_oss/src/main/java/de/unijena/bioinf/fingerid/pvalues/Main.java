

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

package de.unijena.bioinf.fingerid.pvalues;

import de.unijena.bioinf.graphUtils.tree.Tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class Main {

    public static void main(String[] args) {
        // arg1 is file with fingerprints
        // arg2 is tree file
        // arg3 is a file with two fingerprints, one POS and one NEG
        try {
            final FingerprintTree tree = new DotParser().parseFromFile(new File(args[1]));
            new ConditionalProbabilitiesEstimator(tree).estimate(readFingerprints(new File(args[0])));
            final Iterator<boolean[]> fps = readFingerprints(new File(args[2]));
            final boolean[] real = fps.next().clone();
            final boolean[] predicted = fps.next();
            int distance = 0;
            for (Tree<FPVariable> var : tree.nodes) {
                final int k = var.getLabel().to;
                if (real[k]!=predicted[k]) ++distance;
            }
            System.out.println(distance);
            System.out.println(new TreeDP(tree).computeUnitScores(predicted, distance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Iterator<boolean[]> readFingerprints(File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String firstLine = reader.readLine();
        int column=0;
        int fplen=0;
        for (int i=0; i < firstLine.length(); ++i) {
            // check if current column starts with a lot of ones or zeros
            boolean isfp = false;
            final int n =firstLine.length()-i;
            for (fplen=0; fplen < n; ++fplen) {
                final char c = firstLine.charAt(i+fplen);
                if (c != '1' && c != '0') break;
            }
            if (fplen >= 50) {
                break;
            }
            if (firstLine.charAt(i)=='\t') ++column;
        }

        final int fpcol = column;
        final int fplength = fplen;
        final boolean[] fp = new boolean[fplength];
        return new Iterator<boolean[]>() {
            private String currentLine = firstLine;
            @Override
            public boolean hasNext() {
                return currentLine!=null;
            }

            @Override
            public boolean[] next() {
                int i=0;
                if (fpcol>0) {
                    int c=fpcol;
                    for (i=0; i < currentLine.length(); ++i) {
                        if (currentLine.charAt(i)=='\t') {
                            if (--c <= 0) {
                                ++i;
                                break;
                            }
                        }
                    }
                }
                for (int k=0; k < fplength; ++k) {
                    fp[k] = currentLine.charAt(i+k)=='1';
                }
                try {
                    currentLine = reader.readLine();
                    if (currentLine==null) reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return fp;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
