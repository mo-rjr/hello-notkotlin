package uk.co.littlestickyleaves.lambda;


import org.junit.Test;
import uk.co.littlestickyleaves.runtime.LambdaDataProcessingException;

import static org.junit.Assert.assertTrue;

public class StringRepeaterTest {

    @Test
    public void handleRaw() throws LambdaDataProcessingException {
        // arrange
        StringRepeater testObject = new StringRepeater();
        String rawInput = "{\"input\":\"hello\", \"repeat\":3}";

        // act
        String result = testObject.handleRaw(rawInput);

        // assert
        assertTrue(result.contains("\"result\":\"hellohellohello\""));
    }

    @Test(expected = LambdaDataProcessingException.class)
    public void badInput() throws LambdaDataProcessingException {
        // arrange
        StringRepeater testObject = new StringRepeater();
        String rawInput = "{\"input\":2, \"repeat:3}";

        // act
        testObject.handleRaw(rawInput);
    }
}