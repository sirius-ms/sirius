package de.unijena.bioinf.ms.gui.utils;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.text.ParseException;
import java.util.Objects;

/**
 * Custom formatter used to display the maximum double value in a JSpinner as 'Infinite'
 */
public class MaxDoubleAsInfinityTextFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
    private final double infinityValue;
    private final SpinnerNumberModel model;

    public MaxDoubleAsInfinityTextFormatterFactory(SpinnerNumberModel model, double infinityValue) {
        this.infinityValue = infinityValue;
        this.model = model;
    }


    @Override
    public JFormattedTextField.AbstractFormatter getFormatter(final JFormattedTextField tf) {
        if (!(tf.getFormatter() instanceof CustomDoubleFormatter))
            return new CustomDoubleFormatter(model, infinityValue);
        return tf.getFormatter();
    }

    public static class CustomDoubleFormatter extends NumberFormatter {
        private static final String INFINITE_TEXT = "Infinite";
        private final double infinityValue;
        private final SpinnerNumberModel model;

        public CustomDoubleFormatter(SpinnerNumberModel model, double infinityValue) {
            this.infinityValue = infinityValue;
            this.model = model;
            setValueClass(model.getValue().getClass());
        }


        @Override
        public Object stringToValue(final String text) throws ParseException {
            try {
                if (Objects.equals(text, INFINITE_TEXT))
                    return infinityValue;
                Object value = Double.valueOf(text);
                if (value instanceof Number){
                    if (((Number)value).doubleValue()>infinityValue){
                        return infinityValue;
                    }
                }
                return value;
//                        Double.valueOf(text);
            } catch (final NumberFormatException nfx) {
                throw new ParseException("Failed to parse input \"" + text + "\".", 0);
            }
        }

        @Override
        public String valueToString(final Object value) throws ParseException {
            if (Objects.equals(value, infinityValue))
                return INFINITE_TEXT;
            return super.valueToString(value);
        }

        @Override
        public void setMinimum(Comparable<?> min) {
            model.setMinimum(min);
        }

        @Override
        public Comparable<?> getMinimum() {
            return  model.getMinimum();
        }

        @Override
        public void setMaximum(Comparable<?> max) {
            model.setMaximum(max);
        }

        @Override
        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }
    }

}
