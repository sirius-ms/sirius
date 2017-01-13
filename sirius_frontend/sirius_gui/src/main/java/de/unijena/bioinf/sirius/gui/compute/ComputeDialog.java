package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.myxo.structure.CompactPeak;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactPeak;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.NoConnectionDialog;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

public class ComputeDialog extends JDialog implements ActionListener {

	private JButton compute, abort, autoDetectFM, expFM;
	private JComboBox<CompactPeak> box;
	private Vector<CompactPeak> masses;
	private MyListCellRenderer renderer;

	private JButton elementAutoDetect;
	private ElementsPanel elementPanel;
	private SearchProfilePanel searchProfilePanel;
	private MainFrame owner;
	

	private boolean success;
	private ExperimentContainer ec;

//	private JToggleButton enableFinger;

	public ComputeDialog(MainFrame owner,ExperimentContainer ec) {
		super(owner,"compute",true);
		this.owner = owner;
		this.ec = ec;
		this.success = false;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
//		additionalElements = new TreeSet<>();
		
		this.setLayout(new BorderLayout());
		/*JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER));
		north.add(new JLabel(SwingUtils.RUN_64));
		add(north,BorderLayout.NORTH);*/
		

		Box mainPanel = Box.createVerticalBox();
//		JPanel mainPanel = new JPanel(new BorderLayout());
		this.add(mainPanel,BorderLayout.CENTER);
		
		JPanel focMassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		
		masses = new Vector<>();
		double maxInt = -1;
		Object maxObj = null;
		List<CompactSpectrum> ms1Spectra = ec.getMs1Spectra();
		// falls MS1 verfügbar biete MS1 Peaks an, ansonsten nehme MS2 und normalisiere global
		boolean useMS1;
		CompactPeak bestDataIon = null;
		final Deviation dev = new Deviation(10);
		final double focusedMass = ec.getDataFocusedMass();
		if(!ms1Spectra.isEmpty()){
			useMS1 = true;
			CompactSpectrum sp = ms1Spectra.get(0);
			for(int i=0;i<sp.getSize();i++){
				if(sp.getPeak(i).getAbsoluteIntensity()>maxInt){
					maxInt = sp.getPeak(i).getAbsoluteIntensity();
					maxObj = sp.getPeak(i);
				}
				if (focusedMass> 0 && dev.inErrorWindow(sp.getPeak(i).getMass(), focusedMass)) {
					if (bestDataIon == null || sp.getPeak(i).getAbsoluteIntensity() > bestDataIon.getAbsoluteIntensity())
						bestDataIon = sp.getPeak(i);
				}
				masses.add(sp.getPeak(i));
			}
		}else{
			useMS1 = false;
			for(CompactSpectrum sp : ec.getMs2Spectra()){
				for(int i=0;i<sp.getSize();i++){
					if(sp.getPeak(i).getAbsoluteIntensity()>maxInt){
						maxInt = sp.getPeak(i).getAbsoluteIntensity();
						maxObj = sp.getPeak(i);
					}
					masses.add(sp.getPeak(i));
					if (focusedMass> 0 && dev.inErrorWindow(sp.getPeak(i).getMass(), focusedMass)) {
						if (bestDataIon == null || sp.getPeak(i).getAbsoluteIntensity() > bestDataIon.getAbsoluteIntensity())
							bestDataIon = sp.getPeak(i);
					}
				}
			}
		}
		if (bestDataIon != null) masses.add(bestDataIon);
		box = new JComboBox<>(masses);
		
//		box.setEditor(anEditor);
		box.setEditable(true);
		renderer = new MyListCellRenderer(masses);
		box.setRenderer(renderer);
//		box.setSelectedItem(String.valueOf(ec.getFocusedMass()));
		AutoCompleteDecorator.decorate(box,new ObjectToStringConverter() {
			@Override
			public String getPreferredStringForItem(Object item) {
				if(item instanceof CompactPeak){
					CompactPeak peak = (CompactPeak) item;
//					return peak.getMass()+" "+peak.getAbsoluteIntensity();
					return String.valueOf(peak.getMass());
				}else{
					return (String) item;
				}
				
			}
		});
		focMassPanel.add(box);
		focMassPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Parent mass"));
		
		
		
		/*
		 * Was abgefragt werden muss:
		 * 
		 * foc. mass
		 * Ionisierung
		 * seltene Elemente abseits von CHNOPS Br, B, Cl, Se, F, I
		 * 
		 */
		
		
//		msa = new MassSearchable(ec);
//		autoComboBox = new AutocompleteJComboBox(msa);
//		focMassPanel.add(autoComboBox);
		autoDetectFM = new JButton("Most intensive peak");
		autoDetectFM.addActionListener(this);
		if(masses.isEmpty()) autoDetectFM.setEnabled(false);
		expFM = new JButton("File value");
		expFM.addActionListener(this);
		if(ec.getDataFocusedMass()<=0) {
			expFM.setEnabled(false);
			if(masses.isEmpty()){
				box.setSelectedItem("");
			}else{
				box.setSelectedItem(maxObj);
			}
		}
		else if (bestDataIon!=null) {
			box.setSelectedItem(bestDataIon);
		} else {
			box.setSelectedItem(String.valueOf(focusedMass));
		}
		
		focMassPanel.add(autoDetectFM);
		focMassPanel.add(expFM);
		mainPanel.add(focMassPanel,BorderLayout.NORTH);
		
		/////////////////////////////////////////////
		elementPanel = new ElementsPanel(this);
		mainPanel.add(elementPanel);

        elementPanel.add(Box.createHorizontalGlue());
		elementPanel.add(Box.createVerticalGlue());

		elementAutoDetect = new JButton("Auto detect");
		elementAutoDetect.addActionListener(this);
		elementAutoDetect.setEnabled(true);
		elementPanel.add(elementAutoDetect);



		/////////////////////////////////////////////

		searchProfilePanel = new SearchProfilePanel(this, ec.getIonization());
		searchProfilePanel.formulaCombobox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
                    enableElementSelection(searchProfilePanel.formulaCombobox.getSelectedIndex()==0);
			}
		});
		mainPanel.add(searchProfilePanel);


		//todo fingerid integration
		/*JPanel fingeridPanel = new JPanel(new BorderLayout());
		fingeridPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"CSI:FingerID"));
		Box fingeridCenter =  Box.createVerticalBox();

		enableFinger =  new ToolbarToggleButton("CSI:FingerID",SwingUtils.FINGER_64, "Perform online candidate search using CSI:FingerID");
		fingeridPanel.add(enableFinger,BorderLayout.WEST);
		mainPanel.add(fingeridPanel);*/








		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		this.add(southPanel,BorderLayout.SOUTH);

		if (ec.getMs2Spectra().size()==0){
			JLabel label = new JLabel("No MS2 data provided. Identify by isotope pattern.");
			Font font = label.getFont();
			Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
			label.setFont(boldFont);
			southPanel.add(label, BorderLayout.CENTER);
		}

		compute = new JButton("Compute");
		compute.addActionListener(this);
		abort = new JButton("Abort");
		abort.addActionListener(this);
		southPanel.add(compute);
		southPanel.add(abort);

		{
			InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
			KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
			String enterAction = "compute";
			String escAction = "abort";
			inputMap.put(enterKey, enterAction);
			inputMap.put(escKey, escAction);
			getRootPane().getActionMap().put(enterAction, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startComputing();
				}
			});
			getRootPane().getActionMap().put(escAction, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					abortComputation();
				}
			});
		}


		this.pack();
		this.setResizable(false);
		setLocationRelativeTo(getParent());
		this.setVisible(true);
		
	}

	public void enableElementSelection(boolean enabled) {
		elementPanel.enableElementSelection(enabled);
		elementAutoDetect.setEnabled(enabled);
	}

	private void abortComputation() {
		this.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()== abort) {
			abortComputation();
		} else if(e.getSource() == autoDetectFM){
			
//			box.getrender
//			ListCellRenderer<CompactPeak> renderer = box.getRenderer();
			
			
			int maxPos = 0;
			double maxInt = 0;
			for(int i=0;i<masses.size();i++){
				CompactPeak p = masses.get(i);
				if(p.getAbsoluteIntensity()>maxInt){
					maxInt = p.getAbsoluteIntensity();
					maxPos = i;
				}
			}
			box.setSelectedIndex(maxPos);
			
//			Dimension dim = box.getUI().getPreferredSize(box);
//			System.out.println("dim: "+dim.getWidth()+" "+dim.getHeight());
		}else if(e.getSource() == expFM){
			
//			box.getrender
//			ListCellRenderer<CompactPeak> renderer = box.getRenderer();
			
			box.setSelectedItem(String.valueOf(ec.getDataFocusedMass()));
			
//			Dimension dim = box.getUI().getPreferredSize(box);
//			System.out.println("dim: "+dim.getWidth()+" "+dim.getHeight());
		} else if(e.getSource() == this.compute){
			startComputing();
		}
		else if (e.getSource() == elementAutoDetect) {
			final Sirius sirius = new Sirius();

			MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()), getSelectedIonMass());
			final FormulaConstraints c = sirius.predictElementsFromMs1(exp);


			//Collect symbols of predicted elements
			Set<String> predictedElements = new HashSet<>();
			for (Element element : c.getChemicalAlphabet().getElements()) {
				if (c.getUpperbound(element)>0){
					predictedElements.add(element.getSymbol());
				}
			}
			elementPanel.setSelectedElements(predictedElements);

		}
	}

	private double getSelectedIonMass() {
		Object selected = box.getSelectedItem();
		double pm=0;
		if(selected instanceof CompactPeak){
			CompactPeak cp = (CompactPeak) selected;
			pm = cp.getMass();
		}else{
			pm = Double.parseDouble(selected.toString());
		}
		return pm;
	}

	public void startComputing() {
		String val = searchProfilePanel.getInstrument();
		String instrument = "";
		if(val.equals("Q-TOF")) {
			instrument = "qtof";
		}else if(val.equals("Orbitrap")){
			instrument = "orbitrap";
		}else if(val.equals("FT-ICR")){
			instrument = "fticr";
		}else{
			throw new RuntimeException("no valid instrument");
		}
		try{
			//entspricht setup() Methode

			Sirius sirius = new Sirius(instrument);
			final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
			final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
			final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
			final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());

			double ppm = searchProfilePanel.getPpm();

			if ((int)(10*ms2Prof.getAllowedMassDeviation().getPpm()) != (int)(10*ppm)) {
				ms2Prof.setAllowedMassDeviation(new Deviation(ppm));
				ms1Prof.setAllowedMassDeviation(new Deviation(ppm));
			}
			final TreeBuilder builder = ms2.getTreeBuilder();


			if (builder == null) {
				Logger l = LoggerFactory.getLogger(this.getClass());
				String noILPSolver = "Could not load a valid ILP solver (TreeBuilder) " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + ". Please read the installation instructions.";
				l.error(noILPSolver);
				new ErrorReportDialog(owner, noILPSolver);
//				new ExceptionDialog(owner, noILPSolver);
				dispose();
				return;
			}

			LoggerFactory.getLogger(this.getClass()).info("Compute trees using " + builder.getDescription());

			sirius.getMs2Analyzer().setDefaultProfile(ms2Prof);
			sirius.getMs1Analyzer().setDefaultProfile(ms1Prof);

			//Ende setup() Methode

			Object selected = box.getSelectedItem();
			double pm=0;
			if(selected instanceof CompactPeak){
				CompactPeak cp = (CompactPeak) selected;
				pm = cp.getMass();
			}else{
				pm = Double.parseDouble(selected.toString());
			}

			int candidates = searchProfilePanel.getNumberOfCandidates();

			MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization()), pm);

			ProgressDialog progDiag = new ProgressDialog(this);
			FormulaConstraints constraints = elementPanel.getElementConstraints();

            // cancel the corresponding task in background
            owner.getBackgroundComputation().cancel(ec);

			FormulaSource formulaSource = searchProfilePanel.getFormulaSource();

			if (formulaSource!=FormulaSource.ALL_POSSIBLE){
				//Test connection, if needed
				if (!WebAPI.getRESTDb(BioFilter.ALL).testConnection()){
					new NoConnectionDialog(this);
					dispose();
					return;
				}
			}

            progDiag.start(sirius, ec, exp, constraints, candidates, formulaSource);
			if(progDiag.isSucessful()){
//	            	System.err.println("progDiag erfolgreich");
				this.success = true;
				this.ec.setRawResults(progDiag.getResults());
                this.ec.setComputeState(progDiag.getResults()==null || progDiag.getResults().size()==0 ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);
                PrecursorIonType ion = SiriusDataConverter.enumOrNameToIontype(searchProfilePanel.getIonization());
                if (ion==null) ion = PrecursorIonType.getPrecursorIonType("[M+H]+");
				this.ec.setIonization(ion);
				Object o = box.getSelectedItem();
				if(o instanceof String){
					this.ec.setSelectedFocusedMass(Double.parseDouble((String)box.getSelectedItem()));
				}else{
					DefaultCompactPeak p = (DefaultCompactPeak) o;
					this.ec.setSelectedFocusedMass(p.getMass());
				}
			} else {
                ec.setRawResults(Collections.<IdentificationResult>emptyList());
                ec.setComputeState(ComputingStatus.FAILED);
				owner.refreshCompound(ec);
                if (progDiag.getException()!=null){
					if (progDiag.getException().getCause() instanceof UnknownHostException){
						new NoConnectionDialog(this);
					} else {
						LoggerFactory.getLogger(this.getClass()).error("Computation failed",progDiag.getException());
						new ErrorReportDialog(this, "Computation failed");
					}
				}
			}
			owner.refreshCompound(ec);
			this.dispose();

//	            List<IdentificationResult> results = sirius.identify(exp, 10, true, IsotopePatternHandling.omit, whiteset);

		}catch(IOException e2){
			throw new RuntimeException(e2);
		}
	}
	
	public boolean isSuccessful(){
		return this.success;
	}

}


