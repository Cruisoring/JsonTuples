package JsonTuples;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public interface JSONValue extends JSONable {

    Object getObject();

    static final Map<String, JSONValue> simpleValues = new HashMap<String, JSONValue>() {{
        put("null", JSONNull.Null);
        put("true", JSONBoolean.True);
        put("false", JSONBoolean.False);
    }};

    static LinkedHashMap<Pattern, Function<String, JSONValue>> patternedParsers = new LinkedHashMap<Pattern, Function<String, JSONValue>>() {{
        put(JSONObject.JSON_OBJECT_PATTERN, JSONObject::fromJSONRaw);
        put(JSONArray.JSON_ARRAY_PATTERN, JSONArray::fromJSONRaw);
        put(JSONString.JSON_STRING_PATTERN, JSONString::fromJSONRaw);
        put(JSONNumber.JSON_NUMBER_PATTERN, JSONNumber::fromJSONRaw);
    }};

    static JSONValue parseValue(String valueString) {
        valueString = valueString.trim();

        if (simpleValues.containsKey(valueString)) {
            return simpleValues.get(valueString);
        }

        for (Map.Entry<Pattern, Function<String, JSONValue>> entry : patternedParsers.entrySet()) {
            if(entry.getKey().matcher(valueString).matches()) {
                return entry.getValue().apply(valueString);
            }
        }

        throw new IllegalArgumentException("Invalid String to get any JSON value.");
    }
}
