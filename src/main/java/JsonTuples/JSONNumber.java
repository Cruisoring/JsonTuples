package JsonTuples;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

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
            Double dbl = Double.valueOf(valueString);
            String dblString = dbl.toString();
            if(dblString.equals(valueString)) {
                return new JSONNumber(dbl);
            } else {
                BigDecimal bigDecimal = new BigDecimal(valueString);
                return bigDecimal.toString().equals(dblString) ? new JSONNumber(dbl) : new JSONNumber(bigDecimal);
            }
        } else {
            //Otherwise as integer first
            try {
                Integer integer = Integer.valueOf(valueString);
                return new JSONNumber(integer);
            } catch (NumberFormatException ne) {
                //Or bigInteger when integer cannot keep it
                BigInteger bigInteger = new BigInteger(valueString);
                return new JSONNumber(bigInteger);
            }
        }
    }

    protected JSONNumber(Number number) {
        super(number);
    }

}
