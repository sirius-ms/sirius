package de.unijena.bioinf.ChemistryBase.chem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


class Static {
	
	// Elements
	
	static String[] ELEMENT_SYMBOL;
	static String[] ELEMENT_NAME;
	static double[] ELEMENT_MASS;
	static double[] ELEMENT_AVERAGE_MASS;
	static int[] ELEMENT_INT_MASS;
	static int[] ELEMENT_VALENCY;
	static double[][] ELEMENT_ISOTOPE_MASSES;
	static int[][] ELEMENT_ISOTOPE_INT_MASSES;
	//static double[][] ELEMENT_ISOTOPE_ABUNDANCIES;
	
	// Molecule Labels
	
	static String[] MOLECULE_NAME;
	static String[] MOLECULE_FORMULA;
	
	static {
		try {
			
			// read elements
			
			InputStream in = Static.class.getResourceAsStream("/elements.json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuffer content = new StringBuffer();
			String line;
			while ((line = reader.readLine())!= null) {
				content.append(line+"\n");
			}
			reader.close();
			
			JSONObject elements = new JSONObject(content.toString());
			ELEMENT_SYMBOL = JSONObject.getNames(elements);
			
			Arrays.sort(ELEMENT_SYMBOL);
			
			ELEMENT_NAME = new String[ELEMENT_SYMBOL.length];
			ELEMENT_MASS = new double[ELEMENT_SYMBOL.length];
			ELEMENT_AVERAGE_MASS = new double[ELEMENT_SYMBOL.length];
			ELEMENT_INT_MASS = new int[ELEMENT_SYMBOL.length];
			ELEMENT_VALENCY = new int[ELEMENT_SYMBOL.length];
			ELEMENT_ISOTOPE_MASSES = new double[ELEMENT_SYMBOL.length][];
			ELEMENT_ISOTOPE_INT_MASSES = new int[ELEMENT_SYMBOL.length][];
			//ELEMENT_ISOTOPE_ABUNDANCIES = new double[ELEMENT_SYMBOL.length][];
			
			for (int i = 0; i < ELEMENT_SYMBOL.length; i++) {
				JSONObject element = elements.getJSONObject(ELEMENT_SYMBOL[i]);
				ELEMENT_NAME[i] = element.getString("name");
				ELEMENT_AVERAGE_MASS[i] = element.getDouble("average");
				ELEMENT_VALENCY[i] = element.getInt("valence");
				
				JSONObject isotopes = element.getJSONObject("isotopes");
				ELEMENT_ISOTOPE_MASSES[i] = toDoubleArray(isotopes.getJSONArray("mass"));
				//ELEMENT_ISOTOPE_ABUNDANCIES[i] = toDoubleArray(isotopes.getJSONArray("abundance"));
				ELEMENT_ISOTOPE_INT_MASSES[i] = toIntArray(isotopes.getJSONArray("intmass"));
				ELEMENT_MASS[i] = ELEMENT_ISOTOPE_MASSES[i][0];
				ELEMENT_INT_MASS[i] = ELEMENT_ISOTOPE_INT_MASSES[i][0];
			}
						
			// read molecule labels
			
			in = Static.class.getResourceAsStream("/molecules.txt");
			reader = new BufferedReader(new InputStreamReader(in));
			content = new StringBuffer();
			while ((line = reader.readLine())!= null) {
				content.append(line+"\n");
			}
			reader.close();
			
			JSONObject molecules = new JSONObject(content.toString());
			MOLECULE_NAME = JSONObject.getNames(molecules);
			
			Arrays.sort(MOLECULE_NAME);
			
			MOLECULE_FORMULA = new String[MOLECULE_NAME.length];
			for (int i = 0; i < MOLECULE_NAME.length; i++) {
				MOLECULE_FORMULA[i] = molecules.getString(MOLECULE_NAME[i]);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// Labels
	
	static final int HYDROGEN_ID = Arrays.binarySearch(ELEMENT_SYMBOL, "H");
	static final int CARBON_ID = Arrays.binarySearch(ELEMENT_SYMBOL, "C");
	
	static final double ELECTRON_MASS = 0.00054857990946;
	
	// Ions
	
    static final Ionization PROTON_ION = new Adduct(1.007276466, 1, "[M+H+]+");
    //static final Ionization HYDROGEN_ION = new Adduct(ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "H")], 1, "[M+H]+");
    static final Ionization PROTON_NEG_ION = new Adduct(-1.007276466, -1, "[M-H+]-");
    /*
    static final Ionization HYDROGEN_NEG_ION = new Adduct(-ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "H")], -1, "[M-H]-");
    static final Ionization NA_ION = new Adduct(ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "Na")], 1, "[M+Na]+");
    static final Ionization H_WITHOUT_WATER = new Adduct(PROTON_ION.getMass() - (2*ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "H")]
          + ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "O")]), 1, "[M+H-H2O]+");
    static final Ionization CL_ION = new Adduct(ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "Cl")], -1, "[M+Cl]-");
    static final Ionization CL_STRANGE_POS = new Adduct(-ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "Cl")], -1, "[M+H-Cl]");
    static final Ionization NH4 = new Adduct(ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "N")] + ELEMENT_MASS[Arrays.binarySearch(ELEMENT_SYMBOL, "H")]*4, 1, "[M+NH4]+");
    static final Ionization NO_ION_NEG = new Adduct(0d, -1, "[M]-");
    static final Ionization NO_ION_POS = new Adduct(0d, 1, "[M]+");
    
    static final Ionization[] IONS = new Ionization[]{
            PROTON_ION, HYDROGEN_ION, PROTON_NEG_ION, HYDROGEN_NEG_ION, NA_ION,
            H_WITHOUT_WATER, CL_ION, CL_STRANGE_POS,NH4, NO_ION_NEG, NO_ION_POS
    };
    */
    static final Ionization[] IONS = new Ionization[]{PROTON_ION,PROTON_NEG_ION};
    static final String[] ION_FORMULAS = new String[]{"[M+H]+", "[M-H]-", "[M+Na]+", "[M+NH4]+","[M]+",
    	"[M]-"};
	
	protected static int[] toIntArray(JSONArray array) throws JSONException {
		int[] arr = new int[array.length()];
		for (int i = 0; i < array.length(); i++) {
			arr[i] = array.getInt(i);
		}
		return arr;
	}
	
	protected static double[] toDoubleArray(JSONArray array) throws JSONException {
		double[] arr = new double[array.length()];
		for (int i = 0; i < array.length(); i++) {
			arr[i] = array.getDouble(i);
		}
		return arr;
	}
		
}
