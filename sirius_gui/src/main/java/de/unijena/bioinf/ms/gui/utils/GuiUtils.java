/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.utils;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.webView.WebViewBrowserDialog;
import de.unijena.bioinf.ms.properties.PropertyManager;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GuiUtils {

    public final static int SMALL_GAP = 5;
    public final static int MEDIUM_GAP = 10;
    public final static int LARGE_GAP = 20;

    public static void initUI() {

        switch (Colors.THEME()) {
            case DARK:
                try {
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    e.printStackTrace();
                }
            case LIGHT:
            default:
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    LoggerFactory.getLogger(GuiUtils.class).error("Error when configuring look and feel!", e);
                    e.printStackTrace();
                }
        }

        //load fonts. Run AFTER setting look-and-feel
        Fonts.initFonts();
        Colors.adjustLookAndFeel();

        //nicer times for tooltips
        ToolTipManager.sharedInstance().setInitialDelay(500);
        ToolTipManager.sharedInstance().setDismissDelay(60000);
    }

    public static void drawListStatusElement(boolean isComputing, Graphics2D g2, Component c) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String icon = isComputing ? "\u2699" : "";

        int offset = g2.getFontMetrics().stringWidth(icon);
        g2.drawString(icon, c.getWidth() - offset - 10, c.getHeight() - 8);
    }

    public static boolean assignParameterToolTip(@NotNull final JComponent comp, @NotNull String parameterKey) {
        final String parameterKeyShort = PropertyManager.DEFAULTS.shortKey(parameterKey);
        if (PropertyManager.DEFAULTS.getConfigValue(parameterKeyShort) != null) {
            PropertyManager.DEFAULTS.getConfigDescription(parameterKeyShort).ifPresent(des ->
                    comp.setToolTipText(formatToolTip(Stream.concat(Stream.of(des), Stream.of("Commandline: 'CONFIG --" + parameterKeyShort + "'")).collect(Collectors.toList()))));
            return true;
        }
        return false;
    }

    public static void setEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabled(child, enabled);
            }
        }
    }

    public static final int toolTipWidth = 500;

    public static String formatAndStripToolTip(String... lines) {
        return formatAndStripToolTip(Arrays.asList(lines));
    }

    public static String formatAndStripToolTip(List<String> lines) {
        return formatToolTip(lines.stream()
                .filter(Objects::nonNull)
                .map(l -> l.replaceAll("\\s*%n\\s*", ""))
                .map(l -> l.replaceAll("\\s*@\\|.*\\|@\\s*", ""))
                .toList());
    }

    public static String formatToolTip(String... lines) {
        return formatToolTip(toolTipWidth, lines);
    }

    public static String formatToolTip(java.util.List<String> lines) {
        return formatToolTip(toolTipWidth, lines);
    }

    public static String formatToolTip(int width, String... lines) {
        if (lines == null)
            return null;
        return formatToolTip(width, List.of(lines));
    }

    public static String formatToolTip(int width, java.util.List<String> lines) {
        if (lines == null || lines.isEmpty())
            return null;
        return "<html><p width=\"" + width + "\">"
                + lines.stream().filter(Objects::nonNull).map(it -> it.replace("\n", "<br>")).collect(Collectors.joining("<br>"))
                + "</p></html>";
    }

    public static Dimension getEffectiveScreenSize(@NotNull GraphicsConfiguration c) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Dimension((int) Math.round(d.width * .8), (int) Math.round(d.height * .8));
    }

    public static JPanel newNoResultsComputedPanel() {
        return newNoResultsComputedPanel(null);
    }

    public static JPanel newNoResultsComputedPanel(@Nullable String message) {
        JPanel p = new JPanel(new BorderLayout());
        JPanel pp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pp.add(new JLabel(message == null ? "No results Computed!" : message));
        p.add(pp, BorderLayout.SOUTH);
        return p;
    }


    public static JPanel newEmptyResultsPanel() {
        return newEmptyResultsPanel(null);
    }

    public static JPanel newEmptyResultsPanel(@Nullable String message) {
        return newEmptyResultsPanelWithLabel(message).left();
    }

    public static Pair<JPanel, JLabel> newEmptyResultsPanelWithLabel(@Nullable String message) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(Icons.NO_MATCH_128), BorderLayout.CENTER);
        JPanel pp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel label = new JLabel(message == null ? "No results found!" : message);
        pp.add(label);
        p.add(pp, BorderLayout.SOUTH);
        return Pair.of(p, label);
    }

    public static void openURLOrError(@NotNull Frame owner, URI url) {
        try {
            openURL(owner, url, true);
        } catch (IOException e) {
            new ExceptionDialog(owner, "Error opening URL '" + url + "'. Cause: " + e.getMessage());
        }
    }

    public static void openURL(@NotNull Window owner, URI url) throws IOException {
        openURL(owner, url, true);
    }

    public static void openURL(@NotNull Window owner, URI url, boolean useSystemBrowser) throws IOException {
        openURL(owner, url, null, useSystemBrowser);
    }


    public static void openURL(@Nullable Window owner, @NotNull URI url, String title, boolean useSystemBrowser) throws IOException {
        if (url == null)
            if (owner instanceof JDialog dialog)
                new ExceptionDialog(dialog, "Cannot open empty URL!");
            else
                new ExceptionDialog((Frame) owner, "Cannot open empty URL!");

        if (useSystemBrowser) {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(url);
                return;
            } else {
                String message = "Could not Open URL in System Browser. Trying SIRIUS WebView or Please visit Page Manually" + url;
                LoggerFactory.getLogger(GuiUtils.class).error(message);
            }
        }

        if (owner instanceof JDialog dialog)
            new WebViewBrowserDialog(dialog, title == null ? "SIRIUS WebView" : title, url);
        else
            new WebViewBrowserDialog((Frame) owner, title == null ? "SIRIUS WebView" : title, url);
    }

    /**
     * Adds a key binding to close the given dialog on pressing escape
     */
    public static void closeOnEscape(JDialog dialog) {
        JRootPane rootPane = dialog.getRootPane();
        String escapePressed = "escapePressed";
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), escapePressed);
        rootPane.getActionMap().put(escapePressed, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    public static <T extends DescriptiveOptions> JComboBox<T> makeParameterComboBoxFromDescriptiveValues(T[] options) {
        return makeParameterComboBoxFromDescriptiveValues(options, null);
    }

    public static <T extends DescriptiveOptions> JComboBox<T> makeParameterComboBoxFromDescriptiveValues(T[] options, @Nullable T defaultSelection) {
        return makeComboBoxWithTooltips(options, defaultSelection, DescriptiveOptions::getDescription);
    }

    public static <T> JComboBox<T> makeComboBoxWithTooltips(T[] options, @Nullable T defaultSelection, Function<T, String> toolTipProvider) {
        JComboBox<T> box = new JComboBox<>(options);
        if (options.length > 0) {
            box.setToolTipText(toolTipProvider.apply(options[0]));
        }
        box.addItemListener(e -> {
            if (e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            box.setToolTipText(toolTipProvider.apply((T) e.getItem()));
        });

        if (defaultSelection != null && Arrays.asList(options).contains(defaultSelection))
            box.setSelectedItem(defaultSelection);

        return box;
    }
}
