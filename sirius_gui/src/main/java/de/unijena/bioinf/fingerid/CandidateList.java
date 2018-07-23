package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import javax.swing.*;
import java.util.*;

/**
 * Created by fleisch on 15.05.17.
 */
public class CandidateList extends ActionList<CompoundCandidate, Set<FingerIdData>> implements ActiveElementChangedListener<SiriusResultElement, ExperimentContainer> {

    public final DoubleListStats scoreStats;
    public final DoubleListStats logPStats;
    public final DoubleListStats tanimotoStats;

    public CandidateList(final FormulaList source) {
        this(source, DataSelectionStrategy.ALL_SELECTED);
    }

    public CandidateList(final FormulaList source, DataSelectionStrategy strategy) {
        super(CompoundCandidate.class, strategy);

        scoreStats = new DoubleListStats();
        logPStats = new DoubleListStats();
        tanimotoStats = new DoubleListStats();
        source.addActiveResultChangedListener(this);
        resultsChanged(null, null, source.getElementList(), source.getResultListSelectionModel());
    }

    @Override
    public void resultsChanged(ExperimentContainer experiment, SiriusResultElement sre, List<SiriusResultElement> resultElements, ListSelectionModel selectionModel) {
        //call only from EDT
        elementList.clear();
        scoreStats.reset();
        logPStats.reset();
        tanimotoStats.reset();

        data = new HashSet<>();
        List<SiriusResultElement> formulasToShow = new LinkedList<>();

        switch (selectionType) {
            case ALL:
                formulasToShow.addAll(resultElements);
                break;
            case FIRST_SELECTED:
                formulasToShow.add(sre);
                break;
            case ALL_SELECTED:
                for (int i = selectionModel.getMinSelectionIndex(); i <= selectionModel.getMaxSelectionIndex(); i++) {
                    if (selectionModel.isSelectedIndex(i)) {
                        formulasToShow.add(resultElements.get(i));
                    }
                }
                break;
        }

        List<CompoundCandidate> emChache = new ArrayList<>();
        for (SiriusResultElement e : formulasToShow) {
            if (e != null && e.getFingerIdComputeState().equals(ComputingStatus.COMPUTED)) {
                for (int j = 0; j < e.getFingerIdData().compounds.length; j++) {
                    CompoundCandidate c = new CompoundCandidate(j + 1, j, e.getFingerIdData(), e.getResult().getPrecursorIonType());
                    emChache.add(c);
                    scoreStats.addValue(c.getScore());
                    logPStats.addValue(c.compound.getXlogP());
                    tanimotoStats.addValue(c.getTanimotoScore());
                    data.add(c.data);
                }
                if (e.getFingerIdData() != null) {
                    LoadMoleculeJob e1 = new LoadMoleculeJob(e.getFingerIdData().compounds);
                    Jobs.MANAGER.submitJob(e1);
                }
            }
        }

        elementList.addAll(emChache);
        notifyListeners(data, null, elementList, getResultListSelectionModel());
    }
}
