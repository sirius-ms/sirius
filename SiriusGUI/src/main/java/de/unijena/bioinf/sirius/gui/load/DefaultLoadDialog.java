package de.unijena.bioinf.sirius.gui.load;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.gui.msview.msviewer.MSViewerPanel;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class DefaultLoadDialog extends JDialog implements LoadDialog, ActionListener, ListSelectionListener, WindowListener{
	
	private JButton add, remove, ok, abort;
	private JList<SpectrumContainer> msList;
	private MSViewerPanel msviewer;
	private JButton editCE,changeMSLevel;
	private JTextField cEField;
	private JComboBox<String> msLevelBox;
	
	private DefaultListModel<SpectrumContainer> listModel;
	
	private List<LoadDialogListener> listeners;
	
	private DecimalFormat cEFormat;
	
	private ReturnValue returnValue;
	
	private JTextField nameTF;
	private JButton nameB;

	public DefaultLoadDialog(JFrame owner){ 
		super(owner,"load",true);
		
		this.cEFormat = new DecimalFormat("#0.0");
		listeners = new ArrayList<LoadDialogListener>();
		
		this.returnValue = ReturnValue.Abort;
		this.addWindowListener(this);
		
		this.setLayout(new BorderLayout());
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(mainPanel, BorderLayout.CENTER);
		
		JPanel leftPanel = new JPanel(new BorderLayout());
//		leftPanel.setBorder(BorderFactory.createEtchedBorder());
		mainPanel.add(leftPanel,BorderLayout.WEST);
		
		listModel = new DefaultListModel<>();
		
		msList = new JList<SpectrumContainer>(listModel);
		JScrollPane msPanel = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		msPanel.setViewportView(msList);
		leftPanel.add(msPanel,BorderLayout.CENTER);
		msList.setCellRenderer(new LoadSpectraCellRenderer());
		
		msList.addListSelectionListener(this);
		
		
		
		JPanel msControls = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		add = new JButton("+");
		remove = new JButton("-");
		add.addActionListener(this);
		remove.addActionListener(this);
		remove.setEnabled(false);
		msControls.add(add);
		msControls.add(remove);
		leftPanel.add(msControls,BorderLayout.SOUTH);
		
		JPanel rightPanel = new JPanel(new BorderLayout());
		
		
		msviewer = new MSViewerPanel();
		msviewer.setData(new DummySpectrumContainer());
//		msviewer.setData(listmodel.get(0));
		rightPanel.add(msviewer,BorderLayout.CENTER);
		
//		JSpinner minspinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
//		JSpinner maxspinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
		
		JPanel propsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		JPanel cEPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		cEField = new JTextField(10);
		cEField.setEditable(false);
		cEField.setEnabled(false);
		cEPanel.add(cEField);
		cEPanel.add(new JLabel("eV"));
		
		editCE = new JButton("edit");
		editCE.setEnabled(false);
		editCE.addActionListener(this);
		cEPanel.add(editCE);
		
		cEPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"cE"));
		propsPanel.add(cEPanel);
		
		JPanel msLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		msLevelPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"ms level"));
		propsPanel.add(msLevelPanel);
		
		String[] msLevelVals = {"MS 1","MS 2"};
		
		msLevelBox = new JComboBox<>(msLevelVals);
		msLevelBox.setEnabled(false);
		msLevelPanel.add(msLevelBox);
		changeMSLevel = new JButton("change");
		changeMSLevel.addActionListener(this);
		changeMSLevel.setEnabled(false);
		msLevelPanel.add(changeMSLevel);
		
		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		namePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"experiment name"));
		nameTF = new JTextField(12);
		nameTF.setEditable(false);
//		nameTF.setText("unknown");
		namePanel.add(nameTF);
		nameB = new JButton("change");
		nameB.addActionListener(this);
		namePanel.add(nameB);
		propsPanel.add(namePanel);
		
		
		
		rightPanel.add(propsPanel,BorderLayout.SOUTH);
		
//		TODO: Namenfeld einfuegen + eV bei MS1 abschalten + Elemente richtig initialisieren wenn leer und dann Spektren hinzugefuegt
		
		mainPanel.add(rightPanel,BorderLayout.CENTER);
		
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(controlPanel,BorderLayout.SOUTH);
		
		ok = new JButton("OK");
		abort = new JButton("Abort");
		ok.addActionListener(this);
		abort.addActionListener(this);
		controlPanel.add(ok);
		controlPanel.add(abort);
		
