//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


package biotransformer.btransformers;

import ambit2.smarts.IAcceptable;
import ambit2.smarts.SMIRKSManager;
import ambit2.smarts.SMIRKSReaction;
import biotransformer.biomolecule.Enzyme;
import biotransformer.biosystems.BioSystem;
import biotransformer.biosystems.BioSystem.BioSystemName;
import biotransformer.dbrelevant.RetriveFromDB;
import biotransformer.esaprediction.ESSpecificityPredictor;
import biotransformer.transformation.Biotransformation;
import biotransformer.transformation.MReactionSets;
import biotransformer.transformation.MReactionsFilter;
import biotransformer.transformation.MetabolicReaction;
import biotransformer.utils.ChemStructureExplorer;
import biotransformer.utils.ChemStructureManipulator;
import biotransformer.utils.ChemdbRest;
import biotransformer.utils.ChemicalClassFinder;
import biotransformer.utils.FileUtilities;
import biotransformer.utils.HandlePolymers;
import biotransformer.utils.Utilities;
import biotransformer.validateModels.InValidSMARTS;
import exception.BioTransformerException;
import io.siriusms.shadow.bt.cdk.AtomContainerSet;
import io.siriusms.shadow.bt.cdk.DefaultChemObjectBuilder;
import io.siriusms.shadow.bt.cdk.aromaticity.Aromaticity;
import io.siriusms.shadow.bt.cdk.aromaticity.ElectronDonation;
import io.siriusms.shadow.bt.cdk.exception.CDKException;
import io.siriusms.shadow.bt.cdk.graph.Cycles;
import io.siriusms.shadow.bt.cdk.inchi.InChIGenerator;
import io.siriusms.shadow.bt.cdk.inchi.InChIGeneratorFactory;
import io.siriusms.shadow.bt.cdk.io.SDFWriter;
import io.siriusms.shadow.bt.cdk.silent.SilentChemObjectBuilder;
import io.siriusms.shadow.bt.cdk.smiles.SmilesGenerator;
import io.siriusms.shadow.bt.cdk.smiles.SmilesParser;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
/**
 * @author Djoumbou Feunang, Yannick
 * @author Markus Fleischauer (modifications)
 *
 */

//todo this class has been copied and decompiled to fix a nullpointer issue. Should be moved to biotransformer-sirius-mod-relocated project
public class Biotransformer {
	public enum bType {
		ALLHUMAN, CYP450, ECBASED, ENV, HGUT, PHASEII, SUPERBIO
	}

	public boolean useDB;
	public boolean useSubstitution;
	public RetriveFromDB rfdb = null;
	InValidSMARTS invalidSMARTS = new InValidSMARTS();
	protected SMIRKSManager smrkMan;
	public BioSystem bSystem;
	protected MReactionsFilter mRFilter;
	protected LinkedHashMap<String, Double> reactionORatios;
	protected IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
	public SmilesGenerator smiGen = new SmilesGenerator(1800);
	public boolean inAllHuman = false;
	protected SmilesParser smiParser;
	public InChIGeneratorFactory inchiGenFactory;
	public LinkedHashMap<String, ArrayList<MetabolicReaction>> reactionsByGroups;
	public LinkedHashMap<String, ArrayList<Enzyme>> enzymesByreactionGroups;
	public ArrayList<Enzyme> enzymesList;
	public LinkedHashMap<String, MetabolicReaction> reactionsHash;
	public ObjectMapper mapper;
	protected ESSpecificityPredictor esspredictor;
	public HandlePolymers hp;

	public Biotransformer(BioSystem.BioSystemName bioSName, boolean useDB, boolean useSubstitution) throws JsonParseException, JsonMappingException, FileNotFoundException, IOException, BioTransformerException, CDKException {
		this.smiParser = new SmilesParser(this.builder);
		this.reactionsByGroups = new LinkedHashMap<>();
		this.enzymesByreactionGroups = new LinkedHashMap<>();
		this.enzymesList = new ArrayList<>();
		this.reactionsHash = new LinkedHashMap<>();
		this.mapper = new ObjectMapper();
		this.hp = new HandlePolymers();
		this.useDB = useDB;
		this.useSubstitution = useSubstitution;
		this.mapper.configure(Feature.ALLOW_COMMENTS, true);
		this.mapper.configure(Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		this.bSystem = new BioSystem(bioSName, this.mapper);
		this.smrkMan = this.bSystem.getSmirksManager();
		this.esspredictor = new ESSpecificityPredictor(this.bSystem);
		this.inchiGenFactory = InChIGeneratorFactory.getInstance();
		this.smrkMan.setFlagApplyStereoTransformation(false);
		this.smrkMan.setFlagCheckResultStereo(true);
		this.smrkMan.setFlagFilterEquivalentMappings(true);
		this.smrkMan.setFlagProcessResultStructures(true);
		this.smrkMan.setFlagAddImplicitHAtomsOnResultProcess(true);
		this.reactionORatios = this.bSystem.getReactionsORatios();
		this.setReactionsGroups();
		this.setReactionsFilter();
	}

	public ArrayList<Biotransformation> getBiotransoformationsByExploringHDMB(IAtomContainer substrate) throws Exception {
		ArrayList<String> substrateProcessed = new ArrayList<>();
        ArrayList<Biotransformation> currentBiotransformations = this.rfdb.getBiotransformationRetrievedFromDB(substrate, true);
        ArrayList<Biotransformation> results = new ArrayList<>(currentBiotransformations);

		for(IAtomContainerSet substratePool = this.extractProductsFromBiotransformations(currentBiotransformations); substratePool != null && !substratePool.isEmpty(); substrateProcessed = Utilities.updateProcessedSubstratePool(substrateProcessed, substratePool)) {
			currentBiotransformations = new ArrayList<>();

			for(int i = 0; i < substratePool.getAtomContainerCount(); ++i) {
				IAtomContainer oneMole = substratePool.getAtomContainer(i);
				if (this.countNonHydrogenAtoms(oneMole) > 4) {
					ArrayList<Biotransformation> db_results = this.rfdb.getBiotransformationRetrievedFromDB(oneMole, true);

					for(int t = 0; t < db_results.size(); ++t) {
						if (this.isConjugate((Biotransformation)db_results.get(t))) {
							db_results.remove(t);
							--t;
						}
					}

					currentBiotransformations.addAll(db_results);
				}
			}

			results.addAll(currentBiotransformations);
			substratePool = this.extractProductsFromBiotransformations(currentBiotransformations);
			substratePool = Utilities.getNovelSubstrates(substratePool, substrateProcessed);
		}

		return Utilities.selectUniqueBiotransformations(results);
	}

	public boolean isConjugate(Biotransformation bt) throws Exception {
		IAtomContainer substrate = bt.getSubstrates().getAtomContainer(0);
		IAtomContainerSet metabolites = bt.getProducts();
		IAtomContainer metabolite = metabolites.getAtomContainer(0);
		metabolite.getProperty("Reaction ID").equals(2365);
		int currentCounter = this.getNonHydrogenAtoms(metabolite);

		for(int i = 0; i < metabolites.getAtomContainerCount(); ++i) {
			if (this.getNonHydrogenAtoms(metabolites.getAtomContainer(i)) > currentCounter) {
				currentCounter = this.getNonHydrogenAtoms(metabolites.getAtomContainer(i));
				metabolites.getAtomContainer(i);
			}
		}

		if (currentCounter - this.getNonHydrogenAtoms(substrate) > 3) {
			return true;
		} else {
			return false;
		}
	}

	public int getNonHydrogenAtoms(IAtomContainer metabolite) {
		int counter = 0;

		for(int t = 0; t < metabolite.getAtomCount(); ++t) {
			if (!metabolite.getAtom(t).getSymbol().equalsIgnoreCase("H")) {
				++counter;
			}
		}

		return counter;
	}

	private void setReactionsGroups() throws JsonParseException, JsonMappingException, FileNotFoundException, IOException {
		MReactionSets mrs = new MReactionSets();
		this.reactionsByGroups.put("standardizationReactions", mrs.standardizationReactions);
		MReactionSets msets = new MReactionSets();

		for(MetabolicReaction mr : msets.standardizationReactions) {
			this.reactionsHash.put(mr.name, mr);
		}

	}

	private void setReactionsFilter() {
		this.mRFilter = new MReactionsFilter(this.bSystem);
	}

	public BioSystem.BioSystemName getBioSystemName() {
		return this.bSystem.name;
	}

	public LinkedHashMap<String, ArrayList<MetabolicReaction>> getReactionsList() {
		return this.reactionsByGroups;
	}

	public IAtomContainerSet generateAllMetabolitesFromAtomContainer(IAtomContainer molecule, MetabolicReaction mReaction, boolean preprocess) throws Exception {
		IAtomContainerSet metabolites = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		metabolites = this.generateAllMetabolitesFromAtomContainer(molecule, mReaction.getSmirksReaction(), preprocess);
		return metabolites;
	}

	public IAtomContainerSet generateAllMetabolitesFromAtomContainer(IAtomContainer molecule, SMIRKSReaction reaction, boolean preprocess) throws Exception {
		IAtomContainer reactant = molecule.clone();
		if (preprocess) {
			try {
				reactant = ChemStructureManipulator.preprocessContainer(reactant);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(reactant);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(reactant);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(reactant);
		}

		Aromaticity aromaticity = new Aromaticity(ElectronDonation.cdk(), Cycles.all());
		aromaticity.apply(reactant);

		for(int k = 0; k < reactant.getAtomCount(); ++k) {
			IAtom oneAtom = reactant.getAtom(k);
			oneAtom.setProperty("AtomIdx", k);
		}

		IAtomContainerSet metabolites = this.smrkMan.applyTransformationWithSingleCopyForEachPos(reactant, (IAcceptable)null, reaction);
		IAtomContainerSet postprocessed_metabolites = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		if (metabolites != null) {
			int nr_of_metabolites = metabolites.getAtomContainerCount();
			if (nr_of_metabolites > 0) {
				for(int i = 0; i < nr_of_metabolites; ++i) {
					IAtomContainerSet partitions = ChemStructureExplorer.checkConnectivity(metabolites.getAtomContainer(i));

					for(IAtomContainer c : partitions.atomContainers()) {
						if (this.isValidMetabolte(c) && !ChemStructureExplorer.isUnneccessaryMetabolite(c)) {
							try {
								postprocessed_metabolites.addAtomContainer(ChemStructureManipulator.preprocessContainer(c));
							} catch (NullPointerException n) {
								System.err.println(n.getMessage());
								postprocessed_metabolites.addAtomContainer(c);
							}
						}
					}
				}
			}
		}

		IAtomContainerSet final_metabolites = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);

		for(int i = 0; i < postprocessed_metabolites.getAtomContainerCount(); ++i) {
			IAtomContainer oneMetabolite = postprocessed_metabolites.getAtomContainer(i);
			String smiles = this.smiGen.create(oneMetabolite);
			if (smiles.contains("*")) {
				IAtomContainer corrected = ChemStructureManipulator.getCorrectMetabolite(reactant, oneMetabolite);
				corrected.addProperties(oneMetabolite.getProperties());
				final_metabolites.addAtomContainer(corrected);
			} else {
				final_metabolites.addAtomContainer(oneMetabolite);
			}
		}

		return ChemStructureExplorer.uniquefy(final_metabolites);
	}

	public IAtomContainerSet generateAllMetabolitesFromAtomContainerViaTransformationAtAllLocations(IAtomContainer molecule, SMIRKSReaction reaction, boolean preprocess) throws Exception {
		IAtomContainer reactant = molecule.clone();
		if (preprocess) {
			try {
				reactant = ChemStructureManipulator.preprocessContainer(reactant);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(reactant);
		}

		IAtomContainerSet postprocessed_metabolites = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), Cycles.or(Cycles.all(), Cycles.all(6)));
		if (reactant != null) {
			IAtomContainerSet partitions = ChemStructureExplorer.checkConnectivity(reactant);

			for(int k = 0; k < partitions.getAtomContainerCount(); ++k) {
				aromaticity.apply(partitions.getAtomContainer(k));
				postprocessed_metabolites.addAtomContainer(partitions.getAtomContainer(k));
			}
		}

		return ChemStructureExplorer.uniquefy(postprocessed_metabolites);
	}

