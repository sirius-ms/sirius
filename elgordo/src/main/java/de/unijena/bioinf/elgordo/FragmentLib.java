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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

public class FragmentLib {

    private final HashMap<PrecursorIonType, FragmentSet> library;

    private FragmentLib(HashMap<PrecursorIonType, FragmentSet> library) {
        this.library = library;
    }

    public Optional<FragmentSet> getFor(PrecursorIonType ionType) {
        return Optional.ofNullable(library.get(ionType));
    }

    public boolean detectableInPositiveMode() {
        for (PrecursorIonType ionType : library.keySet()) {
            if (ionType.getCharge()>0) return true;
        }
        return false;
    }

    public Set<PrecursorIonType> getDetectableModes() {
        return Collections.unmodifiableSet(library.keySet());
    }

    public boolean hasSphingosin() {
        for (FragmentSet set : library.values()) {
            if (set.sphingosinLosses.length>0 || set.sphingosinFragments.length>0)
                return true;
        }
        return false;
    }

    public static class FragmentSet {
        public final MolecularFormula[] fragments, losses, alkylLosses, acylLosses, alkylFragments,acylFragments, plasmalogenFragments, sphingosinFragments, sphingosinLosses;
        private FragmentSet(MolecularFormula[] fragments, MolecularFormula[] losses, MolecularFormula[] acylFragments, MolecularFormula[] acylLosses, MolecularFormula[] alkylFragments, MolecularFormula[] alkylLosses, MolecularFormula[] plasmalogenFragments, MolecularFormula[] sphingosinFragments,MolecularFormula[] sphingosinLosses) {
            this.fragments = fragments;
            this.losses = losses;
            this.alkylLosses = alkylLosses;
            this.acylLosses = acylLosses;
            this.acylFragments = acylFragments;
            this.alkylFragments = alkylFragments;
            this.plasmalogenFragments = plasmalogenFragments;
            this.sphingosinFragments = sphingosinFragments;
            this.sphingosinLosses = sphingosinLosses;
        }

        public MolecularFormula[] fragmentsFor(LipidChain.Type type) {
            switch (type) {
                case ACYL:return acylFragments;
                case ALKYL:return alkylFragments;
                case SPHINGOSIN: return sphingosinFragments;
                default:return new MolecularFormula[0];
            }
        }
        public MolecularFormula[] lossesFor(LipidChain.Type type) {
            switch (type) {
                case ACYL:return acylLosses;
                case ALKYL:return alkylLosses;
                case SPHINGOSIN: return sphingosinLosses;
                default:return new MolecularFormula[0];
            }
        }

        private final static FragmentSet empty_set = new FragmentSet(new MolecularFormula[0],new MolecularFormula[0],
                new MolecularFormula[0],new MolecularFormula[0],new MolecularFormula[0],new MolecularFormula[0],new MolecularFormula[0], new MolecularFormula[0], new MolecularFormula[0]);
        public static FragmentSet empty() {
            return empty_set;
        }

        public boolean isAdductSwitch() {
            return true;
        }

        public boolean hasAlkyl() {
            return alkylLosses.length>0 || alkylFragments.length>0;
        }
    }

    public static Builder def(String ionType) {
        return new Builder(null, ionType(ionType));
    }

    public static class Builder {
        Builder parent;
        PrecursorIonType ionType;
        boolean adductSwitch=false;

        MolecularFormula[] fragments, losses, alkylLosses, acylLosses,plasmalogenFragments, acylFragments, alkylFragments, sphingosinLosses, sphingosinFragments;

        public Builder(Builder parent, PrecursorIonType ionType) {
            this.parent = parent;
            this.ionType = ionType;
        }

        public Builder def(String ionType) {
            return new Builder(this, ionType(ionType));
        }

