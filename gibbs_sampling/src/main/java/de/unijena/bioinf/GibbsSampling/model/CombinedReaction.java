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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Arrays;

public class CombinedReaction implements Reaction {
    private Reaction[] reactions;
    private MolecularFormula netChange;
    private int stepSize;
    MolecularFormula simpleReactionGroup;
    MolecularFormula transformationRemovedGroup;
    MolecularFormula transformationAddedGroup;
    private String reactionString;

    public CombinedReaction(Reaction... reactions) {
        if(reactions.length < 1) {
            throw new RuntimeException("no reactions specified");
        } else {
            this.simpleReactionGroup = MolecularFormula.emptyFormula();
            this.transformationRemovedGroup = MolecularFormula.emptyFormula();
            this.transformationAddedGroup = MolecularFormula.emptyFormula();
            this.stepSize = 0;

            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < reactions.length; ++i) {
                Reaction transformation = reactions[i];
                builder.append(transformation.toString());
                if (i<reactions.length-1) builder.append(", ");
                this.parseReaction(transformation);
            }

            this.reactionString = builder.toString();

            ArrayList var4 = new ArrayList();
            if(!this.simpleReactionGroup.isEmpty()) {
                SimpleReaction var5 = new SimpleReaction(this.simpleReactionGroup);
                var4.add(var5);
            }

            if(!this.transformationRemovedGroup.isEmpty()) {
                Transformation var6 = new Transformation(this.transformationRemovedGroup, this.transformationAddedGroup);
                var4.add(var6);
            }

            this.reactions = (Reaction[])var4.toArray(new Reaction[0]);
            netChange = reactions[0].netChange().clone();
            for (int i = 1; i < reactions.length; i++) {
                netChange = netChange.add(reactions[i].netChange());
            }

            if(this.netChange == null) {
                throw new RuntimeException("netChange null");
            }
        }
    }

    private void parseReaction(Reaction reaction) {
        this.stepSize += reaction.stepSize();
        if(reaction instanceof SimpleReaction) {
            this.simpleReactionGroup = this.simpleReactionGroup.add(reaction.netChange());
        } else if(reaction instanceof Transformation) {
            Transformation combinedReaction = (Transformation)reaction;
            this.transformationRemovedGroup = this.transformationRemovedGroup.add(combinedReaction.getRemovedGroup());
            this.transformationAddedGroup = this.transformationAddedGroup.add(combinedReaction.getAddedGroup());
        } else {
            if(!(reaction instanceof CombinedReaction)) {
                throw new RuntimeException("Unknown reaction type");
            }

            CombinedReaction combinedReaction1 = (CombinedReaction)reaction;
            this.simpleReactionGroup = this.simpleReactionGroup.add(combinedReaction1.simpleReactionGroup);
            this.transformationRemovedGroup = this.transformationRemovedGroup.add(combinedReaction1.transformationRemovedGroup);
            this.transformationAddedGroup = this.transformationAddedGroup.add(combinedReaction1.transformationAddedGroup);
        }

    }

    public Reaction[] getSubReactions() {
        return (Reaction[])Arrays.copyOf(this.reactions, this.reactions.length);
    }

    public boolean hasReaction(MolecularFormula mf1, MolecularFormula mf2) {
        MolecularFormula mf = mf1;
        Reaction[] var4 = this.reactions;
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Reaction reaction = var4[var6];
            MolecularFormula mfX = mf.add(reaction.netChange());
            if(!reaction.hasReaction(mf, mfX)) {
                return false;
            }

            mf = mfX;
        }

        return mf2.equals(mf);
    }

    public MolecularFormula netChange() {
        return this.netChange.clone();
    }

    public Reaction negate() {
        int length = this.reactions.length;
        Reaction[] inverseReaction = new Reaction[length];

        for(int i = 0; i < this.reactions.length; ++i) {
            Reaction reaction = this.reactions[i];
            inverseReaction[length - 1 - i] = reaction.negate();
        }

        return new CombinedReaction(inverseReaction);
    }

    public int stepSize() {
        return this.stepSize;
    }

    public String toString() {
        return reactionString;
//        StringBuffer sb = new StringBuffer("CombinedReaction{");
//        sb.append("netChange=").append(this.netChange);
//        sb.append(", reactions=").append(this.reactions == null?"null":Arrays.asList(this.reactions).toString());
//        sb.append('}');
//        return sb.toString();
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof CombinedReaction)) {
            return false;
        } else {
            CombinedReaction that = (CombinedReaction)o;
            return !this.netChange.equals(that.netChange)?false:(this.reactions.length != that.reactions.length?false:(!this.simpleReactionGroup.equals(that.simpleReactionGroup)?false:(!this.transformationRemovedGroup.equals(that.transformationRemovedGroup)?false:this.transformationAddedGroup.equals(that.transformationAddedGroup))));
        }
    }

    public int hashCode() {
        int result = this.netChange.hashCode();
        result = 31 * result + this.simpleReactionGroup.hashCode();
        result = 31 * result + this.transformationRemovedGroup.hashCode();
        result = 31 * result + this.transformationAddedGroup.hashCode();
        return result;
    }
}
