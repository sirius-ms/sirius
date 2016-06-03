/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.tree;

import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.List;

/**
 * @author Kai Dührkop
 */
public interface TreeBuilder {

    public Object prepareTreeBuilding(ProcessedInput input, FGraph graph, double lowerbound);

    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound, Object preparation);

    public FTree buildTree(ProcessedInput input, FGraph graph, double lowerbound);

    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound, Object preparation);

    public List<FTree> buildMultipleTrees(ProcessedInput input, FGraph graph, double lowerbound);

    public String getDescription();


}
