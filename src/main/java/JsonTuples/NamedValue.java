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

    //TODO: check if name can contain escaped quotes?
    public static final Pattern SIMPLIFIED_NAME_PATTERN = Pattern.compile("\\\"(.*?)\\\":(.*)", Pattern.MULTILINE);

    //Regex to match simple JSON name-value pair in form of "NAME": VALUE
    //Where VALUE could be: true, false, null, "STRING", number
    public static final Pattern NAME_VALUE_PATTERN = Pattern.compile("\\\"(.*?)\\\":\\s*?(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");

    /**
     * TODO: parse NameValue pair with more efficient means
     * @param nameValueString   JSON name-value pair in form of "NAME": VALUE
     * @return  Parsed NamedValue instance with 2 elements, the first is
     */
    public static NamedValue parse(String nameValueString) {
        Matcher matcher = SIMPLIFIED_NAME_PATTERN.matcher(nameValueString);

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

    public IJSONValue getValue() {
        return getSecond();
    }

    @Override
    public String toJSONString(String indent) {
        IJSONValue value = getSecond();
        return String.format("\"%s\": %s",
                getFirst(),
                value.toJSONString(indent + SPACE));
    }

    @Override
    public String toString(){
        return toJSONString("");
    }
}

