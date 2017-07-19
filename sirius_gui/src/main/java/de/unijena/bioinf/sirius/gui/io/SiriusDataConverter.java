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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.MissingValueValidator;
import de.unijena.bioinf.myxo.structure.*;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElementConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert Sirius Data structures to Myxobase data structures
 */
public class SiriusDataConverter {

    /**
     * add missing data (if possible) to display it properly in gui .
     * @param input
     * @return
     */
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
        c.setSource(sirius.getSource());
        c.setIonization(sirius.getPrecursorIonType()==null ? PrecursorIonType.getPrecursorIonType("[M+H]+") : sirius.getPrecursorIonType());
        for (Spectrum<Peak> s : sirius.getMs1Spectra()) {
            c.getMs1Spectra().add(siriusSpectrumToMyxoSpectrum(s));
        }
        for (Spectrum<Peak> s : sirius.getMs2Spectra()) {
            c.getMs2Spectra().add(siriusSpectrumToMyxoSpectrum(s));
        }
        return c;
    }

    public static de.unijena.bioinf.sirius.gui.mainframe.Ionization siriusIonizationToEnum(PrecursorIonType ion) {
        return Ionization.fromSirius(ion);
    }

    public static PrecursorIonType enumToSiriusIonization(de.unijena.bioinf.sirius.gui.mainframe.Ionization aenum)  {
        return aenum.toRealIonization();
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

    public static MutableMs2Experiment experimentContainerToSiriusExperiment(ExperimentContainer myxo, PrecursorIonType ionType, double ionMass) {
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setName(myxo.getName());
        exp.setSource(myxo.getSource());
        exp.setIonMass(ionMass);
        exp.setPrecursorIonType(ionType);
        for (CompactSpectrum cs : myxo.getMs1Spectra()) {
            exp.getMs1Spectra().add(myxoMs1ToSiriusMs1(cs));
        }
        if (myxo.getMs1Spectra().size()>0) exp.setMergedMs1Spectrum(Spectrums.mergeSpectra(exp.getMs1Spectra()));
        for (CompactSpectrum cs : myxo.getMs2Spectra()) {
            exp.getMs2Spectra().add(myxoMs2ToSiriusMs2(cs, myxo.getDataFocusedMass()));
        }
        return exp;
    }

    public static MutableMs2Experiment experimentContainerToSiriusExperiment(ExperimentContainer myxo) {
        return experimentContainerToSiriusExperiment(myxo, myxo.getIonization(), myxo.getFocusedMass());
    }

    public static SiriusResultElement siriusResultToMyxoResult(IdentificationResult ir) {
        return SiriusResultElementConverter.convertResult(ir);
    }

    public static ExperimentContainer siriusToMyxoContainer(Ms2Experiment experiment, List<IdentificationResult> results) {
        if (experiment==null) throw new NullPointerException();
        final ExperimentContainer c = siriusExperimentToExperimentContainer(experiment);
        if (results.size()>0) {
            c.setRawResults(results);
            final FTree tree = results.get(0).getRawTree();
            final FragmentAnnotation<AnnotatedPeak> pa = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            if (tree!=null) {
                double parentmass;
                if (pa.get(tree.getRoot())!=null) parentmass = pa.get(tree.getRoot()).getMass();
                else if (tree.getAnnotationOrNull(PrecursorIonType.class)!=null) {
                    parentmass = tree.getAnnotationOrNull(PrecursorIonType.class).addIonAndAdduct(tree.getRoot().getFormula().getMass());
                } else parentmass = -1d;

                c.setSelectedFocusedMass(parentmass);
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
        return new MutableMs2Spectrum(myxoMs1ToSiriusMs1(cs), parentMass, cs.getCollisionEnergy() == null ? CollisionEnergy.none() : cs.getCollisionEnergy(), 2);
    }

    public static List<MutableMs2Spectrum> myxoMs2ToSiriusMs2(List<CompactSpectrum> cs, double parentMass) {
        final List<MutableMs2Spectrum> spectra = new ArrayList<>(cs.size());
        for (CompactSpectrum c : cs) spectra.add(myxoMs2ToSiriusMs2(c, parentMass));
        return spectra;
    }

    public static SimpleSpectrum myxoMs1ToSiriusMs1(CompactSpectrum cs) {
        final SimpleMutableSpectrum ms = new SimpleMutableSpectrum(cs.getSize());
        for (int i=0; i < cs.getSize(); ++i) {
            final CompactPeak cp = cs.getPeak(i);
            ms.addPeak(cp.getMass(), cp.getAbsoluteIntensity());
        }
        return new SimpleSpectrum(ms);
    }

    public static SimpleSpectrum myxoMs1ToSiriusMs1(List<CompactSpectrum> myxo) {
        if (myxo.size()==1) return myxoMs1ToSiriusMs1(myxo.get(0));
        final List<SimpleSpectrum> spectra = new ArrayList<>(myxo.size());
        for (CompactSpectrum cs : myxo) {
            spectra.add(myxoMs1ToSiriusMs1(cs));
        }
        return (Spectrums.mergeSpectra(spectra));
    }

    public static PrecursorIonType enumOrNameToIontype(String selectedItem) {
        Ionization stupidEnum = Ionization.byName(selectedItem);
        if (stupidEnum==null) {
            return PrecursorIonType.getPrecursorIonType(selectedItem);
        }
        else return enumToSiriusIonization(stupidEnum);
    }
}
