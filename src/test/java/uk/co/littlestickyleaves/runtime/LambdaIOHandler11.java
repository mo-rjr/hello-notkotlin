package uk.co.littlestickyleaves.runtime;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
public class LambdaIOHandler11 {

    private static final String LAMBDA_RUNTIME_AWS_REQUEST_ID = "Lambda-Runtime-Aws-Request-Id";

    private static final String HTTP = "http://";
    private static final String RUNTIME = "/2018-06-01/runtime/";
    private static final String NEXT = "invocation/next";
    private static final String RESPONSE = "/response";
    private static final String INIT = "init";
    private static final String ERROR = "/error";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String runtimeApiRoot;

    public LambdaIOHandler11(String runtimeApiEndpoint) {
        this.runtimeApiRoot = HTTP + runtimeApiEndpoint + RUNTIME;
    }

    public LambdaInputWithId getLambdaInput() throws LambdaIOException {

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(runtimeApiRoot + NEXT))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Optional<String> awsRequestID = response.headers().firstValue(LAMBDA_RUNTIME_AWS_REQUEST_ID);

            return new LambdaInputWithId(awsRequestID.orElseThrow(this::noAwsRequestIdException),
                    response.body());
        } catch (InterruptedException | IOException exception) {
            throw new LambdaIOException("Poll for input resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    private LambdaIOException noAwsRequestIdException() {
        return new LambdaIOException("No " + LAMBDA_RUNTIME_AWS_REQUEST_ID + " header supplied");
    }

    public void returnLambdaOutput(String awsRequestId, String result) throws LambdaIOException {

        try {
            URI uri = URI.create(runtimeApiRoot + awsRequestId + RESPONSE);

            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(result))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("Posted successful output for " + awsRequestId + ", receiving: " + response.statusCode()); // proper logging
        } catch (InterruptedException | IOException exception) {
            throw new LambdaIOException("Attempt to POST successful output resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }

    }

    public void returnLambdaError(String awsRequestId, LambdaDataProcessingException ldpe)  {
        try {
            String jsonPayload = JsonExceptionShape.exceptionAsJson(ldpe);

            URI uri = URI.create(runtimeApiRoot + awsRequestId + RESPONSE);

            HttpResponse<String> response = postError(uri, jsonPayload);

            System.out.println("Posted processing error for " + awsRequestId + ", receiving: " + response.statusCode()); // proper logging
        } catch (InterruptedException | IOException exception) {
            throw new RuntimeException("Attempt to POST error output resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    public void returnInitializationError(LambdaIOException lioe) {
        try {
            URI uri = URI.create(runtimeApiRoot + INIT + ERROR);
            String jsonPayload = JsonExceptionShape.exceptionAsJson(lioe);
            HttpResponse<String> response = postError(uri, jsonPayload);

            System.out.println("Posted Initialistion error, receiving: " + response.statusCode()); // proper logging
        } catch (InterruptedException | IOException exception) {
            throw new RuntimeException("Attempt to POST initialization error resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    private HttpResponse<String> postError(URI uri, String jsonPayload) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .header("Lambda-Runtime-Function-Error-Type", "Unhandled")
                .build();

        return HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }
}
