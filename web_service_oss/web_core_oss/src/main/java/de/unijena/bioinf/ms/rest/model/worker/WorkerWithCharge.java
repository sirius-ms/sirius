/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model.worker;

import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WorkerWithCharge implements Comparable<WorkerWithCharge>{
    public final WorkerType workerType;
    public final PredictorType charge;

    protected WorkerWithCharge(@NotNull WorkerType workerType, @NotNull PredictorType charge) {
        this.workerType = workerType;
        this.charge = charge;
    }

    @NotNull
    public WorkerType getWorkerType() {
        return workerType;
    }

    @NotNull
    public PredictorType getCharge() {
        return charge;
    }

    public boolean isPositive() {
        return charge.isPositive();
    }

    public boolean isNegative() {
        return charge.isNegative();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerWithCharge)) return false;
        WorkerWithCharge that = (WorkerWithCharge) o;
        return workerType == that.workerType && charge == that.charge;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerType, charge);
    }

    @Override
    public String toString() {
        return workerType.name() + (isPositive() ? "_POS" : "_NEG");
    }

    public static WorkerWithCharge of(@NotNull WorkerInfo workerInfo) {
        return of(workerInfo.getType(), workerInfo.getPredictorsAsEnums().iterator().next());
    }

    public static WorkerWithCharge of(@NotNull WorkerType workerType, boolean positive) {
        return of(workerType, positive ? PredictorType.CSI_FINGERID_POSITIVE : PredictorType.CSI_FINGERID_NEGATIVE);
    }

    public static WorkerWithCharge of(@NotNull WorkerType workerType, @NotNull PredictorType charge) {
        return new WorkerWithCharge(workerType, charge);
    }

    @Override
    public int compareTo(@NotNull WorkerWithCharge o) {
        return Integer.compare(this.workerType.ordinal() * 10 + this.charge.ordinal(), o.workerType.ordinal() * 10 + o.charge.ordinal());
    }
}
