package de.unijena.ftreeheuristics.main;

import com.thoughtworks.xstream.XStream;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.ftreeheuristics.treebuilder.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Main {

	public static void main(String[] args) {
		try {
			// directory for the input files
			File inputDir = new File(
					"/home/marie/Dokumente/Studium/BA/data/gnpsneu");
			// number of random input files
			int randInputFiles = -2;
			ArrayList<File> inputFiles;
			inputFiles = getRandomInputFiles(randInputFiles, inputDir);

			// directory to the logs
			File logDir = new File("/home/marie/Dokumente/Studium/BA/log/xml5");
			// if you do not need to pint the logs in a file
			boolean printLogInFile = true;

			// list of TreeBuilder you want so use
			// 0->Gurobi, 1->Greedy, 2->GreedyRDE, 3->GreedyRDS, 4->PrimStyle,
			// 5->PrimStyleRDE, 6->PrimStyleRDS, 7->PrimStyleStar,
			// 8->CriticalPath,
			// 9->Insertion, 10->TopDown
			ArrayList<TreeBuilder> treeBuilderList = new ArrayList<TreeBuilder>();
			for (int i = 0; i < 11; i++) {
				treeBuilderList.add(getNewTreeBuilder(i));
			}

			// number of runs for the runtime test
			int numbOfRuns = 3;

			new Main().doMain(inputFiles, logDir, printLogInFile,
					treeBuilderList, numbOfRuns);

		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		} catch (Exception e) {
			System.err.println("Something went wrong.");
			e.printStackTrace();
		}
	}

	/**
	 * Starts {@link Sirius} with a list of input files, certain
	 * {@link TreeBuilder} and certain parameters.
	 * 
	 * @param inputFiles
	 *            the input files
	 * @param logDir
	 *            where to save the log files
	 * @param printLogInFile
	 *            whether to print the logs
	 * @param treeBuilderList
	 *            the {@link TreeBuilder} to use
	 * @param numberOfRuns
	 *            number of runs for the runtime test
	 * @throws Exception
	 */
	public void doMain(ArrayList<File> inputFiles, File logDir,
			boolean printLogInFile, ArrayList<TreeBuilder> treeBuilderList,
			int numberOfRuns) throws Exception {

		// check parameter
		if (!logDir.isDirectory()) {
			throw new NullPointerException(logDir.getAbsolutePath()
					+ " is not a directory.");
		} else if (numberOfRuns <= 0) {
			throw new IllegalArgumentException(numberOfRuns
					+ " is negative, but you can not travel in time...");
		}

		// load SIRIUS framework/configurations
		final Sirius sirius = new Sirius();
		// use the MS/MS analyzer
		final FragmentationPatternAnalysis analyzer = sirius.getMs2Analyzer();

		for (File inputFile : inputFiles) {
			// System.out.println("start: " + inputFile.getName());

			for (TreeBuilder treeBuilder : treeBuilderList) {

				// sets a certain TreeBuilder
				analyzer.setTreeBuilder(treeBuilder);

				// log file
				String input = inputFile.getName();
				if (input.contains(".")) {
					input = input.split("\\.")[0];
				}
				File logFile = new File(logDir.getAbsolutePath() + "/" + input
						+ ".xml");

				// run a few times for a runtime test
				for (int i = 0; i < numberOfRuns; i++) {

					try {
						// parse input file
						final Ms2Experiment experiment = new MsExperimentParser()
								.getParser(inputFile).parseFromFile(inputFile)
								.get(0);

						// get the date
						Date date = new Date();

						// compute all trees for the input instance
						final List<FTree> listTree = analyzer
								.computeTrees(
										analyzer.preprocessing(experiment))
								.withoutRecalibration().list();

						// get the runtime
						long runtime;
						if (!(treeBuilder instanceof GurobiTreeBuilder)) {
							HeuristicTreeBuilder htb = (HeuristicTreeBuilder) treeBuilder;
							runtime = htb.getRuntimeSumNs();
						} else {
							GurobiTreeBuilder gutb = (GurobiTreeBuilder) treeBuilder;
							runtime = gutb.getRuntimeSumNs();
						}

						// get output
						ArrayList<Double> scoreList = new ArrayList<Double>();
						ArrayList<String> formularList = new ArrayList<String>();
						ArrayList<Boolean> isRightFormularList = new ArrayList<Boolean>();

						String correctFormular = experiment
								.getMolecularFormula().toString();
						for (FTree tree : listTree) {
							String formular = tree.getRoot().getFormula()
									.toString();

							formularList.add(formular);
							scoreList.add(tree.getAnnotationOrThrow(
									TreeScoring.class).getOverallScore());

							if (correctFormular.equals(formular)) {
								isRightFormularList.add(true);
							} else {
								isRightFormularList.add(false);
							}

						}

						// save with xstream in xml-format
						XStream xstream = new XStream();

						// nice date format for the logs
						DateFormat dateFormat = new SimpleDateFormat(
								"yyyy/MM/dd HH:mm:ss");

						Log log = new Log(analyzer.getTreeBuilder()
								.getDescription(), dateFormat.format(date),
								runtime);

						for (int j = 0; j < listTree.size(); j++) {
							LogTree lt = new LogTree(scoreList.get(j),
									formularList.get(j),
									isRightFormularList.get(j));
							xstream.processAnnotations(LogTree.class);
							log.addLogTree(lt);
						}

						xstream.processAnnotations(Log.class);

						// print in file
						if (printLogInFile) {
							if (logFile.exists()) {
								xstream.processAnnotations(LogFile.class);

								LogFile lf = (LogFile) xstream.fromXML(logFile);
								lf.addLog(log);

								xstream.toXML(lf, new FileOutputStream(logFile));
							} else {
								LogFile lf = new LogFile(input, correctFormular);
								lf.addLog(log);

								xstream.processAnnotations(LogFile.class);
								xstream.toXML(lf, new FileOutputStream(logFile));
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				// nice to see what has been done so far
				// System.out.println("end: " + inputFile.getName() + " / "
				// + treeBuilder.getDescription());
			}

		}
	}

	/**
	 * Returns a new {@link TreeBuilder} with a certain id
	 * 
	 * @param id
	 *            of the new {@link TreeBuilder}
	 * @return a new {@link TreeBuilder}
	 */
	private static TreeBuilder getNewTreeBuilder(int id) {
		if (id == 0) {
			return new GurobiTreeBuilder();
		} else if (id == 1) {
			return new GreedyTreeBuilder();
		} else if (id == 2) {
			return new GreedyRDETreeBuilder();
		} else if (id == 3) {
			return new GreedyRDSTreeBuilder();
		} else if (id == 4) {
			return new PrimStyleTreeBuilder();
		} else if (id == 5) {
			return new PrimStyleRDETreeBuilder();
		} else if (id == 6) {
			return new PrimStyleRDSTreeBuilder();
		} else if (id == 7) {
			return new PrimStyleStarTreeBuilder();
		} else if (id == 8) {
			return new CriticalPathTreeBuilder();
		} else if (id == 9) {
			return new InsertionTreeBuilder();
		} else if (id == 10) {
			return new TopDownTreeBuilder();
		} else {
			throw new IllegalArgumentException(
					"The TreeBuilder with id "
							+ id
							+ " has not been implemented yet. Choose one between 0 and 10!");
		}
	}

	/**
	 * Returns a list of randomly selected input files.
	 * 
	 * @param count
	 *            number of files to select
	 * @param inputDir
	 * @return
	 */
	private static ArrayList<File> getRandomInputFiles(int count, File inputDir)
			throws Exception {
		if (count < 0) {
			throw new IllegalArgumentException(
					"You can not select a negative number of files.");
		} else if (!inputDir.isDirectory()) {
			throw new NullPointerException(inputDir.getAbsolutePath()
					+ " is not a directory.");
		}

		ArrayList<File> selectedInputFiles = new ArrayList<File>(count);
		int numOfFiles = inputDir.listFiles().length;

		int rand = new Random().nextInt(numOfFiles);
		while (selectedInputFiles.size() < count) {
			File currentFile = inputDir.listFiles()[rand];
			selectedInputFiles.add(currentFile);
			rand = new Random().nextInt(numOfFiles);
		}

		return selectedInputFiles;
	}
}
