package de.unijena.bioinf.sirius.gui.mainframe.results;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 31.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ButtoGroupTabPane extends JPanel {
    FormulaTable formulaTable;
    JPanel buttonPane;
    JPanel centerPane;
    List<JToggleButton> buttons;
    List<Action> actions;

    public ButtoGroupTabPane(FormulaTable menu) {
        super(new BorderLayout());
        this.formulaTable = menu;
        buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER,-10,0));


        add(buttonPane,BorderLayout.NORTH);
        add(centerPane,BorderLayout.CENTER);

        //A toggle button without any caption or icon
        ButtonGroup bg = new ButtonGroup();

        JToggleButton overviewB = new JToggleButton("Overview");
        int w = 200;
        Dimension d = new Dimension(w,overviewB.getPreferredSize().height);
        overviewB.setPreferredSize(d);
        bg.add(overviewB);
        bg.setSelected(overviewB.getModel(),true);

        JToggleButton specB = new JToggleButton("Spectra");
        specB.setPreferredSize(d);
        bg.add(specB);

        JToggleButton treeB = new JToggleButton("Trees");
        treeB.setPreferredSize(d);
        bg.add(treeB);

        JToggleButton csiB = new JToggleButton("CSI:FingerId");
        csiB.setPreferredSize(d);
        bg.add(csiB);


        JPanel buttonList = new JPanel(new FlowLayout(FlowLayout.CENTER,-10,1));
        buttonList.add(Box.createHorizontalGlue());
        buttonList.add(overviewB);
        buttonList.add(specB);
        buttonList.add(treeB);
        buttonList.add(csiB);
        buttonList.add(Box.createHorizontalGlue());
        buttonList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(),"TestTitle"));

        add(buttonList,BorderLayout.NORTH);
    }

    public void add(String title, JComponent toAdd, boolean showNorth){
        if (showNorth){
            JPanel tmp =  new JPanel(new BorderLayout());
            //todo finish me
        }else{

        }
    }
}
