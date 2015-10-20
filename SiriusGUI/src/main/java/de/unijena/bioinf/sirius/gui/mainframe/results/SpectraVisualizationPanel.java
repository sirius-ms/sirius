package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.myxo.gui.msviewer.MSViewerPanel;
import de.unijena.bioinf.myxo.gui.msviewer.MSViewerPanelListener;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ResultsMSViewerDataModel;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;

public class SpectraVisualizationPanel extends JPanel implements ActionListener, MSViewerPanelListener, MouseListener{

	private JComboBox<String> spectraSelection;
	private MSViewerPanel msviewer;
	
	private ExperimentContainer ec;
	private DefaultComboBoxModel<String> cbModel;
	
	private DecimalFormat cEFormat;
	
	private ResultsMSViewerDataModel model;
	
	private JButton zoomIn, zoomOut;
	
	private boolean zoomed;
	
	private SiriusResultElement sre;
	
	JPopupMenu zoomPopMenu;
	JMenuItem zoomInMI, zoomOutMI;
	
	public SpectraVisualizationPanel(ExperimentContainer ec) {
		this.ec = ec;
		
		this.sre = null;
		
		JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
//		zoomPanel.setBorder(BorderFactory.createEtchedBorder());
		zoomIn = new JButton("Zoom in",new ImageIcon("src/main/resources/icons/zoom-in.png"));
		zoomOut = new JButton("Zoom out",new ImageIcon("src/main/resources/icons/zoom-out.png"));
		zoomIn.addActionListener(this);
		zoomOut.addActionListener(this);
		zoomIn.setEnabled(false);
		zoomOut.setEnabled(false);
		zoomed = false;
		
		constructZoomPopupMenu();
		
		this.cEFormat = new DecimalFormat("#0.0");
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
		northPanel.setBorder(BorderFactory.createEtchedBorder());
		cbModel = new DefaultComboBoxModel<>();
		updateLogic();
		
		spectraSelection = new JComboBox<String>(cbModel);
		spectraSelection.addActionListener(this);
		
		JPanel spectrumSelection = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
//		spectrumSelection.setBorder(BorderFactory.createEtchedBorder());
		spectrumSelection.add(new JLabel("spectrum "));
		spectrumSelection.add(spectraSelection);
		northPanel.add(spectrumSelection);
		
		zoomPanel.add(zoomIn);
		zoomPanel.add(zoomOut);
		northPanel.add(zoomPanel);
		
		this.add(northPanel, BorderLayout.NORTH);
		
		model = new ResultsMSViewerDataModel(ec);
		
		msviewer = new MSViewerPanel();
		msviewer.showPeakInfoOnlyForImportantPeaks(true);
		msviewer.setData(model);
		this.add(msviewer);
		msviewer.addMSViewerPanelListener(this);
		msviewer.addMouseListener(this);
		
//		msviewer.set
	}
	
	public void constructZoomPopupMenu(){
		zoomPopMenu = new JPopupMenu();
		zoomInMI = new JMenuItem("Zoom in",new ImageIcon("src/main/resources/icons/zoom-in.png"));
		zoomOutMI = new JMenuItem("Zoom out",new ImageIcon("src/main/resources/icons/zoom-out.png"));
		
		zoomInMI.addActionListener(this);
		zoomOutMI.addActionListener(this);
		
		zoomInMI.setEnabled(false);
		zoomOutMI.setEnabled(false);
		
		zoomPopMenu.add(zoomInMI);
		zoomPopMenu.add(zoomOutMI);
	}
	
	private void updateLogic(){
		HashSet<String> nameStorage = new HashSet<>();
		
		if(spectraSelection!=null) spectraSelection.removeActionListener(this);
		cbModel.removeAllElements();
		if(ec==null){
			this.zoomIn.setEnabled(false);
			this.zoomOut.setEnabled(false);
			this.zoomInMI.setEnabled(false);
			this.zoomOutMI.setEnabled(false);
		}else{
			this.zoomIn.setEnabled(false);
			this.zoomOut.setEnabled(false);
			this.zoomInMI.setEnabled(false);
			this.zoomOutMI.setEnabled(false);
			java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			java.util.List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			if(!(ms1==null||ms1.isEmpty())){
				cbModel.addElement("MS 1");
			}
			if(ms2!=null){
				for(CompactSpectrum sp : ms2){
					
					String value = null;
					
					if(sp.getCollisionEnergy()!=null){
						double minEn = sp.getCollisionEnergy().getMinEnergy();
						double maxEn = sp.getCollisionEnergy().getMaxEnergy();
						
						if(minEn==maxEn){
							value = cEFormat.format(minEn)+" eV";
						}else{
							value = cEFormat.format(minEn)+"-"+cEFormat.format(maxEn)+" eV";
						}
						int counter = 2;
						while(nameStorage.contains(value)){
							if(minEn==maxEn){
								value = cEFormat.format(minEn)+" eV ("+counter+")";
							}else{
								value = cEFormat.format(minEn)+"-"+cEFormat.format(maxEn)+" eV ("+counter+")";
							}
							counter++;
						}
					}else{
						value = "MS 2";
						int counter = 2;
						while(nameStorage.contains(value)){
							value = "MS 2 ("+counter+")";
							counter++;
						}
					}
					
					
					nameStorage.add(value);
					cbModel.addElement(value);
				}
			}
			if(cbModel.getSize()>0) spectraSelection.setSelectedIndex(0);
			else spectraSelection.setSelectedIndex(-1);
		}
		
		if(spectraSelection!=null){
			if(sre==null){
				spectraSelection.setEnabled(false);
			}else{
				spectraSelection.setEnabled(true);
			}
			spectraSelection.addActionListener(this);
		}
	}
	
