package uk.co.littlestickyleaves.lambda;

public class StringRepeatResult {

    private String input;

    private int repeat;

    private String result;

    public StringRepeatResult() {
    }

    public StringRepeatResult(String input, int repeat, String result) {
        this.input = input;
        this.repeat = repeat;
        this.result = result;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}