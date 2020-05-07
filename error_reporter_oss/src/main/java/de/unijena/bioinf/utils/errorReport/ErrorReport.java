package de.unijena.bioinf.utils.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.09.16.
 */


import com.google.gson.*;
import de.unijena.bioinf.utils.io.Compress;
import de.unijena.bioinf.utils.mailService.Mail;
import de.unijena.bioinf.utils.systemInfo.SystemInformation;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReport {
    public final static String[] TYPES =  {"Error Report","Bug Report", "Feature Request"};
    public final static String NO_USER_MAIL = "none"; //this is to make json happy, because not every parser likes null values
    private String identifier = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

    private boolean sendSystemInfo = false;
    protected String version = "UnknownVersion";
    protected String subject = "";
    protected String userMessage = "";
    protected Map<InputStream, String> additionalFiles = new HashMap<>();
    protected byte[] compessedAdditionalFiles = null;
    private String userEmail = NO_USER_MAIL;
    private boolean sendReportToUser = true;
    private String type = TYPES[0];

    public ErrorReport(String subject, String userMessage, Map<InputStream, String> additionalFiles) {
        this.subject = subject;
        setUserMessage(userMessage);
        this.additionalFiles = additionalFiles;
    }

    public ErrorReport() {}

    public ErrorReport(String subject) {
        setSubject(subject);
    }

    public String getIdentifier() {
        return identifier;
    }

    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public Map<InputStream, String> getAdditionalFiles() {
        return additionalFiles;
    }

    public void writeAdditionalFilesToCompressedArchive(File file) {
        Compress.compressToZipArchive(file, getAdditionalFiles());
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public byte[] getAdditionalFilesAsCompressedBytes() {
        if (compessedAdditionalFiles == null) {
            ByteArrayOutputStream attach = new ByteArrayOutputStream();
            Compress.compressToZipArchive(attach, getAdditionalFiles());
            compessedAdditionalFiles = attach.toByteArray();
        }
        return compessedAdditionalFiles;
    }

    public void setAdditionalFiles(Map<InputStream, String> additionalFiles) {
        compessedAdditionalFiles = null;
        this.additionalFiles = additionalFiles;
    }

    public void addAdditionalFiles(InputStream fileStream, String filename) {
        compessedAdditionalFiles = null;
        additionalFiles.put(fileStream, filename);
    }

    public void addAdditionalFiles(File file) throws FileNotFoundException {
        addAdditionalFiles(new FileInputStream(file), file.getName());
    }

    public void addAdditionalFiles(String file) throws FileNotFoundException {
        File f = new File(file);
        addAdditionalFiles(f);
    }

    public void addSystemInfoFile() throws IOException {
        sendSystemInfo = true;
        PipedOutputStream sysInfoOut = new PipedOutputStream();
        LoggerFactory.getLogger(this.getClass()).info("Collection system Information");
        SystemInformation.writeSystemInformationTo(sysInfoOut);
        PipedInputStream sysInfoIn = new PipedInputStream(sysInfoOut);
        addAdditionalFiles(sysInfoIn, "system.info");
    }

    public boolean isSendSystemInfo() {
        return sendSystemInfo;
    }

    //this is only for serialisation
    boolean setSendSystemInfo(boolean sendSystemInfo) {
        return this.sendSystemInfo = sendSystemInfo;
    }


    public String getUserEmail() {
        return userEmail;
    }

    public boolean setUserEmail(String userEmail) {
        if (Mail.validateMailAdress(userEmail)) {
            this.userEmail = userEmail;
            return true;
        } else if (userEmail == null || userEmail.isEmpty() || userEmail.equals(NO_USER_MAIL)) {
            this.userEmail = NO_USER_MAIL;
            return true;
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHeadline() {
        return "========== " + type + " (" + identifier + ") ==========";
    }

    public boolean isSendReportToUser() {
        return sendReportToUser;
    }

    public void setSendReportToUser(boolean sendReportToUser) {
        this.sendReportToUser = sendReportToUser;
    }

    public boolean hasUserMail() {
        return !userEmail.equals(NO_USER_MAIL);
    }

    void setCompessedAdditionalFiles(byte[] bytes) {
        this.compessedAdditionalFiles = bytes;
    }

    protected static <T extends ErrorReport> Gson getGson(Class<T> clas) {
        return new GsonBuilder().registerTypeAdapter(clas, new ReportSerializer<T>()).registerTypeAdapter(clas, new ReportDeSerializer<T>()).create();
    }

    public static <T extends ErrorReport> String toJson(T report) {
        return getGson(report.getClass()).toJson(report);
    }

    public static <T extends ErrorReport> T fromJson(String json, Class<T> clas) {
        Gson gson = getGson(clas);
        return gson.fromJson(json, clas);
    }

    protected static class ReportSerializer<T extends ErrorReport> implements JsonSerializer<T> {

        public JsonElement serialize(final T report, final Type type, final JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.add("sendSystemInfo", new JsonPrimitive((report.isSendSystemInfo())));
            result.add("sendReportToUser", new JsonPrimitive((report.isSendReportToUser())));

            result.add("identifier", new JsonPrimitive(report.getIdentifier()));
            result.add("type", new JsonPrimitive(report.getType()));
            result.add("version", new JsonPrimitive(report.getVersion()));
            result.add("subject", new JsonPrimitive(report.getSubject()));
            result.add("userMessage", new JsonPrimitive(report.getUserMessage()));
            result.add("userMail", new JsonPrimitive(report.getUserEmail()));

            String encoded = Base64.getEncoder().encodeToString(report.getAdditionalFilesAsCompressedBytes());
            result.add("attachment", new JsonPrimitive(encoded));
            return result;
        }
    }

    protected static class ReportDeSerializer<T extends ErrorReport> implements JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                T report = ((Class<T>) typeOfT).newInstance();

                JsonObject j = json.getAsJsonObject();

                report.setSendSystemInfo(j.get("sendSystemInfo").getAsBoolean());
                report.setSendReportToUser(j.get("sendReportToUser").getAsBoolean());

                report.setSubject(j.get("subject").getAsString());
                report.setIdentifier(j.get("identifier").getAsString());
                report.setType(j.get("type").getAsString());
                report.setVersion(j.get("version").getAsString());
                report.setUserMessage(j.get("userMessage").getAsString());
                report.setUserEmail(j.get("userMail").getAsString());

                String decoded = j.get("attachment").getAsString();
                report.setCompessedAdditionalFiles(Base64.getDecoder().decode(decoded));
                return report;
            } catch (InstantiationException | IllegalAccessException e) {
                LoggerFactory.getLogger(this.getClass()).error("Could not deserialize logger from Json", e);
            }
            return null;
        }

        private static final String TYPE_NAME_PREFIX = "class ";

        public static String getClassName(Type type) {
            if (type == null) {
                return "";
            }
            String className = type.toString();
            if (className.startsWith(TYPE_NAME_PREFIX)) {
                className = className.substring(TYPE_NAME_PREFIX.length());
            }
            return className;
        }

        public static Class<?> getClass(Type type)
                throws ClassNotFoundException {
            String className = getClassName(type);
            if (className == null || className.isEmpty()) {
                return null;
            }
            return Class.forName(className);
        }

        public static Object newInstance(Type type)
                throws ClassNotFoundException, InstantiationException, IllegalAccessException {
            Class<?> clazz = getClass(type);
            if (clazz == null) {
                return null;
            }
            return clazz.newInstance();
        }
    }
}
