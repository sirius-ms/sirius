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

import ca.odell.glazedlists.matchers.*;

public class MatcherEditorWithOptionalInvert<E> extends AbstractMatcherEditorListenerSupport<E> implements MatcherEditor.Listener<E> {

    final AbstractMatcherEditor<E> matcherEditor;
    private boolean isInverted;
    public MatcherEditorWithOptionalInvert(AbstractMatcherEditor<E> matcherEditor) {
        this.matcherEditor = matcherEditor;
        this.matcherEditor.addMatcherEditorListener(this);
    }

    public void setInverted(boolean isInverted) {
        this.isInverted = isInverted;
        fireChangedMatcher(new Event<E>(this, Event.CHANGED, getMatcher()));
    }

    public boolean isInverted() {
        return isInverted;
    }

    @Override
    public Matcher<E> getMatcher() {
        if (isInverted) {
            return new InvertedMatcher<>(matcherEditor.getMatcher());
        } else {
            return matcherEditor.getMatcher();
        }
    }

    @Override
    public void changedMatcher(Event<E> matcherEvent) {
        fireChangedMatcher(new Event<E>(this, matcherEvent.getType(), getMatcher()));
    }

    protected class InvertedMatcher<E> implements Matcher<E> {
        private final Matcher<E> matcher;

        public InvertedMatcher(Matcher<E> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(E item) {
            return !matcher.matches(item);
        }
    }
}
