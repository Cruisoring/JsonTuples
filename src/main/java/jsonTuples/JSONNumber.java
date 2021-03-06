package jsonTuples;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
 * A number is very much like a C or Java number, except that the octal and hexadecimal formats are not used.
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class JSONNumber extends JSONValue<Number> {

    //Determine if the String shall be matched with JSON_NUMBER_PATTERN to ensure it can be parsed as a number
    public static boolean validateJSONNumber = false;

    /**
     * Regular Expression Pattern that matches JSON Numbers. This is primarily used for
     * output to guarantee that we are always writing valid JSON.
     */
    static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("^-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");

    static final Pattern TRAILING_ZEROS_PATTERN = Pattern.compile("(?<=\\.)(\\d*?[1-9]+)?0*");

    protected JSONNumber(Number number) {
        super(number);
    }

    /**
     * By assuming the given {@code valueString} representing a number, parse it as a {@code JSONNumber}
     * @param valueString   the String representing a number
     * @return      a {@code JSONNumber} containing only one element to represent the number of type
     *      of {@code Integer}, {@code BigInteger}, {@code Double} or {@code BigDecimal}
     */
    public static JSONNumber parseNumber(String valueString) {
        valueString = valueString.trim();

//        assertTrue(JSON_NUMBER_PATTERN.matcher(valueString).matches(), "Not of number pattern: %s", valueString);

        //Assume the valueString represent float number if it contains '.', 'e' or 'E'
        if (valueString.contains(".") || StringUtils.containsIgnoreCase(valueString, "e")) {
            Double dbl = Double.valueOf(valueString);
            String dblString = TRAILING_ZEROS_PATTERN.matcher(dbl.toString()).replaceFirst("$1");
            String trailingZerosTrimmed = TRAILING_ZEROS_PATTERN.matcher(valueString).replaceFirst("$1");
            if (trailingZerosTrimmed.endsWith(dblString)) {
                return new JSONNumber(dbl);
            } else {
                BigDecimal bigDecimal = new BigDecimal(valueString);
                return new JSONNumber(bigDecimal);
            }
        } else {
            //Otherwise as integer first that shall be enough for most cases
            try {
                Integer integer = Integer.valueOf(valueString);
                return new JSONNumber(integer);
            } catch (NumberFormatException ignored) {
                //a second chance to parse the string as BigInteger
                BigInteger bigInteger = new BigInteger(valueString);
                return new JSONNumber(bigInteger);
            }
        }
    }

}
