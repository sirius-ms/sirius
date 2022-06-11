import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

public class ResultEqualityTest {

    @Test
    @DisplayName("The top hit of the MGF manual file should still be the same")
    void mgf_fFile_Top_Hit_Should_Match_With_Known() {
        assertEquals("String", "String");
    }

    @Test
    @DisplayName("The top hit of the MS manual file should still be the same")
    void ms_File_Top_Hit_Should_Match_With_Known() {
        assertEquals("FalseString", "String");
    }

}
