package uk.co.littlestickyleaves.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Using the most basic HTTP stuff I could find, because GraalVM doesn't like Unirest or OKHttp and only goes up to Java 8
 *
 *  * This class deals with getting Lambda input and output from the API.
 *  * It deals with String payloads and does not care about their meaning.
 *  * It has four options:
 *  * -- GET input
 *  * -- POST successful output for a specific input
 *  * -- POST an error for a specific input
 *  * -- POST a general initialization error
 *  *
 *  * The exception handling in this is not ideal.
 *  * What *should* happen if it all goes wrong
 */
public class LambdaIOHandlerSimple implements LambdaIOHandler {

    private static final String LAMBDA_RUNTIME_AWS_REQUEST_ID = "Lambda-Runtime-Aws-Request-Id";

    private static final String HTTP = "http://";
    private static final String RUNTIME = "/2018-06-01/runtime/";
    private static final String INVOCATION = "invocation/";
    private static final String NEXT = "next";
    private static final String RESPONSE = "/response";
    private static final String INIT = "init";
    private static final String ERROR = "/error";

    private final String runtimeApiRoot;

    public LambdaIOHandlerSimple(String runtimeApiEndpoint) {
        this.runtimeApiRoot = HTTP + runtimeApiEndpoint + RUNTIME;
    }

    /**
     * URL is "http://${AWS_LAMBDA_RUNTIME_API}/2018-06-01/runtime/invocation/next"
     */
    @Override
    public LambdaInputWithId getLambdaInput() throws LambdaIOException {

        try {
            URL inputUrl = new URL(runtimeApiRoot + INVOCATION + NEXT);

            HttpURLConnection httpURLConnection = (HttpURLConnection) inputUrl.openConnection();
            httpURLConnection.setRequestMethod("GET");

            int status = httpURLConnection.getResponseCode();

            if (status > 299) {
                handleError("Poll for input", status, httpURLConnection);
            }

            String awsRequestId = httpURLConnection.getHeaderField(LAMBDA_RUNTIME_AWS_REQUEST_ID);

            if (awsRequestId == null) {
                throw new LambdaIOException("Poll for input returned no header value for " + LAMBDA_RUNTIME_AWS_REQUEST_ID);
            }

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
                String content = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                return new LambdaInputWithId(awsRequestId, content);
            }

        } catch (IOException exception) {
            throw new LambdaIOException("Poll for input resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    /**
     * "http://${AWS_LAMBDA_RUNTIME_API}/2018-06-01/runtime/invocation/$REQUEST_ID/response";
     *
     * @param awsRequestId the aws id that came in the with request
     * @param result       the processed output as a json string
     * @throws LambdaIOException if something goes wrong
     */
    public void returnLambdaOutput(String awsRequestId, String result) throws LambdaIOException {

        try {
            URL outputUrl = new URL(runtimeApiRoot + INVOCATION + awsRequestId + RESPONSE);
            byte[] payload = result.getBytes();

            HttpURLConnection httpURLConnection = setUpPost(outputUrl, payload, false);

            int status = httpURLConnection.getResponseCode();

            if (status > 299) {
                handleError("POSTing processed output for aws id " + awsRequestId, status, httpURLConnection);
            }

            System.out.println("POSTing processed output for " + awsRequestId + ", receiving: " + status); // proper logging
        } catch (IOException exception) {
            throw new LambdaIOException("POSTing processed output resulted in " + exception.getClass().getSimpleName() +
                    " with message " + exception.getMessage(), exception);
        }
    }

    /**
     * "http://${AWS_LAMBDA_RUNTIME_API}/2018-06-01/runtime/invocation/$REQUEST_ID/error"
     *
     * @param awsRequestId    the request id of the input that resulted in an error
     * @param lambdaException the data processing exception thrown
     */
    public void returnLambdaError(String awsRequestId, LambdaDataProcessingException lambdaException) {

        try {
            URL outputUrl = new URL(runtimeApiRoot + INVOCATION + awsRequestId + ERROR);
            byte[] payload = JsonExceptionShape.exceptionAsJson(lambdaException).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection httpURLConnection = setUpPost(outputUrl, payload, true);

            int status = httpURLConnection.getResponseCode();

            System.out.println("Posted processing error for " + awsRequestId + ", receiving: " + status); // proper logging
        } catch (IOException ex) {
            throw new RuntimeException("Attempt to POST error output resulted in " + ex.getClass().getSimpleName() +
                    " with message " + ex.getMessage(), ex);
        }
    }

    /**
     * "http://${AWS_LAMBDA_RUNTIME_API}/2018-06-01/runtime/init/error"
     *
     * @param lambdaException the exception thrown
     */
    public void returnInitializationError(LambdaIOException lambdaException) {
        try {
            URL outputUrl = new URL(runtimeApiRoot + INIT + ERROR);
            byte[] payload = JsonExceptionShape.exceptionAsJson(lambdaException).getBytes(StandardCharsets.UTF_8);
            HttpURLConnection httpURLConnection = setUpPost(outputUrl, payload, true);

            int status = httpURLConnection.getResponseCode();

            System.out.println("Posted Initialization error, receiving: " + status); // proper logging
        } catch (IOException ex) {
            throw new RuntimeException("Attempt to POST initialization error resulted in " + ex.getClass().getSimpleName() +
                    " with message " + ex.getMessage(), ex);
        }
    }

    private void handleError(String action, int statusCode, HttpURLConnection httpURLConnection) throws LambdaIOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getErrorStream()))) {
            String errorContent = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            throw new LambdaIOException(action + " resulted in status code " + statusCode + " with message: " + errorContent);
        } catch (IOException e) {
            throw new LambdaIOException(action + " resulted in " + e.getClass().getSimpleName() + " with message " + e.getMessage());
        }
    }

    private HttpURLConnection setUpPost(URL url, byte[] payload, boolean error) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Content-Type", "application/json; utf-8");
        if (error) {
            httpURLConnection.setRequestProperty("Lambda-Runtime-Function-Error-Type", "Unhandled");
        }
        httpURLConnection.setDoOutput(true);

        try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
            outputStream.write(payload, 0, payload.length);
        }
        return httpURLConnection;
    }

}
