package de.unijena.bioinf.ChemistryBase.fp;

import java.io.*;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

public class ClassyFireFingerprintVersion extends FingerprintVersion {

    protected ClassyfireProperty[] properties;

    protected ClassyFireFingerprintVersion(ClassyfireProperty[] classyfireProperties) {
        this.properties = classyfireProperties;
    }

    public static ClassyFireFingerprintVersion loadClassyfire(File csvFile) throws IOException {
        final TreeMap<Integer, ClassyfireProperty> properties = new TreeMap<>();
        InputStream fr = null;
        try {
            fr = new FileInputStream(csvFile);
            if (csvFile.getName().endsWith(".gz")) {
                fr = new GZIPInputStream(fr);
            }
            final BufferedReader br = new BufferedReader(new InputStreamReader(fr));
            String line;
            while ((line=br.readLine())!=null) {
                String[] tbs = line.split("\t", 4);
                final int id = Integer.parseInt(tbs[0]);
                properties.put(id, new ClassyfireProperty(id, tbs[1], tbs[3], Integer.parseInt(tbs[2])));
            }
        } catch (IOException e) {
            if (fr!=null) fr.close();
        }
        for (ClassyfireProperty entry : properties.values()) {
            if (entry.getParentId()>=0) {
                entry.setParent(properties.get(entry.getParentId()));
            }
        }
        return new ClassyFireFingerprintVersion(properties.values().toArray(new ClassyfireProperty[properties.size()]));
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        return fingerprintVersion instanceof ClassyFireFingerprintVersion && ((ClassyFireFingerprintVersion) fingerprintVersion).properties.length == properties.length;
    }
}
