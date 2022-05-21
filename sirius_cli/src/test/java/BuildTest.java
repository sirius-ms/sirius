import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class BuildTest {

    public static void main(String[] args){

        final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        final PrintStream originalOut = System.out;

        System.setOut(new PrintStream(outContent));

        System.out.print("Hello World.");

        if(!outContent.toString().equals("Hello World.")){
            throw new RuntimeException();
        }

        System.setOut(originalOut);
    }
}
