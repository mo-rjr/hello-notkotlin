package uk.co.littlestickyleaves.runtime;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class JsonExceptionShapeTest {

    @Test
    public void exceptionAsJson() throws IOException {
        // arrange
        String message = "Terrible things";
        RuntimeException runtimeException = new RuntimeException(message);

        // act
        String result = JsonExceptionShape.exceptionAsJson(runtimeException);

        // assert
        assertTrue(result.contains("\"errorMessage\":\"" + message));
        assertTrue(result.contains("\"errorType\":\"RuntimeException"));
    }
}