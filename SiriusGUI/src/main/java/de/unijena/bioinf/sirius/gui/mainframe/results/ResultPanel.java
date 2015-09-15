package de.unijena.bioinf.sirius.gui.mainframe.results;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.calendar.DateSelectionModel.SelectionMode;

import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public class ResultPanel extends JPanel implements ListSelectionListener{
	
	private ResultTreeListModel listModel;
	private JList<SiriusResultElement> resultsJList;
//	private ResultTreeListThumbnailCellRenderers listRenderer;
	private TreeVisualizationPanel tvp;
	private SpectraVisualizationPanel svp;
	
	private ExperimentContainer ec;
	
	private Frame owner;
	
	public ResultPanel(Frame owner) {
		this(null, owner);
	}
	
	public ResultPanel(ExperimentContainer ec, Frame owner) {
		this.setLayout(new BorderLayout());
		this.ec = ec;
		this.owner = owner;
		
		if(this.ec!=null) this.listModel = new ResultTreeListModel(ec.getResults());
		else this.listModel = new ResultTreeListModel();
		this.resultsJList = new JList<>(this.listModel);
		this.listModel.setJList(this.resultsJList);
//		if(this.ec!=null){
//			listRenderer = new ResultTreeListThumbnailCellRenderers(ec.getResults());
//		}else{
//			listRenderer = new ResultTreeListThumbnailCellRenderers(new ArrayList<SiriusResultElement>());
//		}
//		resultsJList.setCellRenderer(listRenderer);
		resultsJList.setCellRenderer(new ResultTreeListTextCellRenderer());
		resultsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsJList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		resultsJList.setVisibleRowCount(1);
//		resultsJList.getPreferredSize()
		resultsJList.setMinimumSize(new Dimension(0,45));
		resultsJList.setPreferredSize(new Dimension(0,45));
		resultsJList.addListSelectionListener(this);
		System.err.println("resultsPS"+resultsJList.getPreferredSize().getWidth()+" "+resultsJList.getPreferredSize().getHeight());
		
		JScrollPane listJSP = new JScrollPane(resultsJList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JPanel temp = new JPanel(new BorderLayout());
		temp.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(),"molecular formulas"));
		temp.add(listJSP,BorderLayout.NORTH);
		this.add(temp,BorderLayout.NORTH);
		
		JTabbedPane centerPane = new JTabbedPane();
		tvp = new TreeVisualizationPanel(owner);
		centerPane.addTab("tree view",tvp);
		
		svp = new SpectraVisualizationPanel(ec);
		centerPane.addTab("spectra view",svp);
		
		this.add(centerPane,BorderLayout.CENTER);
		
		
//		this.add(new JLabel("Dummy^2"), BorderLayout.CENTER);
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"results"));
//		resultsJList.set
	}
	
	public void changeData(ExperimentContainer ec){
		this.ec = ec;
		
//		if(this.ec!=null){
//			listRenderer.updateData(ec.getResults());
//		}else{
//			listRenderer.updateData(new ArrayList<SiriusResultElement>());
//		}
		
		SiriusResultElement sre = null;
		resultsJList.removeListSelectionListener(this);
		if(this.ec!=null&&!this.ec.getResults().isEmpty()){
			this.listModel.setData(ec.getResults());
			if(this.listModel.getSize()>0){
				this.resultsJList.setSelectedIndex(0);
				sre = ec.getResults().get(0);
			}
//			tvp.showTree(ec.getResults().get(0).getTree());
		}else{
			this.listModel.setData(new ArrayList<SiriusResultElement>());
		}
		resultsJList.addListSelectionListener(this);
		svp.changeExperiment(this.ec,sre);
		if(sre==null) tvp.showTree(null);
		else tvp.showTree(sre);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		SiriusResultElement sre = this.resultsJList.getSelectedValue();
		if(sre==null){
			tvp.showTree(null);
			svp.changeSiriusResultElement(null);
		}else{
			tvp.showTree(sre);
			svp.changeSiriusResultElement(sre);
		}
	}

}
