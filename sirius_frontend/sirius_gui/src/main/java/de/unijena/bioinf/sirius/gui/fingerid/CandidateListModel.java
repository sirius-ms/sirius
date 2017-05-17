package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.chemdb.DatasourceService;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListModel extends AbstractListModel<CompoundCandidate> {
    private CandidateList sourceList;
    ArrayList<CompoundCandidate> candidates;

    public CandidateListModel(CandidateList sourceList) {
        this.sourceList = sourceList;
        this.candidates = new ArrayList<>();
        change();
    }

    @Override
    public int getSize() {
        return candidates.size();
    }

    public void refreshList() {
        fireContentsChanged(this, 0, getSize());
    }

    public void change(int elem) {
        fireContentsChanged(this, elem, elem);
    }

    public void change(int from, int to) {
        fireContentsChanged(this, from, to);
    }

    public void change() { //todo i think this does filtering and db flags??? we have to move this
        //todo this is ugly and ony a temporary soltion, we want to get rid of this model and du the filtering with event lists

        candidates.clear();
        int toFlag = 0;
        for (CompoundCandidate candidate : sourceList.getElementList()) {
            final double minValue = candidate.data.getMinLogPFilter();
            final double maxValue = candidate.data.getMaxLogPFilter();

            if (candidate.data.dbSelection.contains(DatasourceService.Sources.PUBCHEM)) toFlag = -1;
            else for (DatasourceService.Sources s : candidate.data.dbSelection) toFlag |= s.flag;

            if (toFlag < 0 || (toFlag & candidate.compound.bitset) != 0) {
                double logp = candidate.compound.xlogP;
                if (!Double.isNaN(logp) && logp >= minValue && logp <= maxValue) {
                    candidates.add(candidate);
                }
            }
        }

        refreshList();
    }

    @Override
    public CompoundCandidate getElementAt(int index) {
        return candidates.get(index);
    }
}
