package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class ResultsTreeList extends JList<SiriusResultElement>{

	public ResultsTreeList(ResultTreeListModel listModel) {
		super(listModel);
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		final int index = locationToIndex(event.getPoint());
		if (index >= 0 && index < getModel().getSize()) {
			final SiriusResultElement elem = getModel().getElementAt(index);
			return elem.getFormulaAndIonText() + (elem.getCharge() > 0 ? "+" : "-");
		} else return null;
	}
}
