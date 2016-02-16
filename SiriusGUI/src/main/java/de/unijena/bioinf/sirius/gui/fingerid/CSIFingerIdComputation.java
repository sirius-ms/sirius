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

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.fingerid.*;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * keeps all compounds in memory
 */
public class CSIFingerIdComputation {

    protected FingerprintStatistics statistics;
    protected int[] fingerprintIndizes;
    protected TIntObjectHashMap<String> absoluteIndex2Smarts;
    protected String[] relativeIndex2Smarts;
    protected HashMap<String, Compound> compounds;
    protected HashMap<MolecularFormula, List<Compound>> compoundsPerFormula;
    protected boolean configured = false;
    protected MarvinsScoring scoring = new MarvinsScoring();
    protected File directory;
    protected ExecutorService service;

    public CSIFingerIdComputation() {
        setDirectory(getDefaultDirectory());
        this.compounds = new HashMap<>(32768);
        this.compoundsPerFormula = new HashMap<>(128);
        this.service = Executors.newSingleThreadExecutor();
        absoluteIndex2Smarts=new TIntObjectHashMap<>(4096);
        try {
            readSmarts();
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO: Fix
        }
    }

    public double[] getFScores() {
        return statistics.f;
    }

    private void readSmarts() throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(CSIFingerIdComputation.class.getResourceAsStream("/sirius/features_smarts.tsv")));
        String line;
        while ((line=br.readLine())!=null) {
            final int tabI = line.indexOf('\t');
            if (tabI < 0) continue;
            final int index = Integer.parseInt(line.substring(0,tabI));
            final String smarts = line.substring(tabI+1);
            absoluteIndex2Smarts.put(index, smarts);
        }
    }

    public void compute(Ms2Experiment experiment, SiriusResultElement elem) throws IOException {
        if (statistics==null) {

            final TIntArrayList list = new TIntArrayList(4096);
            this.statistics = new WebAPI().getStatistics(list);
            this.fingerprintIndizes = list.toArray();
            this.relativeIndex2Smarts = new String[fingerprintIndizes.length];
            for (int i=0; i < fingerprintIndizes.length; ++i) {
                relativeIndex2Smarts[i] = absoluteIndex2Smarts.get(fingerprintIndizes[i]);
            }
        }
        final MolecularFormula formula = elem.getMolecularFormula();
        final List<Compound> compounds = getCompoundsForGivenMolecularFormula(formula);
        final Future<double[]> platts = new WebAPI().predictFingerprint(service, experiment, elem.getRawTree());
        final Scorer scorer = scoring.getScorer(statistics);
        try {
            final double[] plattScores = platts.get();
            final TreeMap<Double, Compound> map = new TreeMap<>();
            final Query query = new Query("_query_", null, plattScores);
            scorer.preprocessQuery(query, statistics);
            for (Compound c : compounds) {
                map.put(scorer.score(query, new Candidate(c.inchi.in2D, c.fingerprint), statistics), c);
            }
            final double[] scores = new double[map.size()];
            final Compound[] comps = new Compound[map.size()];
            int k=0;
            for (Map.Entry<Double, Compound> entry : map.descendingMap().entrySet()) {
                scores[k] = entry.getKey();
                comps[k] = entry.getValue();
                ++k;
            }
            elem.setFingerIdData(new FingerIdData(comps, scores, plattScores));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public File getDirectory() {
        return directory;
    }

    public File getDefaultDirectory() {
        final String val = System.getenv("CSI_FINGERID_STORAGE");
        if (val!=null) return new File(val);
        return new File(System.getProperty("user.home"), "csi_fingerid_cache");
    }

    public List<Compound> getCompoundsForGivenMolecularFormula(MolecularFormula formula) throws IOException {
        final List<Compound> formulas = compoundsPerFormula.get(formula);
        if (formulas==null) {
            loadCompoundsForGivenMolecularFormula(formula);
            return compoundsPerFormula.get(formula);
        } else return formulas;
    }

    public void loadCompoundsForGivenMolecularFormula(MolecularFormula formula) throws IOException {
        if (!directory.exists()) {
            directory.mkdir();
        }
        final File mfile = new File(directory, formula.toString() + ".json");
        final List<Compound> compounds;
        if (mfile.exists()) {
            try (final JsonParser parser = Json.createParser(new BufferedInputStream(new FileInputStream(mfile)))) {
                compounds = new ArrayList<>();
                Compound.parseCompounds(fingerprintIndizes, compounds, parser);
            }
        } else {
            compounds = new WebAPI().getCompoundsFor(formula, mfile, fingerprintIndizes);
        }
        for (Compound c : compounds) {
            this.compounds.put(c.inchi.key2D(), c);
        }
        this.compoundsPerFormula.put(formula, compounds);
    }

    public Compound getCompound(String inchiKey2D) {
        return compounds.get(inchiKey2D); // TODO: probably we have to implement a cache here
    }

    public void setDirectory(File directory) {
        this.directory = directory;
        this.compounds = new HashMap<>();
    }
}
