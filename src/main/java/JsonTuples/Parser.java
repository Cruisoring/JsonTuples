package JsonTuples;

import com.sun.org.glassfish.gmbal.NameValue;
import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

    private TreeSet<Range> _sortedRanges = null;

    int bufferSize = 1024;
    int[] positions = new int[bufferSize];
    char[] controls = new char[bufferSize];
    int controlCount = -1;
    char lastControl = START_JSON_SIGN;

    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;
//    Stack<Tuple3<Boolean, Integer, Character>> segmentStack = new Stack<>();
    Boolean isInObject = null;
    Tuple3<Boolean, Integer, Range> segmentOpen = null;

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
        if (_sortedRanges == null || _sortedRanges.size() != rangedAdvices.size()) {
            _sortedRanges = new TreeSet<>(rangedAdvices.keySet());
        }
//        if (_sortedRanges == null || _sortedRanges.size() != rangedHandlers.size()) {
//            _sortedRanges = new TreeSet<>(rangedHandlers.keySet());
//        }
        return _sortedRanges;
    }

    protected  NamedValue asNamedValue(Range... ranges) {
        Range nameRange = ranges[1];
        Range valueRange = ranges[0];

        JSONString name = (JSONString) lazyRanges.get(nameRange).getValue();
        IJSONValue value = (IJSONValue) lazyRanges.get(valueRange).getValue();
        NamedValue namedValue = new NamedValue(name.getFirst(), value);
        return namedValue;
    }

//    protected NamedValue getNamedValue(Range namedValueRange){
//        if(rangedResults.containsKey(namedValueRange)){
//            return (NamedValue) rangedResults.get(namedValueRange);
//        }
//
//        List<Range> children = namedValueRange.getChildRanges(_sortedRanges);
//        if(children.size() != 2)
//            checkState(children.size() == 2);
//        return asNamedValue(children.get(1), children.get(0));
//    }
//
//    protected NamedValue asNamedValue(Range valueRange, Range nameRange) {
//        Range nameValueRange = nameRange.intersection(valueRange);
//        if(rangedResults.containsKey(nameValueRange)){
//            return (NamedValue) rangedResults.get(nameValueRange);
//        }
//
//        JSONString nameElement = asJSONString(nameRange);
//        IJSONValue value = asJSONValue(valueRange);
//        NamedValue namedValue = new NamedValue(nameElement.getFirst(), value);
//        rangedResults.put(nameValueRange, namedValue);
//        return namedValue;
//    }

    protected JSONString asJSONString(Range... ranges){
        String string = JSONString.unescapeJson(jsonContext, ranges[0].getStartInclusive()+1, ranges[0].getEndInclusive());
        return new JSONString(string);
    }

//    /**
//     * Parse the given Range of the {@code jsonContext} as a JSONString.
//     *
//     * @param range Range to specify the content enclosed by '"'s.
//     * @return JSONString instance from the given range of the {@code jsonContext}
//     */
//    protected JSONString asJSONString(Range range) {
//        String string = JSONString.unescapeJson(jsonContext, range.getStartInclusive()+1, range.getEndInclusive());
//        JSONString result = new JSONString(string);
//        rangedResults.put(range, result);
//        return result;
//    }

//    /**
//     * Parse the given Range of the {@code jsonContext} as a JSONObject.
//     *
//     * @param range Range to specify the content enclosed by '{' and '}'
//     * @return Parsed JSONObject instance from the given range of the {@code jsonContext}
//     */
//    protected JSONObject asJSONObject(Range range) {
//        if(rangedResults.containsKey(range))
//            return (JSONObject)rangedResults.get(range);
//
//        Range[] namedValueChildren = range.getChildRanges(_sortedRanges).toArray(new Range[0]);
////        System.out.println("???????" + subString(range) + ": as JSONObject");
////        namedValueChildren.forEach(r -> System.out.println(subString(r)));
//
//        return asJSONObject(range, namedValueChildren);
//    }

//    protected JSONObject asJSONObject(Range range, Range[] childrenRanges) {
//        NamedValue[] namedValues = Arrays.stream(childrenRanges).parallel()
//                .map(child -> getNamedValue(child))
//                .toArray(size -> new NamedValue[size]);
//        JSONObject object = new JSONObject(namedValues);
//        rangedResults.put(range, object);
//        return object;
//    }

    protected JSONObject asJSONObject(Range... ranges){
        NamedValue[] namedValues = Arrays.stream(ranges).parallel()
                .map(range -> (NamedValue) (lazyRanges.get(range).getValue()))
                .toArray(size -> new NamedValue[size]);
        JSONObject object = new JSONObject(namedValues);
        return object;
    }

    protected JSONArray asJSONOArray(Range...ranges){
        IJSONValue[] values = Arrays.stream(ranges).parallel()
                .map(range -> (IJSONValue)(lazyRanges.get(range).getValue()))
                .toArray(size -> new IJSONValue[size]);
        JSONArray array = new JSONArray(values);
        return array;
    }

