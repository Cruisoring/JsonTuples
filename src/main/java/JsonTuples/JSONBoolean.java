package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  http://www.json.org
 *  Represent the values of either 'true' or 'false'
 */
public class JSONBoolean extends Tuple1<Boolean> implements JSONValue {

    public static final JSONBoolean True = new JSONBoolean(true);
    public static final JSONBoolean False = new JSONBoolean(false);

    public static JSONBoolean fromJSONRaw(String valueString) {
        switch (valueString = valueString.trim()) {
            case "true":
                return True;
            case "false":
                return False;
            default:
                throw new IllegalArgumentException("Cannot parse Boolean value from " + valueString);
        }
    }

    protected JSONBoolean(Boolean aBoolean) {
        super(aBoolean);

        checkNotNull(aBoolean, "null shall not be used to create JSONBoolean instance.");
    }


    @Override
    public String toJSONString(int indentFactor) {
        return getFirst().toString();
    }

    @Override
    public Object getObject() {
        return getFirst();
    }
}
