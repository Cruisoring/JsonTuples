package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple4;
import io.github.cruisoring.utility.ArrayHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;

import static io.github.cruisoring.Asserts.assertNotNull;
import static io.github.cruisoring.Asserts.checkStates;
import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
 * Utility to parse JSON text based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {

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
    /**
     * To parse the given qualified JSON text to an {@code IJSONValue}
     *
     * @param jsonText JSON text to be parsed.
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(CharSequence jsonText) {
        Parser parser = new Parser(jsonText);

        return parser.parse();
    }

    /**
     * To parse the given JSON text to an {@code IJSONValue} with elements of {@code JSONObject} in fixed order.
     *
     * @param jsonText the whole JSON text to be parsed.
     * @param comparator    the name comparator used to sort the {@code NamedValues} with fixed orders.
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(CharSequence jsonText, Comparator<String> comparator) {
        assertNotNull(jsonText, comparator);

        Parser parser = new Parser(jsonText, Range.ofLength(jsonText.length()), comparator);
        return parser.parse();
    }

    /**
     * To parse a part of the given JSON text to an {@code IJSONValue} with elements of {@code JSONObject} in fixed order.
     *
     * @param jsonText the whole JSON text to be parsed.
     * @param comparator    the name comparator used to sort the {@code NamedValues} with fixed orders.
     * @param range the {@code Range} of the concerned portion to be parsed
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(CharSequence jsonText, Comparator<String> comparator, Range range) {
        assertNotNull(comparator, jsonText, range);

        Parser parser = new Parser(jsonText, range, comparator);
        return parser.parse();
    }
    //endregion

    //region instance variables
    public final CharSequence jsonContext;
    public final int length;
    public final Comparator<String> nameComparator;
    final Consumer<String> nameConsumer;
    char lastControl = START_JSON_SIGN;
    int lastPosition = -1;
    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;
    JSONString lastName = null, lastStringValue;
    Stack<Tuple4<Boolean, IJSONable[], JSONString, Boolean>> contextStack = new Stack<>();
    //endregion

    //region Constructors
    Parser(CharSequence jsonText, Range range, Comparator<String> comparator) {
        this(Range.subString(jsonText, range), comparator);
    }

    Parser(CharSequence jsonText, Comparator<String> comparator) {
        this.nameComparator = comparator;
        this.jsonContext = jsonText;
        length = jsonContext.length();
        if(comparator instanceof OrdinalComparator){
            OrdinalComparator<String> ordinalComparator = (OrdinalComparator)comparator;
            nameConsumer = name -> ordinalComparator.putIfAbsent(name);
        } else {
            nameConsumer = name -> {};
        }
    }

    Parser(CharSequence jsonText) {
        this(jsonText, null);
    }
    //endregion

    //region Instance methods
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

        //If there is no single control character to get tuple, it must be a JSONValue
        if(tuple == null || tuple.getFirst() == null) {
            return JSONValue.parse(jsonContext, Range.ofLength(jsonContext.length()));
        } else {
            return tuple.getSecond();
        }
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
                checkStates(isObject);
                switch (lastControl) {
                    case COLON:
                        valueElement = asSimpleValue(Range.open(lastPosition, position)); //Simple value
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

                closedValue = createObject(children);
                children.clear();

                state = contextStack.pop();
                if (state.getFirst() == null) {
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    checkWithoutNull(name);
                    children.add(new NamedValue(name, closedValue));
                } else {
                    checkStates(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case RIGHT_BRACKET: //Close of current JSONArray
                checkStates(!isObject);
                switch (lastControl) {
                    case COMMA:
                        valueElement = asSimpleValue(Range.open(lastPosition, position));     //Simple value
                        children.add(valueElement);
                        break;
                    case LEFT_BRACKET:  //Empty Array or with only one simple value
                        String valueString = Range.open(lastPosition, position).subString(jsonContext);
                        if (!StringUtils.isBlank(valueString)) {
                            valueElement = asSimpleValue(Range.open(lastPosition, position)); //Simple value
                            children.add(valueElement);
                        }
                        break;
                    default:
                        break;
                }

                closedValue = createArray(children);
                children.clear();

                state = contextStack.pop();
                if (state.getFirst() == null) {
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    checkWithoutNull(name);
                    children.add(new NamedValue(name, closedValue));
                } else {
                    checkStates(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
                switch (lastControl) {
                    case LEFT_BRACKET:
                    case COMMA:
                        valueElement = asSimpleValue(Range.open(lastPosition, position));     //Simple value
                        children.add(valueElement);
                        break;
                    case COLON:
                        valueElement = asSimpleValue(Range.open(lastPosition, position));     //Simple value
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
                nameConsumer.accept(lastName.getFirst());
                lastStringValue = null;
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    currentStringEnd = position;
                    lastStringValue = new JSONString(JSONString._unescapeJson(jsonContext, currentStringStart + 1, currentStringEnd));
                    if (isObject != null && !isObject) {
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

    protected JSONObject createObject(List<IJSONable> children) {
        NamedValue[] namedValues = (NamedValue[]) ArrayHelper.create(NamedValue.class, children.size(), i -> (NamedValue)children.get(i));
        JSONObject object = new JSONObject(nameComparator, namedValues);
        return object;
    }

    protected JSONArray createArray(List<IJSONable> children) {
        IJSONValue[] values = children.toArray(new IJSONValue[0]);
        JSONArray array = new JSONArray(nameComparator, values);
        return array;
    }

    protected IJSONValue asSimpleValue(Range range) {
        final String trimmed = range.subString(jsonContext).trim();
        switch (trimmed) {
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                //The valueString can only stand for a number or get Exception thrown there
                return JSONNumber.parseNumber(trimmed);
        }
    }
    //endregion
}