package de.unijena.bioinf.ms.gui.tree_viewer;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ftalign.CommonLossScoring;

public class TreeViewerConnector{

    public String getRescoredTree(String json_tree){
        // TODO implement
        FTree tree = (new FTJsonReader()).treeFromJsonString(json_tree, null);
        // do stuff on tree
        return (new FTJsonWriter()).treeToJsonString(tree);
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

}
