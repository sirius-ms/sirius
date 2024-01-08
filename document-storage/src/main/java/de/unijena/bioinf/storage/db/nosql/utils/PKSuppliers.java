/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.storage.db.nosql.utils;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Supplier;

public class PKSuppliers {

    @Getter
    private static final Supplier<Long> longKey = () -> Math.abs(UUID.randomUUID().getLeastSignificantBits());

    @Getter
    private static final Supplier<Double> doubleKey = () -> (double) longKey.get();

    @Getter
    private static final Supplier<BigInteger> bigIntKey = () -> {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             DataOutputStream dataOut = new DataOutputStream(byteOut)) {
            UUID uuid = UUID.randomUUID();
            dataOut.writeLong(uuid.getMostSignificantBits());
            dataOut.writeLong(uuid.getLeastSignificantBits());
            return new BigInteger(1, byteOut.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    @Getter
    private static final Supplier<BigDecimal> bigDecimalKey = () -> new BigDecimal(bigIntKey.get());

    @Getter
    private static final Supplier<String> stringKey = () -> UUID.randomUUID().toString();

}
