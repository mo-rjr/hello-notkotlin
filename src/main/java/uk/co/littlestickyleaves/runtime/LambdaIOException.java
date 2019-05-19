package uk.co.littlestickyleaves.runtime;

public class LambdaIOException extends Exception {

    public LambdaIOException(String message) {
        super(message);
    }

    public LambdaIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
