package de.unijena.bioinf.ms.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
import java.awt.*;
import java.util.List;
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
        //load nimbus look and feel, before mainframe is built
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        UIManager.put("nimbusOrange", Colors.ICON_GREEN);

//        ToolTipManager.sharedInstance().setInitialDelay(250);


    }

    public static void drawListStatusElement(boolean isComputing, Graphics2D g2, Component c) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        final Color prevCol = g2.getColor();
        String icon = isComputing ? "\u2699" :"";


        /*switch (state) {
            case COMPUTING:
                icon = "\u2699";
                break;
            case COMPUTED:
                g2.setColor(Colors.ICON_GREEN);
                icon = "\u2713";
                break;
            case QUEUED:
                icon = "...";
                break;
            case FAILED:
                g2.setColor(Colors.ICON_RED);
                icon = "\u2718";
                break;
            default:
                icon = "";
        }*/

        int offset = g2.getFontMetrics().stringWidth(icon);
        g2.drawString(icon, c.getWidth() - offset - 10, c.getHeight() - 8);
        g2.setColor(prevCol);
    }

    public static class SimplePainter extends AbstractRegionPainter {

        private Color fillColor;

        public SimplePainter(Color color) {
            // as a slight visual improvement, make the color transparent
            // to at least see the background gradient
            // the default progressBarPainter does it as well (plus a bit more)
            fillColor = new Color(
                    color.getRed(), color.getGreen(), color.getBlue(), 156);
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int width,
                               int height, Object[] extendedCacheKeys) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fillColor);
            g.fillRect(0, 0, width, height);
        }

        @Override
        protected PaintContext getPaintContext() {
            return null;
        }

    }

    public static class ProgressPainter implements Painter {

        private Color light, dark;
        private GradientPaint gradPaint;

        public ProgressPainter(Color light, Color dark) {
            this.light = light;
            this.dark = dark;
        }

        @Override
        public void paint(Graphics2D g, Object c, int w, int h) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gradPaint = new GradientPaint((w / 2.0f), 0, light, (w / 2.0f), (h / 2.0f), dark, true);
            g.setPaint(gradPaint);
            g.fillRect(2, 2, (w - 5), (h - 5));

            Color outline = new Color(0, 85, 0);
            g.setColor(outline);
            g.drawRect(2, 2, (w - 5), (h - 5));
            Color trans = new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), 100);
            g.setColor(trans);
            g.drawRect(1, 1, (w - 3), (h - 3));
        }
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
                + lines.stream().map(it -> it.replace("\n", "<br>")).collect(Collectors.joining("<br>"))
                + "</p></html>";
    }
}
