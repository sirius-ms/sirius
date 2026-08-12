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

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.LoadingBackroundTask;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.gui.webView.BrowserPanelProvider;
import it.unimi.dsi.fastutil.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.isNullOrBlank;


/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@Slf4j
public class GuiUtils {

    public final static int SMALL_GAP = 5;
    public final static int MEDIUM_GAP = 10;
    public final static int LARGE_GAP = 20;

    /**
     * Default size for windows showing large content that has no meaningful preferred size of its
     * own, e.g. web views. Always to be used together with {@link #shrinkToUsableScreen(Dimension)}
     * or {@link #packWithinUsableScreen(Window)}, since it may exceed small screens.
     */
    public final static Dimension LARGE_CONTENT_SIZE = new Dimension(1350, 800);

    /**
     * Size of a popup window showing a single web page, e.g. an external page that could not be
     * opened in the system browser.
     */
    public final static Dimension WEB_VIEW_POPUP_SIZE = new Dimension(600, 800);

    /**
     * @return the screen area that is usable for windows, so excluding task bars and the like
     */
    public static Rectangle getUsableScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    /**
     * Shrinks the given size to the screen area that is usable for windows.
     */
    public static Dimension shrinkToUsableScreen(@NotNull Dimension size) {
        Rectangle usableScreenBounds = getUsableScreenBounds();
        return new Dimension(Math.min(usableScreenBounds.width, size.width),
                Math.min(usableScreenBounds.height, size.height));
    }

    /**
     * Sizes the given window to fit its content but never larger than the screen area that is usable
     * for windows.
     */
    public static void packWithinUsableScreen(@NotNull Window window) {
        window.pack(); //preferred size of the content including window decorations
        window.setSize(shrinkToUsableScreen(window.getSize()));
    }

    public static synchronized void initUI() {
        // 1. Enable custom window decorations
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        // disable custom scaling on Mac because Mac is preventing it anyway.
        if (SystemUtils.IS_OS_MAC) {
            SiriusProperties.setProperty("de.unijena.bioinf.sirius.customUiScale", "false");
            SiriusProperties.setProperty("sun.java2d.uiScale", "1.0");
        }
        //override with custom scaling if enabled
        if (SiriusProperties.getBoolean("de.unijena.bioinf.sirius.customUiScale", false)) {
            String scale = SiriusProperties.getProperty("sun.java2d.uiScale");
            if (Utils.notNullOrBlank(scale))
                System.setProperty("sun.java2d.uiScale", scale);
        }

        switch (Colors.THEME()) {
            case DARK:
                try {
                    UIManager.setLookAndFeel(new FlatDarculaLaf());
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    log.error("Unsupported look and feel!", e);
                }
            case LIGHT:
            default:
                try {
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
                } catch (UnsupportedLookAndFeelException e) {
                    log.error("Error when configuring look and feel!", e);
                }
        }

        //load fonts. Run AFTER setting look-and-feel
        Fonts.initFonts();
        Colors.adjustLookAndFeel();

        //nicer times for tooltips
        ToolTipManager.sharedInstance().setInitialDelay(500);
        ToolTipManager.sharedInstance().setDismissDelay(60000);
    }