	public ArrayList<Biotransformation> applyReactionsAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter) throws Exception {
		return this.applyReactionsAndReturnBiotransformations(target, reactions, preprocess, filter, (double)0.0F);
	}

	public ArrayList<Biotransformation> applyReactionsAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();
		IAtomContainer starget = ChemStructureManipulator.standardizeMoleculeWithCopy(target, preprocess);
		if (target.getProperty("InChI") == null) {
			try {
				InChIGenerator gen0 = this.inchiGenFactory.getInChIGenerator(target);
				target.setProperty("InChI", gen0.getInchi());
				target.setProperty("InChIKey", gen0.getInchiKey());
			} catch (CDKException c) {
				System.err.println(c.getLocalizedMessage());
			}
		}

		Utilities.addPhysicoChemicalProperties(target);
		ArrayList<MetabolicReaction> matchedReactions = new ArrayList<>();
		new ArrayList<>();

		for(MetabolicReaction i : reactions) {
			boolean match_constraints = ChemStructureExplorer.compoundMatchesReactionConstraints(i, starget);
			if (match_constraints) {
				matchedReactions.add(i);
			}
		}

		ArrayList<MetabolicReaction> filteredReactions;
		if (!filter) {
			filteredReactions = matchedReactions;
		} else {
			filteredReactions = new ArrayList<>(this.mRFilter.filterReactions(matchedReactions).values());
		}

		for(MetabolicReaction j : filteredReactions) {
			
			IAtomContainerSet partialSet = this.generateAllMetabolitesFromAtomContainer(starget, j, true);
			Double score = (double)0.0F;
			AtomContainerSet subs = new AtomContainerSet();
			AtomContainerSet prod = new AtomContainerSet();
			if (partialSet.getAtomContainerCount() > 0) {
				if (target.getProperty("Score") != null) {
					score = (double)target.getProperty("Score") * this.bSystem.getReactionsORatios().get(j.name);
				} else {
					score = (Double)this.bSystem.getReactionsORatios().get(j.name);
				}

				if (score != null && score >= scoreThreshold) {
					subs.addAtomContainer(target);

					for(IAtomContainer pc : partialSet.atomContainers()) {
						if (!ChemStructureExplorer.isUnneccessaryMetabolite(pc)) {
							try {
								InChIGenerator gen = this.inchiGenFactory.getInChIGenerator(pc);
								pc.setProperty("InChI", gen.getInchi());
								pc.setProperty("InChIKey", gen.getInchiKey());
							} catch (CDKException c) {
								System.err.println(c.getLocalizedMessage());
							}

							Utilities.addPhysicoChemicalProperties(pc);
							prod.addAtomContainer(AtomContainerManipulator.removeHydrogens(pc));
						}
					}

					new ArrayList<>();
					Biotransformation bioT = new Biotransformation(subs, j.name, null, prod, score, this.getBioSystemName());
					results.add(bioT);
				}
			}
		}

		return results;
	}

	public ArrayList<Biotransformation> applyReactionAndReturnBiotransformations(IAtomContainer target, MetabolicReaction reaction, boolean preprocess) throws Exception {
		return this.applyReactionAndReturnBiotransformations(target, reaction, preprocess, (double)0.0F);
	}

	public ArrayList<Biotransformation> applyReactionAndReturnBiotransformations(IAtomContainer target, MetabolicReaction reaction, boolean preprocess, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();
		IAtomContainer starget = ChemStructureManipulator.standardizeMoleculeWithCopy(target, preprocess);
		ArrayList<MetabolicReaction> matchedReactions = new ArrayList<>();
		InChIGenerator gen0 = this.inchiGenFactory.getInChIGenerator(target);
		target.setProperty("InChI", gen0.getInchi());
		target.setProperty("InChIKey", gen0.getInchiKey());
		target.setProperty("SMILES", this.smiGen.create(target));
		Utilities.addPhysicoChemicalProperties(target);
		target.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(target));
		boolean match_constraints = ChemStructureExplorer.compoundMatchesReactionConstraints(reaction, starget);
		if (match_constraints) {
			matchedReactions.add(reaction);
		}

		IAtomContainerSet partialSet = this.generateAllMetabolitesFromAtomContainer(starget, reaction, false);
		Double score = (double)0.0F;
		AtomContainerSet subs = new AtomContainerSet();
		AtomContainerSet prod = new AtomContainerSet();
		if (partialSet.getAtomContainerCount() > 0) {
			if (target.getProperty("Score") != null) {
				score = (double)target.getProperty("Score") * this.bSystem.getReactionsORatios().get(reaction.name);
			} else {
				score = (Double)this.bSystem.getReactionsORatios().get(reaction.name);
			}

			if (score != null && score >= scoreThreshold) {
				subs.addAtomContainer(target);

				for(IAtomContainer pc : partialSet.atomContainers()) {
					InChIGenerator gen = this.inchiGenFactory.getInChIGenerator(pc);
					pc.setProperty("InChI", gen.getInchi());
					pc.setProperty("InChIKey", gen.getInchiKey());
					pc.setProperty("SMILES", this.smiGen.create(pc));
					pc.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(pc));
					Utilities.addPhysicoChemicalProperties(pc);
					prod.addAtomContainer(AtomContainerManipulator.removeHydrogens(pc));
				}

				Biotransformation bioT = new Biotransformation(subs, reaction.name, null, prod, score, this.getBioSystemName());
				results.add(bioT);
			}
		}

		return results;
	}

	public ArrayList<Biotransformation> applyReactionsFromContainersAndReturnBiotransformations(IAtomContainerSet targets, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> products = new ArrayList<>();

		for(IAtomContainer ac : targets.atomContainers()) {
			products.addAll(this.applyReactionsAndReturnBiotransformations(ac, reactions, preprocess, filter, scoreThreshold));
		}

		return products;
	}

	public ArrayList<Biotransformation> applyReactionsChainAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps) throws Exception {
		return this.applyReactionsChainAndReturnBiotransformations(target, reactions, preprocess, filter, nr_of_steps, (double)0.0F);
	}

	public ArrayList<Biotransformation> applyReactionsChainAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps, Double scoreThreshold) throws Exception {
		AtomContainerSet startingSet = new AtomContainerSet();
		startingSet.addAtomContainer(target);
		return this.applyReactionsChainAndReturnBiotransformations((IAtomContainerSet)startingSet, reactions, preprocess, filter, nr_of_steps, scoreThreshold);
	}

	public ArrayList<Biotransformation> applyReactionsChainAndReturnBiotransformations(IAtomContainerSet targets, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps) throws Exception {
		return this.applyReactionsChainAndReturnBiotransformations(targets, reactions, preprocess, filter, nr_of_steps, (double)0.0F);
	}

	public ArrayList<Biotransformation> applyReactionsChainAndReturnBiotransformations(IAtomContainerSet targets, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> products = new ArrayList<>();
		IAtomContainerSet containers = targets;

		ArrayList<Biotransformation> currentProducts;
		for(int counter = 0; nr_of_steps > 0; containers = this.extractProductsFromBiotransformations(currentProducts)) {
			++counter;
			currentProducts = this.applyReactionsFromContainersAndReturnBiotransformations(containers, reactions, preprocess, filter, scoreThreshold);
			--nr_of_steps;
			if (currentProducts.isEmpty()) {
				break;
			}

			products.addAll(currentProducts);
			containers.removeAllAtomContainers();
		}

		return products;
	}

	public IAtomContainerSet applyReactions(IAtomContainer target, SMIRKSManager smrkMan, LinkedHashMap<String, MetabolicReaction> reactions, boolean preprocess, boolean filter) throws Exception {
		return this.applyReactions(target, reactions, preprocess, filter, (double)0.0F);
	}

	public IAtomContainerSet applyReactions(IAtomContainer target, LinkedHashMap<String, MetabolicReaction> reactions, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		IAtomContainerSet products = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		int count = 0;
		ArrayList<MetabolicReaction> matchedReactions = new ArrayList<>();
		new ArrayList<>();
		IAtomContainer starget = target.clone();
		if (preprocess) {
			try {
				starget = ChemStructureManipulator.preprocessContainer(target);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(starget);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(target);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(target);
		}

		InChIGenerator gen0 = this.inchiGenFactory.getInChIGenerator(target);

		for(MetabolicReaction i : reactions.values()) {
			boolean match_constraints = ChemStructureExplorer.compoundMatchesReactionConstraints(i, starget);
			if (match_constraints) {
				matchedReactions.add(i);
			}
		}

		ArrayList<MetabolicReaction> filteredReactions;
		if (!filter) {
			filteredReactions = matchedReactions;
		} else {
			filteredReactions = new ArrayList<>(this.mRFilter.filterReactions(matchedReactions).values());
		}

		for(MetabolicReaction j : filteredReactions) {
			IAtomContainerSet partialSet = this.generateAllMetabolitesFromAtomContainer(starget, j, false);
			if (partialSet.getAtomContainerCount() > 0) {
				for(IAtomContainer pc : partialSet.atomContainers()) {
					Double score = (double)0.0F;
					if (target.getProperty("Score") != null) {
						score = (double)target.getProperty("Score") * this.bSystem.getReactionsORatios().get(j.name);
					} else {
						score = (Double)this.bSystem.getReactionsORatios().get(j.name);
					}

					if (score != null && score >= scoreThreshold) {
						pc.setProperty("Reaction", j.name);
						InChIGenerator gen = this.inchiGenFactory.getInChIGenerator(pc);
						pc.setProperty("Precursor", gen0.getInchiKey());
						pc.setProperty("InChI", gen.getInchi());
						pc.setProperty("InChIKey", gen.getInchiKey());
						pc.setProperty("Score", score);
						Utilities.addPhysicoChemicalProperties(pc);
						products.addAtomContainer(AtomContainerManipulator.removeHydrogens(pc));
					}
				}
			}
		}

		IAtomContainerSet unique = ChemStructureExplorer.uniquefy(products);
		return unique;
	}

	public IAtomContainerSet applyReactionChain(IAtomContainer target, SMIRKSManager smrkMan, LinkedHashMap<String, MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps) throws Exception {
		return this.applyReactionChain(target, smrkMan, reactions, preprocess, filter, nr_of_steps, (double)0.0F);
	}

	public IAtomContainerSet applyReactionChain(IAtomContainer target, SMIRKSManager smrkMan, LinkedHashMap<String, MetabolicReaction> reactions, boolean preprocess, boolean filter, int nr_of_steps, Double scoreThreshold) throws Exception {
		IAtomContainerSet products = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		int step = 0;
		boolean stop = false;
		if (preprocess) {
			try {
				target = ChemStructureManipulator.preprocessContainer(target);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(target);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(target);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(target);
		}

		while(nr_of_steps > 0) {
			++step;
			IAtomContainerSet currentProducts = this.applyReactions(target, this.smrkMan, reactions, false, filter);
			--nr_of_steps;
			if (currentProducts.getAtomContainerCount() <= 0) {
				stop = true;
				break;
			}

			int j = 1;

			for(IAtomContainer ac : currentProducts.atomContainers()) {
				if (target.getProperty("cdk:Title") == null) {
					ac.setProperty("cdk:Title", "metabolite_" + j);
				} else {
					ac.setProperty("cdk:Title", target.getProperty("cdk:Title") + "_" + j);
				}

				++j;
				products.addAtomContainer(ac);
				IAtomContainerSet t = this.applyReactionChain(ac, this.smrkMan, reactions, false, filter, nr_of_steps, scoreThreshold);
				if (t.getAtomContainerCount() > 0) {
					products.add(t);
				}
			}
		}

		System.err.println(products.getAtomContainerCount() + " products after " + step + " step(s).");
		return ChemStructureExplorer.uniquefy(products);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainer substrate, String enz, boolean preprocess, boolean filter, double threshold) throws Exception {
		return this.metabolizeWithEnzyme(substrate, enz, true, preprocess, filter, threshold);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainer substrate, String enz, boolean predictESSpecificity, boolean preprocess, boolean filter, double threshold) throws Exception {
		IAtomContainer clonedSubs = substrate.clone();

		try {
			if (this.bSystem.getEnzymeHash().containsKey(enz)) {
				Enzyme e = (Enzyme)this.bSystem.getEnzymeHash().get(enz);
				return this.metabolizeWithEnzyme(substrate, e, predictESSpecificity, preprocess, filter, threshold);
			} else {
				throw new IllegalArgumentException(enz.toString() + " is not associated with the biosystem " + this.getBioSystemName());
			}
		} catch (IllegalArgumentException iae) {
			System.err.println(iae.getLocalizedMessage());
			return null;
		}
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainer substrate, Enzyme enzyme, boolean preprocess, boolean filter, double threshold) throws Exception {
		return this.metabolizeWithEnzyme(substrate, enzyme, true, preprocess, filter, threshold);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainer substrate, Enzyme enzyme, boolean predictESSpecificity, boolean preprocess, boolean filter, double threshold) throws Exception {
		IAtomContainer clonedSub = substrate.clone();
		if (preprocess) {
			clonedSub = ChemStructureManipulator.preprocessContainer(clonedSub);
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(clonedSub);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(clonedSub);
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(clonedSub);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(clonedSub);
		}

		if (!this.bSystem.getEnzymeHash().containsKey(enzyme.getName())) {
			throw new IllegalArgumentException(enzyme.getName() + " is not associated with the biosystem " + this.getBioSystemName());
		} else {
			ArrayList<Biotransformation> biotransformations = new ArrayList<>();
			ArrayList<ChemicalClassFinder.ChemicalClassName> chemClasses = ChemicalClassFinder.AssignChemicalClasses(clonedSub);
			if (predictESSpecificity) {
				if (this.esspredictor.isValidSubstrate(clonedSub, enzyme.getName(), chemClasses)) {
					biotransformations = this.applyReactionsAndReturnBiotransformations(substrate, enzyme.getReactionSet(), preprocess, filter, threshold);

					for(Biotransformation bt : biotransformations) {
						if (bt.getEnzymeNames() != null && !bt.getEnzymeNames().isEmpty()) {
							bt.getEnzymeNames().add(enzyme.getName());
						} else {
							ArrayList<String> elist = new ArrayList<>();
							elist.add(String.valueOf(enzyme.getName()));
							bt.setEnzymeNames(elist);
						}
					}
				} else {
					biotransformations = this.applyReactionsAndReturnBiotransformations(substrate, enzyme.getReactionSet(), preprocess, filter, threshold);

					for(Biotransformation bt : biotransformations) {
						if (bt.getEnzymeNames() != null && !bt.getEnzymeNames().isEmpty()) {
							bt.getEnzymeNames().add(enzyme.getName());
						} else {
							ArrayList<String> elist = new ArrayList<>();
							elist.add(String.valueOf(enzyme.getName()));
							bt.setEnzymeNames(elist);
						}
					}
				}
			}

			return biotransformations;
		}
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainer substrate, Enzyme enzyme, boolean predictESSpecificity, boolean preprocess, boolean filter, int nrOfSteps, double threshold) throws Exception {
		IAtomContainerSet substrates = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		substrates.addAtomContainer(substrate);
		IAtomContainerSet containers = substrates;
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();

		ArrayList<Biotransformation> currentProducts;
		for(int counter = 0; nrOfSteps > 0; containers = this.extractProductsFromBiotransformations(currentProducts)) {
			++counter;
			currentProducts = this.metabolizeWithEnzyme(containers, enzyme, predictESSpecificity, preprocess, filter, threshold);
			--nrOfSteps;
			if (currentProducts.isEmpty()) {
				break;
			}

			biotransformations.addAll(currentProducts);
			containers.removeAllAtomContainers();
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> metabolizeWithEnzyme(IAtomContainerSet substrates, Enzyme enzyme, boolean predictESSpecificity, boolean preprocess, boolean filter, double threshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();

		for(IAtomContainer atc : substrates.atomContainers()) {
			biotransformations.addAll(this.metabolizeWithEnzyme(atc, enzyme, predictESSpecificity, preprocess, filter, threshold));
		}

		return biotransformations;
	}

	public boolean isValidSubstrate(IAtomContainer target, String enzymeName) throws Exception {
		return this.esspredictor.isValidSubstrate(target, enzymeName);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymes(IAtomContainer target, ArrayList<Enzyme> enzymes, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		return this.metabolizeWithEnzymes(target, enzymes, true, preprocess, filter, scoreThreshold);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymes(IAtomContainer target, ArrayList<Enzyme> enzymes, boolean predictESSpecificity, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();
		ArrayList<Enzyme> metabolizingEnzymes = new ArrayList<>();
		LinkedHashMap<String, ArrayList<String>> reactToEnzymes = new LinkedHashMap<>();
		LinkedHashMap<String, MetabolicReaction> reactions = new LinkedHashMap<>();
		ArrayList<MetabolicReaction> matchedReactions = new ArrayList<>();
		IAtomContainer starget = target.clone();
		if (preprocess) {
			try {
				starget = ChemStructureManipulator.preprocessContainer(target);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(starget);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(starget);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(starget);
		}

		InChIGenerator gen0 = this.inchiGenFactory.getInChIGenerator(target);
		if (target.getProperty("InChI") == null || ((String)target.getProperty("InChI")).trim().length() == 0) {
			target.setProperty("InChI", gen0.getInchi());
			target.setProperty("InChIKey", gen0.getInchiKey());
			Utilities.addPhysicoChemicalProperties(target);
			target.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(target));
		}

		if (target.getProperty("SMILES") == null) {
			target.setProperty("SMILES", this.smiGen.create(AtomContainerManipulator.removeHydrogens(target)));
		}

		ArrayList<ChemicalClassFinder.ChemicalClassName> chemClasses = ChemicalClassFinder.AssignChemicalClasses(starget);
		if (predictESSpecificity) {
			for(Enzyme enz : enzymes) {
				if (this.esspredictor.isValidSubstrate(starget, enz.getName(), chemClasses)) {
					metabolizingEnzymes.add(enz);
				}
			}
		} else {
			metabolizingEnzymes = enzymes;
		}

		for(Enzyme enzy : metabolizingEnzymes) {
			for(MetabolicReaction m : enzy.getReactionSet()) {
				m.getComonName().equals("EAWAG_RULE_BT1764");
				if (ChemStructureExplorer.compoundMatchesReactionConstraints(m, starget)) {
					if (reactToEnzymes.get(m.getReactionName()) == null) {
						reactToEnzymes.put(m.getReactionName(), new ArrayList<>());
						((ArrayList)reactToEnzymes.get(m.getReactionName())).add(enzy.getName());
					} else {
						((ArrayList)reactToEnzymes.get(m.getReactionName())).add(enzy.getName());
					}

					reactions.put(m.getReactionName(), m);
					matchedReactions.add(m);
				}
			}
		}

		new ArrayList<>();
		ArrayList<MetabolicReaction> var30;
		if (!filter) {
			var30 = matchedReactions;
		} else {
			var30 = new ArrayList<>(this.mRFilter.filterReactions(matchedReactions).values());
		}

		for(MetabolicReaction j : var30) {
			IAtomContainer n = this.smiParser.parseSmiles(this.smiGen.create(starget));
			IAtomContainerSet partialSet = this.generateAllMetabolitesFromAtomContainer(n, j, true);
			Double score = (double)0.0F;
			AtomContainerSet subs = new AtomContainerSet();
			AtomContainerSet prod = new AtomContainerSet();
			if (partialSet.getAtomContainerCount() > 0) {
				if (target.getProperty("Score") != null) {
					score = (double)target.getProperty("Score") * this.bSystem.getReactionsORatios().get(j.name);
				} else {
					try {
						score = (Double)this.bSystem.getReactionsORatios().get(j.name);
					} catch (Exception var27) {
						score = (double)0.5F;
					}
				}

				if (score != null && score >= scoreThreshold) {
					subs.addAtomContainer(target);

					for(IAtomContainer pc : partialSet.atomContainers()) {
						if (!this.containsK(pc) && !ChemStructureExplorer.isUnneccessaryMetabolite(pc)) {
							try {
								InChIGenerator gen = this.inchiGenFactory.getInChIGenerator(pc);
								pc.setProperty("InChI", gen.getInchi());
								pc.setProperty("InChIKey", gen.getInchiKey());
								pc.setProperty("SMILES", this.smiGen.create(AtomContainerManipulator.removeHydrogens(pc)));
							} catch (CDKException c) {
								System.err.println(c.getLocalizedMessage());
							}

							Utilities.addPhysicoChemicalProperties(pc);
							prod.addAtomContainer(AtomContainerManipulator.removeHydrogens(pc));
							prod.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(target));
						}
					}

					new ArrayList<>();
					Biotransformation bioT = new Biotransformation(subs, j.name, reactToEnzymes.get(j.name), prod, score, this.getBioSystemName());
					results.add(bioT);
				}
			}
		}

		return results;
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymes(IAtomContainerSet substrates, ArrayList<Enzyme> enzymes, boolean preprocess, boolean filter, double threshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();

		for(IAtomContainer ac : substrates.atomContainers()) {
			biotransformations.addAll(this.metabolizeWithEnzymes(ac, enzymes, preprocess, filter, threshold));
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymesDephtFirst(IAtomContainer target, ArrayList<Enzyme> enzymes, boolean preprocess, boolean filter, int nrOfSteps, Double scoreThreshold) throws Exception {
		return this.metabolizeWithEnzymesDephtFirst(target, enzymes, true, preprocess, filter, nrOfSteps, scoreThreshold);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymesDephtFirst(IAtomContainer target, ArrayList<Enzyme> enzymes, boolean predictESSpecificity, boolean preprocess, boolean filter, int nrOfSteps, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();

		for(Enzyme enz : enzymes) {
			results.addAll(this.metabolizeWithEnzyme(target, enz, predictESSpecificity, preprocess, filter, nrOfSteps, scoreThreshold));
		}

		return results;
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymesBreadthFirst(IAtomContainer target, ArrayList<Enzyme> enzymes, boolean preprocess, boolean filter, int nrOfSteps, Double scoreThreshold) throws Exception {
		IAtomContainerSet targets = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		targets.addAtomContainer(target);
		return this.metabolizeWithEnzymesBreadthFirst(targets, enzymes, preprocess, filter, nrOfSteps, scoreThreshold);
	}

	public ArrayList<Biotransformation> metabolizeWithEnzymesBreadthFirst(IAtomContainerSet targets, ArrayList<Enzyme> enzymes, boolean preprocess, boolean filter, int nrOfSteps, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();
		IAtomContainerSet containers = targets;

		ArrayList<Biotransformation> currentProducts;
		for(int counter = 0; nrOfSteps > 0; containers = this.extractProductsFromBiotransformations(currentProducts)) {
			++counter;
			currentProducts = this.metabolizeWithEnzymes(containers, enzymes, preprocess, filter, scoreThreshold);
			--nrOfSteps;
			if (currentProducts.isEmpty()) {
				break;
			}

			results.addAll(currentProducts);
			containers.removeAllAtomContainers();
		}

		return results;
	}

	public ArrayList<Biotransformation> applyReactionAtOnceAndReturnBiotransformations(IAtomContainer target, MetabolicReaction reaction, boolean preprocess, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> results = new ArrayList<>();
		IAtomContainer starget = target.clone();
		if (preprocess) {
			try {
				starget = ChemStructureManipulator.preprocessContainer(starget);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(starget);
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(starget);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(starget);
		}

		InChIGenerator gen0 = this.inchiGenFactory.getInChIGenerator(target);
		target.setProperty("InChI", gen0.getInchi());
		target.setProperty("InChIKey", gen0.getInchiKey());
		target.setProperty("SMILES", this.smiGen.create(target));
		target.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(target));
		IAtomContainerSet partialSet = this.generateAllMetabolitesFromAtomContainerViaTransformationAtAllLocations(starget, reaction.getSmirksReaction(), false);
		Double score = (double)0.0F;
		IAtomContainerSet subs = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		IAtomContainerSet prods = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		if (partialSet.getAtomContainerCount() > 0) {
			if (target.getProperty("Score") != null) {
				score = (double)target.getProperty("Score") * this.bSystem.getReactionsORatios().get(reaction.name);
			} else {
				score = (Double)this.bSystem.getReactionsORatios().get(reaction.name);
			}

			if (score != null && score >= scoreThreshold) {
				subs.addAtomContainer(target);

				for(IAtomContainer pc : partialSet.atomContainers()) {
					InChIGenerator gen = this.inchiGenFactory.getInChIGenerator(pc);
					pc.setProperty("InChI", gen.getInchi());
					pc.setProperty("InChIKey", gen.getInchiKey());
					pc.setProperty("SMILES", this.smiGen.create(pc));
					pc.setProperty("Molecular formula", ChemStructureExplorer.getMolecularFormula(pc));
					prods.addAtomContainer(AtomContainerManipulator.removeHydrogens(pc));
				}

				Biotransformation bioT = new Biotransformation(subs, reaction.name, null, prods, score, this.getBioSystemName());
				results.add(bioT);
			}
		}

		return results;
	}

	public ArrayList<Biotransformation> applyReactionAtOnceAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		ArrayList<MetabolicReaction> matchedReactions = new ArrayList<>();
		new ArrayList<>();

		for(MetabolicReaction i : reactions) {
			boolean match_constraints = ChemStructureExplorer.compoundMatchesReactionConstraints(i, target);
			if (match_constraints) {
				matchedReactions.add(i);
			}
		}

		ArrayList<MetabolicReaction> filteredReactions;
		if (!filter) {
			filteredReactions = matchedReactions;
		} else {
			filteredReactions = new ArrayList<>(this.mRFilter.filterReactions(matchedReactions).values());
		}


		for(MetabolicReaction mreact : reactions) {
			ArrayList<Biotransformation> bt = this.applyReactionAtOnceAndReturnBiotransformations(target, mreact, preprocess, scoreThreshold);
			ArrayList<Biotransformation> selectedBiotransformations = new ArrayList<>();

			for(Biotransformation b : bt) {
				if (b.getSubstrates().getAtomContainerCount() == b.getProducts().getAtomContainerCount() && b.getSubstrates().getAtomContainerCount() == 1 && ChemStructureExplorer.inchikeyEqualityHolds(b.getSubstrates().getAtomContainer(0), b.getProducts().getAtomContainer(0))) {
					System.err.println("Removing " + b.getReactionType());
				} else {
					selectedBiotransformations.add(b);
				}
			}

			biotransformations.addAll(selectedBiotransformations);
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> applyReactionAtOnceAndReturnBiotransformations(IAtomContainerSet targets, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();

		for(IAtomContainer a : targets.atomContainers()) {
			biotransformations.addAll(this.applyReactionAtOnceAndReturnBiotransformations(a, reactions, preprocess, filter, scoreThreshold));
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> applyReactionAtOnceAndReturnBiotransformations(IAtomContainer target, ArrayList<MetabolicReaction> reactions, boolean preprocess, boolean filter, int nrOfSteps, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		int step = 0;
		int i = nrOfSteps;
		IAtomContainerSet startingSet = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		startingSet.addAtomContainer(target);

		while(nrOfSteps > 0) {
			++step;
			System.out.println("Step " + step + " out of " + i);
			ArrayList<Biotransformation> partialBiotransf = this.applyReactionAtOnceAndReturnBiotransformations(target, reactions, preprocess, filter, scoreThreshold);
			--nrOfSteps;
			System.out.println("Remaining steps " + nrOfSteps);
			if (partialBiotransf.size() <= 0) {
				break;
			}

			biotransformations.addAll(partialBiotransf);
			startingSet.removeAllAtomContainers();
			startingSet = this.extractProductsFromBiotransformations(partialBiotransf);

			for(IAtomContainer a : startingSet.atomContainers()) {
				biotransformations.addAll(this.applyReactionAtOnceAndReturnBiotransformations(a, reactions, false, filter, nrOfSteps, scoreThreshold));
			}
		}

		return biotransformations;
	}

	public SmilesParser getSmiParser() {
		return this.smiParser;
	}

	public IAtomContainerSet extractProductsFromBiotransformations(ArrayList<Biotransformation> biotransformations) throws Exception {
		IAtomContainerSet acontainers = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		LinkedHashMap<String, IAtomContainer> hMap = new LinkedHashMap<>();

		for(Biotransformation b : biotransformations) {
			int numAtoms_substrate = 0;
			IAtomContainerSet substrates = b.getSubstrates();

			for(int i = 0; i < substrates.getAtomContainerCount(); ++i) {
				IAtomContainer oneSubstrate = substrates.getAtomContainer(i);
				numAtoms_substrate += this.countNonHydrogenAtoms(oneSubstrate);
			}

			for(IAtomContainer ac : b.getProducts().atomContainers()) {
				String ikey = (String)ac.getProperty("InChIKey");
				int numAtoms_metabolites = this.countNonHydrogenAtoms(ac);
				if (numAtoms_metabolites >= numAtoms_substrate + 4 || ChemStructureExplorer.getMajorIsotopeMass(ac) > (double)1500.0F) {
					ac.setProperty("isEndProduct", true);
				}

				if (!hMap.containsKey(ikey)) {
					hMap.put(ikey, ac);
				}
			}
		}

		for(IAtomContainer a : new ArrayList<>(hMap.values())) {
			if (this.countNonHydrogenAtoms(a) > 4) {
				acontainers.addAtomContainer(ChemStructureManipulator.preprocessContainer(a));
			}
		}

		return acontainers;
	}

	public int countNonHydrogenAtoms(IAtomContainer molecule) throws Exception {
		int count = 0;

		for(int i = 0; i < molecule.getAtomCount(); ++i) {
			if (!molecule.getAtom(i).getSymbol().equalsIgnoreCase("H")) {
				++count;
			}
		}

		return count;
	}

	public IAtomContainerSet extractProductsFromBiotransformationsWithTransformationData(ArrayList<Biotransformation> biotransformations, LinkedHashMap<String, MetabolicReaction> customReactionHash, boolean annotate) throws Exception {
		ArrayList<Biotransformation> uniqueBiotransformations = Utilities.selectUniqueBiotransformations(biotransformations);
		IAtomContainerSet acontainers = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		LinkedHashMap<String, IAtomContainer> hMap = new LinkedHashMap<>();
		System.out.println("Unique Biotransformations: " + uniqueBiotransformations.size());
		System.out.println("Unique metabolites: " + Utilities.extractProductsFromBiotransformations(uniqueBiotransformations).getAtomContainerCount());
		int metaboliteID = 0;
		if (uniqueBiotransformations != null) {
			for(Biotransformation b : uniqueBiotransformations) {
				for(IAtomContainer ac : b.getProducts().atomContainers()) {
					if (!this.containsK(ac)) {
						LinkedHashMap<Object, Object> properties = new LinkedHashMap<>();
						String ikey = (String)ac.getProperty("InChIKey");
						IAtomContainer hash_ac;
						if (hMap.containsKey(ikey)) {
							hash_ac = ((IAtomContainer)hMap.get(ikey)).clone();
							properties.put("InChI", hash_ac.getProperty("InChI"));
							properties.put("InChIKey", hash_ac.getProperty("InChIKey"));
							properties.put("SMILES", hash_ac.getProperty("SMILES"));
							properties.put("Synonyms", hash_ac.getProperty("Synonyms"));
							properties.put("PUBCHEM_CID", hash_ac.getProperty("PUBCHEM_CID"));
							properties.put("Molecular formula", hash_ac.getProperty("Molecular formula"));
							properties.put("Major Isotope Mass", hash_ac.getProperty("Major Isotope Mass"));
							properties.put("ALogP", hash_ac.getProperty("ALogP"));
							properties.put("Lipinski_Violations", hash_ac.getProperty("Lipinski_Violations"));
							properties.put("Insecticide_Likeness_Violations", hash_ac.getProperty("Insecticide_Likeness_Violations"));
							properties.put("Post_Em_Herbicide_Likeness_Violations", hash_ac.getProperty("Post_Em_Herbicide_Likeness_Violations"));
							properties.put("Metabolite ID", hash_ac.getProperty("Metabolite ID"));
							properties.put("cdk:Title", hash_ac.getProperty("cdk:Title"));
						} else {
							hash_ac = ac.clone();
							LinkedHashMap<Object, Object> refProps = new LinkedHashMap();
							String synonyms = null;
							String pubchemCID = null;
							if (annotate) {
								System.out.println("\n\n===========================================");
								System.out.println("Fetching CIDs and synonyms from PubChem");
								System.out.println("===========================================\n\n");
								LinkedHashMap<String, ArrayList<String>> data = ChemdbRest.getSynonymsObjectViaInChIKey(ikey);
								if (data != null) {
									pubchemCID = (String)((ArrayList<?>)data.get("CID")).getFirst();
									synonyms = StringUtils.join(data.get("Synonyms"), "\n");
								}
							}

							refProps.put("InChI", ac.getProperty("InChI"));
							refProps.put("InChIKey", ac.getProperty("InChIKey"));
							refProps.put("SMILES", hash_ac.getProperty("SMILES"));
							refProps.put("Synonyms", synonyms);
							refProps.put("PUBCHEM_CID", pubchemCID);
							if (ac.getProperty("Molecular formula") != null) {
								refProps.put("Molecular formula", ac.getProperty("Molecular formula"));
							} else {
								refProps.put("Molecular formula", ChemStructureExplorer.getMolecularFormula(ac));
							}

							if (ac.getProperty("Molecular formula") != null) {
								refProps.put("Major Isotope Mass", ac.getProperty("Major Isotope Mass"));
								refProps.put("ALogP", ac.getProperty("ALogP"));
								refProps.put("Lipinski_Violations", ac.getProperty("Lipinski_Violations"));
								refProps.put("Insecticide_Likeness_Violations", ac.getProperty("Insecticide_Likeness_Violations"));
								refProps.put("Post_Em_Herbicide_Likeness_Violations", ac.getProperty("Post_Em_Herbicide_Likeness_Violations"));
							} else {
								LinkedHashMap<String, String> physchem = ChemStructureExplorer.computePhysicoChemicalProperties(ac);
								refProps.put("Major Isotope Mass", physchem.get("Major Isotope Mass"));
								refProps.put("ALogP", physchem.get("ALogP"));
								LinkedHashMap<String, Integer> violations = ChemStructureExplorer.calculateLikenessViolations(ac);
								refProps.put("Lipinski_Violations", String.format("%s", violations.get("Lipinski_Violations")));
								refProps.put("Insecticide_Likeness_Violations", String.format("%s", violations.get("Insecticide_Likeness_Violations")));
								refProps.put("Post_Em_Herbicide_Likeness_Violations", String.format("%s", violations.get("Post_Em_Herbicide_Likeness_Violations")));
							}

							++metaboliteID;
							refProps.put("Metabolite ID", "BTM" + String.format("%05d", metaboliteID));
							refProps.put("cdk:Title", "BTM" + String.format("%05d", metaboliteID));
							hash_ac.setProperties(refProps);
							hMap.put((String)ac.getProperty("InChIKey"), hash_ac);
							properties = (LinkedHashMap)refProps.clone();
						}

						String reactionName;
						if (customReactionHash.get(b.getReactionType().toString()) != null) {
							reactionName = ((MetabolicReaction)customReactionHash.get(b.getReactionType().toString())).getComonName();
						} else if (b.getReactionType().toString().contains(" AndFromCyProduct")) {
							String reactionType = b.getReactionType().toString().replace(" AndFromCyProduct", "");
							if (customReactionHash.get(reactionType) != null) {
								reactionName = ((MetabolicReaction)customReactionHash.get(reactionType)).getComonName();
							} else {
								reactionName = b.getReactionType();
							}

							reactionName = reactionName + " AndFromCyProduct";
						} else {
							reactionName = b.getReactionType();
						}

						if (reactionName.isEmpty()) {
							reactionName = ((MetabolicReaction)customReactionHash.get(b.getReactionType().toString())).toString();
						}

						properties.put("Reaction", reactionName);
						if (customReactionHash.get(b.getReactionType().toString()) != null) {
							properties.put("Reaction ID", ((MetabolicReaction)customReactionHash.get(b.getReactionType().toString())).getBTRMID());
						} else if (ac.getProperty("Reaction ID") != null) {
							properties.put("Reaction ID", ac.getProperty("Reaction ID"));
						} else {
							properties.put("Reaction ID", "N/A");
						}

						if (!b.getEnzymeNames().isEmpty()) {
							ArrayList<String> enzymes = new ArrayList<>();

							for(String en : b.getEnzymeNames()) {
								if (en.toString().contains("EC_")) {
									enzymes.add(en.toString().replace("EC_", "EC ").replaceAll("_", "."));
								} else {
									String enzymeName = en.toString().replace("HYDROXYCINNAMATE_DECARBOXYLASE", "4-Hydroxycinnamate decarboxylase").replace("PHENOLIC_ACID_DECARBOXYLASE", "Bacterial phenolic acid decarboxylase (EC 4.1.1.-)").replace("DECARBOXYLASE", "Unspecified bacterial decarboxylase").replace("DEMETHYLASE", "Unspecified bacterial demethylase").replace("DEHYDROXYLASE", "Unspecified bacterial dehydroxylase").replace("DEHYDROXYLASE", "Unspecified bacterial dehydroxylase").replace("BACTERIAL_LACTONASE", "Unspecified bacterial lactonase").replace("VINYL_PHENOL_REDUCTASE", "Vinyl phenol reductase").replace("ABKAR1", "Alpha,beta-ketoalkene double bond reductase 1").replace("UDP_GLUCURONOSYLTRANSFERASE", "Bacterial UDP-glucuronosyltransferase").replace("ACETYLTRANSFERASE", "Unspecified acetyltransferase").replace("UNSPECIFIED_BACTERIAL_ISOFLAVONE_REDUCTASE", "Unspecified bacterial isoflavone reductase").replace("UNSPECIFIED_GUT_BACTERIAL_ENZYME", "Unspecified gut bacterial enzyme").replace("UNSPECIFIED_ENVIRONMENTAL_BACTERIAL_ENZYME", "Unspecified environmental bacterial enzyme").replace("BACTERIAL_BILE_SALT_3_HYDROXYSTEROID_DEHYDROGENASE", "Bacterial bile salt 3-hydroxysteroid dehydrogenase").replace("BACTERIAL_BILE_SALT_7_HYDROXYSTEROID_DEHYDROGENASE", "Bacterial bile salt 7-hydroxysteroid dehydrogenase").replace("BACTERIAL_BILE_SALT_12_HYDROXYSTEROID_DEHYDROGENASE", "Bacterial bile salt 12-hydroxysteroid dehydrogenase").replace("BACTERIAL_NITROREDUCTASE", "Bacterial oxygen-insensitive NADPH nitroreductase").replace("EC_3_5_1_24", "Choloylglycine hydrolase");
									enzymes.add(enzymeName.toString());
								}
							}

							properties.put("Enzyme(s)", StringUtils.join(enzymes, "\n"));
							properties.put("Biosystem", b.getBioSystemName().name());
						}

						if (b.getSubstrates().getAtomContainerCount() == 1) {
							IAtomContainer substrate = (IAtomContainer)hMap.get(b.getSubstrates().getAtomContainer(0).getProperty("InChIKey"));
							String tt = null;
							if (substrate == null) {
								substrate = b.getSubstrates().getAtomContainer(0).clone();
								LinkedHashMap<Object, Object> refProps = new LinkedHashMap<>();
								if (substrate.getProperty("SMILES") != null) {
									refProps.put("SMILES", substrate.getProperty("SMILES"));
								} else {
									refProps.put("SMILES", this.smiGen.create(substrate));
								}

								if (substrate.getProperty("Molecular formula") != null) {
									refProps.put("Molecular formula", ac.getProperty("Molecular formula"));
								} else {
									refProps.put("Molecular formula", ChemStructureExplorer.getMolecularFormula(substrate));
								}

								if (substrate.getProperty("Major Isotope Mass") != null) {
									refProps.put("Major Isotope Mass", substrate.getProperty("Major Isotope Mass"));
									refProps.put("ALogP", substrate.getProperty("ALogP"));
									refProps.put("Lipinski_Violations", substrate.getProperty("Lipinski_Violations"));
									refProps.put("Insecticide_Likeness_Violations", substrate.getProperty("Insecticide_Likeness_Violations"));
									refProps.put("Post_Em_Herbicide_Likeness_Violations", substrate.getProperty("Post_Em_Herbicide_Likeness_Violations"));
								} else {
									LinkedHashMap<String, String> physchem = ChemStructureExplorer.computePhysicoChemicalProperties(substrate);
									refProps.put("Major Isotope Mass", String.format("%.8s", Double.valueOf((String)physchem.get("Major Isotope Mass"))));
									refProps.put("Major Isotope Mass", String.format("%.8s", Double.valueOf((String)physchem.get("Major Isotope Mass"))));
									LinkedHashMap<String, Integer> violations = ChemStructureExplorer.calculateLikenessViolations(substrate);
									refProps.put("Lipinski_Violations", String.format("%s", violations.get("Lipinski_Violations")));
									refProps.put("Insecticide_Likeness_Violations", String.format("%s", violations.get("Insecticide_Likeness_Violations")));
									refProps.put("Post_Em_Herbicide_Likeness_Violations", String.format("%s", violations.get("Post_Em_Herbicide_Likeness_Violations")));
								}

								tt = (String)substrate.getProperty("cdk:Title");
								if (tt == null) {
									String syno = (String)substrate.getProperty("Synonyms");
									if (syno != null) {
										tt = Utilities.returnFirstCleanSynonym(syno.split("\n"));
									}

									if (tt == null) {
										tt = (String)substrate.getProperty("Name");
										if (tt == null) {
											tt = (String)substrate.getProperty("NAME");
											if (tt == null) {
												tt = (String)substrate.getProperty("DATABASE_ID");
												if (tt == null) {
													tt = (String)substrate.getProperty("DRUGBANK_ID");
													if (tt == null) {
														tt = (String)substrate.getProperty("Metabolite ID");
														if (tt == null) {
															tt = (String)substrate.getProperty("$MolName");
															if (annotate && tt == null) {
																System.out.println("\n\n===========================================");
																System.out.println("Fetching CIDs and synonyms from PubChem");
																System.out.println("===========================================\n\n");
																LinkedHashMap<String, ArrayList<String>> data = ChemdbRest.getSynonymsObjectViaInChIKey(ikey);
																if (data != null && data.get("Synonyms") != null) {
																	tt = Utilities.returnFirstCleanSynonym(data.get("Synonyms"));
																}
															}
														}
													}
												}
											}
										}
									}
								}

								refProps.put("cdk:Title", tt);
								substrate.addProperties(refProps);
								hMap.put((String)substrate.getProperty("InChIKey"), substrate);
							}

							properties.put("Precursor ID", substrate.getProperty("cdk:Title"));
							properties.put("Precursor SMILES", substrate.getProperty("SMILES"));
							properties.put("Precursor InChI", substrate.getProperty("InChI"));
							properties.put("Precursor InChIKey", substrate.getProperty("InChIKey"));
							properties.put("Precursor ALogP", substrate.getProperty("ALogP"));
							properties.put("Precursor Major Isotope Mass", substrate.getProperty("Major Isotope Mass"));
						}

						hash_ac.setProperties(properties);
						acontainers.addAtomContainer(hash_ac);
					}
				}
			}
		}

		return acontainers;
	}

	public IAtomContainerSet extractProductsFromBiotransformationsWithTransformationData(ArrayList<Biotransformation> biotransformations, boolean annotate) throws Exception {
		return this.extractProductsFromBiotransformationsWithTransformationData(biotransformations, this.reactionsHash, annotate);
	}

	public void saveBioTransformationProductsToSdf(ArrayList<Biotransformation> biotransformations, String outputFileName) throws Exception {
		this.saveBioTransformationProductsToSdf(biotransformations, outputFileName, this.reactionsHash, false);
	}

	public void saveBioTransformationProductsToSdf(ArrayList<Biotransformation> biotransformations, String outputFileName, boolean annotate) throws Exception {
		this.saveBioTransformationProductsToSdf(biotransformations, outputFileName, this.reactionsHash, annotate);
	}

	public void saveBioTransformationProductsToSdf(ArrayList<Biotransformation> biotransformations, String outputFileName, LinkedHashMap<String, MetabolicReaction> customReactionHash) throws Exception {
		this.saveBioTransformationProductsToSdf(biotransformations, outputFileName, customReactionHash, false);
	}

	public void saveBioTransformationProductsToSdf(ArrayList<Biotransformation> biotransformations, String outputFileName, LinkedHashMap<String, MetabolicReaction> customReactionHash, boolean annotate) throws Exception {
		IAtomContainerSet uniqueSetOfProducts = this.extractProductsFromBiotransformationsWithTransformationData(biotransformations, customReactionHash, annotate);
		SDFWriter sdfWriter = new SDFWriter(new FileOutputStream(outputFileName));
		sdfWriter.write(uniqueSetOfProducts);
		sdfWriter.close();
	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformationChain(IAtomContainer target, String pathwayName, boolean preprocess, boolean filter, int nr_of_steps) throws Exception {
		return this.applyPathwaySpecificBiotransformationsChain(target, pathwayName, preprocess, filter, nr_of_steps, (double)0.0F);
	}

	public void saveBioTransformationProductsToCSV(ArrayList<Biotransformation> biotransformations, String outputFileName) throws Exception {
		this.saveBioTransformationProductsToCSV(biotransformations, outputFileName, this.reactionsHash, false);
	}

	public void saveBioTransformationProductsToCSV(ArrayList<Biotransformation> biotransformations, String outputFileName, boolean annotate) throws Exception {
		this.saveBioTransformationProductsToCSV(biotransformations, outputFileName, this.reactionsHash, annotate);
	}

	public void saveBioTransformationProductsToCSV(ArrayList<Biotransformation> biotransformations, String outputFileName, LinkedHashMap<String, MetabolicReaction> customReactionHash) throws Exception {
		this.saveBioTransformationProductsToSdf(biotransformations, outputFileName, customReactionHash, false);
	}

	public void saveBioTransformationProductsToCSV(ArrayList<Biotransformation> biotransformations, String outputFileName, LinkedHashMap<String, MetabolicReaction> customReactionHash, boolean annotate) throws Exception {
		try {
			IAtomContainerSet products = this.extractProductsFromBiotransformationsWithTransformationData(biotransformations, customReactionHash, annotate);
			FileUtilities.saveAtomContainerSetToCSV(products, outputFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void saveBioTransformationProductsToSDF(ArrayList<Biotransformation> biotransformations, String outputFileName, LinkedHashMap<String, MetabolicReaction> customReactionHash, boolean annotate) throws Exception {
		try {
			IAtomContainerSet products = this.extractProductsFromBiotransformationsWithTransformationData(biotransformations, customReactionHash, annotate);
			FileUtilities.saveAtomContainerSetToSDF(products, outputFileName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformations(IAtomContainer target, String pathwayName, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		return this.applyPathwaySpecificBiotransformations(target, pathwayName, true, preprocess, filter, scoreThreshold);
	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformations(IAtomContainer target, String pathwayName, boolean predictESSpecificity, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		if (ChemStructureExplorer.isBioTransformerValid(target)) {
			for(Enzyme e : this.bSystem.getMetPathwaysHash().get(pathwayName)) {
				biotransformations.addAll(this.metabolizeWithEnzyme(target, e, predictESSpecificity, preprocess, filter, scoreThreshold));
			}
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformations(IAtomContainerSet targets, String pathwayName, boolean preprocess, boolean filter, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		IAtomContainerSet readyTargets = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);

		for(IAtomContainer atc : targets.atomContainers()) {
			IAtomContainer a = atc.clone();
			if (preprocess) {
				a = ChemStructureManipulator.preprocessContainer(a);
				AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(a);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(a);
				readyTargets.addAtomContainer(a);
			} else {
				AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(a);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(a);
				readyTargets.addAtomContainer(a);
			}
		}

		for(IAtomContainer aa : readyTargets.atomContainers()) {
			biotransformations.addAll(this.applyPathwaySpecificBiotransformations(aa, pathwayName, false, filter, scoreThreshold));
		}

		return Utilities.selectUniqueBiotransformations(biotransformations);
	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformationsChain(IAtomContainerSet targets, String pathwayName, boolean preprocess, boolean filter, int nr_of_steps, Double scoreThreshold) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		IAtomContainerSet containers = targets;
		Iterator<IAtomContainer> var10 = targets.atomContainers().iterator();
		if (!var10.hasNext()) {
			return Utilities.selectUniqueBiotransformations(biotransformations);
		} else {
			var10.next();

			ArrayList<Biotransformation> currentProducts;
			for(int counter = 0; nr_of_steps > counter; containers = this.extractProductsFromBiotransformations(currentProducts)) {
				++counter;
				currentProducts = this.applyPathwaySpecificBiotransformations(containers, pathwayName, preprocess, filter, scoreThreshold);
				if (currentProducts.isEmpty()) {
					break;
				}

				biotransformations.addAll(currentProducts);
				containers.removeAllAtomContainers();
			}

			return biotransformations;
		}
	}

	public ArrayList<Biotransformation> applyPathwaySpecificBiotransformationsChain(IAtomContainer target, String pathwayName, boolean preprocess, boolean filter, int nr_of_steps, Double scoreThreshold) throws Exception {
		new ArrayList<>();
		IAtomContainerSet targets = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		targets.addAtomContainer(target);
		ArrayList<Biotransformation> biotransformations = this.applyPathwaySpecificBiotransformationsChain(targets, pathwayName, preprocess, filter, nr_of_steps, scoreThreshold);
		return Utilities.selectUniqueBiotransformations(biotransformations);
	}

	public boolean isValidMetabolte(IAtomContainer oneMole) throws Exception {
		if (this.invalidSMARTS.containInvalidSubstructure(oneMole)) {
			return false;
		} else {
			int countAtom_nonH = 0;
			int countCarbon = 0;

			for(int t = 0; t < oneMole.getAtomCount(); ++t) {
				if (!oneMole.getAtom(t).getSymbol().equalsIgnoreCase("H")) {
					++countAtom_nonH;
				}

				if (oneMole.getAtom(t).getSymbol().equalsIgnoreCase("C")) {
					++countCarbon;
				}
			}

			if (countAtom_nonH >= 1 && countCarbon >= 1) {
				return true;
			} else {
				return false;
			}
		}
	}

	public ArrayList<Biotransformation> processPolymer(IAtomContainerSet molecules) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();

		for(int i = 0; i < molecules.getAtomContainerCount(); ++i) {
			IAtomContainer startingCompound = molecules.getAtomContainer(i).clone();
			if (this.hp.isPolymer(startingCompound)) {
				ArrayList<Biotransformation> monomers_biotransformation = this.hp.converPolymerToMonomer_biotransformation(startingCompound);
				biotransformations.addAll(monomers_biotransformation);
			}
		}

		return biotransformations;
	}

	public ArrayList<Biotransformation> processPolymer_oneMole(IAtomContainer molecule) throws Exception {
		ArrayList<Biotransformation> biotransformations = new ArrayList<>();
		if (this.hp.isPolymer(molecule)) {
			ArrayList<Biotransformation> monomers_biotransformation = this.hp.converPolymerToMonomer_biotransformation(molecule);
			biotransformations.addAll(monomers_biotransformation);
		}

		return biotransformations;
	}

	public boolean containsK(IAtomContainer ac) throws Exception {
		for(int t = 0; t < ac.getAtomCount(); ++t) {
			if (ac.getAtom(t).getSymbol().equalsIgnoreCase("K")) {
				return true;
			}
		}

		return false;
	}

	public ArrayList<Biotransformation> convertMLProductToBioTransformation(IAtomContainerSet substrates, IAtomContainerSet cyProductMolecules) throws Exception {
		IAtomContainerSet cleanedCyProductMolecules = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
		ArrayList<String> checkedInChIKeyList = new ArrayList<>();

		for(int i = 0; i < cyProductMolecules.getAtomContainerCount(); ++i) {
			IAtomContainer oneMetabolite = cyProductMolecules.getAtomContainer(i);
			if (!checkedInChIKeyList.contains((String) oneMetabolite.getProperty("InChIKey"))) {
				checkedInChIKeyList.add(oneMetabolite.getProperty("InChIKey"));
				cleanedCyProductMolecules.addAtomContainer(oneMetabolite);
			}
		}

		ArrayList<Biotransformation> convertedBioTrans = new ArrayList<>();
		HashMap<String, IAtomContainerSet> reactionEnzyme_Metabolite_map = new HashMap<>();

		for(int i = 0; i < cleanedCyProductMolecules.getAtomContainerCount(); ++i) {
			IAtomContainer oneMolecule = cyProductMolecules.getAtomContainer(i);
			String reactionType = oneMolecule.getProperty("ReactionType");
			String enzymeNameString = oneMolecule.getProperty("Enzyme");
			String key = reactionType + ";" + enzymeNameString;
			if (!reactionEnzyme_Metabolite_map.containsKey(key)) {
				IAtomContainerSet notIncludedMolecules = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);
				notIncludedMolecules.addAtomContainer(oneMolecule);
				reactionEnzyme_Metabolite_map.put(key, notIncludedMolecules);
			} else {
				IAtomContainerSet notIncludedMolecules = reactionEnzyme_Metabolite_map.get(key);
				notIncludedMolecules.addAtomContainer(oneMolecule);
				reactionEnzyme_Metabolite_map.put(key, notIncludedMolecules);
			}
		}

		for(String reactionEnzyme : reactionEnzyme_Metabolite_map.keySet()) {
			IAtomContainerSet notIncludedMolecules = reactionEnzyme_Metabolite_map.get(reactionEnzyme);
			String reactionType = reactionEnzyme.split(";")[0];
			String enzymeNameString = reactionEnzyme.split(";")[1];
			String[] enzymeArray = enzymeNameString.split(" ");

            ArrayList<String> enzymeNames = new ArrayList<>(Arrays.asList(enzymeArray));

			BioSystem.BioSystemName bioSys = BioSystemName.HUMAN;
			Biotransformation bioTrans = new Biotransformation(substrates, reactionType, enzymeNames, notIncludedMolecules, bioSys);
			convertedBioTrans.add(bioTrans);
		}

		return convertedBioTrans;
	}

	public IAtomContainerSet notIncludedMolecules(IAtomContainerSet mlPredictedResults, IAtomContainerSet ruleBasedResults) throws Exception {
		IAtomContainerSet notIncludedMolecuels = (IAtomContainerSet)DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainerSet.class, new Object[0]);

		for(int i = 0; i < mlPredictedResults.getAtomContainerCount(); ++i) {
			if (!ChemStructureExplorer.atomContainerInclusionHolds(ruleBasedResults, mlPredictedResults.getAtomContainer(i))) {
				notIncludedMolecuels.addAtomContainer(mlPredictedResults.getAtomContainer(i));
			}
		}

		return notIncludedMolecuels;
	}
}
