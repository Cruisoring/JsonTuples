package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Name value pair contained by JSONObject. Each name is followed by : (colon) and the name/value pairs are separated by , (comma)
 * @see <a href="http://www.json.org">http://www.json.org</a>
 */
public class NamedValue extends Tuple2<JSONString, IJSONValue>
        implements IJSONable, ISortable {

    //TODO: check if name can contain escaped quotes?
    public static final Pattern SIMPLIFIED_NAME_PATTERN = Pattern.compile("\\\"(.*?)\\\":(.*)", Pattern.MULTILINE);
    public static final Pattern DEFAULT_NAMED_VALUE_PATTERN = Pattern.compile("(\\\".*?(?!\\\\)\\\"): (.*)$");

    //Regex to match simple JSON name-value pair in form of "NAME": VALUE
    //Where VALUE could be: true, false, null, "STRING", number
    public static final Pattern NAME_VALUE_PATTERN = Pattern.compile("\\\"(.*?)\\\":\\s*?(true|false|null|\\\".*?\\\"|-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");

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
    public String toString() {
        if (_toString == null) {
            _toString = getFirst().toString() + ": " + getSecond().toString();
        }
        return _toString;
    }

    @Override
    public int hashCode() {
        if (_hashCode == null) {
            _hashCode = toString().hashCode();
        }
        return _hashCode;
    }

    @Override
    public NamedValue getSorted(Comparator<String> comparator) {
        IJSONValue sortedValue = getSecond().getSorted(comparator);
        if (sortedValue == getSecond()) {
            return this;
        } else {
            return new NamedValue(getFirst(), sortedValue);
        }
    }
}

