package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;

public class VerticalJLabel extends JLabel {

    public VerticalJLabel(String text) {
        super(text);
    }

    @Override
    public void setText(String text) {
        // Convert the string to vertical by inserting <br> between each character
        StringBuilder verticalText = new StringBuilder("<html>");
        for (char c : text.toCharArray()) {
            verticalText.append(c).append("<br>");
        }
        verticalText.append("</html>");

        super.setText(verticalText.toString());
    }
}
