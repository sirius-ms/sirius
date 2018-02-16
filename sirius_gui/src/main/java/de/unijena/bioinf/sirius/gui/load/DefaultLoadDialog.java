package de.unijena.bioinf.sirius.gui.load;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanel;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;
import de.unijena.bioinf.sirius.gui.utils.ExperimentEditPanel;
import de.unijena.bioinf.sirius.gui.utils.TextHeaderBoxPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DefaultLoadDialog extends JDialog implements LoadDialog, ActionListener, ListSelectionListener, WindowListener,
        DropTargetListener, MouseListener {

    private JButton add, remove, ok, abort;

    private JList<SpectrumContainer> msList;
    ExperimentEditPanel editPanel;

    private MSViewerPanel msviewer;
    private JButton editCE;
    private JTextField cEField;
    private JComboBox<String> msLevelBox;


    private List<LoadDialogListener> listeners;

    private DecimalFormat cEFormat;

    JPopupMenu spPopMenu;
    JMenuItem addMI, removeMI;

    private static Pattern NUMPATTERN = Pattern.compile("^[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?$");

    public DefaultLoadDialog(JFrame owner, EventList<SpectrumContainer> list) {
        super(owner, "load", true);

        this.cEFormat = new DecimalFormat("#0.0");
        listeners = new ArrayList<LoadDialogListener>();

        this.addWindowListener(this);

        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        this.add(mainPanel, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.WEST);

        msList = new JList<>(GlazedListsSwing.eventListModel(list));
        msList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane msPanel = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        msPanel.setViewportView(msList);
        leftPanel.add(msPanel, BorderLayout.CENTER);
        msList.setCellRenderer(new LoadSpectraCellRenderer());

        msList.addListSelectionListener(this);
        msList.addMouseListener(this);


        JPanel msControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        add = Buttons.getAddButton16();
        remove = Buttons.getRemoveButton16();
        add.addActionListener(this);
        remove.addActionListener(this);
        remove.setEnabled(false);
        msControls.add(add);
        msControls.add(remove);
        leftPanel.add(msControls, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());

        msviewer = new MSViewerPanel();
        msviewer.setData(new DummySpectrumContainer());
        rightPanel.add(msviewer, BorderLayout.CENTER);


        //edit panel
        editPanel = new ExperimentEditPanel();
        list.addListEventListener(listChanges -> {
            editPanel.precursorSelection.setData(list, editPanel.getSelectedIonMass());
        });

        String[] msLevelVals = {"MS 1", "MS 2"};
        msLevelBox = new JComboBox<>(msLevelVals);
        msLevelBox.setEnabled(false);
        msLevelBox.addActionListener(this);
        editPanel.add(new TextHeaderBoxPanel("MS level", msLevelBox));

        JPanel cEPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        cEField = new JTextField(10);
        cEField.setEditable(false);
        cEField.setEnabled(false);
        cEPanel.add(cEField);

        editCE = new JButton("Change");
        editCE.setEnabled(false);
        editCE.addActionListener(this);
        cEPanel.add(editCE);
        editPanel.add(new TextHeaderBoxPanel("Collision energy (eV)", cEPanel));


        rightPanel.add(editPanel, BorderLayout.SOUTH);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        this.add(controlPanel, BorderLayout.SOUTH);

        ok = new JButton("OK");
        abort = new JButton("Abort");
        ok.addActionListener(this);
        abort.addActionListener(this);
        controlPanel.add(ok);
        controlPanel.add(abort);

        {
            InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
            KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
            String enterAction = "load";
            String escAction = "abort";
            inputMap.put(enterKey, enterAction);
            inputMap.put(escKey, escAction);
            getRootPane().getActionMap().put(enterAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    load();
                }
            });
            getRootPane().getActionMap().put(escAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    abort();
                }
            });
        }


        DropTarget dropTarget = new DropTarget(msList, this);
        constructSpectraListPopupMenu();
    }

    @Override
    public void setParentMass(double ionMass) {
        editPanel.setSelectedIonMass(ionMass);
    }

    public void constructSpectraListPopupMenu() {
        spPopMenu = new JPopupMenu();
        addMI = new JMenuItem("Add experiment(s)", Icons.LIST_ADD_16);
        removeMI = new JMenuItem("Remove experiment(s)", Icons.LIST_REMOVE_16);

        addMI.addActionListener(this);
        removeMI.addActionListener(this);

        removeMI.setEnabled(false);

        spPopMenu.add(addMI);
        spPopMenu.add(removeMI);
    }

    private void updateCETextField() {
        final SpectrumContainer spcont = msList.getSelectedValue();
        if (spcont == null) {
            this.cEField.setText("");
            this.cEField.setEnabled(false);
            this.editCE.setEnabled(false);
            return;
        }


        if (spcont.getSpectrum().getMsLevel() == 1) {
            cEField.setText("");
            cEField.setEnabled(false);
            editCE.setEnabled(false);
        } else {
            cEField.setEnabled(true);
            editCE.setEnabled(true);
            CollisionEnergy ce = spcont.getSpectrum().getCollisionEnergy();
            String ceString = null;
            if (ce == null) {
                ceString = "unknown";
            } else {
                double ceMin = ce.getMinEnergy();
                double ceMax = ce.getMaxEnergy();
                ceString = ceMin == ceMax ? cEFormat.format(ceMin) : cEFormat.format(ceMin) + " - " + cEFormat.format(ceMax);
            }
            cEField.setText(ceString);
        }

    }

    @Override
    public void newCollisionEnergy(SpectrumContainer container) {
        msList.repaint();
        updateCETextField();
    }

    @Override
    public void ionizationChanged(PrecursorIonType ionization) {
        if (ionization != null) {
            String name = ionization.toString();
            if (name != null) {
                editPanel.ionizationCB.setSelectedItem(name);
            }
        }
    }

    @Override
    public PrecursorIonType getIonization() {
        return editPanel.getSelectedIonization();
    }


    @Override
    public void addLoadDialogListener(LoadDialogListener ldl) {
        listeners.add(ldl);
    }

    @Override
    public void msLevelChanged(SpectrumContainer container) {
        msList.repaint();
        updateCETextField();
        SpectrumContainer spcont = msList.getSelectedValue();
        //check if the active spectrum has to be actualized
        if (spcont.getSpectrum().equals(container)) {
            msLevelBox.setSelectedIndex(spcont.getSpectrum().getMsLevel() - 1);
        }
    }

    @Override
    public void showDialog() {
        pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    private void load() {
        if (!editPanel.validateFormula()) {
            new ExceptionDialog(this, "The molecular formula is invalid. Please specify a valid molecular formula or leave the field empty if the molecular formula is unknown.");
            return;
        }
        this.setVisible(false);
        for (LoadDialogListener ldl : listeners) {
            ldl.completeProcess();
        }
    }


    private void abort() {
        this.setVisible(false);
        for (LoadDialogListener ldl : listeners) {
            ldl.abortProcess();
        }
    }

    /////// ActionListener /////////

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.msLevelBox) {
            SpectrumContainer spCont = msList.getSelectedValue();
            int msLevel = msLevelBox.getSelectedIndex() + 1;
            if (spCont.getSpectrum().getMsLevel() != msLevel) {
                for (LoadDialogListener ldl : listeners) {
                    ldl.changeMSLevel(spCont, msLevelBox.getSelectedIndex() + 1);
                }
            }
        } else if (e.getSource() == this.remove || e.getSource() == this.removeMI) {
            for (LoadDialogListener ldl : listeners) {
                ldl.removeSpectra(msList.getSelectedValuesList());
            }
        } else if (e.getSource() == this.editCE) {
            for (LoadDialogListener ldl : listeners) {
                ldl.changeCollisionEnergy(msList.getSelectedValue());
            }
        } else if (e.getSource() == this.add || e.getSource() == this.addMI) {
            for (LoadDialogListener ldl : listeners) {
                ldl.addSpectra();
            }
        } else if (e.getSource() == this.ok) {
            load();
        } else if (e.getSource() == this.abort) {
            abort();
        }
    }

    ////// ListSelectionListener ////////

    @Override
    public void valueChanged(ListSelectionEvent e) {

        int[] indices = msList.getSelectedIndices();
        if (indices.length <= 1) {
            updateCETextField();
            if (msList.getSelectedIndex() < 0) {
//				this.cEField.setText("");
                this.msviewer.setData(new DummySpectrumContainer());
                this.msviewer.repaint();
                this.msLevelBox.setEnabled(false);
//				this.changeMSLevel.setEnabled(false);
                this.remove.setEnabled(false);
                this.removeMI.setEnabled(false);
                return;
            } else {
                this.remove.setEnabled(true);
                this.removeMI.setEnabled(true);
            }
            SpectrumContainer spcont = msList.getSelectedValue();
            msviewer.setData(spcont);
            msviewer.repaint();
            msLevelBox.setEnabled(true);
            msLevelBox.setSelectedIndex(spcont.getSpectrum().getMsLevel() - 1);
        } else {
            this.cEField.setText("");
            this.cEField.setEnabled(false);
            this.editCE.setEnabled(false);
            this.msLevelBox.setEnabled(false);
        }

    }

    @Override
    public void windowOpened(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowClosing(WindowEvent e) {
        for (LoadDialogListener ldl : listeners) {
            ldl.abortProcess();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void windowIconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowActivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void experimentNameChanged(String name) {
        editPanel.nameTF.setText(name);
    }

    @Override
    public String getExperimentName() {
        return editPanel.nameTF.getText();
    }

    //todo error handling
    @Override
    public double getParentMass() throws NumberFormatException {
        return editPanel.getSelectedIonMass();
    }

    /// drag and drop support...

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // TODO Auto-generated method stub

    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        final List<File> newFiles = DragAndDrop.getFileListFromDrop(dtde);
        for (LoadDialogListener li : listeners) {
            li.addSpectra(newFiles);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.spPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            this.spPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}
