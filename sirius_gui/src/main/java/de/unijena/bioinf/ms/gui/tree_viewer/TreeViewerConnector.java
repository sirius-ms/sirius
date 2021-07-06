/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ftalign.CommonLossScoring;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.VisualizationPanelSynchronizer;

import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TreeViewerConnector{

    VisualizationPanelSynchronizer sync;

    public void registerSynchronizer(VisualizationPanelSynchronizer sync){
        this.sync = sync;
    }

    public void selectionChanged(float fragment_mz) {
        sync.fragmentChanged(fragment_mz);
    }

    public String getRescoredTree(String json_tree){
        // TODO implement
        try {
            FTree tree = (new FTJsonReader()).treeFromJsonString(json_tree, null);
            return (new FTJsonWriter()).treeToJsonString(tree);
        } catch (IOException e) {
            LoggerFactory.getLogger(TreeViewerConnector.class).error(e.getMessage(), e);
        }
        return json_tree;
    }

    public String formulaDiff(String f1, String f2) throws UnknownElementException{
        MolecularFormula formula1 = MolecularFormula.parse(f1);
        final MolecularFormula formula2 = MolecularFormula.parse(f2);
        return formula1.subtract(formula2).toString();
    }

    public boolean formulaIsSubset(String f1, String f2) throws UnknownElementException{
        final MolecularFormula formula1 = MolecularFormula.parse(f1);
        final MolecularFormula formula2 = MolecularFormula.parse(f2);
        return formula2.contains(formula1);
    }

    public String[] getCommonLosses(){
        return CommonLossScoring.LOSSES;
    }

	public boolean getXmas(){
		return SiriusProperties.getProperty("de.unijena.bioinf.tree_viewer.special", null, "").equals("xmas");
	}

	public void deactivateSpecial(){
		SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty("de.unijena.bioinf.tree_viewer.special", "");
	}

}
