/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;

public class BatchComputeDialog extends JDialog implements ActionListener {

    private JButton compute, abort;

    private JCheckBox bromine, borone, selenium, chlorine, iodine, fluorine;
    private JTextField elementTF;
    private JButton elementButton;
    private JCheckBox elementAutoDetect;
    private MainFrame owner;

    private TreeSet<String> additionalElements;

    private Vector<String> ionizations, instruments;
    private JComboBox<String> ionizationCB, instrumentCB;
    private JSpinner ppmSpinner;
    private SpinnerNumberModel snm;

    private boolean success;
    private HashMap<String,Ionization> stringToIonMap;
    private HashMap<Ionization,String> ionToStringMap;
    private final JSpinner candidatesSpinner;

    public BatchComputeDialog(MainFrame owner) {
        super(owner,"compute",true);
        this.owner = owner;
        this.success = false;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        additionalElements = new TreeSet<>();

        this.setLayout(new BorderLayout());

        Box mainPanel = Box.createVerticalBox();

        this.add(mainPanel,BorderLayout.CENTER);


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

        elementAutoDetect = new JCheckBox("Auto detect");
        elementAutoDetect.setEnabled(false);
        elementAutoDetect.addActionListener(this);
        elementTF = new JTextField(10);
        elementTF.setEditable(false);
        elementButton = new JButton("More elements");
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


        mainPanel.add(otherPanel);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
        this.add(southPanel,BorderLayout.SOUTH);
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
                    abortComputing();
                }
            });
        }


        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
        this.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()== abort) {
            this.dispose();
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
            startComputing();
        }
    }

    private void abortComputing() {
        this.dispose();
    }

    private void startComputing() {
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
        FormulaConstraints constraints;
        {
            HashSet<String> eles = new HashSet<>();
            if(borone.isSelected()) eles.add("B");
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

        final double ppm = snm.getNumber().doubleValue();

        final int candidates = ((Number)candidatesSpinner.getModel().getValue()).intValue();

        //entspricht setup() Methode
        final BackgroundComputation bgc = owner.getBackgroundComputation();
        final Enumeration<ExperimentContainer> compounds = owner.getCompounds();
        final ArrayList<BackgroundComputation.Task> tasks = new ArrayList<>();
        final ArrayList<ExperimentContainer> compoundList = new ArrayList<>();
        while (compounds.hasMoreElements()) {
            final ExperimentContainer ec = compounds.nextElement();
            if (ec.isUncomputed()) {
                final BackgroundComputation.Task task = new BackgroundComputation.Task(instrument, ec, constraints, ppm, candidates);
                tasks.add(task);
                compoundList.add(ec);
            }
        }
        bgc.addAll(tasks);
        for (ExperimentContainer ec : compoundList) {
            owner.refreshCompound(ec);
        }
        dispose();
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
        exp.setPrecursorIonType(PeriodicTable.getInstance().ionByName(val));

//		FormulaConstraints constraints = null; //TODO
//		MutableMeasurementProfile profile = new MutableMeasurementProfile();
//        profile.setFormulaConstraints(constraints);
//        exp.setMeasurementProfile(profile);
        System.err.println("ComputeDialog: setMeasurementProfile entfernt");

        java.util.List<MutableMs2Spectrum> ms2spectra = new ArrayList<>();
        exp.setMs2Spectra(ms2spectra);
        for(CompactSpectrum sp : ec.getMs2Spectra()){
            MutableMs2Spectrum spNew = new MutableMs2Spectrum();
            System.err.println("ComputeDialog: setIonization nach Verdacht gesetzt");
            spNew.setIonization(exp.getPrecursorIonType().getIonization());
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

        java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
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
            java.util.List<SimpleSpectrum> temp = new ArrayList<>();
            temp.add(ms);
            exp.setMs1Spectra(temp);
        }

        return exp;
    }

}