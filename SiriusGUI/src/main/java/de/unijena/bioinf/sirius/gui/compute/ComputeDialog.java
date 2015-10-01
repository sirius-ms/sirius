package de.unijena.bioinf.sirius.gui.compute;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;

import org.apache.commons.collections.map.HashedMap;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.ChemistryBase.ms.ft.LossAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.DPTreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.myxo.structure.CompactPeak;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactPeak;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElementConverter;

public class ComputeDialog extends JDialog implements ActionListener{

	private JButton compute, abort, autoDetectFM, expFM;
	private JComboBox<CompactPeak> box;
	private Vector<CompactPeak> masses;
	private MyListCellRenderer renderer;
	
	private JCheckBox bromine, borone, selenium, chlorine, iodine, fluorine;
	private JTextField elementTF;
	private JButton elementButton, elementAutoDetect;
	
	private TreeSet<String> additionalElements;
	
	private Vector<String> ionizations, instruments;
	private JComboBox<String> ionizationCB, instrumentCB;
	private JSpinner ppmSpinner;
	private SpinnerNumberModel snm;
	
	private boolean success;
	private ExperimentContainer ec;
	private HashMap<String,Ionization> stringToIonMap;
	private HashMap<Ionization,String> ionToStringMap;
	
	public ComputeDialog(JFrame owner,ExperimentContainer ec) {
		super(owner,"compute",true);
		
		this.ec = ec;
		this.success = false;
		
		additionalElements = new TreeSet<>();
		
		this.setLayout(new BorderLayout());
		
		Box mainPanel = Box.createVerticalBox();
		
//		JPanel mainPanel = new JPanel(new BorderLayout());
		this.add(mainPanel,BorderLayout.CENTER);
		
		JPanel focMassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		
		masses = new Vector<>();
		double maxInt = -1;
		Object maxObj = null;
		List<CompactSpectrum> ms1Spectra = ec.getMs1Spectra();
		if(!ms1Spectra.isEmpty()){
			CompactSpectrum sp = ms1Spectra.get(0);
			for(int i=0;i<sp.getSize();i++){
				if(sp.getPeak(i).getAbsoluteIntensity()>maxInt){
					maxInt = sp.getPeak(i).getAbsoluteIntensity();
					maxObj = sp.getPeak(i);
				}
				masses.add(sp.getPeak(i));
			}
		}
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
		focMassPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"focused mass"));
		
		
		
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
		autoDetectFM = new JButton("most intensive peak");
		autoDetectFM.addActionListener(this);
		if(masses.isEmpty()) autoDetectFM.setEnabled(false);
		expFM = new JButton("file value");
		expFM.addActionListener(this);
		if(ec.getDataFocusedMass()<=0)expFM.setEnabled(false);
		
		if(!masses.isEmpty()){
			box.setSelectedItem(maxObj);
		}else if(ec.getDataFocusedMass()>0){
			box.setSelectedItem(String.valueOf(ec.getDataFocusedMass()));
		}else{
			box.setSelectedItem("");
		}
		
		focMassPanel.add(autoDetectFM);
		focMassPanel.add(expFM);
		mainPanel.add(focMassPanel,BorderLayout.NORTH);
		
		/////////////////////////////////////////////
//		Box elementPanel = Box.createVerticalBox();
		JPanel elementPanel = new JPanel(new BorderLayout());
		elementPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"elements beside CHNOPS"));
		mainPanel.add(elementPanel);
		
		bromine = new JCheckBox("bromine");
		borone = new JCheckBox("borone");
		selenium = new JCheckBox("selenium");
		chlorine = new JCheckBox("chlorine");
		iodine = new JCheckBox("iodine");
		fluorine = new JCheckBox("fluorine");
		
		JPanel elements = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		elements.add(bromine);
		elements.add(borone);
		elements.add(chlorine);
		elements.add(fluorine);
		elements.add(iodine);
		elements.add(selenium);
		
		elementAutoDetect = new JButton("auto detect");
		elementAutoDetect.addActionListener(this);
		elementTF = new JTextField(10);
		elementTF.setEditable(false);
		elementButton = new JButton("more elements");
		elementButton.addActionListener(this);
		
		elements.add(elementAutoDetect);
		elementPanel.add(elements,BorderLayout.NORTH);
		
		JPanel elements2 = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		elements2.add(elementTF);
		elements2.add(elementButton);
		elementPanel.add(elements2,BorderLayout.SOUTH);
		
		
