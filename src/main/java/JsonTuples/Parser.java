package JsonTuples;

import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple4;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility to parse JSON text based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {

    /**
     * Unique method to parse the given qualified JSON text to an IJSONValue.
     *
     * @param jsonText JSON text to be parsed.
     * @return An IJSONValue that is either JSONObject or JSONArray.
     */
    public static IJSONValue parse(String jsonText) {
        Parser parser = new Parser(jsonText);

        return parser.parse();
    }

    //region static variables
    public static final char START_JSON_SIGN = '^';

    public static final String JSON_NULL = "null";
    public static final String JSON_TRUE = "true";
    public static final String JSON_FALSE = "false";

    //Special keys to mark the value boundary or escaped sequences
    final static char LEFT_BRACE = '{';
    final static char RIGHT_BRACE = '}';
    final static char LEFT_BRACKET = '[';
    final static char RIGHT_BRACKET = ']';
    final static char COMMA = ',';
    final static char COLON = ':';
    final static char QUOTE = '"';
    final static char BACK_SLASH = '\\';

    final static Set<Character> CONTROLS = new HashSet<Character>(
            Arrays.asList(LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET, COMMA, COLON));

    final static Character MIN_CONTROL = Collections.min(CONTROLS);
    final static Character MAX_CONTROL = Collections.max(CONTROLS);
    //endregion

    //region Static methods

    protected static JSONObject createObject(IJSONable[] children) {
        NamedValue[] namedValues = Arrays.stream(children)
                .map(c -> (NamedValue) c).toArray(size -> new NamedValue[size]);
        JSONObject object = new JSONObject(namedValues);
        return object;
    }

    protected static JSONArray createArray(IJSONable[] children) {
        IJSONValue[] values = Arrays.stream(children)
                .map(c -> (IJSONValue) c).toArray(size -> new IJSONValue[size]);

        JSONArray array = new JSONArray(values);
        return array;
    }

    protected static IJSONValue asJSONValue(String valueString) {
        final String trimmed = checkNotNull(valueString).trim();
        switch (trimmed) {
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                //Switch based on the first character, leave the corresponding methods to validate and parse
                switch (trimmed.charAt(0)) {
                    case QUOTE:
                        return JSONString.parseString(trimmed);
                    case LEFT_BRACE:
                        return JSONObject.parse(trimmed);
                    case LEFT_BRACKET:
                        return JSONArray.parseArray(trimmed);
                    default:
                        //The valueString can only stand for a number or get Exception thrown there
                        return JSONNumber.parseNumber(trimmed);
                }
        }

    }
    //endregion

    //region instance variables
    public final CharSequence jsonContext;
    public final int length;

    char lastControl = START_JSON_SIGN;
    int lastPosition = -1;

    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;

    JSONString lastName = null, lastStringValue;

    Stack<Tuple4<Boolean, IJSONable[], JSONString, Boolean>> contextStack = new Stack<>();
    //endregion

    public Parser(String jsonText) {
        checkState(StringUtils.isNoneBlank(jsonText));

        this.jsonContext = jsonText;
        length = jsonContext.length();
    }

    /**
     * Extract the content of the specified Range as a String.
     *
     * @param range Range to be extracted.
     * @return SubString of the content relative to the saved jsonContext.
     */
    String subString(Range range) {
        checkNotNull(range);

        int end = range.getEndExclusive();
        return jsonContext.subSequence(range.getStartInclusive(), end > length ? length : end).toString();
    }

    protected IJSONValue asJSONValue(Range range) {
        String valueString = subString(range).trim();
        return asJSONValue(valueString);
    }

    Tuple2<Boolean, IJSONValue> processControl(Boolean isObject, List<IJSONable> children, char control, int position) {

        final JSONString nameElement;
        final IJSONValue valueElement;
        final NamedValue namedValueElement;

        IJSONValue closedValue = null;
        Tuple4<Boolean, IJSONable[], JSONString, Boolean> state = null;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
            case LEFT_BRACKET:  //Start of JSONArray
                nameElement = lastName;
                boolean enterObject = control == LEFT_BRACE;
                state = Tuple.create(isObject, children.toArray(new IJSONable[0]), nameElement, enterObject);
                isObject = enterObject;
                lastName = null;
                children.clear();
                contextStack.push(state);
                break;
            case RIGHT_BRACE:   //End of current JSONObject
                checkState(isObject);
                switch (lastControl) {
                    case COLON:
                        valueElement = asJSONValue(Range.open(lastPosition, position));
                        nameElement = lastName;
                        namedValueElement = new NamedValue(nameElement, valueElement);
                        lastName = null;
                        children.add(namedValueElement);
                        break;
                    case QUOTE:
                        valueElement = lastStringValue;
                        nameElement = lastName;
                        namedValueElement = new NamedValue(nameElement, valueElement);
                        lastName = null;
                        children.add(namedValueElement);
                        break;
                    default:
                        break;
                }

                closedValue = createObject(children.toArray(new IJSONable[0]));
                children.clear();

                state = contextStack.pop();
                if (state.getFirst() == null) {
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    checkNotNull(name);
                    children.add(new NamedValue(name, closedValue));
                } else {
                    checkState(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case RIGHT_BRACKET: //Close of current JSONArray
                checkState(!isObject);
                switch (lastControl) {
                    case COMMA:
                        valueElement = asJSONValue(Range.open(lastPosition, position));
                        children.add(valueElement);
                        break;
                    case LEFT_BRACKET:
                        String valueString = subString(Range.open(lastPosition, position));
                        if (!StringUtils.isBlank(valueString)) {
                            valueElement = asJSONValue(valueString);
                            children.add(valueElement);
                        }
                        break;
                    default:
                        break;
                }

                closedValue = createArray(children.toArray(new IJSONable[0]));
                children.clear();

                state = contextStack.pop();
                if (state.getFirst() == null) {
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    checkNotNull(name);
                    children.add(new NamedValue(name, closedValue));
                } else {
                    checkState(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
                switch (lastControl) {
                    case LEFT_BRACKET:
                    case COMMA:
                        valueElement = asJSONValue(Range.open(lastPosition, position));
                        children.add(valueElement);
                        break;
                    case COLON:
                        valueElement = asJSONValue(Range.open(lastPosition, position));
                        namedValueElement = new NamedValue(lastName, valueElement);
                        children.add(namedValueElement);
                        break;
                    case QUOTE:
                        if (isObject) {
                            namedValueElement = new NamedValue(lastName, lastStringValue);
                            children.add(namedValueElement);
                        }
                    default:
                        break;
                }
                break;
            case COLON:
                lastName = lastStringValue;
                lastStringValue = null;
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    currentStringEnd = position;
                    String string = jsonContext.subSequence(currentStringStart + 1, currentStringEnd).toString();
                    lastStringValue = new JSONString(string);
                    if (!isObject) {
                        children.add(lastStringValue);
                    }
                } else {
                    //This position is the start of a new String scope
                    currentStringStart = position;
                    currentStringEnd = Integer.MAX_VALUE;
                }
                break;

            default:
                break;
        }
        lastControl = control;
        lastPosition = position;
        return Tuple.create(isObject, closedValue);
    }

    /**
     * Parse the preloaded string as an IJSONValue that can be either JSONObject or JSONArray.
     *
     * @return The root node of either JSONObject or JSONArray.
     */
    IJSONValue parse() {
        Boolean isObject = null;
        List<IJSONable> children = new ArrayList<>();
        Tuple2<Boolean, IJSONValue> tuple = null;
        boolean isInString = false;

        //Identify the JSONString elements, save all control characters
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);

            //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
            if (current == QUOTE) {
                boolean isEscaped = false;
                for (int j = i - 1; j >= 0; j--) {
                    if (jsonContext.charAt(j) == BACK_SLASH) {
                        isEscaped = !isEscaped;
                    } else {
                        break;
                    }
                }
                if (isEscaped) {
                    //This Quote is escaped, not a control
                    continue;
                }

                isInString = !isInString;
                tuple = processControl(isObject, children, current, i);
                isObject = tuple.getFirst();
                continue;
            }

            //Check if the position is within the scope of the current JSONString
            if (isInString || current < MIN_CONTROL || current > MAX_CONTROL) {
                //When the String has not ended, or not any control char
                continue;
            }

            if (CONTROLS.contains(current)) {
                tuple = processControl(isObject, children, current, i);
                isObject = tuple.getFirst();
            }
        }
        return tuple.getSecond();
    }
}