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

package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.tensorflow.Tensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrainingData {

    public final static CdkFingerprintVersion VERSION = CdkFingerprintVersion.getComplete();

    // if true, features are scaled by maximum instead of standard deviation
    public static boolean SCALE_BY_MAX = false;
    public static boolean VECNORM_SCALING = false;
    public static boolean PLATT_CENTERING = true;
    public static boolean SCALE_BY_STD = false;

    public static boolean CLIPPING = false;

    public static final boolean INCLUDE_FINGERPRINT = false;

    public static final boolean SAMPLE_FROM_TEMPLATE_FINGERPRINTS = true;

    protected static int GROW = 1;

    protected ClassyFireFingerprintVersion classyFireFingerprintVersion;
    protected MaskedFingerprintVersion classyFireMask;
    protected MaskedFingerprintVersion fingerprintVersion;
    protected MaskedFingerprintVersion canopusFingerprint, withoutCanopus, canopusOnly;
    protected final TIntObjectHashMap<CompoundClass> compoundClasses;
    protected final HashMap<String, CompoundClass> name2class;
    protected final List<LabeledCompound> compounds;
    protected final List<LabeledCompound> prioritizedCompounds;
    protected final List<EvaluationInstance> npcInstances;

    protected List<LabeledCompound> npcList;

    protected final HashSet<String> blacklist;

    protected CustomFingerprintVersion dummyFingerprintVersion;

    protected List<EvaluationInstance> crossvalidation, independent;

    public double[] formulaNorm, formulaScale, plattNorm, plattScale;

    protected Sampler fingerprintSampler;
    protected Pattern independentPattern;
    protected int nplatts, nformulas, nlabels;

    protected NPCFingerprintVersion NPCVersion;

    public boolean isNPC() {
        return !npcList.isEmpty();
    }

    public TrainingData(File env) throws IOException {
        this(env,null);
    }

    public TrainingData(File env, Pattern independent) throws IOException {
        this.independentPattern = independent;
        this.compoundClasses = new TIntObjectHashMap<>(4000);
        this.compounds = new ArrayList<>(1200000);
        this.blacklist = new HashSet<>(12000);
        this.name2class = new HashMap<>(4000);
        this.npcInstances = new ArrayList<>();
        this.prioritizedCompounds = new ArrayList<>();
        setupEnv(env);
    }

    public void normalizeVector(LabeledCompound compound) {
        final float[] fv = new float[compound.formulaFeatures.length];
        for (int k=0; k < compound.formulaFeatures.length; ++k) {
            double val = compound.formulaFeatures[k];
            val -= formulaNorm[k];
            val /= formulaScale[k];
            fv[k] = (float)val;
        }
        compound.formulaFeaturesF = fv;
    }


    public TrainingBatch fillUpWithTrainDataNPC(boolean includeIndep) {
        final Random r = new Random();
        final List<EvaluationInstance> instances = new ArrayList<>();
        instances.addAll(crossvalidation);
        if (includeIndep){
            instances.addAll(independent);
        } else {
            final Set<String> independentCompound = independent.stream().map(x->x.compound.inchiKey).collect(Collectors.toSet());
            instances.removeIf(x->independentCompound.contains(x.compound.inchiKey));
        }
        instances.removeIf(x->x.compound.npcLabel==null);
        //instances.addAll(independent);
        final TIntIntHashMap counter = new TIntIntHashMap();
        for (EvaluationInstance i : instances) {
            for (short index : i.compound.npcLabel.toIndizesArray())
                counter.adjustOrPutValue(index, 1, 1);
        }
        final List<LabeledCompound> xs = new ArrayList<>(npcList);
        Collections.shuffle(xs);
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        xs.removeIf(x->blacklist.contains(x.inchiKey));
        final List<Future<EvaluationInstance>> futures = new ArrayList<>();
        for (LabeledCompound c : xs) {
            boolean use = false;
            for (FPIter x : c.npcLabel) {
                if (counter.get(x.getIndex())<10) {
                    use = true;
                    break;
                }
            }
            if (use) {
                for (FPIter x : c.npcLabel) {
                    counter.adjustOrPutValue(x.getIndex(),1,1);
                }
                futures.add(service.submit(new Callable<EvaluationInstance>() {
                    @Override
                    public EvaluationInstance call() throws Exception {
                        return new EvaluationInstance("",fingerprintSampler.sample(c.fingerprint,false), c);
                    }
                }));
            }
        }
        for (Future<EvaluationInstance> x : futures) {
            try {
                instances.add(x.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        service.shutdown();
        Collections.shuffle(instances);

        return generateNPCBatch(instances);
    }

    public TrainingBatch fillUpWithTrainData(boolean includeIndep) {
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final List<Future<EvaluationInstance>> futures = new ArrayList<>();
        final Random r = new Random();
        final List<EvaluationInstance> instances = new ArrayList<>();
        instances.addAll(crossvalidation);
        if (includeIndep){
            instances.addAll(independent);
        } else {
            final Set<String> independentCompound = independent.stream().map(x->x.compound.inchiKey).collect(Collectors.toSet());
            instances.removeIf(x->independentCompound.contains(x.compound.inchiKey));
        }
        //instances.addAll(independent);
        final TIntIntHashMap counter = new TIntIntHashMap();
        for (EvaluationInstance i : instances) {
            for (short index : i.compound.label.toIndizesArray())
                counter.adjustOrPutValue(index, 1, 1);
        }
        counter.forEachEntry(new TIntIntProcedure() {
            @Override
            public boolean execute(int klass, int count) {
                if (count < 30) {
                    // generate example data
                    final List<LabeledCompound> compounds = compoundClasses.get(klass).drawExamples(20-count, r);
                    // sample fingerprints
                    for (LabeledCompound c : compounds) {
                        for (short index : c.label.toIndizesArray())
                            counter.adjustOrPutValue(index, 1,1);
                        futures.add(service.submit(new Callable<EvaluationInstance>() {
                            @Override
                            public EvaluationInstance call() throws Exception {
                                final ProbabilityFingerprint fingerprint = TrainingData.SAMPLE_FROM_TEMPLATE_FINGERPRINTS ? fingerprintSampler.sample(c.fingerprint,false) : fingerprintSampler.sampleIndependently(c.fingerprint, false);
                                return new EvaluationInstance("", fingerprint, c);
                            }
                        }));
                    }
                }
                return true;
            }
        });
        futures.forEach(x-> {
            try {
                instances.add(x.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        service.shutdown();
        return generateBatch(instances);
    }

    protected void addNormalizedPlatts(FloatBuffer buffer, double[] array) {
        if (VECNORM_SCALING) {
            double vecnorm = 0d;
            for (int i=0; i < array.length; ++i) {
                if (PLATT_CENTERING) {
                    array[i] -= plattNorm[i];
                }
                vecnorm += array[i]*array[i];
            }
            vecnorm = Math.sqrt(vecnorm);
            for (int i=0; i < nplatts; ++i)  {
                buffer.put((float)(array[i]/vecnorm));
            }
        } else if (!CLIPPING) {
            if (PLATT_CENTERING||SCALE_BY_STD) {
                for (int i=0; i < array.length; ++i) {
                    buffer.put((float) ((array[i] - plattNorm[i])/plattScale[i]));
                }
            } else {
                for (double val : array) {
                    buffer.put((float) val);
                }
            }
        } else {
            for (double val : array) {
                // clipping
                buffer.put((float)rescale(val));
            }
        }
    }

    private double rescale(double input){
        double x = Math.min(0.8, Math.max(input,0.2));
        x -= 0.2;
        x /= 0.6;
        return x;
    }

    public TrainingBatch generateNPCBatch(List<EvaluationInstance> instances) {
        if (NPCVersion==null) return generateBatch(instances);
        final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
        final FloatBuffer npcLabels = FloatBuffer.allocate(instances.size()*NPCVersion.size());
        for (EvaluationInstance i : instances) {
            addNormalizedPlatts(platts, i.fingerprint.toProbabilityArray());
            formulas.put(i.compound.formulaFeaturesF);
            labels.put(getLabelVector(i.compound));
            npcLabels.put(getNPCLabelVector(i.compound));

        }
        platts.rewind();
        formulas.rewind();
        labels.rewind();
        npcLabels.rewind();
        return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels), Tensor.create(new long[]{instances.size(), NPCVersion.size()}, npcLabels));
    }
    public TrainingBatch generateBatch(List<EvaluationInstance> instances) {
        final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
        for (EvaluationInstance i : instances) {
            addNormalizedPlatts(platts, i.fingerprint.toProbabilityArray());
            formulas.put(i.compound.formulaFeaturesF);
            labels.put(getLabelVector(i.compound));

        }
        platts.rewind();
        formulas.rewind();
        labels.rewind();
        return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels));
    }


    static enum SamplingStrategy {
        PERFECT, INDEPENDENT, INDEPENDENT_DISTURBED, TEMPLATE, DISTURBED_TEMPLATE, CONDITIONAL,STATISTICAL, STATISTICAL_DISTURBED;
    }

    public static interface SamplingStrategyFunction {
        public SamplingStrategy sample(EvaluationInstance instance, int k);
    }

    public TrainingBatch resample(List<EvaluationInstance> instances, SamplingStrategyFunction instance) {
        final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
        int k=0;
        for (EvaluationInstance i : instances) {
            final SamplingStrategy strategy = instance.sample(i, k++);
            switch (strategy) {
                case PERFECT: addNormalizedPlatts(platts,i.compound.fingerprint.toProbabilityArray()); break;
                case INDEPENDENT: addNormalizedPlatts(platts, fingerprintSampler.sampleIndependently(i.compound.fingerprint, false).toProbabilityArray()); break;
                case INDEPENDENT_DISTURBED: addNormalizedPlatts(platts, fingerprintSampler.sampleIndependently(i.compound.fingerprint, true).toProbabilityArray()); break;
                case TEMPLATE: addNormalizedPlatts(platts, fingerprintSampler.sample(i.compound.fingerprint, false).toProbabilityArray()); break;
                case DISTURBED_TEMPLATE: addNormalizedPlatts(platts, fingerprintSampler.sample(i.compound.fingerprint, true).toProbabilityArray()); break;
                case CONDITIONAL: addNormalizedPlatts(platts, fingerprintSampler.sampleFromCovariance(i.compound.fingerprint).toProbabilityArray()); break;
                case STATISTICAL: addNormalizedPlatts(platts, fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 1)); break;
                case STATISTICAL_DISTURBED: addNormalizedPlatts(platts, fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 5));
            }
            formulas.put(i.compound.formulaFeaturesF);
            labels.put(getLabelVector(i.compound));
            ++k;
        }
        platts.rewind();
        formulas.rewind();
        labels.rewind();
        return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels));
    }

    public TrainingBatch resample(List<EvaluationInstance> instances) {
        return resample(instances, 1000);
    }

    public TrainingBatch resampleMultithreaded(List<EvaluationInstance> instances, SamplingStrategyFunction f) {
        ExecutorService service = Executors.newFixedThreadPool(40);
        try {
            final List<Future<double[]>> values = new ArrayList<>();
            int j = 0;
            for (final EvaluationInstance instance : instances) {
                final SamplingStrategy S = f.sample(instance, j++);
                values.add(service.submit(new Callable<double[]>() {
                    @Override
                    public double[] call() {
                        return sampleFingerprintVector(instance.compound, S);
                    }
                }));
            }
            final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
            final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
            final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
            for (int k = 0; k < instances.size(); ++k) {
                try {
                    addNormalizedPlatts(platts, values.get(k).get());
                    formulas.put(instances.get(k).compound.formulaFeaturesF);
                    labels.put(getLabelVector(instances.get(k).compound));
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            platts.rewind();
            formulas.rewind();
            labels.rewind();
            return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels));
        } finally {
            service.shutdown();
        }
    }

    public TrainingBatch resample(List<EvaluationInstance> instances, int iterationNum) {
        final List<SamplingStrategy> samplingStrategies = getSamplingStrategies(iterationNum);
        return resample(instances, samplingStrategies);
    }

    public double[] sampleBy(EvaluationInstance i, SamplingStrategy strategy) {
        switch (strategy) {
            case PERFECT: return i.compound.fingerprint.toProbabilityArray();
            case INDEPENDENT: return fingerprintSampler.sampleIndependently(i.compound.fingerprint, false).toProbabilityArray();
            case INDEPENDENT_DISTURBED: return fingerprintSampler.sampleIndependently(i.compound.fingerprint, true).toProbabilityArray();
            case TEMPLATE: return fingerprintSampler.sample(i.compound.fingerprint, false).toProbabilityArray();
            case DISTURBED_TEMPLATE: return fingerprintSampler.sample(i.compound.fingerprint, true).toProbabilityArray();
            case CONDITIONAL: return fingerprintSampler.sampleFromCovariance(i.compound.fingerprint).toProbabilityArray();
            case STATISTICAL: return fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 1);
            case STATISTICAL_DISTURBED: return fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 5);
            default:throw new IllegalArgumentException("Unknown strategy '" + String.valueOf(strategy) + "'");
        }
    }

    public TrainingBatch resample(List<EvaluationInstance> instances, List<SamplingStrategy> samplingStrategies) {
        final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
        int k=0;
        for (EvaluationInstance i : instances) {
            final SamplingStrategy strategy = samplingStrategies.get(k%samplingStrategies.size());
            switch (strategy) {
                case PERFECT: addNormalizedPlatts(platts,i.compound.fingerprint.toProbabilityArray()); break;
                case INDEPENDENT: addNormalizedPlatts(platts, fingerprintSampler.sampleIndependently(i.compound.fingerprint, false).toProbabilityArray()); break;
                case INDEPENDENT_DISTURBED: addNormalizedPlatts(platts, fingerprintSampler.sampleIndependently(i.compound.fingerprint, true).toProbabilityArray()); break;
                case TEMPLATE: addNormalizedPlatts(platts, fingerprintSampler.sample(i.compound.fingerprint, false).toProbabilityArray()); break;
                case DISTURBED_TEMPLATE: addNormalizedPlatts(platts, fingerprintSampler.sample(i.compound.fingerprint, true).toProbabilityArray()); break;
                case CONDITIONAL: addNormalizedPlatts(platts, fingerprintSampler.sampleFromCovariance(i.compound.fingerprint).toProbabilityArray()); break;
                case STATISTICAL: addNormalizedPlatts(platts, fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 1)); break;
                case STATISTICAL_DISTURBED: addNormalizedPlatts(platts, fingerprintSampler.sampleByErrorStats(i.compound.fingerprint, 5));
            }
            formulas.put(i.compound.formulaFeaturesF);
            labels.put(getLabelVector(i.compound));
            ++k;
        }
        platts.rewind();
        formulas.rewind();
        labels.rewind();
        return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels));
    }
