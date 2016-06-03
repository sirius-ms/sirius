/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.babelms.utils.Base64;
import de.unijena.bioinf.fingerid.FingerprintStatistics;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParser;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

public class WebAPI implements Closeable {

    protected final static boolean DEBUG = false;

    private final CloseableHttpClient client;

    public WebAPI() {
        client = HttpClients.createDefault();
    }

    public static void SHUT_UP_STUPID_LOGGING() {
        java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
        java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");
    }

    static {
        SHUT_UP_STUPID_LOGGING();
    }

    protected static URIBuilder getFingerIdURI(String path) {
        URIBuilder b = new URIBuilder().setScheme("http").setHost(DEBUG ? "localhost" : "www.csi-fingerid.org");
        if (DEBUG) b = b.setPort(8080).setPath("/csi_fingerid_frontend" + path);
        else b.setPath(path);
        return b;
    }

    public boolean updateJobStatus(FingerIdJob job) throws URISyntaxException, IOException {
        final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
        try (CloseableHttpResponse response = client.execute(get)) {
            try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                final JsonObject obj = json.readObject();
                if (obj.containsKey("prediction")) {
                    final byte[] bytes = Base64.decode(obj.getString("prediction"));
                    final TDoubleArrayList platts = new TDoubleArrayList(2000);
                    final ByteBuffer buf = ByteBuffer.wrap(bytes);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    while (buf.position() < buf.limit()) {
                        platts.add(buf.getDouble());
                    }
                    job.prediction = platts.toArray();
                    return true;
                } else {
                    job.state = obj.containsKey("state") ? obj.getString("state") : "SUBMITTED";
                }
            }
        }
        return false;
    }

    public FingerIdJob submitJob(final Ms2Experiment experiment, final FTree ftree) throws IOException, URISyntaxException {
        final HttpPost post = new HttpPost(getFingerIdURI("/webapi/predict.json").build());
        final String stringMs, jsonTree;
        {
            final JenaMsWriter writer = new JenaMsWriter();
            final StringWriter sw = new StringWriter();
            try (final BufferedWriter bw = new BufferedWriter(sw)) {
                writer.write(bw, experiment);
            }
            stringMs = sw.toString();
        }
        {
            final FTJsonWriter writer = new FTJsonWriter();
            final StringWriter sw = new StringWriter();
            writer.writeTree(new BufferedWriter(sw), ftree);
            jsonTree = sw.toString();
        }

        final NameValuePair ms = new BasicNameValuePair("ms", stringMs);
        final NameValuePair tree = new BasicNameValuePair("ft", jsonTree);

        final UrlEncodedFormEntity params = new UrlEncodedFormEntity(Arrays.asList(ms, tree));
        post.setEntity(params);

        final String securityToken;
        final long jobId;
        // SUBMIT JOB
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode()==200) {
                try (final JsonReader json = Json.createReader(new BufferedReader(new InputStreamReader(response.getEntity().getContent(), ContentType.getOrDefault(response.getEntity()).getCharset())))) {
                    final JsonObject obj = json.readObject();
                    securityToken = obj.getString("securityToken");
                    jobId =obj.getInt("jobId");
                    return new FingerIdJob(jobId, securityToken);
                }
            } else {
                throw new RuntimeException(response.getStatusLine().getReasonPhrase());
            }
        }
    }

    public Future<double[]> predictFingerprint(ExecutorService service, final Ms2Experiment experiment, final FTree tree) {
        return service.submit(new Callable<double[]>() {
            @Override
            public double[] call() throws Exception {
                final FingerIdJob job = submitJob(experiment, tree);
                // RECEIVE RESULTS
                final HttpGet get = new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId)).setParameter("securityToken", job.securityToken).build());
                for (int k=0; k < 60; ++k) {
                    Thread.sleep(3000);
                    if (updateJobStatus(job)) {
                        return job.prediction;
                    } else if (job.state=="CRASHED") {
                        throw new RuntimeException("Job crashed");
                    }
                }
                throw new TimeoutException("Reached timeout");
            }
        });
    }


    /**
     * make statistics of fingerprints and write the used indizes of fingerprints into the
     * given TIntArrayList (as this property is not contained in FingerprintStatistics)
     * @param fingerprintIndizes
     * @return
     * @throws IOException
     */
    public FingerprintStatistics getStatistics(TIntArrayList fingerprintIndizes, List<String> fingerprintSmarts, List<String> fingerprintComments) throws IOException {
        fingerprintIndizes.clear();
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/statistics.csv").build());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        final TIntArrayList[] lists = new TIntArrayList[5];
        for (int k=1; k < 5; ++k) lists[k] = new TIntArrayList();
        lists[0] = fingerprintIndizes;
        try (CloseableHttpResponse response = client.execute(get)) {
            HttpEntity e = response.getEntity();
            final BufferedReader br = new BufferedReader(new InputStreamReader(e.getContent(), ContentType.getOrDefault(e).getCharset()));
            String line; //br.readLine();
            while ((line=br.readLine())!=null) {
                String[] tabs = line.split("\t");
                System.out.println(tabs.length);
                for (int k=0; k < 5; ++k) {
                    lists[k].add(Integer.parseInt(tabs[k]));
                }
                fingerprintSmarts.add(tabs[5]);
                if (tabs.length > 6) fingerprintComments.add(tabs[6].trim());
                else fingerprintComments.add(null);
            }
        }
        final FingerprintStatistics stats = new FingerprintStatistics(lists[1].toArray(), lists[2].toArray(), lists[3].toArray(), lists[4].toArray());
        stats.setFThreshold(0.25);
        stats.setMinimalNumberOfOccurences(10);
        return stats;
    }

    public List<Compound> getCompoundsFor(MolecularFormula formula, File output, int[] fingerprintIndizes, boolean bio) throws IOException {
        final HttpGet get;
        try {
            get = new HttpGet(getFingerIdURI("/webapi/compounds/" + (bio ? "bio/" : "not-bio/") + formula.toString() + ".json").build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        final ArrayList<Compound> compounds = new ArrayList<>(100);
        try (CloseableHttpResponse response = client.execute(get)) {
            try (MultiplexerFileAndIO io = new MultiplexerFileAndIO(response.getEntity().getContent(), new GZIPOutputStream(new FileOutputStream(output)))) {
                try (final JsonParser parser = Json.createParser(io)) {
                    return Compound.parseCompounds(fingerprintIndizes, compounds, parser);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    private static class MultiplexerFileAndIO extends InputStream implements Closeable {

        private final byte[] buffer;
        private final InputStream stream;
        private final OutputStream writer;
        private int offset, limit;
        private boolean closed = false;

        private MultiplexerFileAndIO(InputStream stream, OutputStream writer) throws IOException {
            this.buffer = new byte[1024*512];
            this.stream = stream;
            this.writer = writer;
            this.offset = 0; this.limit = 0;
            fillCache();
        }

        private boolean fillCache() throws IOException {
            this.limit = stream.read(buffer, 0, buffer.length);
            this.offset = 0;
            if (limit<=0) return false;
            writer.write(buffer, offset, limit);
            return true;
        }

        @Override
        public int read() throws IOException {
            if (offset >= limit) {
                if (!fillCache()) return -1;
            }
            return buffer[offset++];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int written = 0;
            while (true) {
                final int bytesAvailable = limit-offset;
                if (bytesAvailable<=0) {
                    if (!fillCache()) return written;
                }
                final int bytesToRead = len-off;
                if (bytesToRead==0) return written;
                final int bytesToWrite = Math.min(bytesAvailable, bytesToRead);
                System.arraycopy(buffer, offset, b, off, bytesToWrite);
                written += bytesToWrite;
                off += bytesToWrite;
                offset += bytesToWrite;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            boolean finished;
            do {
                finished = fillCache();
            } while (finished);
            stream.close();
            writer.close();
            closed=true;
        }
    }

}
