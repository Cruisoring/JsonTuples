package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends Set<IJSONValue> implements IJSONValue {

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    public static JSONArray parseArray(String valueString) {
        return null;
    }

    protected JSONArray(IJSONValue... values) {
        super(values);
    }

    @Override
    public String toJSONString(String indent) {
        int length = getLength();
        if(length == 0) {
            return "[]";
        }

        List<String> valueRows = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            String valueString = get(i).toJSONString().replaceAll(NEW_LINE, NEW_LINE+ SPACE);
            valueRows.add(String.format("%s%s%s", SPACE, valueString, i == length-1 ? "" : COMMA));
        }
        valueRows.add(0, "" + LEFT_BRACKET);
        valueRows.add("" + RIGHT_BRACKET);

        String string = String.join(NEW_LINE+indent, valueRows);
        return string;
    }

    protected Lazy<Object[]> arrayLazy = new Lazy<>(() -> Arrays.stream(asArray())
            .map(IJSONValue::getObject)
            .toArray());

    @Override
    public Object getObject() {
        return arrayLazy.getValue();
    }

    @Override
    public String toString() {
        return toJSONString("");
    }
}
