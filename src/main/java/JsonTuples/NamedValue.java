package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;

/**
 * Name value pair contained by JSONObject. Each name is followed by : (colon) and the name/value pairs are separated by , (comma)
 */
public class NamedValue extends Tuple2<String, JSONValue> implements JSONable {
    public static final Character Colon = ':';
    public static final Character Comma = ',';

    protected NamedValue(String name, JSONValue value) {
        super(name, value);
    }

    @Override
    public String toJSONString(int indentFactor) {
        return String.format("%s\"%s\": %s",
                JSONable.getIndent(indentFactor),
                getFirst(),
                getSecond().toJSONString(indentFactor+1));
    }
}