	public void changeExperiment(ExperimentContainer ec,SiriusResultElement sre){
		this.ec = ec;
		this.sre = sre;
		updateLogic();
		this.model = new ResultsMSViewerDataModel(ec);
		if(this.ec==null){
			model.showDummySpectrum();
		}else{
			java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			java.util.List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			if(sre==null){
				model.showDummySpectrum();
			}else if(ms1!=null&&ms1.size()>0){
				model.selectMS1Spectrum();
			}else if(ms2!=null&&ms2.size()>0){
				model.selectMS2Spectrum(0);
			}else{
				model.showDummySpectrum();
			}
		}
		
		msviewer.setData(model);
		if(this.ec != null) this.showMolecularFormulaMarkings();
//		msviewer.repaint();
	}
	
	public ExperimentContainer getExperiment(){
		return ec;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==spectraSelection){
			model = new ResultsMSViewerDataModel(ec);
			java.util.List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			java.util.List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			int index = this.spectraSelection.getSelectedIndex();
			if(index<0) return;
			
			if(ms1!=null&&ms1.size()>0){
				if(index==0){
					model.selectMS1Spectrum();
				}else if(ms2!=null&&ms2.size()>0){
					model.selectMS2Spectrum(index-1);
				}	
			}else{
				if(ms2!=null&&ms2.size()>0){
					model.selectMS2Spectrum(index);
				}
			}
			msviewer.setData(model);
			this.showMolecularFormulaMarkings();
//			msviewer.repaint();
		}else if(e.getSource()==zoomIn || e.getSource()==zoomInMI){
			zoomIn.setEnabled(false);
			zoomInMI.setEnabled(false);
			int start = model.getFirstMarkedIndex();
			int end = model.getLastMarkedIndex();
			this.model.removeMarkings();
			if(start<0||end<0||start==end){
				return;
			}
			this.msviewer.showZoomedView(start, end);
			zoomOut.setEnabled(true);
			zoomOutMI.setEnabled(true);
		}else if(e.getSource()==zoomOut || e.getSource()==zoomOutMI){
			zoomOut.setEnabled(false);
			zoomOutMI.setEnabled(false);
			this.model.removeMarkings();
			this.msviewer.showOverview();
			zoomIn.setEnabled(true);
			zoomInMI.setEnabled(true);
		}
		
	}
	
	public void changeSiriusResultElement(SiriusResultElement sre){
		this.sre = sre;
		if(sre==null){
			spectraSelection.setEnabled(false);
		}else{
			spectraSelection.setEnabled(true);
		}
		showMolecularFormulaMarkings();
	}
	
	private void showMolecularFormulaMarkings(){
		if(sre==null){
			this.zoomIn.setEnabled(false);
			this.zoomOut.setEnabled(false);
			this.zoomInMI.setEnabled(false);
			this.zoomOutMI.setEnabled(false);
			this.model.showDummySpectrum();
			msviewer.repaint();
			return;
		}
		TreeNode root = sre.getTree();
		ArrayDeque<TreeNode> deque = new ArrayDeque<>();
		this.model.markAllPeaksAsUnimportant();
		this.model.removeMarkings();
		deque.add(root);
		while(!deque.isEmpty()){
			TreeNode tn = deque.remove();
			for(TreeEdge te : tn.getOutEdges()){
				deque.add(te.getTarget());
			}
			double mass = tn.getPeakMass();
			double tolerance = mass>1000 ? 0.02 : 0.01;
			int index = model.findIndexOfPeak(mass, tolerance);
			if(index>-1){
				this.model.setImportant(index, true);
				this.model.setMolecularFormula(index, tn.getMolecularFormula());
			}
		}
		msviewer.showOverview();
		this.zoomIn.setEnabled(false);
		this.zoomOut.setEnabled(false);
		this.zoomInMI.setEnabled(false);
		this.zoomOutMI.setEnabled(false);
		msviewer.repaint();
	}

	@Override
	public void markingsRemoved() {
		this.model.removeMarkings();
		this.zoomIn.setEnabled(false);
		this.zoomInMI.setEnabled(false);
//		this.msviewer.repaint();
	}

	@Override
	public void peaksMarked(List<Integer> indices) {
		this.model.removeMarkings();
		for(int i : indices) this.model.setMarked(i, true);
		if(indices.size()>0){
			this.zoomIn.setEnabled(true);
			this.zoomInMI.setEnabled(true);
		}
	}

	@Override
	public void peaksMarkedPerDrag(List<Integer> indices) {
		this.model.removeMarkings();
		for(int i : indices) this.model.setMarked(i, true);
		if(indices.size()>0){
			this.zoomIn.setEnabled(true);
			this.zoomInMI.setEnabled(true);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.isPopupTrigger()){
			this.zoomPopMenu.show(e.getComponent(), e.getX(), e.getY());			
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger()){
			this.zoomPopMenu.show(e.getComponent(), e.getX(), e.getY());			
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
