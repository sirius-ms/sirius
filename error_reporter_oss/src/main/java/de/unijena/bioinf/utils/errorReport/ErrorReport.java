/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.utils.errorReport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
@JsonDeserialize(using = ErrorReport.ReportDeSerializer.class)
@JsonSerialize(using = ErrorReport.ReportSerializer.class)
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

    protected static class ReportSerializer extends JsonSerializer<ErrorReport> {
        @Override
        public void serialize(ErrorReport report, JsonGenerator result, SerializerProvider serializers) throws IOException {
            result.writeBooleanField("sendSystemInfo", (report.isSendSystemInfo()));
            result.writeBooleanField("sendReportToUser", (report.isSendReportToUser()));

            result.writeStringField("identifier", report.getIdentifier());
            result.writeStringField("type", report.getType());
            result.writeStringField("version", report.getVersion());
            result.writeStringField("subject", report.getSubject());
            result.writeStringField("userMessage", report.getUserMessage());
            result.writeStringField("userMail", report.getUserEmail());

            String encoded = Base64.getEncoder().encodeToString(report.getAdditionalFilesAsCompressedBytes());
            result.writeStringField("attachment", encoded);
        }
    }

    protected static class ReportDeSerializer extends JsonDeserializer<ErrorReport> {
        @Override
        public ErrorReport deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode j = jp.getCodec().readTree(jp);
//            T report = ((Class<T>) typeOfT).newInstance();
            ErrorReport report = new ErrorReport();
            report.setSendSystemInfo(j.get("sendSystemInfo").asBoolean());
            report.setSendReportToUser(j.get("sendReportToUser").asBoolean());

            report.setSubject(j.get("subject").asText());
            report.setIdentifier(j.get("identifier").asText());
            report.setType(j.get("type").asText());
            report.setVersion(j.get("version").asText());
            report.setUserMessage(j.get("userMessage").asText());
            report.setUserEmail(j.get("userMail").asText());

            String decoded = j.get("attachment").asText();
            report.setCompessedAdditionalFiles(Base64.getDecoder().decode(decoded));
            return report;
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
