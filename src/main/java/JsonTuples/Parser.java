package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.Tuple3;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

/**
 * Utility to parse JSON text based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {
    //region static variables
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
    final static Character MIN_CONTROL = Collections.min(CONTROLS);
    final static Character MAX_CONTROL = Collections.max(CONTROLS);
    //endregion

    public class StateMachine {
        int length = 1024;
        int[] positions = new int[length];
        char[] controls = new char[length];
        int controlCount = -1;
        char lastControl = ' ';

        int currentStringStart = Integer.MAX_VALUE;
        int currentStringEnd = -1;
        Stack<Tuple3<Boolean, Integer, Character>> segmentStack = new Stack<>();
        Boolean isInObject = null;
        Tuple3<Boolean, Integer, Character> objectOpen = null;
        Tuple3<Boolean, Integer, Character> arrayOpen = null;
        private final Map<Range, Lazy<IJSONable>> rangesLazyMap = new HashMap<>();

        Stack<Tuple2<Integer, Consumer<Range>>> actionsToCloseSegment = new Stack<>();

        void add(char control, int position) {
            Tuple2<Integer, Consumer<Range>> actionWhenClosing;
            switch (control) {
                case LEFT_BRACE:    //Start of JSONObject
                    switch (lastControl) {
                        case LEFT_BRACE:
                        case RIGHT_BRACE:
                        case RIGHT_BRACKET:
                            checkState(false,
                                    "Invalid state when %s follows %s immediately: %s",
                                    control, lastControl, subString(Range.closed(controls[controlCount], position)));
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
                                    objectRange -> rangesLazyMap.put(
                                        objectNameRange.intersection(objectRange), new Lazy<>(() -> asNamedValue(objectNameRange, objectRange))));
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
                                    control, lastControl, subString(Range.closed(controls[controlCount], position)));
                            break;
                        case COLON:
                            checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==QUOTE);
                            final Range objectNameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                            Range vRange = Range.open(controls[controlCount], position);
                            Range nvRange = objectNameRange.intersection(vRange);
                            rangesLazyMap.put(nvRange, new Lazy<>(() -> asNamedValue(objectNameRange, vRange)));
                            break;
                        case QUOTE:
                            checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==COLON
                                    && controls[controlCount-3]==QUOTE && controls[controlCount-4]==QUOTE
                            );
                            Range nRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                            Range sRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                            Range namedStringRange = nRange.intersection(sRange);
                            rangesLazyMap.put(namedStringRange, new Lazy<>(() -> asNamedValue(nRange, sRange)));
                            break;
                        default:
                            break;
                    }

                    checkState(objectOpen == segmentStack.pop(),
                            "The stacked '%s' at %d is not matched with '%s' at %d",
                            objectOpen.getThird(), objectOpen.getSecond(), control, position);

                    Range objectRange = Range.closed(objectOpen.getSecond(), position);
                    rangesLazyMap.put(objectRange, new Lazy<>(() -> asJSONObject(objectRange)));
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
                                    control, lastControl, subString(Range.closed(controls[controlCount], position)));
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
                                    rangesLazyMap.put(
                                            arrayNameRange.intersection(arrayRange), new Lazy<>(() -> asNamedValue(arrayNameRange, arrayRange))));
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
                                    control, lastControl, subString(Range.closed(controls[controlCount], position)));
                            break;
                        case COMMA:
                            Range vRange = Range.open(controls[controlCount], position);
                            rangesLazyMap.put(vRange, new Lazy<>(() -> asValue(vRange)));
                            break;
                        case QUOTE:
                            checkState(controls[controlCount-1]==QUOTE && controls[controlCount-2]==COMMA);
                            break;
                        default:
                            break;
                    }

                    checkState(arrayOpen == segmentStack.pop(),
                            "The stacked '%s' at %d is not matched with '%s' at %d",
                            arrayOpen.getThird(), arrayOpen.getSecond(), control, position);

                    Range arrayRange = Range.closed(arrayOpen.getSecond(), position);
                    rangesLazyMap.put(arrayRange, new Lazy<>(() -> asJSONOArray(arrayRange)));
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
                                    control, lastControl, subString(Range.closed(controls[controlCount], position)));
                            break;
                        case COMMA:
                            checkState(!isInObject, "It's not in a JSONArray?");
                            Range vRange = Range.open(controls[controlCount], position);
                            rangesLazyMap.put(vRange, rangesLazyMap.put(vRange, new Lazy<>(() -> asValue(vRange))));
                            break;
                        case COLON:
                            checkState(isInObject, "It shall be in a bigger JSONObject");
                            //It must be led by a JSONString
                            checkState(controls[controlCount-2]==QUOTE && controls[controlCount-1]==QUOTE);
                            final Range nRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
                            final Range valueRange = Range.open(positions[controlCount], position);
                            rangesLazyMap.put(valueRange, new Lazy<>(() -> asValue(valueRange)));
                            rangesLazyMap.put(nRange.intersection(valueRange), new Lazy<>(() -> asNamedValue(nRange, valueRange)));
                            break;
                        case QUOTE:
                            checkState(controls[controlCount-1]==QUOTE, "The JSONString is not enclosed by two Quotes.");
                            if(isInObject) {
                                checkState(controls[controlCount-3]==QUOTE && controls[controlCount-4]==QUOTE
                                        && controls[controlCount-2]==COLON);
                                final Range nameRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
                                final Range strRange = Range.closed(positions[controlCount-1], positions[controlCount]);
                                rangesLazyMap.put(nameRange.intersection(strRange), new Lazy<>(() -> asNamedValue(nameRange, strRange)));
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
                        rangesLazyMap.put(range, new Lazy<>(() -> asJSONString(range)));
                    } else {
                        //This position is the start of a new String scope
                        currentStringStart = position;
                        currentStringEnd = Integer.MAX_VALUE;
                    }
                    break;

                default:
                    checkState(false, "Unexpected control char of: %s at %d", control, position);
            }
            if(++controlCount == length) {
                length = length*2;
                controls = Arrays.copyOf(controls, length);
                positions = Arrays.copyOf(positions, length);
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
    }

    /**
     * Parse the remaining part of NamedValue other than Name to an IJSONValue instance.
     *
     * @param jsonText   All JSON Text to be parsed.
     * @param valueRange Range of the value portion of the NameValuePair, shall be led by COLON ':' with optional spaces.
     * @return Either a simple JSON value (true, false, null, number, string) or compound JSON Object or Array.
     */
    public static IJSONValue parseValuePortion(String jsonText, Range valueRange) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkNotNull(valueRange);

        String colonAndValue = Range.subString(jsonText, valueRange).trim();
        checkState(colonAndValue.charAt(0) == COLON, "The value of the NameValuePair must be led by a COLON ':'.");

        return JSONValue.parse(colonAndValue.substring(1));
    }


    /**
     * Find the subList of the given index list within a specific range.
     *
     * @param allIndexes Indexes which would not contain the lower and upper end point of the range.
     * @param range      Range under concern.
     * @return Sublist of the given sorted index list within a specific range.
     */
    public static List<Integer> getIndexesInRange(List<Integer> allIndexes, Range range) {
        checkNotNull(allIndexes);
        checkNotNull(range);

        if (allIndexes.isEmpty()) {
            return new ArrayList<Integer>();
        }

        //Sort the indexes with nature order
        Collections.sort(allIndexes, Comparator.naturalOrder());

        return _getIndexesInRange(allIndexes, range);
    }

    private static List<Integer> _getIndexesInRange(List<Integer> allIndexes, Range range) {
        List<Integer> result = new ArrayList<>();

        int count = allIndexes.size();
        Integer lower = range.getStartInclusive();
        Integer upper = range.getEndInclusive();
        if (count == 0 || lower > allIndexes.get(count - 1) || upper < allIndexes.get(0)) {
            return result;
        }

        boolean belowRange = true;
        for (int i = 0; i < count; i++) {
            Integer index = allIndexes.get(i);

            if (belowRange) {
                if (index < lower)
                    continue;
                if (range.contains(index)) {
                    result.add(index);
                } else if (index > upper) {
                    return result;
                }
                belowRange = false;
            } else {
                if (range.contains(index)) {
                    result.add(index);
                } else if (index > upper) {
                    return result;
                }
            }
        }
        return result;
    }

    private static List<Range> getPairsWithValueRanges(Set<Range> nameRangeSet, Collection<Range> valueRanges, Set<Integer> indicatorIndexes) {
        List<Range> nvpRanges = new ArrayList<>();
        for (Range valueRange : valueRanges) {
            Range nameRange = nameRangeSet.stream()
                    .filter(scope -> scope.getEndInclusive() < valueRange.getStartInclusive())
                    .sorted(comparing(Range::getEndInclusive).reversed())
                    .findFirst().orElse(null);
            if (nameRange != null) {
                Range gapRange = valueRange.gapWith(nameRange);
                List<Integer> colonsWithin = indicatorIndexes.stream().filter(i -> gapRange.contains(i)).collect(Collectors.toList());
                checkState(colonsWithin.size() == 1,
                        String.format("Failed to get one single indictor between '%s' and '%s'", nameRange, valueRange));
                Range nameValueRange = nameRange.intersection(valueRange);
                nameRangeSet.remove(nameRange);
                indicatorIndexes.remove(colonsWithin.get(0));
                nvpRanges.add(nameValueRange);
            }
        }
        return nvpRanges;
    }

    private static List<Range> _getNamedValueRanges(Set<Range> nameRangeSet, Set<Integer> indicatorIndexes, List<Integer> sortedEnderIndexes) {

        List<Range> nvpRanges = new ArrayList<>();
        for (Integer joinerIndex : indicatorIndexes) {
            Range nameRange = nameRangeSet.stream()
                    .filter(r -> r.getEndInclusive() < joinerIndex)
                    .sorted(comparing(Range::getEndInclusive).reversed())
                    .findFirst().orElse(null);

            if (nameRange == null)
                checkNotNull(nameRange, "Failed to locate the name range right before COLON at " + joinerIndex);
            Integer endIndex = sortedEnderIndexes.stream()
                    .filter(i -> i > joinerIndex)
                    .sorted()
                    .findFirst().orElse(null);
            checkNotNull(endIndex, "Failed to find the end of value after COLON at " + joinerIndex);

            Range range = Range.closedOpen(nameRange.getStartInclusive(), endIndex);
            nvpRanges.add(range);
        }
        return nvpRanges;
    }

    public final String jsonContext;
    public final int length;

    //Keep the indexes of all characters with special meaning in JSON
    private Map<Character, List<Integer>> controlsIndexMap = new HashMap<>();

    private List<Tuple2<Character, Integer>> activeControls = new ArrayList<>();

    private IJSONValue parsedResult = null;

    private List<Range> stringRanges = new ArrayList<>();

    private final Map<Range, Lazy<IJSONable>> rangedElements = new HashMap<>();

    private TreeSet<Range> unnamedValueRanges = null;

    private TreeSet<Range> _sortedRanges = null;

    private TreeSet<Range> getSortedRanges() {
        if (_sortedRanges == null || _sortedRanges.size() != rangedElements.size()) {
            _sortedRanges = new TreeSet<>(rangedElements.keySet());
        }
        return _sortedRanges;
    }

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


    protected void demarcateValues(List<Tuple2<Character, Integer>> controls) {
        Stack<Boolean> isInObjects = new Stack<>();
        int size = controls.size();
        int nameStart = -1, nameEnd = -1, valueStart = -1, valueEnd = -1;
        int colonIndex = -1;
        for (int i = 0; i < size; i++) {
            Tuple2<Character, Integer> current = controls.get(i);
            switch (current.getFirst()) {
                case LEFT_BRACE:
                    isInObjects.push(true);
                    break;
                case RIGHT_BRACE:
                    checkState(isInObjects.pop());
                    break;
                case LEFT_BRACKET:
                    isInObjects.push(false);
                    break;
                case RIGHT_BRACKET:
                    checkState(!isInObjects.pop());
                    break;
                case COMMA:
                    break;
                case COLON:
                    checkState(isInObjects.peek());

                    break;
                case QUOTE:
                    break;
                default:
                    checkState(false, "Shall never contain other chars");
            }
        }
    }

    public StateMachine stateMachine = new StateMachine();

    /**
     * Scan all the characters of the jsonContext to keep indexes of special characters as defined in {@code MARKERS}
     *
     * @return Indexes of all special characters as a Map.
     */
    private List<Range> scan() {
        List<Range> objectArrayRanges = new ArrayList<>();

        CONTROLS.stream().forEach(c -> controlsIndexMap.put(c, new ArrayList<>()));

        int lastBackSlash = -100;
        int currentStringStart = Integer.MAX_VALUE;
        int currentStringEnd = -1;
        Stack<Integer> objectStarts = new Stack<>();
        Stack<Integer> arrayStarts = new Stack<>();
        Stack<Range> nameRanges = new Stack<>();
        Range lastNameRange = null;
        Stack<Boolean> isInObjectStack = new Stack<>();

        //Get all indexes of all concerned markers
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);

            //Ensure only escapable chars following BACK_SLASH
            if (i == lastBackSlash + 1) {
                checkState(ESCAPABLE_CHARS.contains(current));
                i++;

                continue;
            }

            //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
            if (current == QUOTE) {
                if (currentStringEnd == Integer.MAX_VALUE) {
                    //This position i is the end of an existing String scope, keep it with currentStringEnd
                    currentStringEnd = i;
                    Range range = Range.closed(currentStringStart, currentStringEnd);
                    rangedElements.put(range, new Lazy<>(() -> asJSONString(range)));
                    stringRanges.add(range);
                } else {
                    //This position i is the start of a new String scope
                    currentStringStart = i;
                    currentStringEnd = Integer.MAX_VALUE;
                }
                activeControls.add(Tuple.create(current, i));
                stateMachine.add(current, i);
                continue;
            }

            //Check if the position is within the scope of the current JSONString
            if (i < currentStringEnd) {
                //When the String has not ended
                continue;
            }

            //Now the position is out of JSONString
            switch (current) {
                case LEFT_BRACE:
                    isInObjectStack.push(true);
                    objectStarts.push(i);

                    if (lastNameRange != null) {
                        nameRanges.push(lastNameRange);
                        lastNameRange = null;
                    }
                    break;
                case LEFT_BRACKET:
                    isInObjectStack.push(false);
                    arrayStarts.push(i);

                    if (lastNameRange != null) {
                        nameRanges.push(lastNameRange);
                        lastNameRange = null;
                    }
                    break;
                case RIGHT_BRACE:
                    checkState(isInObjectStack.pop(), "Incorrect state showing it is not exiting a JSONObject.");

                    checkState(!objectStarts.isEmpty(),
                            "There is no matched '{' before the '}' at position %d", i);
                    //It close Value, NamedValue, JSONObject
                    int lastObjectStart = objectStarts.pop();
                    Range oRange = Range.closed(lastObjectStart, i);
                    rangedElements.put(oRange, new Lazy<>(() -> asJSONObject(oRange)));
                    objectArrayRanges.add(oRange);
                    if (!nameRanges.isEmpty()) {
                        Range nRange = nameRanges.pop();
                        Range nvRange = nRange.intersection(oRange);
                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, oRange)));
                        stringRanges.remove(nRange);
                    }
                    break;
                case RIGHT_BRACKET:
                    checkState(!isInObjectStack.pop(), "Incorrect state showing it is not exiting a JSONArray.");
                    checkState(!arrayStarts.isEmpty(),
                            "There is no matched '[' before the ']' at position %d", i);
                    int lastArrayStart = arrayStarts.pop();
                    Range aRange = Range.closed(lastArrayStart, i);
                    rangedElements.put(aRange, new Lazy<>(() -> asJSONOArray(aRange)));
                    objectArrayRanges.add(aRange);
                    if (!nameRanges.isEmpty()) {
                        Range nRange = nameRanges.pop();
                        Range nvRange = nRange.intersection(aRange);
                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, aRange)));
                        stringRanges.remove(nRange);
                    }
                    break;
                case COLON:
                case COMMA:
                    break;
                default:
                    continue;
            }

            //Keep the index of this CONTROL char into its list
            controlsIndexMap.get(current).add(i);
            activeControls.add(Tuple.create(current, i));
            stateMachine.add(current, i);
        }

