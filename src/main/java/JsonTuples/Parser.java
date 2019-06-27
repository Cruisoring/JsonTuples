package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.throwables.SupplierThrowable;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple5;
import io.github.cruisoring.utility.SetHelper;
import io.github.cruisoring.utility.SimpleTypedList;
import io.github.cruisoring.utility.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;

import static io.github.cruisoring.Asserts.*;

/**
 * Utility to parse JSON text of either JSONArray or JSONObject based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {

    public static int JSON_TEXT_LENGTH_TO_LOG = 200;
    public static final int DEFAULT_CAPCITY = 128;
    public static final int MEGABYTE = 1024 * 1024;

    //region static variables
    public static final char START_JSON_SIGN = '^';
    public static final String JSON_NULL = "null";
    public static final String JSON_TRUE = "true";
    public static final String JSON_FALSE = "false";

    //Special keys to mark the value boundary or escaped sequences
    static final char LEFT_BRACE = '{';
    static final char RIGHT_BRACE = '}';
    static final char LEFT_BRACKET = '[';
    static final char RIGHT_BRACKET = ']';
    static final char COMMA = ',';
    static final char COLON = ':';
    static final char QUOTE = '"';
    static final char BACK_SLASH = '\\';
    static final Set<Character> CONTROLS = SetHelper.asHashSet(LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET, COMMA, COLON);

    static final String SPACE = "  ";
    static final String NEW_LINE = "\n";
    static final String COMMA_NEWLINE = COMMA + NEW_LINE;

    public static SupplierThrowable<Comparator<String>> defaultComparatorSupplier = OrdinalComparator::new;
    //endregion

    //region Static methods
    /**
     * To parse the given qualified JSON text to an {@code IJSONValue}
     *
     * @param jsonText JSON text to be parsed that represent a JSONObject or JSONArray.
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parse(CharSequence jsonText) {
        Parser parser = new Parser(null, jsonText);

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
        if(jsonText==null){
            throw new NullPointerException("The jsonText cannot be null");
        }

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
    public static IJSONValue parseRange(Comparator<String> comparator, CharSequence jsonText, Range range) {
        if(jsonText==null || range == null){
            throw new NullPointerException("The jsonText and range cannot be null");
        } else if (range.getStartInclusive() < 0 || range.getEndExclusive() > jsonText.length()) {
            throw new IndexOutOfBoundsException();
        }

        Parser parser = new Parser(comparator, jsonText, range);
        return parser.parse();
    }

    /**
     * To parse a part of the given JSON text to an {@code IJSONValue} with original order.
     *
     * @param jsonText the whole JSON text to be parsed.
     * @param range    the {@code Range} of the concerned portion to be parsed
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parseRange(CharSequence jsonText, Range range) {
        return parseRange(null, jsonText, range);
    }

    /**
     * To parse a part of the given JSON text to an {@code IJSONValue} with original order.
     *
     * @param jsonText      the whole JSON text to be parsed.
     * @param fromInclusive the start positon of the concerned portion to be parsed
     * @param toExclusive   the end positon of the concerned portion to be parsed
     * @return An {@code IJSONValue} that is usually either JSONObject or JSONArray.
     */
    public static IJSONValue parseRange(CharSequence jsonText, int fromInclusive, int toExclusive) {
        return parseRange(null, jsonText, Range.closedOpen(fromInclusive, toExclusive));
    }
    //endregion

    //region Constructors
    Parser(Comparator<String> comparator, CharSequence jsonText, Range range) {
        this.nameComparator = comparator;
        this.jsonContext = jsonText;
        jsonRange = range;
        startInclusive = jsonRange.getStartInclusive();
        endExclusive = jsonRange.getEndExclusive();
        length = endExclusive - startInclusive;
    }

    Parser(Comparator<String> comparator, CharSequence jsonText) {
        this(comparator, jsonText, Range.ofLength(jsonText.length()));
    }
    //endregion

    //region instance variables
    final CharSequence jsonContext;
    final Range jsonRange;
    final int startInclusive;
    final int endExclusive;
    final int length;
    final Comparator<String> nameComparator;

    //isObject acts as triple-state flag to indicate if the context under parsing is in an Object (true), an Array (false) or None of them (null).
    Boolean isObject = null;
    //lastControl keeps the last control character, '^' means there is no last control character recorded
    char lastControl = START_JSON_SIGN;
    //lastControlPosition keeps the position of the last control character, -1 means there is no last control character recorded
    int lastControlPosition = -1;
    //currentStringStart and currentStringEnd are used to record the start and end quotation marks of the current JSONString
    int currentStringStart = Integer.MIN_VALUE;
    //lastName and lastStringValue are used to keep the recent JSONString values for further evaluations
    JSONString lastName = null;
    JSONString lastStringValue;
    IJSONValue closedValue = null;
    /**
     * Use this simple Stack to keep parsing results of parent nodes
     * The meaning of the 4 values and their type:
     * First (Boolean): <tt>null</tt> means there is no parent node, <tt>true</tt> means parent node is a JSONObject, otherwise it is a JSONArray
     * Second (Integer): Position of the opening control for the parent node, '{' or '[' for JSONObject or JSONArray respectively
     * Third (JSONString): name of current JSONObject/JSONArray in their parent nodes, must not be <tt>null</tt> if  the parent is JSONObject, otherwise must be null
     * Fourth(NamedValue[] and IJSONValue[]): null when there is no parent node, or children array of the parent JSONObject or JSONArray respectively
     * Fifth (Integer): cursor of the parent JSONObject or JSONArray, -1 means there is no parent
     */
    SimpleTypedList<Tuple5<Boolean, Integer, JSONString, Object[], Integer>> stateStack = new SimpleTypedList<>();
    Tuple5<Boolean, Integer, JSONString, Object[], Integer> state = null;

    int capacity = DEFAULT_CAPCITY;
    Object[] children = null;
    int nextChildIndex = -1;

    //endregion

    //region Instance methods

    private void printError(int position, Exception e) {
        try {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.length() > 120) {
                message = message.substring(0, 120) + "...";
            }
            Range problemRange = Range.closed(lastControlPosition, position).intersectionWith(jsonRange);
            Range beforeProblem = Range.open(lastControlPosition - JSON_TEXT_LENGTH_TO_LOG / 2, lastControlPosition).intersectionWith(jsonRange);
            Range afterProblem = Range.open(position, position + JSON_TEXT_LENGTH_TO_LOG / 2).intersectionWith(jsonRange);

            String summary = String.format("%s(%s): ", e.getClass().getSimpleName(), message);
            if (problemRange.size() > JSON_TEXT_LENGTH_TO_LOG) {
                if (problemRange.getStartInclusive() == 0) {
                    Logger.D(summary + "%s...%s<<<[%d]" + subString(afterProblem),
                            subString(problemRange.getStartInclusive(), problemRange.getStartInclusive() + JSON_TEXT_LENGTH_TO_LOG / 2 + 1),
                            subString(problemRange.getEndInclusive() - JSON_TEXT_LENGTH_TO_LOG / 2, problemRange.getEndInclusive() + 1),
                            problemRange.getEndInclusive());
                } else {
                    Logger.D(summary + (beforeProblem.getStartInclusive() == 0 ? "" : "...") + subString(beforeProblem) + "[%d]>>>" + "%s...%s<<<[%d]" + subString(afterProblem),
                            problemRange.getStartInclusive(),
                            subString(problemRange.getStartInclusive(), problemRange.getStartInclusive() + JSON_TEXT_LENGTH_TO_LOG / 2 + 1),
                            subString(problemRange.getEndInclusive() - JSON_TEXT_LENGTH_TO_LOG / 2, problemRange.getEndInclusive() + 1),
                            problemRange.getEndInclusive());
                }
            } else if (problemRange.getStartInclusive() == 0) {
                Logger.D(summary + "%s<<<[%d]" + subString(afterProblem),
                        subString(problemRange), problemRange.getEndInclusive());
            } else {
                Logger.D(summary + (beforeProblem.getStartInclusive() == 0 ? "" : "...") + subString(beforeProblem) + "[%d]>>>" + "%s<<<[%d]" + subString(afterProblem),
                        problemRange.getStartInclusive(), subString(problemRange), problemRange.getEndInclusive());
            }
        } catch (Exception e2) {
            Logger.D(e.getClass().getSimpleName() + ": >>>%s<<<",
                    Range.closed(lastControlPosition < 0 ? 0 : lastControlPosition, position).subString(jsonContext));
        }
    }

    /**
     * With syntax tolerance, parse the concerned CharSequence as an IJSONValue that can be either a JSONObject, JSONArray or simply JSONValue.
     *
     * @return The root node of either JSONObject or JSONArray.
     */
    IJSONValue parse() {
        IJSONValue value = null;

        //Identify the JSONString elements, save all control characters
        int position = -1;
        lastControlPosition = startInclusive-1;
        //isInString signals if the parser has entered a scope with previous control of Quotation mark
        boolean isInString = false;
        try {
            for (position = startInclusive; position < endExclusive; position++) {
                char currentChar = jsonContext.charAt(position);

                //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
                if (currentChar == QUOTE) {
                    boolean isEscaped = false;
                    int prevIndex = position - 1;
                    while (prevIndex >= startInclusive && jsonContext.charAt(prevIndex--) == BACK_SLASH) {
                        isEscaped = !isEscaped;
                    }
                    if (!isEscaped) {
                        isInString = !isInString;
                        value = updateState(currentChar, position);
                    }
                } else if (!isInString && CONTROLS.contains(currentChar)) { //Check if the position is within the scope of the current JSONString
                    value = updateState(currentChar, position);
                }
            }
            if(isInString) {
                throw new IllegalStateException("JSONString is not enclosed properly");
            } else if (lastName != null) {
                throw new IllegalStateException("The NamedValue is not supported by Parser.parse() which returns IJSONValue only!");
            } else if (isObject != null && nextChildIndex != 0) {
                fail("%s is not closed properly?", isObject?"JSONObject":"JSONArray");
            }
        } catch (Exception e) {
            printError(position, e);
            throw e;
        }

        //If there is no single control character to get tuple, it must be a JSONValue
        if (isObject == null || value == null) {
            return asSimpleValue(jsonRange);
        } else {
            return value;
        }
    }

    int checkPoint = MEGABYTE;
    long lastMoment = System.currentTimeMillis();
    IJSONValue updateState(char control, int position) {
        if(position >= checkPoint) {
            Logger.I("Position=%d, %dms spend for %d Megabyte, stack depth = %d, children length = %d",
                    position, System.currentTimeMillis() - lastMoment, 1 + checkPoint / MEGABYTE, stateStack.size(), nextChildIndex);
            checkPoint += MEGABYTE;
            lastMoment = System.currentTimeMillis();
        }

        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
                pushState(position, true);
                break;
            case LEFT_BRACKET:  //Start of JSONArray
                pushState(position, false);
                break;
            case RIGHT_BRACE:   //End of current JSONObject
                if(stateStack.isEmpty()) {
                    fail("Missing '%s' to pair '%s'", LEFT_BRACE, RIGHT_BRACE);
                } else if (!isObject) {
                    fail("Existing JSONArray is not closed properly with '%s' present when '%s' is expected.", RIGHT_BRACE, RIGHT_BRACKET);
                }

                switch (lastControl) {
                    case COLON:
                        addNamedValue(asSimpleValue(Range.open(lastControlPosition, position)));
                        break;
                    case QUOTE:
                        addNamedValue(lastStringValue);
                        break;
                    default:
                        break;
                }

                closedValue = new JSONObject(nameComparator, (NamedValue[]) Arrays.copyOf(children, nextChildIndex, NamedValue[].class));
                nextChildIndex = 0;

                state = stateStack.pop();
                //Check to see if the parent is the root node
                if (state.getFirst() == null) {
                    //If yes, then there shall be no name and children defined
                    assertAllNull(state.getThird(), state.getFourth());
                    break;
                }

                restoreParentState(closedValue);
                break;

            case RIGHT_BRACKET: //Close of current JSONArray
                if(stateStack.isEmpty()) {
                    fail("Missing '%s' to pair '%s'", LEFT_BRACKET, RIGHT_BRACKET);
                } else if (isObject) {
                    fail("Existing JSONObject is not closed properly with '%s' present when '%s' is expected.", RIGHT_BRACKET, RIGHT_BRACE);
                }

                switch (lastControl) {
                    case COMMA:
                        addValue(asSimpleValue(Range.open(lastControlPosition, position)));
                        break;
                    case LEFT_BRACKET:  //Special case when Empty Array or with only one simple value
                        String valueString = subString(1+lastControlPosition, position);
                        if (!StringUtils.isBlank(valueString)) {
                            addValue(asSimpleValue(Range.open(lastControlPosition, position)));
                        }
                        break;
                    default:
                        break;
                }

                closedValue = new JSONArray(nameComparator, Arrays.copyOf(children, nextChildIndex, IJSONValue[].class));
                nextChildIndex = 0;

                state = stateStack.pop();
                //Check to see if the parent is the root node
                if (state.getFirst() == null) {
                    //If yes, then there shall be no name and children defined
                    assertAllNull(state.getThird(), state.getFourth());
                    break;
                }

                restoreParentState(closedValue);
                break;

            case COMMA:
                //assertNotNull(isObject, "No parent JSONObject/JSONArray?");
                switch (lastControl) {
                    case LEFT_BRACKET:
                    case COMMA:
                        addValue(asSimpleValue(Range.open(lastControlPosition, position)));
                        break;
                    case COLON:
                        addNamedValue(asSimpleValue(Range.open(lastControlPosition, position)));
                        break;
                    case QUOTE:
                        if (isObject) {
                            addNamedValue(lastStringValue);
                        }
                        break;
                    default:
                        break;
                }
                break;

            case COLON:
                getLastName(position);
                break;

            case QUOTE:
                getLastString(position);
                break;

            default:
                break;
        }
        lastControl = control;
        lastControlPosition = position;
        return closedValue;
    }

    void addNamedValue(IJSONValue value) {
        assertTrue(isObject, "State worng when it looks to be in a JSONObject");
        if (nextChildIndex >= capacity) {
            capacity *= 2;
            children = Arrays.copyOf(children, capacity);
        }
        children[nextChildIndex++] = new NamedValue(lastName, value);
        lastName = null;
    }

    void addValue(IJSONValue child) {
        assertFalse(isObject, "State worng when it looks to be in a JSONArray");
        if(lastName != null) {
            fail("There shall be no name %s for this value in JSONArray", lastName);
        }
        if (nextChildIndex >= capacity) {
            capacity *= 2;
            children = Arrays.copyOf(children, capacity);

        }
        children[nextChildIndex++] = child;
    }

    void getLastName(int position) {
        if (lastStringValue == null) {
            fail("Fail to enclose \"%s\" with quotation marks before ':'",
                    Range.open(lastControlPosition, position).subString(jsonContext));
        }
        lastName = lastStringValue;
        if (nameComparator instanceof OrdinalComparator) {
            ((OrdinalComparator<String>) nameComparator).putIfAbsent(lastName.getFirst());
        }
        lastStringValue = null;
    }

    void getLastString(int position) {
        if (currentStringStart != Integer.MIN_VALUE) {
            String text = StringEscapeUtils.unescapeJson(jsonContext.subSequence(currentStringStart + 1, position).toString());
            currentStringStart = Integer.MIN_VALUE;
            lastStringValue = new JSONString(text);
            if (isObject != null && !isObject) {
                addValue(lastStringValue);
            }
        } else {
            if (lastControl == QUOTE) {
                fail("two JSONStrings must be seperated by a control char.");
            }
            //This position is the start of a new String scope
            currentStringStart = position;
        }
    }

    void pushState(int position, boolean enterObject) {
        stateStack.push(Tuple.create(isObject, position, lastName, children, nextChildIndex));
        isObject = enterObject;
        reload(enterObject ? new NamedValue[DEFAULT_CAPCITY] : new IJSONValue[DEFAULT_CAPCITY], 0);
        lastName = null;
    }

    void reload(Object[] others, int length) {
        nextChildIndex = length;
        children = others;
        capacity = others.length;
    }

    void restoreParentState(IJSONValue closedValue) {
        reload(state.getFourth(), state.getFifth());
        isObject = state.getFirst();
        lastName = state.getThird();
        if (isObject) {
            addNamedValue(closedValue);
        } else {
            addValue(closedValue);
        }
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
                    if(trimmed.startsWith("\"")) {
                        return JSONString.parseString(trimmed);
                    } else {
                        return JSONNumber.parseNumber(trimmed);
                    }
            }
        } catch (NumberFormatException ne) {
            String message = StringHelper.tryFormatString("Cannot parse \"%s\" as a JSONValue", range.subString(jsonContext));
            throw new IllegalStateException(message, ne);
        } catch (Exception e) {
            throw e;
        }
    }

    protected String subString(Range range) {
        return range.subString(jsonContext);
    }

    protected String subString(int fromInclusive, int toExclusive) {
        return jsonContext.subSequence(fromInclusive, toExclusive).toString();
    }
    //endregion
}