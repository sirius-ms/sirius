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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LipidChain implements Comparable<LipidChain> {

    public static boolean validFormulaForAcylChains(MolecularFormula formula, int numberOfChains ) {
        if (!formula.isAllPositiveOrZero()) return false;
        if (formula.numberOfHydrogens()%2 != 0) return false;
        if (formula.numberOfCarbons() < 2) return false;
        if (formula.numberOfHydrogens() > (formula.numberOfCarbons()*2 - 2)) return false;
        if (formula.numberOfOxygens() != numberOfChains) return false;
        int db = ((formula.numberOfCarbons() * 2 - 2)-formula.numberOfHydrogens()) / 2;
        if (db >= formula.numberOfCarbons()/2) return false;
        return formula.isCHO();
    }
    public static boolean validFormulaForSphingosinChains(MolecularFormula formula) {
        if (!formula.isAllPositiveOrZero()) return false;
        if (formula.numberOfNitrogens()!=1) return false;
        if (formula.numberOfHydrogens()%2 == 0) return false;
        if (formula.numberOfCarbons() < 2) return false;
        if (formula.numberOfHydrogens() > (formula.numberOfCarbons()*2 + 3 )) return false;
        if (formula.numberOfOxygens() != 2) return false;
        int db = (((formula.numberOfCarbons()*2 + 3)-formula.numberOfHydrogens())/2);
        if (db >= formula.numberOfCarbons()/2) return false;
        return formula.isCHNO();
    }

    private static MolecularFormula SPH = MolecularFormula.parseOrNull("NH3O2"), O=MolecularFormula.parseOrThrow("O");
    public static Optional<LipidChain> getMergedFromFormula(MolecularFormula origFormula, LipidClass lipidClass) {
        int chains = lipidClass.chains;
        MolecularFormula formula = origFormula;
        if (formula.numberOfCarbons() < chains) return Optional.empty();
        if (lipidClass.sphingolipid) {
            formula = formula.subtract(SPH);
            --chains;
        }
        int acylChains = formula.numberOfOxygens();
        chains -= acylChains;
        if (chains < 0) {
            return Optional.empty();
        }
        int alkylChains = chains;
        formula = formula.subtract(O.multiply(acylChains));
        if (formula.atomCount()==(formula.numberOfCarbons()+formula.numberOfHydrogens())) {
            int c = formula.numberOfCarbons();
            int doublebonds = -(formula.numberOfHydrogens() + acylChains*2 - c*2)/2;
            if (doublebonds<0 || doublebonds>c/2) return Optional.empty();
            return Optional.of(new LipidChain(origFormula, c, doublebonds));
        } else {
            return Optional.empty();
        }

    }

    public static Optional<LipidChain> fromFormula(MolecularFormula formula) {
        if (!formula.isCHNO()) return Optional.empty();
        final int c = formula.numberOfCarbons();
        if (c<2) return Optional.empty();
        final int o = formula.numberOfOxygens();
        final int n = formula.numberOfNitrogens();
        final int h = formula.numberOfHydrogens();
        if (n>0) {
            if (n==1 && o==2 && h % 2 != 0) {
                int db = (((c*2 + 3)-h)/2);
                if (db >= c/2) return Optional.empty();
                if (db>=0) return Optional.of(new LipidChain(Type.SPHINGOSIN, c, db));
            }
        } else if (o>0 && h%2==0) {
            if (o==1) {
                int db = ((c * 2 - 2)-h) / 2;
                if (db >= c/2) return Optional.empty();
                if (db >= 0)
                    return Optional.of(new LipidChain(Type.ACYL, c, db
                    ));
            }
        } else if (h%2==0) {
            int db = (2*c - h)/2;
            if (db >= c/2) return Optional.empty();
            if (db>=0) return Optional.of(new LipidChain(Type.ALKYL, c, db));
        }
        return Optional.empty();
    }

    public static LipidChain merge(LipidChain... chains) {
        return new LipidChain(Arrays.stream(chains).map(x -> x.formula).reduce(MolecularFormula::add).orElseGet(MolecularFormula::emptyFormula), Arrays.stream(chains).mapToInt(LipidChain::getChainLength).sum(), Arrays.stream(chains).mapToInt(LipidChain::getNumberOfDoubleBonds).sum());
    }

    public boolean isMerged() {
        return type==Type.MERGED;
    }

    public enum Type {
        ACYL, ALKYL, SPHINGOSIN, MERGED;
    }

    protected final int chainLength;
    protected final int numberOfDoubleBonds;
    protected final Type type;
    protected MolecularFormula formula;

    LipidChain(MolecularFormula formula, int chainLength, int numberOfDoubleBonds) {
        this.chainLength = chainLength;
        this.numberOfDoubleBonds = numberOfDoubleBonds;
        this.type = Type.MERGED;
        this.formula = formula;
    }

    public LipidChain(Type type, int chainLength, int numberOfDoubleBonds) {
        this.chainLength = chainLength;
        this.numberOfDoubleBonds = numberOfDoubleBonds;
        this.type = type;
        this.formula = generateMolecularFormula(type, chainLength, numberOfDoubleBonds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LipidChain that = (LipidChain) o;
        return chainLength == that.chainLength && numberOfDoubleBonds == that.numberOfDoubleBonds && type == that.type;
    }

    @Override
    public int compareTo(@NotNull LipidChain o) {
        return Comparator.comparingInt((LipidChain x)->x.chainLength).thenComparing((LipidChain x)->x.numberOfDoubleBonds).thenComparing((LipidChain x)->x.type.ordinal()).compare(this, o);
    }


    @Override
    public int hashCode() {
        return Objects.hash(chainLength, numberOfDoubleBonds, type);
    }

    public int getChainLength() {
        return chainLength;
    }

    public int getNumberOfDoubleBonds() {
        return numberOfDoubleBonds;
    }

    public Type getType() {
        return type;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    private final static BitSet defaultSet = new BitSet();
    static {
        defaultSet.set(1);
        defaultSet.set(6);
        defaultSet.set(7);
        defaultSet.set(8);
    }

    public static MolecularFormula generateMolecularFormula(Type type, int chainLength, int numberOfDoubleBonds) {
        TableSelection hcno = PeriodicTable.getInstance().getSelectionFor(defaultSet);
        switch (type) {
            case ACYL:
                return MolecularFormula.fromCompomer(hcno, new short[]{
                        (short)((chainLength-numberOfDoubleBonds)*2 - 2),
                        (short)chainLength,
                        0,
                        1
                });
            case ALKYL:
                return MolecularFormula.fromCompomer(hcno, new short[]{
                        (short)((chainLength-numberOfDoubleBonds)*2),
                        (short)chainLength,
                        0,
                        0
                });
            case SPHINGOSIN:
                return MolecularFormula.fromCompomer(hcno, new short[]{
                        (short)((chainLength-numberOfDoubleBonds)*2 + 3),
                        (short)chainLength,
                        1,
                        2
                });
            default: throw new IllegalArgumentException("Unknown chain type.");
        }
    }

    @Override
    public String toString() {
        if (type==Type.ACYL) return chainLength + ":" + numberOfDoubleBonds;
        else if (type == Type.ALKYL) return "O-" + chainLength + ":" + numberOfDoubleBonds;
        else if (type == Type.SPHINGOSIN) return "d" + chainLength + ":" + numberOfDoubleBonds;
        else if (type == Type.MERGED) return chainLength + ":" + numberOfDoubleBonds;
        else return "unknown-chain-type";
    }

    /*public static LipidChain mergedFromString(String x) {
        final int d = x.indexOf(':');
        final int length = Integer.parseInt(x.substring(0, d));

        String db = x.substring(d + 1);
        int special = db.indexOf('(');
        if (special>=0) db = db.substring(0,special);
        final int doubleBonds = Integer.parseInt(db);
        return new LipidChain(Type.MERGED,length,doubleBonds);
    }*/

    public static LipidChain fromString(String x) {
        if ("?".equals(x))
            return null;
        Type t = Type.ACYL;
        if (x.startsWith("d")) {
            t = Type.SPHINGOSIN;
            x = x.substring(1);
        } else if (x.startsWith("O-")) {
            t = Type.ALKYL;
            x = x.substring(2);
        }
        final int d = x.indexOf(':');
        final int length = Integer.parseInt(x.substring(0, d));

        String db = x.substring(d + 1);
        int special = db.indexOf('(');
        if (special>=0) db = db.substring(0,special);
        final int doubleBonds = Integer.parseInt(db);
        return new LipidChain(t,length,doubleBonds);
    }

}