//        List<Range> ranges = rangedElements.keySet().stream()
//                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
//        for (Range r :
//                ranges) {
//            System.out.println(subString(r));
//        }
        return objectArrayRanges;
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONString.
     *
     * @param range Range to specify the content enclosed by '"'s.
     * @return JSONString instance from the given range of the {@code jsonContext}
     */
    protected JSONString asJSONString(Range range) {
        return new JSONString(subString(range.getInside()));
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

        NamedValue[] namedValues = namedValueChildren.stream()
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
        IJSONValue[] values = elementRanges.stream()
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
        String name = subString(nameRange.getInside());
        IJSONValue value = (IJSONValue) rangedElements.get(valueRange).getValue();
        return new NamedValue(name, value);
    }

    /**
     * Check if the index is within any Range of stringRanges.
     *
     * @param index Index to be checked.
     * @return True if any Range contains it that make special char invalid for JSON parsing, otherwise False.
     */
    private boolean inAnyStringRange(Integer index) {
        checkNotNull(index);
        checkNotNull(stringRanges);

        for (Range range : stringRanges) {
            if (range.contains(index))
                return true;
            if (range.getStartInclusive() > index)
                return false;
        }

        return false;
    }


//    /**
//     * Scan all the characters of the jsonContext to keep indexes of special characters as defined in {@code MARKERS}
//     *
//     * @return Indexes of all special characters as a Map.
//     */
//    private List<Range> scan() {
//        List<Range> objectArrayRanges = new ArrayList<>();
//
//        CONTROLS.stream().forEach(c -> controlsIndexMap.put(c, new ArrayList<>()));
//
//        int lastBackSlash = -100;
//        int currentStringStart = Integer.MAX_VALUE;
//        int currentStringEnd = -1;
//        Stack<Integer> objectStarts = new Stack<>();
//        Stack<Integer> arrayStarts = new Stack<>();
//        Stack<Range> nameRanges = new Stack<>();
//        Range lastStringRange = null;
//        Range lastNameRange = null;
//        int valueStart = -1;
//        Stack<Boolean> isInObjectStack = new Stack<>();
//
//        //Get all indexes of all concerned markers
//        for (int i = 0; i < length; i++) {
//            char current = jsonContext.charAt(i);
//
//            //Ensure only escapable chars following BACK_SLASH
//            if(i == lastBackSlash+1) {
//                checkState(ESCAPABLE_CHARS.contains(current));
//                i++;
//
//                continue;
//            }
//
//            //Since the Quote is not escaped by BACK_SLASH, it shall be the start or end of a JSONString
//            if(current == QUOTE) {
//                if (currentStringEnd == Integer.MAX_VALUE) {
//                    //This position i is the end of an existing String scope, keep it with currentStringEnd
//                    currentStringEnd = i;
//                    Range range = Range.closed(currentStringStart, currentStringEnd);
//                    rangesLazyMap.put(range, new Lazy<>(() -> asJSONString(range)));
//                    lastStringRange = range;
//                    stringRanges.add(range);
//                } else {
//                    //This position i is the start of a new String scope
//                    currentStringStart = i;
//                    currentStringEnd = Integer.MAX_VALUE;
//                }
//                activeControls.add(current);
//                continue;
//            }
//
//            //Check if the position is within the scope of the current JSONString
//            if(i < currentStringEnd) {
//                //When the String has not ended
//                continue;
//            }
//
//            //Now the position is out of JSONString
//            switch (current) {
//                case LEFT_BRACE:
//                case LEFT_BRACKET:
//                    if(current == LEFT_BRACE) {
//                        isInObjectStack.push(true);
//                        objectStarts.push(i);
//                    } else {
//                        isInObjectStack.push(false);
//                        arrayStarts.push(i);
//                        valueStart = i;
//                    }
//
//                    if(lastNameRange != null) {
//                        nameRanges.push(lastNameRange);
//                        lastNameRange = null;
//                    }
//                    break;
//                case RIGHT_BRACE:
//                    checkState(isInObjectStack.pop(), "Incorrect state showing it is not exiting a JSONObject.");
//
//                    checkState(!objectStarts.isEmpty(),
//                            "There is no matched '{' before the '}' at position %d", i);
//                    //It close Value, NamedValue, JSONObject
//                    int lastObjectStart = objectStarts.pop();
//                    Range oRange = Range.closed(lastObjectStart, i);
//                    rangedElements.put(oRange, new Lazy<>(() -> asJSONObject(oRange)));
//                    objectArrayRanges.add(oRange);
//                    if(!nameRanges.isEmpty()) {
//                        Range nRange = nameRanges.pop();
//                        Range nvRange = nRange.intersection(oRange);
//                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, oRange)));
//                        stringRanges.remove(nRange);
//                    }
//                    break;
//                case RIGHT_BRACKET:
//                    checkState(!isInObjectStack.pop(), "Incorrect state showing it is not exiting a JSONArray.");
//                    checkState(!arrayStarts.isEmpty(),
//                            "There is no matched '[' before the ']' at position %d", i);
//                    int lastArrayStart = arrayStarts.pop();
//                    Range aRange = Range.closed(lastArrayStart, i);
//                    rangedElements.put(aRange, new Lazy<>(() -> asJSONOArray(aRange)));
//                    objectArrayRanges.add(aRange);
//                    if(!nameRanges.isEmpty()) {
//                        Range nRange = nameRanges.pop();
//                        Range nvRange = nRange.intersection(aRange);
//                        rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, aRange)));
//                        stringRanges.remove(nRange);
//                    }
//                    break;
//                case COLON:
//                    checkState(isInObjectStack.peek(), "COLON(:) shall present only within JSONObjects.");
//                    valueStart = i;
//                    checkNotNull(lastStringRange, "COLON(') must follow a JSONString immediately that is kept as lastStringRange.");
//                    lastNameRange = lastStringRange;
//                    lastStringRange = null;
//                    break;
//                case COMMA:
//                    Character lastControl = activeControls.get(activeControls.size()-1);
//                    if(lastControl.equals(QUOTE)){
//                        Range vRange = lastStringRange;
//                        if(isInObjectStack.peek()) {
//                            checkNotNull(lastNameRange);
//                            Range nRange = lastNameRange;
//                            Range nvRange = lastNameRange.intersection(vRange);
//                            rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, vRange)));
//                            stringRanges.remove(nRange);
//                            stringRanges.remove(vRange);
//                            lastNameRange = null;
//                            lastStringRange = null;
//                        } else {
//                            checkState(lastNameRange == null);
//                        }
//                    } else if(!lastControl.equals(RIGHT_BRACE) && !lastControl.equals(RIGHT_BRACKET)) {
//                        Range vRange = Range.open(valueStart, i);
//                        rangedElements.put(vRange, new Lazy<>(() -> asValue(vRange)));
//                        if(isInObjectStack.peek()) {
//                            checkNotNull(lastNameRange);
//                            Range nRange = lastNameRange;
//                            Range nvRange = lastNameRange.intersection(vRange);
//                            rangedElements.put(nvRange, new Lazy<>(() -> asNamedValue(nRange, vRange)));
//                            stringRanges.remove(nRange);
//                            lastNameRange = null;
//                        } else {
//                            checkState(lastNameRange == null,
//                                    "Invalid state: %s", subString(lastNameRange.intersection(vRange)));
//                        }
//                    }
//                    valueStart = i;
//                    break;
//                default:
//                    continue;
//            }
//
//            //Keep the index of this CONTROL char into its list
//            controlsIndexMap.get(current).add(i);
//            activeControls.add(current);
//        }
//
//        List<Range> ranges = rangedElements.keySet().stream()
//                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
//        for (Range r :
//                ranges) {
//            System.out.println(subString(r));
//        }
//        return objectArrayRanges;
//    }

    /**
     * Mark the Ranges of all NamedValues by starting with JSONObject and JSONArray.
     *
     * @param objectOrArrayRanges Ranges of either JSONObject or JSONArray.
     * @return Ranges of either JSONObject or JSONArray that have no names.
     */
    protected TreeSet<Range> demarcateNamedValues(List<Range> objectOrArrayRanges) {
        List<Range> unpairedStringRanges = new ArrayList<>(stringRanges);
        unpairedStringRanges.sort(Comparator.reverseOrder());
        Set<Range> consumedStringRange = new HashSet<>();

        TreeSet<Range> unnamedValueRanges = new TreeSet<>();
        Set<Integer> colonIndexes = new HashSet(controlsIndexMap.get(COLON));
        List<Integer> valueEndIndexes = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, COMMA).stream()
                .map(c -> controlsIndexMap.get(c))
                .flatMap(list -> list.stream())
                .sorted()
                .collect(Collectors.toList());
        TreeSet<Integer> specialIndexes = new TreeSet<>(controlsIndexMap.values().stream()
                .flatMap(list -> list.stream())
                .sorted()
                .collect(Collectors.toList()));

        //Ensure the pairing happens from the big ones to small ones
        objectOrArrayRanges.sort(Comparator.comparing(Range::size).reversed());
        //JSONObject or JSONArray can act as:
        // 1) Root node
        // 2) Child node of a bigger JSONArray
        // 3) Value part of "Name": Value
        for (Range objectArrayRange : objectOrArrayRanges) {
            Integer markerAhead = specialIndexes.lower(objectArrayRange.getStartInclusive());

            //When the node is the root
            if (markerAhead == null) {
                unnamedValueRanges.add(objectArrayRange);
                continue;
            }

            char marker = jsonContext.charAt(markerAhead);
            //When the node is a Child node of a bigger JSONArray
            if (VALUE_START_INDICATORS.contains(marker)) {
                unnamedValueRanges.add(objectArrayRange);
                continue;
            }

            //Now it can only be the Value of a NamedValue
            checkState(marker == COLON, "Invlid marker '%s' ahead of: %s", marker, subString(objectArrayRange));

            Range nameRange = unpairedStringRanges.stream()
                    .filter(range -> range.getStartInclusive() < objectArrayRange.getStartInclusive())
                    .findFirst().orElse(null);
            checkState(nameRange != null && specialIndexes.higher(nameRange.getEndInclusive()) == markerAhead,
                    "Invalid NamedValue: %s...", subString(Range.closed(nameRange.getStartInclusive(), objectArrayRange.getStartInclusive() + 1)));

            consumedStringRange.add(nameRange);
            Range nameValueRange = nameRange.intersection(objectArrayRange);
//            System.out.println(subString(nameValueRange));
            rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(nameRange, objectArrayRange)));
        }

        unpairedStringRanges.removeAll(consumedStringRange);
        unpairedStringRanges.sort(Comparator.naturalOrder());

        int size = unpairedStringRanges.size();
        //JSONString can act as: 1) Name part of "Name": Value; 2) Value part of Name": Value; 3) Child node of JSONArray
        for (int i = 0; i < size; i++) {
            Range nameRange = unpairedStringRanges.get(i);
            Integer markerIndexAfter = specialIndexes.ceiling(nameRange.getEndInclusive());
            checkNotNull(markerIndexAfter, "JSONString must be followed by some special marker chars");

            char marker = jsonContext.charAt(markerIndexAfter);
            if (VALUE_END_INDICATORS.contains(marker)) {
                //As Child node of JSONArray
                //Current JSONString shall be a value of a JSONArray
                rangedElements.put(nameRange, new Lazy<>(() -> asJSONString(nameRange)));
//                System.out.println(subString(nameRange));
                continue;
            }
            checkState(marker == COLON);

            if (i != size - 1) {
                //As Name part of "Name": Value;
                Range nextStringRange = unpairedStringRanges.get(i + 1);
                Integer markerBeforeNext = specialIndexes.lower(nextStringRange.getStartInclusive());
                if (markerBeforeNext == markerIndexAfter) {
                    consumedStringRange.add(nextStringRange);
                    Range nameValueRange = nameRange.intersection(nextStringRange);
                    rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(nameRange, nextStringRange)));
                    i++;
                    continue;
                }
            }

            //Now need to find the matched value that is not JSONString, JSONObject or JSONArray
            Integer valueEndIndex = specialIndexes.higher(markerIndexAfter);
            checkState(VALUE_END_INDICATORS.contains(jsonContext.charAt(valueEndIndex)),
                    "The value portion cannot be the end of JSON context.");

            Range valueRange = Range.open(markerIndexAfter, valueEndIndex);
            rangedElements.put(valueRange, new Lazy<>(() -> asValue(valueRange)));
            Range nameValueRange = nameRange.intersection(valueRange);
            rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(nameRange, valueRange)));

            consumedStringRange.add(nameRange);

        }

        //The root JSONValue shall be included as the one with largest size
        return unnamedValueRanges;
    }

    public IJSONValue parse() {
        if (parsedResult != null) {
            return parsedResult;
        }

        List<Range> objectOrArrayRanges = scan();

//        Map<Character, List<Integer>> activeCharIndexes = demarcateStrings(specialCharIndexes);
//
//        List<Range> objectOrArrayRanges = demarcateObjectAndArrays(activeCharIndexes);

        demarcateNamedValues(objectOrArrayRanges);

        Range largestRange = Collections.max(rangedElements.keySet(), Comparator.comparing(Range::size));

        IJSONValue result = (IJSONValue) rangedElements.get(largestRange).getValue();

        return result;
    }
}