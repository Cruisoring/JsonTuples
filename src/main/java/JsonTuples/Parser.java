package JsonTuples;

import io.github.cruisoring.Range;
import io.github.cruisoring.logger.Logger;
import io.github.cruisoring.throwables.SupplierThrowable;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple4;
import io.github.cruisoring.utility.ArrayHelper;
import io.github.cruisoring.utility.SetHelper;
import io.github.cruisoring.utility.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.function.Consumer;

import static io.github.cruisoring.Asserts.*;

/**
 * Utility to parse JSON text of either JSONArray or JSONObject based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {

    public static final int JSON_TEXT_LENGTH_WHEN_THROWS = 100;

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
    final static Set<Character> CONTROLS = SetHelper.asHashSet(LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET, COMMA, COLON);

    public static SupplierThrowable<Comparator<String>> defaultComparatorSupplier = () -> new OrdinalComparator<>();
    //endregion

    //region Static methods
    /**
     * To parse the given qualified JSON text to an {@code IJSONValue}
     *
     * @param jsonText JSON text to be parsed that represent a JSONObject or JSONArray.
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
        assertAllNotNull(jsonText);

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
        assertAllNotNull(jsonText, range);

        Parser parser = new Parser(comparator, jsonText, range);
        return parser.parse();
    }
    //endregion

    //region Constructors
    Parser(Comparator<String> comparator, CharSequence jsonText, Range range) {
        this.nameComparator = comparator;
        this.jsonContext = jsonText;
        jsonRange = range;
        length = (int) range.size();
        if(comparator instanceof OrdinalComparator){
            OrdinalComparator<String> ordinalComparator = (OrdinalComparator)comparator;
            nameConsumer = name -> ordinalComparator.putIfAbsent(name);
        } else {
            nameConsumer = null;
        }
    }

    Parser(Comparator<String> comparator, CharSequence jsonText) {
        this(comparator, jsonText, Range.ofLength(jsonText.length()));
    }

    Parser(CharSequence jsonText) {
        this(defaultComparatorSupplier==null ? null : defaultComparatorSupplier.tryGet(), jsonText);
    }
    //endregion

    //region instance variables
    public final CharSequence jsonContext;
    public final Range jsonRange;
    public final int length;
    public final Comparator<String> nameComparator;
    final Consumer<String> nameConsumer;

    //isObject acts as triple-state flag to indicate if the context under parsing is in an Object (true), an Array (false) or None of them (null).
    Boolean isObject = null;
    //lastControl keeps the last control character, '^' means there is no last control character recorded
    char lastControl = START_JSON_SIGN;
    //lastPosition keeps the position of the last control character, -1 means there is no last control character recorded
    int lastPosition = -1;
    //currentStringStart and currentStringEnd are used to record the start and end quotation marks of the current JSONString
    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;
    //lastName and lastStringValue are used to keep the recent JSONString values for further evaluations
    JSONString lastName = null, lastStringValue;
    //contextStack is a simple Stack function as the state machine of this Parser
    Stack<Tuple4<Boolean, IJSONable[], JSONString, Integer>> contextStack = new Stack<>();
    //endregion

    //region Instance methods
    /**
     * Scan the preloaded string to parse it as an IJSONValue.
     *
     * @return The root node of either JSONObject or JSONArray.
     */
    IJSONValue parse() {
        List<IJSONable> children = new ArrayList<>();
        IJSONValue value = null;

        //Identify the JSONString elements, save all control characters
        int position = -1;
        //isInString signals if the parser has entered a scope with previous control of Quotation mark
        boolean isInString = false;
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
                    value = updateState(children, currentChar, position);
                    continue;
                }

                //Check if the position is within the scope of the current JSONString
                if (isInString || !CONTROLS.contains(currentChar)) {
                    //When the String has not ended, or not any control char
                    continue;
                }

                value = updateState(children, currentChar, position);
            }
            assertFalse(isInString, "JSONString is not enclosed properly");
            assertNull(lastName, "The NamedValue is not supported by Parser.parse() which returns IJSONValue only!");
            if(isObject == null) {
                return asSimpleValue(jsonRange);
            }
            assertTrue(children.isEmpty(), "%s is not closed properly?", isObject?"JSONObject":"JSONArray");
        } catch (Exception e) {
            position = position > length - 1 ? length - 1 : position;
            try {
                String message = e.getMessage()==null ? "" : e.getMessage();
                Range problemRange = Range.closed(lastPosition, position).intersectionWith(jsonRange);
                Range beforeProblem = Range.open(lastPosition-40, lastPosition).intersectionWith(jsonRange);
                Range afterProblem = Range.open(position, position+40).intersectionWith(jsonRange);

                String summary = String.format("%s(%s): ", e.getClass().getSimpleName(), message.length()<40 ?  message : message.substring(0, 40)+"...");
                if(problemRange.size() > JSON_TEXT_LENGTH_WHEN_THROWS) {
                    if(problemRange.getStartInclusive() == 0) {
                        Logger.D(summary + "%s...%s<<<[%d]" + subString(afterProblem),
                                subString(Range.closed(problemRange.getStartInclusive(), problemRange.getStartInclusive() + JSON_TEXT_LENGTH_WHEN_THROWS/2)),
                                subString(Range.closed(problemRange.getEndInclusive()-JSON_TEXT_LENGTH_WHEN_THROWS/2, problemRange.getEndInclusive())),
                                problemRange.getEndInclusive());
                    } else {
                        Logger.D(summary + (beforeProblem.getStartInclusive()==0?"":"...") + subString(beforeProblem) + "[%d]>>>" + "%s...%s<<<[%d]" + subString(afterProblem),
                                problemRange.getStartInclusive(),
                                subString(Range.closed(problemRange.getStartInclusive(), problemRange.getStartInclusive() + JSON_TEXT_LENGTH_WHEN_THROWS/2)),
                                subString(Range.closed(problemRange.getEndInclusive()-JSON_TEXT_LENGTH_WHEN_THROWS/2, problemRange.getEndInclusive())),
                                problemRange.getEndInclusive());
                    }
                } else {
                    if(problemRange.getStartInclusive() == 0) {
                        Logger.D(summary + "%s<<<[%d]" + subString(afterProblem),
                                subString(problemRange), problemRange.getEndInclusive());
                    } else {
                        Logger.D(summary + (beforeProblem.getStartInclusive()==0?"":"...") + subString(beforeProblem) + "[%d]>>>" + "%s<<<[%d]" + subString(afterProblem),
                                problemRange.getStartInclusive(), subString(problemRange), problemRange.getEndInclusive());
                    }
                }
            } catch (Exception e2) {
                Logger.D(e.getClass().getSimpleName() + ": >>>%s<<<",
                        Range.closed(lastPosition<0?0:lastPosition, position).subString(jsonContext));
            }
            throw e;
        }

        //If there is no single control character to get tuple, it must be a JSONValue
        if(value == null || isObject == null) {
            return JSONValue.parse(jsonContext, jsonRange);
        } else {
            return value;
        }
    }

    IJSONValue updateState(List<IJSONable> children, char control, int position) {

        final JSONString nameElement;
        final IJSONValue valueElement;
        final NamedValue namedValueElement;

        IJSONValue closedValue = null;
        //The 4 values of state: isParentObject (Boolean), children elements, name JSONString, Position of either '{' or '['
        Tuple4<Boolean, IJSONable[], JSONString, Integer> state = null;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
            case LEFT_BRACKET:  //Start of JSONArray
                nameElement = lastName;
                boolean enterObject = control == LEFT_BRACE;
                state = Tuple.create(isObject, children.toArray(new IJSONable[0]), nameElement, position);
                isObject = enterObject;
                lastName = null;
                children.clear();
                contextStack.push(state);
                break;
            case RIGHT_BRACE:   //End of current JSONObject
                assertTrue(!contextStack.isEmpty(), "Missing '%s' to pair '%s'", LEFT_BRACE, RIGHT_BRACE);
                assertTrue(isObject, "No matching '%s' for this '%s]?", LEFT_BRACE, RIGHT_BRACE);
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
                    if(state.getThird() != null) {
                        lastPosition = state.getFourth();
                        fail("The current JSONObject is value of a NamedValue which is the root element");
                    }
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    children.add(new NamedValue(name, closedValue));
                    lastName = null;
                } else {
                    assertAllTrue(state.getThird() == null);
                    children.add(closedValue);
                }
                break;

            case RIGHT_BRACKET: //Close of current JSONArray
                assertTrue(!contextStack.isEmpty(), "Missing '%s' to pair '%s'", LEFT_BRACKET, RIGHT_BRACKET);
                assertTrue(!isObject, "No matching '%s' for this '%s]?", LEFT_BRACKET, RIGHT_BRACKET);
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
                    if(state.getThird() != null) {
                        lastPosition = state.getFourth();
                        fail("The current JSONArray is value of a NamedValue which is the root element");
                    }
                    break;
                }

                children.addAll(Arrays.asList(state.getSecond()));
                isObject = state.getFirst();
                if (isObject) {
                    final JSONString name = state.getThird();
                    children.add(new NamedValue(name, closedValue));
                    lastName = null;
                } else {
                    assertAllTrue(state.getThird() == null);
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
                        lastName = null;
                        children.add(namedValueElement);
                        break;
                    case QUOTE:
                        if (isObject) {
                            namedValueElement = new NamedValue(lastName, lastStringValue);
                            lastName = null;
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
                    assertFalse(lastControl == QUOTE, "two JSONStrings must be seperated by a control char.");
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
        return closedValue;
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

    protected String subString(Range range){
        Range intersection = range.intersectionWith(jsonRange);
        return intersection.subString(jsonContext);
    }
    //endregion
}