package de.unijena.bioinf.sirius.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;

import javax.swing.*;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SwingUtils {

    public static Color ICON_BLUE = new Color(17,145,187);
    public static Color ICON_GREEN = new Color(0,191,48);
    public static Color ICON_RED = new Color(204,71,41);
    public static Color ICON_YELLOW = new Color(255,204,0);


    //ICONS
    public static final Icon ADD_DOC_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-add-doc-16px.png"));
    public static final Icon BATCH_DOC_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-documents-16px.png"));
    public static final Icon REMOVE_DOC_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-remove-doc-16px.png"));
    public static final Icon EDIT_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-edit-16px.png"));
    public static final Icon EXPORT_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-export-16px.png"));
    public static final Icon EXPORT_24 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-export-24px.png"));
    public static final Icon EXPORT_20 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-export-20px.png"));
    public static final Icon EXPORT_32 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-export@0.5x.png"));
    public static final Icon EXPORT_48 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-export-48px.png"));
    public static final Icon RUN_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-controls-play-16px.png"));
    public static final Icon RUN_32 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-controls-play@0.5x.png"));
    public static final Icon RUN_64 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-controls-play.png"));
    public static final Icon NO_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-no-16px.png"));
    public static final Icon LIST_ADD_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-list-add-16px.png"));
    public static final Icon LIST_REMOVE_16 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-list-remove-16px.png"));
    public static final Icon Zoom_In_24 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-magnify-plus-24px.png"));
    public static final Icon Zoom_In_20 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-magnify-plus-20px.png"));
    public static final Icon Zoom_Out_24 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-magnify-minus-24px.png"));
    public static final Icon Zoom_Out_20 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-magnify-minus-20px.png"));
    public static final Icon FILTER_UP_24 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-filter-up-24px.png"));
    public static final Icon FILTER_DOWN_24 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-filter-down-24px.png"));
    public static final Icon FINGER_32 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-fingerprint@0.5x.png"));
    public static final Icon FINGER_64 = new ImageIcon(SwingUtils.class.getResource("/icons/circular-icons/c-fingerprint.png"));

public static void initUI(){
    ToolTipManager.sharedInstance().setInitialDelay(250);
    Painter painter = new ProgressPainter(Color.WHITE, ICON_GREEN);
    UIManager.put("ProgressBar[Enabled].foregroundPainter", painter);
    UIManager.put("ProgressBar[Enabled+Finished].foregroundPainter", painter);
}

    public static JButton getExportButton24(String tootip){
        return new IconButton(tootip,EXPORT_24);
    }

    public static JButton getExportButton20(String tootip){
        return new IconButton(tootip,EXPORT_20);
    }

    public static JButton getZoomInButton20(){
        return new IconButton("Zoom in",Zoom_In_20);
    }

    public static JButton getZoomOutButton20(){
        return new IconButton("Zoom out",Zoom_Out_20);
    }


    public static class IconButton extends JButton{
        public IconButton(String text, Icon icon) {
            super(icon);
            setToolTipText(text);
            setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
//            setContentAreaFilled(false);
//            setRolloverEnabled(true);
        }
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


}
