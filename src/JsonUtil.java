import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    public static String encodeString(String value) {
        if (value == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;

        int i = 0;
        int len = json.length();

        while (i < len && json.charAt(i) != '{') i++;
        if (i >= len) return map;
        i++;

        while (i < len) {
            i = skipWhitespace(json, i);
            if (i >= len) break;
            if (json.charAt(i) == '}') break;
            if (json.charAt(i) == ',') { i++; continue; }
            if (json.charAt(i) != '"') { i++; continue; }

            int[] keyEnd = new int[1];
            String key = readString(json, i, keyEnd);
            i = keyEnd[0];

            i = skipWhitespace(json, i);
            if (i < len && json.charAt(i) == ':') i++;
            i = skipWhitespace(json, i);

            if (i < len && json.charAt(i) == '"') {
                int[] valEnd = new int[1];
                String value = readString(json, i, valEnd);
                i = valEnd[0];
                map.put(key, value);
            } else {
                int valStart = i;
                while (i < len && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                map.put(key, json.substring(valStart, i).trim());
            }
        }

        return map;
    }

    public static List<String> splitTopLevelObjects(String json) {
        List<String> result = new ArrayList<>();
        if (json == null) return result;

        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (inString) {
                if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }

            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return result;
    }

    public static String encodeArray(List<String> objectJsons) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < objectJsons.size(); i++) {
            sb.append("  ").append(objectJsons.get(i));
            if (i < objectJsons.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String readString(String s, int start, int[] endOut) {
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        int len = s.length();

        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (i + 5 < len) {
                            String hex = s.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ignored) {}
                            i += 4;
                        }
                        break;
                    default: sb.append(next);
                }
                i += 2;
            } else if (c == '"') {
                endOut[0] = i + 1;
                return sb.toString();
            } else {
                sb.append(c);
                i++;
            }
        }

        endOut[0] = i;
        return sb.toString();
    }
}
