package uk.co.littlestickyleaves.runtime;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonExceptionShape {

    private String errorType;

    private String errorMessage;

    private List<String> stackTrace;

    public JsonExceptionShape(String errorType, String errorMessage, List<String> stackTrace) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
    }

    public static String exceptionAsJson(Exception exception) throws IOException {
        List<String> stackTraceAsStrings = Arrays.stream(exception.getStackTrace())
                .map(Object::toString)
                .collect(Collectors.toList());

        JsonExceptionShape jsonShape = new JsonExceptionShape(exception.getClass().getSimpleName(),
                exception.getMessage(), stackTraceAsStrings);
        return JSON.std.asString(jsonShape);
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }
}