/*
    public TrainingBatch resample(List<EvaluationInstance> instances, int iterationNum) {
        final int GROW = TrainingData.GROW;
        final ArrayList<LabeledCompound> compounds = new ArrayList<>();
        final boolean[] sampleIndependently = new boolean[20];
        if (SAMPLE_FROM_TEMPLATE_FINGERPRINTS){
            final List<Boolean> bs = new ArrayList<>();
            for (int k=0; k < 20; ++k) bs.add(false);
            for (int k=0, n=Math.min(15, (int)Math.ceil(15*iterationNum/(GROW*10000d))); k < n; ++k) {
                bs.set(k,true);
            }
            Collections.shuffle(bs);
            for (int k=0; k < 20; ++k) sampleIndependently[k] = !bs.get(k);
        } else {
            Arrays.fill(sampleIndependently, true);
        }
        final FloatBuffer platts = FloatBuffer.allocate(instances.size() * nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(instances.size() * nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(instances.size() * nlabels);
        int k=0;
        for (EvaluationInstance i : instances) {
            final boolean indep = sampleIndependently[k%sampleIndependently.length];
            addNormalizedPlatts(platts, indep ? fingerprintSampler.sampleIndependently(i.compound.fingerprint).toProbabilityArray() : fingerprintSampler.sample(i.compound.fingerprint).toProbabilityArray());
            formulas.put(i.compound.formulaFeaturesF);
            labels.put(getLabelVector(i.compound));
            ++k;
        }
        platts.rewind();
        formulas.rewind();
        labels.rewind();
        return new TrainingBatch(Tensor.create(new long[]{instances.size(), nplatts}, platts), Tensor.create(new long[]{instances.size(), nformulas}, formulas), Tensor.create(new long[]{instances.size(), nlabels}, labels));
    }
*/

    public TrainingBatch generateBatch(int iterationNum, BufferedTrainData buffer, final ExecutorService service) {
        //final long time1 = System.currentTimeMillis();
        final int GROW = TrainingData.GROW;
        final ArrayList<LabeledCompound> compounds = new ArrayList<>();
        final SamplingStrategy[] strategies = getSamplingStrategies(iterationNum).toArray(new SamplingStrategy[0]);
        final ArrayList<Future<double[]>> futures = new ArrayList<>();
        balancedSample(iterationNum, 6, 1000, 1000, new Function<LabeledCompound, LabeledCompound>(){
            protected int counter=0;
            @Override
            public LabeledCompound apply(final LabeledCompound input) {
                final int n = counter++;
                compounds.add(input);
                futures.add(service.submit(new Callable<double[]>() {
                    @Override
                    public double[] call() throws Exception {
                        return sampleFingerprintVector(input, strategies[n % strategies.length] );
                    }
                }));
                return input;
            }
        });

        final FloatBuffer platts = FloatBuffer.allocate(compounds.size()*nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(compounds.size()*nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(compounds.size()*nlabels);

        final TIntArrayList indizes = new TIntArrayList(compounds.size());
        for (int i=0; i < compounds.size(); ++i) indizes.add(i);
        indizes.shuffle(new Random());
        int k=0;
        for (int index : indizes.toArray()) {
            final LabeledCompound c = compounds.get(index);
            try {
                final double[] fp = futures.get(index).get();
                addNormalizedPlatts(platts, fp);
                formulas.put(c.formulaFeaturesF);
                final float[] lbs = getLabelVector(c);
                labels.put(lbs);
                if (buffer!=null) {
                    final BufferedTrainData.Buffer b = buffer.getBuffer(k++);
                    synchronized (b) {
                        if (b.filled < b.size) {
                            addNormalizedPlatts(b.p, fp);
                            b.f.put(c.formulaFeaturesF);
                            b.l.put(lbs);
                            b.fill();
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        platts.rewind();
        formulas.rewind();
        labels.rewind();

        //final long time2 = System.currentTimeMillis();
        //System.out.println(((time2-time1)/1000) + " s for sampling the data");


        return new TrainingBatch(Tensor.create(new long[]{compounds.size(), nplatts}, platts),
                Tensor.create(new long[]{compounds.size(), nformulas}, formulas), Tensor.create(new long[]{compounds.size(), nlabels}, labels));
    }

    public TrainingBatch generateNPCBatch(int iterationNum, final ExecutorService service) {
        //final long time1 = System.currentTimeMillis();
        final int GROW = TrainingData.GROW;
        final ArrayList<LabeledCompound> compounds = new ArrayList<>();
        final SamplingStrategy[] strategies = getSamplingStrategies(iterationNum).toArray(new SamplingStrategy[0]);
        final ArrayList<Future<double[]>> futures = new ArrayList<>();
        {
            List<Integer> indizes = new ArrayList<>();
            int ix = 0;
            for (LabeledCompound c : npcList) indizes.add(ix++);
            Collections.shuffle(indizes);
            indizes = indizes.subList(0,10000);
            ix = 0;
            for (Integer i : indizes) {
                final LabeledCompound c = npcList.get(i);
                compounds.add(c);
                final int n = ix;
                futures.add(service.submit(() -> sampleFingerprintVector(c, strategies[n % strategies.length])));
                ++ix;
            }
        }

        final FloatBuffer platts = FloatBuffer.allocate(compounds.size()*nplatts);
        final FloatBuffer formulas = FloatBuffer.allocate(compounds.size()*nformulas);
        final FloatBuffer labels = FloatBuffer.allocate(compounds.size()*nlabels);
        final FloatBuffer npcLabels = FloatBuffer.allocate(compounds.size()*NPCVersion.size());

        final TIntArrayList indizes = new TIntArrayList(compounds.size());
        for (int i=0; i < compounds.size(); ++i) indizes.add(i);
        indizes.shuffle(new Random());
        int k=0;
        for (int index : indizes.toArray()) {
            final LabeledCompound c = compounds.get(index);
            try {
                final double[] fp = futures.get(index).get();
                addNormalizedPlatts(platts, fp);
                formulas.put(c.formulaFeaturesF);
                final float[] lbs = getLabelVector(c);
                final float[] npcL = getNPCLabelVector(c);
                labels.put(lbs);
                npcLabels.put(npcL);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        platts.rewind();
        formulas.rewind();
        labels.rewind();
        npcLabels.rewind();

        //final long time2 = System.currentTimeMillis();
        //System.out.println(((time2-time1)/1000) + " s for sampling the data");


        return new TrainingBatch(Tensor.create(new long[]{compounds.size(), nplatts}, platts),
                Tensor.create(new long[]{compounds.size(), nformulas}, formulas), Tensor.create(new long[]{compounds.size(), nlabels}, labels), Tensor.create(new long[]{compounds.size(), NPCVersion.size()}, npcLabels));
    }

    private List<SamplingStrategy> getSamplingStrategies(int iterationNum) {
        {
            final SamplingStrategy[] strategies;
            if (iterationNum < 200) {
                strategies = new SamplingStrategy[100];
                /*
                for (int k=0; k < 10; ++k)
                    strategies[k] = SamplingStrategy.INDEPENDENT;
                for (int k=10; k < 19; ++k)
                    strategies[k] = SamplingStrategy.TEMPLATE;
                strategies[19] = SamplingStrategy.PERFECT;
                */
                for (int k=0; k < 66; ++k) {
                    strategies[k] = SamplingStrategy.STATISTICAL;
                }
                for (int k=66; k < 90; ++k) {
                    strategies[k] = SamplingStrategy.TEMPLATE;
                }
                for (int k=90; k < 100; ++k) {
                    strategies[k] = SamplingStrategy.PERFECT;
                }

            } else {
                strategies = new SamplingStrategy[200];
                for (int k=0; k < 70; ++k) {
                    strategies[k] = SamplingStrategy.STATISTICAL;
                }
                for (int k=70; k < 80; ++k) {
                    strategies[k] = SamplingStrategy.STATISTICAL_DISTURBED;
                }
                for (int k=80; k < 180; ++k) {
                    strategies[k] = SamplingStrategy.TEMPLATE;
                }
                for (int k=180; k < 199; ++k) {
                    strategies[k] = SamplingStrategy.DISTURBED_TEMPLATE;
                }
                for (int k=199; k < 200; ++k) {
                    strategies[k] = SamplingStrategy.PERFECT;
                }

            }
            final ArrayList<SamplingStrategy> list = new ArrayList<>(Arrays.asList(strategies));
            Collections.shuffle(list);
            return list;
        }
    }

    private void balancedSample(final int iterationNum, final int numberPerClass,  final int numberOfPriorized, final int numberTotal,Function<LabeledCompound, LabeledCompound> function) {
        final Random r = new Random();
        final TIntHashSet chosen = new TIntHashSet();
        final HashSet<String> chosenCompounds = new HashSet<>();
        for (CompoundClass klass : compoundClasses.valueCollection()) {
            if (klass.compounds.size() < 10*numberPerClass) {
                final List<LabeledCompound> sel = new ArrayList<>(klass.compounds);
                Collections.shuffle(sel, r);
                int count=0;
                for (LabeledCompound l : sel) {
                    if (!chosenCompounds.contains(l.inchiKey) && !blacklist.contains(l.inchiKey)) {
                        chosenCompounds.add(l.inchiKey);
                        function.apply(l);
                        if (++count >= numberPerClass) break;
                    }
                }
            } else {
                chosen.clear();
                int ntry = 0;
                final int ntotal=numberPerClass*10;
                int selectedSize = 0;
                while (selectedSize < numberPerClass && ++ntry < ntotal) {
                    final int n = r.nextInt(klass.compounds.size());
                    if (!chosen.contains(n)) {
                        chosen.add(n);
                        ++selectedSize;
                        final LabeledCompound c = klass.compounds.get(n);
                        if (!chosenCompounds.contains(c.inchiKey)  && !blacklist.contains(c.inchiKey)) {
                            chosenCompounds.add(c.inchiKey);
                            function.apply(c);
                        }
                    }
                }
            }
        }
        int offset = iterationNum*numberTotal;
        for (int i=0; i < numberTotal; ++i) {
            final int j = (i+offset) % compounds.size();
            if (!chosenCompounds.contains(compounds.get(j).inchiKey) && !blacklist.contains(compounds.get(j).inchiKey)) {
                function.apply(compounds.get(j));
                chosenCompounds.add(compounds.get(j).inchiKey);
            }
        }
        if (prioritizedCompounds.size() > numberOfPriorized) {
            for (int i = 0; i < numberOfPriorized; ++i) {
                final LabeledCompound selected = prioritizedCompounds.get(r.nextInt(prioritizedCompounds.size()));
                if (!chosenCompounds.contains(selected.inchiKey) && !blacklist.contains(selected.inchiKey)) {
                    function.apply(selected);
                    chosenCompounds.add(selected.inchiKey);
                }
            }
        }
    }

    private double[] sampleFingerprintVectorPerfectly(LabeledCompound input) {
        return input.fingerprint.asProbabilistic().toProbabilityArray();
    }

    public ProbabilityFingerprint sampleFingerprint(LabeledCompound c, SamplingStrategy strategy) {
        switch (strategy) {
            case INDEPENDENT:
                return fingerprintSampler.sampleIndependently(c.fingerprint, false);
            case INDEPENDENT_DISTURBED:
                return fingerprintSampler.sampleIndependently(c.fingerprint, true);
            case PERFECT:
                return c.fingerprint.asProbabilistic();
            case DISTURBED_TEMPLATE:
                return fingerprintSampler.sample(c.fingerprint, true);
            case TEMPLATE:
                return fingerprintSampler.sample(c.fingerprint, false);
            case CONDITIONAL: return fingerprintSampler.sampleFromCovariance(c.fingerprint);
            case STATISTICAL: return new ProbabilityFingerprint(fingerprintSampler.version, fingerprintSampler.sampleByErrorStats(c.fingerprint, 1));
            case STATISTICAL_DISTURBED:  return new ProbabilityFingerprint(fingerprintSampler.version, fingerprintSampler.sampleByErrorStats(c.fingerprint, 5));
            default: throw new RuntimeException("Unknown strategy: " + String.valueOf(strategy));
        }
    }

    public double[] sampleFingerprintVector(LabeledCompound c, SamplingStrategy strategy) {
        return sampleFingerprint(c, strategy).toProbabilityArray();
    }

    public static float[] getLabelVector(LabeledCompound c) {
        int n = c.label.getFingerprintVersion().size();
        if (INCLUDE_FINGERPRINT) n += c.learnableFp.getFingerprintVersion().size();
        final MaskedFingerprintVersion v = (MaskedFingerprintVersion) c.label.getFingerprintVersion();
        final float[] vec = new float[n];
        Arrays.fill(vec, -1);
        for (FPIter i : c.label.presentFingerprints()) {
            vec[v.getRelativeIndexOf(i.getIndex())] = 1;
        }
        if (INCLUDE_FINGERPRINT) {
            final int OFFSET = v.size();
            for (FPIter i : c.learnableFp.presentFingerprints()) {
                vec[OFFSET + i.getIndex()] = 1;
            }
        }
        return vec;
    }

    public static float[] getNPCLabelVector(LabeledCompound c) {
        final float[] vec = new float[c.npcLabel.getFingerprintVersion().size()];
        Arrays.fill(vec, -1);
        for (FPIter x : c.npcLabel.presentFingerprints()) vec[x.getIndex()] = 1;
        return vec;
    }

    public List<LabeledCompound> balancedSample(final int iterationNum, final int numberPerClass, final int numberTotal) {
        final Random r = new Random();
        final ArrayList<LabeledCompound> subset = new ArrayList<>(numberPerClass*compoundClasses.size());
        final TIntHashSet chosen = new TIntHashSet();
        final HashSet<String> chosenCompounds = new HashSet<>();
        for (CompoundClass klass : compoundClasses.valueCollection()) {
            if (klass.compounds.size() < 10*numberPerClass) {
                final List<LabeledCompound> sel = new ArrayList<>(klass.compounds);
                Collections.shuffle(sel, r);
                int count=0;
                for (LabeledCompound l : sel) {
                    if (!chosenCompounds.contains(l.inchiKey) && !blacklist.contains(l.inchiKey)) {
                        chosenCompounds.add(l.inchiKey);
                        subset.add(l);
                        if (++count >= numberPerClass) break;
                    }
                }
            } else {
                chosen.clear();
                int ntry = 0;
                final int ntotal=numberPerClass*10;
                while (chosen.size() < numberPerClass && ++ntry < ntotal) {
                    final int n = r.nextInt(klass.compounds.size());
                    if (!chosen.contains(n) && !chosenCompounds.contains(klass.compounds.get(n).inchiKey) && !blacklist.contains(klass.compounds.get(n).inchiKey)) {
                        chosen.add(n);
                        chosenCompounds.add(klass.compounds.get(n).inchiKey);
                        subset.add(klass.compounds.get(n));
                    }
                }
            }
        }
        int offset = iterationNum*numberTotal;
        for (int i=0; i < numberTotal; ++i) {
            final int j = (i+offset)%compounds.size();
            if (!chosenCompounds.contains(compounds.get(j).inchiKey) && !blacklist.contains(compounds.get(j).inchiKey)) {
                subset.add(compounds.get(j));
                chosenCompounds.add(compounds.get(j).inchiKey);
            }
        }
        Collections.shuffle(subset, r);
        return subset;
    }

    public void setupEnv(File env) throws IOException {
        final CdkFingerprintVersion v = TrainingData.VERSION;
        {
            final MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(v);
            b.disableAll();

            for (String i : Files.readAllLines(new File(env, "fingerprint_indizes.txt").toPath(), Charset.forName("UTF-8"))) {
                b.enable(Integer.parseInt(i));
            }

            this.fingerprintVersion = b.toMask();
        }
        final HashSet<String> knownKlasses = new HashSet<>();
        final HashSet<String> doubleCheck = new HashSet<>();
        int counter=0;
        for (String line : Files.readAllLines(new File(env, "klasses_with_indizes.csv").toPath(), Charset.forName("UTF-8"))) {
            final String name = line.split("\t")[0];
            final String nname = normalizeName(name);
            knownKlasses.add(name);
            // normalize name!!!
            knownKlasses.add(nname);
            if (!doubleCheck.add(nname))
                System.out.println("Double class with name: " + nname + " and " + name);
            ++counter;
        }
        System.out.println("Number of classes: " + doubleCheck.size() + " ( lines:  " + counter + ")" );

        this.classyFireFingerprintVersion = ClassyFireFingerprintVersion.loadClassyfire(new File(env, "chemont.csv.gz"));

        final double[][] formulaMatrix = new FileUtils().readAsDoubleMatrix(new File("formula_normalized.txt"));
        if (SCALE_BY_MAX) {
            this.formulaNorm = formulaMatrix[0];
            this.formulaScale = formulaMatrix[1];
        }

        {
            final MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(classyFireFingerprintVersion);
            b.disableAll();
            for (int i=0, n=classyFireFingerprintVersion.size(); i < n; ++i) {
                final ClassyfireProperty prop = (ClassyfireProperty)classyFireFingerprintVersion.getMolecularProperty(i);
                if (knownKlasses.contains((prop).getName())) {
                    compoundClasses.put(i, new CompoundClass((short)i, prop));
                    b.enable(i);
                    doubleCheck.remove(((ClassyfireProperty)classyFireFingerprintVersion.getMolecularProperty(i)).getName());
                }
            }
            System.out.println("MISSING:\n" + doubleCheck);
            classyFireMask = b.toMask();
            System.out.println("Number of Labels: " + classyFireMask.size());

            for (CompoundClass prop : compoundClasses.valueCollection()) {
                name2class.put(prop.ontology.getName(), prop);
                name2class.put(unnormalize(prop.ontology.getName()), prop);
            }

            // read all fingerprints
            final HashMap<String, ArrayFingerprint> fingerprints = new HashMap<>();
            final HashMap<String, MolecularFormula> formulas = new HashMap<>();
            final HashMap<String, MolecularFormula> formulaCache = new HashMap<>();
            try (final BufferedReader br = FileUtils.getReader(new File(env, "fingerprints.csv"))) {
                String line;
                while ((line=br.readLine())!=null) {
                    String[] tbs = line.split("\t");
                    final String inchikey = tbs[0];
                    final String formulaString = tbs[1];
                    MolecularFormula formula = formulaCache.get(formulaString);
                    if (formula==null) {
                        formula = MolecularFormula.parseOrThrow(formulaString);
                        formulaCache.put(formulaString, formula);
                    }
                    final short[] indizes = new short[tbs.length-2];
                    for (int j=0; j < indizes.length; ++j)
                        indizes[j] = Short.parseShort(tbs[j+2]);

                    formulas.put(inchikey, formula);
                    fingerprints.put(inchikey, new ArrayFingerprint(fingerprintVersion, indizes));
                }
            }
            final HashMap<String, ArrayFingerprint> trainableFingerprints = new HashMap<>();
            if (INCLUDE_FINGERPRINT) {
                final TIntIntHashMap mapping = new TIntIntHashMap();
                boolean header = false;
                int relIndex = 0;
                final List<MolecularProperty> dummyProperties = new ArrayList<>();
                for (String line : Files.readAllLines(new File(env,"trainable_indizes.csv").toPath(), Charset.forName("UTF-8"))) {
                    if (!header) {
                        header = true;
                        continue;
                    }
                    String[] tab = line.split("\t");
                    int index = Integer.parseInt(tab[0]);
                    mapping.put(index, relIndex);
                    double tp = Double.parseDouble(tab[6]), fp = Double.parseDouble(tab[7]), tn = Double.parseDouble(tab[8]), fn = Double.parseDouble(tab[9]);
                    dummyProperties.add(new DummyMolecularProperty(index, relIndex, new PredictionPerformance(tp,fp,tn,fn, 0)));
                    ++relIndex;
                }
                dummyFingerprintVersion = new CustomFingerprintVersion("DUMMY", dummyProperties);

                for (String line : Files.readAllLines(new File(env,"trainable_fingerprints.csv").toPath(), Charset.forName("UTF-8"))) {
                    String[] tab = line.split("\t");
                    final short[] fp = new short[tab.length-1];
                    final String key = tab[0];
                    for (int k=1; k < tab.length; ++k) {
                        fp[k-1] = (short)mapping.get(Integer.parseInt(tab[k]));
                    }
                    trainableFingerprints.put(key, new ArrayFingerprint(dummyFingerprintVersion, fp));
                }
            }

            final FormulaConstraints constraints = new FormulaConstraints("CHNOPSClBrIFBSeAs");
            final double maximalAllowedMass = 1500d;
            this.fingerprintSampler = new Sampler(fingerprintVersion);
            // read all compounds!!!!
            try (final BufferedReader br = FileUtils.getReader(new File(env, "compounds.csv"))) {
                String line;
                while ((line=br.readLine())!=null) {
                    String[] tbs = line.split("\t");
                    final String inchikey = tbs[0].substring(0,14);
                    final MolecularFormula formula = formulas.get(inchikey);
                    if (formula==null) continue;
                    Ionization neutralIonization = PeriodicTable.getInstance().neutralIonization();
                    if (constraints.isViolated(new ChemicalAlphabet(formula.elementArray())) || formula.getMass() > maximalAllowedMass || formula.numberOfCarbons()<=0 || formula.numberOfHydrogens()<=0)
                        continue;
                    final ArrayFingerprint fp = fingerprints.get(inchikey);
                    if (fp == null) continue;
                    short[] labels = new short[tbs.length-1];
                    int k=0;
                    for (int j=1; j < tbs.length; ++j) {
                        final CompoundClass kl = name2class.get(tbs[j]);
                        if (kl!=null) {
                            labels[k++] = kl.index;
                        }
                    }
                    labels = Arrays.copyOf(labels, k);
                    Arrays.sort(labels);

                    /////////////////////
                    final LabeledCompound compound;
                    compound = new LabeledCompound(inchikey, formula, fp, classyFireMask.mask(new ArrayFingerprint(classyFireMask.getMaskedFingerprintVersion(), labels)), getFormulaFeatures(formula), trainableFingerprints.get(inchikey.substring(0,14)));

                    compounds.add(compound);
                    for (int j=0; j < k; ++j) {
                        compoundClasses.get(labels[j]).compounds.add(compound);
                    }
                }
            }
        }

        Collections.shuffle(compounds);

        // normalize formula features
        if (!SCALE_BY_MAX) {
            scaleFormulaFeatures();
        } else {
            for (LabeledCompound c : compounds) {
                c.formulaFeaturesF = new float[c.formulaFeatures.length];
                for (int i=0; i < c.formulaFeatures.length; ++i) {
                    c.formulaFeatures[i] -= this.formulaNorm[i];
                    c.formulaFeatures[i] /= this.formulaScale[i];
                    c.formulaFeaturesF[i] = (float)c.formulaFeatures[i];
                }
            }
        }
        if (VECNORM_SCALING) {
            for (LabeledCompound c : compounds) {
                double vecnorm = 0d;
                for (int i=0; i < c.formulaFeatures.length; ++i) {
                    vecnorm += c.formulaFeatures[i] * c.formulaFeatures[i];
                }
                vecnorm = Math.sqrt(vecnorm);
                for (int i=0; i < c.formulaFeatures.length; ++i) {
                    c.formulaFeatures[i] /= vecnorm;
                    c.formulaFeaturesF[i] = (float)c.formulaFeatures[i];
                }
            }
        }

        {
            final HashMap<String, LabeledCompound> compoundHashMap = new HashMap<>();
            for (LabeledCompound c : compounds) {
                compoundHashMap.put(c.inchiKey, c);
            }

            if (independentPattern==null) {
                this.independent = null;
                this.crossvalidation = this.fingerprintSampler.readCrossvalidation(new File(env, "prediction_prediction.csv"), compoundHashMap);
            } else {
                this.fingerprintSampler.setExclude(independentPattern);
                this.crossvalidation = this.fingerprintSampler.readCrossvalidation(new File(env, "prediction_prediction.csv"), compoundHashMap);
                final Sampler indepSampler = new Sampler(fingerprintVersion);
                indepSampler.setInclude(independentPattern);
                this.independent = indepSampler.readCrossvalidation(new File(env, "prediction_prediction.csv"), compoundHashMap);
            }

            for (EvaluationInstance i : crossvalidation)
                blacklist.add(i.compound.inchiKey);

            if (independent!=null) {
                for (EvaluationInstance i : independent)
                    blacklist.add(i.compound.inchiKey);
            }


            this.nformulas = formulaNorm.length;
            this.nplatts = fingerprintVersion.size();
            this.nlabels = classyFireMask.size();
            if (INCLUDE_FINGERPRINT)
                this.nlabels += dummyFingerprintVersion.size();

            if (PLATT_CENTERING||SCALE_BY_STD) {
                this.plattNorm = new double[nplatts];
                this.plattScale = new double[nplatts];
                fingerprintSampler.standardize(plattNorm, plattScale);
                if (!PLATT_CENTERING) Arrays.fill(plattNorm, 0d);
                if (!SCALE_BY_STD) Arrays.fill(plattScale, 1d);
            } else {
                this.plattNorm = new double[nplatts];
                this.plattScale = new double[nplatts];
                Arrays.fill(plattNorm, 0d);
                Arrays.fill(plattScale, 1d);

            }


            if (new File("biokeys.txt").exists()) {
                System.out.println("Load biological compounds");
                for (String line : FileUtils.readLines(new File("biokeys.txt"))) {
                    if (compoundHashMap.containsKey(line)) {
                        prioritizedCompounds.add(compoundHashMap.get(line));
                    } else {
                        System.err.println("Do not find bio compound " + line);
                    }
                }
                System.out.println(prioritizedCompounds.size() + " compounds loaded.");
            }


            ///////////////////////
            // NPC
            ///////////////////////
            this.npcList = new ArrayList<>();
            if (new File("npc").exists()) {
                System.out.println("Load NPC data.");
                NPCVersion = NPCFingerprintVersion.readFromDirectory(new File("npc"));
                FileUtils.eachRow(new File("npc/compounds.csv"), (row)->{
                    final LabeledCompound labeledCompound = compoundHashMap.get(row[0]);
                    if (labeledCompound==null) {
                        System.err.println("Do not find compound " + row[0]);
                        return true;
                    }
                    ArrayFingerprint fp = Fingerprint.fromOneZeroString(NPCVersion, row[2]+row[3]+row[4]).asArray();
                    labeledCompound.npcLabel = fp;

                    if (!blacklist.contains(row[0])) {
                        npcList.add(labeledCompound);
                    }

                    return true;
                });
                System.out.println(npcList.size() + " NPC compounds for training");

                for (EvaluationInstance i : crossvalidation) {
                    if (i.compound.npcLabel!=null) {
                        npcInstances.add(i);
                    }
                }
                if (independent!=null) {
                    for (EvaluationInstance i : independent) {
                        if (i.compound.npcLabel != null) {
                            npcInstances.add(i);
                        }
                    }
                }

            }
        }

        // check if we have train data for all compound classes
        for (int index : classyFireMask.allowedIndizes()) {
            ClassyfireProperty molecularProperty = (ClassyfireProperty) classyFireMask.getMolecularProperty(index);
            if (compoundClasses.containsKey(index)) {
                int size = compoundClasses.get(index).compounds.size();
                if (size < 300) {
                    System.err.println("We have less than " + size + " training examples for " + molecularProperty.getName());
                }
            } else {
                System.err.println("Inconsistency with " + molecularProperty.getName() + " which is part of the fingerprint but not part of the hash map");
            }
        }

        // build covariance tree
        if (new File("treeWithCovariance.tree").exists()) {
            System.out.println("Build tree with covariance");
            this.fingerprintSampler.buildCovarianceTree(new File("treeWithCovariance.tree"));
        }

    }

    private ArrayFingerprint addFingerprintsAsLabels(ArrayFingerprint fpIters) {
        return null;
    }

    private void scaleFormulaFeatures() {
        final int n = compounds.get(0).formulaFeatures.length;
        this.formulaNorm = new double[n];
        this.formulaScale = new double[n];
        for (LabeledCompound c : compounds) {
            final double[] lb = c.formulaFeatures;
            for (int i=0; i < lb.length; ++i) {
                formulaNorm[i] += lb[i];
            }
        }
        for (int i=0; i < formulaNorm.length; ++i)
            formulaNorm[i] /= compounds.size();
        for (LabeledCompound c : compounds) {
            final double[] lb = c.formulaFeatures;
            for (int i=0; i < lb.length; ++i) {
                lb[i] -= formulaNorm[i];
                formulaScale[i] += (lb[i]*lb[i]);
            }
        }
        for (int i=0; i < formulaNorm.length; ++i)
            formulaScale[i] = Math.sqrt(formulaScale[i]/compounds.size());
        for (LabeledCompound c : compounds) {
            final double[] lb = c.formulaFeatures;
            for (int i=0; i < lb.length; ++i) {
                lb[i] /= formulaScale[i];
            }
            final float[] fs = new float[c.formulaFeatures.length];
            for (int i=0; i < lb.length; ++i) {
                fs[i] = (float)lb[i];
            }
            c.formulaFeaturesF = fs;
        }
    }

    private String normalizeName(String name) {
        return name.replaceAll("&#39;", "'").replaceAll("&gt;",">");
    }

    private String unnormalize(String name) {
        return name.replaceAll("'", "&#39;").replaceAll(">","&gt;");
    }


    public ArrayFingerprint integrateCanopusFingerprint(ArrayFingerprint origFp, ArrayFingerprint origLabel) {
        final short[] a = origFp.toIndizesArray();
        final TShortArrayList fingerprints = new TShortArrayList(a);
        for (FPIter lab : origLabel.presentFingerprints()) {
            final int i = canopusFingerprintMapping.get(lab.getIndex());
            if (i >= 0) fingerprints.add((short)i);
        }
        fingerprints.sort(a.length, fingerprints.size());
        return new ArrayFingerprint(canopusFingerprint, fingerprints.toArray());
    }

    protected TIntIntHashMap canopusFingerprintMapping;

    public double[] getFormulaFeatures(MolecularFormula f) {
        return Canopus.getFormulaFeatures(f);
    }
}
