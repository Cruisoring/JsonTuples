package JsonTuples;

import io.github.cruisoring.tuple.Set;

import java.util.regex.Pattern;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends Set<IJSONValue> implements IJSONValue {

    public static final Character LeftBracket = '[';
    public static final Character RightBracket = ']';

    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    public static JSONArray parseArray(String valueString) {
        return null;
    }

    protected JSONArray(IJSONValue... values) {
        super(values);
    }

    @Override
    public String toJSONString(int indentFactor) {
        int length = getLength();

        if(length == 0) {
            return "[]";
        }

        String indent = IJSONable.getIndent(indentFactor);
        String childrenIndent = IJSONable.getIndent(indentFactor+1);
        //JSON string starts with '['
        StringBuilder sb = new StringBuilder(LeftBracket);

        for (int i = 0; i < length; i++) {
            IJSONValue element = get(i);
            sb.append(childrenIndent);
            sb.append(element.toJSONString(indentFactor+1));
            sb.append(NewLine);
        }

        //JSON String ends with ']'
        sb.append(indent + RightBracket);
        return sb.toString();
    }

    @Override
    public Object getObject() {
        return asArray();
    }
}
