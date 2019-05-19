package uk.co.littlestickyleaves.runtime;


public interface LambdaIOHandler {

    LambdaInputWithId getLambdaInput() throws LambdaIOException;

    void returnLambdaOutput(String awsRequestId, String result) throws LambdaIOException;

    void returnLambdaError(String awsRequestId, LambdaDataProcessingException exception);

    void returnInitializationError(LambdaIOException exception);
}
