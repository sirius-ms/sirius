package de.unijena.bioinf.fingerid.blast;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.exceptions.InsufficientDataException;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.jjobs.JobManager;
import gnu.trove.list.array.TShortArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;


public class BayesScoringUtilsTest {

    final int minNumInformativeProperties = 1;

    public static void main(String... args) throws InsufficientDataException, ExecutionException {
        SiriusJobs.setGlobalJobManager(5);
        long start = System.currentTimeMillis();
        (new BayesScoringUtilsTest()).testMutualInfoParallelWithRandomMatrices();
        System.out.println("Duration: "+(System.currentTimeMillis() - start));
        (new BayesScoringUtilsTest()).testMutualInfoParallel();
    }


    public void testMutualInfoParallel() throws InsufficientDataException, ExecutionException {
        FingerprintVersion fingerprintVersion = new TestFingerprintVersion(10);
        MaskedFingerprintVersion maskedFingerprintVersion = MaskedFingerprintVersion.allowAll(fingerprintVersion);
        short[][] fingerprintIndices = new short[][]{   new short[]{1,2,4}, //a
                new short[]{2,3,5,7}, //b
                new short[]{1,2,4}, //a
                new short[]{2,3,5,7}, //b
                new short[]{2,5,7},   //-b
                new short[]{1,2,4,8,9}, //--a
                new short[]{2,6,7,8,9}, //c
                new short[]{5,6,7,8,9}, //c
                new short[]{},
                new short[]{1,2,4,7}, //-a

        };

        int[] groundTruthProperties = new int[]{1,2,3,4,5,6,7,8,9};
        double[][] groundTruthMutualInfo = new double[][]{
                new double[]{0.67301167, 0.11849392, 0.11849392, 0.67301167,
                0.29110317, 0.11849392, 0.17774088, 0.00402174, 0.00402174},
               new double[]{ 0.11849392, 0.50040242, 0.05053431, 0.11849392,
                        0.00513164, 0.06035686, 0.00513164, 0.02236675, 0.02236675},
               new double[]{ 0.11849392, 0.05053431, 0.50040242, 0.11849392,
                        0.22314355, 0.05053431, 0.11849392, 0.08161371, 0.08161371},
               new double[]{ 0.67301167, 0.11849392, 0.11849392, 0.67301167,
                        0.29110317, 0.11849392, 0.17774088, 0.00402174, 0.00402174},
               new double[]{ 0.29110317, 0.00513164, 0.22314355, 0.29110317,
                        0.67301167, 0.00513164, 0.29110317, 0.00402174, 0.00402174},
               new double[]{ 0.11849392, 0.06035686, 0.05053431, 0.11849392,
                        0.00513164, 0.50040242, 0.11849392, 0.30944817, 0.30944817},
               new double[]{ 0.17774088, 0.00513164, 0.11849392, 0.17774088,
                        0.29110317, 0.11849392, 0.67301167, 0.00402174, 0.00402174},
               new double[]{ 0.00402174, 0.02236675, 0.08161371, 0.00402174,
                        0.00402174, 0.30944817, 0.00402174, 0.6108643 , 0.6108643 },
               new double[]{ 0.00402174, 0.02236675, 0.08161371, 0.00402174,
                        0.00402174, 0.30944817, 0.00402174, 0.6108643 , 0.6108643}
        };
        List<Fingerprint> maskedFingerprints = createMaskedFingerprints(fingerprintVersion, fingerprintIndices);

        testMutualInfoParallel(maskedFingerprintVersion, maskedFingerprints, groundTruthProperties, groundTruthMutualInfo, 1e-8);
    }

    public void testMutualInfoParallelWithRandomMatrices() throws InsufficientDataException, ExecutionException {
        int numberOfProperties = 1000;
        int numberOfFPs = 10000;
        for (int i = 0; i < 10; i++) {
            FingerprintVersion fingerprintVersion = new TestFingerprintVersion(numberOfProperties);
            short[][] fingerprintIndices = createRandomFingerprintMatrix(numberOfFPs, numberOfProperties, 0.1);
            testMutualInfoParallel(fingerprintVersion, fingerprintIndices);
        }

    }

    private short[][] createRandomFingerprintMatrix(int numberOfFPs, int numberOfProperties, double density) {
        Random random = new Random();
        short[][] fingerprintIndices = new short[numberOfFPs][];
        for (int i = 0; i < fingerprintIndices.length; i++) {
            TShortArrayList fingerprint = new TShortArrayList();
            for (short j = 0; j < numberOfProperties; j++) {
                if (random.nextDouble()<=density) fingerprint.add(j);
            }
            fingerprintIndices[i] = fingerprint.toArray();
        }
        return fingerprintIndices;
    }

