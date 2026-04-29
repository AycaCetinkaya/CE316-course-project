public class ExactMatchComparator implements OutputComparator {

    @Override
    public boolean compare(String actual, String expected) {
        String a = actual   != null ? actual   : "";
        String e = expected != null ? expected : "";

        String[] actualLines   = normalise(a);
        String[] expectedLines = normalise(e);

        if (actualLines.length != expectedLines.length) return false;

        for (int i = 0; i < actualLines.length; i++) {
            if (!actualLines[i].equals(expectedLines[i])) return false;
        }
        return true;
    }

    private String[] normalise(String text) {
        String normalised = text.replace("\r\n", "\n").replace("\r", "\n");
        if (normalised.endsWith("\n")) {
            normalised = normalised.substring(0, normalised.length() - 1);
        }
        String[] lines = normalised.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].stripTrailing();
        }
        return lines;
    }

    /*
    1. compiled == false  ->  COMPILE_ERROR
    2. runExitCode != 0   ->  RUNTIME_ERROR
    3. compare() == false ->  WRONG_OUTPUT
    4. compare() == true  ->  SUCCESS
    */

    public Result toResult(EvaluationResult evalResult, String expectedOutput) {
        if (!evalResult.isCompiled()) {
            return new Result(Status.COMPILE_ERROR, "", evalResult.getCompileStderr());
        }

        if (evalResult.getRunExitCode() != 0) {
            return new Result(Status.RUNTIME_ERROR, evalResult.getRunStdout(), evalResult.getRunStderr());
        }

        boolean match = compare(evalResult.getRunStdout(), expectedOutput);
        if (!match) {
            return new Result(Status.WRONG_OUTPUT, evalResult.getRunStdout(), "");
        }

        return new Result(Status.SUCCESS, evalResult.getRunStdout(), "");
    }
}