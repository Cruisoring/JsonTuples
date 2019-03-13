package JsonTuples;

import io.github.cruisoring.Lazy;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Comparator.comparing;

/**
 * Utility to parse JSON text based on information disclosed on <a href="http://www.json.org/">json.org</a>
 */
public final class Parser {
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
    final static List<Character> VALUE_END_INDICATORS = Arrays.asList(RIGHT_BRACE, RIGHT_BRACKET, COMMA);
    final static Character MIN_MARKER = Collections.min(MARKERS);
    final static Character MAX_MARKER = Collections.max(MARKERS);

    /**
     * Get subString of the concerned JSON text with its Range.
     * @param jsonText  All JSON text to be parsed.
     * @param range     Range of the subString within the jsonText.
     * @return          SubString specified by the given Range.
     */
    public static String subString(String jsonText, Range range) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkState(Range.isValidOfLength(range, jsonText.length()));

        return jsonText.substring(range.getStartInclusive(), range.getEndExclusive());
    }

    /**
     * Extract content of a JSON String object.
     * @param jsonText      All JSON Text to be parsed.
     * @param stringRange   Range of the JSON String object including leading and ending quotation marks '"".
     * @return              Unescaped content of the JSON String object returned as a String.
     */
    public static String getStringContent(String jsonText, Range stringRange) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkNotNull(stringRange);
        checkState(QUOTE== jsonText.charAt(stringRange.getStartInclusive()) && QUOTE== jsonText.charAt(stringRange.getEndInclusive()));

        String enclosedText = jsonText.substring(stringRange.getStartInclusive()+1, stringRange.getEndInclusive());
        return StringEscapeUtils.unescapeJson(enclosedText);
    }

    /**
     * Parse the remaining part of NamedValue other than Name to an IJSONValue instance.
     * @param jsonText      All JSON Text to be parsed.
     * @param valueRange    Range of the value portion of the NameValuePair, shall be led by COLON ':' with optional spaces.
     * @return      Either a simple JSON value (true, false, null, number, string) or compound JSON Object or Array.
     */
    public static IJSONValue parseValuePortion(String jsonText, Range valueRange) {
        checkState(StringUtils.isNotBlank(jsonText));
        checkNotNull(valueRange);

        String colonAndValue = subString(jsonText, valueRange).trim();
        checkState(colonAndValue.charAt(0)==COLON, "The value of the NameValuePair must be led by a COLON ':'.");

        String valueString = colonAndValue.substring(1).trim();

        return parseValue(valueString);
    }

    public static IJSONValue parseValue(String valueString) {
        final String trimmed = valueString.trim();
        switch (trimmed) {
            case JSON_TRUE:
                return JSONValue.True;
            case JSON_FALSE:
                return JSONValue.False;
            case JSON_NULL:
                return JSONValue.Null;
            default:
                //Switch based on the first character, leave the corresponding methods to validate and parse
                switch(trimmed.charAt(0)) {
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

    /**
     * Converting the indexes of wrapping characters in pairs as unmodifiable Range list.
     * @param indexes   List of the indexes that must be even.
     * @return      List of the ranges with even indexes as startInclusive, and odd indexes as endInclusive.
     */
    public static List<Range> pairsAsRanges(List<Integer> indexes) {
        checkNotNull(indexes);
        checkState(indexes.size() % 2 == 0);

        Collections.sort(indexes);
        return _asRanges(indexes);
    }

    private static List<Range> _asRanges(List<Integer> indexes) {
        int size = indexes.size();

        List<Range> ranges = new ArrayList<>();
        for (int i = 0; i < size; i+=2) {
            Integer startIndex = indexes.get(i);
            Integer endIndex = indexes.get(i+1);
            Range range = Range.closed(startIndex, endIndex);
            ranges.add(range);
        }

        return Collections.unmodifiableList(ranges);
    }

    /**
     * Converting the indexes of starts and indexes of ends in pairs as unmodifiable Range list.
     * @param startIndexes  Index list of the starting characters.
     * @param endIndexes    Index list of the ending characters.
     * @return      Unmodifiable list of the ranges with one of the index of starting character, and one of the index of the ending character.
     */
    public static List<Range> pairsAsRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        checkNotNull(startIndexes);
        checkNotNull(endIndexes);

        int size = startIndexes.size();
        checkState(size == endIndexes.size());

        Collections.sort(startIndexes);
        Collections.sort(endIndexes);
        return _asRanges(startIndexes, endIndexes);
    }

    private static List<Range> _asRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        int size = startIndexes.size();
        if(size == 0) {
            return new ArrayList<>();
        } else if (size == 1) {
            return Arrays.asList(Range.closed(startIndexes.get(0), endIndexes.get(0)));
        }

        checkState(startIndexes.get(0) < endIndexes.get(0),
                String.format("First startIndex of %d is greater or equal to first endIndex of %d.",
                        startIndexes.get(0), endIndexes.get(0)));

        List<Range> ranges = new ArrayList<>();
        List<Integer> availableStarts = new ArrayList<>(startIndexes);

        Integer end, start;
        boolean getMatched;
        for (int j = 0; j < size; j++) {
            end = endIndexes.get(j);
            getMatched = false;
            for (int i = availableStarts.size()-1; i>=0; i--) {
                start = availableStarts.get(i);
                if(start < end) {
                    Range newRange = Range.closed(start, end);
                    ranges.add(newRange);
                    availableStarts.remove(start);
                    getMatched = true;
                    break;
                }
            }

            checkState(getMatched,
                    String.format("Not start index matching end index of %d!", end));
        }

        return Collections.unmodifiableList(ranges);
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

//    private List<Range> namedObjectRanges = null;
//
//    private List<Range> namedArrayRanges = null;
//
//    private Map<Class<? extends IJSONable>, List<Range>> pairedScopes = new HashMap<>();

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

//        for (Character marker : MARKERS) {
//            specialCharIndexes.put(marker, new ArrayList<>());
//        }
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
        List<Range> namedValueChildren = range.getChildrens(getSortedRanges());
        System.out.println("???????" + subString(range) + ": as JSONObject");
        namedValueChildren.forEach(r -> System.out.println(subString(r)));

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
        List<Range> elementRanges = range.getChildrens(unnamedValueRanges);
        IJSONValue[] values = elementRanges.stream()
                .map(r -> parseValue(subString(jsonContext, r))).toArray(i -> new IJSONValue[i]);
        JSONArray array = new JSONArray(values);
        return array;
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
                return JSONNumber.parseNumber(valueString);
        }
    }

    protected NamedValue asNamedValue(Range nameRange, Range valueRange) {
        String name = subString(nameRange.getInside());
        IJSONValue value = asValue(valueRange);
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
        List<Integer> backSlashIndexes = markersIndexes.get(BACK_SLASH);
        for (Integer backSlashIndex : backSlashIndexes) {
            Integer toBeEscaped = backSlashIndex+1;
            if(validStringBoundaries.contains(toBeEscaped)){
                validStringBoundaries.remove(toBeEscaped);
            }
        }

        //Fail fast if valid quotation marks not appear in pairs which means JSON Strings are not enclosed properly
        checkState(validStringBoundaries.size()%2==0, "JSON strings are not wrapped in double quotes!");

        //Notice the Ranges within stringRanges are sorted by nature
        stringRanges = _asRanges(validStringBoundaries);
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
        List<Range> objectRanges = _asRanges(activeCharIndexes.get(LEFT_BRACE), activeCharIndexes.get(RIGHT_BRACE));
        objectRanges.forEach(range -> rangedElements.put(range, new Lazy(() -> asJSONObject(range))));
        objectRanges.forEach(objRange -> System.out.println(subString(objRange)));

        //Get ranges of JSONArrays and keeps clues of how to process ranges as JSONArray instances
        List<Range> arrayRanges = _asRanges(activeCharIndexes.get(LEFT_BRACKET), activeCharIndexes.get(RIGHT_BRACKET));
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
        List<Integer> specialIndexes = activeCharIndexes.values().stream()
                .flatMap(list -> list.stream())
                .sorted()
                .collect(Collectors.toList());

        //Ensure the pairing happens from the big ones to small ones
        objectOrArrayRanges.sort(Comparator.comparing(Range::size).reversed());
        for (Range objectArrayRange : objectOrArrayRanges) {
            Range lastStringBefore = unpairedStringRanges.stream()
                    .filter(range -> range.getStartInclusive() < objectArrayRange.getStartInclusive())
                    .findFirst().orElse(null);
            if(lastStringBefore == null || consumedStringRange.contains(lastStringBefore)) {
                //There is no JSONString ahead of the JSONObject or JSONArray, so it can be top level element
                unnamedValueRanges.add(objectArrayRange);
                continue;
            }

            checkState(lastStringBefore.getEndInclusive() < objectArrayRange.getStartInclusive(), "Name String is overlapped with Object/Array?");

            Range gap = objectArrayRange.gapWith(lastStringBefore);
            checkState(gap.size() > 0);

            List<Integer> specialChars = specialIndexes.stream().filter(i -> gap.contains(i)).collect(Collectors.toList());
            //There must be at least one special char to make JSON format compliant
            if(colonIndexes.contains(specialChars.get(0))) {
                checkState(specialChars.size() == 1,
                        String.format("Unexpected NameValue connector: '%s' between '%s' and '%s...'",
                            subString(gap), subString(lastStringBefore),
                            objectArrayRange.size()>30 ? subString(objectArrayRange).substring(0, 30) : subString(objectArrayRange)));

                consumedStringRange.add(lastStringBefore);
                Range nameValueRange = lastStringBefore.intersection(objectArrayRange);
                System.out.println(subString(nameValueRange));
                rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(lastStringBefore, objectArrayRange)));
            } else {
                //This JSONArray or JSONObject shall be a value of a JSONArray
                unnamedValueRanges.add(objectArrayRange);
            }
        }

        unpairedStringRanges.removeAll(consumedStringRange);
        unpairedStringRanges.sort(Comparator.naturalOrder());

        int size = unpairedStringRanges.size();
        for (int i = 0; i < size; i++) {
            Range nameRange = unpairedStringRanges.get(i);
            if(consumedStringRange.contains(nameRange)) {
                continue;
            }

            Range nextStringRange = i == size-1 ? null : unpairedStringRanges.get(i+1);
            if(nextStringRange != null) {
                //Assume the JSONString is the value portion
                Range gap = nextStringRange.gapWith(nameRange);
                List<Integer> specialChars = specialIndexes.stream().filter(index -> gap.contains(index)).collect(Collectors.toList());
                if(specialChars.size()==1 && colonIndexes.contains(specialChars.get(0))) {
//                    //Remove the consume COLON by this NamedValue
//                    colonIndexes.remove(specialChars.get(0));

                    consumedStringRange.add(nextStringRange);
                    Range nameValueRange = nextStringRange.intersection(nameRange);
                    rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(nameRange, nextStringRange)));
                    System.out.println(subString(nameValueRange));
                    i++;    //bypass the next JSONString that is the identified value named by current JSONString instance
                    continue;
                }
            }

            Integer nextEnd = valueEndIndexes.stream().filter(index -> index > nameRange.getEndInclusive()).findFirst().orElse(null);
            checkNotNull(nextEnd);

            if(!colonIndexes.contains(nextEnd)) {
                //Current JSONString shall be a value of a JSONArray
                rangedElements.put(nameRange, new Lazy<>(() -> asValue(nameRange)));
                System.out.println(subString(nameRange));
                continue;
            }

            Range gap = nameRange.gapWith(Range.closed(nextEnd, nextEnd));
            List<Integer> specialChars = specialIndexes.stream().filter(index -> gap.contains(index)).collect(Collectors.toList());
            checkState(specialChars.isEmpty());

            Integer nextSpecialIndex = specialIndexes.stream().filter(index -> index > nextEnd).findFirst().orElse(null);
            checkState(VALUE_END_INDICATORS.contains(jsonContext.charAt(nextSpecialIndex)));

            Range nameValueRange = Range.closedOpen(nameRange.getStartInclusive(), nextSpecialIndex);
            Range valueRange = Range.open(nextEnd, nextSpecialIndex);
            System.out.println(subString(nameValueRange));
            rangedElements.put(nameValueRange, new Lazy<>(() -> asNamedValue(nameRange, valueRange)));
        }

        //The root JSONValue shall be included as the one with largest size
        return unnamedValueRanges;
    }

