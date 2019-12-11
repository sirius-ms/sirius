package de.unijena.bioinf.ms.rest.model.fingerid;

public class FingerprintJobData {
    //optional fields
    public final String securityToken;
    public final byte[] fingerprints; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES
    public final byte[] iokrVector; // LITTLE ENDIAN BINARY ENCODED PLATT PROBABILITIES


    private FingerprintJobData() {
        this(null, null, null);
    }

    public FingerprintJobData(String securityToken, byte[] fingerprints, byte[] iokrVector) {
        this.securityToken = securityToken;
        this.fingerprints = fingerprints;
        this.iokrVector = iokrVector;
    }



    /*private FingerprintJobUpdate(@NotNull JsonObject obj) throws IOException {
        if (!obj.containsKey("jobId"))
            throw new IllegalArgumentException("Job Update has no jobId. JobId is mandatory!");
        if (!obj.containsKey("state"))
            throw new IllegalArgumentException("Job Update has no state. State is mandatory!");


        this.jobId = obj.getJsonNumber("jobId").longValue();
        this.state = JobState.valueOf(obj.getString("state").toUpperCase());

        this.securityToken = obj.containsKey("securityToken") ? obj.getString("securityToken") : null;

        this.fingerprints = obj.containsKey("prediction") ? Base64.decode(obj.getString("prediction")) : null;
        this.iokrVector = obj.containsKey("iokrVector") ? Base64.decode(obj.getString("iokrVector")) : null;
        this.errorMessage = obj.containsKey("errors") ? obj.getString("errors") : null;
    }
*/
    /*public String toJson() {
        try (final StringWriter writer = new StringWriter()) {
            try (final JsonGenerator generator = Json.createGenerator(writer)) {
                toJsonContext(generator);
            }
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void toJson(@NotNull Writer writer) {
        try (final JsonGenerator generator = Json.createGenerator(writer)) {
            toJsonContext(generator);
        }
    }

    public void toJsonContext(@NotNull JsonGenerator generator) {
        generator.writeStartObject();

        generator.write("jobId", String.valueOf(jobId));
        generator.write("state", state.toString());

        if (securityToken != null)
            generator.write("securityToken", securityToken);
        if (fingerprints != null)
            generator.write("prediction", Base64.encodeBytes(fingerprints));
        if (iokrVector != null)
            generator.write("iokrVector", Base64.encodeBytes(iokrVector));
        if (errorMessage != null)
            generator.write("errors", errorMessage);

        generator.writeEnd();
    }*/


//    public static FingerprintJobData fromJson(@NotNull String json) throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        return objectMapper.readValue(json, FingerprintJobData.class);
//        /*try (final JsonReader jsonReader = Json.createReader(new BufferedReader(new StringReader(json)))) {
//            return fromJson(jsonReader.readObject());
//        }*/
//    }
//
//    public static FingerprintJobData fromJson(@NotNull Reader obj) throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        return objectMapper.readValue(obj, FingerprintJobData.class);
//    }
    /*public static FingerprintJobUpdate fromJson(@NotNull JsonObject obj) throws IOException {
        return new FingerprintJobUpdate(obj);
    }*/
}