//		this.setSize(new Dimension(800,600));
//		this.setVisible(true);
	}
	
	private void updateCETextField(){
		int index = msList.getSelectedIndex();
		if(index==-1||listModel.size()<=index){
			this.cEField.setText("");
			return;
		}
		SpectrumContainer spcont = listModel.get(index);
		if(spcont.getSpectrum().getMSLevel()==1){
			cEField.setText("");
			cEField.setEnabled(false);
			editCE.setEnabled(false);
		}else{
			cEField.setEnabled(true);
			editCE.setEnabled(true);
			CollisionEnergy ce = spcont.getSpectrum().getCollisionEnergy();
			String ceString = null;
			if(ce==null){
				ceString = "unknown";
			}else{
				double ceMin = ce.getMinEnergy();
				double ceMax = ce.getMaxEnergy();
				ceString = ceMin == ceMax ? cEFormat.format(ceMin) : cEFormat.format(ceMin) + " - " + cEFormat.format(ceMax);
			}
			cEField.setText(ceString);
		}
		
	}

	@Override
	public void newCollisionEnergy(CompactSpectrum sp) {
		msList.repaint();
		updateCETextField();
		
	}

	@Override
	public void spectraAdded(CompactSpectrum sp) {
		listModel.addElement(new SpectrumContainer(sp));
	}

	@Override
	public void spectraRemoved(CompactSpectrum sp) {
		for(int i=0;i<listModel.size();i++){
			SpectrumContainer spCont = listModel.getElementAt(i);
			if(spCont.getSpectrum().equals(sp)){
				listModel.remove(i);
				break;
			}
		}
	}

	@Override
	public void addLoadDialogListener(LoadDialogListener ldl) {
		listeners.add(ldl);
	}
	
	@Override
	public void msLevelChanged(CompactSpectrum sp) {
		msList.repaint();
		updateCETextField();
		SpectrumContainer spcont = listModel.get(msList.getSelectedIndex()); 
		if(spcont.getSpectrum().equals(sp)){
			msLevelBox.setSelectedIndex(spcont.getSpectrum().getMSLevel()-1);
		}
	}
	
	@Override
	public void showDialog(){
		this.setSize(new Dimension(950,640));
		this.setVisible(true);
	}

	/////// ActionListener /////////
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.changeMSLevel){
			SpectrumContainer spcont = listModel.get(msList.getSelectedIndex());
			for(LoadDialogListener ldl : listeners){
				ldl.changeMSLevel(spcont.getSpectrum(), msLevelBox.getSelectedIndex()+1);
			}
		}else if(e.getSource()==this.remove){
			SpectrumContainer spcont = listModel.get(msList.getSelectedIndex());
			for(LoadDialogListener ldl : listeners){
				ldl.removeSpectrum(spcont.getSpectrum());
			}
		}else if(e.getSource()==this.editCE){
			for(LoadDialogListener ldl : listeners){
				ldl.changeCollisionEnergy(listModel.get(msList.getSelectedIndex()).getSpectrum());
			}
		}else if(e.getSource()==this.add){
			for(LoadDialogListener ldl : listeners){
				ldl.addSpectra();
			}
		}else if(e.getSource()==this.ok){
			this.returnValue = ReturnValue.Success;
			this.setVisible(false);
			for(LoadDialogListener ldl : listeners){
				ldl.completeProcess();
			}
		}else if(e.getSource()==this.abort){
			this.returnValue = ReturnValue.Abort;
			this.setVisible(false);
			for(LoadDialogListener ldl : listeners){
				ldl.abortProcess();
			}
		}else if(e.getSource()==this.nameB){
			ExperimentNameDialog diag = new ExperimentNameDialog(this, nameTF.getText());
			if(diag.getReturnValue()==ReturnValue.Success){
				String newName = diag.getNewName();
				if(newName!=null && !newName.isEmpty()){
					System.out.println(newName);
					for(LoadDialogListener ldl : listeners){
						ldl.experimentNameChanged(newName);
					}
				}
			}
		}
		
	}

	////// ListSelectionListener ////////
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		updateCETextField();
//		System.out.println(msList.getSelectedIndex());
		if(msList.getSelectedIndex()<0){
//			this.cEField.setText("");
			this.msviewer.setData(new DummySpectrumContainer());
			this.msviewer.repaint();
			this.msLevelBox.setEnabled(false);
			this.changeMSLevel.setEnabled(false);
			this.remove.setEnabled(false);
			return;
		}else{
			this.remove.setEnabled(true);
		}
		SpectrumContainer spcont = listModel.get(msList.getSelectedIndex()); 
		msviewer.setData(spcont);
		msviewer.repaint();
//		CollisionEnergy ce = spcont.getSpectrum().getCollisionEnergy();
//		double ceMin = ce.getMinEnergy();
//		double ceMax = ce.getMaxEnergy();
//		String ceString = ceMin == ceMax ? cEFormat.format(ceMin) : cEFormat.format(ceMin) + " - " + cEFormat.format(ceMax); 
//		cEField.setText(ceString);
//		System.out.println(spcont.getSpectrum().getMSLevel());
		msLevelBox.setEnabled(true);
		changeMSLevel.setEnabled(true);
		msLevelBox.setSelectedIndex(spcont.getSpectrum().getMSLevel()-1);
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		for(LoadDialogListener ldl : listeners){
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
	
}