//    private void denote(Map<Character, List<Integer>> activeCharIndexes) {
//
//        denote(specialCharIndexes.get(COLON));
//
//
//        Set<Range> stringRangeSet = new HashSet<Range>(pairedScopes.get(JSONString.class));
//
//        namedObjectRanges = getPairsWithValueRanges(stringRangeSet,
//                pairedScopes.get(JSONObject.class), colonIndexes);
//        System.out.println("++++++++namedObjectRanges:");
//        namedObjectRanges.stream().forEach(range -> System.out.println(subString(range)));
//
//        namedArrayRanges = getPairsWithValueRanges(stringRangeSet,
//                pairedScopes.get(JSONArray.class), colonIndexes);
//        System.out.println("++++++++namedArrayRanges:");
//        namedArrayRanges.stream().forEach(range -> System.out.println(subString(range)));
//
//        List<Integer> valueEnderIndexes = new ArrayList<>(specialCharIndexes.get(COMMA));
//        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACE));
//        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACKET));
//
//        List<Range> nameValueRanges = _getNamedValueRanges(stringRangeSet, colonIndexes, valueEnderIndexes);
//        System.out.println("++++++++nameValueRanges:");
//        nameValueRanges.stream().forEach(range -> System.out.println(subString(range)));
//
//        pairedScopes.put(NamedValue.class, nameValueRanges);
//    }

