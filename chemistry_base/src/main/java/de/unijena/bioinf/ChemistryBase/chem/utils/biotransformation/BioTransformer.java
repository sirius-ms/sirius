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

package de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by fleisch on 29.05.17.
 */
public class BioTransformer {

    public static List<MolecularFormula> getAllTransformations(MolecularFormula source) {
        List<MolecularFormula> ts = new ArrayList<>(BioTransformation.values().length * 2);
        for (BioTransformation transformation : BioTransformation.values()) {
            ts.addAll(transform(source, transformation));
        }
        return ts;
    }

    public static List<MolecularFormula> transform(final MolecularFormula source, Collection<BioTransformation> transformations) {
        return transform(source, transformations.stream());
    }

    public static List<MolecularFormula> transform(final MolecularFormula source, BioTransformation[] transformations) {
        return transform(source, Arrays.stream(transformations));
    }

    public static List<MolecularFormula> transform(final MolecularFormula source, Stream<BioTransformation> transformations) {
        return transformations.distinct().map((trans) -> transform(source, trans)).flatMap((List::stream)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<MolecularFormula> transform(MolecularFormula source, BioTransformation transformation) {
        List<MolecularFormula> ts = new ArrayList<>(2);
        if (transformation.isConditional()) {
            if (source.contains(transformation.getCondition())) {
                ts.add(transform(source, transformation.getCondition(), transformation.getFormula()));
            }
            if (transformation.isSymmetric() && source.contains(transformation.getFormula())) {
                ts.add(transform(source, transformation.getFormula(), transformation.getCondition()));
            }
        } else {
            ts.add(transformAdd(source, transformation.getFormula()));
            if (transformation.isSymmetric() && source.contains(transformation.getFormula())) {
                ts.add(transformRemove(source, transformation.getFormula()));
            }
        }
        return ts;
    }

    public static MolecularFormula transform(MolecularFormula source, MolecularFormula remove, MolecularFormula add) {
        return transformAdd(transformRemove(source, remove), add);
    }

    public static MolecularFormula transformAdd(MolecularFormula source, MolecularFormula add) {
        return source.add(add);
    }

    public static MolecularFormula transformRemove(MolecularFormula source, MolecularFormula remove) {
        return source.subtract(remove);
    }


}
