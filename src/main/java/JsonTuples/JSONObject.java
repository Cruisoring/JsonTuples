package JsonTuples;

import io.github.cruisoring.tuple.Set;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * http://www.json.org
 * An object is an unordered set of name/value pairs. An object begins with { (left brace) and ends with } (right brace). Each name is followed by : (colon) and the name/value pairs are separated by , (comma).
 */
public class JSONObject extends Set<NamedValue> implements IJSONValue {
    public static Character LeftBrace = '{';
    public static Character RightBrace = '}';

    public static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\{[\\s\\S]*?\\}$", Pattern.MULTILINE);

    public static IJSONValue parse(String jsonContext, Range range) {
        checkNotNull(jsonContext);
        checkNotNull(range);

        String valueString = jsonContext.substring(range.getStartInclusive(), range.getEndExclusive()).trim();
        return parse(valueString);
    }


    public static JSONObject parse(String valueString) {
        return null;
    }

    private final Map<String, IJSONValue> map = new HashMap<>();

    protected JSONObject(NamedValue... namedValues) {
        super(checkNotNull(namedValues));

        for (NamedValue namedValue :
                namedValues) {
            map.put(namedValue.getName(), namedValue.getValue());
        }
    }

    @Override
    public String toJSONString(int indentFactor) {
        int length = getLength();

        if(length == 0) {
            return "{}";
        }

        //JSON string starts with '{'
        StringBuilder sb = new StringBuilder(LeftBrace);

        for (int i = 0; i < length; i++) {
            NamedValue kvp = get(i);
            sb.append(kvp.toJSONString(indentFactor+1));
            sb.append(NewLine);
        }

        //JSON String ends with ']'
        sb.append(IJSONable.getIndent(indentFactor) + RightBrace);
        return sb.toString();

    }

    @Override
    public Object getObject() {
        return asArray();
    }

    @Override
    public String toString() {
        return toJSONString(0);
    }
}
