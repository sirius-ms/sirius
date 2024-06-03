package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

public class ToggableSidePanel extends JPanel {

    private final String name;
    private final JButton button;
    private final JComponent content;
    private final Action show,hide;

    protected boolean hidden;

    public static final String SOUTH=BorderLayout.SOUTH,EAST=BorderLayout.EAST,
            WEST=BorderLayout.WEST,NORTH=BorderLayout.NORTH;

    public ToggableSidePanel(String name, JComponent content) {
        this.name = name;
        this.content = content;
        this.hidden = false;
        this.show = new Show();
        this.hide = new Hide();

        this.button = new RotatedButton(hide);
        button.setFont(button.getFont().deriveFont(Font.BOLD));

        setLayout(new BorderLayout());
        add(button);
        add(content, BorderLayout.CENTER);
    }

    private class Show extends AbstractAction {

        public Show() {
            super("Show " + name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            button.setAction(hide);
            content.setVisible(true);
            invalidate();
        }
    }
    private class Hide extends AbstractAction {

        public Hide() {
            super("Hide " + name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            button.setAction(show);
            content.setVisible(false);
            invalidate();
        }
    }

    // WTF? Why is Swing not supporting this?
    private static class RotatedButton extends JButton {
        public RotatedButton(Action a) {
            super(a);
            this.setHideActionText(true); // we draw the text ourself
            addPropertyChangeListener("action", (event)->repaint());
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            // add text
            final String value = (String)getAction().getValue(Action.NAME);
            final Rectangle bounds = getBounds();
            final Rectangle2D stringBounds = g.getFontMetrics(getFont()).getStringBounds(value, g);
            // draw string in the middle of the button
            int x = (int)(bounds.width-stringBounds.getHeight())/2;
            int y = (int)(bounds.height-stringBounds.getWidth())/2;
            x += (int)stringBounds.getHeight();
            y += (int)stringBounds.getWidth()-2;
            g.translate(x,y);
            ((Graphics2D)g).rotate(-Math.PI/2d);
            g.drawString(value, 0,0);

        }
    }


}
