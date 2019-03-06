package JsonTuples;

import io.github.cruisoring.tuple.Set;
import io.github.cruisoring.tuple.Tuple2;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends Set<NamedValue> implements JSONValue {
    public static Character LeftBrace = '{';
    public static Character RightBrace = '}';

    protected JSONObject(NamedValue... namedValues) {
        super(namedValues);
    }

    @Override
    public String toJSONString(int indentFactor) {
        int length = getLength();

        if(length == 0) {
            return "{}";
        }

        //JSON string starts with '{'
        StringBuilder sb = new StringBuilder(LeftBrace);

        for (int i = 0; i < length; i++) {
            NamedValue kvp = get(i);
            sb.append(kvp.toJSONString(indentFactor+1));
            sb.append(NewLine);
        }

        //JSON String ends with ']'
        sb.append(JSONable.getIndent(indentFactor) + RightBrace);
        return sb.toString();

    }
}
