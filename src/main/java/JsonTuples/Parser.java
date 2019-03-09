package JsonTuples;

import com.google.common.collect.Range;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
    public static List<Integer> getIndexesInRange(List<Integer> allIndexes, Range<Integer> range) {
        checkNotNull(allIndexes);
        checkNotNull(range);

        if(allIndexes.isEmpty()) {
            return new ArrayList<Integer>();
        }

        //Sort the indexes with nature order
        Collections.sort(allIndexes, Comparator.naturalOrder());

        return _getIndexesInRange(allIndexes, range);
    }

    private static List<Integer> _getIndexesInRange(List<Integer> allIndexes, Range<Integer> range) {
        List<Integer> result = new ArrayList<>();

        int count = allIndexes.size();
        Integer lower = range.lowerEndpoint();
        Integer upper = range.upperEndpoint();
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


    public static List<Range<Integer>> asRanges(List<Integer> indexes) {
        checkNotNull(indexes);
        checkState(indexes.size() % 2 == 0);

        Collections.sort(indexes);
        return _asRanges(indexes);
    }

    private static List<Range<Integer>> _asRanges(List<Integer> indexes) {
        int size = indexes.size();

        List<Range<Integer>> ranges = new ArrayList<>();
        for (int i = 0; i < size; i+=2) {
            Integer startIndex = indexes.get(i);
            Integer endIndex = indexes.get(i+1);
            Range range = Range.closed(startIndex, endIndex);
            ranges.add(range);
        }

        return ranges;
    }

    public static List<Range<Integer>> asRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        checkNotNull(startIndexes);
        checkNotNull(endIndexes);

        int size = startIndexes.size();
        checkState(size == endIndexes.size());

        Collections.sort(startIndexes);
        Collections.sort(endIndexes);
        return _asRanges(startIndexes, endIndexes);
    }

    private static List<Range<Integer>> _asRanges(List<Integer> startIndexes, List<Integer> endIndexes) {
        int size = startIndexes.size();
        if(size == 0) {
            return new ArrayList<>();
        } else if (size == 1) {
            return Arrays.asList(Range.closed(startIndexes.get(0), endIndexes.get(0)));
        }

        checkState(startIndexes.get(0) < endIndexes.get(0),
                String.format("First startIndex of %d is greater or equal to first endIndex of %d.",
                        startIndexes.get(0), endIndexes.get(0)));

        List<Range<Integer>> ranges = new ArrayList<>();
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

    //Keep the indexes of all characters with special meaning in JSON
    final Map<Character, List<Integer>> markersIndexes = new HashMap<Character, List<Integer>>();

    private final char[] jsonContext;
    private final int length;

    private IJSONValue parsedResult = null;

    private Map<Range, IJSONValue> rangedValues = new HashMap<>();

    private List<Range> stringRanges = null;

    private Map<Class<? extends IJSONable>, List<Range<Integer>>> pairedScopes = new HashMap<>();

    public Parser(String jsonText) {
        checkState(StringUtils.isNoneBlank(jsonText));

        this.jsonContext = jsonText.toCharArray();
        length = jsonContext.length;

        for (Character marker : MARKERS) {
            markersIndexes.put(marker, new ArrayList<>());
        }
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
            stringRanges.add(Range.closed(validStringBoundaries.get(i), validStringBoundaries.get(i+1)));
        }
    }

    private boolean inAnyStringRange(Integer index) {
        checkNotNull(index);
        checkNotNull(stringRanges);

        for (Range<Integer> range :
                stringRanges) {
            if(range.contains(index))
                return true;
            if(range.lowerEndpoint() > index)
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

        Map<Character, Set<Integer>>  solidMarkers = markersIndexes.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(QUOTE))
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> new HashSet<>(
                                entry.getValue().stream()
                                .filter(index -> !inAnyStringRange(index))
                                .collect(Collectors.toSet())
                        )
                ));

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

        List<Range<Integer>> stringScopes = _asRanges(markersIndexes.get(QUOTE));
        List<Range<Integer>> objectScopes = _asRanges(markersIndexes.get(LEFT_BRACE), markersIndexes.get(RIGHT_BRACE));
        List<Range<Integer>> arrayScopes = _asRanges(markersIndexes.get(LEFT_BRACKET), markersIndexes.get(RIGHT_BRACKET));

        pairedScopes.put(JSONString.class, stringScopes);
        pairedScopes.put(JSONObject.class, objectScopes);
        pairedScopes.put(JSONArray.class, arrayScopes);
    }

    private void validateScopes() {
        if(parsedResult != null)
            return;

        List<Range<Integer>> stringScopes = pairedScopes.get(JSONString.class);
        List<Range<Integer>> objectScopes = pairedScopes.get(JSONObject.class);
        List<Range<Integer>> arrayScopes = pairedScopes.get(JSONArray.class);



    }

    public IJSONValue parse() {
        scan();

        getScopes();

        return null;
    }
}
