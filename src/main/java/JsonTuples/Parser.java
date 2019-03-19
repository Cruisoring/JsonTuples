package JsonTuples;

import io.github.cruisoring.Lazy;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple3;
import io.github.cruisoring.tuple.Tuple6;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Utility to parse JSON text based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {
    //region static variables
    public static final char START_JSON_SIGN = '^';
    public static final int PARALLEL_EVALUATE_THRESHOLD = 100;

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

    //region instance variables
    public final String jsonContext;
    public final int length;

    int bufferSize = 1024;
    int[] positions = new int[bufferSize];
    char[] controls = new char[bufferSize];
    int controlCount = -1;
    char lastControl = START_JSON_SIGN;

    int currentStringStart = Integer.MAX_VALUE;
    int currentStringEnd = -1;
    Boolean isInObject = null;
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

    protected  NamedValue asNamedValue(Range... ranges) {
        Range nameRange = ranges[1];
        Range valueRange = ranges[0];

        JSONString name = (JSONString) lazyRanges.get(nameRange).getValue();
        IJSONValue value = (IJSONValue) lazyRanges.get(valueRange).getValue();
        if(value == null) {
            System.out.println(String.format("Failed to get value from %s: \n%s",
                    valueRange.toString(), subString(valueRange)));
        }
        NamedValue namedValue = new NamedValue(name.getFirst(), value);
        return namedValue;
    }

    protected JSONString asJSONString(Range... ranges){
        String string = JSONString._unescapeJson(jsonContext, ranges[0].getStartInclusive()+1, ranges[0].getEndInclusive());
        return new JSONString(string);
    }

    protected JSONObject asJSONObject(Range... ranges){
        NamedValue[] namedValues = Arrays.stream(ranges).parallel()
                .map(range -> (NamedValue) (lazyRanges.get(range).getValue()))
                .toArray(size -> new NamedValue[size]);
        JSONObject object = new JSONObject(namedValues);
        return object;
    }

    protected JSONArray asJSONOArray(Range...ranges){
        //*/
        Stream<Range> rangeStream = Arrays.stream(ranges);
        if(ranges.length > PARALLEL_EVALUATE_THRESHOLD){
            rangeStream = rangeStream.parallel();
        }

        IJSONValue[] values = rangeStream
                .map(range -> (IJSONValue)(lazyRanges.get(range).getValue()))
                .toArray(size -> new IJSONValue[size]);
        /*/
        int size = ranges.length;
        IJSONValue[] values = new IJSONValue[size];
        for (int i = 0; i < size; i++) {
            IJSONable value = lazyRanges.get(ranges[i]).getValue();
            values[i] = (IJSONValue) value;
        }
        //*/

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

    public Map<Integer, List<Range>> traverse() {
//        rangedHandlers.clear();
//        rangedAdvices.clear();
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

    final Map<Integer, List<Range>> rangesByDepth = new HashMap<>();

    private int _nodesCount = -1;
    public int nodesCount() {
        return _nodesCount;
    }

    int currentDepth = 0;
    Range lastStringRange = null;
    Range lastNameRange = null;
    List<Range> childrenOfSegment = new ArrayList<>();
    Map<Range, Lazy<IJSONable>> lazyRanges = new HashMap<>();
    final Stack<Tuple6<Boolean, Character, Integer, Range, Range[], Integer>> parentStateStack = new Stack<>();

    void measure(char control, int position) {
        final Range nameRange, valueRange, nameValueRange;
        switch (control) {
            case LEFT_BRACE:    //Start of JSONObject
            case LEFT_BRACKET:  //Start of JSONArray
                nameRange = lastControl == COLON ? lastNameRange : null;
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
                        valueRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                        rangesByDepth.get(0).add(valueRange);

                        nameValueRange = lastNameRange.intersection(valueRange);
                        nameRange = lastNameRange;
                        lazyRanges.put(nameValueRange, new Lazy<>(() -> asNamedValue(valueRange, nameRange)));
                        childrenOfSegment.add(nameValueRange);
                        currentDepth = currentDepth > 2 ? currentDepth : 2;
                        rangesByDepth.get(1).add(nameValueRange);
                        break;
                    case QUOTE:
                        nameValueRange = lastNameRange.intersection(lastStringRange);
                        valueRange = lastStringRange;
                        nameRange = lastNameRange;
                        lazyRanges.put(nameValueRange, new Lazy<>(() -> asNamedValue(valueRange, nameRange)));
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
                        valueRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                        childrenOfSegment.add(valueRange);
                        rangesByDepth.get(0).add(valueRange);
                        currentDepth = currentDepth > 1 ? currentDepth : 1;
                        break;
                    case LEFT_BRACKET:
                        valueRange = Range.open(positions[controlCount], position);
                        if(!StringUtils.isBlank(subString(valueRange))){
                            lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                            childrenOfSegment.add(valueRange);
                            rangesByDepth.get(0).add(valueRange);
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
                        valueRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                        rangesByDepth.get(0).add(valueRange);
                        childrenOfSegment.add(valueRange);
                        break;
                    case COLON:
                        valueRange = Range.open(positions[controlCount], position);
                        lazyRanges.put(valueRange, new Lazy<>(() -> asJSONValue(valueRange)));
                        rangesByDepth.get(0).add(valueRange);

                        nameValueRange = lastNameRange.intersection(valueRange);
                        nameRange = lastNameRange;
                        lazyRanges.put(nameValueRange, new Lazy<>(() -> asNamedValue(valueRange, nameRange)));
                        rangesByDepth.get(1).add(nameValueRange);
                        childrenOfSegment.add(nameValueRange);
                        break;
                    case QUOTE:
                        if(isInObject) {
                            nameValueRange = lastNameRange.intersection(lastStringRange);
                            nameRange = lastNameRange;
                            valueRange = lastStringRange;
                            lazyRanges.put(nameValueRange, new Lazy(() -> asNamedValue(valueRange, nameRange)));
                            rangesByDepth.get(1).add(nameValueRange);
                            childrenOfSegment.add(nameValueRange);
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
                    valueRange = Range.closed(currentStringStart, currentStringEnd);
                    lastStringRange = valueRange;
                    //JSONString shall always be parsed first
                    rangesByDepth.get(0).add(valueRange);
                    lazyRanges.put(valueRange, new Lazy<>(() -> asJSONString(valueRange)));
                    if(!isInObject){
                        childrenOfSegment.add(valueRange);
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
            lazyRanges.put(nameValueRange, new Lazy(() -> asNamedValue(thisRange, nameRange)));
            if(!rangesByDepth.containsKey(depth+1)){
                rangesByDepth.put(depth+1, new ArrayList<>());
            }
            rangesByDepth.get(depth+1).add(nameValueRange);
            childrenOfSegment.add(nameValueRange);
            currentDepth = currentDepth > depth+2 ? currentDepth : depth+2;
        }
    }

    final Map<Range, Tuple3<Boolean, Function<Range[], IJSONable>, Range[]>> rangedAdvices = new HashMap<>();

    final Map<Range, IJSONable> rangedResults = new HashMap<>();
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
        Range topRange = null;
        _nodesCount = 0;
        for (int i = 0; i < groupedRanges.size(); i++) {
            List<Range> ranges = groupedRanges.get(i);
            if(ranges.isEmpty()){
                continue;
            }
            _nodesCount += ranges.size();
            /*/
            System.out.println("**************************Values at level " + i);
            ranges.stream().forEach(range -> System.out.println(lazyRanges.get(range).getValue()));
            /*/
            ranges.stream().parallel().forEach(range -> lazyRanges.get(range).getValue());
//            System.out.println(String.format("At level %d: %s", i, lazyRanges.get(ranges.get(0)).getValue()));
            //*/
            if(ranges.size() == 1) {
                topRange = ranges.get(0);
            }
        }

        Duration evaluateDuration = Duration.between(start, LocalDateTime.now());
        System.out.println(String.format("Evaluate costs %s", evaluateDuration));

        checkState(lazyRanges.get(topRange).isValueInitialized());
        return lazyRanges.get(topRange).getValue();
    }
}