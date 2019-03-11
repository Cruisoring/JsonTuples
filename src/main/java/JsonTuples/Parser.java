package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * Converting the indexes of wrapping characters in pairs as Ranges.
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

        return ranges;
    }

    /**
     * Converting the indexes of starts and indexes of ends in pairs as Ranges.
     * @param startIndexes  Index list of the starting characters.
     * @param endIndexes    Index list of the ending characters.
     * @return      List of the ranges with one of the index of starting character, and one of the index of the ending character.
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
                    ranges.add(Range.closed(start, end));
                    availableStarts.remove(start);
                    getMatched = true;
                    break;
                }
            }

            checkState(getMatched,
                    String.format("Not start index matching end index of %d!", end));
        }

        return ranges;
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
    final Map<Character, List<Integer>> markersIndexes = new HashMap<Character, List<Integer>>();

    private IJSONValue parsedResult = null;

    private List<Range> stringRanges = null;

    private List<Range> namedObjectRanges = null;

    private List<Range> namedArrayRanges = null;

    private Map<Character, Set<Integer>>  solidMarkers;

    private Map<Class<? extends IJSONable>, List<Range>> pairedScopes = new HashMap<>();

    private final Map<Range, IJSONable> rangedValues = new HashMap<>();

    public Parser(String jsonText) {
        checkState(StringUtils.isNoneBlank(jsonText));

        this.jsonContext = jsonText;
        length = jsonContext.length();

        for (Character marker : MARKERS) {
            markersIndexes.put(marker, new ArrayList<>());
        }
    }

    public String subString(Range range) {
        checkNotNull(range);

        int end = range.getEndExclusive();
        return jsonContext.substring(range.getStartInclusive(), end>length?length:end);
    }

    private void scan() {
        if(parsedResult != null)
            return;

        //Get all indexes of all concerned markers
        for (int i = 0; i < length; i++) {
            char current = jsonContext.charAt(i);
            if(current < MIN_MARKER || current > MAX_MARKER || !MARKERS.contains(current)){
                continue;
            }

            markersIndexes.get(current).add(i);
        }

        //Screen out escaped quotation marks
        List<Integer> backSlashIndexes = markersIndexes.get(BACK_SLASH);
        List<Integer> validStringBoundaries = new ArrayList<>(markersIndexes.get(QUOTE));
        for (Integer backSlashIndex : backSlashIndexes) {
            Integer toBeEscaped = backSlashIndex+1;
            if(validStringBoundaries.contains(toBeEscaped)){
                validStringBoundaries.remove(toBeEscaped);
            }
        }

        //Fail fast if valid quotation marks not appear in pairs which means JSON Strings are not enclosed properly
        checkState(validStringBoundaries.size()%2==0, "JSON strings are not wrapped in double quotes!");

        List<Range> list = new ArrayList<>();
        for (int i = 0; i < validStringBoundaries.size(); i+=2) {
            list.add(Range.closed(validStringBoundaries.get(i), validStringBoundaries.get(i+1)));
        }

        stringRanges = Collections.unmodifiableList(list);
    }

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
     * Get scope of JSON Object wrapped by '{' and '}', JSON Array wrapped by '[' and ']', JSON Strings, JSON Name-Value Pairs.
     */
    private void getScopes() {
        if(parsedResult != null)
            return;

        solidMarkers = markersIndexes.entrySet().stream()
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
        int backSlashCount = solidMarkers.get(BACK_SLASH).size();
        checkState(backSlashCount == 0,
                String.format("There are %d '\\' out of any JSON String object.", backSlashCount));

        int leftBraceCount = solidMarkers.get(LEFT_BRACE).size();
        int rightBraceCount = solidMarkers.get(RIGHT_BRACE).size();
        checkState( leftBraceCount == rightBraceCount,
                String.format("Unbalanced JSON Objects with %d '{', but %d '}'", leftBraceCount, rightBraceCount ));

        int leftBracketCount = solidMarkers.get(LEFT_BRACKET).size();
        int rightBracketCount = solidMarkers.get(RIGHT_BRACKET).size();
        checkState( leftBracketCount == rightBracketCount,
                String.format("Unbalanced JSON Arrays with %d '[', but %d ']'", leftBracketCount, rightBracketCount ));

        List<Range> stringRanges = _asRanges(markersIndexes.get(QUOTE));
        pairedScopes.put(JSONString.class, stringRanges);
        pairedScopes.put(JSONObject.class, _asRanges(markersIndexes.get(LEFT_BRACE), markersIndexes.get(RIGHT_BRACE)));
        pairedScopes.put(JSONArray.class, _asRanges(markersIndexes.get(LEFT_BRACKET), markersIndexes.get(RIGHT_BRACKET)));

        keepNameValueRanges(solidMarkers.get(COLON));
    }

    private void keepNameValueRanges(Set<Integer> colonIndexes) {
        Set<Range> stringRangeSet = new HashSet<Range>(pairedScopes.get(JSONString.class));

        namedObjectRanges = getPairsWithValueRanges(stringRangeSet,
                pairedScopes.get(JSONObject.class), colonIndexes);
        System.out.println("++++++++namedObjectRanges:");
        namedObjectRanges.stream().forEach(range -> System.out.println(subString(range)));

        namedArrayRanges = getPairsWithValueRanges(stringRangeSet,
                pairedScopes.get(JSONArray.class), colonIndexes);
        System.out.println("++++++++namedArrayRanges:");
        namedArrayRanges.stream().forEach(range -> System.out.println(subString(range)));

        List<Integer> valueEnderIndexes = new ArrayList<>(solidMarkers.get(COMMA));
        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACE));
        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACKET));

        List<Range> nameValueRanges = _getNamedValueRanges(stringRangeSet, colonIndexes, valueEnderIndexes);
        System.out.println("++++++++nameValueRanges:");
        nameValueRanges.stream().forEach(range -> System.out.println(subString(range)));

        pairedScopes.put(NamedValue.class, nameValueRanges);
    }

    private void validateScopes() {
        if(parsedResult != null)
            return;

        List<List<Range>> categoredScopes = new ArrayList(pairedScopes.values());
        int size = categoredScopes.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i+1; j < size; j++) {
                List<Tuple2<Range, Range>> overlapped = Range.getOverlappedRangePairs(categoredScopes.get(i), categoredScopes.get(j));

                if(!overlapped.isEmpty()) {
                    for (Tuple2<Range, Range> tuple : overlapped) {
                        System.out.println(String.format("--------------%s overlaps %s", tuple.getFirst(), tuple.getSecond()));
                        String text1 = subString(tuple.getFirst());
                        String text2 = subString(tuple.getSecond());
                        System.out.println(String.format("'%s' is overlapped with '%s'", text1, text2));
                    }
                }
                checkState(overlapped.isEmpty());
            }
        }
    }

    private void parseNameValues() {
        if(parsedResult != null)
            return;

        //Parse all Ranges of strings, either as names or JSONStrings.
        List<Range> stringRanges = pairedScopes.get(JSONString.class);
        for (Range stringRange : stringRanges) {
            String content = getStringContent(jsonContext, stringRange);
            rangedValues.put(stringRange, new JSONString(content));
        }

        //Then parse all simple Name Value pairs where Values are not JSONObjects or JSONArrays
        List<Range> nvpRanges = pairedScopes.get(NamedValue.class);
        for (Range nvpRange : nvpRanges) {
//            String nvpString = subString(nvpRange);
            List<Range> stringRangesContained = stringRanges.stream().filter(r -> nvpRange.contains(r)).collect(Collectors.toList());
            int count = stringRangesContained.size();
            checkState(count != 0, "Failed to find the name of the NameValuePair.");
            checkState(count <= 2, "There shall never be 3rd String in a NameValuePair.");

            Range nameRange = stringRangesContained.get(0);
            JSONString nameJson = (JSONString)(rangedValues.get(nameRange));
            String name = nameJson.getFirst();
            IJSONValue value = count == 2 ?
                    (JSONString)rangedValues.get(stringRangesContained.get(1))
                    : parseValuePortion(jsonContext, Range.closedOpen(nameRange.getEndExclusive(), nvpRange.getEndExclusive()));
            NamedValue nvp = new NamedValue(name, value);
            rangedValues.put(nvpRange, nvp);
        }
    }

    private void parseObjectOrArrays() {
        List<Range> arrayRanges = pairedScopes.get(JSONArray.class);
        List<Range> objectRanges = pairedScopes.get(JSONObject.class);
        List<Range> objectOrArrayRanges = new ArrayList<Range>(arrayRanges);
        objectOrArrayRanges.addAll(objectRanges);
        objectOrArrayRanges.sort(Comparator.comparing(Range::size));
        Set<Integer> commaIndexes = solidMarkers.get(COMMA);

        for (Range range : objectOrArrayRanges) {
            if(arrayRanges.contains(range)) {
                List<Integer> commasInRange = commaIndexes.stream().filter(i -> range.contains(i)).collect(Collectors.toList());
                commasInRange.add(0, range.getStartInclusive());
                commasInRange.add(range.getEndInclusive());
                List<Range> elementRanges = IntStream.range(1, commasInRange.size())
                        .mapToObj(i -> Range.open(commasInRange.get(i-1), commasInRange.get(i)))
                        .collect(Collectors.toList());
                IJSONValue[] values = elementRanges.stream()
                        .map(r -> parseValue(subString(jsonContext, r))).toArray(i -> new IJSONValue[i]);
                JSONArray array = new JSONArray(values);

                Range namedArrayRange = namedArrayRanges.stream().filter(r -> r.contains(range)).findFirst().orElse(null);
                Range nameRange = stringRanges.stream().filter(r -> namedArrayRange.contains(r)).findFirst().orElse(null);
                NamedValue nvp = new NamedValue(rangedValues.get(nameRange).toString(), array);
                rangedValues.put(namedArrayRange, nvp);
            } else {
                List<NamedValue> namedValues = rangedValues.entrySet().stream()
                        .filter(entry -> range.contains(entry.getKey()) && (entry.getValue() instanceof NamedValue))
                        .map(entry -> (NamedValue)(entry.getValue())).collect(Collectors.toList());
                
            }
        }
    }

    public IJSONValue parse() {
        if(parsedResult != null){
            return parsedResult;
        }

        scan();

        getScopes();

        validateScopes();

        parseNameValues();

        parseObjectOrArrays();

        return null;
    }
}
