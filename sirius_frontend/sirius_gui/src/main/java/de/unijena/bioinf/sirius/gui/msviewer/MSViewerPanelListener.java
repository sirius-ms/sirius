package de.unijena.bioinf.sirius.gui.msviewer;

import java.util.List;

public interface MSViewerPanelListener {

	public void peaksMarked(List<Integer> indices);

	public void peaksMarkedPerDrag(List<Integer> indices);

//	public void peaksSelected(List<Integer> indices);

	public void markingsRemoved();

}
