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

package de.unijena.bioinf.sirius.elementdetection.prediction;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class DNNElementPredictor implements ElementPredictor {

    protected TrainedElementDetectionNetwork[] networks;
    protected ChemicalAlphabet alphabet;
    protected double[] thresholds;

    public DNNElementPredictor() {
        this.networks = readNetworks();
        this.thresholds = new double[DETECTABLE_ELEMENTS.length];
        Arrays.fill(thresholds, 0.05);
        setThreshold("Si", 0.5);
        final Element[] elems = new Element[FREE_ELEMENTS.length+DETECTABLE_ELEMENTS.length];
        System.arraycopy(FREE_ELEMENTS, 0, elems, 0, FREE_ELEMENTS.length);
        System.arraycopy(DETECTABLE_ELEMENTS, 0, elems, FREE_ELEMENTS.length, DETECTABLE_ELEMENTS.length);
        this.alphabet = new ChemicalAlphabet(elems);
    }

    public void disableSilicon() {
        setThreshold("Si", Double.POSITIVE_INFINITY);
    }

    public void setThreshold(double threshold) {
        Arrays.fill(thresholds, threshold);
    }

    public void setThreshold(String symbol, double threshold) {
        setThreshold(PeriodicTable.getInstance().getByName(symbol), threshold);
    }

    public void setThreshold(Element element, double threshold) {
        for (int i=0; i < DETECTABLE_ELEMENTS.length; ++i) {
            if (DETECTABLE_ELEMENTS[i].equals(element)) {
                thresholds[i] = threshold;
                return;
            }
        }
        throw new IllegalArgumentException(element.getSymbol() + " is not predictable");
    }

    private static TrainedElementDetectionNetwork[] readNetworks() {
        try {
            final TrainedElementDetectionNetwork fivePeaks = TrainedElementDetectionNetwork.readNetwork(DNNElementPredictor.class.getResourceAsStream("/dnn_element_detection_5.param"));
            final TrainedElementDetectionNetwork fourPeaks = TrainedElementDetectionNetwork.readNetwork(DNNElementPredictor.class.getResourceAsStream("/dnn_element_detection_4.param"));
            final TrainedElementDetectionNetwork threePeaks = TrainedElementDetectionNetwork.readNetwork(DNNElementPredictor.class.getResourceAsStream("/dnn_element_detection_3.param"));
            return new TrainedElementDetectionNetwork[]{fivePeaks, fourPeaks, threePeaks};
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Element[] DETECTABLE_ELEMENTS;
    private final static int[] UPPERBOUNDS, FREE_UPPERBOUNDS;
    private final static Element[] FREE_ELEMENTS;
    private final static Element SELENE;

    static {
        PeriodicTable T = PeriodicTable.getInstance();
        DETECTABLE_ELEMENTS = new Element[]{
                T.getByName("B"),
                T.getByName("Br"),
                T.getByName("Cl"),
                T.getByName("S"),
                T.getByName("Si"),
                T.getByName("Se"),
        };
        FREE_ELEMENTS = new Element[]{
                T.getByName("C"),
                T.getByName("H"),
                T.getByName("N"),
                T.getByName("O"),
                T.getByName("P"),
                T.getByName("F"),
                T.getByName("I")
        };
        UPPERBOUNDS = new int[]{2,5,5,10,2,2};
        FREE_UPPERBOUNDS = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 10, 20, 6};
        SELENE = T.getByName("Se");
    }

    @Override
    public FormulaConstraints predictConstraints(SimpleSpectrum pickedPattern) {
        final HashSet<Element> elements = new HashSet<>(10);
        elements.addAll(Arrays.asList(FREE_ELEMENTS));
        // special case for selene
        if (pickedPattern.size() > 5) {
            double intensityAfterFifth = 0d;
            for (int i=pickedPattern.size()-1; i >= 5; --i) {
                intensityAfterFifth += pickedPattern.getIntensityAt(i);
            }
            double intensityBeforeFifth = 0d;
            for (int i=0; i < 5; ++i) {
                intensityBeforeFifth += pickedPattern.getIntensityAt(i);
            }
            intensityAfterFifth /= intensityBeforeFifth;
            if (intensityAfterFifth > 0.25) elements.add(SELENE);
        }
        for (TrainedElementDetectionNetwork network : networks) {
            if (network.numberOfPeaks() <= pickedPattern.size() ) {
                final double[] prediction = network.predict(pickedPattern);
                for (int i=0; i < prediction.length; ++i) {
                    if (prediction[i] >= thresholds[i]) {
                        elements.add(DETECTABLE_ELEMENTS[i]);
                    }
                }
                break;
            }
        }
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.toArray(new Element[elements.size()]));
        final FormulaConstraints constraints = new FormulaConstraints(alphabet);
        for (int i=0; i < FREE_UPPERBOUNDS.length; ++i) {
            constraints.setUpperbound(FREE_ELEMENTS[i], FREE_UPPERBOUNDS[i]);
        }
        for (int i=0; i < UPPERBOUNDS.length; ++i) {
            if (elements.contains(DETECTABLE_ELEMENTS[i]))
                constraints.setUpperbound(DETECTABLE_ELEMENTS[i], UPPERBOUNDS[i]);
        }
        return constraints;
    }

    @Override
    public ChemicalAlphabet getChemicalAlphabet() {
        return alphabet;
    }

    @Override
    public boolean isPredictable(Element element) {
        for (Element detectable : DETECTABLE_ELEMENTS) {
            if (detectable.equals(element)) return true;
        }
        return false;
    }
}