package JsonTuples;

import io.github.cruisoring.tuple.Set;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends Set<JSONValue> implements JSONValue {

    public static final Character LeftBracket = '[';
    public static final Character RightBracket = ']';


    protected JSONArray(JSONValue... values) {
        super(values);
    }

    @Override
    public String toJSONString(int indentFactor) {
        int length = getLength();

        if(length == 0) {
            return "[]";
        }

        String indent = JSONable.getIndent(indentFactor);
        String childrenIndent = JSONable.getIndent(indentFactor+1);
        //JSON string starts with '['
        StringBuilder sb = new StringBuilder(LeftBracket);

        for (int i = 0; i < length; i++) {
            JSONValue element = get(i);
            sb.append(childrenIndent);
            sb.append(element.toJSONString(indentFactor+1));
            sb.append(NewLine);
        }

        //JSON String ends with ']'
        sb.append(indent + RightBracket);
        return sb.toString();
    }
}
