import java.util.ArrayList;
import java.util.List;

public class LineDiff {

    public enum Type {
        SAME,
        CHANGED,
        MISSING_EXPECTED,
        MISSING_ACTUAL
    }

    public static class Row {
        private final int lineNumber;
        private final String expectedLine;
        private final String actualLine;
        private final Type type;

        public Row(int lineNumber, String expectedLine, String actualLine, Type type) {
            this.lineNumber = lineNumber;
            this.expectedLine = expectedLine == null ? "" : expectedLine;
            this.actualLine = actualLine == null ? "" : actualLine;
            this.type = type;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getExpectedLine() {
            return expectedLine;
        }

        public String getActualLine() {
            return actualLine;
        }

        public Type getType() {
            return type;
        }
    }

    public static List<Row> diff(String expected, String actual) {
        String[] expectedLines = splitLines(expected);
        String[] actualLines = splitLines(actual);

        List<Row> rows = new ArrayList<>();

        if (expectedLines.length == 0 && actualLines.length == 0) {
            rows.add(new Row(1, "", "", Type.SAME));
            return rows;
        }

        int max = Math.max(expectedLines.length, actualLines.length);

        for (int i = 0; i < max; i++) {
            String expectedLine = i < expectedLines.length ? expectedLines[i] : "";
            String actualLine = i < actualLines.length ? actualLines[i] : "";

            Type type;

            if (i >= expectedLines.length) {
                type = Type.MISSING_EXPECTED;
            } else if (i >= actualLines.length) {
                type = Type.MISSING_ACTUAL;
            } else if (expectedLine.equals(actualLine)) {
                type = Type.SAME;
            } else {
                type = Type.CHANGED;
            }

            rows.add(new Row(i + 1, expectedLine, actualLine, type));
        }

        return rows;
    }

    private static String[] splitLines(String text) {
        if (text == null) {
            return new String[0];
        }

        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");

        if (normalized.isEmpty()) {
            return new String[0];
        }

        if (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized.split("\n", -1);
    }
}