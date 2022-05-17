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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.storage.blob.AbstractCompressible;
import de.unijena.bioinf.storage.blob.BlobStorage;
import de.unijena.bioinf.storage.blob.BlobStorages;
import de.unijena.bioinf.storage.blob.Compressible;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChemicalBlobDatabase<Storage extends BlobStorage> extends AbstractCompressible implements AbstractChemicalDatabase {
public enum Format {
    CSV(".csv"), JSON(".json");
    public final String ext;

    Format(@NotNull String ext) {
        this.ext = ext;
    }

    public String ext() {
        return ext;
    }

    public static @Nullable Format fromPath(@NotNull Path p) {
        return fromName(p.toString());
    }

    public static @Nullable Format fromName(@NotNull String s) {
        s = s.toLowerCase();
        if (s.endsWith(CSV.ext()))
            return CSV;
        if (s.endsWith(JSON.ext()))
            return JSON;
        return null;
    }
}

    public static final String TAG_FORMAT = "chemdb-format";
//    public static final String TAG_COMPRESSION = "chemdb-compression";
    public static final String TAG_DATE = "chemdb-date";
    public static final String TAG_FLAVOR = "chemdb-flavor";

    public static final String BLOB_SETTINGS = "SETTINGS";
    public static final String BLOB_FORMULAS = "formulas";

    public static final Set<String> CONFIG_BLOBS = Set.of(BLOB_FORMULAS, BLOB_SETTINGS);

    protected final Storage storage;
    protected Format format; // csv or json
    protected CompoundReader reader;
    protected MolecularFormula[] formulas;
    protected final TObjectLongMap<MolecularFormula> formulaFlags = new TObjectLongHashMap<>();
    protected FingerprintVersion version;



    public ChemicalBlobDatabase(Storage storage) throws IOException {
        this(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), storage);
    }

    public ChemicalBlobDatabase(FingerprintVersion version, Storage storage) throws IOException {
        super(null);
        this.storage = storage;
        this.version = version;
        setDecompressStreams(true);
        init();
    }

    public Storage getBlobStorage(){
        return storage;
    }

    public String getName() {
        return storage.getName();
    }

    @Override
    public String getChemDbDate() throws ChemicalDatabaseException {
        try {
            return storage.getTag(TAG_DATE);
        } catch (IOException e) {
            throw new ChemicalDatabaseException("Error when requesting ChemDbDate via Storage Tag '" + TAG_DATE +"'." ,e);
        }
    }

    protected void init() throws IOException {
        Map<String, String> tags = storage.getTags();

        if (tags.containsKey(TAG_COMPRESSION) && tags.containsKey(TAG_FORMAT)) {
            format = Optional.ofNullable(tags.get(TAG_FORMAT)).map(String::toUpperCase).map(Format::valueOf)
                    .orElseThrow(() -> new IOException("Could not determine database file format."));


            compression = Optional.ofNullable(tags.get(TAG_COMPRESSION)).map(String::toUpperCase).map(Compression::valueOf)
                    .orElseGet(() -> {
                        LoggerFactory.getLogger(getClass()).warn("Could not determine compressions type. Assuming uncompressed data!");
                        return Compression.NONE;
                    });
        } else { // guess format and compression from file endings
            Iterator<BlobStorage.Blob> it = storage.listBlobs();
            while (it.hasNext()) {
                BlobStorage.Blob blob = it.next();
                String fname = blob.getFileName();
                if (!blob.isDirectory() && !fname.toUpperCase().startsWith(BLOB_SETTINGS)) {
                    compression = Compression.fromName(fname);
                    format = Format.fromName(fname.substring(0, fname.length() - compression.ext().length()));
                    break;
                }
            }
            if (compression == null) {
                compression = Compression.GZIP;
                LoggerFactory.getLogger(getClass()).warn("Could not determine Compression of storage '" + storage.getName() + "'. Using default compression '" + compression.ext + "'.");
            }
            if (format == null) {
                format = Format.JSON;
                LoggerFactory.getLogger(getClass()).warn("Could not determine Format of storage '" + storage.getName() + "'. Using default format '" + format.ext + "'.");
            }
        }

        this.reader = format == Format.CSV ? new CSVReader() : new JSONReader();

        @NotNull Optional<Reader> optReader = getReader(BLOB_FORMULAS);
        if (optReader.isPresent()) {
            try (Reader r = optReader.get()) {
                final Map<String, String> map = new ObjectMapper().readValue(r, new TypeReference<>() {
                });

                this.formulas = new MolecularFormula[map.size()];
                final AtomicInteger i = new AtomicInteger(0);
                formulaFlags.clear();
                map.entrySet().stream().parallel().forEach(e -> {
                    final MolecularFormula mf = MolecularFormula.parseOrThrow(e.getKey());
                    final long flag = Long.parseLong(e.getValue());
                    this.formulas[i.getAndIncrement()] = mf;
                    synchronized (formulaFlags) {
                        this.formulaFlags.put(mf, flag);
                    }
                });
                Arrays.sort(this.formulas);
            }
        } else {
            LoggerFactory.getLogger(getClass()).debug("No formula index found! Loading molecular formulas by iterating over all blobs in storage. Might be slow...");
            List<MolecularFormula> formulaList = new ArrayList<>();
            storage.listBlobs().forEachRemaining(blob -> {
                String fname = blob.getFileName();
                if (!CONFIG_BLOBS.contains(fname)) {
                    formulaList.add(MolecularFormula.parseOrThrow(fname.substring(0, fname.length() - format.ext().length() - compression.ext().length())));
                }
            });
            Collections.sort(formulaList);
            formulas = formulaList.toArray(MolecularFormula[]::new);
        }
    }

    @NotNull
    public Optional<Reader> getCompoundReader(@NotNull MolecularFormula formula) throws IOException {
        return getReader(formula.toString());
    }

    @NotNull
    public Optional<Reader> getReader(@NotNull String name) throws IOException {
        return getStream(name).map(inputStream -> new InputStreamReader(inputStream, storage.getCharset()));
    }

    @NotNull
    public Optional<InputStream> getCompoundStream(@NotNull MolecularFormula formula) throws IOException {
        return getStream(formula.toString());
    }


        /**
         * Returns stream for the requested filename and handles decompression if needed
         *
         * @param name resource name
         * @return Stream of the resource
         * @throws IOException if IO goes wrong
         */
    @NotNull
    public Optional<InputStream> getStream(@NotNull String name) throws IOException {
        return Compressible.decompressRawStream(storage.reader(Path.of(name + format.ext() + getCompression().ext())), getCompression(), isDecompressStreams());
    }

    /**
     * Returns stream for the filename without handling decompression
     *
     * @param name resource name
     * @return Optional of Stream of the resource
     */
    @NotNull
    public Optional<InputStream> getRawStream(@NotNull String name) throws IOException{
        return Optional.ofNullable(storage.reader(Path.of(name + format.ext() + getCompression().ext())));
    }


    public boolean containsFormula(MolecularFormula formula) {
        return ChemDBs.containsFormula(formulas, formula);
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(final double ionMass, Deviation
            deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {

        final double mass = ionType.precursorMassToNeutralMass(ionMass);

        final int searchP = Arrays.binarySearch(formulas, mass - deviation.absoluteFor(ionMass), (o1, o2) -> {
            double mzL = (o1 instanceof MolecularFormula ? ((MolecularFormula) o1).getMass() : (Double) o1);
            double mzR = (o2 instanceof MolecularFormula ? ((MolecularFormula) o2).getMass() : (Double) o2);
            return Double.compare(mzL, mzR);
        });

        int insertionPoint;
        if (searchP >= 0) {
            insertionPoint = searchP;
        } else {
            insertionPoint = -searchP - 1;
        }

        final double max = mass + deviation.absoluteFor(ionMass);
        ArrayList<FormulaCandidate> candidates = new ArrayList<>();
        while (insertionPoint < formulas.length && formulas[insertionPoint].getMass() <= max) {
            final MolecularFormula f = formulas[insertionPoint++];
            candidates.add(new FormulaCandidate(f, ionType, formulaFlags.get(f)));
        }

        return candidates;
    }


    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        final ArrayList<CompoundCandidate> candidates = new ArrayList<>();

        try (final InputStream blobReader = getCompoundStream(formula).orElse(null)) {
            if (blobReader != null) {
                try (final CloseableIterator<CompoundCandidate> iter = reader.readCompounds(blobReader)) {
                    iter.forEachRemaining(candidates::add);
                }
            }
            return candidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try (final InputStream blobReader = getCompoundStream(formula).orElse(null)) {
            if (blobReader != null) {
                try (final CloseableIterator<FingerprintCandidate> iter = reader.readFingerprints(version, blobReader)) {
                    iter.forEachRemaining(fingerprintCandidates::add);
                }
            }
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        final HashMap<String, CompoundCandidate> innerMap = new HashMap<>();
        final Multimap<MolecularFormula, CompoundCandidate> formulas2Candidates = ArrayListMultimap.create();
        for (CompoundCandidate c : compounds) {
            final MolecularFormula f = c.getInchi().extractFormulaOrThrow();
            formulas2Candidates.put(f, c);
            innerMap.put(c.getInchiKey2D(), c);
        }

        for (Map.Entry<MolecularFormula, Collection<CompoundCandidate>> entry : formulas2Candidates.asMap().entrySet()) {
            final MolecularFormula f = entry.getKey();
            final Collection<FingerprintCandidate> pseudoQueue = new AbstractCollection<>() {

                @Override
                public boolean add(FingerprintCandidate fingerprintCandidate) {
                    final CompoundCandidate c = innerMap.get(fingerprintCandidate.getInchiKey2D());
                    if (c != null) {
                        candidates.add(new FingerprintCandidate(c, fingerprintCandidate.fingerprint));
                    }
                    return true;
                }

                @Override
                public Iterator<FingerprintCandidate> iterator() {
                    return null;
                }

                @Override
                public int size() {
                    return 0;
                }
            };
            lookupStructuresAndFingerprintsByFormula(f, pseudoQueue);
        }
        return candidates;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        // compounds are already annotated
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }


    public static ChemicalBlobDatabase<?> defaultChemDB() throws IOException {
       return new ChemicalBlobDatabase<>(BlobStorages.openDefault(FingerIDProperties.chemDBStorePropertyPrefix(),FingerIDProperties.defaultChemDBBucket()));
    }
}