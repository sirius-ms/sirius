/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

import java.util.Arrays;
import java.util.Objects;

public final class LipidSpecies implements ProcessedInputAnnotation {

    private final LipidChain[] chains;
    private final LipidClass type;

    public static LipidSpecies fromString(String lipid) {
        LipidClass klasse = null;

        for (LipidClass c : LipidClass.values()) {
            if (lipid.startsWith(c.abbr())) {
                klasse = c;
                break;
            }
        }
        if (klasse==null) throw new IllegalArgumentException("Unknown lipid: " + lipid);
        int chainIndex = lipid.indexOf('(')+1;
        String chain = lipid.substring(chainIndex, lipid.lastIndexOf(')'));
        if (chain.isEmpty()) throw new IllegalArgumentException("Unknown lipid: " + lipid);
        final String[] subchains = chain.split("/");
        final LipidChain[] chains = Arrays.stream(subchains).map(x->LipidChain.fromString(x)).toArray(LipidChain[]::new);
        return new LipidSpecies(klasse, chains);
    }

    public LipidSpecies(LipidClass type, LipidChain[] chains) {
        this.chains = chains;
        this.type = type;
    }

    public LipidSpecies(LipidClass type) {
        this.chains = new LipidChain[0];
        this.type = type;
    }

    public boolean chainsUnknown() {
        return chains.length==0;
    }

    public HeadGroup getHeadGroup() {
        return type.headgroup;
    }

    public LipidClass getLipidClass() {
        return type;
    }

    public LipidChain[] getChains() {
        return chains;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LipidSpecies that = (LipidSpecies) o;
        return Arrays.equals(chains, that.chains) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(chains);
        return result;
    }

    @Override
    public String toString() {
        return type.abbr() + "("+ (chains.length>0 ? Joiner.on('/').join(chains) : "?")  + ")";
    }
}