    public void testMutualInfoParallel(FingerprintVersion fingerprintVersion, short[][] fingerprintIndices) throws InsufficientDataException, ExecutionException {
        JobManager jobManager = SiriusJobs.getGlobalJobManager();

        MaskedFingerprintVersion maskedFingerprintVersion = MaskedFingerprintVersion.allowAll(fingerprintVersion);
        BayesnetScoringTrainingData dummyData = new BayesnetScoringTrainingData(new MolecularFormula[0], new Fingerprint[0], new ProbabilityFingerprint[0], new PredictionPerformance[]{new PredictionPerformance()});
        BayesianScoringUtils bayesianScoringUtils = BayesianScoringUtils.getInstance(maskedFingerprintVersion, dummyData, true, jobManager);
        List<Fingerprint> maskedFingerprints = createMaskedFingerprints(fingerprintVersion, fingerprintIndices);

        BayesianScoringUtils.MutualInformationAndIndices mutualInformationAndIndices = bayesianScoringUtils.mutualInfoBetweenProperties(maskedFingerprints, 1.0, minNumInformativeProperties);


        long start = System.currentTimeMillis();
        testMutualInfoParallel(maskedFingerprintVersion, maskedFingerprints, mutualInformationAndIndices.usedProperties, mutualInformationAndIndices.mutualInfo, 1e-12);
        System.out.println("Duration parallel: "+(System.currentTimeMillis() - start));
    }

    @NotNull
    private List<Fingerprint> createMaskedFingerprints(FingerprintVersion fingerprintVersion, short[][] fingerprintIndices) {
        List<Fingerprint> maskedFingerprints = new ArrayList<>();

        for (short[] fingerprintIndex : fingerprintIndices) {
            maskedFingerprints.add(new ArrayFingerprint(fingerprintVersion, fingerprintIndex));
        }
        return maskedFingerprints;
    }

    public void testMutualInfoParallel(MaskedFingerprintVersion maskedFingerprintVersion, List<Fingerprint> maskedFingerprints, int[] groundTruthProperties, double[][] groundTruthMutualInfo, double delta) throws InsufficientDataException, ExecutionException {
        JobManager jobManager = SiriusJobs.getGlobalJobManager();

        BayesnetScoringTrainingData dummyData = new BayesnetScoringTrainingData(new MolecularFormula[0], new Fingerprint[0], new ProbabilityFingerprint[0], new PredictionPerformance[]{new PredictionPerformance()});
        BayesianScoringUtils bayesianScoringUtils = BayesianScoringUtils.getInstance(maskedFingerprintVersion, dummyData, true, jobManager);

        int numThreads = 30; //test dead-lock possibility // SiriusJobs.getCPUThreads();
        BayesianScoringUtils.MutualInformationAndIndices mutualInformationAndIndicesParallel = SiriusJobs.getGlobalJobManager().submitJob(bayesianScoringUtils.createMutualInfoJJob(maskedFingerprints, 1.0, minNumInformativeProperties, numThreads)).awaitResult();


        int length = groundTruthProperties.length;

        Assert.assertArrayEquals("Mutual information properties differ when computed in parallel.", groundTruthProperties, mutualInformationAndIndicesParallel.usedProperties);

        for (int i = 0; i < length; i++) {
            Assert.assertArrayEquals("Mutual information matrix differs when computed in parallel.", groundTruthMutualInfo[i], mutualInformationAndIndicesParallel.mutualInfo[i], delta);
        }

//        System.out.println("Properties: "+Arrays.toString(mutualInformationAndIndicesParallel.usedProperties));
//        for (int i = 0; i < mutualInformationAndIndicesParallel.mutualInfo.length; i++) {
//            System.out.println(Arrays.toString(mutualInformationAndIndicesParallel.mutualInfo[i]));
//        }
    }

    class TestFingerprintVersion extends FingerprintVersion {

        final int numberOrProperties;

        TestFingerprintVersion(int numberOfProperties) {
            this.numberOrProperties = numberOfProperties;
        }

        @Override
        public MolecularProperty getMolecularProperty(int index) {
            return null;
        }

        @Override
        public int size() {
            return numberOrProperties;
        }

        @Override
        public boolean compatible(FingerprintVersion fingerprintVersion) {
            if (fingerprintVersion instanceof TestFingerprintVersion) return true;
            return false;
        }

        @Override
        public boolean identical(FingerprintVersion fingerprintVersion) {
            return fingerprintVersion instanceof TestFingerprintVersion && size() == fingerprintVersion.size();
        }
    }
}
