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

package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InvalidException;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.Warning;
import de.unijena.bioinf.myxo.structure.*;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElementConverter;

import java.util.List;

/**
 * Convert Sirius Data structures to Myxobase data structures
 *
 * @Marvin: Please do not write your own parsers! It is really confusing if for some data types the sirius commandline
 *          tool behaves differently than the sirius gui.
 */
public class SiriusDataConverter {

    public static Ms2Experiment validateInput(Ms2Experiment input) {
        final MissingValueValidator validator = new MissingValueValidator();
        try {
            return validator.validate(input, new Warning.Noop(), true);
        } catch (InvalidException e) {
            return input;
        }
    }

    public static ExperimentContainer siriusExperimentToExperimentContainer(Ms2Experiment sirius) {
        sirius = validateInput(sirius);
        final ExperimentContainer c = new ExperimentContainer();
        c.setDataFocusedMass(sirius.getIonMass());
        c.setName(sirius.getName());
        c.setIonization(siriusIonizationToEnum(sirius.getPrecursorIonType()));
        for (Spectrum<Peak> s : sirius.getMs1Spectra()) {
            c.getMs1Spectra().add(siriusSpectrumToMyxoSpectrum(s));
        }
        for (Spectrum<Peak> s : sirius.getMs2Spectra()) {
            c.getMs2Spectra().add(siriusSpectrumToMyxoSpectrum(s));
        }
        return c;
    }

    public static de.unijena.bioinf.sirius.gui.mainframe.Ionization siriusIonizationToEnum(PrecursorIonType ion) {
        final PeriodicTable table = PeriodicTable.getInstance();
        if (ion.isIonizationUnknown()) return Ionization.Unknown;
        if (ion.equals(table.ionByName("[M+H]+"))) {
            return Ionization.MPlusH;
        } else if (ion.equals(table.ionByName("[M]+"))) {
            return Ionization.M;
        } else if (ion.equals(table.ionByName("[M+Na]+"))) {
            return Ionization.MPlusNa;
        } else if (ion.equals(table.ionByName("[M-H]-"))) {
            return Ionization.MMinusH;
        } else return Ionization.Unknown; // -_-
    }

    public static PrecursorIonType enumToSiriusIonization(de.unijena.bioinf.sirius.gui.mainframe.Ionization aenum)  {
        final PeriodicTable pt = PeriodicTable.getInstance();
        switch (aenum) {
            case Unknown: return pt.ionByName("[M+?]+");
            case MMinusH: return pt.ionByName("[M-H]-");
            case MPlusH: return pt.ionByName("[M+H]+");
            case M: return pt.ionByName("[M]+");
            case MPlusNa: return pt.ionByName("[M+Na]+");
            default: return pt.ionByName("[M+?]+");

        }
    }

    public static CompactExperiment siriusExperimentToCompactExperiment(Ms2Experiment sirius) {
        sirius = validateInput(sirius);
        final DefaultCompactExperiment exp = new DefaultCompactExperiment();
        exp.setMolecularFormula(sirius.getMolecularFormula());
        exp.setCompoundName(sirius.getName());
        exp.setFocusedMass(sirius.getIonMass());
        exp.setIonization(sirius.getPrecursorIonType().toString());
        exp.setMS1Spectrum(siriusSpectrumToMyxoSpectrum(sirius.getMergedMs1Spectrum()));
        for (Ms2Spectrum<? extends Peak> spec : sirius.getMs2Spectra()) {
            exp.addMS2Spectrum(siriusSpectrumToMyxoSpectrum(spec));
        }
        return exp;
    }

    public static MutableMs2Experiment experimentContainerToSiriusExperiment(ExperimentContainer myxo) {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setName(myxo.getName());
        exp.setIonMass(myxo.getDataFocusedMass());
        exp.setPrecursorIonType(enumToSiriusIonization(myxo.getIonization()));
        for (CompactSpectrum cs : myxo.getMs1Spectra()) {
            exp.getMs1Spectra().add(myxoMs1ToSiriusMs1(cs));
        }
        for (CompactSpectrum cs : myxo.getMs2Spectra()) {
            exp.getMs2Spectra().add(myxoMs2ToSiriusMs2(cs, myxo.getDataFocusedMass()));
        }
        return exp;
    }

    public static SiriusResultElement siriusResultToMyxoResult(IdentificationResult ir) {
        return SiriusResultElementConverter.convertResult(ir);
    }

    public static ExperimentContainer siriusToMyxoContainer(Ms2Experiment experiment, List<IdentificationResult> results) {
        final ExperimentContainer c = siriusExperimentToExperimentContainer(experiment);
        c.setRawResults(results);
        if (results.size()>0) {
            final FTree tree = results.get(0).getTree();
            if (tree!=null) {
                final Precursor parentmass = tree.getAnnotationOrNull(Precursor.class);
                if (parentmass!=null) c.setSelectedFocusedMass(parentmass.getPrecursorMass());
            }
        }
        return c;
    }

    public static CompactSpectrum siriusSpectrumToMyxoSpectrum(Spectrum<? extends Peak> spec) {
        final CompactSpectrum cs = new DefaultCompactSpectrum(Spectrums.copyMasses(spec), Spectrums.copyIntensities(spec));
        if (spec instanceof Ms2Spectrum) {
            final Ms2Spectrum<? extends Peak> ms2Spec = (Ms2Spectrum<? extends Peak>)spec;
            cs.setCollisionEnergy(ms2Spec.getCollisionEnergy());
            cs.setMSLevel(ms2Spec.getMsLevel());
        } else {
            cs.setMSLevel(1);
            cs.setCollisionEnergy(null);
        }
        return cs;
    }

    public static MutableMs2Spectrum myxoMs2ToSiriusMs2(CompactSpectrum cs, double parentMass) {
        return new MutableMs2Spectrum(myxoMs1ToSiriusMs1(cs), parentMass, cs.getCollisionEnergy(), 2);
    }

    public static SimpleSpectrum myxoMs1ToSiriusMs1(CompactSpectrum cs) {
        final SimpleMutableSpectrum ms = new SimpleMutableSpectrum(cs.getSize());
        for (int i=0; i < cs.getSize(); ++i) {
            final CompactPeak cp = cs.getPeak(i);
            ms.addPeak(cp.getMass(), cp.getAbsoluteIntensity());
        }
        return new SimpleSpectrum(ms);
    }

}
