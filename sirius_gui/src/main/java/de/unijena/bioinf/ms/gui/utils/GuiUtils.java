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
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
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
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.isNullOrBlank;


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

    public static void openURL(@NotNull URI url,  @Nullable SiriusGui browserProvider, boolean useSystemBrowser) throws IOException {
        openURL(null, url, browserProvider, useSystemBrowser);
    }

    public static void openURL(@Nullable Window owner, @NotNull URI url, SiriusGui browserProvider, boolean trySystemBrowserFirst) throws IOException {
        openURL(owner, url, null, browserProvider, trySystemBrowserFirst);
    }

    public static void openURL(@NotNull URI url, @Nullable String title, SiriusGui browserProvider, boolean trySystemBrowserFirst) throws IOException {
        openURL(null, url, title, browserProvider, trySystemBrowserFirst);
    }

    public static void openURL(@Nullable Window owner, @NotNull URI url, @Nullable String title, SiriusGui browserProvider, boolean trySystemBrowserFirst) throws IOException {
        if (owner == null && browserProvider != null)
            owner = browserProvider.getMainFrame();

        if (url == null)
            if (owner instanceof JDialog dialog)
                new ExceptionDialog(dialog, "Cannot open empty URL!");
            else
                new ExceptionDialog((Frame) owner, "Cannot open empty URL!");

        if (trySystemBrowserFirst || browserProvider == null) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(url);
                    return;
                } else {
                    LoggerFactory.getLogger(GuiUtils.class).error(
                            "Could not Open URL in System Browser. Trying SIRIUS WebView or  try visit page manually: {}", url);
                }
            } catch (Exception e) {
                LoggerFactory.getLogger(GuiUtils.class).error(
                        "Unexpected Error when opening URL in System Browser. Trying SIRIUS WebView or  try visit page manually: {}", url, e);
            }
        }

        if (browserProvider == null)
           throw new IOException("Could not open URL in System Browser. NO fallback given!", new NullPointerException("Provider for internal browser is null!"));

        if (owner instanceof JDialog dialog)
            browserProvider.newBrowserPopUp(dialog, title == null ? "SIRIUS WebView" : title, url);
        else if (owner instanceof JFrame frame)
            browserProvider.newBrowserPopUp(frame, title == null ? "SIRIUS WebView" : title, url);
        else
            browserProvider.newBrowserPopUp(title == null ? "SIRIUS WebView" : title, url);

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
}
