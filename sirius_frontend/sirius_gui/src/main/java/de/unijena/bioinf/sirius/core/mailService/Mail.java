package de.unijena.bioinf.sirius.core.mailService;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Mail {


    private final static Service DEFAULT_SERVICE;

    static {
        DEFAULT_SERVICE = new Service();
        DEFAULT_SERVICE.setAuth("finleymcserver@gmail.com", "xxx");
        DEFAULT_SERVICE.setSmtpPort(465);
        DEFAULT_SERVICE.setHostName("smtp.googlemail.com");
        DEFAULT_SERVICE.setSSLOnConnect(true);
    }

    public static void send(Email mail) throws EmailException {
        DEFAULT_SERVICE.send(mail);
    }

    public static String getSender() {
        return DEFAULT_SERVICE.getSender();
    }


    public static class Service {
        private boolean sSLOnConnect = true;
        private int smtpPort = 465;
        private javax.mail.Authenticator auth = null;
        private String sender = null;
        private String hostName = null;

        public boolean isSSLOnConnect() {
            return sSLOnConnect;
        }

        public void setSSLOnConnect(boolean sslOnConnect) {
            this.sSLOnConnect = sslOnConnect;
        }

        public int getSmtpPort() {
            return smtpPort;
        }

        public void setSmtpPort(int smtpPort) {
            this.smtpPort = smtpPort;
        }

        public javax.mail.Authenticator getAuth() {
            return auth;
        }

        public String getSender() {
            return sender;
        }

        public void setAuth(final String userName, final String password) {
            this.auth = new DefaultAuthenticator(userName, password);
            this.sender = userName;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public void send(Email mail) throws EmailException {
            mail.setHostName(hostName);
            mail.setSmtpPort(smtpPort);
            mail.setAuthenticator(auth);
            mail.setSSLOnConnect(sSLOnConnect);
            mail.send();
        }
    }
}
