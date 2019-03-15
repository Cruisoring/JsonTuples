package JsonTuples;

import io.github.cruisoring.Lazy;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
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

    final static Set<Character> MARKERS = new HashSet<Character>(
            Arrays.asList(LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET, COMMA, COLON, QUOTE, BACK_SLASH));
    final static List<Character> VALUE_START_INDICATORS = Arrays.asList(COMMA, LEFT_BRACE, LEFT_BRACKET);
    final static List<Character> VALUE_END_INDICATORS = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, COMMA);
    final static Character MIN_MARKER = Collections.min(MARKERS);
    final static Character MAX_MARKER = Collections.max(MARKERS);
    //endregion

    /**
     * Parse the remaining part of NamedValue other than Name to an IJSONValue instance.
     * @param jsonText      All JSON Text to be parsed.
     * @param valueRange    Range of the value portion of the NameValuePair, shall be led by COLON ':' with optional spaces.
     * @return      Either a simple JSON value (true, false, null, number, string) or compound JSON Object or Array.
     */
    public static IJSONValue parseValuePortion(String jsonText, Range valueRange) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkNotNull(valueRange);

        String colonAndValue = Range.subString(jsonText, valueRange).trim();
        checkState(colonAndValue.charAt(0)==COLON, "The value of the NameValuePair must be led by a COLON ':'.");

        return JSONValue.parse(colonAndValue.substring(1));
    }


    /**
     * Find the subList of the given index list within a specific range.
     * @param allIndexes    Indexes which would not contain the lower and upper end point of the range.
     * @param range         Range under concern.
     * @return              Sublist of the given sorted index list within a specific range.
     */
    public static List<Integer> getIndexesInRange(List<Integer> allIndexes, Range range) {
        checkNotNull(allIndexes);
        checkNotNull(range);

        if(allIndexes.isEmpty()) {
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
        if(count == 0 || lower > allIndexes.get(count-1) || upper < allIndexes.get(0)) {
            return result;
        }

        boolean belowRange = true;
        for (int i = 0; i < count; i++) {
            Integer index = allIndexes.get(i);

            if(belowRange) {
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
            if(nameRange != null) {
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

            if(nameRange == null)
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
    private Map<Character, Set<Integer>> specialCharIndexes;

    private IJSONValue parsedResult = null;

    private List<Range> stringRanges = null;

    private final Map<Range, Lazy<IJSONable>> rangedElements = new HashMap<>();

    private TreeSet<Range> unnamedValueRanges = null;

    private TreeSet<Range> _sortedRanges = null;
    private TreeSet<Range> getSortedRanges() {
        if(_sortedRanges == null || _sortedRanges.size() != rangedElements.size()) {
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
     * @param range Range to be extracted.
     * @return      SubString of the content relative to the saved jsonContext.
     */
    public String subString(Range range) {
        checkNotNull(range);

        int end = range.getEndExclusive();
        return jsonContext.substring(range.getStartInclusive(), end>length?length:end);
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONString.
     * @param range Range to specify the content enclosed by '"'s.
     * @return      JSONString instance from the given range of the {@code jsonContext}
     */
    protected JSONString asJSONString(Range range) {
        return new JSONString(subString(range.getInside()));
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONObject.
     * @param range Range to specify the content enclosed by '{' and '}'
     * @return      Parsed JSONObject instance from the given range of the {@code jsonContext}
     */
    protected JSONObject asJSONObject(Range range) {
        List<Range> namedValueChildren = range.getChildRanges(getSortedRanges());
//        System.out.println("???????" + subString(range) + ": as JSONObject");
//        namedValueChildren.forEach(r -> System.out.println(subString(r)));

        NamedValue[] namedValues = namedValueChildren.stream()
                .map(child -> (NamedValue)rangedElements.get(child).getValue())
                .toArray(size -> new NamedValue[size]);
        JSONObject object = new JSONObject(namedValues);
        return object;
    }

    /**
     * Parse the given Range of the {@code jsonContext} as a JSONArray.
     * @param range Range to specify the content enclosed by '[' and ']'
     * @return      Parsed JSONArray instance from the given range of the {@code jsonContext}
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
     * @param range Range to specify the content
     * @return      Parsed JSONNumber instance from the given range of the {@code jsonContext}
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
                if(firstChar == LEFT_BRACE) {
                    return asJSONObject(range);
                } else if(firstChar == LEFT_BRACKET) {
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
     * @param index Index to be checked.
     * @return  True if any Range contains it that make special char invalid for JSON parsing, otherwise False.
     */
    private boolean inAnyStringRange(Integer index) {
        checkNotNull(index);
        checkNotNull(stringRanges);

        for (Range range : stringRanges) {
            if(range.contains(index))
                return true;
            if(range.getStartInclusive() > index)
                return false;
        }

        return false;
    }

    /**
     * Scan all the characters of the jsonContext to keep indexes of special characters as defined in {@code MARKERS}
     *
     * @return Indexes of all special characters as a Map.
     */
    private Map<Character, List<Integer>> scanSpecialIndexes() {
        final Map<Character, List<Integer>> markersIndexes = new HashMap<Character, List<Integer>>();
        MARKERS.stream().forEach(c -> markersIndexes.put(c, new ArrayList<>()));

        //Get all indexes of all concerned markers
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);
            if(current < MIN_MARKER || current > MAX_MARKER || !MARKERS.contains(current)){
                continue;
            }
            markersIndexes.get(current).add(i);
        }

        return markersIndexes;
    }

    /**
     * Get ranges of JSON String instances, and return indexes of special chars out of these JSONString instances.
     * @param markersIndexes    Known special char indexes organised as a Map.
     *
     * @return Indexes of special chars out of these JSONString instances, not including '"' since all shall be consumed.
     */
    private Map<Character, List<Integer>> demarcateStrings(Map<Character, List<Integer>> markersIndexes) {

        //Screen out escaped quotation marks, assume no BACK_SLASH presented out of JSONString elements.
        List<Integer> validStringBoundaries = new ArrayList<>(markersIndexes.get(QUOTE));
        List<Integer> escpaedQuotes = markersIndexes.get(BACK_SLASH).stream().map(i -> i+1).collect(Collectors.toList());
        for (int i = validStringBoundaries.size()-1; i>=0; i--) {
            for (int j = escpaedQuotes.size()-1; j>=0; j--) {
                Integer x = validStringBoundaries.get(i);
                Integer y = escpaedQuotes.get(j);
                if(x == y) {
                    validStringBoundaries.remove(i);
                    j--;
                } else if (x > y) {
                    continue;
                } else {
                    do {
                        y = escpaedQuotes.get(--j);
                    }while(x < y);
                }
            }
        }

        //Fail fast if valid quotation marks not appear in pairs which means JSON Strings are not enclosed properly
        checkState(validStringBoundaries.size()%2==0, "JSON strings are not wrapped in double quotes!");

        //Notice the Ranges within stringRanges are sorted by nature
        stringRanges = Range._indexesToRanges(validStringBoundaries);
        //Clear any index of special chars if they are contained by any of the stringRanges
        //TODO: not define it as class variable
        specialCharIndexes = markersIndexes.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(QUOTE))
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> new HashSet<>(
                                entry.getValue().stream()
                                        .filter(index -> !inAnyStringRange(index))
                                        .collect(Collectors.toSet())
                        )
                ));

        //Validate there is no BACK_SLASH out of JSON String enclosed by '"' pairs.
        int backSlashCount = specialCharIndexes.get(BACK_SLASH).size();
        checkState(backSlashCount == 0,
                String.format("There are %d '\\' out of any JSON String object.", backSlashCount));

        int leftBraceCount = specialCharIndexes.get(LEFT_BRACE).size();
        int rightBraceCount = specialCharIndexes.get(RIGHT_BRACE).size();
        checkState( leftBraceCount == rightBraceCount,
                String.format("Unbalanced JSON Objects with %d '{', but %d '}'", leftBraceCount, rightBraceCount ));

        int leftBracketCount = specialCharIndexes.get(LEFT_BRACKET).size();
        int rightBracketCount = specialCharIndexes.get(RIGHT_BRACKET).size();
        checkState( leftBracketCount == rightBracketCount,
                String.format("Unbalanced JSON Arrays with %d '[', but %d ']'", leftBracketCount, rightBracketCount ));

        //Keeps clues of how to process ranges as JSONString instances
        stringRanges.forEach(range -> rangedElements.put(range, new Lazy(() -> asJSONString(range))));

        //Return active char lists that make JSON parsing differences
        Map<Character, List<Integer>> activeCharIndexes = specialCharIndexes.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> new ArrayList<>(entry.getValue())
                ));
        return activeCharIndexes;
    }

    /**
     * Get scope of JSON Object wrapped by '{' and '}', JSON Array wrapped by '[' and ']'
     * @param activeCharIndexes Indexes of special chars out of these JSONString instances, not including '"' since all shall be consumed.
     * @return  List of the Ranges of the detected JSONObject and JSONArray for later processing.
     */
    protected List<Range> demarcateObjectAndArrays(Map<Character, List<Integer>> activeCharIndexes) {
        //Get ranges of JSONObjects and keeps clues of how to process ranges as JSONObject instances
        List<Range> objectRanges = Range._indexesToRanges(activeCharIndexes.get(LEFT_BRACE), activeCharIndexes.get(RIGHT_BRACE));
        objectRanges.forEach(range -> rangedElements.put(range, new Lazy(() -> asJSONObject(range))));
//        objectRanges.forEach(objRange -> System.out.println(subString(objRange)));

        //Get ranges of JSONArrays and keeps clues of how to process ranges as JSONArray instances
        List<Range> arrayRanges = Range._indexesToRanges(activeCharIndexes.get(LEFT_BRACKET), activeCharIndexes.get(RIGHT_BRACKET));
        arrayRanges.forEach(range -> rangedElements.put(range, new Lazy(() -> asJSONOArray(range))));

        List<Range> bothRanges = new ArrayList<>(objectRanges);
        bothRanges.addAll(arrayRanges);
        return bothRanges;
    }

    /**
     * Mark the Ranges of all NamedValues by starting with JSONObject and JSONArray.
     * @param objectOrArrayRanges   Ranges of either JSONObject or JSONArray.
     * @param activeCharIndexes     Indexes of special chars out of these JSONString instances, not including '"' since all shall be consumed.
     * @return      Ranges of either JSONObject or JSONArray that have no names.
     */
    protected TreeSet<Range> demarcateNamedValues(List<Range> objectOrArrayRanges, Map<Character, List<Integer>> activeCharIndexes) {
        List<Range> unpairedStringRanges = new ArrayList<>(stringRanges);
        unpairedStringRanges.sort(Comparator.reverseOrder());
        Set<Range> consumedStringRange = new HashSet<>();

        TreeSet<Range> unnamedValueRanges = new TreeSet<>();
        Set<Integer> colonIndexes = new HashSet(activeCharIndexes.get(COLON));
        List<Integer> valueEndIndexes = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, COMMA, COLON).stream().map(c -> activeCharIndexes.get(c))
                .flatMap(list -> list.stream())
                .sorted()
                .collect(Collectors.toList());
        TreeSet<Integer> specialIndexes = new TreeSet<>(activeCharIndexes.values().stream()
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
            if(markerAhead == null) {
                unnamedValueRanges.add(objectArrayRange);
                continue;
            }

            char marker = jsonContext.charAt(markerAhead);
            //When the node is a Child node of a bigger JSONArray
            if(VALUE_START_INDICATORS.contains(marker)){
                unnamedValueRanges.add(objectArrayRange);
                continue;
            }

            //Now it can only be the Value of a NamedValue
            checkState(marker==COLON, "Invlid marker '%s' ahead of: %s", marker, subString(objectArrayRange));

            Range nameRange = unpairedStringRanges.stream()
                    .filter(range -> range.getStartInclusive() < objectArrayRange.getStartInclusive())
                    .findFirst().orElse(null);
            checkState(nameRange != null && specialIndexes.higher(nameRange.getEndInclusive()) == markerAhead,
                    "Invalid NamedValue: %s...", subString(Range.closed(nameRange.getStartInclusive(), objectArrayRange.getStartInclusive()+1)));

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

            if(i != size-1) {
                //As Name part of "Name": Value;
                Range nextStringRange = unpairedStringRanges.get(i+1);
                Integer markerBeforeNext = specialIndexes.lower(nextStringRange.getStartInclusive());
                if(markerBeforeNext == markerIndexAfter) {
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
        if(parsedResult != null){
            return parsedResult;
        }

        Map<Character, List<Integer>> specialCharIndexes = scanSpecialIndexes();

        Map<Character, List<Integer>> activeCharIndexes = demarcateStrings(specialCharIndexes);

        List<Range> objectOrArrayRanges = demarcateObjectAndArrays(activeCharIndexes);

        unnamedValueRanges = demarcateNamedValues(objectOrArrayRanges, activeCharIndexes);

        Range largestRange = Collections.max(unnamedValueRanges, Comparator.comparing(Range::size));

        IJSONValue result = (IJSONValue) rangedElements.get(largestRange).getValue();

        return result;
    }
}