//    private void validateScopes() {
//        if(parsedResult != null)
//            return;
//
//        List<List<Range>> categoredScopes = new ArrayList(pairedScopes.values());
//        int size = categoredScopes.size();
//        for (int i = 0; i < size - 1; i++) {
//            for (int j = i+1; j < size; j++) {
//                List<Tuple2<Range, Range>> overlapped = Range.getOverlappedRangePairs(categoredScopes.get(i), categoredScopes.get(j));
//
//                if(!overlapped.isEmpty()) {
//                    for (Tuple2<Range, Range> tuple : overlapped) {
//                        System.out.println(String.format("--------------%s overlaps %s", tuple.getFirst(), tuple.getSecond()));
//                        String text1 = subString(tuple.getFirst());
//                        String text2 = subString(tuple.getSecond());
//                        System.out.println(String.format("'%s' is overlapped with '%s'", text1, text2));
//                    }
//                }
//                checkState(overlapped.isEmpty());
//            }
//        }
//    }

//    private void parseNameValues() {
//        if(parsedResult != null)
//            return;
//
//        //Parse all Ranges of strings, either as names or JSONStrings.
//        List<Range> stringRanges = pairedScopes.get(JSONString.class);
//        for (Range stringRange : stringRanges) {
//            String content = getStringContent(jsonContext, stringRange);
//            rangedValues.put(stringRange, new JSONString(content));
//        }
//
//        //Then parse all simple Name Value pairs where Values are not JSONObjects or JSONArrays
//        List<Range> nvpRanges = pairedScopes.get(NamedValue.class);
//        for (Range nvpRange : nvpRanges) {
////            String nvpString = subString(nvpRange);
//            List<Range> stringRangesContained = stringRanges.stream().filter(r -> nvpRange.contains(r)).collect(Collectors.toList());
//            int count = stringRangesContained.size();
//            checkState(count != 0, "Failed to find the name of the NameValuePair.");
//            checkState(count <= 2, "There shall never be 3rd String in a NameValuePair.");
//
//            Range nameRange = stringRangesContained.get(0);
//            JSONString nameJson = (JSONString)(rangedValues.get(nameRange));
//            String name = nameJson.getFirst();
//            IJSONValue value = count == 2 ?
//                    (JSONString)rangedValues.get(stringRangesContained.get(1))
//                    : parseValuePortion(jsonContext, Range.closedOpen(nameRange.getEndExclusive(), nvpRange.getEndExclusive()));
//            NamedValue nvp = new NamedValue(name, value);
//            rangedValues.put(nvpRange, nvp);
//        }
//    }
//
//    private void parseObjectOrArrays() {
//        List<Range> arrayRanges = pairedScopes.get(JSONArray.class);
//        List<Range> objectRanges = pairedScopes.get(JSONObject.class);
//        List<Range> objectOrArrayRanges = new ArrayList<Range>(arrayRanges);
//        objectOrArrayRanges.addAll(objectRanges);
//        objectOrArrayRanges.sort(Comparator.comparing(Range::size));
//        Set<Integer> commaIndexes = specialCharIndexes.get(COMMA);
//
//        for (Range range : objectOrArrayRanges) {
//            if(arrayRanges.contains(range)) {
//                List<Integer> commasInRange = commaIndexes.stream().filter(i -> range.contains(i)).collect(Collectors.toList());
//                commasInRange.add(0, range.getStartInclusive());
//                commasInRange.add(range.getEndInclusive());
//                List<Range> elementRanges = IntStream.range(1, commasInRange.size())
//                        .mapToObj(i -> Range.open(commasInRange.get(i-1), commasInRange.get(i)))
//                        .collect(Collectors.toList());
//                IJSONValue[] values = elementRanges.stream()
//                        .map(r -> parseValue(subString(jsonContext, r))).toArray(i -> new IJSONValue[i]);
//                JSONArray array = new JSONArray(values);
//
//                Range namedArrayRange = namedArrayRanges.stream().filter(r -> r.contains(range)).findFirst().orElse(null);
//                Range nameRange = stringRanges.stream().filter(r -> namedArrayRange.contains(r)).findFirst().orElse(null);
//                NamedValue nvp = new NamedValue(rangedValues.get(nameRange).toString(), array);
//                rangedValues.put(namedArrayRange, nvp);
//            } else {
//                List<NamedValue> namedValues = rangedValues.entrySet().stream()
//                        .filter(entry -> range.contains(entry.getKey()) && (entry.getValue() instanceof NamedValue))
//                        .map(entry -> (NamedValue)(entry.getValue())).collect(Collectors.toList());
//
//            }
//        }
//    }

    public IJSONValue parse() {
        if(parsedResult != null){
            return parsedResult;
        }

        Map<Character, List<Integer>> specialCharIndexes = scanSpecialIndexes();

        Map<Character, List<Integer>> activeCharIndexes = demarcateStrings(specialCharIndexes);

        List<Range> objectOrArrayRanges = demarcateObjectAndArrays(activeCharIndexes);

        unnamedValueRanges = demarcateNamedValues(objectOrArrayRanges, activeCharIndexes);

        Range largestRange = Collections.max(unnamedValueRanges);

        IJSONValue result = (IJSONValue) rangedElements.get(largestRange).getValue();

        return result;
    }
}
