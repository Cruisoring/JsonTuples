package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple3;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.omg.CORBA.NameValuePair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

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
    final static Set<Character> ESCAPABLE_CHARS = new HashSet<>(Arrays.asList('"', '\\', '/', 'b', 'n', 'r', 't', 'u'));

    final static List<Character> VALUE_START_INDICATORS = Arrays.asList(COMMA, LEFT_BRACE, LEFT_BRACKET);
    final static List<Character> VALUE_END_INDICATORS = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, COMMA);
    final static List<Character> ARRAY_VALUE_SPLITTERS = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, LEFT_BRACKET, COMMA);
    final static Character MIN_CONTROL = Collections.min(CONTROLS);
    final static Character MAX_CONTROL = Collections.max(CONTROLS);
    //endregion

    //region instance variables
    public final String jsonContext;
    public final int length;

    private final Map<Range, Lazy<IJSONable>> rangedElements = new HashMap<>();

    private TreeSet<Range> _sortedRanges = null;

    int bufferSize = 1024;
    int[] positions = new int[bufferSize];
    char[] controls = new char[bufferSize];
    int controlCount = -1;
    char lastControl = START_JSON_SIGN;

    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;
    Stack<Tuple3<Boolean, Integer, Character>> segmentStack = new Stack<>();
    Boolean isInObject = null;
    Tuple3<Boolean, Integer, Character> objectOpen = null;
    Tuple3<Boolean, Integer, Character> arrayOpen = null;

    private Lazy<IJSONValue> rootNodeLazy = null;

    Stack<Tuple2<Integer, Consumer<Range>>> actionsToCloseSegment = new Stack<>();
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
    public String subString(Range range) {
        checkNotNull(range);

        int end = range.getEndExclusive();
        return jsonContext.substring(range.getStartInclusive(), end > length ? length : end);
    }

    private TreeSet<Range> getSortedRanges() {
        if (_sortedRanges == null || _sortedRanges.size() != rangedElements.size()) {
            _sortedRanges = new TreeSet<>(rangedElements.keySet());
        }
        return _sortedRanges;
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONString.
     *
     * @param range Range to specify the content enclosed by '"'s.
     * @return JSONString instance from the given range of the {@code jsonContext}
     */
    protected JSONString asJSONString(Range range) {
        String string = JSONString.unescapeJson(jsonContext, range.getStartInclusive()+1, range.getEndExclusive());
        return new JSONString(string);
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONObject.
     *
     * @param range Range to specify the content enclosed by '{' and '}'
     * @return Parsed JSONObject instance from the given range of the {@code jsonContext}
     */
    protected JSONObject asJSONObject(Range range) {
        List<Range> namedValueChildren = range.getChildRanges(getSortedRanges());
//        System.out.println("???????" + subString(range) + ": as JSONObject");
//        namedValueChildren.forEach(r -> System.out.println(subString(r)));

        NamedValue[] namedValues = namedValueChildren.parallelStream()
                .map(child -> (NamedValue) rangedElements.get(child).getValue())
                .toArray(size -> new NamedValue[size]);
        JSONObject object = new JSONObject(namedValues);
        return object;
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONArray.
     *
     * @param range Range to specify the content enclosed by '[' and ']'
     * @return Parsed JSONArray instance from the given range of the {@code jsonContext}
     */
    protected JSONArray asJSONOArray(Range range) {
        List<Range> elementRanges = range.getChildRanges(getSortedRanges());

        IJSONValue[] values = elementRanges.parallelStream()
                .map(r -> rangedElements.get(r).getValue())
                .toArray(i -> new IJSONValue[i]);
        JSONArray array = new JSONArray(values);
        return array;
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONNumber.
     *
     * @param range Range to specify the content
     * @return Parsed JSONNumber instance from the given range of the {@code jsonContext}
     */
    protected JSONNumber asJSONNumber(Range range) {
        String valueString = subString(range).trim();

        return JSONNumber.parseNumber(valueString);
    }


    protected IJSONValue asValue(Range range) {
        String valueString = subString(range).trim();

        switch (valueString) {
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                char firstChar = valueString.charAt(0);
                if (firstChar == LEFT_BRACE) {
                    return asJSONObject(range);
                } else if (firstChar == LEFT_BRACKET) {
                    return asJSONOArray(range);
                } else {
                    return asJSONNumber(range);
                }
        }
    }

    protected NamedValue asNamedValue(Range nameRange, Range valueRange) {
        String name;
        if(rangedElements.containsKey(nameRange)) {
            name = ((JSONString) rangedElements.get(nameRange).getValue()).getFirst();
        } else {
            name = asJSONString(nameRange).getFirst();
        }

        IJSONValue value;
        if(rangedElements.containsKey(valueRange)) {
            value = (IJSONValue) rangedElements.get(valueRange).getValue();;
        } else {
            value = asValue(valueRange);
        }
        return new NamedValue(name, value);
    }

    protected NamedValue asNamedValue(Range nameValueRange) {
        return NamedValue.parse(subString(nameValueRange));
    }

    public IJSONValue getRootNode() {
        return rootNodeLazy.getValue();
    }

    public Map<Range, Lazy<IJSONable>> parseFast() {
        rangedElements.clear();
        boolean isInString = false;

        //Demarcate the JSONString elements, save all control characters
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);

            //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
            if (current == QUOTE) {
                boolean isEscaped = false;
                for (int j = i-1; j >= 0 ; j--) {
                    if(jsonContext.charAt(j) == BACK_SLASH){
                        isEscaped = !isEscaped;
                    } else {
                        break;
                    }
                }
                if(isEscaped) {
                    //This Quote is escaped, not a control
                    continue;
                }

                isInString = !isInString;
                addControlFast(current, i);
                continue;
            }

            //Check if the position is within the scope of the current JSONString
            if (isInString || current < MIN_CONTROL || current > MAX_CONTROL) {
                //When the String has not ended, or not any control char
                continue;
            }

            if(CONTROLS.contains(current)) {
                addControlFast(current, i);
            }
        }
        return rangedElements;
    }

    void addControlFast(char control, int position) {
        Tuple2<Integer, Consumer<Range>> actionWhenClosing;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
                switch (lastControl) {
                    case COLON:
                        final Range objectNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        actionWhenClosing = Tuple.create(position,
                                objectRange -> rangedElements.put(
                                        objectNameRange.intersection(objectRange), new Lazy<>(() -> asNamedValue(objectNameRange, objectRange))));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case START_JSON_SIGN:
                        actionWhenClosing = Tuple.create(position,
                                objectRange -> rootNodeLazy = new Lazy<>(() -> asJSONObject(objectRange)) );
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    default:
                        break;
                }

                isInObject = true;
                objectOpen = Tuple.create(isInObject, position, control);
                segmentStack.push(objectOpen);

                break;
            case RIGHT_BRACE:   //End of current JSONObject
                switch (lastControl) {
                    case COLON:
                        final Range objectNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(()-> asValue(vRange)));
                        Range nvRange = objectNameRange.intersection(vRange);
                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(objectNameRange, vRange)));
                        break;
                    case QUOTE:
                        Range nRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                        Range sRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                        Range namedStringRange = nRange.intersection(sRange);
                        rangedElements.put(namedStringRange, new Lazy<>(() -> asNamedValue(nRange, sRange)));
                        break;
                    default:
                        break;
                }

                checkState(objectOpen == segmentStack.pop(),
                        "The stacked '%s' at %d is not matched with '%s' at %d",
                        objectOpen.getThird(), objectOpen.getSecond(), control, position);

                Range objectRange = Range.closed(objectOpen.getSecond(), position);
                rangedElements.put(objectRange, new Lazy<>(() -> asJSONObject(objectRange)));
                //Check if the JSONObject is marked as named, if yes then save the corresponding NamedValue
                tryCloseRegion(objectRange);

                //restore state to parent of the current JSONObject
                restoreState();
                break;
            case LEFT_BRACKET:  //Start of JSONArray
                switch (lastControl) {
                    case COMMA:
                        actionWhenClosing = Tuple.create(position, null);
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case COLON:
                        final Range arrayNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        actionWhenClosing = Tuple.create(position, arrayRange ->
                                rangedElements.put(
                                        arrayNameRange.intersection(arrayRange), new Lazy<>(() -> asNamedValue(arrayNameRange, arrayRange))));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case START_JSON_SIGN:
                        actionWhenClosing = Tuple.create(position,
                                arrayRange -> rootNodeLazy = new Lazy<>(() -> asJSONOArray(arrayRange)));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    default:
                        break;
                }
                isInObject = false;
                arrayOpen = Tuple.create(isInObject, position, control);
                segmentStack.push(arrayOpen);
                break;
            case RIGHT_BRACKET: //Close of current JSONArray
                switch (lastControl) {
                    case COMMA:
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(() -> asValue(vRange)));
                        break;
                    default:
                        break;
                }

                checkState(arrayOpen == segmentStack.pop(),
                        "The stacked '%s' at %d is not matched with '%s' at %d",
                        arrayOpen.getThird(), arrayOpen.getSecond(), control, position);

                Range arrayRange = Range.closed(arrayOpen.getSecond(), position);
                rangedElements.put(arrayRange, new Lazy<>(() -> asJSONOArray(arrayRange)));
                //Check if the array is marked as named, if yes then save the corresponding NamedValue
                tryCloseRegion(arrayRange);

                //restore state to parent of the current JSONArray
                restoreState();
                break;
            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
                switch (lastControl) {
                    case COMMA:
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(() -> asValue(vRange)));
                        break;
                    case COLON:
                        final Range nRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        final Range valueRange = Range.open(positions[controlCount], position);
                        rangedElements.put(valueRange, new Lazy<>(() -> asValue(valueRange)));
                        rangedElements.put(nRange.intersection(valueRange), new Lazy<>(() -> asNamedValue(nRange, valueRange)));
                        break;
                    case QUOTE:
                        if(isInObject) {
                            final Range nameRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                            final Range strRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                            rangedElements.put(nameRange.intersection(strRange), new Lazy<>(() -> asNamedValue(nameRange, strRange)));
                        }
                    default:
                        break;
                }
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    //This position is the end of an existing String scope, keep it with currentStringEnd
                    currentStringEnd = position;
                    Range range = Range.closed(currentStringStart, currentStringEnd);
                    rangedElements.put(range, new Lazy<>(() -> asJSONString(range)));
                } else {
                    //This position is the start of a new String scope
                    currentStringStart = position;
                    currentStringEnd = Integer.MAX_VALUE;
                }
                break;

            default:
                break;
        }
        if(++controlCount == bufferSize) {
            bufferSize = bufferSize *2;
            controls = Arrays.copyOf(controls, bufferSize);
            positions = Arrays.copyOf(positions, bufferSize);
        }
        controls[controlCount] = control;
        positions[controlCount] = position;
        lastControl = control;
    }

    public Map<Range, Lazy<IJSONable>> parseCautious() {
        rangedElements.clear();
        boolean isInString = false;

        //Demarcate the JSONString elements, save all control characters
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);

            //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
            if (current == QUOTE) {
                boolean isEscaped = false;
                for (int j = i-1; j >= 0 ; j--) {
                    if(jsonContext.charAt(j) == BACK_SLASH){
                        isEscaped = !isEscaped;
                    } else {
                        break;
                    }
                }
                if(isEscaped) {
                    //This Quote is escaped, not a control
                    continue;
                }

                isInString = !isInString;
                addControl(current, i);
                continue;
            }

            //Check if the position is within the scope of the current JSONString
            if (isInString || current < MIN_CONTROL || current > MAX_CONTROL) {
                //When the String has not ended, or not any control char
                continue;
            }

            if(CONTROLS.contains(current)) {
                addControl(current, i);
            }
        }
        return rangedElements;
    }

    void addControl(char control, int position) {
        Tuple2<Integer, Consumer<Range>> actionWhenClosing;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
                switch (lastControl) {
                    case LEFT_BRACE:
                    case RIGHT_BRACE:
                    case RIGHT_BRACKET:
                        checkState(false,
                                "Invalid state when %s follows %s immediately: %s",
                                control, lastControl, subString(Range.closed(positions[controlCount], position)));
                        break;
                    case COMMA:
                        checkState(!isInObject, "It's not in a JSONArray?");
                        break;
                    case COLON:
                        checkState(isInObject, "It is not in a bigger JSONObject");
                        //It must be led by a JSONString
                        checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==QUOTE);
                        final Range objectNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        actionWhenClosing = Tuple.create(position,
                                objectRange -> rangedElements.put(
                                        objectNameRange.intersection(objectRange), new Lazy<>(() -> asNamedValue(objectNameRange, objectRange))));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case START_JSON_SIGN:
                        actionWhenClosing = Tuple.create(position,
                                objectRange -> rootNodeLazy = new Lazy<>(() -> asJSONObject(objectRange)) );
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    default:
                        break;
                }

                isInObject = true;
                objectOpen = Tuple.create(isInObject, position, control);
                segmentStack.push(objectOpen);

                break;
            case RIGHT_BRACE:   //End of current JSONObject
                switch (lastControl) {
                    case COMMA:
                        checkState(false,
                                "Invalid state when %s follows %s immediately: %s",
                                control, lastControl, subString(Range.closed(positions[controlCount], position)));
                        break;
                    case COLON:
                        checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==QUOTE);
                        final Range objectNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(()-> asValue(vRange)));
                        Range nvRange = objectNameRange.intersection(vRange);
                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(objectNameRange, vRange)));
                        break;
                    case QUOTE:
                        checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==COLON
                                && controls[controlCount-3]==QUOTE && controls[controlCount-4]==QUOTE
                        );
                        Range nRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                        Range sRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                        Range namedStringRange = nRange.intersection(sRange);
                        rangedElements.put(namedStringRange, new Lazy<>(() -> asNamedValue(nRange, sRange)));
                        break;
                    default:
                        break;
                }

                checkState(objectOpen == segmentStack.pop(),
                        "The stacked '%s' at %d is not matched with '%s' at %d",
                        objectOpen.getThird(), objectOpen.getSecond(), control, position);

                Range objectRange = Range.closed(objectOpen.getSecond(), position);
                rangedElements.put(objectRange, new Lazy<>(() -> asJSONObject(objectRange)));
                //Check if the JSONObject is marked as named, if yes then save the corresponding NamedValue
                tryCloseRegion(objectRange);

                //restore state to parent of the current JSONObject
                restoreState();
                break;
            case LEFT_BRACKET:  //Start of JSONArray
                switch (lastControl) {
                    case RIGHT_BRACE:
                    case RIGHT_BRACKET:
                        checkState(false,
                                "Invalid state when %s follows %s immediately: %s",
                                control, lastControl, subString(Range.closed(positions[controlCount], position)));
                        break;
                    case COMMA:
                        checkState(!isInObject, "It's not in a JSONArray?");
                        actionWhenClosing = Tuple.create(position, null);
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case COLON:
                        checkState(isInObject, "It shall be in a bigger JSONObject");
                        //It must be led by a JSONString
                        checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==QUOTE);
                        final Range arrayNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        actionWhenClosing = Tuple.create(position, arrayRange ->
                                rangedElements.put(
                                        arrayNameRange.intersection(arrayRange), new Lazy<>(() -> asNamedValue(arrayNameRange, arrayRange))));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    case START_JSON_SIGN:
                        actionWhenClosing = Tuple.create(position,
                                arrayRange -> rootNodeLazy = new Lazy<>(() -> asJSONOArray(arrayRange)));
                        actionsToCloseSegment.push(actionWhenClosing);
                        break;
                    default:
                        break;
                }
                isInObject = false;
                arrayOpen = Tuple.create(isInObject, position, control);
                segmentStack.push(arrayOpen);
                break;
            case RIGHT_BRACKET: //Close of current JSONArray
                switch (lastControl) {
                    case LEFT_BRACE:
                    case COLON:
                        checkState(false,
                                "Invalid state when %s follows %s immediately: %s",
                                control, lastControl, subString(Range.closed(positions[controlCount], position)));
                        break;
                    case COMMA:
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(() -> asValue(vRange)));
                        break;
                    case QUOTE:
                        checkState(controls[controlCount-1]==QUOTE && ARRAY_VALUE_SPLITTERS.contains(controls[controlCount-2]),
                                "Invalid state encounter at position %d", position);
                        break;
                    default:
                        break;
                }

                checkState(arrayOpen == segmentStack.pop(),
                        "The stacked '%s' at %d is not matched with '%s' at %d",
                        arrayOpen.getThird(), arrayOpen.getSecond(), control, position);

                Range arrayRange = Range.closed(arrayOpen.getSecond(), position);
                rangedElements.put(arrayRange, new Lazy<>(() -> asJSONOArray(arrayRange)));
                //Check if the array is marked as named, if yes then save the corresponding NamedValue
                tryCloseRegion(arrayRange);

                //restore state to parent of the current JSONArray
                restoreState();
                break;
            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
                switch (lastControl) {
                    case LEFT_BRACE:
                    case LEFT_BRACKET:
                        checkState(false,
                                "Invalid state when %s follows %s immediately: %s",
                                control, lastControl, subString(Range.closed(positions[controlCount], position)));
                        break;
                    case COMMA:
                        checkState(!isInObject, "It's not in a JSONArray?");
                        Range vRange = Range.open(positions[controlCount], position);
                        rangedElements.put(vRange, new Lazy<>(() -> asValue(vRange)));
                        break;
                    case COLON:
                        checkState(isInObject, "It shall be in a bigger JSONObject");
                        //It must be led by a JSONString
                        checkState(controls[controlCount-2]==QUOTE && controls[controlCount-1]==QUOTE);
                        final Range nRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                        final Range valueRange = Range.open(positions[controlCount], position);
                        rangedElements.put(valueRange, new Lazy<>(() -> asValue(valueRange)));
                        rangedElements.put(nRange.intersection(valueRange), new Lazy<>(() -> asNamedValue(nRange, valueRange)));
                        break;
                    case QUOTE:
                        checkState(controls[controlCount-1]==QUOTE,
                                "The JSONString is not enclosed by two Quotes at %d.", position);
                        if(isInObject) {
                            checkState(controls[controlCount-3]==QUOTE && controls[controlCount-4]==QUOTE
                                            && controls[controlCount-2]==COLON,
                                    "Unexpected pattern at %d: %s", position, subString(Range.closed(positions[controlCount-4], position)));
                            final Range nameRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                            final Range strRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                            rangedElements.put(nameRange.intersection(strRange), new Lazy<>(() -> asNamedValue(nameRange, strRange)));
                        }
                    default:
                        break;
                }
                break;
            case COLON:
                checkState(isInObject,
                        "COLON(':') shall appear in JSONObject only");
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    //This position is the end of an existing String scope, keep it with currentStringEnd
                    currentStringEnd = position;
                    Range range = Range.closed(currentStringStart, currentStringEnd);
                    rangedElements.put(range, new Lazy<>(() -> asJSONString(range)));
                } else {
                    //This position is the start of a new String scope
                    currentStringStart = position;
                    currentStringEnd = Integer.MAX_VALUE;
                }
                break;

            default:
                checkState(false, "Unexpected control char of: %s at %d", control, position);
        }
        if(++controlCount == bufferSize) {
            bufferSize = bufferSize *2;
            controls = Arrays.copyOf(controls, bufferSize);
            positions = Arrays.copyOf(positions, bufferSize);
        }
        controls[controlCount] = control;
        positions[controlCount] = position;
        lastControl = control;
    }

    private void restoreState(){
        isInObject = segmentStack.isEmpty() ? null : segmentStack.peek().getFirst();
        if (isInObject == null) {
            objectOpen = null;
            arrayOpen = null;
        } else if(isInObject) {
            objectOpen = segmentStack.peek();
            arrayOpen = null;
        } else {
            objectOpen = null;
            arrayOpen = segmentStack.peek();
        }
    }

    private void tryCloseRegion(Range range) {
        if(!actionsToCloseSegment.isEmpty() && Objects.equals(actionsToCloseSegment.peek().getFirst(), range.getStartInclusive())) {
            Consumer<Range> action = actionsToCloseSegment.pop().getSecond();
            if (action != null) {
                action.accept(range);
            }
        }
    }

    public IJSONValue parse() {

        Map<Range, Lazy<IJSONable>> map = parseFast();

        IJSONValue result = getRootNode();
        return result;
    }
}