package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Set;
import io.github.cruisoring.tuple.Tuple2;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends Set<NamedValue> implements IJSONValue {

    //Pattern of string to represent a solid JSON Object
    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);

    private static final Pattern LINE_STARTS = Pattern.compile("^" + SPACE, Pattern.MULTILINE);

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }

    public static JSONObject parse(String valueString) {
        return null;
    }

    final TupleMap<String, IJSONValue> map = new TupleMap<>();

    final Lazy<Set<Tuple2<String, IJSONValue>>> tuples = new Lazy<>(map::asTupleSet);

    protected JSONObject(NamedValue... namedValues) {
        super(checkNotNull(namedValues));

        for (NamedValue namedValue : namedValues) {
            //Assume JSON handle duplicated NamedValues by keeping only the value of latest copies
            //TODO: Add static flag to handle them in another way
            map.put(namedValue.getName(), namedValue.getValue());
        }
    }

    @Override
    public String toJSONString(String indent) {
        return map.toString(indent);
    }

    @Override
    public Object getObject() {
        return map;
    }

    @Override
    public String toString() {
        return toJSONString("");
    }
}
