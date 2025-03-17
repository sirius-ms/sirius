package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.SoftwareTourMessage;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SoftwareTourUtils {

    /**
     * enable all software tours that have been disabled via "don't ask again"
     * @param guiProperties
     */
    public static void enableAllTours(GuiProperties guiProperties) {
        setAllTourProperties(true);
        guiProperties.resetAllTutorialsKnownForThisSession();

    }

    /**
     * disable all software tours. No further tours will be started.
     */
    public static void disableAllTours() {
        setAllTourProperties(false);
    }

    private static void setAllTourProperties(boolean enabled) {
        for (String tourKey : SoftwareTourInfoStore.AllTourKeys) {
            SiriusProperties.setAndStoreInBackground(tourKey, enabled ? null : ReturnValue.Cancel.name());
        }

    }
    public static void checkAndInitTour(Container owner, String propertyKey, GuiProperties guiProperties) {
        if (!owner.isShowing()) return; //not starting. Panel was probably decorated with data in the background
        if (guiProperties.isAskedTutorialThisSession(propertyKey)) return;
        else guiProperties.setTutorialKnownForThisSession(propertyKey);

        checkAndInitTour(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner), owner, propertyKey);
    }

    protected static void checkAndInitTour(Window windowOwner, Container tutorialRoot, String propertyKey) {
        ToolTipManager.sharedInstance().setEnabled(false); //disable tool tips so they don't interfere with the tour

        QuestionDialog askToStart = new QuestionDialog(windowOwner,"<html><body>Should I give you a quick tour of the interface?<br><br><small>(You can enable/disable all tours at once in the settings.)</small></body></html>", propertyKey, ReturnValue.Cancel);

        if (askToStart.isSuccess()) {
            List<Component> allComponents = collectNestedComponents(windowOwner);
            Map<Component, Boolean> componentToEnabledState = allComponents.stream().collect(Collectors.toMap(component -> component, Component::isEnabled));

            List<JComponent> componentsWithTutorial = collectNestedComponents(tutorialRoot).stream()
                    .filter(c -> c instanceof JComponent)
                    .map(c -> (JComponent) c)
                    .filter(jc -> getTourInfo(jc) != null)
                    .filter(jc -> getTourInfo(jc).isInScope(propertyKey))
                    .filter(SoftwareTourUtils::isVisibleOnScreen)
                    .sorted(Comparator.comparing(jc -> getTourInfo(jc).getOrderImportance()))
                    .toList();

            for (int i = 0; i < componentsWithTutorial.size(); i++) {
                JComponent component = componentsWithTutorial.get(i);
                SoftwareTourInfo tourInfo = getTourInfo(component);
                if (component instanceof SoftwareTourHighlighter<?> h) {
                    h.updateBounds();
                }

                //disable all components
                allComponents.forEach(c -> c.setEnabled(false));
                //highlight current
                Border originalBorder = highlight(component);
                boolean originalBorderPainted = ensureBorderPainted(component);
                enableFocusedComponent(component, componentToEnabledState);
                //dialog
                SoftwareTourMessage tutorialDialog = new SoftwareTourMessage(windowOwner, tourInfo.getTutorialDescription(), i+1, componentsWithTutorial.size());
                setRelativeLocation(component, tourInfo, tutorialDialog);
                tutorialDialog.setVisible(true);
                //reset
                resetHighlight(component, originalBorder, originalBorderPainted);
                if (tutorialDialog.isCancel()) break;
            }

            componentToEnabledState.forEach(Component::setEnabled);
        }
        ToolTipManager.sharedInstance().setEnabled(true);
    }

    private static boolean ensureBorderPainted(JComponent jc) {
        if (jc instanceof JToolBar tb) {
            boolean state =  tb.isBorderPainted();
            tb.setBorderPainted(true);
            return state;
        } else {
            return true;
        }
    }

    private static boolean isVisibleOnScreen(Component component) {
        if (component == null) return false;
        return component.isShowing();

        //in case the above method is not sufficient, we may try to check more in-depth, see below

//        // Check if component is part of a visible hierarchy and has a valid size
//        if (!component.isShowing() || component.getWidth() <= 0 || component.getHeight() <= 0) {
//            return false;
//        }
//
//        // Traverse up the parent hierarchy to check visibility
//        Container parent = component.getParent();
//        while (parent != null) {
//            if (!parent.isVisible()) {
//                return false; // If any parent is not visible, component is not visible
//            }
//            parent = parent.getParent();
//        }
//
//        // Check if it is actually rendered on screen
//        try {
//            component.getLocationOnScreen(); // This will fail if the component is not fully visible
//        } catch (Exception e) {
//            return false;
//        }
//
//        return true;
    }

    private static Border highlight(JComponent jc) {
        Border originalBorder = jc.getBorder();
        jc.setBorder(BorderFactory.createLineBorder(Colors.GOOD, 4));
        return originalBorder;
    }

    private static void resetHighlight(JComponent jc, Border originalBorder, boolean originalBorderPainted) {
        jc.setBorder(originalBorder);
        if (jc instanceof JToolBar tb) {
            tb.setBorderPainted(originalBorderPainted);
        }
    }

    private static void enableFocusedComponent(JComponent component, Map<Component, Boolean> componentToEnabledState) {
        collectNestedComponents(component).forEach(c -> c.setEnabled(componentToEnabledState.get(c)));
    }

    @Nullable
    public static SoftwareTourInfo getTourInfo(JComponent jc) {
        return (SoftwareTourInfo) jc.getClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY);
    }

    private static void setRelativeLocation(JComponent component, SoftwareTourInfo tourInfo, SoftwareTourMessage tutorialDialog) {
        int x = 0, y = 0;
        switch (tourInfo.getLocationHorizontal()) {
            case LEFT_ALIGN_TO_RIGHT -> x = component.getX();
            case CENTER -> x = component.getX() + component.getWidth() / 2 - tutorialDialog.getWidth() / 2;
            case RIGHT_SPACE -> x = component.getX() + component.getWidth() + 10;
            case LEFT_SPACE_TO_LEFT -> x = component.getX() - tutorialDialog.getWidth() - 10;
            case RIGHT_ALIGN -> x = component.getX() + component.getWidth() - tutorialDialog.getWidth();
        }
        switch (tourInfo.getLocationVertical()) {
            case ON_TOP -> y = component.getY() - tutorialDialog.getHeight() - 10;
            case TOP_TOP_ALIGN -> y = component.getY();
            case CENTER -> y = component.getY() + component.getHeight() / 2 - tutorialDialog.getHeight() / 2;
            case BOTTOM_BOTTOM_ALIGN -> y = component.getY() + component.getHeight() - tutorialDialog.getHeight();
            case BELOW_BOTTOM -> y = component.getY() + component.getHeight() + 10;
        }

        Point location = new Point(x, y);
        SwingUtilities.convertPointToScreen(location, component.getParent()); // this is used to convert coordinates for a dialog (top-level window). If used for a component with a parent use 'SwingUtilities.convertPoint()'
        tutorialDialog.setLocation(location);
    }


    public static void addSoftwareTourGlassPane(JLayeredPane layeredPane, JList<?> list, Component nestedComponent, SoftwareTourInfo tourInfo) {
        SoftwareTourHighlighter<?> glassPanel = new SoftwareTourHighlighter<>(list, nestedComponent, tourInfo);
        layeredPane.add(glassPanel, JLayeredPane.PALETTE_LAYER); // Above the scrollPane
    }

    public static List<Component> collectNestedComponents(Container c) {
        List<Component> components = new ArrayList<>();
        recursiveTraverse(components, c);
        return components;
    }

    private static void recursiveTraverse(List<Component> components, Container root) {
        components.add(root);
        for (Component comp : root.getComponents()) {
            if (comp instanceof Container container) {
                recursiveTraverse(components, container);
            } else {
                components.add(comp);
            }
        }
    }

    /**
     * "Glass panel" above a nested component in a JList to highlight it for software tour
     * @param <E>
     */
    private static class SoftwareTourHighlighter<E> extends JPanel {

        private final JList<E> list;
        private final Component nestedComponent;

        public SoftwareTourHighlighter(JList<E> list, Component nestedComponent, SoftwareTourInfo tourInfo) {
            super();
            putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, tourInfo);
            setOpaque(false);
            this.list = list;
            this.nestedComponent = nestedComponent;
        }

        public void updateBounds() {
            ListCellRenderer<? super E> renderer = list.getCellRenderer();

            // force nestedComponent to be for row 0
            Component cell = renderer.getListCellRendererComponent(list, list.getModel().getElementAt(0), 0, false, false);

            Point point = nestedComponent.getLocation();
            Component currentSource = nestedComponent.getParent();
            while (currentSource != null) {
                if (currentSource == cell) break;
                point.x += currentSource.getX();
                point.y += currentSource.getY();

                currentSource = currentSource.getParent();
            }

            int proxyX = point.x;
            int proxyY = point.y;

            setBounds(proxyX, proxyY, nestedComponent.getWidth(), nestedComponent.getHeight());
        }
    }
}