//    /**
//     * Parse the given Range of the {@code jsonContext} as a JSONArray.
//     *
//     * @param range Range to specify the content enclosed by '[' and ']'
//     * @return Parsed JSONArray instance from the given range of the {@code jsonContext}
//     */
//    protected JSONArray asJSONOArray(Range range) {
//        if(rangedResults.containsKey(range))
//            return (JSONArray) rangedResults.get(range);
//
//        Range[] elementRanges = range.getChildRanges(getSortedRanges()).toArray(new Range[0]);
//
//        return asJSONOArray(range, elementRanges);
//    }
//
//    protected JSONArray asJSONOArray(Range range, Range[] children) {
//        IJSONValue[] values = Arrays.stream(children).parallel()
//                .map(r -> asJSONValue(r))
//                .toArray(i -> new IJSONValue[i]);
//        JSONArray array = new JSONArray(values);
//        rangedResults.put(range, array);
//        return array;
//    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONNumber.
     *
     * @param range Range to specify the content
     * @return Parsed JSONNumber instance from the given range of the {@code jsonContext}
     */
    protected JSONNumber asJSONNumber(Range range) {
        if(rangedResults.containsKey(range))
            return (JSONNumber) rangedResults.get(range);

        String valueString = subString(range).trim();

        JSONNumber number = JSONNumber.parseNumber(valueString);
        rangedResults.put(range, number);
        return number;
    }


    protected IJSONValue asJSONValue(Range range) {
        String valueString = subString(range).trim();

        switch (valueString) {
            case JSON_FALSE:
                rangedResults.put(range, JSONValue.False);
                return JSONValue.False;
            case JSON_TRUE:
                rangedResults.put(range, JSONValue.True);
                return JSONValue.True;
            case JSON_NULL:
                rangedResults.put(range, JSONValue.Null);
                return JSONValue.Null;
            default:
                char firstChar = valueString.charAt(0);
                if (firstChar == LEFT_BRACE) {
                    return asJSONObject(range);
                } else if (firstChar == LEFT_BRACKET) {
                    return asJSONOArray(range);
                } else if (firstChar == QUOTE) {
                    return asJSONString(range);
                } else {
                    return asJSONNumber(range);
                }
        }
    }

    public IJSONValue getRootNode() {
        return rootNodeLazy.getValue();
    }

    protected JSONString extractJSONString(Range... ranges) {
        return asJSONString(ranges[0]);
    }

    protected JSONObject extractJSONObject(Range... ranges) {
        return asJSONObject(ranges[0]);
    }

    protected JSONArray extractJSONArray(Range... ranges) {
        return asJSONOArray(ranges[0]);
    }

    protected IJSONValue extractJSONValue(Range... ranges){
        return asJSONValue(ranges[0]);
    }

    protected IJSONable extractNamedValue(Range... ranges){
        return asNamedValue(ranges[0], ranges[1]);
    }

    protected IJSONable setRoot(Range... ranges){
        rootRange = ranges[0];
        return null;
    }

    Range rootRange = null;
    public Map<Integer, List<Range>> traverse() {
//        rangedHandlers.clear();
        rangedAdvices.clear();
        boolean isInString = false;

        rangesByDepth.clear();
        rangesByDepth.put(0, new ArrayList<>());
        rangesByDepth.put(1, new ArrayList<>());

        //Identify the JSONString elements, save all control characters
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
                measure(current, i);
                continue;
            }

            //Check if the position is within the scope of the current JSONString
            if (isInString || current < MIN_CONTROL || current > MAX_CONTROL) {
                //When the String has not ended, or not any control char
                continue;
            }

            if(CONTROLS.contains(current)) {
                measure(current, i);
            }
        }
        return rangesByDepth;
    }

    Map<Integer, List<Range>> rangesByDepth = new HashMap<>();

    int currentDepth = 0;
    Range lastStringRange = null;
    Range lastNameRange = null;
    List<Range> childrenOfSegment = new ArrayList<>();
    List<Tuple2<Range, Integer>> currentChildrenWithDepth = new ArrayList<>();
    Map<Range, Lazy<IJSONable>> lazyRanges = new HashMap<>();
    final Stack<Tuple6<Boolean, Character, Integer, Range, Range[], Integer>> parentStateStack = new Stack<>();

    void measure(char control, int position) {
        Tuple3<Integer, Function<Range[], IJSONable>, Range[]> closeSegmentHandler = null;
        Tuple2<Function<Range[], IJSONable>, Range[]> handler = null;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
            case LEFT_BRACKET:  //Start of JSONArray
                final Range nameRange = lastNameRange;
                Tuple6<Boolean, Character, Integer, Range, Range[], Integer> stateInParent = Tuple.create(
                        isInObject, control, position, nameRange, childrenOfSegment.toArray(new Range[0]), currentDepth);
                childrenOfSegment.clear();
                parentStateStack.push(stateInParent);
                isInObject = control == LEFT_BRACE;
                currentDepth = 0;
                break;
            case RIGHT_BRACE:   //End of current JSONObject
                switch (lastControl) {
                    case COLON:
                        final Range vRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(vRange, new Lazy<>(() -> asJSONValue(vRange)));
                        rangesByDepth.get(0).add(vRange);

                        final Range nvRange = lastNameRange.intersection(vRange);
                        final Range nRange = lastNameRange;
                        lazyRanges.put(nvRange, new Lazy<>(() -> asNamedValue(vRange, nRange)));
                        childrenOfSegment.add(nvRange);
                        currentDepth = currentDepth > 2 ? currentDepth : 2;
                        rangesByDepth.get(1).add(nvRange);
                        break;
                    case QUOTE:
                        final Range nameValueRange = lastNameRange.intersection(lastStringRange);
                        final Range vRange1 = lastStringRange;
                        final Range nRange1 = lastNameRange;
                        lazyRanges.put(nameValueRange, new Lazy<>(() -> asNamedValue(vRange1, nRange1)));
                        childrenOfSegment.add(nameValueRange);
                        rangesByDepth.get(1).add(nameValueRange);
                        currentDepth = currentDepth > 2 ? currentDepth : 2;
                        break;
                    default:
                        break;
                }

                closeObjectOrArray(control, position, childrenOfSegment.toArray(new Range[0]), currentDepth);
                break;
            case RIGHT_BRACKET: //Close of current JSONArray
                switch (lastControl) {
                    case COMMA:
                        final Range valueRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                        childrenOfSegment.add(valueRange);
                        rangesByDepth.get(0).add(valueRange);
                        currentDepth = currentDepth > 1 ? currentDepth : 1;
                        break;
                    case LEFT_BRACKET:
                        final Range valueRange2 = Range.open(positions[controlCount], position);
                        if(!StringUtils.isBlank(subString(valueRange2))){
                            lazyRanges.put(valueRange2, new Lazy<>(() -> asJSONValue(valueRange2)));
                            childrenOfSegment.add(valueRange2);
                            rangesByDepth.get(0).add(valueRange2);
                        }
                        break;
                    default:
                        break;
                }

                closeObjectOrArray(control, position, childrenOfSegment.toArray(new Range[0]), currentDepth);
                break;
            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
                currentDepth = currentDepth > 1 ? currentDepth : 1;
                switch (lastControl) {
                    case COMMA:
                        final Range valueRange3 = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange3, new Lazy<>(() -> asJSONValue(valueRange3)));
                        rangesByDepth.get(0).add(valueRange3);
                        childrenOfSegment.add(valueRange3);
                        break;
                    case COLON:
                        final Range valueRange4 = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange4, new Lazy<>(() -> asJSONValue(valueRange4)));
                        rangesByDepth.get(0).add(valueRange4);

                        final Range nameValueRange4 = lastNameRange.intersection(valueRange4);
                        final Range lastNameRange4 = lastNameRange;
                        lazyRanges.put(nameValueRange4, new Lazy<>(() -> asNamedValue(valueRange4, lastNameRange4)));
                        rangesByDepth.get(1).add(nameValueRange4);
                        childrenOfSegment.add(nameValueRange4);
                        break;
                    case QUOTE:
                        if(isInObject) {
                            final Range nameValueRange5 = lastNameRange.intersection(lastStringRange);
                            final Range nameRange5 = lastNameRange;
                            final Range valueRange5 = lastStringRange;
                            lazyRanges.put(nameValueRange5, new Lazy(() -> asNamedValue(valueRange5, nameRange5)));
                            rangesByDepth.get(1).add(nameValueRange5);
                            childrenOfSegment.add(nameValueRange5);
                        }
                    default:
                        break;
                }
                break;
            case COLON:
                lastNameRange = lastStringRange;
                lastStringRange = null;
                currentDepth = currentDepth > 2 ? currentDepth : 2;
                break;
            case QUOTE:
                if (currentStringEnd == Integer.MAX_VALUE) {
                    currentStringEnd = position;
                    final Range valueRange = Range.closed(currentStringStart, currentStringEnd);
                    lastStringRange = valueRange;
                    //JSONString shall always be parsed first
                    rangesByDepth.get(0).add(lastStringRange);
                    lazyRanges.put(valueRange, new Lazy<>(() -> asJSONString(valueRange)));
                    if(!isInObject){
                        childrenOfSegment.add(lastStringRange);
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
        if(++controlCount == bufferSize) {
            bufferSize = bufferSize *2;
            controls = Arrays.copyOf(controls, bufferSize);
            positions = Arrays.copyOf(positions, bufferSize);
        }
        controls[controlCount] = control;
        positions[controlCount] = position;
        lastControl = control;
    }

    void closeObjectOrArray(char control, int position, Range[] childrenRanges, int depth) {
        childrenOfSegment.clear();
        //The elements show: 1) isInObject, StartChar, StartPosition, NameRange, ChildrenRanges
        Tuple6<Boolean, Character, Integer, Range, Range[], Integer> parentState = parentStateStack.pop();
        isInObject = parentState.getFirst();
        currentDepth = parentState.getSixth();
        childrenOfSegment.addAll(Arrays.asList(parentState.getFifth()));

        //Register newly closed Object or Array
        Range thisRange = Range.closed(parentState.getThird(), position);
        if(control==RIGHT_BRACE) {
            //This is a JSONObject
            checkState(parentState.getSecond().equals('{'));
            lazyRanges.put(thisRange, new Lazy<>(() -> asJSONObject(childrenRanges)));
        } else {
            checkState(parentState.getSecond().equals('['));
            lazyRanges.put(thisRange, new Lazy<>(() -> asJSONOArray(childrenRanges)));
        }

        if(!rangesByDepth.containsKey(depth)){
            rangesByDepth.put(depth, new ArrayList<>());
        }
        rangesByDepth.get(depth).add(thisRange);

        Range nameRange = parentState.getFourth();
        if(nameRange == null){
            //Add the closed JSONObject/JSONArray to the children list of the current JSONObject
            childrenOfSegment.add(thisRange);
            currentDepth = currentDepth > depth+1 ? currentDepth : depth+1;
        } else {
            //Add the closed JSONObject/JSONArray to the children list of the current JSONArray
            Range nameValueRange = nameRange.intersection(thisRange);
            lazyRanges.put(nameValueRange, new Lazy(() -> asNamedValue(nameRange, thisRange)));
            if(!rangesByDepth.containsKey(depth+1)){
                rangesByDepth.put(depth+1, new ArrayList<>());
            }
            rangesByDepth.get(depth+1).add(nameValueRange);
            childrenOfSegment.add(nameValueRange);
            currentDepth = currentDepth > depth+2 ? currentDepth : depth+2;
        }
    }



    final Map<Range, Tuple3<Boolean, Function<Range[], IJSONable>, Range[]>> rangedAdvices = new HashMap<>();
//    final Map<Range, LazyFactory<Range, IJSONable>> rangedHandlers = new HashMap<>();
    final Stack<Tuple3<Boolean, Integer, Range>> segmentStack = new Stack<>();

//    void closeSegment(char control, int position) {
//        Tuple4<Boolean, Character, Integer, Range> parentState = parentStateStack.pop();
//        isInObject = parentState.getFirst();
//        Range thisRange = Range.closed(parentState.getThird(), position);
//        if(control==RIGHT_BRACE) {
//            checkState(parentState.getSecond().equals('{'));
//            rangedAdvices.put(thisRange, Tuple.create(false, this::extractJSONObject, new Range[]{thisRange}));
////            rangedHandlers.put(thisRange, new LazyFactory<>(this::asJSONObject, thisRange));
//        } else {
//            checkState(parentState.getSecond().equals('['));
//            rangedAdvices.put(thisRange, Tuple.create(false, this::extractJSONArray, new Range[]{thisRange}));
////            rangedHandlers.put(thisRange, new LazyFactory<>(this::asJSONArray, thisRange));
//        }
//
//        Range nRange = parentState.getFourth();
//        if(nRange == null){
//            rootRange = thisRange;
////            _sortedRanges = new TreeSet<>(rangedHandlers.keySet());
//            _sortedRanges = new TreeSet<>(rangedAdvices.keySet());
//        } else {
//            Range nvRange = nRange.intersection(thisRange);
////            rangedHandlers.put(nvRange, new LazyFactory<>(this::asNamedValue, thisRange, nRange));
//            rangedAdvices.put(nvRange, Tuple.create(false, this::extractNamedValue, new Range[]{thisRange, nRange}));
//        }
//    }

//    void getHandlers(char control, int position) {
//        Tuple3<Integer, Function<Range[], IJSONable>, Range[]> closeSegmentHandler = null;
//        Tuple2<Function<Range[], IJSONable>, Range[]> handler = null;
//        Range valueRange, nameRange, nameValueRange;
//        switch (control) {
//            case LEFT_BRACE:    //Start of JSONObject
//            case LEFT_BRACKET:  //Start of JSONArray
//                nameRange = lastControl == COLON ? Range.closed(positions[controlCount-2], positions[controlCount-1]) : null;
//                Tuple4<Boolean, Character, Integer, Range> parentState = Tuple.create(isInObject, control, position, nameRange);
//                parentStateStack.push(parentState);
//                isInObject = control == LEFT_BRACE;
//                break;
//            case RIGHT_BRACE:   //End of current JSONObject
//                switch (lastControl) {
//                    case COLON:
//                        nameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
//                        valueRange = Range.open(positions[controlCount], position);
////                        rangedHandlers.put(valueRange, new LazyFactory<>(this::asJSONValue, valueRange));
//                        rangedAdvices.put(valueRange, Tuple.create(true, this::extractJSONValue, new Range[]{valueRange}));
//
//                        nameValueRange = nameRange.intersection(valueRange);
////                        rangedHandlers.put(nameValueRange, new LazyFactory<>(this::asNamedValue, valueRange, nameRange));
//                        rangedAdvices.put(nameValueRange, Tuple.create(false, this::extractNamedValue, new Range[]{valueRange, nameRange}));
//                        break;
//                    case QUOTE:
//                        nameRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
//                        valueRange = Range.closed(positions[controlCount-1], positions[controlCount]);
//                        nameValueRange = nameRange.intersection(valueRange);
////                        rangedHandlers.put(nameValueRange, new LazyFactory<>(this::asNamedValue, valueRange, nameRange));
//                        rangedAdvices.put(nameValueRange, Tuple.create(false, this::extractNamedValue, new Range[]{valueRange, nameRange}));
//                        break;
//                    default:
//                        break;
//                }
//
//                closeSegment(control, position);
//                break;
//            case RIGHT_BRACKET: //Close of current JSONArray
//                switch (lastControl) {
//                    case COMMA:
//                        valueRange = Range.open(positions[controlCount], position);
////                        rangedHandlers.put(valueRange, new LazyFactory<>(this::asJSONValue, valueRange));
//                        rangedAdvices.put(valueRange, Tuple.create(true, this::extractJSONValue, new Range[]{valueRange}));
//                        break;
//                    default:
//                        break;
//                }
//
//                closeSegment(control, position);
//                break;
//            case COMMA: //End of Value in JSONArray or NamedValue in JSONObject
//                switch (lastControl) {
//                    case COMMA:
//                        valueRange = Range.open(positions[controlCount], position);
////                        rangedHandlers.put(vRange, new LazyFactory<>(this::asJSONValue, valueRange));
//                        rangedAdvices.put(valueRange, Tuple.create(true, this::extractJSONValue, new Range[]{valueRange}));
//                        break;
//                    case COLON:
//                        nameRange = Range.closed(positions[controlCount-2], positions[controlCount-1]);
//                        valueRange = Range.open(positions[controlCount], position);
////                        rangedHandlers.put(valueRange, new LazyFactory<>(this::asJSONValue, valueRange));
//                        rangedAdvices.put(valueRange, Tuple.create(true, this::extractJSONValue, new Range[]{valueRange}));
//                        nameValueRange = nameRange.intersection(valueRange);
////                        rangedHandlers.put(nameValueRange, new LazyFactory<>(this::asNamedValue, valueRange, nameRange));
//                        rangedAdvices.put(nameValueRange, Tuple.create(false, this::extractNamedValue, new Range[]{valueRange, nameRange}));
//                        break;
//                    case QUOTE:
//                        if(isInObject) {
//                            nameRange = Range.closed(positions[controlCount-4], positions[controlCount-3]);
//                            valueRange = Range.closed(positions[controlCount-1], positions[controlCount]);
//                            nameValueRange = nameRange.intersection(valueRange);
////                            rangedHandlers.put(nameValueRange, new LazyFactory<>(this::asNamedValue, valueRange, nameRange));
//                            rangedAdvices.put(nameValueRange, Tuple.create(false, this::extractNamedValue, new Range[]{valueRange, nameRange}));
//                        }
//                    default:
//                        break;
//                }
//                break;
//            case QUOTE:
//                if (currentStringEnd == Integer.MAX_VALUE) {
//                    //This position is the end of an existing String scope, keep it with currentStringEnd
//                    currentStringEnd = position;
//                    valueRange = Range.closed(currentStringStart, currentStringEnd);
////                    rangedHandlers.put(valueRange, new LazyFactory<>(this::asJSONString, valueRange));
//                    rangedAdvices.put(valueRange, Tuple.create(true, this::extractJSONString, new Range[]{valueRange}));
//                } else {
//                    //This position is the start of a new String scope
//                    currentStringStart = position;
//                    currentStringEnd = Integer.MAX_VALUE;
//                }
//                break;
//
//            default:
//                break;
//        }
//        if(++controlCount == bufferSize) {
//            bufferSize = bufferSize *2;
//            controls = Arrays.copyOf(controls, bufferSize);
//            positions = Arrays.copyOf(positions, bufferSize);
//        }
//        controls[controlCount] = control;
//        positions[controlCount] = position;
//        lastControl = control;
//    }

    final Map<Range, IJSONable> rangedResults = new HashMap<>();

    void extract(Map<Range, Tuple3<Boolean, Function<Range[], IJSONable>, Range[]>> advices) {
        rangedResults.clear();

        //Handle leave nodes first
        advices.entrySet().stream().parallel()
                .filter(entry -> entry.getValue().getFirst())
                .forEach(entry -> {
                    Tuple3<Boolean, Function<Range[], IJSONable>, Range[]> tuple = entry.getValue();
                    IJSONable value = tuple.getSecond().apply(tuple.getThird());
                    rangedResults.put(entry.getKey(), value);
                });

        //Handle composed nodes first
        advices.entrySet().stream()
                .filter(entry -> !entry.getValue().getFirst())
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Range::size)))
                .forEach(entry -> {
                    Tuple3<Boolean, Function<Range[], IJSONable>, Range[]> tuple = entry.getValue();
                    IJSONable value = tuple.getSecond().apply(tuple.getThird());
                    rangedResults.put(entry.getKey(), value);
                });

    }

    //*/


    public IJSONable parse() {

        LocalDateTime start = LocalDateTime.now();
        /*/
        Map<Range, Tuple3<Boolean, Function<Range[], IJSONable>, Range[]>> handlers = traverse();
        /*/
        Map<Integer, List<Range>> groupedRanges = traverse();
        //*/
        Duration traverseDuration = Duration.between(start, LocalDateTime.now());
        System.out.println(String.format("Traverse costs %s", traverseDuration));

        start = LocalDateTime.now();
        for (int i = 0; i < groupedRanges.size(); i++) {
            List<Range> ranges = groupedRanges.get(i);
            System.out.println("**************************Values at level " + i);
            ranges.stream().forEach(range ->
                    System.out.println(lazyRanges.get(range).getValue()));
        }

        Duration extractDuration = Duration.between(start, LocalDateTime.now());
        System.out.println(String.format("Extraction costs %s", extractDuration));

//        List<Range> rangesBySize = handlers.keySet().stream()
//                .sorted(Comparator.comparing(Range::size))
//                .collect(Collectors.toList());
//
//        rangesBySize.stream().forEach(
//                range -> {
//                    Tuple3<BiFunction<Range, Range, IJSONable>, Range, Range> tuple = handlers.get(range);
//                    IJSONable element = tuple.getFirst().apply(tuple.getSecond(), tuple.getThird());
//                    rangedElements.put(range, new Lazy<>(() -> element));
//                }
//        );
        Duration evaluateDuration = Duration.between(start, LocalDateTime.now());
        System.out.println(String.format("Evaluate costs %s", evaluateDuration));

        return rangedResults.get(rootRange);
    }
}