        public Builder sphingosinFragments(String... frags) {
            sphingosinFragments = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder sphingosinFragments(MolecularFormula... frags) {
            sphingosinFragments = frags;
            return this;
        }
        public Builder sphingosinLosses(String... frags) {
            sphingosinLosses = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder sphingosinLosses(MolecularFormula... frags) {
            sphingosinLosses = frags;
            return this;
        }

        public Builder fragments(String... frags) {
            fragments = Arrays.stream(frags).map(MolecularFormula::parseOrThrow).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder fragments(MolecularFormula... frags) {
            fragments = frags;
            return this;
        }

        public Builder losses(String... frags) {
            losses = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder losses(MolecularFormula... frags) {
            losses = frags;
            return this;
        }

        public Builder alkyl(String... frags) {
            alkylLosses = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder alkyl(MolecularFormula... frags) {
            alkylLosses = frags;
            return this;
        }

        public Builder acyl(String... frags) {
            acylLosses = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder acyl(MolecularFormula... frags) {
            acylLosses =frags;
            return this;
        }

        public Builder plasmalogenFragment(String... frags) {
            plasmalogenFragments = Arrays.stream(frags).map(MolecularFormula::parseOrThrow).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder plasmalogenFragment(MolecularFormula... frags) {
            plasmalogenFragments = frags;
            return this;
        }

        public Builder adductSwitch() {
            adductSwitch = true;
            return this;
        }

        public FragmentLib done() {
            final ArrayList<Builder> builders = new ArrayList<>();
            builders.add(this);
            Builder b = parent;
            while (b!=null) {
                builders.add(b);
                b = b.parent;
            }
            final ArrayList<Builder> positiveAdducts = new ArrayList<>();
            final ArrayList<Builder> negativeAdducts = new ArrayList<>();
            Builder positive = new Builder(null, ionType("+"));
            Builder negative = new Builder(null, ionType("-"));
            for (Builder c : builders) {
                if (c.ionType.isIonizationUnknown()) {
                    if (c.ionType.getCharge()>0) positive = c;
                    else negative = c;
                } else {
                    if (c.ionType.getCharge()>0) positiveAdducts.add(c);
                    else negativeAdducts.add(c);
                }
            }
            return buildLibrary(positive,positiveAdducts.toArray(Builder[]::new),negative,negativeAdducts.toArray(Builder[]::new));

        }

        public Builder acylFragments(String... frags) {
            acylFragments = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder acylFragments(MolecularFormula... frags) {
            acylFragments = frags;
            return this;
        }
        public Builder alkylFragments(String... frags) {
            alkylFragments = Arrays.stream(frags).map(FragmentLib::parseF).toArray(MolecularFormula[]::new);
            return this;
        }
        public Builder alkylFragments(MolecularFormula... frags) {
            alkylFragments = frags;
            return this;
        }
    }

    private static FragmentLib buildLibrary(Builder positive, Builder[] positiveAdducts, Builder negative, Builder[] negativeAdducts) {
        final HashMap<PrecursorIonType, FragmentSet> map = new HashMap<>();
        for (Builder b : positiveAdducts) {
            map.put(b.ionType, new FragmentSet(
               join(positive.fragments, b.fragments),
                join(positive.losses,b.losses),
                    join(positive.acylFragments, b.acylFragments),
                    join(positive.acylLosses, b.acylLosses),
                    join(positive.alkylFragments, b.alkylFragments),
                    join(positive.alkylLosses,b.alkylLosses),
                    join(positive.plasmalogenFragments,b.plasmalogenFragments),
                    join(positive.sphingosinFragments, b.sphingosinFragments),
                    join(positive.sphingosinLosses, b.sphingosinLosses)
            ));
        }
        for (Builder b : negativeAdducts) {
            map.put(b.ionType, new FragmentSet(
                    join(negative.fragments, b.fragments),
                    join(negative.losses,b.losses),
                    join(negative.acylFragments, b.acylFragments),
                    join(negative.acylLosses, b.acylLosses),
                    join(negative.alkylFragments, b.alkylFragments),
                    join(negative.alkylLosses,b.alkylLosses),
                    join(negative.plasmalogenFragments,b.plasmalogenFragments),
                    join(negative.sphingosinFragments, b.sphingosinFragments),
                    join(negative.sphingosinLosses, b.sphingosinLosses)
            ));
        }
        return new FragmentLib(map);
    }

    private static MolecularFormula[] mf(MolecularFormula[] a) {
        if (a==null) return new MolecularFormula[0];
        else return a;
    }

    private static MolecularFormula[] join(MolecularFormula[] a, MolecularFormula[] b) {
        if (b==null && a == null) return new MolecularFormula[0];
        if (a == null) return b;
        if (b == null) return a;
        final HashSet<MolecularFormula> g = new HashSet<>(Arrays.asList(a));
        g.addAll(Arrays.asList(b));
        return g.toArray(MolecularFormula[]::new);
    }

    private static MolecularFormula parseF(String form) {
        if (form.isEmpty()) return MolecularFormula.emptyFormula();
        if (form.charAt(0) == '-') {
            return MolecularFormula.parseOrThrow(form.substring(1)).negate();
        } else {
            return MolecularFormula.parseOrThrow(form);
        }
    }

    private static PrecursorIonType ionType(String ionType) {
        return ionType.equals("+") ? PrecursorIonType.unknownPositive() : (ionType.equals("-") ? PrecursorIonType.unknownNegative() : PrecursorIonType.getPrecursorIonType(ionType));
    }

}
