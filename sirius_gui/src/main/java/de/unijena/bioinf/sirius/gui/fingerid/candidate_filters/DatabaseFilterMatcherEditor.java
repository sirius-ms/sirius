package de.unijena.bioinf.sirius.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.sirius.gui.db.CustomDataSourceService;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidate;
import de.unijena.bioinf.sirius.gui.fingerid.DBFilterPanel;

import java.util.Map;

/**
 * Created by fleisch on 19.05.17.
 */
public class DatabaseFilterMatcherEditor extends AbstractMatcherEditor<CompoundCandidate> {
    Map<String, CustomDataSourceService.Source> SOURCE_MAP = CustomDataSourceService.SOURCE_MAP;

    public DatabaseFilterMatcherEditor(DBFilterPanel panel) {
        panel.addFilterChangeListener(new DBFilterPanel.FilterChangeListener() {
            @Override
            public void fireFilterChanged(long filterSet) {
                fireChanged(new DatabaseMatcher(filterSet));
            }
        });

    }

    public static class DatabaseMatcher implements Matcher<CompoundCandidate> {
        final long filterSet;

        public DatabaseMatcher(long filterSet) {
            this.filterSet = filterSet;
        }

        @Override
        public boolean matches(CompoundCandidate candidate) {
            return (filterSet == 0 || (filterSet & candidate.getCompound().getBitset()) != 0);
        }
    }
}
