package uk.co.littlestickyleaves.runtime;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This class deals with getting Lambda input and output from the API.
 * It deals with String payloads and does not care about their meaning.
 * It has four options:
 * -- GET input
 * -- POST successful output for a specific input
 * -- POST an error for a specific input
 * -- POST a general initialization error
 * <p>
 * So the exception handling in this is not ideal.
 * What *should* happen if it all goes wrong?
 */
public class LambdaIOHandlerUnirest {

    private static final String LAMBDA_RUNTIME_AWS_REQUEST_ID = "Lambda-Runtime-Aws-Request-Id";

    private static final String HTTP = "http://";
    private static final String RUNTIME = "/2018-06-01/runtime/";
    private static final String NEXT = "invocation/next";
    private static final String RESPONSE = "/response";
    private static final String INIT = "init";
    private static final String ERROR = "/error";

    private final String runtimeApiRoot;

    public LambdaIOHandlerUnirest(String runtimeApiEndpoint) {
        this.runtimeApiRoot = HTTP + runtimeApiEndpoint + RUNTIME;
    }

    public LambdaInputWithId getLambdaInput() throws LambdaIOException {

        try {

            HttpResponse<String> response = Unirest.get(runtimeApiRoot + NEXT)
                    .asString();
            String awsRequestID = response.getHeaders().getFirst(LAMBDA_RUNTIME_AWS_REQUEST_ID);
            if (awsRequestID == null) {
                throw noAwsRequestIdException();
            }

            return new LambdaInputWithId(awsRequestID, response.getBody());
        } catch (UnirestException exception) {
            throw new LambdaIOException("Poll for input resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    private LambdaIOException noAwsRequestIdException() {
        return new LambdaIOException("No " + LAMBDA_RUNTIME_AWS_REQUEST_ID + " header supplied");
    }

    public void returnLambdaOutput(String awsRequestId, String result) throws LambdaIOException {

        try {
            HttpResponse<String> response = Unirest.post(runtimeApiRoot + awsRequestId + RESPONSE)
                    .body(result)
                    .asString();

            System.out.println("Posted successful output for " + awsRequestId + ", receiving status code: " + response.getStatus()); // proper logging
        } catch (UnirestException exception) {
            throw new LambdaIOException("Attempt to POST successful output resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    public void returnLambdaError(String awsRequestId, LambdaDataProcessingException ldpe)  {
        try {
            String jsonPayload = JsonExceptionShape.exceptionAsJson(ldpe);

            HttpResponse<String> response = Unirest.post(runtimeApiRoot + awsRequestId + RESPONSE)
                    .header("Lambda-Runtime-Function-Error-Type", "Unhandled")
                    .body(jsonPayload)
                    .asString();

            System.out.println("Posted processing error for " + awsRequestId + ", receiving status code: " + response.getStatus()); // proper logging
        } catch (IOException | UnirestException exception) {
            throw new RuntimeException("Attempt to POST error output resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    public void returnInitializationError(LambdaIOException lioe) {
        try {
            String jsonPayload = JsonExceptionShape.exceptionAsJson(lioe);
            HttpResponse<String> response = Unirest.post(runtimeApiRoot + INIT + ERROR)
                    .header("Lambda-Runtime-Function-Error-Type", "Unhandled")
                    .body(jsonPayload)
                    .asString();

            System.out.println("Posted Initialistion error, receiving status code: " + response.getStatus()); // proper logging
        } catch (IOException | UnirestException exception) {
            throw new RuntimeException("Attempt to POST initialization error resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

}
