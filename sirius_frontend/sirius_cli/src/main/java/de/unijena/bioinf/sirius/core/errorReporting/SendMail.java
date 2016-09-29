package de.unijena.bioinf.sirius.core.errorReporting;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 28.09.16.
 */

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.MultiPartEmail;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SendMail {

    public static MultiPartEmail getEmailInstance(){
        MultiPartEmail email = new MultiPartEmail();
        email.setHostName("smtp.googlemail.com");
        email.setSmtpPort(465);
        email.setAuthenticator(new DefaultAuthenticator("finleymcserver@gmail.com", "xxx"));
        email.setSSLOnConnect(true);

        return email;
    }

}
