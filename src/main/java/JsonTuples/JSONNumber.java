package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * A number is very much like a C or Java number, except that the octal and hexadecimal formats are not used.
 */
public class JSONNumber extends Tuple1<Number> implements JSONValue {

    public JSONNumber(Number number) {
        super(number);

        checkNotNull(number, "null shall not be used to create JSONNumber instance.");
    }

    @Override
    public String toJSONString(int indentFactor) {
        return getFirst().toString();
    }
}
