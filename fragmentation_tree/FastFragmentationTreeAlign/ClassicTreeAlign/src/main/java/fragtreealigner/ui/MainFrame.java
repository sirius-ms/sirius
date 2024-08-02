
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package fragtreealigner.ui;

import att.grappa.*;
import fragtreealigner.domainobjects.Alignment;
import fragtreealigner.domainobjects.AlignmentComparator;
import fragtreealigner.domainobjects.graphs.FragmentationTree;
import fragtreealigner.util.Session;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame implements ComponentListener, Serializable {
	private static final long serialVersionUID = 1L;
	private JTable fileTable;
	private JTable aligTable;
	private DefaultTableModel fileTableModel;
	private DefaultTableModel aligTableModel;
	private JSplitPane splitPaneLeftRight;
	private JSplitPane splitPaneTablesStructure;
	private JSplitPane splitPaneTable1Table2;
	private JSplitPane splitPaneGraph1Graph2;
	private JScrollPane scrollPaneGraph1;
	private JScrollPane scrollPaneGraph2;
	private JScrollPane scrollPaneStructure;
	private JPanel panelAlignmentList;
	private GrappaPanel grappaPanelGraph1;
	private GrappaPanel grappaPanelGraph2;
	private GrappaPanel grappaPanelStructure;
	private GraphPanelListener grappaPanelGraph1Listener;
	private GraphPanelListener grappaPanelGraph2Listener;
	private JComboBox comboBoxSorting;
	
	private int currentWindowWidth;
	private int currentWindowHeight;
	private int currentSpg1g2Width;
	private int currentSpg1g2Height;
	private int currentSpt1t2Height;
	
	private List< List<Alignment> > alignmentSets;
	private int selectedAlignmentSet;
	private int selectedAlignment;
	private List<String> files;

	private Session session;
	
	public MainFrame(Session session) {
		Locale.setDefault(Locale.US);
		this.session = session;
		alignmentSets = new ArrayList<List<Alignment>>();
		selectedAlignmentSet = -1;
		selectedAlignment = -1;
		files = new ArrayList<String>();

		initialize();
		addMenu();
		addListeners();
		
		currentSpg1g2Width = splitPaneGraph1Graph2.getWidth();
		currentSpg1g2Height = splitPaneGraph1Graph2.getHeight();
		currentSpt1t2Height = splitPaneTable1Table2.getHeight();
		
	    this.setPreferredSize(new Dimension(700, 500));
	    this.pack();
	    splitPaneLeftRight.setDividerLocation(500);
	    this.pack();
	    splitPaneGraph1Graph2.setDividerLocation(250);
	    this.pack();
	    splitPaneTablesStructure.setDividerLocation(300);
	    this.pack();
	    splitPaneTable1Table2.setDividerLocation(150);
	    currentWindowWidth = this.getWidth();
	    currentWindowHeight = this.getHeight();
	    this.setVisible(true);
	}

	private void initialize() {
	    this.setTitle( "Main frame" ); 
	    this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ); 
	    this.setResizable(true);
	    this.addComponentListener(this);
//	    this.setLayout(new FlowLayout());
	    

	    fileTableModel = new DefaultTableModel();
	    aligTableModel = new DefaultTableModel();
	    fileTableModel.addColumn("File");
	    aligTableModel.addColumn("Pos");
	    aligTableModel.addColumn("Score");
	    if (session.getParameters().computePlikeValue) aligTableModel.addColumn("p");
	    aligTableModel.addColumn("File");
	    
	    fileTable = new JTable( fileTableModel );
	    aligTable = new JTable( aligTableModel );
	    
	    fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    aligTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


	    comboBoxSorting = new JComboBox(new String[]{ "Score_0"});
	    panelAlignmentList = new JPanel();
	    panelAlignmentList.setLayout(new GridBagLayout());
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 0.5;
	    c.gridx = 0;
	    c.gridy = 0;
	    panelAlignmentList.add(new JLabel("Sorting"), c);
	    
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 0.5;
	    c.gridx = 1;
	    c.gridy = 0;
	    panelAlignmentList.add(comboBoxSorting, c);
	    
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    panelAlignmentList.add(new JScrollPane(aligTable),  c);
	    
	    splitPaneLeftRight = new JSplitPane();
	    splitPaneTablesStructure = new JSplitPane();    
		splitPaneTable1Table2 = new JSplitPane();
		splitPaneGraph1Graph2 = new JSplitPane();

		scrollPaneStructure = new JScrollPane();
	    scrollPaneGraph1 = new JScrollPane();
	    scrollPaneGraph2 = new JScrollPane();
	 
	    splitPaneGraph1Graph2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
	    splitPaneGraph1Graph2.setLeftComponent( scrollPaneGraph1 );
	    splitPaneGraph1Graph2.setRightComponent( scrollPaneGraph2 );
	    splitPaneGraph1Graph2.setContinuousLayout(true);
	    splitPaneGraph1Graph2.setDividerSize(2);

	    splitPaneTable1Table2.setOrientation(JSplitPane.VERTICAL_SPLIT);
	    splitPaneTable1Table2.setTopComponent( new JScrollPane(fileTable) );
	    splitPaneTable1Table2.setBottomComponent( panelAlignmentList );
	    splitPaneTable1Table2.setContinuousLayout(true);
	    splitPaneTable1Table2.setDividerSize(2);
	    
	    splitPaneTablesStructure.setOrientation(JSplitPane.VERTICAL_SPLIT);
	    splitPaneTablesStructure.setTopComponent( splitPaneTable1Table2 );
	    splitPaneTablesStructure.setBottomComponent( scrollPaneStructure );
	    splitPaneTablesStructure.setContinuousLayout(true);
	    splitPaneTablesStructure.setDividerSize(2);

	    splitPaneLeftRight.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
	    splitPaneLeftRight.setLeftComponent( splitPaneGraph1Graph2 );
	    splitPaneLeftRight.setRightComponent( splitPaneTablesStructure );
	    splitPaneLeftRight.setContinuousLayout(true);
	    splitPaneLeftRight.setDividerSize(2);

		grappaPanelGraph1 = new GrappaPanel(new Graph("empty"));
		grappaPanelGraph2 = new GrappaPanel(new Graph("empty"));
		grappaPanelStructure = new GrappaPanel(new Graph("empty"));
	    grappaPanelGraph1Listener = new GraphPanelListener( 1, grappaPanelGraph2, grappaPanelStructure );
	    grappaPanelGraph2Listener = new GraphPanelListener( 2, grappaPanelGraph1, grappaPanelStructure );
		grappaPanelGraph1.addGrappaListener(grappaPanelGraph1Listener);
		grappaPanelGraph2.addGrappaListener(grappaPanelGraph2Listener);
		grappaPanelStructure.addGrappaListener(new GrappaAdapter());
		grappaPanelGraph1.setScaleToFit(true);
		grappaPanelGraph2.setScaleToFit(true);
		grappaPanelStructure.setScaleToFit(true);

		scrollPaneGraph1.setViewportView(grappaPanelGraph1);
		scrollPaneGraph2.setViewportView(grappaPanelGraph2);
		scrollPaneStructure.setViewportView(grappaPanelStructure);
	
	    this.add(splitPaneLeftRight);
	}
	
	private void addMenu() {
	    JMenuBar menubar = new JMenuBar();
	    setJMenuBar(menubar);

	    JMenu menuFile = new JMenu("File");
	    menuFile.setMnemonic(KeyEvent.VK_F);
	    menubar.add(menuFile);

	    JMenu menuMisc = new JMenu("Misc");
	    menuMisc.setMnemonic(KeyEvent.VK_M);
	    menubar.add(menuMisc);

	    JMenuItem menuFileOpen = new JMenuItem("Open");
	    menuFileOpen.setMnemonic(KeyEvent.VK_C);
	    menuFileOpen.setToolTipText("Open an alignment set");
	    menuFileOpen.addActionListener(new ActionListener() {
	    	@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
	    		try {
	    			JFileChooser chooser = new JFileChooser();
	    			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    		    int returnVal = chooser.showOpenDialog(session.getMainFrame());
	    		    if(returnVal == JFileChooser.APPROVE_OPTION) {
	    		    	ObjectInputStream objectStream = new ObjectInputStream( new FileInputStream (chooser.getSelectedFile()) );
						alignmentSets = (ArrayList<List<Alignment>>)objectStream.readObject();
						files = (ArrayList<String>)objectStream.readObject();
						for (String file : files) {
							fileTableModel.addRow(new String[] { file });
						}
						selectedAlignmentSet = -1;
						selectedAlignment = -1;
	    		    }
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} 
	    	}
	    });
	    menuFile.add(menuFileOpen);
	    
	    JMenuItem menuFileSave = new JMenuItem("Save");
	    menuFileSave.setMnemonic(KeyEvent.VK_C);
	    menuFileSave.setToolTipText("Save an alignment set");
	    menuFileSave.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent event) {
	    		try {
	    			JFileChooser chooser = new JFileChooser();
	    		    int returnVal = chooser.showSaveDialog(session.getMainFrame());
	    		    if(returnVal == JFileChooser.APPROVE_OPTION) {
						ObjectOutputStream objectStream = new ObjectOutputStream( new FileOutputStream (chooser.getSelectedFile()) );
						MainFrame mainFrame = session.getMainFrame();
						session.setMainFrame(null);
						objectStream.writeObject(alignmentSets);
						objectStream.writeObject(files);
						session.setMainFrame(mainFrame);
	    		    }   		 
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
	    	}
	    });
	    menuFile.add(menuFileSave);

	    JMenuItem menuFileClose = new JMenuItem("Close");
	    menuFileClose.setMnemonic(KeyEvent.VK_C);
	    menuFileClose.setToolTipText("Exit application");
	    menuFileClose.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent event) {
	    		System.exit(0);
	    	}
	    });
	    menuFile.add(menuFileClose);

	    JMenuItem menuAllGraphics = new JMenuItem("Create all figures");
	    menuAllGraphics.setMnemonic(KeyEvent.VK_C);
	    menuAllGraphics.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent event) {
	    		for (List<Alignment> set : alignmentSets){
	    			for (Alignment alig : set){
	    				String name1 = alig.getTree1().getId();
	    				name1 = name1.substring(0, name1.length()-4);
	    				String name2 = alig.getTree2().getId();
	    				name2 = name2.substring(0, name2.length()-4);	    				
	    				alig.createGraphics(name1, name2);
	    			}
	    		}
	    	}
	    });
	    menuMisc.add(menuAllGraphics);

	    JMenuItem menuShowScores = new JMenuItem("Show scores");
	    menuShowScores.setMnemonic(KeyEvent.VK_S);
	    menuShowScores.setToolTipText("Show scores");
	    menuShowScores.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent event) {
	    		if (selectedAlignmentSet >= 0 && selectedAlignment >= 0) {
	    			alignmentSets.get(selectedAlignmentSet).get(selectedAlignment).getAlignmentResult().visualize();
	    		}
	    	}
	    });
	    menuMisc.add(menuShowScores);

	    JMenuItem menuFlipGraphPane = new JMenuItem("Flip graph pane");
	    menuFlipGraphPane.setMnemonic(KeyEvent.VK_F);
	    menuFlipGraphPane.setToolTipText("Flip graph pane");
	    menuFlipGraphPane.addActionListener(new ActionListener() {
	    	public void actionPerformed(ActionEvent event) {
	    		if (splitPaneGraph1Graph2.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
	    			splitPaneGraph1Graph2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
	    		} else {
	    			splitPaneGraph1Graph2.setOrientation(JSplitPane.VERTICAL_SPLIT);	    			
	    		}
	    	}
	    });
	    menuMisc.add(menuFlipGraphPane);

	}
	
	private void addListeners() {
	    fileTable.getSelectionModel().addListSelectionListener(
	    		new ListSelectionListener() {
	    			public void valueChanged(ListSelectionEvent event) {
	    				if (event.getSource() == fileTable.getSelectionModel() && fileTable.getRowSelectionAllowed()) {
	    					int selectedRow = fileTable.getSelectedRow();
	    					if (selectedRow != selectedAlignmentSet) showAlignmentSet(selectedRow);
	                    }
	                }
	            }
	    );

	    aligTable.getSelectionModel().addListSelectionListener(
	    		new ListSelectionListener() {
	    			public void valueChanged(ListSelectionEvent event) {
	    				if (event.getSource() == aligTable.getSelectionModel() && aligTable.getRowSelectionAllowed()) {
	    					int selectedRow = aligTable.getSelectedRow();
	    					if (selectedRow != alignmentSets.get(selectedAlignmentSet).size() - selectedAlignment - 1) {
	    						displayAlignment(alignmentSets.get(selectedAlignmentSet).get(aligTableModel.getRowCount() - selectedRow - 1));
	    						selectedAlignment = alignmentSets.get(selectedAlignmentSet).size() - selectedRow - 1;
	    					}
	                    }
	                }
	            }
	    );
	    
	    splitPaneGraph1Graph2.addComponentListener(
	    		new ComponentListener() {
					public void componentHidden(ComponentEvent e) {}
					public void componentMoved(ComponentEvent e) {}
					public void componentShown(ComponentEvent e) {}
					public void componentResized(ComponentEvent e) {
						if (splitPaneGraph1Graph2.getDividerLocation() > currentSpg1g2Width) {
							currentSpg1g2Width = e.getComponent().getWidth();
							return;
						}
						if (splitPaneGraph1Graph2.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
							double ratioWidth = (double) e.getComponent().getWidth() / currentSpg1g2Width;
							splitPaneGraph1Graph2.setDividerLocation((int) (splitPaneGraph1Graph2.getDividerLocation() * ratioWidth));
						} else {
							double ratioHeight = (double) e.getComponent().getHeight() / currentSpg1g2Height;
							splitPaneGraph1Graph2.setDividerLocation((int) (splitPaneGraph1Graph2.getDividerLocation() * ratioHeight));
						}
						currentSpg1g2Width = e.getComponent().getWidth();
						currentSpg1g2Height = e.getComponent().getHeight();
					}
	    		}
	    );
	    
	    splitPaneTable1Table2.addComponentListener(
	    		new ComponentListener() {
					public void componentHidden(ComponentEvent e) {}
					public void componentMoved(ComponentEvent e) {}
					public void componentShown(ComponentEvent e) {}
					public void componentResized(ComponentEvent e) {
						if (splitPaneTable1Table2.getDividerLocation() > currentSpt1t2Height) {
							currentSpt1t2Height = e.getComponent().getHeight();
							return;
						}
						double ratioHeight = (double) e.getComponent().getHeight() / currentSpt1t2Height;
						splitPaneTable1Table2.setDividerLocation((int) (splitPaneTable1Table2.getDividerLocation() * ratioHeight));
						currentSpt1t2Height = e.getComponent().getHeight();
					}
	    		}
	    );
	    
	    comboBoxSorting.addActionListener(
	    		new ActionListener() {
	    			public void actionPerformed(ActionEvent e) {
	    				showAlignmentSet(fileTable.getSelectedRow());
	    		    }
 	    		}
	    );
	}
	
	public void componentHidden(ComponentEvent arg0) {}
	public void componentMoved(ComponentEvent arg0) {}
	public void componentShown(ComponentEvent arg0) {}

	public void componentResized(ComponentEvent arg0) {
		adaptSplitPaneDividers(arg0.getComponent().getWidth(), arg0.getComponent().getHeight());
	}


	private void adaptSplitPaneDividers(int newWindowWidth, int newWindowHeight) {
		double ratioWidth = (double) newWindowWidth / currentWindowWidth;
		double ratioHeight = (double) newWindowHeight / currentWindowHeight;
//		System.out.println(newWindowWidth + " " + currentWindowWidth + " " + ratioWidth + " " + splitPaneLeftRight.getDividerLocation());
		splitPaneLeftRight.setDividerLocation((int) (splitPaneLeftRight.getDividerLocation() * ratioWidth));
		splitPaneTablesStructure.setDividerLocation((int) (splitPaneTablesStructure.getDividerLocation() * ratioHeight));
		currentWindowWidth = newWindowWidth;
		currentWindowHeight = newWindowHeight;
	}
	
	public void addAlignmentSet(String name, List<Alignment> alignments) {
		alignmentSets.add(alignments);
		fileTableModel.addRow(new String[] { name });
		files.add(name);
	}
	
	public void showAlignmentSet(int index) {
		List<Alignment> alignmentSet = alignmentSets.get(index);
		Alignment repAlignment = alignmentSet.get(0);
		int numScores = repAlignment.getScoreList().size() + 1;
		
		if (numScores != comboBoxSorting.getItemCount()) {
			comboBoxSorting.removeAllItems();
		    for (int i = 0; i < numScores; i++) {
		    	comboBoxSorting.addItem("Score_" + i);
		    }
		    comboBoxSorting.setSelectedIndex(0);
		}
		
		Collections.sort(alignmentSet, new AlignmentComparator(comboBoxSorting.getSelectedIndex()));
		
		aligTableModel.setRowCount(0);
		
		if (numScores != aligTableModel.getColumnCount() - 2) {
			aligTableModel.setColumnCount(0);
		    aligTableModel.addColumn("Pos");
		    for (int i = 0; i < numScores; i++) {
		    	aligTableModel.addColumn("Score_" + i);
		    }
		    if (session.getParameters().computePlikeValue) aligTableModel.addColumn("p");
		    aligTableModel.addColumn("File");
		}

		Alignment resAlignment = null;
		int pos = 1, selSorting = comboBoxSorting.getSelectedIndex();
		float score = Float.POSITIVE_INFINITY, currScore;
		String valueStr;
//		for (Alignment resAlignment : alignmentSets.get(index)) {
		
		DecimalFormat df = new DecimalFormat("0.00");
		for (int i = alignmentSet.size() - 1; i >= 0; i--) {
			resAlignment = alignmentSet.get(i);
			if (selSorting == 0) currScore = resAlignment.getScore();
			else currScore = resAlignment.getScoreList().get(selSorting - 1);
			if (currScore < score) pos = alignmentSet.size() - i - 1;
			Vector<String> values = new Vector<String>();
			values.add(Integer.toString(pos));
			valueStr = df.format(resAlignment.getScore());
			values.add(valueStr);
		    for (int j = 0; j < numScores - 1; j++) {
				valueStr = df.format(resAlignment.getScoreList().get(j));
				values.add(valueStr);
		    }
		    if (session.getParameters().computePlikeValue) {
		    	valueStr = Float.toString(resAlignment.getPlikeValue());
		    	values.add(valueStr);
		    }
			values.add(resAlignment.getTree2().getId());
			String[] valuesStr = new String[values.size()];
			valuesStr = values.toArray(valuesStr);
			aligTableModel.addRow(valuesStr);
			score = currScore;
//			aligTableModel.addRow(new String[] { Integer.toString(pos), Float.toString(score), resAlignment.getTree2().getId() });
		}
		selectedAlignmentSet = index;
		selectedAlignment = alignmentSet.size() - 1;
		aligTable.getSelectionModel().setSelectionInterval(0, 0);
		displayAlignment(alignmentSet.get(alignmentSet.size() -1));
	}
	
	public void displayAlignment(Alignment alignment) {
		StringWriter dotContentGraph1 = new StringWriter();
		StringWriter dotContentGraph2 = new StringWriter();
		StringWriter dotContentGraph3 = new StringWriter();
		FragmentationTree fragTree = alignment.getTree2().getCorrespondingFragTree();
		
		try {
			alignment.writeToDot(new BufferedWriter(dotContentGraph1), true, false);
			alignment.writeToDot(new BufferedWriter(dotContentGraph2), false, true);
			if (fragTree != null && fragTree.getRoot().getCompound() != null &&
					fragTree.getRoot().getCompound().getMolecularStructure() != null) {
				fragTree.getRoot().getCompound().getMolecularStructure().writeToDot(new BufferedWriter(dotContentGraph3));
			}
		} catch (IOException e) {
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}

		Graph graph1 = parseGraph(dotContentGraph1);
		Graph graph2 = parseGraph(dotContentGraph2);
		Graph graph3 = parseGraph(dotContentGraph3);	
		
	    graph1.addPanel(grappaPanelGraph1);
	    setSubgraphOfPanel(grappaPanelGraph1, graph1);
        setSubgraphOfPanel(grappaPanelGraph2, graph2);
        setSubgraphOfPanel(grappaPanelStructure, graph3);
        //grappaPanelGraph1.setSubgraph(graph1);
	    graph2.addPanel(grappaPanelGraph2);
	    //grappaPanelGraph2.setSubgraph(graph2);
	    graph3.addPanel(grappaPanelStructure);
	    //grappaPanelStructure.setSubgraph(graph3);
	    
		layoutGraph(graph1);
		layoutGraph(graph2);		
		layoutGraph(graph3, "formatNeato");
		
		grappaPanelGraph1Listener.setAlignment(alignment);
		grappaPanelGraph2Listener.setAlignment(alignment);		
	}

    /*
     * because I don't find the source for the Grappa 1.5 version (even on the official site the
     * version is 1.2 oO) I add the setSubgraph method to this class and use the 1.2 version.
     * sourcecode is disassambled from the binary
     * @param panel
     * @param sub
     */
    static Field fsubgraph;
    static Field fbacker;
    static Field fgraph;
    static Field fselectionStyle;
    static Field fdeletionStyle;
    static {
        try {
        fsubgraph = GrappaPanel.class.getField("subgraph");
        fbacker =  GrappaPanel.class.getField("backer");
        fgraph =  GrappaPanel.class.getField("graph");
        fselectionStyle = GrappaPanel.class.getField("selectionStyle");
        fdeletionStyle = GrappaPanel.class.getField("deletionStyle");
        } catch (NoSuchFieldException exc) {
         throw new RuntimeException(exc);
        }
    }

    private void setSubgraphOfPanel(GrappaPanel panel, Subgraph sub) {
        try {
            fsubgraph.set(panel, sub);
            fbacker.set(panel, null);
            final Graph g = sub.getGraph();
            fgraph.set(panel, g);
            panel.addAncestorListener(panel); // häh???
            panel.addComponentListener(panel); // häh???
            fselectionStyle.set(panel, g.getGrappaAttribute("grappaSelectionColor"));
            fdeletionStyle.set(panel, g.getGrappaAttribute("grappaDeletionColor"));
        } catch (IllegalAccessException exc) {
            throw new RuntimeException(exc);
        }
    }

	private Graph parseGraph(StringWriter dotContent) {
		ByteArrayInputStream bs = new ByteArrayInputStream(dotContent.toString().getBytes());
		Parser parser = new Parser(bs);
//		DotParser parser = new DotParser(bs, System.err);
		
		try {
			//program.debug_parse(4);
			parser.parse();
		} catch(Exception e) {
			System.err.println("Exception: " + e.getMessage());
			e.printStackTrace();
		}
		
		return parser.getGraph();
//	    System.err.println("The graph contains " + graph.countOfElements(Grappa.NODE|Grappa.EDGE|Grappa.SUBGRAPH) + " elements.");
	}
	
	private void layoutGraph(Graph graph) {
		layoutGraph(graph, "formatDot");
	}
	
	private void layoutGraph(Graph graph, String layoutScript) {
	    Object connector = null;
	    try {
	    	connector = Runtime.getRuntime().exec("./" + layoutScript);
	    } catch(Exception ex) {
	    	System.err.println("Exception while setting up Process: " + ex.getMessage() + "\nTrying URLConnection...");
	    	connector = null;
	    }
	    if(connector == null) {
		try {
		    connector = (new URL("http://www.research.att.com/~john/cgi-bin/format-graph")).openConnection();
		    URLConnection urlConn = (URLConnection)connector;
		    urlConn.setDoInput(true);
		    urlConn.setDoOutput(true);
		    urlConn.setUseCaches(false);
		    urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		} catch(Exception ex) {
		    System.err.println("Exception while setting up URLConnection: " + ex.getMessage() + "\nLayout not performed.");
		    connector = null;
		}
	    }
	    if(connector != null) {
	    	if(!GrappaSupport.filterGraph(graph,connector)) {
	    		System.err.println("ERROR: somewhere in filterGraph");
	    	}
	    	if(connector instanceof Process) {
	    		try {
	    			int code = ((Process)connector).waitFor();
	    			if(code != 0) {
	    				System.err.println("WARNING: proc exit code is: " + code);
	    			}
	    		} catch(InterruptedException ex) {
	    			System.err.println("Exception while closing down proc: " + ex.getMessage());
	    			ex.printStackTrace(System.err);
	    		}
	    	}
	    	connector = null;
	    }
	    graph.repaint();
	}
}
