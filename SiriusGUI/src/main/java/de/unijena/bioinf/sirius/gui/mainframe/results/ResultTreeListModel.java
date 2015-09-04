package de.unijena.bioinf.sirius.gui.mainframe.results;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import de.unijena.bioinf.myxo.gui.tree.render.DotReader;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public class ResultTreeListModel implements ListModel<SiriusResultElement> {
	
	private ArrayList<ListDataListener> listeners;
	
	private ArrayList<SiriusResultElement> trees;
	private JList<SiriusResultElement> list;
	
	public ResultTreeListModel(List<SiriusResultElement> results){
		listeners = new ArrayList<>(10);
		
		trees = new ArrayList<>(results);
	}
	
	public void setJList(JList<SiriusResultElement> list){
		this.list = list;
	}
	
	public ResultTreeListModel(){
		listeners = new ArrayList<>(10);
		
		trees = new ArrayList<>();
	}
	
	public void setData(List<SiriusResultElement> results){
		int size = this.trees.size();
		this.trees = new ArrayList<>(results.size());
		if(size>0){
			ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED,0,size-1);
			for(ListDataListener listener : listeners){
				listener.intervalRemoved(event);
			}
		}
		this.trees.addAll(results);
		if(this.trees.size()>0){
			ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED,0,trees.size()-1);
			for(ListDataListener listener : listeners){
				listener.intervalAdded(event);
			}
		}
		this.list.setPreferredSize(new Dimension(200*this.trees.size(),45));
		this.list.revalidate();
		this.list.repaint();
	}

	@Override
	public void addListDataListener(ListDataListener listener) {
		if(!listeners.contains(listener)) listeners.add(listener);
	}

	@Override
	public SiriusResultElement getElementAt(int index) {
		return trees.get(index);
	}

	@Override
	public int getSize() {
		return trees.size();
	}

	@Override
	public void removeListDataListener(ListDataListener listener) {
		listeners.remove(listener);
	}

}
