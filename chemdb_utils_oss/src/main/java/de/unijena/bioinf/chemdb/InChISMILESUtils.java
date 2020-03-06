package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import net.sf.jniinchi.*;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InChISMILESUtils extends InChIs {

    public static InChI getStandardInchi(InChI inChI) {
        String inchi = inChI.in3D;
        String sinchi = getStandardInchi(inChI.in3D);
        if (inchi.equals(sinchi)) return inChI;
        String newKey = inchi2inchiKey(sinchi);
        return newInChI(newKey, sinchi);
    }

    private static String getStandardInchi(String inChI){
        if (isStandardInchi(inChI)){
            return inChI;
        } else {
            return getStdInchi(inChI);
        }
    }

    public static String inchi2inchiKey(String inchi) {

        //todo isotopes in inchiKey14 bug removed with latest version 2.2?
        try {
            if (inchi==null) throw new NullPointerException("Given InChI is null");
            if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
            JniInchiOutputKey key = JniInchiWrapper.getInchiKey(inchi);
            if(key.getReturnStatus() == INCHI_KEY.OK) {
                return key.getKey();
            } else {
                throw new RuntimeException("Error while creating InChIKey: " + key.getReturnStatus());
            }
        } catch (JniInchiException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getStdInchi(String inchi) {
        try {
            if (inchi==null) throw new NullPointerException("Given InChI is null");
            if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");

            JniInchiInputInchi inputInchi = new JniInchiInputInchi(inchi);
            JniInchiStructure structure = JniInchiWrapper.getStructureFromInchi(inputInchi);
            JniInchiInput input = new JniInchiInput(structure);
            JniInchiOutput output = JniInchiWrapper.getStdInchi(input);
            if(output.getReturnStatus() == INCHI_RET.WARNING) {
                LOG.warn("Warning issued while computing standard InChI: "+output.getMessage());
                return output.getInchi();
            } else if(output.getReturnStatus() == INCHI_RET.OKAY) {
                return output.getInchi();
            } else {
                throw new RuntimeException("Error while computing standard InChI: " + output.getReturnStatus()
                        +"\nError message: "+output.getMessage());
            }
        } catch (JniInchiException e) {
            throw new RuntimeException(e);
        }
    }

    public static InChI getInchi(IAtomContainer atomContainer) throws CDKException {
        //todo does getInChIGenerator need any specific options!?!?!?
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer);

        String inchi = inChIGenerator.getInchi();
        if (inchi==null) return null;
        String key = inChIGenerator.getInchiKey();
        return newInChI(key, inchi);
    }

    public static String get2DSmiles(IAtomContainer atomContainer) throws CDKException {
        return  SmilesGenerator.unique().create(atomContainer); //Unique - canonical SMILES string, different atom ordering produces the same* SMILES. No isotope or stereochemistry encoded.
    }

    public static String getSmiles(IAtomContainer atomContainer) throws CDKException {
        return  SmilesGenerator.unique().create(atomContainer); //Absolute - canonical SMILES string, different atom ordering produces the same SMILES. Isotope and stereochemistry is encoded.
    }

    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static IAtomContainer getAtomContainer(String someStructureFormat) throws CDKException, IOException {
        if (isInchi(someStructureFormat)) {
            return InChIGeneratorFactory.getInstance().getInChIToStructure(someStructureFormat, DefaultChemObjectBuilder.getInstance()).getAtomContainer();
        } else if (someStructureFormat.contains("\n")) {
            //it is a structure format from some file
            ReaderFactory readerFactory = new ReaderFactory();
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
            ISimpleChemObjectReader reader = readerFactory.createReader(in);
//            MDLV2000Reader reader = new MDLV2000Reader();
//            reader.setReader(in);
            if (reader==null) {
                in.close();
                //try with another new-line
                someStructureFormat += "\n";
                in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
                reader = readerFactory.createReader(in);
            }
            if (reader==null) {
                in.close();
                throw new IOException("No reader found for given format");
            }else if (reader.accepts(ChemFile.class)) {
                ChemFile cfile = new ChemFile();
                cfile = reader.read(cfile);
                List<IAtomContainer> atomContainerList = ChemFileManipulator.getAllAtomContainers(cfile);

                if (atomContainerList.size()>1){
                    throw new IOException("Multiple structures in input");
                } else if (atomContainerList.size()==0){
                    throw new IOException("Could not parse any structure");
                }
                return atomContainerList.get(0);

            } else {
                throw new IOException("Unknown format");
            }
        } else {
            //assume SMILES
            //todo do we need to do any processing?!?
            SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            IAtomContainer iAtomContainer = smilesParser.parseSmiles(someStructureFormat);
            return iAtomContainer;
        }
    }

    public static IAtomContainer getAtomContainer(Smiles smiles) throws CDKException {
        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer iAtomContainer = smilesParser.parseSmiles(smiles.smiles);
        return iAtomContainer;
    }

    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static InChI getInchiAndInchiKey(String someStructureFormat) throws CDKException, IOException {
        if (isInchi(someStructureFormat)) {
            if (!isStandardInchi(someStructureFormat)) {
                someStructureFormat = getStdInchi(someStructureFormat);
            }
            String key = inchi2inchiKey(someStructureFormat);
            return newInChI(key, someStructureFormat);
        } else {
            return getInchi(getAtomContainer(someStructureFormat));
        }
    }

    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static String get2DSmiles(String someStructureFormat) throws CDKException, IOException {
        return get2DSmiles(getAtomContainer(someStructureFormat));
    }

    public static String get2DSmiles(Smiles smiles) throws CDKException {
        return get2DSmiles(getAtomContainer(smiles));
    }


    /**
     * using CDK to create a canonical SMILES without stereo info throw several InvalidSmiles exceptions
     * @param smiles
     * @return
     */
    public static String get2DSmilesByTextReplace(Smiles smiles) {
        return stripDoubleBondGeometry(stripStereoCentres(smiles.smiles));
    }


    /**
     * Regex to match any sterecentre designation including simple @ / @@ forms
     * along with @TH @OH etc with subsquent IDs
     */
    private static Pattern ALL_STEREOCENTRE_INCL_LABEL_PATTERN =
            Pattern.compile("@+(?:(?:TH|AL|SP|TB|OH)\\d*)?");

    /**
     * @param smi
     *            the SMILES String
     * @return The SMILES String with all stereocentre labels removed
     */
    public static String stripStereoCentres(String smi) {
        return ALL_STEREOCENTRE_INCL_LABEL_PATTERN.matcher(smi).replaceAll("");

    }

    /**
     * @param smi
     *            the SMILES String
     * @return The SMILES String with all double bonde geometry labels removed
     */
    public static String stripDoubleBondGeometry(String smi) {
        return smi.replace("\\", "-").replace("/", "-");
    }




    public static MolecularFormula formulaFromSmiles(String smiles) throws InvalidSmilesException, UnknownElementException {
        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer iAtomContainer = smilesParser.parseSmiles(smiles);
        if (iAtomContainer==null) return null;
        String s = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(iAtomContainer));
        if (s==null) return null;
        int formalCharge = getFormalChargeFromSmiles(smiles);
        MolecularFormula formula = MolecularFormula.parse(s);
        if (formalCharge==0) return formula;
        else if (formalCharge<0){
            return formula.add(MolecularFormula.parse(String.valueOf(Math.abs(formalCharge)+"H")));
        } else {
            return formula.subtract(MolecularFormula.parse(String.valueOf(formalCharge+"H")));
        }
    }



    public static int numberOfPartialChargesFromSmiles(String smiles) {
        int count = 0;
        for (int i = 0; i < smiles.length(); i++) {
            char c = smiles.charAt(i);
            if (c=='-' || c=='+') {
                ++count;
            }
        }
        return count;
    }

public static void main(String... args) throws CDKException, IOException {
    //todo remove after testing

//    System.out.println(formulaFromSmiles("CCCCCCCCCCCCCCCCCC(=O)OC[C@H](COP(=O)(O)OCC[N+](C)(C)C)O").formatByHill());
//    System.out.println(InChIs.newInChI(null,"InChI=1S/C26H55NO7P/c1-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-26(29)32-23-25(28)24-34-35(30,31)33-22-21-27(2,3)4/h25,28H,5-24H2,1-4H3,(H,30,31)/t25-/m1/s1").extractFormula().formatByHill());
//    System.out.println(inchi2inchiKey("InChI=1S/C26H55NO7P/c1-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-26(29)32-23-25(28)24-34-35(30,31)33-22-21-27(2,3)4/h25,28H,5-24H2,1-4H3,(H,30,31)/t25-/m1/s1"));
//    InChI i = getInchiAndInchiKey("CCCCCCCCCCCCCCCCCC(=O)OC[C@H](COP(=O)(O)OCC[N+](C)(C)C)O");
//    System.out.println(i.in3D);
//    System.out.println(i.extractFormula().formatByHill());
//    System.out.println(i.key);

    String s = "C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O";
    s = stripStereoCentres(s);
    s = stripDoubleBondGeometry(s);
    Smiles smiles = new Smiles(s);
    System.out.println(get2DSmiles(smiles));
    System.out.println(get2DSmilesByTextReplace(new Smiles("C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O")));
}


    private static final Pattern SMILES_CHARGE = Pattern.compile("([-+])(\\d?)");
    public static int getFormalChargeFromSmiles(String smiles) {
        int abscharge = 0;
        Matcher m = SMILES_CHARGE.matcher(smiles);
        while (m.find()){
            String c = m.group(1);
            int currentCharge;
            if (m.group(2).length()>0){
                currentCharge = Integer.parseInt(m.group(2));
            } else {
                currentCharge = 1;
            }
            if (c.equals("-")){
                currentCharge = -currentCharge;
            } else if (!c.equals("+")){
                throw new RuntimeException("unexpected charge");
            }
            abscharge += currentCharge;
        }
        return abscharge;
    }
}
