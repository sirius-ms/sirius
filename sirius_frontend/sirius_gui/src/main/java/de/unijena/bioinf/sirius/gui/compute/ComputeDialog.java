package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.maximumColorfulSubtree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.myxo.structure.CompactPeak;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactPeak;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ComputeDialog extends JDialog implements ActionListener{

	private JButton compute, abort, autoDetectFM, expFM;
	private JComboBox<CompactPeak> box;
	private Vector<CompactPeak> masses;
	private MyListCellRenderer renderer;
	
	private JCheckBox bromine, boron, selenium, chlorine, iodine, fluorine;
	private JTextField elementTF;
	private JButton elementButton, elementAutoDetect;
	private MainFrame owner;
	
	private TreeSet<String> additionalElements;
	
	private Vector<Ionization> ionizations;
    private Vector<String> instruments;
	private JComboBox<Ionization> ionizationCB;
    private JComboBox<String> instrumentCB,formulaCombobox;
	private JSpinner ppmSpinner;
	private SpinnerNumberModel snm;
	
	private boolean success;
	private ExperimentContainer ec;
	private final JSpinner candidatesSpinner;

//	private JToggleButton enableFinger;

	public ComputeDialog(MainFrame owner,ExperimentContainer ec) {
		super(owner,"compute",true);
		this.owner = owner;
		this.ec = ec;
		this.success = false;

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		additionalElements = new TreeSet<>();
		
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
		JPanel elementPanel = new JPanel();
		elementPanel.setLayout(new BoxLayout(elementPanel,BoxLayout.LINE_AXIS));
		elementPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Elements beside CHNOPS"));
		mainPanel.add(elementPanel);
		
		bromine = new JCheckBox("bromine");
		boron = new JCheckBox("boron");
		selenium = new JCheckBox("selenium");
		chlorine = new JCheckBox("chlorine");
		iodine = new JCheckBox("iodine");
		fluorine = new JCheckBox("fluorine");
		
		elementPanel.add(bromine);
		elementPanel.add(boron);
		elementPanel.add(chlorine);
		elementPanel.add(fluorine);
		elementPanel.add(iodine);
		elementPanel.add(selenium);
		


		elementTF = new JTextField(10);
		elementTF.setEditable(false);
		elementButton = new JButton("More elements");
		elementButton.addActionListener(this);



		JPanel elements2 = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		elements2.add(elementTF);
		elements2.add(elementButton);
		elementPanel.add(elements2);
		
		
		elementPanel.add(Box.createHorizontalGlue());

		elementAutoDetect = new JButton("Auto detect");
		elementAutoDetect.addActionListener(this);
		elementAutoDetect.setEnabled(false);
		elementPanel.add(elementAutoDetect);

		/////////////////////////////////////////////
		
		JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		otherPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Other"));

        ionizations = new Vector<>();
		for (Ionization ion : Ionization.values()) {
			ionizations.add(ion);
		}

		
		ionizationCB = new JComboBox<>(ionizations);
        if (ec.getIonization()!=null) {
            ionizationCB.setSelectedItem(ec.getIonization());
        } else {
            ionizationCB.setSelectedItem(Ionization.MPlusH);
        }
		otherPanel.add(new JLabel("adduct type"));
		otherPanel.add(ionizationCB);
		instruments = new Vector<>();
		instruments.add("Q-TOF");
		instruments.add("Orbitrap");
		instruments.add("FT-ICR");
		instrumentCB = new JComboBox<>(instruments);
		otherPanel.add(new JLabel("  instrument"));
		otherPanel.add(instrumentCB);

		this.snm = new SpinnerNumberModel(10,0.25,20,0.25);
		this.ppmSpinner = new JSpinner(this.snm);
		this.ppmSpinner.setMinimumSize(new Dimension(70,26));
		this.ppmSpinner.setPreferredSize(new Dimension(70,26));
		otherPanel.add(new JLabel("  ppm"));
		otherPanel.add(this.ppmSpinner);

		final SpinnerNumberModel candidatesNumberModel = new SpinnerNumberModel(10, 1, 1000, 1);
		candidatesSpinner = new JSpinner(candidatesNumberModel);
		candidatesSpinner.setMinimumSize(new Dimension(70, 26));
		candidatesSpinner.setPreferredSize(new Dimension(70, 26));
		otherPanel.add(new JLabel(" candidates"));
		otherPanel.add(candidatesSpinner);

		instrumentCB.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				final String name = (String)e.getItem();
				final double recommendedPPM;

				if (name.equals("Q-TOF")) recommendedPPM = 10;
				else if (name.equals("Orbitrap")) recommendedPPM = 5;
				else if (name.equals("FT-ICR")) recommendedPPM = 2;
				else recommendedPPM = 10;

				ppmSpinner.setValue(new Double(recommendedPPM)); // TODO: test
			}
		});


        //////////
        {
            JLabel label = new JLabel("Consider ");
            final Vector<String> values = new Vector<>();
            values.add("all possible molecular formulas");
            values.add("PubChem formulas");
            values.add("formulas from biological databases");
            formulaCombobox = new JComboBox<>(values);
            formulaCombobox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    enableElementSelection(formulaCombobox.getSelectedIndex()==0);
                }
            });
            otherPanel.add(label);
            otherPanel.add(formulaCombobox);
        }
		
		
		mainPanel.add(otherPanel);


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
		if (enabled) {
			for (JCheckBox b : Arrays.asList(boron, bromine, chlorine, fluorine, iodine, selenium)) {
				b.setEnabled(true);
			}
			elementButton.setEnabled(true);
			elementAutoDetect.setEnabled(false);
			elementTF.setEnabled(true);
		} else {
			for (JCheckBox b : Arrays.asList(boron, bromine, chlorine, fluorine, iodine, selenium)) {
				b.setEnabled(false);
			}
			elementButton.setEnabled(false);
			elementAutoDetect.setEnabled(false);
			elementTF.setEnabled(false);
		}
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
		}else if(e.getSource()==this.elementButton){
			HashSet<String> eles = new HashSet<>();
			if(boron.isSelected()) eles.add("B");
			if(bromine.isSelected()) eles.add("Br");
			if(chlorine.isSelected()) eles.add("Cl");
			if(fluorine.isSelected()) eles.add("F");
			if(iodine.isSelected()) eles.add("I");
			if(selenium.isSelected()) eles.add("Se");
			eles.addAll(additionalElements);
			AdditionalElementDialog diag = new AdditionalElementDialog(this,eles);
			if(diag.successful()){
				additionalElements = new TreeSet<>(diag.getSelectedElements());
				if(additionalElements.contains("B")){
					boron.setSelected(true);
					additionalElements.remove("B");
				}else{
					boron.setSelected(false);
				}
				if(additionalElements.contains("Br")){
					bromine.setSelected(true);
					additionalElements.remove("Br");
				}else{
					bromine.setSelected(false);
				}
				if(additionalElements.contains("Cl")){
					chlorine.setSelected(true);
					additionalElements.remove("Cl");
				}else{
					chlorine.setSelected(false);
				}
				if(additionalElements.contains("F")){
					fluorine.setSelected(true);
					additionalElements.remove("F");
				}else{
					fluorine.setSelected(false);
				}
				if(additionalElements.contains("I")){
					iodine.setSelected(true);
					additionalElements.remove("I");
				}else{
					iodine.setSelected(false);
				}
				if(additionalElements.contains("Se")){
					selenium.setSelected(true);
					additionalElements.remove("Se");
				}else{
					selenium.setSelected(false);
				}
				
				StringBuilder newText = new StringBuilder();
				Iterator<String> it = additionalElements.iterator();
				while(it.hasNext()){
					newText.append(it.next());
					if(it.hasNext()) newText.append(",");
				}
				elementTF.setText(newText.toString());
				
				
			}
		}else if(e.getSource() == this.compute){
			startComputing();
		}
	}

	public void startComputing() {
		String val = (String) instrumentCB.getSelectedItem();
		String instrument = "";
		if(val.equals("Q-TOF")){
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

			double ppm = snm.getNumber().doubleValue();

			ms2Prof.setAllowedMassDeviation(new Deviation(ppm));
			ms1Prof.setAllowedMassDeviation(new Deviation(ppm));

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

			int candidates = ((Number)candidatesSpinner.getModel().getValue()).intValue();

//	            System.err.println(pm);

			MutableMs2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(ec, (Ionization)ionizationCB.getSelectedItem(), pm);

			ProgressDialog progDiag = new ProgressDialog(this);
			FormulaConstraints constraints;
			{
				HashSet<String> eles = new HashSet<>();
				if(boron.isSelected()) eles.add("B");
				if(bromine.isSelected()) eles.add("Br");
				if(chlorine.isSelected()) eles.add("Cl");
				if(fluorine.isSelected()) eles.add("F");
				if(iodine.isSelected()) eles.add("I");
				if(selenium.isSelected()) eles.add("Se");
				eles.addAll(additionalElements);
				Element[] elems = new Element[eles.size()];
				int k=0;
				final PeriodicTable tb = PeriodicTable.getInstance();
				for (String s : eles) {
					final Element elem = tb.getByName(s);
					if (elem != null)
						elems[k++] = elem;
				}
				if (k < elems.length) elems = Arrays.copyOf(elems, k);
				constraints = new FormulaConstraints().getExtendedConstraints(elems);
			}

            // cancel the corresponding task in background
            owner.getBackgroundComputation().cancel(ec);

            FormulaSource formulaSource;
            if (formulaCombobox.getSelectedIndex()==0) formulaSource = FormulaSource.ALL_POSSIBLE;
            else if (formulaCombobox.getSelectedIndex()==1) formulaSource = FormulaSource.PUBCHEM;
            else formulaSource = FormulaSource.BIODB;


            progDiag.start(sirius, ec, exp, constraints, candidates, formulaSource);
			if(progDiag.isSucessful()){
//	            	System.err.println("progDiag erfolgreich");
				this.success = true;
				this.ec.setRawResults(progDiag.getResults());
                this.ec.setComputeState(progDiag.getResults()==null || progDiag.getResults().size()==0 ? ComputingStatus.FAILED : ComputingStatus.COMPUTED);
                Ionization ion = (Ionization)ionizationCB.getSelectedItem();
                if (ion==null) ion = Ionization.MPlusH;
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
                    LoggerFactory.getLogger(this.getClass()).error("Computation failed",progDiag.getException());
					new ErrorReportDialog(this, "Computation failed");
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


