/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.query.LuceneQueryCompiler;
import de.unijena.bioinf.ms.gui.utils.query.QueryNode;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The session is the one transaction boundary of every filter editor, so its contract is what the overlay,
 * the dialog and the quick switches all rely on: edits stay in the working value until commit, revert drops
 * them, commit publishes exactly the working value with one list refresh, and the preview of a restore or a
 * clear is nothing but the working value rendered.
 */
public class FilterSessionTest {

    private static final PrecursorIonType PROTONATED = PrecursorIonType.fromString("[M + H]+");

    private static FeatureFilterModel modelWithAdducts() {
        FeatureFilterModel model = new FeatureFilterModel(FilterContext.unbounded(List.of(PROTONATED)));
        return model;
    }

    /** Property change support delivers on the EDT; wait for it so the follow-the-applied path has run. */
    private static void drainEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    @Test
    public void editsStayInTheWorkingValueUntilCommit() {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);
        FeatureFilter applied = model.get();

        session.edit(f -> f.withMz(300, 400));

        assertTrue(session.isDirty());
        assertEquals(applied, model.get(), "the applied filter is untouched before commit");
        assertTrue(session.working().isMzFilterActive(session.context()));
    }

    @Test
    public void revertDropsEveryEdit() {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);

        session.edit(f -> f.withMz(300, 400).withInverted(true));
        session.restoreDefaults();
        session.revert();

        assertFalse(session.isDirty());
        assertEquals(model.get(), session.working());
    }

    @Test
    public void commitPublishesExactlyTheWorkingValueAndRefiltersOnce() throws Exception {
        FeatureFilterModel model = modelWithAdducts();
        AtomicInteger refilters = new AtomicInteger();
        model.addUpdateCompleteListener(evt -> refilters.incrementAndGet());
        FilterSession session = new FilterSession(model);

        session.edit(f -> f.withMz(300, 400));
        session.edit(f -> f.withHasMsMs(false));
        session.edit(f -> f.withUserQuery("name:caffeine"));
        FeatureFilter working = session.working();
        session.commit();
        drainEdt(); // the model delivers filterUpdateCompleted on the EDT

        assertEquals(working, model.get());
        assertEquals(1, refilters.get(), "a batch of edits re-filters the list once, on commit");
        assertFalse(session.isDirty());
        assertEquals("name:caffeine", model.getSearchText(), "the shared document follows the committed value");
    }

    @Test
    public void restoreDefaultsPreviewIsTheDefaultsRendered() {
        FeatureFilterModel model = modelWithAdducts();
        model.set(FeatureFilter.empty(model.getContext()).withMz(300, 400)); // something that is not the default
        FilterSession session = new FilterSession(model);

        session.restoreDefaults();

        List<String> preview = PanelQueryNodeFactory.nodesFor(session.working(), session.context(), ConfidenceDisplayMode.EXACT)
                .stream().map(LuceneQueryCompiler::render).toList();
        List<String> defaults = PanelQueryNodeFactory.nodesFor(FeatureFilter.defaults(session.context()), session.context(), ConfidenceDisplayMode.EXACT)
                .stream().map(LuceneQueryCompiler::render).toList();
        assertEquals(defaults, preview, "the overlay previews the defaults by rendering the working value");
        assertFalse(preview.isEmpty());
        assertTrue(model.get().isMzFilterActive(model.getContext()), "the applied filter is untouched until commit");
    }

    @Test
    public void clearThenRestoreThenCommitRoundTrips() {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);

        session.clear();
        assertFalse(session.working().isActive(session.context()));
        session.restoreDefaults();
        session.commit();

        assertEquals(FeatureFilter.defaults(model.getContext()), model.get());
    }

    @Test
    public void listenersFireOnEveryWorkingChangeAndUnsubscribe() {
        FilterSession session = new FilterSession(modelWithAdducts());
        AtomicInteger changes = new AtomicInteger();
        Runnable unsubscribe = session.subscribe(changes::incrementAndGet);

        session.edit(f -> f.withMz(300, 400));
        session.edit(f -> f.withMz(300, 400)); // no change -> no event
        assertEquals(1, changes.get());

        unsubscribe.run();
        session.clear();
        assertEquals(1, changes.get());
    }

    @Test
    public void anUneditedSessionFollowsTheAppliedFilter() throws Exception {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);

        model.set(model.get().withHasMs1(true)); // e.g. a commit from another editor
        drainEdt();

        assertEquals(model.get(), session.working(), "not dirty -> the working value follows");
        assertFalse(session.isDirty());
    }

    @Test
    public void anEditedSessionKeepsItsEditsWhenTheAppliedFilterChanges() throws Exception {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);
        session.edit(f -> f.withMz(300, 400));
        FeatureFilter working = session.working();

        model.set(model.get().withHasMs1(true));
        drainEdt();

        assertEquals(working, session.working(), "dirty -> the edits stay until commit or revert");
        assertTrue(session.isDirty());
    }

    @Test
    public void aDisposedSessionStopsFollowing() throws Exception {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);
        FeatureFilter before = session.working();
        session.dispose();

        model.set(model.get().withHasMs1(true));
        drainEdt();

        assertEquals(before, session.working());
    }

    @Test
    public void facetRemovalIsAnEditOnTheWorkingValue() {
        FeatureFilterModel model = modelWithAdducts();
        FilterSession session = new FilterSession(model);
        PanelQueryNodeFactory.Facet hasMsMs = PanelQueryNodeFactory.facets(session.working(), session.context(), ConfidenceDisplayMode.EXACT)
                .stream().filter(f -> f.id().equals("hasMsMs")).findFirst().orElseThrow();

        session.edit(hasMsMs.reset());

        assertFalse(session.working().hasMsMs());
        assertTrue(model.get().hasMsMs(), "staged, not applied");
        List<QueryNode> nodes = PanelQueryNodeFactory.nodesFor(session.working(), session.context(), ConfidenceDisplayMode.EXACT);
        assertTrue(nodes.stream().map(LuceneQueryCompiler::render).noneMatch(s -> s.contains("hasMsMs")));
    }
}
