package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.msviewer.MSViewerPanel;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class DefaultLoadDialog extends JDialog implements LoadDialog, ActionListener, ListSelectionListener, WindowListener,
        DropTargetListener, MouseListener {

    private JButton add, remove, ok, abort;

    private JList<SpectrumContainer> msList;
    private DefaultListModel<SpectrumContainer> listModel;

    private MSViewerPanel msviewer;
    private JButton editCE;
    private JTextField cEField;
    private JTextField parentMzField;
    private JComboBox<String> msLevelBox;

    private JComboBox<String> ionizationCB;


    private List<LoadDialogListener> listeners;

    private DecimalFormat cEFormat;

    private JTextField nameTF;

    JPopupMenu spPopMenu;
    JMenuItem addMI, removeMI;

    private static Pattern NUMPATTERN = Pattern.compile("^[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?$");

    public DefaultLoadDialog(JFrame owner) {
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

        listModel = new DefaultListModel<>();

        msList = new JList<SpectrumContainer>(listModel);
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

        JPanel propsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JPanel msLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        msLevelPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "MS level"));
        propsPanel.add(msLevelPanel);

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        namePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Compound name"));
        nameTF = new JTextField(12);
        nameTF.setEditable(true);
        namePanel.add(nameTF);
        propsPanel.add(namePanel);

        JPanel cEPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        cEPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Ion"));


        Vector<String> ionizations = new Vector<>(PeriodicTable.getInstance().getIonizationsAndUnknowns());
        Collections.sort(ionizations);
        ionizationCB = new JComboBox<>(ionizations);
        ionizationCB.setSelectedItem(PeriodicTable.getInstance().unknownPositivePrecursorIonType().getIonization().getName());


        cEPanel.add(ionizationCB);
        propsPanel.add(cEPanel);

        cEPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        cEField = new JTextField(10);
        cEField.setEditable(false);
        cEField.setEnabled(false);
        cEPanel.add(cEField);
        cEPanel.add(new JLabel("eV"));

        editCE = new JButton("Edit");
        editCE.setEnabled(false);
        editCE.addActionListener(this);
        cEPanel.add(editCE);

        cEPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "collision energy (optional)"));

        cEPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        cEPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "parent mass"));

        parentMzField = new JTextField(12);
        parentMzField.setEditable(true);
        parentMzField.setEnabled(true);
        parentMzField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                JTextField tf = (JTextField) input;
                final String text = tf.getText().trim();
                return NUMPATTERN.matcher(text).matches();
            }
        });
        cEPanel.add(parentMzField);
        cEPanel.add(new JLabel("m/z"));

        propsPanel.add(cEPanel);

        String[] msLevelVals = {"MS 1", "MS 2"};

        msLevelBox = new JComboBox<>(msLevelVals);
        msLevelBox.setEnabled(false);
        msLevelBox.addActionListener(this);
        msLevelPanel.add(msLevelBox);


        rightPanel.add(propsPanel, BorderLayout.SOUTH);
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


        DropTarget dropTarget = new DropTarget(this, this);
        constructSpectraListPopupMenu();
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
        int index = msList.getSelectedIndex();
        if (index == -1 || listModel.size() <= index) {
            this.cEField.setText("");
            this.cEField.setEnabled(false);
            this.editCE.setEnabled(false);
            return;
        }
        SpectrumContainer spcont = listModel.get(index);
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
            String name = ionization.getIonization().getName();
            if (name != null)
                ionizationCB.setSelectedItem(name);
        }
    }

    @Override
    public PrecursorIonType getIonization() {
        String item = (String) ionizationCB.getSelectedItem();
        if (item != null)
            return PeriodicTable.getInstance().ionByName(item);
        return PeriodicTable.getInstance().getUnknownPrecursorIonType(1);
    }


    @Override
    public void addLoadDialogListener(LoadDialogListener ldl) {
        listeners.add(ldl);
    }

    @Override
    public void msLevelChanged(SpectrumContainer container) {
        msList.repaint();
        updateCETextField();
        SpectrumContainer spcont = listModel.get(msList.getSelectedIndex());
        //check if the active spectrum has to be actualized
        if (spcont.getSpectrum().equals(container)) {
            msLevelBox.setSelectedIndex(spcont.getSpectrum().getMsLevel() - 1);
        }
    }

    @Override
    public void showDialog() {
        this.setSize(new Dimension(950, 640));
        setLocationRelativeTo(getParent());
        this.setVisible(true);
    }

    private void load() {
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
            SpectrumContainer spCont = listModel.get(msList.getSelectedIndex());
            int msLevel = msLevelBox.getSelectedIndex() + 1;
            if (spCont.getSpectrum().getMsLevel() != msLevel) {
                for (LoadDialogListener ldl : listeners) {
                    ldl.changeMSLevel(spCont, msLevelBox.getSelectedIndex() + 1);
                }
            }
        } else if (e.getSource() == this.remove || e.getSource() == this.removeMI) {
            int[] indices = msList.getSelectedIndices();
            List<SpectrumContainer> conts = new ArrayList<SpectrumContainer>();
            for (int index : indices) {
                conts.add(listModel.get(index));
            }
            for (SpectrumContainer spcont : conts) {
                for (LoadDialogListener ldl : listeners) {
                    ldl.removeSpectrum(spcont);
                }
            }
        } else if (e.getSource() == this.editCE) {
            for (LoadDialogListener ldl : listeners) {
                ldl.changeCollisionEnergy(listModel.get(msList.getSelectedIndex()));
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
            SpectrumContainer spcont = listModel.get(msList.getSelectedIndex());
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
        nameTF.setText(name);
    }

    @Override
    public String getExperimentName() {
        return nameTF.getText();
    }

    @Override
    public void parentMassChanged(double newMz) {
        parentMzField.setText(String.valueOf(newMz));
    }

    //todo error handling
    @Override
    public double getParentMass() throws NumberFormatException {
        if (NUMPATTERN.matcher(parentMzField.getText()).matches()) {
            return Double.parseDouble(parentMzField.getText());
        }
        return -1d;
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
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable tr = dtde.getTransferable();
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        List<File> newFiles = new ArrayList<File>();
        try {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].isFlavorJavaFileListType()) {
                    List files = (List) tr.getTransferData(flavors[i]);
                    for (Object o : files) {
                        File file = (File) o;
                        newFiles.add(file);
                    }
                }
                dtde.dropComplete(true);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            dtde.rejectDrop();
        }

        DataFormatIdentifier identifier = new DataFormatIdentifier();
        List<File> acceptedFiles = new ArrayList<>(newFiles.size());
        for (File file : newFiles) {
            if (identifier.identifyFormat(file) != DataFormat.NotSupported) {
                acceptedFiles.add(file);
            }
        }

        if (acceptedFiles.size() > 0) {
            for (LoadDialogListener li : listeners) {
                li.addSpectra(acceptedFiles);
            }
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

    @Override
    public DefaultListModel<SpectrumContainer> getSpectra() {
        return listModel;
    }
}
