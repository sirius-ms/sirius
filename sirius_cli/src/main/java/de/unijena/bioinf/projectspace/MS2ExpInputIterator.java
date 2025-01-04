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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.CandidateFormulas;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.ms.InputFileConfig;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressInputStream;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.validation.Ms1Validator;
import de.unijena.bioinf.sirius.validation.Ms2Validator;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * File based input Iterator that allows to iterate over the {@see de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment}s parsed from
 * multiple files (also different types) that are supported by the {@see de.unijena.bioinf.babelms.MsExperimentParser}.
 */
public class MS2ExpInputIterator implements InstIterProvider, CloseableIterator<Ms2Experiment> {
    private static final Logger LOG = LoggerFactory.getLogger(MS2ExpInputIterator.class);
    private final ArrayDeque<Ms2Experiment> instances = new ArrayDeque<>();
    private final Iterator<InputResource<?>> resourceIter;
    private final Predicate<Ms2Experiment> filter;
    private final MsExperimentParser parser = new MsExperimentParser();
    private final boolean ignoreFormula;
    private final boolean allowMS1Only;

    @Nullable
    private final JobProgressMerger progress;

    InputResource<?> currentResource;
    CloseableIterator<Ms2Experiment> currentExperimentIterator;

    public MS2ExpInputIterator(Collection<InputResource<?>> input, double maxMz, boolean ignoreFormula, boolean allowMS1Only) {
        this(input, (exp) -> exp.getIonMass() <= maxMz, ignoreFormula, allowMS1Only, null);
    }

    public MS2ExpInputIterator(Collection<InputResource<?>> input, Predicate<Ms2Experiment> filter, boolean ignoreFormula, boolean allowMS1Only, @Nullable JobProgressMerger progress) {
        this.progress = progress;
        this.resourceIter = input.iterator();
        this.filter = filter;
        this.ignoreFormula = ignoreFormula;
        this.allowMS1Only = allowMS1Only;
        fetchNext();
    }

    @Override
    public boolean hasNext() {
        return !instances.isEmpty();
    }

    @Override
    public Ms2Experiment next() {
        fetchNext();
        return instances.poll();
    }

    private void fetchNext() {
        while (true) {
            if (currentExperimentIterator == null || !currentExperimentIterator.hasNext()) {
                //close old iterator
                if (currentExperimentIterator != null) {
                    try {
                        currentExperimentIterator.close();
                    } catch (IOException e) {
                        LoggerFactory.getLogger(getClass()).warn("Error when closing Parsing Stream Iterator of: " + currentResource.getFilename());
                    }
                }

                if (resourceIter.hasNext()) {
                    currentResource = resourceIter.next();
                    try {
                        GenericParser<Ms2Experiment> p = parser.getParser(currentResource.getFilename());
                        if (p == null) {
                            LOG.error("Unknown file format: '" + currentResource + "'");
                        } else {
                            if (progress == null) {
                                currentExperimentIterator = p.parseIterator(currentResource.getInputStream(), currentResource.toUri());
                            } else {
                                ProgressInputStream s = new ProgressInputStream(currentResource.getInputStream());
                                s.addPropertyChangeListener(progress);
                                currentExperimentIterator = p.parseIterator(s, currentResource.toUri());
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(new Exception("Cannot parse file '" + currentResource.getFilename() + "':\n" + e.getMessage(), e));  // Nesting because JJob will unwrap the cause
                    }
                } else {
                    return;
                }
            } else {
                try {
                    MutableMs2Experiment experiment = Sirius.makeMutable(currentExperimentIterator.next());

                    if (experiment.getPrecursorIonType() == null) {
                        LOG.warn("No ion or charge given for: {} Try guessing charge from name.", experiment.getName());
                        final String name = (Optional.ofNullable(experiment.getName()).orElse("") +
                                "_" + Optional.ofNullable(experiment.getSourceString()).orElse("")).toLowerCase();

                        if ((name.contains("negative") || name.contains("neg")) && (!name.contains("positive") && !name.contains("pos"))) {
                            LOG.info("{}: Negative charge keyword found!", experiment.getName());
                            experiment.setPrecursorIonType(PrecursorIonType.unknownNegative());
                        } else {
                            LOG.info("{}: Falling back to positive", experiment.getName());
                            experiment.setPrecursorIonType(PrecursorIonType.unknownPositive());
                        }
                    }

                    if (experiment.getMs1Spectra().removeIf(Spectrum::isEmpty))
                        LoggerFactory.getLogger(getClass()).warn("Removed at least one empty MS1 spectrum from '{}'.", experiment.getName());
                    if (experiment.getMs2Spectra().removeIf(Spectrum::isEmpty))
                        LoggerFactory.getLogger(getClass()).warn("Removed at least one empty MS/MS spectrum from '{}'.", experiment.getName());

                    if (!allowMS1Only && experiment.getMs2Spectra().isEmpty()) {
                        LOG.info("Skipping instance '{}' because it does not contain any non Empty MS/MS.", experiment.getName());
                    } else if (!filter.test(experiment)) {
                        LOG.info("Skipping instance '{}' because it did not pass the filter setting.", experiment.getName());
                    } else if (experiment.getMolecularFormula() != null && experiment.getMolecularFormula().numberOf("D") > 0) {
                        LOG.warn("Deuterium Formula found in: {} Instance will be Ignored.", experiment.getName());
                    } else {
                        if (ignoreFormula) {
                            experiment.setMolecularFormula(null);
                            experiment.removeAnnotation(InChI.class);
                            experiment.removeAnnotation(Smiles.class);
                            experiment.removeAnnotation(CandidateFormulas.class);
                            experiment.getAnnotation(InputFileConfig.class).ifPresent(config -> config.config().changeConfig("CandidateFormulas", null));
                        }
                        if (experiment.getMs2Spectra().isEmpty()) {
                            new Ms1Validator().validate(experiment, Warning.Logger, true);
                        } else {
                            new Ms2Validator().validate(experiment, Warning.Logger, true);
                        }
                        instances.add(experiment);
                        return;
                    }
                } catch (Exception e) {
                    LOG.error("Error while parsing compound! Skipping entry", e);
                }
            }
        }
    }

    @Override
    public void close() {
        if (currentExperimentIterator != null) {
            try {
                currentExperimentIterator.close();
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when closing Parsing Stream Iterator of: " + currentResource.getFilename());
            }
        }
    }
}