//		elementPanel.add(Box.createVerticalGlue());
		
		/////////////////////////////////////////////
		
		JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		otherPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"other"));
		ionizations = new Vector<>();
		ionizations.add("[M+H]+");
		ionizations.add("[M+Na]+");
		ionizations.add("M+");
		ionizations.add("[M-H]-");
		ionizations.add("M-");
		
		stringToIonMap = new HashMap<>();
		stringToIonMap.put("[M+H]+", Ionization.MPlusH);
		stringToIonMap.put("[M+Na]+", Ionization.MPlusNa);
		stringToIonMap.put("M+", Ionization.M);
		stringToIonMap.put("[M-H]-", Ionization.MMinusH);
		stringToIonMap.put("M-", Ionization.MMinusH);
		
		ionToStringMap = new HashMap<>();
		ionToStringMap.put(Ionization.MPlusH,"[M+H]+");
		ionToStringMap.put(Ionization.MPlusNa,"[M+Na]+");
		ionToStringMap.put(Ionization.M,"M+");
		ionToStringMap.put(Ionization.MMinusH,"[M-H]-");
		ionToStringMap.put(Ionization.MMinusH,"M-");
		
		ionizationCB = new JComboBox<>(ionizations);
		otherPanel.add(new JLabel("ionization"));
		otherPanel.add(ionizationCB);
		instruments = new Vector<>();
		instruments.add("Q-TOF");
		instruments.add("Orbitrap");
		instruments.add("FT-ICR");
		instrumentCB = new JComboBox<>(instruments);
		otherPanel.add(new JLabel("  instrument"));
		otherPanel.add(instrumentCB);
		
		this.snm = new SpinnerNumberModel(6,0,50,0.25);
		this.ppmSpinner = new JSpinner(this.snm);
		this.ppmSpinner.setMinimumSize(new Dimension(70,26));
		this.ppmSpinner.setPreferredSize(new Dimension(70,26));
		otherPanel.add(new JLabel("  ppm"));
		otherPanel.add(this.ppmSpinner);
		
		
		mainPanel.add(otherPanel);
		
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		this.add(southPanel,BorderLayout.SOUTH);
		compute = new JButton("compute");
		compute.addActionListener(this);
		abort = new JButton("abort");
		abort.addActionListener(this);
		southPanel.add(compute);
		southPanel.add(abort);
		
		this.pack();
		this.setResizable(false);
		setLocationRelativeTo(getParent());
		this.setVisible(true);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == autoDetectFM){
			
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
			if(borone.isSelected()) eles.add("B");
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
					borone.setSelected(true);
					additionalElements.remove("B");
				}else{
					borone.setSelected(false);
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
	            
	            final TreeBuilder builder = sirius.getMs2Analyzer().getTreeBuilder();
	            if (builder instanceof DPTreeBuilder) {
	                System.err.println("Cannot load ILP solver. Please read the installation instructions.");
	                System.exit(1);
	            }
	            System.out.println("Compute trees using " + builder.getDescription());
	 
	            sirius.getMs2Analyzer().setDefaultProfile(ms2Prof);
	            sirius.getMs1Analyzer().setDefaultProfile(ms1Prof);
	            
	            //Ende setup() Methode
	            
//	            sirius.setProgress(new DummyProgress());
	            
	            Object selected = box.getSelectedItem();
	            double pm=0;
	            if(selected instanceof CompactPeak){
	            	CompactPeak cp = (CompactPeak) selected;
	            	pm = cp.getMass();
	            }else{
	            	pm = Double.parseDouble(selected.toString());
	            }
	            
//	            System.err.println(pm);
	            
	            Ms2Experiment exp = this.convert(ec,(String) ionizationCB.getSelectedItem(),pm);
	            
	            Set<MolecularFormula> whiteset = new HashSet<MolecularFormula>();
	            
	            ProgressDialog progDiag = new ProgressDialog(this);
	            progDiag.start(sirius, exp, whiteset);
	            if(progDiag.isSucessful()){
//	            	System.err.println("progDiag erfolgreich");
	            	this.success = true;
	            	this.ec.setResults(SiriusResultElementConverter.convertResults(progDiag.getResults()));
	            	this.ec.setIonization(stringToIonMap.get((String) ionizationCB.getSelectedItem()));
	            	Object o = box.getSelectedItem();
	            	if(o instanceof String){
	            		this.ec.setSelectedFocusedMass(Double.parseDouble((String)box.getSelectedItem()));
	            	}else{
	            		DefaultCompactPeak p = (DefaultCompactPeak) o;
	            		this.ec.setSelectedFocusedMass(p.getMass());
	            	}
	            	
	            	this.dispose();
	            }else{
//	            	System.err.println("progDiag nicht erfolgreich");
	            }
	            
//	            List<IdentificationResult> results = sirius.identify(exp, 10, true, IsotopePatternHandling.omit, whiteset);
	            
			}catch(IOException e2){
				throw new RuntimeException(e2);
			}
		}
	}
	
	public boolean isSuccessful(){
		return this.success;
	}
	
