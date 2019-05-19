package uk.co.littlestickyleaves.runtime;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class LambdaIOHandlerSimpleTest {

    public static final String RUNTIME_API_ENDPOINT = "localhost:8080";

    private LambdaIOHandlerSimple testObject;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule();

    @Before
    public void setUp() throws Exception {
        testObject = new LambdaIOHandlerSimple(RUNTIME_API_ENDPOINT);
    }

    @Test
    public void getLambdaInput() throws LambdaIOException {
        // arrange
        String testUrl = "/2018-06-01/runtime/invocation/next";
        String awsId = "314728";
        String body = "bodyText";
        stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withHeader("Lambda-Runtime-Aws-Request-Id", awsId)
                        .withBody(body)));

        // act
        LambdaInputWithId result = testObject.getLambdaInput();

        // assert
        assertEquals(awsId, result.getAwsRequestId());
        assertEquals(body, result.getRawInput());
    }

    @Test
    public void returnLambdaOutput() throws LambdaIOException {
        // arrange
        String awsId = "314728";
        String testUrl = "/2018-06-01/runtime/invocation/" + awsId + "/response";
        String body = "bodyText";
        stubFor(post(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)));

        // act
        testObject.returnLambdaOutput(awsId, body);

        // assert
        verify(postRequestedFor(urlEqualTo(testUrl))
                .withRequestBody(matching(body)));
    }

    @Test
    public void returnLambdaError() throws IOException {
        // arrange
        String awsId = "314728";
        String testUrl = "/2018-06-01/runtime/invocation/" + awsId + "/error";
        LambdaDataProcessingException exception = new LambdaDataProcessingException("Data Processing Fail");
        String body = JsonExceptionShape.exceptionAsJson(exception);
        stubFor(post(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)));

        // act
        testObject.returnLambdaError(awsId, exception);

        // assert
        verify(postRequestedFor(urlEqualTo(testUrl))
                .withRequestBody(equalTo(body))
                .withHeader("Lambda-Runtime-Function-Error-Type", equalTo("Unhandled")));
    }

    @Test
    public void returnInitializationError() throws IOException {
        // arrange
        String awsId = "314728";
        String testUrl = "/2018-06-01/runtime/init/error";
        LambdaIOException exception = new LambdaIOException("Initialization error");
        String body = JsonExceptionShape.exceptionAsJson(exception);
        stubFor(post(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)));

        // act
        testObject.returnInitializationError(exception);

        // assert
        verify(postRequestedFor(urlEqualTo(testUrl))
                .withRequestBody(equalTo(body))
                .withHeader("Lambda-Runtime-Function-Error-Type", equalTo("Unhandled")));
    }
}