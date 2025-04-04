/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.decomp;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.MassDecomposer.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.MS2ExpInputIterator;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DecompWorkflow implements Workflow {
    DecompOptions options;
    InputFilesOptions input;

    public DecompWorkflow(DecompOptions options, InputFilesOptions input) {
        this.options = options;
        this.input = input;
    }

    @Override
    public void run() {
        DecompositionValidator<Element> validator;
        if ((options.level != FilterLevel.COMMON && options.level != FilterLevel.NONE) && options.noRDBE) {
            LoggerFactory.getLogger(getClass()).error("Conflicting options: --nofilter and --filter='" + options.level + "'. Only one of both must be set.");
            return;
        } else {
            switch (options.level) {
                case STRICT:
                    validator = ChemicalValidator.getStrictThreshold();
                    break;
                case COMMON:
                    validator = options.noRDBE ? null : ChemicalValidator.getCommonThreshold();
                    break;
                case PERMISSIVE:
                    validator = ChemicalValidator.getPermissiveThreshold();
                    break;
                case RDBE:
                    validator = new ValenceValidator<>();
                    break;
                default:
                    validator = null;
            }
        }

        if (validator == null) {
            // do nothing
        } else if (validator instanceof ChemicalValidator) {
            final ChemicalValidator c = (ChemicalValidator) validator;
            validator = new ChemicalValidator(c.getRdbeThreshold(), c.getRdbeLowerbound() + 0.5d, c.getHeteroToCarbonThreshold(), c.getHydrogenToCarbonThreshold());
        } else {
            validator = new ValenceValidator<>(-0.5d);
        }

        final Deviation dev = new Deviation(options.ppm, options.absDeviation);
        final ChemicalAlphabet alphabet = options.alphabet.getAlphabet();
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(alphabet);

        // parse input massed and collect DecompInstanceJobs
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        final List<DecompInstanceJob> decompJobs = new ArrayList<>();
        final boolean inputFromSpectrumFiles;
        if(input != null && input.msInput != null && input.msInput.msParserfiles != null){
            final Collection<InputResource<?>> inputResources = input.msInput.msParserfiles.keySet().stream().map(PathInputResource::new).collect(Collectors.toList());
            final double maxMz = options.maxMz == null ? Double.POSITIVE_INFINITY : options.maxMz;

            try(final MS2ExpInputIterator it = new MS2ExpInputIterator(inputResources, maxMz, false, false)){
                while(it.hasNext()){
                    final Ms2Experiment exp = it.next();
                    final String featureId = Optional.ofNullable(exp.getFeatureId()).orElse(exp.getName());
                    final MolecularFormula parentFormula = exp.getMolecularFormula();
                    final PrecursorIonType ionization = exp.getPrecursorIonType();
                    final List<Ms2Spectrum<Peak>> ms2Spectra = exp.getMs2Spectra();

                    for(int i = 0; i < ms2Spectra.size(); i++){
                        final Ms2Spectrum<Peak> spec = ms2Spectra.get(i);

                        final List<Double> masses = new ArrayList<>(spec.size());
                        final List<Double> intensities = new ArrayList<>(spec.size());
                        spec.forEach(p -> {
                            masses.add(p.getMass());
                            intensities.add(p.getIntensity());
                        });

                        decompJobs.add(new DecompInstanceJob(featureId, i, parentFormula, ionization, masses, intensities, alphabet, dev, decomposer, validator));
                    }
                }
            }
            inputFromSpectrumFiles = true;
        }else{
            final MolecularFormula parentFormula = options.parentFormula != null ? MolecularFormula.parseOrNull(options.parentFormula) : null;
            final PrecursorIonType ionization = options.ionization == null ? null : PeriodicTable.getInstance().ionByNameOrNull(options.ionization);

            List<Double> masses = options.masses != null ? Arrays.stream(options.masses).boxed().collect(Collectors.toList()) : new ArrayList<>();
            if (input != null && input.msInput != null && input.msInput.unknownFiles != null) {
                for (Path path : input.msInput.unknownFiles.keySet().stream().sorted().collect(Collectors.toList())) {
                    try {
                        masses.addAll(Files.readAllLines(path).stream().map(Double::valueOf).collect(Collectors.toList()));
                    } catch (IOException e) {
                        LoggerFactory.getLogger(getClass()).error("Error when parsing masses from input file: '" + path.toString() + "'. Skipping this file!", e);
                    }
                }
            }

            decompJobs.add(new DecompInstanceJob(null, 0, parentFormula, ionization, masses, null, alphabet, dev, decomposer, validator));
            inputFromSpectrumFiles = false;
        }

        LoggerFactory.getLogger(getClass()).info("Number of decomposition jobs:\t{}", decompJobs.size());

        // Now, all jobs are collect. Run them and take their results:
        decompJobs.forEach(jobManager::submitJob);
        final List<DecompResult> results = decompJobs.stream().map(JJob::takeResult).toList();

        final boolean printErrors = options.massErrors;
        try (Writer ow = options.out != null ? Files.newBufferedWriter(options.out) : new OutputStreamWriter(System.out)) {
            final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
            formater.applyPattern("#.####");

            //write header
            if(inputFromSpectrumFiles)
                ow.write("featureId\tspectrumId\t");
            ow.write("parentFormula\tadduct\tm/z");
            if(inputFromSpectrumFiles)
                ow.write("\tintensity");
            ow.write("\tdecompositions");
            if (printErrors)
                ow.write("\tabsMassDev\trelMassDev");
            ow.write(System.lineSeparator());

            for(final DecompResult result : results)
                this.writeDecompResult(ow, formater, result, printErrors, inputFromSpectrumFiles);

        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("could not write output! Canceling...", e);
        }
    }

    private void writeDecompResult(Writer ow, DecimalFormat formater, DecompResult result, boolean printErrors, boolean inputFromSpectrumFiles) throws IOException {
        final String featureId = String.valueOf(result.id());
        final String spectrumId = Integer.toString(result.spectrumId());
        final String parentFormula = String.valueOf(result.parentFormula());
        final String ionization = String.valueOf(result.ionization());
        final List<Double> masses = result.masses();
        final List<List<MolecularFormula>> formulas = result.formulas();

        for(int i = 0; i < masses.size(); i++){
            final double mass = masses.get(i);
            final List<MolecularFormula> mfs = formulas.get(i);

            if(inputFromSpectrumFiles) {
                ow.write(featureId);
                ow.write("\t");
                ow.write(spectrumId);
                ow.write("\t");
            }
            ow.write(parentFormula);
            ow.write("\t");
            ow.write(ionization);
            ow.write("\t");
            ow.write(formater.format(mass));

            if(inputFromSpectrumFiles){
                final double intensity = result.intensities().get(i);
                ow.write("\t");
                ow.write(formater.format(intensity));
            }

            ow.write("\t");
            ow.write(mfs.stream().map(MolecularFormula::toString).collect(Collectors.joining(",")));
            if(printErrors){
                ow.write("\t");
                ow.write(mfs.stream().map(f -> formater.format(mass - f.getMass())).collect(Collectors.joining(",")));
                ow.write("\t");
                ow.write(mfs.stream().map(f -> formater.format((mass - f.getMass()) / mass * 1e6)).collect(Collectors.joining(",")));
            }
            ow.write(System.lineSeparator());
        }
    }

    private class DecompInstanceJob extends BasicJJob<DecompResult>{

        private final String instanceId;
        private final int spectrumId;

        private final MolecularFormula parentFormula;
        private final PrecursorIonType ionization;

        private final List<Double> masses;
        private final List<Double> intensities;

        private final DecompositionValidator<Element> validator;
        private final ChemicalAlphabet alphabet;
        private final Deviation deviation;
        private final MassToFormulaDecomposer decomposer;

        public DecompInstanceJob(String instanceId, int spectrumId, MolecularFormula parentFormula, PrecursorIonType ionization, List<Double> masses, List<Double> intensities, ChemicalAlphabet alphabet, Deviation deviation, MassToFormulaDecomposer decomposer, DecompositionValidator<Element> validator) {
            this.instanceId = instanceId;
            this.spectrumId = spectrumId;

            this.parentFormula = parentFormula;
            this.ionization = ionization;

            this.masses = masses;
            this.intensities = intensities;

            this.alphabet = alphabet;
            this.deviation = deviation;
            this.decomposer = decomposer;
            this.validator = validator;
        }

        @Override
        protected DecompResult compute() throws Exception {
            // 1. Set up the boundaries for the sub-formulas
            final Map<Element, Interval> boundary = options.alphabet.getBoundary();
            if(this.parentFormula != null){
                for (Element e : this.alphabet.getElements()) {
                    Interval i = boundary.get(e);
                    if (i == null) {
                        i = new Interval(0, this.parentFormula.numberOf(e));
                        boundary.put(e, i);
                    } else {
                        boundary.put(e, new Interval(i.getMin(), Math.min(i.getMax(), this.parentFormula.numberOf(e))));
                    }
                }
            }

            // 2. Decompose the masses in this.masses:
            final List<List<MolecularFormula>> formulas = new ArrayList<>(this.masses.size());
            for(int i = 0; i < this.masses.size(); i++){
                final double mz = this.masses.get(i);
                final double mass = this.ionization != null ? this.ionization.precursorMassToNeutralMass(mz) : mz;

                final List<int[]> compomers;
                if (options.maxDecomps == null || options.maxDecomps <= 0) {
                    compomers = this.decomposer.decompose(mass, this.deviation, boundary);
                } else {
                    compomers = new ArrayList<>(options.maxDecomps);
                    int count = options.maxDecomps;
                    DecompIterator<Element> it = this.decomposer.decomposeIterator(mass, this.deviation, boundary);
                    while (it.next() && count > 0) {
                        compomers.add(it.getCurrentCompomere().clone());
                        count--;
                    }
                }

                final List<MolecularFormula> mfs = new ArrayList<>(compomers.size());
                for (int[] c : compomers) {
                    if (this.validator == null || this.validator.validate(c, this.decomposer.getOrderedCharacterIds(), this.decomposer.getAlphabet()))
                        mfs.add(alphabet.decompositionToFormula(c));
                }
                mfs.sort(Comparator.comparingDouble(o -> Math.abs(o.getMass() - mass)));
                formulas.add(mfs);
            }

            return new DecompResult(this.instanceId, this.spectrumId, this.parentFormula, this.ionization, this.masses, this.intensities, formulas);
        }
    }

    private record DecompResult(String id, int spectrumId, MolecularFormula parentFormula, PrecursorIonType ionization, List<Double> masses, List<Double> intensities, List<List<MolecularFormula>> formulas) {}
}
