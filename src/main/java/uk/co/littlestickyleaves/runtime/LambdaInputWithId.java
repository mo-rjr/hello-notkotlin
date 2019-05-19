package uk.co.littlestickyleaves.runtime;

public class LambdaInputWithId {

    private String awsRequestId;

    private String rawInput;

    public LambdaInputWithId() {
    }

    public LambdaInputWithId(String awsRequestId, String rawInput) {
        this.awsRequestId = awsRequestId;
        this.rawInput = rawInput;
    }

    public String getAwsRequestId() {
        return awsRequestId;
    }

    public void setAwsRequestId(String awsRequestId) {
        this.awsRequestId = awsRequestId;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }
}
