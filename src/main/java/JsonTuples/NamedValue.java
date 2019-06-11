package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Name value pair contained by JSONObject. Each name is followed by : (colon) and the name/value pairs are separated by , (comma)
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class NamedValue extends Tuple2<JSONString, IJSONValue>
        implements IJSONable, ISortable {

    //TODO: check if name can contain escaped quotes?
    //Where VALUE could be: true, false, null, "STRING", number
    public static final Pattern SIMPLIFIED_NAME_PATTERN = Pattern.compile("\\\"(.*?)\\\":\\s*?(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");
    public static final Pattern DEFAULT_NAMED_VALUE_PATTERN = Pattern.compile("(\\\".*?(?!\\\\)\\\"): (.*)$");

    //Regex to match simple JSON name-value pair in form of "NAME": VALUE
    public static final Pattern NAME_VALUE_PATTERN = Pattern.compile("(?m)\\\"(.*?)\\\":([\\s\\S]*)");

    /**
     * Use for testing purpose only when there could be some discrepencies! Parse the given string as a {@code NamedValue} instance.
     * @param nameValueString   JSON name-value pair in form of "NAME": VALUE
     * @return  Parsed NamedValue instance with 2 elements, the first is
     */
    public static NamedValue parse(String nameValueString) {
        Matcher matcher = NAME_VALUE_PATTERN.matcher(nameValueString);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("No name matched");
        }

        String name = matcher.group(1);
        IJSONValue value = Parser.parse(matcher.group(2));
        return new NamedValue(name, value);
    }

    protected NamedValue(JSONString name, IJSONValue value) {
        super(name, value);
    }

    protected NamedValue(String nameString, IJSONValue value) {
        super(new JSONString(nameString), value);
    }

    public String getName() {
        return getFirst().getFirst();
    }

    public Object getValue() {
        return getSecond().getObject();
    }

    @Override
    public String toJSONString(String indent) {
        if ("".equals(indent)) {
            return toString();
        } else if (indent == null) {
            return getFirst() + ":" + getSecond().toJSONString(null);
        } else {
            return indent + getFirst() + ": " + getSecond().toJSONString(indent);
        }
    }

    @Override
    public int getLeafCount(boolean countNulls) {
        return getSecond().getLeafCount(countNulls);
    }

    @Override
    public int getLeafCount() {
        return getSecond().getLeafCount();
    }

    @Override
    public String toString() {
        if (_toString == null) {
            _toString = getFirst().toString() + ": " + getSecond().toString();
        }
        return _toString;
    }

    @Override
    public NamedValue getSorted(Comparator<String> comparator) {
        IJSONValue second = getSecond();
        IJSONValue sortedValue = second.getSorted(comparator);

        return sortedValue == second ? this : new NamedValue(getFirst(), sortedValue);
    }
}

