package JsonTuples;

import io.github.cruisoring.tuple.Tuple2;
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
     * Find the subList of the given index list within a specific range.
     * @param allIndexes    Indexes which would not contain the lower and upper end point of the range.
     * @param range         Range under concern.
     * @return              Sublist of the given sorted index list within a specific range.
     */
    public static List<Integer> getIndexesInRange(List<Integer> allIndexes, IntRange range) {
        checkNotNull(allIndexes);
        checkNotNull(range);

        if(allIndexes.isEmpty()) {
            return new ArrayList<Integer>();
        }

        //Sort the indexes with nature order
        Collections.sort(allIndexes, Comparator.naturalOrder());

        return _getIndexesInRange(allIndexes, range);
    }

    private static List<Integer> _getIndexesInRange(List<Integer> allIndexes, IntRange range) {
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


    public static List<IntRange> asRanges(List<Integer> indexes) {
        checkNotNull(indexes);
        checkState(indexes.size() % 2 == 0);

        Collections.sort(indexes);
        return _asRanges(indexes);
    }

    private static List<IntRange> _asRanges(List<Integer> indexes) {
        int size = indexes.size();

        List<IntRange> ranges = new ArrayList<>();
        for (int i = 0; i < size; i+=2) {
            Integer startIndex = indexes.get(i);
            Integer endIndex = indexes.get(i+1);
            IntRange range = IntRange.closed(startIndex, endIndex);
            ranges.add(range);
        }

        return ranges;
    }

    public static List<IntRange> asRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        checkNotNull(startIndexes);
        checkNotNull(endIndexes);

        int size = startIndexes.size();
        checkState(size == endIndexes.size());

        Collections.sort(startIndexes);
        Collections.sort(endIndexes);
        return _asRanges(startIndexes, endIndexes);
    }

    private static List<IntRange> _asRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        int size = startIndexes.size();
        if(size == 0) {
            return new ArrayList<>();
        } else if (size == 1) {
            return Arrays.asList(IntRange.closed(startIndexes.get(0), endIndexes.get(0)));
        }

        checkState(startIndexes.get(0) < endIndexes.get(0),
                String.format("First startIndex of %d is greater or equal to first endIndex of %d.",
                        startIndexes.get(0), endIndexes.get(0)));

        List<IntRange> ranges = new ArrayList<>();
        List<Integer> availableStarts = new ArrayList<>(startIndexes);

        Integer end, start;
        boolean getMatched;
        for (int j = 0; j < size; j++) {
            end = endIndexes.get(j);
            getMatched = false;
            for (int i = availableStarts.size()-1; i>=0; i--) {
                start = availableStarts.get(i);
                if(start < end) {
                    ranges.add(IntRange.closed(start, end));
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

    private static List<IntRange> getPairsWithValueRanges(Set<IntRange> nameRangeSet, Collection<IntRange> valueRanges, Set<Integer> indicatorIndexes) {
        List<IntRange> nvpRanges = new ArrayList<>();
        for (IntRange valueRange : valueRanges) {
            IntRange nameRange = nameRangeSet.stream()
                    .filter(scope -> scope.getEndInclusive() < valueRange.getStartInclusive())
                    .sorted(comparing(IntRange::getEndInclusive).reversed())
                    .findFirst().orElse(null);
            if(nameRange != null) {
                IntRange gapRange = valueRange.gapWith(nameRange);
                List<Integer> colonsWithin = indicatorIndexes.stream().filter(i -> gapRange.contains(i)).collect(Collectors.toList());
                checkState(colonsWithin.size() == 1,
                        String.format("Failed to get one single indictor between '%s' and '%s'", nameRange, valueRange));
                IntRange nameValueRange = nameRange.intersection(valueRange);
                nameRangeSet.remove(nameRange);
                indicatorIndexes.remove(colonsWithin.get(0));
                nvpRanges.add(nameValueRange);
            }
        }
        return nvpRanges;
    }

    private static List<IntRange> _getNamedValueRanges(Set<IntRange> nameRangeSet, Set<Integer> indicatorIndexes, List<Integer> sortedEnderIndexes) {

        List<IntRange> nvpRanges = new ArrayList<>();
        for (Integer joinerIndex : indicatorIndexes) {
            IntRange nameRange = nameRangeSet.stream()
                    .filter(r -> r.getEndInclusive() < joinerIndex)
                    .sorted(comparing(IntRange::getEndInclusive).reversed())
                    .findFirst().orElse(null);

            if(nameRange == null)
                checkNotNull(nameRange, "Failed to locate the name range right before COLON at " + joinerIndex);
            Integer endIndex = sortedEnderIndexes.stream()
                    .filter(i -> i > joinerIndex)
                    .sorted()
                    .findFirst().orElse(null);
            checkNotNull(endIndex, "Failed to find the end of value after COLON at " + joinerIndex);

            IntRange range = IntRange.closedOpen(nameRange.getStartInclusive(), endIndex);
            nvpRanges.add(range);
        }
        return nvpRanges;
    }

    //Keep the indexes of all characters with special meaning in JSON
    final Map<Character, List<Integer>> markersIndexes = new HashMap<Character, List<Integer>>();

    private final String jsonText;
    private final char[] jsonContext;
    private final int length;

    private IJSONValue parsedResult = null;

    private Map<IntRange, IJSONValue> rangedValues = new HashMap<>();

    private List<IntRange> stringRanges = null;

    private Map<Character, Set<Integer>>  solidMarkers;

    private Map<Class<? extends IJSONable>, List<IntRange>> pairedScopes = new HashMap<>();

    private Map<IntRange, IJSONValue> parsedRanges;

    public Parser(String jsonText) {
        checkState(StringUtils.isNoneBlank(jsonText));

        this.jsonText = jsonText;
        this.jsonContext = jsonText.toCharArray();
        length = jsonContext.length;

        for (Character marker : MARKERS) {
            markersIndexes.put(marker, new ArrayList<>());
        }
    }

    public String subString(IntRange range) {
        checkNotNull(range);

        int end = range.getEndExclusive();
        char[] chars = Arrays.copyOfRange(jsonContext, range.getStartInclusive(), end>length?length:end);
        return new String(chars);
    }

    private void scan() {
        if(parsedResult != null)
            return;

        //Get all indexes of all concerned markers
        for (int i = 0; i < length; i++) {
            char current = jsonContext[i];
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

        stringRanges = new ArrayList<>();

        for (int i = 0; i < validStringBoundaries.size(); i+=2) {
            stringRanges.add(IntRange.closed(validStringBoundaries.get(i), validStringBoundaries.get(i+1)));
        }
    }

    private boolean inAnyStringRange(Integer index) {
        checkNotNull(index);
        checkNotNull(stringRanges);

        for (IntRange range :
                stringRanges) {
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

        List<IntRange> stringRanges = _asRanges(markersIndexes.get(QUOTE));
        pairedScopes.put(JSONString.class, stringRanges);
        pairedScopes.put(JSONObject.class, _asRanges(markersIndexes.get(LEFT_BRACE), markersIndexes.get(RIGHT_BRACE)));
        pairedScopes.put(JSONArray.class, _asRanges(markersIndexes.get(LEFT_BRACKET), markersIndexes.get(RIGHT_BRACKET)));

        keepNameValueRanges(solidMarkers.get(COLON));
    }

    private void keepNameValueRanges(Set<Integer> colonIndexes) {
        Set<IntRange> stringRangeSet = new HashSet<IntRange>(pairedScopes.get(JSONString.class));

        List<IntRange> namedObjectRanges = getPairsWithValueRanges(stringRangeSet,
                pairedScopes.get(JSONObject.class), colonIndexes);
        namedObjectRanges.stream().forEach(range -> System.out.println(subString(range)));

        List<IntRange> namedArrayRanges = getPairsWithValueRanges(stringRangeSet,
                pairedScopes.get(JSONArray.class), colonIndexes);
        namedArrayRanges.stream().forEach(range -> System.out.println(subString(range)));

        List<Integer> valueEnderIndexes = new ArrayList<>(solidMarkers.get(COMMA));
        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACE));
        valueEnderIndexes.addAll(markersIndexes.get(RIGHT_BRACKET));

        List<IntRange> nameValueRanges = _getNamedValueRanges(stringRangeSet, colonIndexes, valueEnderIndexes);
        nameValueRanges.stream().forEach(range -> System.out.println(subString(range)));

        List<IntRange> allNameValueRanges = new ArrayList<>(namedObjectRanges);
        allNameValueRanges.addAll(namedArrayRanges);
        allNameValueRanges.addAll(namedArrayRanges);
        Collections.sort(allNameValueRanges);

        pairedScopes.put(NamedValue.class, allNameValueRanges);
    }

    private void validateScopes() {
        if(parsedResult != null)
            return;

        List<List<IntRange>> categoredScopes = new ArrayList(pairedScopes.values());
        int size = categoredScopes.size();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i+1; j < size; j++) {
                List<Tuple2<IntRange, IntRange>> overlapped = IntRange.getOverlappedRangePairs(categoredScopes.get(i), categoredScopes.get(j));

                if(!overlapped.isEmpty()) {
                    for (Tuple2<IntRange, IntRange> tuple : overlapped) {
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

    private String parseAsString(final IntRange range) {
        checkState(QUOTE==jsonContext[range.getStartInclusive()] && QUOTE==jsonContext[range.getEndInclusive()]);

        char[] array = Arrays.copyOfRange(jsonContext, range.getStartInclusive()+1, range.getEndInclusive());
        return StringEscapeUtils.unescapeJson(new String(array));
    }

    private void parseNameValues() {
        if(parsedResult != null)
            return;

        parsedRanges = new HashMap<>();
        for (IntRange stringRange :
                pairedScopes.get(JSONString.class)) {
            parsedRanges.put(stringRange, JSONValue.parse(jsonText, stringRange));
        }
    }

    public IJSONValue parse() {
        scan();

        getScopes();

        validateScopes();

        return null;
    }
}
