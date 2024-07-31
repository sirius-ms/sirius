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

package de.unijena.bioinf.babelms.mzml;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.ionidentity.AdductResolver;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.Feature;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.sirius.validation.Ms2Validator;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

public abstract class AbstractMzParser implements Parser<Ms2Experiment> {
    protected InMemoryStorage inMemoryStorage;
    protected Iterator<FragmentedIon> ions;
    protected ProcessedSample sample;
    protected LCMSProccessingInstance instance;
    protected int counter = 0;

    protected abstract boolean setNewSource(Object sourceReaderOrStream, URI source) throws IOException;

    protected abstract LCMSRun parseToLCMSRun() throws IOException;

    @Override
    public Ms2Experiment parse(InputStream inputStream, URI source) throws IOException {
        return parseTypeLess(inputStream, source);
    }

    @Override
    public Ms2Experiment parse(BufferedReader sourceReader, URI source) throws IOException {
        return parseTypeLess(sourceReader, source);
    }

    private Ms2Experiment parseTypeLess(Object sourceReaderOrStream, URI source) throws IOException {
        try {
            if (setNewSource(sourceReaderOrStream, source)) {
                instance = new LCMSProccessingInstance();
                inMemoryStorage = new InMemoryStorage();
                final LCMSRun run = parseToLCMSRun();
                sample = instance.addSample(run, inMemoryStorage);
                instance.detectFeatures(sample);

                ions = sample.ions.stream().filter(i -> i != null && Math.abs(i.getChargeState()) <= 1
                        // TODO: kaidu: maybe we can add some parameter for that? But Marcus SpectralQuality is not flexible enough for this
                        && i.getMsMsQuality().betterThan(Quality.BAD)).iterator();
            }
            if (ions.hasNext()) {
                final FragmentedIon ion = ions.next();
                AdductResolver.resolve(instance, ion);
                Feature feature = instance.makeFeature(sample, ion, false);
                String featureId = String.valueOf(++counter);
                MutableMs2Experiment experiment = feature.toMsExperiment(featureId, featureId).mutate();
                new Ms2Validator().validate(experiment, Warning.Logger, true);
                return experiment;
            } else {
                instance = null;
                inMemoryStorage = null;
                sample = null;
                ions = null;
                return null;
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(AbstractMzParser.class).error("Error while parsing " + source + ": " + e.getMessage());
            if (e instanceof InvalidInputData) {
                return null;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e);
            }
        }
    }
}
