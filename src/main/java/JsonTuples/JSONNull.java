package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

/**
 * http://www.json.org
 * Represent the value of null.
 */
public class JSONNull extends Tuple1<Object> implements JSONValue {

    public final static JSONNull Null = new JSONNull();

    public static JSONNull fromJSONRaw(String valueString) {
        valueString = valueString.trim();
        if("null".equals(valueString)){
            return Null;
        } else {
            throw new IllegalArgumentException("Cannot parse Boolean value from " + valueString);
        }
    }


    protected JSONNull() {
        super(null);
    }

    @Override
    public String toJSONString(int indentFactor) {
        return "null";
    }

    @Override
    public Object getObject() {
        return getFirst();
    }
}
