package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

/**
 * http://www.json.org
 * Represent the value of null.
 */
public class JSONNull extends Tuple1<Object> implements JSONValue {

    public final static JSONNull Null = new JSONNull();

    protected JSONNull() {
        super(null);
    }

    @Override
    public String toJSONString(int indentFactor) {
        return "null";
    }
}
