public class WhitespaceInsensitiveComparator implements OutputComparator {

    @Override
    public boolean compare(String actual, String expected) {
        String a = actual == null ? "" : actual.trim();
        String e = expected == null ? "" : expected.trim();

        if (a.isEmpty() && e.isEmpty()) {
            return true;
        }

        if (a.isEmpty() || e.isEmpty()) {
            return false;
        }

        String[] actualTokens = a.split("\\s+");
        String[] expectedTokens = e.split("\\s+");

        if (actualTokens.length != expectedTokens.length) {
            return false;
        }

        for (int i = 0; i < actualTokens.length; i++) {
            if (!actualTokens[i].equals(expectedTokens[i])) {
                return false;
            }
        }

        return true;
    }
}