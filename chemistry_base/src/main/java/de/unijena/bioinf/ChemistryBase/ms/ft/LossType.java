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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class LossType implements TreeAnnotation {

    public static enum Type {
        REGULAR, IN_SOURCE, ADDUCT_LOSS;
    }

    private final static LossType REGULAR_TYPE = new LossType(Type.REGULAR);

    public static LossType regular() {
        return REGULAR_TYPE;
    }

    public static LossType adductLoss(MolecularFormula adductFormula, MolecularFormula originalFormula ) {
        return new LossType(Type.ADDUCT_LOSS, new AdductLossInformation(adductFormula,originalFormula));
    }

    private static final LossType IN_SOURCE = new LossType(Type.IN_SOURCE);
    public static LossType insource() {
        return IN_SOURCE;
    }

    protected final Type type;
    protected final LossTypeVariant meta;

    public LossType() {
        this(Type.REGULAR);
    }

    public Type getType() {
        return type;
    }

    public boolean isAdductLoss() {
        return type==Type.ADDUCT_LOSS;
    }

    public boolean isInSource() {
        return type==Type.IN_SOURCE;
    }

    public boolean isRegular() {
        return type==Type.REGULAR;
    }

    private LossType(Type type) {
        this(type,null);
    }

    private LossType(Type type, LossTypeVariant meta) {
        this.type = type;
        this.meta = meta;
    }

    public AdductLossInformation getAdductLossInformation() {
        if (type!=Type.ADDUCT_LOSS)
            throw new RuntimeException("loss contains no adduct loss information");
        return (AdductLossInformation) meta;
    }

    protected static interface LossTypeVariant {

    }

    public static class AdductLossInformation implements LossTypeVariant{

        protected final MolecularFormula adductFormula;
        protected final MolecularFormula originalFormula, resolvedFormula;

        public AdductLossInformation(MolecularFormula adductFormula, MolecularFormula originalFormula) {
            this.adductFormula = adductFormula;
            this.originalFormula = originalFormula;
            this.resolvedFormula = originalFormula.subtract(adductFormula);
        }

        public MolecularFormula getAdductFormula() {
            return adductFormula;
        }

        public MolecularFormula getOriginalFormula() {
            return originalFormula;
        }

        public MolecularFormula getResolvedFormula() {
            return resolvedFormula;
        }
    }



}
