package JsonTuples;

import io.github.cruisoring.tuple.Tuple1;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * A number is very much like a C or Java number, except that the octal and hexadecimal formats are not used.
 */
public class JSONNumber extends Tuple1<Number> implements JSONValue {

    /**
     *  Regular Expression Pattern that matches JSON Numbers. This is primarily used for
     *  output to guarantee that we are always writing valid JSON.
     */
    static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");

    public static JSONNumber fromJSONRaw(String valueString) {
        valueString = valueString.trim();
        if(!JSON_NUMBER_PATTERN.matcher(valueString).matches()) {
            throw new IllegalArgumentException("Not of number pattern: " + valueString);
        }

        if(valueString.contains(".")) {
            //Treat the valueString as float if it contains '.'
            try {
                Double d = Double.valueOf(valueString);
                return new JSONNumber(d);
            } catch (NumberFormatException ne) {
                BigDecimal bigDecimal = new BigDecimal(valueString);
                return new JSONNumber(bigDecimal);
            }
        } else {
            //Otherwise as integer
            try {
                Integer integer = Integer.valueOf(valueString);
                return new JSONNumber(integer);
            } catch (NumberFormatException ne) {
                BigInteger bigInteger = new BigInteger(valueString);
                return new JSONNumber(bigInteger);
            }
        }
    }


    public JSONNumber(Number number) {
        super(number);

        checkNotNull(number, "null shall not be used to create JSONNumber instance.");
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
