/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.utils.loading;

import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface Loadable {
    boolean setLoading(boolean loading, boolean absolute);

    default boolean setLoading(boolean loading){
        return setLoading(loading, false);
    }

    default boolean increaseLoading() {
        return setLoading(true);
    }

    default boolean decreaseLoading() {
        return setLoading(false);
    }

    default boolean disableLoading() {
        return setLoading(false, true);
    }


    //todo add support for determined loading.
    default TinyBackgroundJJob<Boolean> runInBackgroundAndLoad(final Runnable task) {
        return Jobs.runInBackground(() -> {
            setLoading(true, true);
            try {
                task.run();
            }finally {
                setLoading(false, true);
            }
        });
    }

    default <T> TinyBackgroundJJob<T> runInBackgroundAndLoad(final Callable<T> task) {
        return Jobs.runInBackground(() -> {
            setLoading(true, true);
            try {
                return task.call();
            }finally {
                setLoading(false, true);
            }
        });
    }

}
