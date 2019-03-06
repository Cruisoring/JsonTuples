package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

/**
 *  http://www.json.org
 *  Represent the values of either 'true' or 'false'
 */
public class JSONBoolean extends Tuple1<Boolean> implements JSONValue {

    public static final JSONBoolean True = new JSONBoolean(true);
    public static final JSONBoolean False = new JSONBoolean(false);

    protected JSONBoolean(Boolean aBoolean) {
        super(aBoolean);

        checkNotNull(aBoolean, "null shall not be used to create JSONBoolean instance.");
    }

    private void checkNotNull(Boolean aBoolean, String s) {
    }

    @Override
    public String toJSONString(int indentFactor) {
        return getFirst().toString();
    }
}