    /**
     * Gets the current UI scale factor for a given component.
     * This method should be called after the component has been added to a frame and displayed.
     *
     * @param component The component to check the scale factor for.
     * @return The scale factor (e.g., 1.0 for 100%, 2.0 for 200%). Returns 1.0 if undetected.
     */
    public static double getScaleFactor(Component component) {
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        if (gc != null) {
            // The default transform contains the scaling information
            return gc.getDefaultTransform().getScaleX();
        }
        return 1.0;
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

    public static Dimension getEffectiveScreenSize() {
        return getPreferredSizeLimitedByScreenSize(null);
    }

    public static Dimension getPreferredSizeLimitedByScreenSize(int preferredWidth, int preferredHeight) {
        return getPreferredSizeLimitedByScreenSize(new Dimension(preferredWidth, preferredHeight));
    }

    public static Dimension getPreferredSizeLimitedByScreenSize(@Nullable Dimension preferredSize) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        Rectangle screenBounds = ge.getMaximumWindowBounds();

        int maxScreenWidth = (int) Math.round(screenBounds.width * .9);
        int maxScreenHeight = (int) Math.round(screenBounds.height * .9);

        if (preferredSize == null)
            return new Dimension(maxScreenWidth, maxScreenHeight);

        return new Dimension(Math.min(preferredSize.width, maxScreenWidth), Math.min(preferredSize.height, maxScreenHeight));
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


    public static void openURLInSystemBrowserOrError(@NotNull URI url) {
        openURLInSystemBrowserOrError(url, null);
    }

    public static void openURLInSystemBrowserOrError(@NotNull URI url, @Nullable SiriusGui browserProvider) {
        openURLInSystemBrowserOrError(null, url, browserProvider);
    }

    public static void openURLInSystemBrowserOrError(@Nullable Frame owner, @NotNull URI url, @Nullable SiriusGui browserProvider) {
        try {
            openURLInSystemBrowser(owner, url, browserProvider);
        } catch (IOException e) {
            new ExceptionDialog(owner, "Error opening URL '" + url + "'. Cause: " + e.getMessage());
        }
    }

    public static void openURLInSystemBrowser(@Nullable Window owner, URI url, @Nullable SiriusGui fallbackBrowserProvider) throws IOException {
        openURL(owner, url, fallbackBrowserProvider, true);
    }

    public static void openURLInSystemBrowser(@NotNull URI url,  @Nullable SiriusGui browserProvider) throws IOException {
        openURLInSystemBrowser(null, url, browserProvider);
    }

    public static void openURLInSystemBrowser(@NotNull URI url) throws IOException {
        openURLInSystemBrowser(null, url, null);
    }

    public static void openURL(@NotNull URI url,  @Nullable SiriusGui gui, boolean useSystemBrowser) throws IOException {
        openURL(null, url, gui, useSystemBrowser);
    }

    public static void openURL(@Nullable Window owner, @NotNull URI url, SiriusGui gui, boolean trySystemBrowserFirst) throws IOException {
        openURL(owner, url, null, gui, trySystemBrowserFirst);
    }

    public static void openURL(@NotNull URI url, @Nullable String title, SiriusGui gui, boolean trySystemBrowserFirst) throws IOException {
        openURL(null, url, title, gui, trySystemBrowserFirst);
    }

    public static void openURL(@Nullable Window owner, @NotNull URI url, @Nullable String title, SiriusGui gui, boolean trySystemBrowserFirst) throws IOException {
        if (owner == null && gui != null)
            owner = gui.getMainFrame();

        if (url == null)
            if (owner instanceof JDialog dialog)
                new ExceptionDialog(dialog, "Cannot open empty URL!");
            else
                new ExceptionDialog((Frame) owner, "Cannot open empty URL!");

        if (trySystemBrowserFirst || gui == null) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(url);
                    return;
                } else {
                    log.error(
                            "Could not Open URL in System Browser. Trying SIRIUS WebView or  try visit page manually: {}", url);
                }
            } catch (Exception e) {
                log.error(
                        "Unexpected Error when opening URL in System Browser. Trying SIRIUS WebView or  try visit page manually: {}", url, e);
            }
        }


        if (gui == null)
           throw new IOException("Could not open URL in System Browser. NO fallback given!", new NullPointerException("Provider for internal browser is null!"));

        @NotNull BrowserPanelProvider<?> browserProvider = gui.getBrowserPanelProvider();

        browserProvider.newBrowserWindow(url)
                .title(title == null ? "SIRIUS WebView" : title)
                .owner(owner)
                .modality(Dialog.ModalityType.APPLICATION_MODAL)
                .size(WEB_VIEW_POPUP_SIZE)
                .resizable(false)
                .show();
    }

    /**
     * Adds a key binding to close the given dialog on pressing escape
     */
    public static void closeOnEscape(JDialog dialog) {
        closeOnEscape(dialog, dialog.getRootPane());
    }

    /**
     * Adds a key binding to close the given frame on pressing escape
     */
    public static void closeOnEscape(JFrame frame) {
        closeOnEscape(frame, frame.getRootPane());
    }

    /**
     * Adds a key binding to close the given window on pressing escape
     */
    public static void closeOnEscape(Window window, JRootPane rootPane) {
        String escapePressed = "escapePressed";
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), escapePressed);
        rootPane.getActionMap().put(escapePressed, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
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

    private static final Map<Character, String> CHAR_MAP = new HashMap<>();
    static {
        // Superscript digits
        CHAR_MAP.put('⁰', "0");
        CHAR_MAP.put('¹', "1");
        CHAR_MAP.put('²', "2");
        CHAR_MAP.put('³', "3");
        CHAR_MAP.put('⁴', "4");
        CHAR_MAP.put('⁵', "5");
        CHAR_MAP.put('⁶', "6");
        CHAR_MAP.put('⁷', "7");
        CHAR_MAP.put('⁸', "8");
        CHAR_MAP.put('⁹', "9");

        // Subscript digits
        CHAR_MAP.put('₀', "0");
        CHAR_MAP.put('₁', "1");
        CHAR_MAP.put('₂', "2");
        CHAR_MAP.put('₃', "3");
        CHAR_MAP.put('₄', "4");
        CHAR_MAP.put('₅', "5");
        CHAR_MAP.put('₆', "6");
        CHAR_MAP.put('₇', "7");
        CHAR_MAP.put('₈', "8");
        CHAR_MAP.put('₉', "9");

        // Superscript signs
        CHAR_MAP.put('⁺', "+");
        CHAR_MAP.put('⁻', "-");

        // If encountered, subscript plus/minus can also be mapped if needed
        // (not common in IUPAC names, but for completeness)
        CHAR_MAP.put('₊', "+");
        CHAR_MAP.put('₋', "-");

        // Greek letters often found in IUPAC names (approximate transliterations)
        // Lowercase:
        CHAR_MAP.put('α', "a");
        CHAR_MAP.put('β', "b");
        CHAR_MAP.put('γ', "g");
        CHAR_MAP.put('δ', "d");
        CHAR_MAP.put('ε', "e");
        CHAR_MAP.put('ζ', "z");
        CHAR_MAP.put('η', "h");
        CHAR_MAP.put('θ', "th");
        CHAR_MAP.put('κ', "k");
        CHAR_MAP.put('λ', "l");
        CHAR_MAP.put('μ', "m");
        CHAR_MAP.put('ν', "n");
        CHAR_MAP.put('ξ', "x");
        CHAR_MAP.put('ο', "o");
        CHAR_MAP.put('π', "p");
        CHAR_MAP.put('ρ', "r");
        CHAR_MAP.put('σ', "s");
        CHAR_MAP.put('ς', "s");
        CHAR_MAP.put('τ', "t");
        CHAR_MAP.put('υ', "u");
        CHAR_MAP.put('φ', "f");
        CHAR_MAP.put('χ', "ch");
        CHAR_MAP.put('ψ', "ps");
        CHAR_MAP.put('ω', "w");

        // If uppercase Greek letters are used, map them similarly if needed:
        CHAR_MAP.put('Α', "A");
        CHAR_MAP.put('Β', "B");
        CHAR_MAP.put('Γ', "G");
        CHAR_MAP.put('Δ', "D");
        CHAR_MAP.put('Ε', "E");
        CHAR_MAP.put('Ζ', "Z");
        CHAR_MAP.put('Η', "H");
        CHAR_MAP.put('Θ', "Th");
        CHAR_MAP.put('Κ', "K");
        CHAR_MAP.put('Λ', "L");
        CHAR_MAP.put('Μ', "M");
        CHAR_MAP.put('Ν', "N");
        CHAR_MAP.put('Ξ', "X");
        CHAR_MAP.put('Ο', "O");
        CHAR_MAP.put('Π', "P");
        CHAR_MAP.put('Ρ', "R");
        CHAR_MAP.put('Σ', "S");
        CHAR_MAP.put('Τ', "T");
        CHAR_MAP.put('Υ', "U");
        CHAR_MAP.put('Φ', "F");
        CHAR_MAP.put('Χ', "Ch");
        CHAR_MAP.put('Ψ', "Ps");
        CHAR_MAP.put('Ω', "W");
    }

    /**
     * Normalizes an IUPAC chemical name string by converting any superscripts, subscripts,
     * and Greek letters into their ASCII equivalents, producing a name that does not contain
     * these special Unicode characters.
     *
     * @param input The IUPAC name string to normalize.
     * @return A normalized version of the IUPAC name string.
     */
    public static String normalizeIUPACName(String input) {
        if (isNullOrBlank(input)) {
            return input;
        }

        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String replacement = CHAR_MAP.get(c);
            if (replacement != null) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    // If GuiUtils enables JDialog.setDefaultLookAndFeelDecorated(true), a new dialog is created with
    // FlatLaf window decorations (rootpane windowDecorationStyle == PLAIN_DIALOG). For this floating,
    // inline-expansion overlay we clear that decoration style so FlatLaf draws no title bar. The
    // USE_WINDOW_DECORATIONS client property alone does not undo the already-decorated rootpane;
    // resetting the standard windowDecorationStyle to NONE is what actually removes it.

    public static void setUndecorated(JFrame frame) {
        frame.setUndecorated(true);
        frame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        frame.getRootPane().putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, false);
    }

    public static void setUndecorated(JDialog dialog) {
        dialog.setUndecorated(true);
        dialog.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        dialog.getRootPane().putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, false);
    }
}
