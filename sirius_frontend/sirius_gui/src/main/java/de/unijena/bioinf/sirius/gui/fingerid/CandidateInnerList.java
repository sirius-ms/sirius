package de.unijena.bioinf.sirius.gui.fingerid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateInnerList extends JList<CompoundCandidate> {
    private final static NumberFormat prob = new DecimalFormat("%");

    public CandidateInnerList(ListModel<CompoundCandidate> dataModel) {
        super(dataModel);
    }
    @Override
    public String getToolTipText(MouseEvent e) {
        final Point point = e.getPoint();
        final int index = locationToIndex(point);
//        selectedCompoundId = index;
        if (index < 0) return null;
        final CompoundCandidate candidate = getModel().getElementAt(index);
//        highlightedCandidate = candidate.rank;
        final Rectangle relativeRect = getCellBounds(index, index);
        final boolean in;
        int rx, ry;
        {
               /* if (candidate.substructures != null)
                    return null;*/
            final Rectangle box = candidate.substructures.getBounds();
            final int absX = box.x + relativeRect.x;
            final int absY = box.y + relativeRect.y;
            final int absX2 = box.width + absX;
            final int absY2 = box.height + absY;
            in = point.x >= absX && point.y >= absY && point.x < absX2 && point.y < absY2;
            rx = point.x - absX;
            ry = point.y - absY;
        }
        int fpindex = -1;
        if (in) {
            final int row = ry / CandidateCellRenderer.CELL_SIZE;
            final int col = rx / CandidateCellRenderer.CELL_SIZE;
            fpindex = candidate.substructures.indexAt(row, col);
        }
        if (fpindex >= 0) {
            return candidate.compound.fingerprint.getFingerprintVersion().getMolecularProperty(fpindex).getDescription() + "  (" + prob.format(candidate.getPlatts().getProbability(fpindex)) + " %)";
        } else return null;

    }
}
