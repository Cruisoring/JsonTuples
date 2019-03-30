package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends Tuple<IJSONValue> implements IJSONValue {

    public static final JSONArray EMPTY = new JSONArray();

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    public static JSONArray parseArray(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    final Lazy<String> toStringLazy = new Lazy<>(() -> toJSONString(""));

    protected JSONArray(IJSONValue... values) {
        super(values);
    }

    protected Lazy<Object[]> arrayLazy = new Lazy<>(() -> Arrays.stream(asArray())
            .map(IJSONValue::getObject)
            .toArray());

    @Override
    public Object getObject() {
        return arrayLazy.getValue();
    }

    @Override
    public IJSONValue deltaWith(IJSONValue other) {
        checkNotNull(other);

        if(other instanceof JSONArray){

        }
        return null;
    }

    @Override
    public IJSONValue deltaWith(Comparator<String> comparator, IJSONValue other) {
        return null;
    }

    @Override
    public String toJSONString(String indent) {
        checkState(StringUtils.isBlank(indent));

        String noIndent = toString();
        if(values.length == 0 || "".equals(indent)){
            return noIndent;
        }

        String indented = indent==null ?
                noIndent.replaceAll("(?m)\\n\\s*", "")
                : noIndent.replaceAll("(?m)\\n", NEW_LINE+indent);
        return indented;
    }

    @Override
    public String toString() {
        if(_toString == null){
            if(values.length == 0) {
                _toString = "[]";
            } else {
                TextStringBuilder sb = new TextStringBuilder();
                sb.append(LEFT_BRACKET + NEW_LINE+SPACE);
                List<String> valueStrings = Arrays.stream(values)
                        .map(v -> v.toJSONString(SPACE))
                        .collect(Collectors.toList());
                sb.appendWithSeparators(valueStrings, COMMA+NEW_LINE+SPACE);
                sb.append(NEW_LINE+RIGHT_BRACKET);
                _toString = sb.toString();
            }
        }
        return _toString;
    }
}
