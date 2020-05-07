package de.unijena.bioinf.utils.mailService;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Mail {


    private final static Service DEFAULT_SERVICE;

    static {
        DEFAULT_SERVICE = new Service();
        INIT_PROPS(System.getProperties());
    }

    public static boolean INIT_PROPS(Properties props) {
        if (props != null) {
            String name = props.getProperty("de.unijena.bioinf.utils.mailService.username");
            String pw = props.getProperty("de.unijena.bioinf.utils.mailService.password");
            DEFAULT_SERVICE.setAuth(name != null ? name : "", pw != null ? pw : "");

            String port = props.getProperty("de.unijena.bioinf.utils.mailService.smtpPort");
            DEFAULT_SERVICE.setSmtpPort(port != null ? Integer.valueOf(port) : 465);

            String host = props.getProperty("de.unijena.bioinf.utils.mailService.smtpHost");
            DEFAULT_SERVICE.setHostName(host != null ? host : "");

            String sslOnConnect = props.getProperty("de.unijena.bioinf.utils.mailService.sslOnConnect");
            DEFAULT_SERVICE.setSSLOnConnect(sslOnConnect != null ? Boolean.valueOf(sslOnConnect) : false);
            return true;
        }
        return false;
    }

    public static void send(Email mail) throws EmailException {
        DEFAULT_SERVICE.send(mail);
    }

    public static String getSender() {
        return DEFAULT_SERVICE.getSender();
    }


    public static class Service {
        private boolean sSLOnConnect = false;
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
            mail.setAuthenticator(auth);
            mail.setSSLOnConnect(sSLOnConnect);
            //how to handle this properly
            mail.setSslSmtpPort(String.valueOf(smtpPort));
            mail.setSmtpPort(smtpPort);
            mail.send();

        }
    }

    private static final String EMAIL_REGEX =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean validateMailAdress(String adress) {
        return EMAIL_PATTERN.matcher(adress).matches();
    }
}
