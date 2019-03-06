package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org/
 * A string is a sequence of zero or more Unicode characters, wrapped in double quotes, using backslash escapes.
 * A character is represented as a single character string. A string is very much like a C or Java string.
 */
public class JSONString extends Tuple1<String> implements JSONValue {
    public static final Character Quote = '"';
    public static final Character BackSlash = '\\';

    public JSONString(String s) {
        super(s);

        checkNotNull(s, "null shall not be treated as JSON String Object.");
    }

    @Override
    public String toJSONString(int indentFactor) {
        String string = getFirst();


        if (string.isEmpty()) {
            return "\"\"";
        }

        //TODO: refactor the codes later
        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        StringBuilder w = new StringBuilder(Quote);
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.append(Quote + c);
                    break;
                case '/':
                    if (b == '<') {
                        w.append(BackSlash);
                    }
                    w.append(c);
                    break;
                case '\b':
                    w.append(BackSlash + 'b');
                    break;
                case '\t':
                    w.append(BackSlash + 't');
                    break;
                case '\n':
                    w.append(BackSlash + 'n');
                    break;
                case '\f':
                    w.append(BackSlash + 'f');
                    break;
                case '\r':
                    w.append(BackSlash + 'r');
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        w.append(BackSlash + 'u');
                        hhhh = Integer.toHexString(c);
                        w.append("0000", 0, 4 - hhhh.length());
                        w.append(hhhh);
                    } else {
                        w.append(c);
                    }
            }
        }
        w.append('"');
        return w.toString();
    }
}
