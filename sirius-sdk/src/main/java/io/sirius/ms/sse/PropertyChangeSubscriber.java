/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package io.sirius.ms.sse;

import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.Flow;

public class PropertyChangeSubscriber implements Flow.Subscriber<DataObjectEvent<?>> {

    private final PropertyChangeListener wrappedListener;

    public PropertyChangeSubscriber(PropertyChangeListener wrappedListener) {
        this.wrappedListener = wrappedListener;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    public void onNext(@Nullable String propertyName, DataObjectEvent<?> item) {
        wrappedListener.propertyChange(new PropertyChangeEvent(item, propertyName, null, item));
    }
    @Override
    public void onNext(DataObjectEvent<?> item) {
        onNext(null, item);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    public static PropertyChangeSubscriber wrap(PropertyChangeListener listener){
        return new PropertyChangeSubscriber(listener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyChangeSubscriber that)) return false;
        return Objects.equals(wrappedListener, that.wrappedListener);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrappedListener);
    }
}
