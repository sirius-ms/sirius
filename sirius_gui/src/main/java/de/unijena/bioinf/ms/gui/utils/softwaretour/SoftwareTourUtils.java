package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.SoftwareTourMessage;
import de.unijena.bioinf.ms.gui.properties.GuiProperties;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SoftwareTourUtils {

    public static <C extends Component & SoftwareTourElement> void checkAndInitTutorial(Container owner, String propertyKey, GuiProperties guiProperties) {
        if (!owner.isShowing()) return; //not starting. Panel was probably decorated with data in the background
        if (guiProperties.isAskedTutorialThisSession(propertyKey)) return;
        else guiProperties.setTutorialKnownForThisSession(propertyKey);

        checkAndInitTutorial(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner), owner, propertyKey);
    }

    protected static <C extends Component & SoftwareTourElement> void checkAndInitTutorial(Window windowOwner, Container tutorialRoot, String propertyKey) {
        QuestionDialog askToStart = new QuestionDialog(windowOwner,"Should I give you a quick tour of the interface?", propertyKey);

        if (askToStart.isSuccess()) {
            List<Component> allComponents = new ArrayList<>();
            new ComponentIterator(windowOwner, (c) -> true).forEachRemaining(allComponents::add);

            Map<Component, Boolean> componentToEnabledState = allComponents.stream().collect(Collectors.toMap(component -> component, component -> component.isEnabled()));


            List<C> componentsWithTutorial = StreamSupport.stream(ComponentIterator.iterable(tutorialRoot, c -> c instanceof JComponent & c instanceof SoftwareTourElement).spliterator(), false)
                    .map(c -> (C)c)
                    .filter(c -> c.isInScope(propertyKey))
                    .filter(SoftwareTourElement::isActive)
                    .filter(SoftwareTourUtils::isVisibleOnScreen)
                    .sorted(Comparator.comparing(SoftwareTourElement::getOrderImportance))
                    .toList();

//            System.out.println("number of tutorials: " + componentsWithTutorial.size());
            for (int i = 0; i < componentsWithTutorial.size(); i++) {
                C component = componentsWithTutorial.get(i);

                //disable all components
                allComponents.forEach(c -> c.setEnabled(false));
                //highlight current
                component.highlightComponent(Colors.GOOD, 4);
                enableFocussedComponent(component, componentToEnabledState);
                //dialog
                SoftwareTourMessage tutorialDialog = new SoftwareTourMessage(windowOwner, component.getTutorialDescription(), i+1, componentsWithTutorial.size());
                setRelativeLocation(component, tutorialDialog);
                tutorialDialog.setVisible(true);
                //reset
                component.resetHighlight();
                if (tutorialDialog.isCancel()) break;
            }

            componentToEnabledState.forEach((component, isEnabled) -> component.setEnabled(isEnabled));
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

    private static <C extends Component & SoftwareTourElement> void enableFocussedComponent(C component, Map<Component, Boolean> componentToEnabledState) {
        if (component instanceof Container container) {
            StreamSupport.stream(ComponentIterator.iterable(container).spliterator(), false)
                    .forEach(c -> {if (componentToEnabledState.get(c) != null) c.setEnabled(componentToEnabledState.get(c));});
        } else {
            component.setEnabled(componentToEnabledState.get(component));
        }
    }

    private static  <C extends Component & SoftwareTourElement>  void setRelativeLocation(C component, SoftwareTourMessage tutorialDialog) {
        int x = 0, y = 0;
        switch (component.getLocationHorizontal()) {
            case LEFT_ALIGN_TO_RIGHT -> x = component.getX();
            case CENTER -> x = component.getX() + component.getWidth() / 2 - tutorialDialog.getWidth() / 2;
            case RIGHT_SPACE -> x = component.getX() + component.getWidth() + 10;
            case LEFT_SPACE_TO_LEFT -> x = component.getX() - tutorialDialog.getWidth() - 10;
            default -> {
                LoggerFactory.getLogger(SoftwareTourUtils.class).warn("Unknown location for tutorial dialog: "+ component.getLocationHorizontal());
                x = component.getX();
            }
        }
        switch (component.getLocationVertical()) {
            case ON_TOP -> y = component.getY() - tutorialDialog.getHeight() - 10;
            case TOP_TOP_ALIGN -> y = component.getY();
            case CENTER -> y = component.getY() + component.getHeight() / 2 - tutorialDialog.getHeight() / 2;
            case BOTTOM_BOTTOM_ALIGN -> y = component.getY() + component.getHeight() - tutorialDialog.getHeight();
            case BELOW_BOTTOM -> y = component.getY() + component.getHeight() + 10;
            default -> {
                LoggerFactory.getLogger(SoftwareTourUtils.class).warn("Unknown location for tutorial dialog: "+ component.getLocationHorizontal());
                y = component.getY();
            }
        }

        Point location = new Point(x, y);
        SwingUtilities.convertPointToScreen(location, component.getParent()); // this is used to convert coordinates for a dialog (top-level window). If used for a component with a parent use 'SwingUtilities.convertPoint()'
        tutorialDialog.setLocation(location);
    }


    public static <C extends Component> void addSoftwareTourGlassPane(JLayeredPane layeredPane, JScrollPane scrollPane, JList list, Class<C> cellClass, Function<C, Component> componentFunction, SoftwareTourInfo tourInfo) {
        ListModel listModel = list.getModel();
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        SoftwareTourDecorator glassPanel = new SoftwareTourDecorator<>(empty, tourInfo);
        glassPanel.setBounds(0, 0, 0, 0); // You can adjust its position as needed
        layeredPane.add(glassPanel, JLayeredPane.PALETTE_LAYER); // Above the scrollPane



        // Function to update proxy position
        Runnable updateProxyPosition = () -> {
            int index = 0; // Row index to overlay
            Rectangle rowBounds = list.getCellBounds(index, index);

            if (rowBounds != null) {
                Component cell = list.getCellRenderer().getListCellRendererComponent(
                        list, list.getModel().getElementAt(index), index, false, false
                );

                Component reference = componentFunction.apply((C)cell);
                if (reference == null) return;

                Point refPos = reference.getLocation();

                Point point = refPos;
                Component currentSource = reference.getParent();
                while (currentSource != null) {
                    if (currentSource == cell) break;
                    point.x += currentSource.getX();
                    point.y += currentSource.getY();

                    currentSource = currentSource.getParent();
                }

                int proxyX = point.x; // + rowBounds.x;
                int proxyY = point.y; // + rowBounds.y;

                glassPanel.setBounds(proxyX, proxyY, reference.getWidth(), reference.getHeight());
//                    System.out.println("setting the location to " + proxyX + ", " + proxyY + ", " + reference.getWidth() + ", " + reference.getHeight());
//
            }
        };

        //todo some listener for initialization still seems to be missing

        // 🟢 Update proxy when the list model changes (adding/removing elements)
        listModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) { updateProxyPosition.run(); }
            @Override
            public void intervalRemoved(ListDataEvent e) { updateProxyPosition.run(); }
            @Override
            public void contentsChanged(ListDataEvent e) { updateProxyPosition.run(); }
        });

        // 🟢 Update proxy when the list is resized
        list.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { updateProxyPosition.run(); }
        });

        // 🟢 Update proxy when the scroll position changes
        JViewport viewport = scrollPane.getViewport();
        viewport.addChangeListener(e -> updateProxyPosition.run());

        // 🟢 Initial positioning (invoke later to ensure layout is ready)
        SwingUtilities.invokeLater(updateProxyPosition);
    }

    public static class ComponentIterator implements Iterator<Component> {
        private final Stack<Component> stack = new Stack<>();
        private final Predicate<Component> selectable;
        private Component next = null;

        public ComponentIterator(Container root, Predicate<Component> selectable) {
            if (root != null) {
                stack.push(root); // Start with the root container
            }
            this.selectable = selectable;
        }

        @Override
        public boolean hasNext() {
            while (next == null && !stack.isEmpty()) {
                Component component = stack.pop();
                if (selectable.test(component)) next = component;

                // If the component is a container, push its children onto the stack
                if (component instanceof Container) {
                    Container container = (Container) component;
                    Component[] children = container.getComponents();

                    // Push children in reverse order so we process them in normal order
                    for (int i = children.length - 1; i >= 0; i--) {
                        if (children[i] instanceof Container || selectable.test(children[i])) {
                            stack.push(children[i]);
                        }
                    }
                }
            }

            return next != null;
        }

        @Override
        public Component next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more components.");
            }
            Component current = next;
            next = null;
            return current;
        }

        public static Iterable<Component> iterable(Container root, Predicate<Component> selectable) {
            return () -> new ComponentIterator(root, selectable);
        }

        public static Iterable<Component> iterable(Container root) {
            return () -> new ComponentIterator(root, (c) -> true);
        }
    }

}
