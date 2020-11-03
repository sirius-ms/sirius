
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

package fragtreealigner;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses.ScoreWeightingType;
import fragtreealigner.algorithm.TreeAligner;
import fragtreealigner.algorithm.TreeAligner.NormalizationType;
import fragtreealigner.domainobjects.Alignment;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabase;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabase.DecoyType;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabaseEntry;
import fragtreealigner.domainobjects.graphs.AlignmentTree;
import fragtreealigner.domainobjects.graphs.FragmentationTree;
import fragtreealigner.ui.MainFrame;
import fragtreealigner.util.Macros;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;
import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Iterator;

public class FragmentationTreeAligner {
	public enum ExecutionMode { NORMAL, PARAMETER_OPTIMIZATION }

	public static void main(String[] args) throws IOException, InterruptedException {
		Options opt = new Options(args, 1);
		opt.addSet("setHelp").addOption("h");
		opt.addSet("setDB").addOption("db", Separator.BLANK)
		                   .addOption("q", Separator.BLANK, Multiplicity.ZERO_OR_ONE)
		                   .addOption("decoy", Separator.BLANK, Multiplicity.ZERO_OR_ONE)
		                   .addOption("decoydb", Separator.BLANK, Multiplicity.ZERO_OR_ONE)		                   
		                   .addOption("altdb", Separator.BLANK, Multiplicity.ZERO_OR_ONE) // database from which the decoy db is built		                   
		                   .addOption("matrix", Multiplicity.ZERO_OR_ONE)

		                   .addOption("matrixExt", Multiplicity.ZERO_OR_ONE) // zus. Baum groesse, p value
		                   .addOption("gui", Multiplicity.ZERO_OR_ONE);
		opt.addSet("setAlign").addOption("tree1", Separator.BLANK)
							.addOption("tree2", Separator.BLANK)
							.addOption("gui", Multiplicity.ZERO_OR_ONE);
		opt.addSet("setParamOpt").addOption("paramOpt")
		                         .addOption("db", Separator.BLANK)
		                         .addOption("q", Separator.BLANK, Multiplicity.ZERO_OR_ONE)
		                         .addOption("decoy", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		opt.addSet("setTest").addOption("test");
		opt.addSet("setGui").addOption("gui");
		opt.addSet("setRuntime").addOption("runtime").addOption("numNodes", Separator.BLANK).addOption("outDegree", Separator.BLANK);
		opt.addOptionAllSets("scoreSimilarNLMass", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("global", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("endGapFree", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("union", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("noCnl", Multiplicity.ZERO_OR_ONE); // Common neutral losses
		opt.addOptionAllSets("noRDiff", Multiplicity.ZERO_OR_ONE); // reasonable difference 
		opt.addOptionAllSets("noRDiffH2", Multiplicity.ZERO_OR_ONE); // reasonable difference mit H2
		opt.addOptionAllSets("noH2", Multiplicity.ZERO_OR_ONE); // difference of H2 disabled
		opt.addOptionAllSets("normal", Separator.BLANK, Multiplicity.ZERO_OR_ONE); // all moeglich!
		opt.addOptionAllSets("weight", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("scoring", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("pLikeValue", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("pullUps", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("accuracy", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
//		opt.addOptionAllSets("gui", Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("verbose", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("statistics", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		opt.addOptionAllSets("Dseparately", Multiplicity.ZERO_OR_ONE); // treat D as element different from H
        opt.addOptionAllSets("accuracyPen", Separator.BLANK, Multiplicity.ZERO_OR_ONE); // range of the mass accuracy manipulator (-x,+x)
        opt.addOptionAllSets("statout", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("nodelabels", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("cnlSizeDependent", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("NLandNodes", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("useOnlyNodeBonus", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("nodeEquality",Separator.BLANK, Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("NLEquality",Separator.BLANK, Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("oneNodePenalty", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("scoreRoot", Multiplicity.ZERO_OR_ONE);
        opt.addOptionAllSets("gap", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
        
		OptionSet set = opt.getMatchingSet();

		if (set == null) {
			printUsage();
			System.exit(1);
		}
		
		Session session = new Session();
		Parameters params = new Parameters(session);
		session.setParameters(params);

		if (set.isSet("global")){
			params.makeLocalAlignment = false;
			params.makeEndGapFreeAlignment = false;
			params.makeGlobalAlignment = true;
		}else if (set.isSet("endGapFree")){
			params.makeLocalAlignment = false;
			params.makeEndGapFreeAlignment = true;
			params.makeGlobalAlignment = false;
		}
		else{
			params.makeLocalAlignment = true;
			params.makeEndGapFreeAlignment = false;
			params.makeGlobalAlignment = false;
		}
		if (set.isSet("union")) params.isNodeUnionAllowed = true;
		else params.isNodeUnionAllowed = false;
		if (set.isSet("noCnl")) params.useCnl = false;
		else params.useCnl = true;
		if (set.isSet("noRDiff")) params.testRDiff = false;
		else params.testRDiff = true;
		if (set.isSet("noRDiffH2")) params.testRDiffH2 = false;
		else params.testRDiffH2 = true;
		if (set.isSet("noH2")) params.testH2 = false;
		else params.testH2 = true;		
		if (set.isSet("cnlSizeDependent")) {
			params.cnlSizeDependent = true;
		}
		if (set.isSet("scoreSimilarNLMass")) params.testSimilarNLMass = true;
		else params.testSimilarNLMass = false;
		if (set.isSet("accuracy")) {
			try {
				params.ppm_error = Integer.parseInt(set.getOption("accuracy").getResultValue(0).trim());
			} catch (NumberFormatException e) {
                System.err.println(set.getOption("accuracy").getResultValue(0).trim());
				System.err.println("Invalid number given after accuracy using default of "+params.ppm_error);
			}
		}
        if (set.isSet("nodeEquality")) {
            try {
				params.scoreNodeEquality = Integer.parseInt(set.getOption("nodeEquality").getResultValue(0).split("\\+")[0].trim());
                params.scoreNodeInequality = -1 * Integer.parseInt(set.getOption("nodeEquality").getResultValue(0).split("\\+")[0].trim());
			} catch (NumberFormatException e) {
                System.err.println(set.getOption("nodeEquality").getResultValue(0).split("\\+")[0].trim());
				System.err.println("Invalid number given after nodeEquality using default of "+params.scoreNodeEquality);
			}

            try {
				params.scoreNodeEqualityPerAtom = Integer.parseInt(set.getOption("nodeEquality").getResultValue(0).split("\\+")[1].trim());
                params.scoreNodeInequalityPerAtom = -1* Integer.parseInt(set.getOption("nodeEquality").getResultValue(0).split("\\+")[1].trim());
			} catch (NumberFormatException e) {
                System.err.println(set.getOption("nodeEquality").getResultValue(0).split("\\+")[1].trim());
				System.err.println("Invalid number given after nodeEquality using default of "+params.scoreNodeEqualityPerAtom);
			}


		}
        if (set.isSet("NLEquality")) {
            try {
				params.scoreEquality = Integer.parseInt(set.getOption("NLEquality").getResultValue(0).split("\\+")[0].trim());
			} catch (NumberFormatException e) {
                System.err.println(set.getOption("NLEquality").getResultValue(0).split("\\+")[0].trim());
				System.err.println("Invalid number given after NLEquality using default of "+params.scoreEquality);
			}

            try {
				params.scoreEqualityPerAtom = Integer.parseInt(set.getOption("NLEquality").getResultValue(0).split("\\+")[1].trim());
			} catch (NumberFormatException e) {
                System.err.println(set.getOption("NLEquality").getResultValue(0).split("\\+")[1].trim());
				System.err.println("Invalid number given after NLEquality using default of "+params.scoreEqualityPerAtom);
			}


		}
		if (set.isSet("verbose")) params.makeVerboseOutput = true;
		else params.makeVerboseOutput = false;
		if (set.isSet("normal")) {
			if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("tree_size_arithmetic")) params.normalizationType = NormalizationType.TREE_SIZE_ARITHMETIC;
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("tree_size_geometric")) params.normalizationType = NormalizationType.TREE_SIZE_GEOMETRIC;
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("self_alig_arithmetic")) params.normalizationType = NormalizationType.SELF_ALIG_ARITHMETIC;
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("self_alig_geometric")) params.normalizationType = NormalizationType.SELF_ALIG_GEOMETRIC;
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("self_alig_min")) params.normalizationType = NormalizationType.SELF_ALIGN_MIN;
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("p_value")) {
				params.normalizationType = NormalizationType.P_VALUE;
				try{
					BufferedReader r = FileUtils.ensureBuffering(new FileReader("evdParamList"));
					TreeAligner.readStatisticalParameter(r);
				} catch (IOException e) {
					System.err.println("unable to read distribution parameters");
					params.normalizationType = NormalizationType.SELF_ALIGN_MIN;					
				}
			}
			else if (set.getOption("normal").getResultValue(0).equalsIgnoreCase("all")) params.normalizationType = NormalizationType.ALL;
			else System.err.println("Unknown normalization type!");
		}
		else params.normalizationType = NormalizationType.NONE;
		if (set.isSet("weight")) {
			if (set.getOption("weight").getResultValue(0).equalsIgnoreCase("node_weight")) params.scoreWeightingType = ScoreWeightingType.NODE_WEIGHT;
			else if (set.getOption("weight").getResultValue(0).equalsIgnoreCase("nl_freq")) params.scoreWeightingType = ScoreWeightingType.NEUTRAL_LOSS_FREQUENCY;
			else System.err.println("Unknown score weighting type!");
		}
		else params.scoreWeightingType = ScoreWeightingType.NONE;

		if (set.isSet("scoring")) {
			params.setScores(FileUtils.ensureBuffering(new FileReader(set.getOption("scoring").getResultValue(0))));
		}
		if (set.isSet("pLikeValue")) {
			params.computePlikeValue = true;
			params.runsPlikeValue = Integer.valueOf(set.getOption("pLikeValue").getResultValue(0));
		} else params.computePlikeValue = false;
		if (set.isSet("pullUps")) {
			params.considerPullUps = true;
		} else params.considerPullUps = false;
		if (set.isSet("statistics")) {
			params.calcStatistics = Integer.valueOf(set.getOption("statistics").getResultValue(0));
			if (set.isSet("statout")){
				params.statOutDir = set.getOption("statout").getResultValue(0);
			} else {
				params.statOutDir = "/home/m3rafl/workspace/FragmentationTreeAligner/pValStatistics";
			}
		} else {
			params.calcStatistics = 0;
		}
		
		if (set.isSet("gui")) params.makeGraphicalOutput = true;
		else params.makeGraphicalOutput = false;
		if (set.isSet("Dseparately")) params.DmatchesH = false;
		else params.DmatchesH = true;
		if (set.isSet("accuracyPen")) {
			params.manipStrength = Integer.parseInt(set.getOption("accuracyPen").getResultValue(0));
		}
		if (set.isSet("nodelabels")){
			params.useNodeLabels = true;
		}
		if (set.isSet("NLandNodes")){
			params.useNLandNodes = true;
			params.scoreInequality = -2;
			params.scoreInequalityPerAtom = -0.5f;
		}
        if (set.isSet("useOnlyNodeBonus")){
            params.useOnlyNodeBonus=true;
        }
		if (set.isSet("oneNodePenalty")){
			params.oneNodePenalty = true;
		}
		if (set.isSet("scoreRoot")){
			params.scoreRoot = true;
		}
		
        if (set.isSet("gap")){
            params.scoreGap = - Float.parseFloat(set.getOption("gap").getResultValue(0));
            System.err.println("gap score: "+params.scoreGap);
        }
		
		long time = -System.currentTimeMillis();
		if (set.getSetName().equals("setHelp")) {
			printUsage();
			System.exit(0);
		} else if (set.getSetName().equals("setGui")) {
			new MainFrame(session);
		} else if (set.getSetName().equals("setDB")) {
			if (set.isSet("decoy")) {
				if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("db")) params.decoyType = DecoyType.DB;
				else if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("random")) params.decoyType = DecoyType.RANDOM;
				else if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("reverse")) params.decoyType = DecoyType.REVERSE;
				else System.err.println("Unknown decoy type!");
			}
			else if (set.isSet("altdb")) {
				params.decoyType = DecoyType.ALT_DB;
			}
			else {
				params.decoyType = DecoyType.NONE;
			}
			if (set.isSet("matrix")) {
//				if (params.makeGraphicalOutput) {
//					System.err.println("Option -gui is not compatible with option -matrix!\n -matrix will be ignored.");
//				} else params.makeMatrixOutput = true;
				params.makeMatrixOutput = true;
			} else params.makeMatrixOutput = false;
			if (set.isSet("matrixExt")) {
				params.makeMatrixOutput = true;
				params.makeMatrixExtOutput = true;
			} else params.makeMatrixExtOutput = false;
			String db = set.getOption("db").getResultValue(0);
			String query = (set.isSet("q")) ? set.getOption("q").getResultValue(0) : db;
			String altDb = (set.isSet("altdb")) ? set.getOption("altdb").getResultValue(0) : null;
			String decoyDb = (set.isSet("decoydb")) ? set.getOption("decoydb").getResultValue(0) : null;
			//System.out.println(altDb);
			compareWithDatabase(db, query, altDb, decoyDb, session);
		} else if (set.getSetName().equals("setAlign")) {
			align(set.getOption("tree1").getResultValue(0), set.getOption("tree2").getResultValue(0), session);
		} else if (set.getSetName().equals("setParamOpt")) {
			if (set.isSet("decoy")) {
				if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("db")) params.decoyType = DecoyType.DB;
				else if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("random")) params.decoyType = DecoyType.RANDOM;
				else if (set.getOption("decoy").getResultValue(0).equalsIgnoreCase("reverse")) params.decoyType = DecoyType.REVERSE;
				else System.err.println("Unknown decoy type!");
			}
			else params.decoyType = DecoyType.NONE;
			String db = set.getOption("db").getResultValue(0);
			String query = (set.isSet("q")) ? set.getOption("q").getResultValue(0) : db;
			parameterOptimization(db, query, session);
		} else if (set.getSetName().equals("setTest")) {
			testFunc();
		} else if (set.getSetName().equals("setRuntime")) {
			runningTimeDetermination(Integer.valueOf(set.getOption("numNodes").getResultValue(0)), Integer.valueOf(set.getOption("outDegree").getResultValue(0)), session);
		}
		time += System.currentTimeMillis();
		if (!params.makeMatrixOutput) System.out.println(((float)time / 1000) + "s");
	
//		if (args.length > 0) {
//			if (args[0].equalsIgnoreCase("all")) allAgainstAll(Boolean.parseBoolean(args[1]), Boolean.parseBoolean(args[2]));
//			else if (args[0].equalsIgnoreCase("db")) compareWithDatabase(args[1], args[2], Boolean.parseBoolean(args[3]), Boolean.parseBoolean(args[4]), Boolean.parseBoolean(args[5]));
//			else if (args[0].equalsIgnoreCase("paramOpt")) parameterOptimization(args[1], args[2], Boolean.parseBoolean(args[3]), Boolean.parseBoolean(args[4]), Boolean.parseBoolean(args[5]));
//			else if (args[0].equalsIgnoreCase("testFunc")) testFunc();
//			else if (args[0].equalsIgnoreCase("gui")) new MainFrame();
//			else align(args[0], args[1], Boolean.parseBoolean(args[2]), Boolean.parseBoolean(args[3]), Boolean.parseBoolean(args[4]));
//		}
//		else testFunc();
	}
	
	public static void printUsage() {
		System.out.println("\nUsage options:\n" +
				"Alignment with database: ./FragmenationTreeAligner.sh -db database -q query\n" +
				"Alignment of two trees:  ./FragmenationTreeAligner.sh -tree1 file1 -tree2 file2\n" +
				"Parameter optimization:  ./FragmenationTreeAligner.sh -paramOpt\n" +
				"\nAdditional command line arguments:\n" +
				"  -scoring filename               : use scoring parameters from file\n" +
				"  -global                         : make global instead of local alignment\n" +
				"  -union                          : allow unification of neutral losses\n" +
				"  -normal [tree_size_arithmetic|tree_size_geometric|self_alig_arithmetic|self_alig_geometric|all]\n" + 
				"                                  : normalize scores\n" +
				"  -weight [node_weight|nl_freq]   : weight scores\n" +
				"  -decoydb                        : use the trees in the given dir as base for decoy trees\n" +
				"  -noCnl                          : disable use of common neutral losses\n" +
				"  -noRDiff                        : disable consideration of reliable differences\n" + 
				"  -noRDiffH2                      : disable consideration of reliable differences plus H2\n" +
				"  -verbose                        : verbose output\n" +
				"  -matrix                         : enable csv output\n" +
				"  -gui                            : show results graphically\n" +
				"  -h                              : print this usage information\n"
		);
		
	}
	
	public static void allAgainstAll(boolean unionAllowed, boolean localAlign) throws IOException, InterruptedException {
		Session session = new Session();
		Parameters params = new Parameters(session);
		session.setParameters(params);
		session.getParameters().makeLocalAlignment = localAlign;
		session.getParameters().isNodeUnionAllowed = unionAllowed;
		session.getParameters().normalizationType = NormalizationType.NONE;



		ScoringFunctionNeutralLosses sFuncNL = new ScoringFunctionNeutralLosses(session);
		Alignment alig;

		File fragTreeDir = new File("fragmentationTrees/");
		String[] fileList = fragTreeDir.list();
		Arrays.sort(fileList);
		int i = 0;
		for (String fragTree1Str : fileList) {
//			String fragTree1Str = fileList[0]; 
			if (fragTree1Str.endsWith(".dot")) {
				i += 1;
				System.out.println(i + " x -10000");
//				System.out.println("fragmentationTrees/" + fragTree1Str);
				FragmentationTree fTree1 = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/" + fragTree1Str)), session);
				if (fTree1 == null){ continue; } // This is ugly, but works.
				AlignmentTree aTree1 = fTree1.toAlignmentTree();
				for (String fragTree2Str : fileList) {
					if ((fragTree2Str.endsWith(".dot")) && (fragTree1Str.compareToIgnoreCase(fragTree2Str) != 0)) {
//					if (fragTree2Str.endsWith(".dot")) {
						FragmentationTree fTree2 = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/" + fragTree2Str)), session);
						if (fTree2 == null){ continue; } // This is ugly, but works.
//						for (int iter = 0; iter < 10; iter++) {
						AlignmentTree aTree2 = fTree2.toAlignmentTree();
						TreeAligner treeAligner = new TreeAligner(aTree1, aTree2, sFuncNL, session);
//						treeAligner.setThreshold(20);

						alig = treeAligner.performAlignment();
//						}
//						System.out.println(alig.getScore() + "\t" + fragTree1Str + " <-> " + fragTree2Str);						
//						System.out.println(fragTree1Str + "\t" + alig.getScore() + "\t" + " <-> " + fragTree2Str);

						if (!Float.isNaN(alig.getScore())) System.out.printf("%d     %s %4d \t%s\n", i, fragTree1Str, (int)alig.getScore(), fragTree2Str);
						else System.out.printf("%d     %s >>>T \t%s\n", i, fragTree1Str, fragTree2Str);
					}
				}
			}
		}


	}
	
	public static void testFunc()  throws IOException {
		Session session = new Session();
		Parameters params = new Parameters(session);
		session.setParameters(params);
		session.getParameters().makeLocalAlignment = true;
		session.getParameters().isNodeUnionAllowed = false;
		session.getParameters().normalizationType = NormalizationType.NONE;
		
//		AlignmentTree aTree1 = new AlignmentTree();
//		AlignmentTree aTree2 = new AlignmentTree();
		
//		CostFunctionSimple sFunc = new CostFunctionSimple();
		ScoringFunctionNeutralLosses sFuncNL = new ScoringFunctionNeutralLosses(session);
		
		FragmentationTree[] fragTrees = {null, null, null, null, null, null, null, null, null, null, null, null, null, null};
				
		try {
			fragTrees[0] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_phenylalanine.ms1.dot")), session);
			fragTrees[1] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/c_4-hexosylferuloyl_choline.ms2.dot")), session);
			fragTrees[2] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/c_cafeoyl_choline.ms1.dot")), session);
			fragTrees[3] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/c_Syringoyl_choline.ms3.dot")), session);
			fragTrees[4] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/c_4-hydroxybenzoyl_choline.ms1.dot")), session);
			fragTrees[5] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/c_4-hexosyloxybenzoyl_choline.ms1.dot")), session);
			fragTrees[6] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_tryptophane.ms1.dot")), session);
			fragTrees[7] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_tyrosine.ms1.dot")), session);
			fragTrees[8] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/u_6-aminocapronic_acid.ms1.dot")), session);
			fragTrees[9] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_citrulline.ms1.dot")), session);
			fragTrees[10] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_arginine.ms1.dot")), session);
			fragTrees[11] = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader("fragmentationTrees/a_asparagine.ms1.dot")), session);
	
//			aTree1.readFromList(FileUtils.ensureBuffering(new FileReader("TestTree1.list")));
//			aTree2.readFromList(FileUtils.ensureBuffering(new FileReader("TestTree5.list")));
		} catch (FileNotFoundException e) {
			System.out.println("Datei nicht gefunden " + e);
		}
		AlignmentTree aTree3 = fragTrees[9].toAlignmentTree();
		AlignmentTree aTree4 = fragTrees[10].toAlignmentTree();
		fragTrees[6].writeToCml("output/test.cml");
		fragTrees[13] = FragmentationTree.readFromCml(FileUtils.ensureBuffering(new FileReader("output/test.cml")), session);
		fragTrees[13].writeToCml("output/test2.cml");
		
//		TreeAligner treeAligner1 = new TreeAligner(aTree1, aTree2, sFunc, true, false, session);
//		Alignment alig = treeAligner1.performAlignment();

		TreeAligner treeAligner2 = new TreeAligner(aTree3, aTree4, sFuncNL, session);
		Alignment alig = treeAligner2.performAlignment();
		alig.visualize();
		System.out.println(alig.getScore());
	}
	
	public static void align(String filename1, String filename2, Session session) throws IOException {
		if (session.getParameters().makeGraphicalOutput) session.setMainFrame(new MainFrame(session));
		Alignment alig = Macros.performFragTreeAlignment(filename1, filename2, session);
		if (alig == null) { return; }
		if (session.getParameters().makeGraphicalOutput) session.getMainFrame().displayAlignment(alig);
		else alig.visualize();
		System.out.println(alig.getScore());
	}
	
	public static void compareWithDatabase(String directory, String fileOrDirectory, String altDbDir, String decoyDbDir, Session session) throws IOException {
		if (altDbDir != null && altDbDir.length() > 0){
			FragmentationTreeDatabase altFragTreeDb = new FragmentationTreeDatabase(new File(altDbDir), session, true); 
			session.setAltFragTreeDB(altFragTreeDb);
		}
		FragmentationTreeDatabase db = new FragmentationTreeDatabase(new File(directory), session);
		session.setFragTreeDB(db);
		Iterator<FragmentationTreeDatabaseEntry> iter = db.getEntries().iterator();
		// TODO this is very much based on the correct contents of the decoydb dir!
		// TODO read from cml does not work here!!
		if (decoyDbDir != null) {
			for (File decoyFile : new File(decoyDbDir).listFiles()) {
				if (iter.hasNext()) {
					FragmentationTree decoyTree = null;
					if (decoyFile.toString().endsWith("dot")) {
						decoyTree = FragmentationTree.readFromDot(
								FileUtils.ensureBuffering(new FileReader(decoyFile)),
								session);
						if (decoyTree == null){ continue; } //This is ugly, but works
						iter.next().setDecoyAlignmentTree(
								decoyTree.toAlignmentTree());
					} else if (decoyFile.toString().endsWith("cml")) {
						decoyTree = FragmentationTree.readFromCml(
								FileUtils.ensureBuffering(new FileReader(decoyFile)),
								session);
						iter.next().setDecoyAlignmentTree(
								decoyTree.toAlignmentTree());
					}
				}
			}
		}		
		if (session.getParameters().calcStatistics > 0){
			int maxSize = 0;
			for (FragmentationTreeDatabaseEntry e : db.getEntries()){
				int size = e.getFragmentationTree().size();
				if (size> maxSize){
					maxSize = size;
				}
			}
			db.calculateStatistics(maxSize+2);
		}
		if ((new File(fileOrDirectory)).isDirectory()) {
			db.compareFragmentationTreesWithDatabase(fileOrDirectory);
		} else {
			db.compareFragmentationTreeWithDatabase(fileOrDirectory);			
		}
	}
	
	public static void parameterOptimization(String directory, String fileOrDirectory, Session session) throws IOException, InterruptedException {
		Parameters params = session.getParameters();
		params.executionMode = ExecutionMode.PARAMETER_OPTIMIZATION;

		float score, maxScore = Float.NEGATIVE_INFINITY;
		FragmentationTreeDatabase db = new FragmentationTreeDatabase(new File(directory), session);
		session.setFragTreeDB(db);
		
		float scoreEquality = Float.NaN;
		float scoreEqualityPerAtom = Float.NaN;
		float scoreInequality = Float.NaN;
		float scoreInequalityPerAtom = Float.NaN;
		float scoreGap = Float.NaN;
		float scoreGapCnl = Float.NaN;
		float scoreCnlCnl = Float.NaN;
		float scoreDiffCommonFirstOrder = Float.NaN;
		float scoreDiffCommonSecondOrder = Float.NaN;
		float scoreDiffCommonPerAtom = Float.NaN;
		float scoreDiffH2 = Float.NaN;
		float scoreDiffH2PerAtom = Float.NaN;
		float scoreDiffCommonPlusH2 = Float.NaN;
		float scoreDiffCommonPlusH2PerAtom = Float.NaN;
		float scoreUnion = Float.NaN;

		float mScoreEquality = Float.NaN;
		float mScoreEqualityPerAtom = Float.NaN;
		float mScoreInequality = Float.NaN;
		float mScoreInequalityPerAtom = Float.NaN;
		float mScoreGap = Float.NaN;
		float mScoreGapCnl = Float.NaN;
		float mScoreCnlCnl = Float.NaN;
		float mScoreDiffCommonFirstOrder = Float.NaN;
		float mScoreDiffCommonSecondOrder = Float.NaN;
		float mScoreDiffCommonPerAtom = Float.NaN;
		float mScoreDiffH2 = Float.NaN;
		float mScoreDiffH2PerAtom = Float.NaN;
		float mScoreDiffCommonPlusH2 = Float.NaN;
		float mScoreDiffCommonPlusH2PerAtom = Float.NaN;
		float mScoreUnion = Float.NaN;

//		for (scoreEquality = 4; scoreEquality < 11; scoreEquality += 2) {
//			params.scoreEquality = scoreEquality;
//			for (scoreEqualityPerAtom = 0; scoreEqualityPerAtom < 4; scoreEqualityPerAtom += 1) {
//				params.scoreEqualityPerAtom = scoreEqualityPerAtom;
//				for (scoreInequality = -10; scoreInequality < -3; scoreInequality += 2) {
//					params.scoreInequality = scoreInequality;
//					for (scoreGap = -10; scoreGap < 0; scoreGap += 3) {
//					fragmentation	params.scoreGap = scoreGap;
//						for (scoreGapCnl = -5; scoreGapCnl < 2; scoreGapCnl += 3) {
//							params.scoreGapCnl = scoreGapCnl;
//							for (scoreCnlCnl = -5; scoreCnlCnl < 5; scoreCnlCnl += 3) {
//								params.scoreCnlCnl = scoreCnlCnl;
//								for (scoreDiffCommon = -4; scoreDiffCommon < 4; scoreDiffCommon += 3) {
//									params.scoreDiffCommon = scoreDiffCommon;
//									for (scoreDiffH2 = -4; scoreDiffH2 < 4; scoreDiffH2 += 3) {
//										params.scoreDiffH2 = scoreDiffH2;
//										for (scoreDiffCommonPlusH2 = -4; scoreDiffCommonPlusH2 < 2; scoreDiffCommonPlusH2 += 3) {
//											params.scoreDiffCommonPlusH2 = scoreDiffCommonPlusH2;
//											
//											score = db.compareFragmentationTreesWithDatabase(fileOrDirectory, unionAllowed, localAlign);
//											if (score > maxScore) {
//												maxScore = score;
//												mScoreEquality = scoreEquality;
//												mScoreEqualityPerAtom = scoreEqualityPerAtom;
//												mScoreInequality = scoreInequality;
//												mScoreGap = scoreGap;
//												mScoreGapCnl = scoreGapCnl;
//												mScoreCnlCnl = scoreCnlCnl;
//												mScoreDiffCommon = scoreDiffCommon;
//												mScoreDiffH2 = scoreDiffH2;
//												mScoreDiffCommonPlusH2 = scoreDiffCommonPlusH2;
//
//												System.out.println("\n-----------------------------");
//												System.out.println("Maximal score so far: " + maxScore);
//												System.out.println("scoreEquality = " + mScoreEquality);
//												System.out.println("scoreEqualityPerAtom = " + mScoreEqualityPerAtom);
//												System.out.println("scoreInequality = " + mScoreInequality);
//												System.out.println("scoreGap = " + mScoreGap);
//												System.out.println("scoreGapCnl = " + mScoreGapCnl);
//												System.out.println("scoreCnlCnl = " + mScoreCnlCnl);
//												System.out.println("scoreDiffCommon = " + mScoreDiffCommon);
//												System.out.println("scoreDiffH2 = " + mScoreDiffH2);
//												System.out.println("scoreDiffCommonPlusH2 = " + mScoreDiffCommonPlusH2);
//
//											}
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			}
//		}
		
		for (int i = 0; i < 1000000; i++) {
			scoreEquality = (float)(Math.random() * 12);
			params.scoreEquality = scoreEquality;

			scoreEqualityPerAtom = (float)(Math.random() * 5);
			params.scoreEqualityPerAtom = scoreEqualityPerAtom;

			scoreInequality = (float)(Math.random() * 12 - 12);
			params.scoreInequality = scoreInequality;

			scoreInequalityPerAtom = (float)(Math.random() * -5);
			params.scoreInequalityPerAtom = scoreInequalityPerAtom;

			scoreGap = (float)(Math.random() * 15 - 13);
			params.scoreGap = scoreGap;

			scoreGapCnl = (float)(Math.random() * 10 - 6);
			params.scoreGapCnl = scoreGapCnl;

			scoreCnlCnl = (float)(Math.random() * 12 - 6);
			params.scoreCnlCnl = scoreCnlCnl;

			scoreDiffCommonFirstOrder = (float)(Math.random() * 11 - 5);
			params.scoreDiffCommonFirstOrder = scoreDiffCommonFirstOrder;
			
			scoreDiffCommonSecondOrder = (float)(Math.random() * 11 - 5);
			params.scoreDiffCommonSecondOrder = scoreDiffCommonSecondOrder;

			scoreDiffCommonPerAtom = (float)(Math.random() * 6 - 3);
			params.scoreDiffCommonPerAtom = scoreDiffCommonPerAtom;

			scoreDiffH2 = (float)(Math.random() * 11 - 5);
			params.scoreDiffH2 = scoreDiffH2;

			scoreDiffH2PerAtom = (float)(Math.random() * 6 - 3);
			params.scoreDiffH2PerAtom = scoreDiffH2PerAtom;

			scoreDiffCommonPlusH2 = (float)(Math.random() * 11 - 6);
			params.scoreDiffCommonPlusH2 = scoreDiffCommonPlusH2;

			scoreDiffCommonPlusH2PerAtom = (float)(Math.random() * 6 - 3);
			params.scoreDiffCommonPlusH2PerAtom = scoreDiffCommonPlusH2PerAtom;

			scoreUnion = (float)(Math.random() * 15 - 15);
			params.scoreUnion = scoreUnion;

			score = db.compareFragmentationTreesWithDatabase(fileOrDirectory);
			if (score > maxScore) {
				maxScore = score;
				mScoreEquality = scoreEquality;
				mScoreEqualityPerAtom = scoreEqualityPerAtom;
				mScoreInequality = scoreInequality;
				mScoreInequalityPerAtom = scoreInequalityPerAtom;
				mScoreGap = scoreGap;
				mScoreGapCnl = scoreGapCnl;
				mScoreCnlCnl = scoreCnlCnl;
				mScoreDiffCommonFirstOrder = scoreDiffCommonFirstOrder;
				mScoreDiffCommonSecondOrder = scoreDiffCommonSecondOrder;
				mScoreDiffCommonPerAtom = scoreDiffCommonPerAtom;
				mScoreDiffH2 = scoreDiffH2;
				mScoreDiffH2PerAtom = scoreDiffH2PerAtom;
				mScoreDiffCommonPlusH2 = scoreDiffCommonPlusH2;
				mScoreDiffCommonPlusH2PerAtom = scoreDiffCommonPlusH2PerAtom;
				mScoreUnion = scoreUnion;

				System.out.println("\n-----------------------------");
				System.out.println("Iteration number: " + i);
				System.out.println("Maximal score so far: " + maxScore);
				System.out.println("scoreEquality = " + mScoreEquality);
				System.out.println("scoreEqualityPerAtom = " + mScoreEqualityPerAtom);
				System.out.println("scoreInequality = " + mScoreInequality);
				System.out.println("scoreInequalityPerAtom = " + mScoreInequalityPerAtom);
				System.out.println("scoreGap = " + mScoreGap);
				System.out.println("scoreGapCnl = " + mScoreGapCnl);
				System.out.println("scoreCnlCnl = " + mScoreCnlCnl);
				System.out.println("scoreDiffCommon = " + mScoreDiffCommonFirstOrder+"/"+mScoreDiffCommonSecondOrder);
				System.out.println("scoreDiffCommonPerAtom = " + mScoreDiffCommonPerAtom);
				System.out.println("scoreDiffH2 = " + mScoreDiffH2);
				System.out.println("scoreDiffH2PerAtom = " + mScoreDiffH2PerAtom);
				System.out.println("scoreDiffCommonPlusH2 = " + mScoreDiffCommonPlusH2);
				System.out.println("scoreDiffCommonPlusH2PerAtom = " + mScoreDiffCommonPlusH2PerAtom);
				System.out.println("scoreUnion = " + mScoreUnion);			
			}
		}	
		
		System.out.println("\n=============================");
		System.out.println("Maximal score: " + maxScore);
		System.out.println("scoreEquality = " + mScoreEquality);
		System.out.println("scoreEqualityPerAtom = " + mScoreEqualityPerAtom);
		System.out.println("scoreInequality = " + mScoreInequality);
		System.out.println("scoreInequalityPerAtom = " + mScoreInequalityPerAtom);
		System.out.println("scoreGap = " + mScoreGap);
		System.out.println("scoreGapCnl = " + mScoreGapCnl);
		System.out.println("scoreCnlCnl = " + mScoreCnlCnl);
		System.out.println("scoreDiffCommon = " + mScoreDiffCommonFirstOrder+"/"+mScoreDiffCommonSecondOrder);
		System.out.println("scoreDiffCommonPerAtom = " + mScoreDiffCommonPerAtom);
		System.out.println("scoreDiffH2 = " + mScoreDiffH2);
		System.out.println("scoreDiffH2PerAtom = " + mScoreDiffH2PerAtom);
		System.out.println("scoreDiffCommonPlusH2 = " + mScoreDiffCommonPlusH2);
		System.out.println("scoreDiffCommonPlusH2PerAtom = " + mScoreDiffCommonPlusH2PerAtom);
		System.out.println("scoreUnion = " + mScoreUnion);			
	}
	
	public static void runningTimeDetermination(int numNodes, int outDegree, Session session) throws IOException {
		boolean testDependensOnDegree = false;
		boolean testDependensOnSize = true;
		
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		long startCpuTimeNano, endCpuTimeNano;
		
		int[][] testSet = new int[10][];
		testSet[0] = new int[] {5, 4};
		testSet[1] = new int[] {10, 9};
		testSet[2] = new int[] {15, 10};
		testSet[3] = new int[] {20, 10};
		testSet[4] = new int[] {25, 10};
		testSet[5] = new int[] {50, 9};
		testSet[6] = new int[] {75, 8};
		testSet[7] = new int[] {100, 8};
		testSet[8] = new int[] {150, 7};
		testSet[9] = new int[] {200, 7};
		
		ScoringFunctionNeutralLosses sFuncNL = new ScoringFunctionNeutralLosses(session);
		FragmentationTree fragTree = FragmentationTree.generateRandomTree(numNodes, outDegree, session);
		AlignmentTree aTree1 = fragTree.toAlignmentTree();
		AlignmentTree aTree2 = fragTree.toAlignmentTree();
		
		for (int i = 1; i <= 5; i++) {
			TreeAligner treeAligner = new TreeAligner(aTree1, aTree2, sFuncNL, session);
			treeAligner.performAlignment();
		}
		
		if (testDependensOnDegree) {
			for (int i = 0; i < 10; i++) {
				numNodes = testSet[i][0];
				for (outDegree = 1; outDegree <= testSet[i][1]; outDegree++) {
					fragTree = FragmentationTree.generateRandomTree(numNodes, outDegree, session);
					
					aTree1 = fragTree.toAlignmentTree();
					aTree2 = fragTree.toAlignmentTree();
					
					int numIterations = 5;
					startCpuTimeNano = bean.getCurrentThreadCpuTime();
					for (int j = 1; j <= numIterations; j++) {
						TreeAligner treeAligner = new TreeAligner(aTree1, aTree2, sFuncNL, session);
						treeAligner.performAlignment();
					}
					endCpuTimeNano = bean.getCurrentThreadCpuTime();
					System.out.print(Integer.toString(numNodes) + " " + Integer.toString(outDegree) + "\t");
					System.out.println(Long.toString((endCpuTimeNano - startCpuTimeNano) / 1000000 / numIterations));
				}
			}
		}

		if (testDependensOnSize) {
			for (outDegree = 7; outDegree < 8; outDegree++) {
				for (numNodes = 200; numNodes <= 250; numNodes += 10) {
					fragTree = FragmentationTree.generateRandomTree(numNodes, outDegree, session);
					
					aTree1 = fragTree.toAlignmentTree();
					aTree2 = fragTree.toAlignmentTree();
					
					int numIterations = 5;
					startCpuTimeNano = bean.getCurrentThreadCpuTime();
					for (int j = 1; j <= numIterations; j++) {
						TreeAligner treeAligner = new TreeAligner(aTree1, aTree2, sFuncNL, session);
						treeAligner.performAlignment();
					}
					endCpuTimeNano = bean.getCurrentThreadCpuTime();
					System.out.print(Integer.toString(outDegree) + " " + Integer.toString(numNodes) + "\t");
					System.out.println(Long.toString((endCpuTimeNano - startCpuTimeNano) / 1000000 / numIterations));
				}
			}
		}
	
	}

}
