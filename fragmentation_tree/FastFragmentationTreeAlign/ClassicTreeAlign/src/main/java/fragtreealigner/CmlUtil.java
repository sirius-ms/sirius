
package fragtreealigner;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import fragtreealigner.domainobjects.graphs.FragmentationTree;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class CmlUtil {
	public static void main(String[] args) {
		if (args.length == 0) return;
		if (args[0].equalsIgnoreCase("dotToCml")) dotToCml(args[1]);
	}
	
	public static void dotToCml(String arg) {
		Session session = new Session();
		Parameters params = new Parameters(session);
		session.setParameters(params);

		File fileOrDirectory = new File(arg);
		File[] files;
		if (fileOrDirectory.isDirectory()) files = fileOrDirectory.listFiles();
		else files = new File[]{fileOrDirectory};
		
		String filename;
		FragmentationTree fragTree;
		for (File file : files) {
			filename = file.getAbsolutePath();
			if (!filename.endsWith(".dot")) continue;
			try {
				fragTree = FragmentationTree.readFromDot(FileUtils.ensureBuffering(new FileReader(filename)), session);
				if (fragTree == null){
					System.err.println("Tree empty in file: "+filename);
				} else {
				fragTree.writeToCml(filename.replaceAll(".dot$", "") + ".cml");
				}
			} catch (Exception e) {
				System.err.println("The following error occured while processing the files:\n" + e.getMessage());
			}
		}
	}
}
