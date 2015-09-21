package de.unijena.bioinf.sirius.gui.load;

import gnu.trove.list.array.TDoubleArrayList;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactSpectrum;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import java.util.List;
import java.util.Vector;

public class CSVDialog extends JDialog implements ActionListener, ChangeListener{

	private JComboBox<String> massColumn, intColumn;
	private JTable table;
	private JButton ok, abort;
	private JTextField cEField;
	
	private JSpinner minEnergy, maxEnergy, colEnergy;
	private JCheckBox cb;
	private JPanel cardPanel;
	private CardLayout cardLayout;
	
	private JComboBox<String> msLevelBox;
	
	private CardLayout cl;
	private JPanel cEInnerPanel;
	
	private UneditableTableModel dtm;
	
	private static String RAMP_S = "ramp";
	private static String SINGLE_S = "single";
	
	private int columnNumber;
	private int rowNumber;
	private int currentMassColumn;
	private int currentIntensityColumn;
	
	private List<TDoubleArrayList> data;
	
	private ReturnValue returnValue;
	
	private DefaultComboBoxModel<String> massModel, intModel;
	
	public CSVDialog(JDialog owner,List<TDoubleArrayList> data) {
		super(owner,"CSV",true);
		
		returnValue = ReturnValue.Success;
		
		this.data = data;
		
		this.setLayout(new BorderLayout());
		
		columnNumber = data.get(0).size();
		rowNumber = data.size();
		currentMassColumn = 0;
		currentIntensityColumn = 1;
		
		massModel = new DefaultComboBoxModel<>();
		intModel = new DefaultComboBoxModel<>();
		for(int i=1;i<=columnNumber;i++){
			massModel.addElement("column "+i);
			intModel.addElement("column "+i);
		}
		massColumn = new JComboBox<String>(massModel);
		intColumn = new JComboBox<String>(intModel);
		
		massColumn.setSelectedIndex(0);
		intColumn.setSelectedIndex(1);
		
		massColumn.addActionListener(this);
		intColumn.addActionListener(this);
		
		JPanel columnPanel = new JPanel(new BorderLayout());
		columnPanel.setBorder(BorderFactory.createEtchedBorder());
		JPanel columnControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		columnControlPanel.add(new JLabel("mass"));
		columnControlPanel.add(massColumn);
		columnControlPanel.add(new JLabel("intensity"));
		columnControlPanel.add(intColumn);
		columnPanel.add(columnControlPanel,BorderLayout.NORTH);
		this.add(columnPanel,BorderLayout.CENTER);
		
		JPanel propertiesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		columnPanel.add(propertiesPanel,BorderLayout.SOUTH);
		
		JPanel msLevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		msLevelPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"ms level"));
		propertiesPanel.add(msLevelPanel);
		String[] msLevelVals = {"MS 1","MS 2"};
		msLevelBox = new JComboBox<>(msLevelVals);
		msLevelBox.setSelectedIndex(1);
		JPanel msTempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		msTempPanel.add(msLevelBox);
		msLevelPanel.add(msTempPanel);
		
		JPanel cELevelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		cELevelPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"collision energy"));
		propertiesPanel.add(cELevelPanel);
		
		JPanel rampPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		minEnergy = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
		maxEnergy = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
		minEnergy.setEditor(new JSpinner.NumberEditor(minEnergy, "##0.#"));
		maxEnergy.setEditor(new JSpinner.NumberEditor(maxEnergy, "##0.#"));
		minEnergy.addChangeListener(this);
		maxEnergy.addChangeListener(this);
		rampPanel.add(new JLabel("minimal energy"));
		rampPanel.add(minEnergy);
		rampPanel.add(new JLabel("maximal energy"));
		rampPanel.add(maxEnergy);
		
		JPanel singlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		colEnergy = new JSpinner(new SpinnerNumberModel(0, 0, 100, 0.1));
		colEnergy.setEditor(new JSpinner.NumberEditor(colEnergy, "##0.#"));
		singlePanel.add(new JLabel("collision energy"));
		singlePanel.add(colEnergy);
		
		cl = new CardLayout();
		cEInnerPanel = new JPanel(cl);
		cEInnerPanel.add(singlePanel,SINGLE_S);
		cEInnerPanel.add(rampPanel,RAMP_S);
		cl.show(cEInnerPanel, SINGLE_S);
		cELevelPanel.add(cEInnerPanel);
		
		cb = new JCheckBox("ramp");
		cb.setSelected(false);
		cb.addActionListener(this);
		JPanel cbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		cbPanel.add(cb);
		cELevelPanel.add(cbPanel);
		
		dtm = new UneditableTableModel(rowNumber, columnNumber);
		Vector<String> columnNames = new Vector<>();
		columnNames.add("mass (column 1)");
		columnNames.add("intensity (column 2)");
		for(int i=2;i<columnNumber;i++){
			columnNames.add("column "+(i+1));
		}
		dtm.setColumnIdentifiers(columnNames);
		for(int i=0;i<columnNumber;i++){
			for(int j=0;j<rowNumber;j++){
				dtm.setValueAt(data.get(j).get(i), j, i);
			}
		}
		
		table = new JTable(dtm);
		JScrollPane jsb = new JScrollPane(table);
		columnPanel.add(jsb,BorderLayout.CENTER);
		table.setDefaultRenderer(Object.class, new ColoredTableCellRenderer());
		
		
		ok = new JButton("ok");
		ok.addActionListener(this);
		abort = new  JButton("abort");
		abort.addActionListener(this);
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		controlPanel.add(ok);
		controlPanel.add(abort);
		this.add(controlPanel,BorderLayout.SOUTH);
		
		this.setSize(new Dimension(640,480));
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==cb){
			if(cb.isSelected()){
				cl.show(cEInnerPanel, RAMP_S);
			}else{
				cl.show(cEInnerPanel, SINGLE_S);
			}
		}else if(e.getSource()==massColumn||e.getSource()==intColumn){
			int massIndex = massColumn.getSelectedIndex();
			int intIndex = intColumn.getSelectedIndex();
			
			if(massIndex==intIndex){
				if(e.getSource()==massColumn){
					intColumn.removeActionListener(this);
					if(intIndex+1 > columnNumber-1){
						intIndex--;
					}else{
						intIndex++;
					}
					intColumn.setSelectedIndex(intIndex);
					intColumn.addActionListener(this);
				}else{
					massColumn.removeActionListener(this);
					if(massIndex+1 > columnNumber-1){
						System.out.println("way1: "+massIndex+" "+columnNumber);
						massIndex--;
					}else{
						System.out.println("way2: "+massIndex+" "+columnNumber);
						massIndex++;
					}
					massColumn.setSelectedIndex(massIndex);
					massColumn.addActionListener(this);
				}
			}
			
			this.currentMassColumn = massIndex;
			this.currentIntensityColumn = intIndex;
			
			System.out.println(currentMassColumn+" "+currentIntensityColumn);
			
			Vector<String> newNames = new Vector<>();
			newNames.add("mass (column "+(currentMassColumn+1)+")");
			newNames.add("intensity (column "+(currentIntensityColumn+1)+")");
			for(int i=0;i<columnNumber;i++){
				if(i==currentIntensityColumn||i==currentMassColumn) continue;
				newNames.add("column "+(i+1));
			}
			dtm.setColumnIdentifiers(newNames);
			
			for(int i=0;i<rowNumber;i++){
				dtm.setValueAt(data.get(i).get(currentMassColumn), i, 0);
				dtm.setValueAt(data.get(i).get(currentIntensityColumn),i,1);
			}
			
			int counter=2;
			for(int column=0;column<columnNumber;column++){
				if(column==currentIntensityColumn||column==currentMassColumn) continue;
				for(int row=0;row<rowNumber;row++){
					dtm.setValueAt(data.get(row).get(column), row, counter);
				}
				counter++;
			}
			
			System.out.println(massIndex+" "+intIndex);
		}else if(e.getSource()==ok){
			this.returnValue = ReturnValue.Success;
			this.setVisible(false);
		}else if(e.getSource()==abort){
			this.returnValue = ReturnValue.Abort;
			this.setVisible(false);
		}
	}
	
	public ReturnValue getReturnValue(){
		return returnValue;
	}
	
	public CompactSpectrum getSpectrum(){
		if(returnValue==ReturnValue.Abort){
			return null;
		}else{
			double[] masses = new double[rowNumber];
			double[] ints = new double[rowNumber];
			for(int i=0;i<rowNumber;i++){
				masses[i] = data.get(i).get(currentMassColumn);
				ints[i] = data.get(i).get(currentIntensityColumn);
			}
			DefaultCompactSpectrum sp = new DefaultCompactSpectrum(masses,ints);
			sp.setMSLevel(msLevelBox.getSelectedIndex()+1);
			if(cb.isSelected()){
				sp.setCollisionEnergy(new CollisionEnergy((Double) minEnergy.getValue(), (Double) maxEnergy.getValue()));
			}else{
				double cE = (Double) colEnergy.getValue();
				sp.setCollisionEnergy(new CollisionEnergy(cE,cE));
			}
			return sp;
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Double val1 = (Double) minEnergy.getValue();
		Double val2 = (Double) maxEnergy.getValue();
		if(val1>val2){
			maxEnergy.setValue(val1);
		}
	}

}

class UneditableTableModel extends DefaultTableModel{
	
	public UneditableTableModel(int rowNumber, int columnNumber) {
		super(rowNumber,columnNumber);
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
        return false;
    }
}

class ColoredTableCellRenderer extends DefaultTableCellRenderer{

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		if(isSelected){
			return  super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
		}else{
			JLabel label = new JLabel(value.toString());
			if(column<2){
				label.setForeground(Color.black);
			}else{
				label.setForeground(Color.gray);
			}
			return label;
		}
	}
	
}
