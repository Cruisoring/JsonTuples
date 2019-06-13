package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.throwables.SupplierThrowable;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple4;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.function.Consumer;

import static io.github.cruisoring.Asserts.*;

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

    public static SupplierThrowable<Comparator<String>> defaultComparatorSupplier = () -> new OrdinalComparator<>();
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
     * @param comparator    the name comparator used to sort the {@code NamedValues} with fixed orders.
     * @param jsonText the whole JSON text to be parsed.
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(Comparator<String> comparator, CharSequence jsonText) {
        assertNotNull(jsonText);

        Parser parser = new Parser(comparator, jsonText, Range.ofLength(jsonText.length()));
        return parser.parse();
    }

    /**
     * To parse a part of the given JSON text to an {@code IJSONValue} with elements of {@code JSONObject} in fixed order.
     *
     * @param comparator    the name comparator used to sort the {@code NamedValues} with fixed orders.
     * @param jsonText the whole JSON text to be parsed.
     * @param range the {@code Range} of the concerned portion to be parsed
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(Comparator<String> comparator, CharSequence jsonText, Range range) {
        assertNotNull(jsonText, range);

        Parser parser = new Parser(comparator, jsonText, range);
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
    Parser(Comparator<String> comparator, CharSequence jsonText, Range range) {
        this(comparator, Range.subString(jsonText, range));
    }

    Parser(Comparator<String> comparator, CharSequence jsonText) {
        this.nameComparator = comparator;
        this.jsonContext = jsonText;
        length = jsonContext.length();
        if(comparator instanceof OrdinalComparator){
            OrdinalComparator<String> ordinalComparator = (OrdinalComparator)comparator;
            nameConsumer = name -> ordinalComparator.putIfAbsent(name);
        } else {
            nameConsumer = null;
        }
    }

    Parser(CharSequence jsonText) {
        this(defaultComparatorSupplier==null ? null : defaultComparatorSupplier.tryGet(), jsonText);
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
        int position = -1;
        try {
            for (position = 0; position < length; position++) {
                char currentChar = jsonContext.charAt(position);

                //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
                if (currentChar == QUOTE) {
                    boolean isEscaped = false;
                    for (int j = position - 1; j >= 0; j--) {
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
                    tuple = processControl(isObject, children, currentChar, position);
                    isObject = tuple.getFirst();
                    continue;
                }

                //Check if the position is within the scope of the current JSONString
                if (isInString || !CONTROLS.contains(currentChar)) {
                    //When the String has not ended, or not any control char
                    continue;
                }

                tuple = processControl(isObject, children, currentChar, position);
                isObject = tuple.getFirst();
            }
            assertTrue(children.isEmpty(), "%s is not closed properly?", isObject?"JSONObject":"JSONArray");
            assertFalse(isInString, "JSONString is not enclosed properly");
        } catch (Exception e) {
            position = position > length - 1 ? length - 1 : position;
            try {
                String message = e.getMessage();
                int last = Math.max(0, lastPosition - 20);
                String beforeLast = lastPosition - last > 1 ? Range.closedOpen(last, lastPosition).subString(jsonContext) : "";
                String afterPosition = position >= length - 1 ? "" : Range.open(position, Math.min(position + 10, length)).subString(jsonContext);
                String between = Range.closed(lastPosition < 0 ? 0 : lastPosition, position).subString(jsonContext);
                Logger.D(e.getClass().getSimpleName() + (message == null ? "" : "("+message+")") + ": >>>" + beforeLast + "%s" + afterPosition + "<<<", between);
            } catch (Exception e2) {
                Logger.D(e.getClass().getSimpleName() + ": >>>%s<<<",
                        Range.closed(lastPosition<0?0:lastPosition, position).subString(jsonContext));
            }
            throw e;
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
        //The 4 values of state: isParentObject (Boolean), children elements, name JSONString, isCurrentObject
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
                checkState(!contextStack.isEmpty(), "Missing '%s' to pair '%s'", LEFT_BRACE, RIGHT_BRACE);
                checkState(isObject, "No matching '%s' for this '%s]?", LEFT_BRACE, RIGHT_BRACE);
                switch (lastControl) {
                    case COLON:
                        valueElement = asSimpleValue(Range.open(lastPosition, position)); //Simple value
                        nameElement = lastName;
                        namedValueElement = new NamedValue(nameElement, valueElement);
                        lastName = null;
                        children.add(namedValueElement);
                        break;
                    case QUOTE:
                        nameElement = lastName;
                        valueElement = lastStringValue;
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
                    children.add(new NamedValue(name, closedValue));
                } else {
                    checkStates(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case RIGHT_BRACKET: //Close of current JSONArray
                checkState(!contextStack.isEmpty(), "Missing '%s' to pair '%s'", LEFT_BRACKET, RIGHT_BRACKET);
                checkState(!isObject, "No matching '%s' for this '%s]?", LEFT_BRACKET, RIGHT_BRACKET);
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
                lastName = checkNotNull(lastStringValue, "Fail to enclose \"%s\" with quotation marks before ':'",
                        Range.open(lastPosition, position).subString(jsonContext));
                if(nameConsumer != null) {
                    nameConsumer.accept(lastName.getFirst());
                }
                lastStringValue = null;
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    currentStringEnd = position;
                    String text = StringEscapeUtils.unescapeJson(jsonContext.subSequence(currentStringStart+1, currentStringEnd).toString());
                    lastStringValue = new JSONString(text);
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
        try {
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
        } catch (Exception e) {
            String message = StringHelper.tryFormatString("Cannot parse \"%s\" between '%s' and '%s' as JSONValue",
                    range.subString(jsonContext), jsonContext.charAt(range.getStartExclusive()), jsonContext.charAt(range.getEndExclusive()));
            throw new IllegalStateException(message);
        }
    }
    //endregion
}