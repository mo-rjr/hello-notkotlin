package uk.co.littlestickyleaves.lambda;

import com.fasterxml.jackson.jr.ob.JSON;
import uk.co.littlestickyleaves.runtime.LambdaDataProcessingException;

import java.io.IOException;

public class StringRepeater {

    public String handleRaw(String rawInput) throws LambdaDataProcessingException {

        StringRepeatInstruction instruction = null;
        try {
            instruction = JSON.std.beanFrom(StringRepeatInstruction.class, rawInput);
            StringRepeatResult stringRepeatResult = handle(instruction);
            return JSON.std.asString(stringRepeatResult);
        } catch (IOException e) {
            throw new LambdaDataProcessingException("Unable to deserialize StringRepeatInstruction from input: " + rawInput, e);
        }
    }

    private StringRepeatResult handle(StringRepeatInstruction instruction) {
        String result = repeat(instruction.getInput(), instruction.getRepeat());
        return new StringRepeatResult(instruction.getInput(), instruction.getRepeat(), result);
    }

    private String repeat(String input, int times) {
        if (times <= 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int step = 0; step < times; step++) {
            stringBuilder.append(input);
        }
        return stringBuilder.toString();

    }
}
