package JsonTuples;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * A number is very much like a C or Java number, except that the octal and hexadecimal formats are not used.
 */
public class JSONNumber extends JSONValue<Number> {

    /**
     *  Regular Expression Pattern that matches JSON Numbers. This is primarily used for
     *  output to guarantee that we are always writing valid JSON.
     */
    static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");

    public static JSONNumber parseNumber(String valueString) {
        valueString = valueString.trim();
        if(!JSON_NUMBER_PATTERN.matcher(valueString).matches()) {
            throw new IllegalArgumentException("Not of number pattern: " + valueString);
        }

        if(valueString.contains(".") || StringUtils.containsIgnoreCase(valueString, "e")) {
            //Treat the valueString as bigDecimal if it contains '.'
            BigDecimal bigDecimal = new BigDecimal(valueString);
            return new JSONNumber(bigDecimal);
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
        super(checkNotNull(number));
    }
}
