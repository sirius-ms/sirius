package de.unijena.bioinf.projectspace;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import de.unijena.bioinf.jjobs.FastPropertyChangeSupport;
import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a feature that starts or stops computing reaches the toolbar, and how it must not.
 * <p>
 * The actions that may not run during a computation - compute, delete, write summaries, FBMN export - decide
 * whether they are enabled by asking whether anything is computing, so something has to make them ask again
 * when a computation ends. The obvious way, having the feature announce the change like it announces its
 * results, is the wrong one: a job usually covers every feature of the project, the compound list is an
 * {@code ObservableElementList} over a bean connector, and so a per-feature announcement would become a list
 * change per feature - each of which makes every action rescan the whole list. Quadratic, on the event
 * dispatch thread, exactly when the application is busiest.
 * <p>
 * So a compute-state change stays invisible to the list, and the batch reports itself once instead
 * ({@code CompoundList.notifyComputeStateChange}). These tests hold that line: the state is applied, repeated
 * states are recognised as nothing, and none of it disturbs the list.
 */
public class ComputeStateChangeTest {

    private GuiProjectManager projectManager;

    @BeforeEach
    public void setup() throws Exception {
        // A feature registers itself with the project manager's change support when it is constructed, and that
        // is all it needs from it here. A mock has no fields, so the change support is put in by hand rather
        // than standing up a whole project.
        projectManager = Mockito.mock(GuiProjectManager.class);
        Field pcs = GuiProjectManager.class.getDeclaredField("pcs");
        pcs.setAccessible(true);
        pcs.set(projectManager, new FastPropertyChangeSupport(projectManager));
    }

    private InstanceBean feature(String id, boolean computing) {
        AlignedFeature feature = new AlignedFeature();
        feature.setAlignedFeatureId(id);
        feature.setComputing(computing);
        return new InstanceBean(feature, List.of(AlignedFeatureOptField.NONE), projectManager);
    }

    /**
     * A pipeline wired the way {@code CompoundList} wires it, since the bean connector is what would turn a
     * property change on a feature into a list change.
     */
    private ObservableElementList<InstanceBean> compoundListOver(InstanceBean... features) {
        BasicEventList<InstanceBean> source = new BasicEventList<>();
        ObservableElementList<InstanceBean> observable =
                new ObservableElementList<>(source, GlazedLists.beanConnector(InstanceBean.class));
        source.addAll(List.of(features));
        return observable;
    }

    private static AtomicInteger listChangesOf(ObservableElementList<InstanceBean> list) {
        AtomicInteger changes = new AtomicInteger();
        list.addListEventListener(event -> changes.incrementAndGet());
        return changes;
    }

    /**
     * A feature announces its results on the event dispatch thread, so anything it announced would arrive only
     * once that thread has caught up. Waiting here means "nothing was raised" is a fact rather than a race.
     */
    private static void awaitAnyNotification() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    /**
     * The state has to be applied - that is what {@code isComputing} answers and what every action asks.
     */
    @Test
    public void testTheComputeStateIsApplied() {
        InstanceBean computing = feature("1", true);

        assertTrue(computing.changeComputeStateOfCache(false), "the state really changed");
        assertFalse(computing.isComputing());

        assertTrue(computing.changeComputeStateOfCache(true));
        assertTrue(computing.isComputing());
    }

    /**
     * The point of the whole arrangement: a job over a project full of features must not raise a list change
     * per feature. Both directions, since a job flips every feature twice.
     */
    @Test
    public void testChangingTheComputeStateDoesNotDisturbTheList() throws Exception {
        InstanceBean computing = feature("1", true);
        InstanceBean idle = feature("2", false);
        ObservableElementList<InstanceBean> list = compoundListOver(computing, idle);
        AtomicInteger changes = listChangesOf(list);

        assertTrue(computing.changeComputeStateOfCache(false));
        assertTrue(idle.changeComputeStateOfCache(true));
        awaitAnyNotification();

        assertEquals(0, changes.get(),
                "a compute-state change must not reach the list - the batch reports itself once instead");
    }

    /**
     * The server repeats the state of a running job on every progress event. A batch has to be able to tell
     * that such an event changed nothing, or it would report itself for every progress tick.
     */
    @Test
    public void testRepeatingTheSameStateChangesNothing() {
        InstanceBean computing = feature("1", true);

        assertFalse(computing.changeComputeStateOfCache(true), "already computing");
        assertTrue(computing.isComputing());

        assertTrue(computing.changeComputeStateOfCache(false));
        assertFalse(computing.changeComputeStateOfCache(false), "already not computing");
    }

    /**
     * A feature nobody selected has its project-space listener switched off, which is what silences its result
     * events. Its compute state must still be applied, since the actions ask the whole list and not just the
     * selection - and it must stay just as invisible to the list as any other.
     */
    @Test
    public void testAnUnselectedFeatureIsHandledLikeAnyOther() throws Exception {
        InstanceBean unselected = feature("1", true);
        unselected.disableProjectSpaceListener(); // what deselecting a feature does
        ObservableElementList<InstanceBean> list = compoundListOver(unselected);
        AtomicInteger changes = listChangesOf(list);

        assertTrue(unselected.changeComputeStateOfCache(false));
        awaitAnyNotification();

        assertFalse(unselected.isComputing());
        assertEquals(0, changes.get(), "still nothing for the list to hear");
    }
}
