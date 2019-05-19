package uk.co.littlestickyleaves;

import uk.co.littlestickyleaves.lambda.StringRepeater;
import uk.co.littlestickyleaves.runtime.*;

public class StringRepeatLambdaRunner {

    private final LambdaIOHandler lambdaIOHandler;

    private final StringRepeater stringRepeater;

    public StringRepeatLambdaRunner(LambdaIOHandler lambdaIOHandler, StringRepeater stringRepeater) {
        this.lambdaIOHandler = lambdaIOHandler;
        this.stringRepeater = stringRepeater;
    }

    public static void main(String[] args) {
        String runtimeApiEndpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
        LambdaIOHandlerSimple lambdaIOHandler = new LambdaIOHandlerSimple(runtimeApiEndpoint);
        StringRepeater stringRepeater = new StringRepeater();
        StringRepeatLambdaRunner stringRepeatLambdaRunner = new StringRepeatLambdaRunner(lambdaIOHandler, stringRepeater);
        stringRepeatLambdaRunner.loop();
    }

    private void loop() {

        LambdaInputWithId lambdaInputWithId = null;

        while (true) {

            try {
                lambdaInputWithId = lambdaIOHandler.getLambdaInput();

                // process
                String result = stringRepeater.handleRaw(lambdaInputWithId.getRawInput());

                // return
                lambdaIOHandler.returnLambdaOutput(lambdaInputWithId.getAwsRequestId(), result);

            } catch (LambdaIOException e) {
                lambdaIOHandler.returnInitializationError(e);
            } catch (LambdaDataProcessingException ldpe) {
                lambdaIOHandler.returnLambdaError(lambdaInputWithId.getAwsRequestId(), ldpe);
            }
        }
    }



}
