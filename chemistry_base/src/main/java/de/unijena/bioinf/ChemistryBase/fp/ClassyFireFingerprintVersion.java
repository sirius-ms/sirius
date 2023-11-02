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

package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import gnu.trove.map.hash.TIntIntHashMap;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

public class ClassyFireFingerprintVersion extends FingerprintVersion {

    protected ClassyfireProperty[] properties;
    protected TIntIntHashMap chemOntIdToIndex;
    protected int root;

    private static final ClassyFireFingerprintVersion DEFAULT;
    static {
        ClassyFireFingerprintVersion f;
        try {
             f = loadClassyfire(new BufferedInputStream(new GZIPInputStream(ClassyFireFingerprintVersion.class.getResourceAsStream("/fingerprints/chemont.csv.gz"))));
        } catch (IOException e) {
            LoggerFactory.getLogger(ClassyFireFingerprintVersion.class).error(e.getMessage(),e);
            f = null;
        }
        DEFAULT = f;
    };
    public static ClassyFireFingerprintVersion getDefault() {
        return DEFAULT;
    }

    public ClassyFireFingerprintVersion(ClassyfireProperty[] classyfireProperties) {
        this.properties = classyfireProperties;
        this.chemOntIdToIndex = new TIntIntHashMap(classyfireProperties.length);
        int root=0;
        for (int k=0; k < classyfireProperties.length; ++k) {
            chemOntIdToIndex.put(classyfireProperties[k].getChemOntId(), k);
            if (classyfireProperties[k].parentId<0) {
                root = k;
            }
        }
        this.root = root;
        updateParents();
        getChemicalEntity().level = 0;
        for (ClassyfireProperty prop : properties) {
            updateLevel(prop);
        }
    }

    private int updateLevel(ClassyfireProperty prop) {
        if (prop.level>=0) return prop.level;
        prop.level = updateLevel(prop.parent)+1;
        return prop.level;
    }

    public ClassyfireProperty getChemicalEntity() {
        return this.properties[root];
    }

    private void updateParents() {
        for (ClassyfireProperty prop : properties) {
            if (prop.parentId>=0) {
                prop.parent = properties[chemOntIdToIndex.get(prop.parentId)];
            }
        }
    }

    public ClassyfireProperty getPropertyWithChemontId(int id) {
        return properties[chemOntIdToIndex.get(id)];
    }

    public int getIndexOfMolecularProperty(ClassyfireProperty property) {
        return chemOntIdToIndex.get(property.getChemOntId());
    }

    public static ClassyFireFingerprintVersion loadClassyfire(File csvFile) throws IOException {
        return loadClassyfire(FileUtils.getIn(csvFile));
    }

    public static ClassyFireFingerprintVersion loadClassyfire(BufferedInputStream stream) throws IOException {
        final TreeMap<Integer, ClassyfireProperty> properties = new TreeMap<>();
        InputStream fr = stream;
        try {
            final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(fr));
            String line;
            while ((line=br.readLine())!=null) {
                String[] tbs = line.split("\t");
                final int id = Integer.parseInt(tbs[1]);
                properties.put(id, new ClassyfireProperty(id, tbs[0], tbs[3], Integer.parseInt(tbs[2]), Integer.parseInt(tbs[4]), Float.parseFloat(tbs[5])));
            }
        } finally {
            if (fr!=null) fr.close();
        }
        for (ClassyfireProperty entry : properties.values()) {
            if (entry.getParentId()>=0) {
                entry.setParent(properties.get(entry.getParentId()));
            }
        }
        return new ClassyFireFingerprintVersion(properties.values().toArray(new ClassyfireProperty[properties.size()]));
    }

    @Override
    public ClassyfireProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        return identical(fingerprintVersion);
    }

    @Override
    public boolean identical(FingerprintVersion fingerprintVersion) {
        return fingerprintVersion instanceof ClassyFireFingerprintVersion && ((ClassyFireFingerprintVersion) fingerprintVersion).properties.length == properties.length;
    }

    public ClassyfireProperty getPrimaryClass(AbstractFingerprint classyfireFingerprint) {
        ClassyfireProperty bestAbove50 = this.getChemicalEntity();
        for (FPIter iter : classyfireFingerprint.presentFingerprints()) {
            ClassyfireProperty prop = (ClassyfireProperty)iter.getMolecularProperty();
            final int thisPrio = prop.getFixedPriority();
            final int bestPrio = bestAbove50.getFixedPriority();
            if (thisPrio >= bestPrio) {
                if (thisPrio>bestPrio || prop.level>bestAbove50.level)
                    bestAbove50 = prop;
            }
        }
        return bestAbove50;
    }

    public ClassyfireProperty[] getPredictedLeafs(ProbabilityFingerprint classyfireFingerprint) {
        return getPredictedLeafs(classyfireFingerprint,0.5d);
    }

    public ClassyfireProperty[] getPredictedLeafs(ProbabilityFingerprint classyfireFingerprint, double probabilityThreshold) {
        HashSet<ClassyfireProperty> predictedClasses = new HashSet<>();
        for (FPIter iter : classyfireFingerprint) {
            if (iter.getProbability()>=probabilityThreshold) {
                predictedClasses.add((ClassyfireProperty) iter.getMolecularProperty());
            }
        }
        ClassyfireProperty[] nodes = predictedClasses.toArray(ClassyfireProperty[]::new);
        for (ClassyfireProperty node : nodes) {
            for (ClassyfireProperty ancestor : node.getAncestors(true)) {
                predictedClasses.remove(ancestor);
            }
        }
        return predictedClasses.toArray(ClassyfireProperty[]::new);
    }
}
