/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.noise;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.NoiseInformation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.list.array.TFloatArrayList;

import java.util.Iterator;

public class Ms2NoiseStatistics {

    private TFloatArrayList allNoiseIntensitites, signalIntensities;
    private TFloatArrayList meanPerSpectrum, medianPerSpectrum;

    private MassToFormulaDecomposer decomposer;
    private FormulaConstraints constraints;
    private Deviation ms2Dev;
    private PrecursorIonType ionType;

    private TFloatArrayList buffer, buffer2;

    private NoiseInformation noiseInformation;

    public Ms2NoiseStatistics() {
        this.signalIntensities = new TFloatArrayList();
        this.allNoiseIntensitites = new TFloatArrayList();
        this.meanPerSpectrum = new TFloatArrayList();
        this.medianPerSpectrum = new TFloatArrayList();
        this.buffer = new TFloatArrayList();
        this.buffer2 = new TFloatArrayList();

        this.decomposer = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPSNa").elementArray()));
        this.constraints = new FormulaConstraints("CHNOPSNa[0-1]");
        this.ms2Dev = new Deviation(15);
        ionType = PrecursorIonType.getPrecursorIonType("[M+H]+");
    }

    // TODO: prüfen ob die Spektren wenig Peaks enthalten. Dann wurden die nämlich schon entnoised.
    public void add(Scan scan, SimpleSpectrum spectrum) {
        final double right = scan.getPrecursor().getMass() + 20;
        buffer.clearQuick();
        buffer2.clearQuick();
        final double samplingChance = 100d/spectrum.size();
//        System.out.println(samplingChance);
        for (int k=0; k < spectrum.size(); ++k) {
            if (samplingChance<1 && Math.random()>samplingChance)
                continue;
            final double mz = spectrum.getMzAt(k);
            final double intensity = spectrum.getIntensityAt(k);
            if (intensity<=0)
                continue;
            if (mz > right || noDecompositionFor(mz)) {
                allNoiseIntensitites.add((float)intensity);
                buffer.add((float)intensity);
            } else buffer2.add((float)intensity);
        }
        if (buffer.size()>=10) {
            buffer.sort();
            meanPerSpectrum.add(buffer.sum()/buffer.size());
            medianPerSpectrum.add(buffer.getQuick(buffer.size()/2));
        }
        if (buffer2.size()>=5) {
            buffer2.sort();
            int tk = (int)Math.ceil(15*samplingChance);
            signalIntensities.add(buffer2.getQuick(Math.max(0, buffer2.size() - tk)));
        }
    }

    public NoiseInformation done() {
        if (allNoiseIntensitites.size()==0) {
            this.noiseInformation = new NoiseInformation(0d,0d,0d,1d,null);
            return this.noiseInformation;
        }
        final float[] array = allNoiseIntensitites.toArray();
        signalIntensities.sort();
        double signalLevel = signalIntensities.getQuick((int)(signalIntensities.size()*0.15));
        double x = 0.5;
        double noiselevel = Double.POSITIVE_INFINITY;
        while (x >= 0.1 && (signalLevel < 3*noiselevel) ) {
            noiselevel = Quickselect.quickselectInplace(array, 0, array.length, (int) (array.length * x));
            x -= 0.1;
        }
        if (signalLevel < 2*noiselevel) {
            noiselevel = signalLevel/2d;
        }

        this.noiseInformation = new NoiseInformation(
                allNoiseIntensitites.sum()/allNoiseIntensitites.size(),
                noiselevel, noiselevel, signalLevel, ParetoDistribution.learnFromData((float)Quickselect.quickselectInplace(array, 0,array.length, (int)(array.length*0.15)), array)

        );
        return noiseInformation;
    }

    public GlobalNoiseModel getGlobalNoiseModel() {
        if (noiseInformation==null) done();
        return new GlobalNoiseModel(noiseInformation.getNoiseLevel(), noiseInformation.getSignalLevel());
    }



    private boolean noDecompositionFor(double mz) {
        final Iterator<MolecularFormula> fiter = decomposer.formulaIterator(mz, ionType.getIonization(), ms2Dev, constraints);
        while (fiter.hasNext()) {
            if (fiter.next().rdbe()>=-0.5) {
                return false;
            }
        }
        return true;
    }
}