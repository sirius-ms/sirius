package mailService;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.09.16.
 */

import de.unijena.bioinf.utils.mailService.Mail;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class EmailValidationTest {
    private static String[] VALID = new String[]{"mkyong@yahoo.com",
            "mkyong-100@yahoo.com", "mkyong.100@yahoo.com",
            "mkyong111@mkyong.com", "mkyong-100@mkyong.net",
            "mkyong.100@mkyong.com.au", "mkyong@1.com",
            "mkyong@gmail.com.com", "mkyong+100@gmail.com",
            "mkyong-100@yahoo-test.com"};
    private static String[] INVALID = new String[]{"mkyong", "mkyong@.com.my",
            "mkyong123@gmail.a", "mkyong123@.com", "mkyong123@.com.com",
            ".mkyong@mkyong.com", "mkyong()*@gmail.com", "mkyong@%*.com",
            "mkyong..2002@gmail.com", "mkyong.@gmail.com",
            "mkyong@mkyong@gmail.com", "mkyong@gmail.com.1a"};


    @Test
    public void ValidEmailTest() {

        for (String temp : VALID) {
            boolean valid = Mail.validateMailAdress(temp);
            System.out.println("Email is valid : " + temp + " , " + valid);
            Assert.assertEquals(valid, true);
        }

    }

    @Test
    public void InValidEmailTest() {
        for (String temp : INVALID) {
            boolean valid = Mail.validateMailAdress(temp);
            System.out.println("Email is valid : " + temp + " , " + valid);
            Assert.assertEquals(valid, false);
        }
    }
}
