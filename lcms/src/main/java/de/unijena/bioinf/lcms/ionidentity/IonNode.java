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

package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.align.AlignedFeatures;

import java.util.*;

class IonNode {

    protected List<Edge> neighbours;
    private AlignedFeatures feature;
    protected double mz;
    protected boolean hasMsMs;
    protected IonAssignment assignment;

    // gibbs sampling
    protected int activeAssignment = 0;

    protected float priorForUnknownIonType;
    protected static final float priorForUncommonIonType = -4;
    protected static final float priorForCommonIonType = 0;
    protected static final float priorForAdductsAndInsource = -2;

    public IonNode(AlignedFeatures feature) {
        this.mz = feature.getMass();
        this.neighbours = new ArrayList<>();
        setFeature(feature);
    }


    public Set<PrecursorIonType> possibleIonTypes() {
        final HashSet<PrecursorIonType> types = new HashSet<>();
        for (Edge e : neighbours) {
            if (e.type== Edge.Type.ADDUCT) {
                if (e.fromType!=null) types.add(e.fromType);
            }
        }
        return types;
    }

    public boolean hasConflicts() {
        int prev = 0;
        for (; prev < neighbours.size(); ++prev) {
            if (neighbours.get(prev).type== Edge.Type.ADDUCT)
                break;
        }
        for (int k=prev+1; k < neighbours.size(); ++k) {
            if (neighbours.get(k).type== Edge.Type.ADDUCT) {
                if (!neighbours.get(prev).fromType.equals(neighbours.get(k).fromType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public AlignedFeatures getFeature() {
        return feature;
    }

    public void setFeature(AlignedFeatures feature) {
        this.feature = feature;
        this.hasMsMs = feature.getFeatures().values().stream().anyMatch(x->x.getMsMs()!=null);
    }

    public boolean hasEdge(Edge edge) {
        if (edge.fromType!=null && edge.toType!=null) {
            return neighbours.stream().anyMatch(n->edge.to == n.to && edge.fromType.equals(n.fromType) && edge.toType.equals(n.toType));
        } else {
            return neighbours.stream().anyMatch(n->n.to==edge.to);
        }

    }

    @Override
    public String toString() {
        return "[m/z = " + mz + "]";
    }

    public Set<PrecursorIonType> likelyIonTypes() {
        return likelyIonTypes(0.1);
    }

    public Set<PrecursorIonType> likelyIonTypes(double threshold) {
        final HashSet<PrecursorIonType> types = new HashSet<>();
        for (int k=0; k < assignment.probabilities.length; ++k) {
            if (assignment.probabilities[k]>=threshold) {
                types.add(assignment.ionTypes[k]);
            }
        }
        return types;
    }

    String likelyTypesWithProbs() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        for (int k=0; k < assignment.probabilities.length; ++k) {
            if (assignment.probabilities[k]>=0.1) {
                buf.append('"');
                buf.append(assignment.ionTypes[k].toString());
                buf.append('"');
                buf.append(':');
                buf.append(Math.round(assignment.probabilities[k] * 100));
                buf.append(",");
            }
        }
        buf.append("}");
        return buf.toString();
    }

    public PrecursorIonType activeType() {
        return assignment.ionTypes[activeAssignment];
    }

    public String typesWithScore(GibbsSampler s) {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append("\"[M+?]+\": ");
        buf.append(String.format(Locale.US,"%.2f",priorForUnknownIonType));
        for (int k=0; k < assignment.probabilities.length; ++k) {
            if (!assignment.ionTypes[k].isIonizationUnknown()) {
                buf.append(',');
                buf.append('"');
                buf.append(assignment.ionTypes[k].toString());
                buf.append('"');
                buf.append(':');
                buf.append(' ');
                double maxScore = 0d;
                if (s.commonTypes.contains(assignment.ionTypes[k])) {
                    maxScore += priorForCommonIonType;
                } else maxScore += priorForUncommonIonType;
                for (Edge e : neighbours) {
                    maxScore += e.score;
                }
                buf.append(String.format(Locale.US, "%.2f", maxScore));
            }
        }
        buf.append("}");
        return buf.toString();
    }
}
