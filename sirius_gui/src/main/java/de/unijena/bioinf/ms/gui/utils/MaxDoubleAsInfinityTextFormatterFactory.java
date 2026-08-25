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
import java.text.ParsePosition;
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


        /**
         * Reads back what {@link #valueToString} wrote, which means reading it with the same format.
         * <p>
         * It used to be {@code Double.valueOf}, which is not that format and disagrees with it as soon as a
         * number is long enough to be grouped: this field displays 4990 as "4,990" (or "4.990"), and
         * {@code Double.valueOf} either refuses it - and a spinner whose text will not parse silently declines
         * to step, which is what made the arrows look broken above 1000 - or, where the grouping separator is a
         * dot, reads it as 4.99 without complaining at all.
         */
        @Override
        public Object stringToValue(final String text) throws ParseException {
            if (Objects.equals(text, INFINITE_TEXT))
                return infinityValue;

            ParsePosition position = new ParsePosition(0);
            Object parsed = getFormat().parseObject(text, position);
            // all of it has to be a number, or "12abc" would quietly become 12
            if (!(parsed instanceof Number number) || position.getIndex() != text.length())
                throw new ParseException("Failed to parse input \"" + text + "\".", Math.max(position.getErrorIndex(), 0));

            // asking for more than the maximum is how one asks for no upper bound at all
            return Math.min(number.doubleValue(), infinityValue);
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
