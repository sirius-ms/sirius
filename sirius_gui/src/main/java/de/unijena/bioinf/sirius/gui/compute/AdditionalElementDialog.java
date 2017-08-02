package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class AdditionalElementDialog extends JDialog implements ActionListener{

	PeriodicTable periodicTable;

	private HashSet<String> elementMap;
	private static final HashSet<String> dontEnable = new HashSet<>(Arrays.asList(new String[]{"C", "H", "N", "O", "P"}));
	
	private JButton ok, abort;
	private JPanel rareElements;

	private boolean success;
	
	public AdditionalElementDialog(Window owner,Collection<String> selectedRareElements) {
		super(owner,"additional elements",Dialog.DEFAULT_MODALITY_TYPE);
		
		success = false;

		periodicTable = PeriodicTable.getInstance();

		elementMap = new HashSet<>(selectedRareElements);
		
		this.setLayout(new BorderLayout());
		
		rareElements = new JPanel(new GridLayout(6,18));
		rareElements.setBorder(BorderFactory.createEtchedBorder());
//		rareElements.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"elements"));

		String[] row1 = {"H","","","","","","","","","","","","","","","","","He"};
		addRow(row1);
		String[] row2 = {"Li","Be","","","","","","","","","","","B","C","N","O","F","Ne"};
		addRow(row2);
		String[] row3 = {"Na","Mg","","","","","","","","","","","Al","Si","P","S","Cl","Ar"};
		addRow(row3);
		String[] row4 = {"K","Ca","Sc","Ti","V","Cr","Mn","Fe","Co","Ni","Cu","Zn","Ga","Ge","As","Se","Br","Kr"};
		addRow(row4);
		String[] row5 = {"Rb","Sr","Y","Zr","Nb","Mo","Tc","Ru","Rh","Pd","Ag","Cd","In","Sn","Sb","Te","I","Xe"};
		addRow(row5);
		String[] row6 = {"Cs","Ba","Lu","Hf","Ta","W","Re","Os","Ir","Pt","Au","Hg","Ti","Pb","Bi","Po","At","Rn"};

		addRow(row6);
		this.add(rareElements,BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		ok = new JButton("ok");
		ok.addActionListener(this);
		abort = new JButton("abort");
		abort.addActionListener(this);
		southPanel.add(ok);
		southPanel.add(abort);
		this.add(southPanel,BorderLayout.SOUTH);
		
		
		this.pack();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}

	private void addRow(String[] row){
		for(String s : row){
			if(s.isEmpty()){
				rareElements.add(new JLabel(""));
			}else{
				JToggleButton button = new JToggleButton(s);
				button.setToolTipText(periodicTable.getByName(s).getName());
				if(dontEnable.contains(s)||s.isEmpty()) button.setEnabled(false);
				if(elementMap.contains(s)) button.setSelected(true);
				button.addActionListener(this);
				rareElements.add(button);
			}
		}
	}

	public boolean successful(){
		return success;
	}
	
	public Collection<String> getSelectedElements(){
		return Collections.unmodifiableCollection(this.elementMap);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if(o instanceof JToggleButton){
			JToggleButton b = (JToggleButton) o;
			if(b.isSelected()){
				this.elementMap.add(b.getText());
			}else{
				this.elementMap.remove(b.getText());
			}
		}else if(e.getSource() == this.ok){
			success = true;
			this.setVisible(false);
		}else if(e.getSource() == this.abort){
			success = false;
			this.setVisible(false);
		}
	}
	
}