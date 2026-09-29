/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.utils.filter;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

/**
 * The transaction boundary of a filter editor: a <b>working</b> {@link FeatureFilter} that every edit goes to,
 * next to the <b>applied</b> one the {@link FeatureFilterModel} holds. Chips, widgets and switches are all
 * editors of the same working value - staging an edit is {@link #edit}, previewing is rendering
 * {@link #working()}, {@link #revert()} is an assignment and {@link #commit()} publishes. Nothing else needs
 * to stage anything.
 * <p>
 * While the working value has no edits of its own it follows the applied one, so an editor that is open
 * during a project reload keeps showing the truth; once edited it stays put until commit or revert.
 * <p>
 * Meant for the event dispatch thread; listeners are called synchronously on the thread that edits.
 */
public final class FilterSession {

    private final FeatureFilterModel model;
    private FeatureFilter working;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final PropertyChangeListener followApplied;

    /** A session over the model's applied filter; the working value starts equal to it. */
    public FilterSession(@NotNull FeatureFilterModel model) {
        this.model = model;
        this.working = model.get();
        this.followApplied = evt -> {
            // no edits of our own -> follow the applied value (a project reload, a commit elsewhere)
            if (working.equals(evt.getOldValue()))
                setWorking((FeatureFilter) evt.getNewValue());
        };
        model.addPropertyChangeListener(FeatureFilterModel.FILTER_CHANGED, followApplied);
    }

    /** The holder of the applied filter this session commits to. */
    @NotNull
    public FeatureFilterModel model() {
        return model;
    }

    /** The project context both values are interpreted against. */
    @NotNull
    public FilterContext context() {
        return model.getContext();
    }

    /** The filter as it is applied to the list. */
    @NotNull
    public FeatureFilter applied() {
        return model.get();
    }

    /** The filter as it is being edited - what an editor renders and what {@link #commit()} publishes. */
    @NotNull
    public FeatureFilter working() {
        return working;
    }

    /** Whether the working value differs from the applied one. */
    public boolean isDirty() {
        return !working.equals(applied());
    }

    /** Starts an editing session: the working value is the applied one. */
    public void open() {
        setWorking(applied());
    }

    /** Discards every edit: the working value is the applied one again. */
    public void revert() {
        open();
    }

    /** Stages an edit. */
    public void edit(@NotNull UnaryOperator<FeatureFilter> edit) {
        setWorking(edit.apply(working));
    }

    /** Replaces the working value. */
    public void set(@NotNull FeatureFilter filter) {
        setWorking(filter);
    }

    /** Stages "nothing filtered". */
    public void clear() {
        setWorking(FeatureFilter.empty(context()));
    }

    /** Stages SIRIUS's recommended starting filter, see {@link FeatureFilter#defaults}. */
    public void restoreDefaults() {
        setWorking(FeatureFilter.defaults(context()));
    }

    /**
     * Publishes the working value as the applied filter and re-filters the list (one
     * {@code filterUpdateCompleted} for the whole batch of edits).
     */
    public void commit() {
        model.set(working);
        model.fireUpdateCompleted();
    }

    /** Runs {@code onChange} whenever the working value changes; returns the handle that unsubscribes. */
    @NotNull
    public Runnable subscribe(@NotNull Runnable onChange) {
        listeners.add(onChange);
        return () -> listeners.remove(onChange);
    }

    /** Stops following the applied filter; call when the editor that owns this session goes away. */
    public void dispose() {
        model.removePropertyChangeListener(FeatureFilterModel.FILTER_CHANGED, followApplied);
        listeners.clear();
    }

    private void setWorking(@NotNull FeatureFilter filter) {
        if (filter.equals(working))
            return;
        working = filter;
        listeners.forEach(Runnable::run);
    }
}
