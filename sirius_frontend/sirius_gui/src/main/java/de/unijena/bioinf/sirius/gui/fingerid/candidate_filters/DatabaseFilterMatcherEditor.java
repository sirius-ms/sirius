package de.unijena.bioinf.sirius.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import com.google.common.collect.Sets;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidate;
import de.unijena.bioinf.sirius.gui.fingerid.DBFilterPanel;

import java.util.EnumSet;

/**
 * Created by fleisch on 19.05.17.
 */
public class DatabaseFilterMatcherEditor extends AbstractMatcherEditor<CompoundCandidate> {

    public DatabaseFilterMatcherEditor(DBFilterPanel panel) {
        panel.addFilterChangeListener(new DBFilterPanel.FilterChangeListener() {
            @Override
            public void fireFilterChanged(EnumSet<DatasourceService.Sources> filterSet) {
                fireChanged(new DatabaseMatcher(filterSet));
            }
        });

    }

    public static class DatabaseMatcher implements Matcher<CompoundCandidate> {
        final EnumSet<DatasourceService.Sources> filterSet;

        public DatabaseMatcher(EnumSet<DatasourceService.Sources> filterSet) {
            this.filterSet = filterSet;
        }

        @Override
        public boolean matches(CompoundCandidate candidate) {
            int toFlag = 0;
            for (DatasourceService.Sources s : filterSet) toFlag |= s.flag;
            //todo remove pubchem hack when db is correctly labeled
            return (filterSet.contains(DatasourceService.Sources.PUBCHEM) || (toFlag & candidate.getCompound().getBitset()) != 0);
        }
    }
}