//	private static List<SiriusResultElement> convertResults(List<IdentificationResult> in){
//		List<SiriusResultElement> outs = new ArrayList<>();
//		for(IdentificationResult res : in){
//			SiriusResultElement out = new SiriusResultElement();
//			out.setMolecularFormula(res.getMolecularFormula());
//			out.setRank(res.getRank());
//			out.setScore(res.getScore());
//			
//			FTree ft = res.getTree();
//			out.setRawTree(ft);
//			
//			FragmentAnnotation<Peak> peakAno = ft.getFragmentAnnotationOrThrow(Peak.class);
//			LossAnnotation<Score> lscore = ft.getLossAnnotationOrNull(Score.class);
//			FragmentAnnotation<Score> fscore = ft.getFragmentAnnotationOrNull(Score.class);
//			
//			double maxInt = Double.NEGATIVE_INFINITY;
//			for(Fragment fragment : ft.getFragments()){
//				double fragInt = peakAno.get(fragment).getIntensity();
//				if(fragInt>maxInt) maxInt = fragInt;
//			}
//			
//			TreeNode root = initConvertNode(ft, peakAno, lscore, fscore, maxInt);
//			out.setTree(root);	
//			outs.add(out);
//		}
//		return outs;
//	}
//	
//	public static TreeNode initConvertNode(FTree ft,FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt){
//		Fragment rootK = ft.getRoot();
//		TreeNode rootM = new DefaultTreeNode();
//		rootM.setMolecularFormula(rootK.getFormula().toString());
//		rootM.setMolecularFormulaMass(rootK.getFormula().getMass());
//		rootM.setPeakMass(peakAno.get(rootK).getMass());
//		rootM.setPeakAbsoluteIntenstiy(peakAno.get(rootK).getIntensity());
//		rootM.setPeakRelativeIntensity(peakAno.get(rootK).getIntensity()/maxInt);
//		double tempScore = fscore.get(rootK).sum();
//		rootM.setScore(tempScore);
//		
//		convertNode(ft, rootK, rootM, peakAno, lscore, fscore, maxInt);
//		
//		return rootM;
//	}
//	
//	private static void convertNode(FTree ft, Fragment sourceK, TreeNode sourceM, FragmentAnnotation<Peak> peakAno, LossAnnotation<Score> lscore, FragmentAnnotation<Score> fscore, double maxInt){
//		for(Loss edgeK : sourceK.getOutgoingEdges()){
//			Fragment targetK = edgeK.getTarget();
//			
//			DefaultTreeNode targetM = new DefaultTreeNode();
//			targetM.setMolecularFormula(targetK.getFormula().toString());
//			targetM.setMolecularFormulaMass(targetK.getFormula().getMass());
//			targetM.setPeakMass(peakAno.get(targetK).getMass());
//			targetM.setPeakAbsoluteIntenstiy(peakAno.get(targetK).getIntensity());
//			targetM.setPeakRelativeIntensity(peakAno.get(targetK).getIntensity()/maxInt);
//			double tempScore = fscore.get(targetK).sum();
//			tempScore += lscore.get(edgeK).sum();
//			targetM.setScore(tempScore);
//			
//			DefaultTreeEdge edgeM = new DefaultTreeEdge();
//			edgeM.setSource(sourceM);
//			edgeM.setTarget(targetM);
//			edgeM.setScore(lscore.get(edgeK).sum()); //TODO korrekt???
//			MolecularFormula mfSource = sourceK.getFormula();
//			MolecularFormula mfTarget = targetK.getFormula();
//			MolecularFormula mfLoss = mfSource.subtract(mfTarget);
//			edgeM.setLossFormula(mfLoss.toString());
//			edgeM.setLossMass(targetM.getPeakMass()-sourceM.getPeakMass());
//			
//			sourceM.addOutEdge(edgeM);
//			targetM.setInEdge(edgeM);
//			
//			convertNode(ft,targetK,targetM,peakAno,lscore,fscore,maxInt);
//			
//		}
//	}
	
	private Ms2Experiment convert(ExperimentContainer ec,String ionization, double pm){
		MutableMs2Experiment exp = new MutableMs2Experiment();
		String val = ionization;
//		if(ec.getIonization() == Ionization.M){
//			val = "[M]+";
//		}else if(ec.getIonization() == Ionization.MMinusH){
//			val = "[M-H]-";
//		}else if(ec.getIonization() == Ionization.MPlusH){
//			val = "[M+H]+";
//		}else if(ec.getIonization() == Ionization.MPlusNa){
//			val = "[M+Na]+";
//		}
		exp.setIonization(PeriodicTable.getInstance().ionByName(val));
		FormulaConstraints constraints = null; //TODO
		MutableMeasurementProfile profile = new MutableMeasurementProfile();
        profile.setFormulaConstraints(constraints);
        exp.setMeasurementProfile(profile);
        
        List<MutableMs2Spectrum> ms2spectra = new ArrayList<>();
        exp.setMs2Spectra(ms2spectra);
        for(CompactSpectrum sp : ec.getMs2Spectra()){
        	MutableMs2Spectrum spNew = new MutableMs2Spectrum();
        	spNew.setIonization(exp.getIonization());
        	spNew.setMsLevel(2);
//        	spNew.setPrecursorMz(ec.getFocusedMass()); //TODO
        	spNew.setPrecursorMz(pm);
//        	System.err.println(spNew.getPrecursorMz());
        	spNew.setCollisionEnergy(sp.getCollisionEnergy());
        	for(int i=0;i<sp.getSize();i++){
        		spNew.addPeak(sp.getMass(i), sp.getAbsoluteIntensity(i));
        	}
        	ms2spectra.add(spNew);
        }
        
        List<CompactSpectrum> ms1 = ec.getMs1Spectra();
        if(ms1!=null && !ms1.isEmpty()){
        	CompactSpectrum ms11 = ms1.get(0);
        	double[] masses = new double[ms11.getSize()];
        	double[] ints = new double[ms11.getSize()];
        	for(int i=0;i<masses.length;i++){
        		masses[i] = ms11.getMass(i);
        		ints[i] = ms11.getAbsoluteIntensity(i);
        	}
        	SimpleSpectrum ms = new SimpleSpectrum(masses,ints);
        	exp.setMergedMs1Spectrum(ms);
        	List<SimpleSpectrum> temp = new ArrayList<>();
        	temp.add(ms);
        	exp.setMs1Spectra(temp);
        }
        
		return exp;
	}

}


