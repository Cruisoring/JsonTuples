package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.TupleSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * http://www.json.org
 * An array is an ordered collection of values. An array begins with [ (left bracket) and ends with ] (right bracket). Values are separated by , (comma).
 */
public class JSONArray extends TupleSet<IJSONValue> implements IJSONValue {

    //Pattern of string to represent a solid JSON Array
    public static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\[[\\s\\S]*?\\]$", Pattern.MULTILINE);

    public static JSONArray parseArray(String valueString) {
        return (JSONArray) Parser.parse(valueString);
    }

    final Lazy<String> toStringLazy = new Lazy<>(() -> toJSONString(""));

    protected JSONArray(IJSONValue... values) {
        super(values);
    }

    @Override
    public String toJSONString(String indent) {
        int length = getLength();
        if(length == 0) {
            return "[]";
        }

        checkState(StringUtils.isBlank(indent));

        TextStringBuilder sb = new TextStringBuilder();
        sb.append(LEFT_BRACKET + (indent==null?"":NEW_LINE+SPACE+indent));
        final String valueIndent = indent == null ? null : SPACE+indent;
        List<String> valueStrings = Arrays.stream(asArray())
                .map(v -> v.toJSONString(valueIndent))
                .collect(Collectors.toList());
        sb.appendWithSeparators(valueStrings, indent==null?", ":COMMA+NEW_LINE+indent+SPACE);
        sb.append(indent==null ? RIGHT_BRACKET : NEW_LINE+indent+RIGHT_BRACKET);
        return sb.toString();
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
    public String toString() {
        return toJSONString("");
    }

    @Override
    public int compareTo(Tuple o) {
        return toString().compareTo(o.toString());
    }
}
