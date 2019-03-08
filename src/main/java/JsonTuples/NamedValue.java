package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Name value pair contained by JSONObject. Each name is followed by : (colon) and the name/value pairs are separated by , (comma)
 */
public class NamedValue extends Tuple2<String, IJSONValue> implements IJSONable {
    public static final Character Colon = ':';
    public static final Character Comma = ',';

    public static final Pattern NAME_PATTERN = Pattern.compile("\\\"(.*?)\\\":(.*)", Pattern.MULTILINE);
    public static final Pattern SIMPLE_VALUE_PATTERN = Pattern.compile("\\\"(.*?)\\\":\\s*?(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");

    public static NamedValue parse(String nameValueString) {
        Matcher matcher = NAME_PATTERN.matcher(nameValueString);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("No name matched");
        }

        String name = matcher.group(1);
        IJSONValue value = JSONValue.parse(matcher.group(2));
        return new NamedValue(name, value);
    }


    protected NamedValue(String name, IJSONValue value) {
        super(name, value);
    }

    public String getName() {
        return getFirst();
    }

    public Object getValue() {
        return getSecond().getObject();
    }

    @Override
    public String toJSONString(int indentFactor) {
        return String.format("%s\"%s\": %s",
                IJSONable.getIndent(indentFactor),
                getFirst(),
                getSecond().toJSONString(indentFactor + 1));
    }
}

