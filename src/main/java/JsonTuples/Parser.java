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

    //Keep the indexes of all characters with special meaning in JSON
    final Map<Character, List<Integer>> markersIndexes = new HashMap<Character, List<Integer>>();

    private final char[] jsonContext;
    private final int length;

    private IJSONValue parsedResult = null;

    private Map<Range, IJSONValue> rangedValues = new HashMap<>();

    private List<Range> stringRanges = null;

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

    /**
     * Find the subList of the given index list within a specific range, the given list must be sorted to get better performance.
     * @param allIndexes    Sorted indexes which would not contain the lower and upper end point of the range.
     * @param range         Range under concern.
     * @return              Sublist of the given sorted index list within a specific range.
     */
    private List<Integer> getIndexesInRange(List<Integer> allIndexes, Range<Integer> range) {
        List<Integer> result = new ArrayList<>();

        int count = allIndexes.size();
        Integer lower = range.lowerEndpoint();
        Integer upper = range.upperEndpoint();
        if(count == 0 || lower > allIndexes.get(count-1) || upper < allIndexes.get(0)) {
            return result;
        }

        boolean inRange = false;
        for (int i = 0; i < count; i++) {
            Integer index = allIndexes.get(i);
            if(!inRange && index >= lower) {
                if (index > upper) {
                    return result;
                } else {
                    result.add(index);
                    inRange = true;
                }
            } else if(inRange) {
                if(index < upper) {
                    result.add(index);
                } else {
                    return result;
                }
            }
        }
        return result;
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

    private void getRangeSets() {
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

        int leftBraceCount = solidMarkers.get(LEFT_BRACE).size();
        int rightBraceCount = solidMarkers.get(RIGHT_BRACE).size();
        checkState( leftBraceCount == rightBraceCount,
                String.format("Unbalanced JSON Objects with %d '{', but %d '}'", leftBraceCount, rightBraceCount ));

        int leftBracketCount = solidMarkers.get(LEFT_BRACKET).size();
        int rightBracketCount = solidMarkers.get(RIGHT_BRACKET).size();
        checkState( leftBracketCount == rightBracketCount,
                String.format("Unbalanced JSON Arrays with %d '[', but %d ']'", leftBracketCount, rightBracketCount ));
    }

    public IJSONValue parse() {
        scan();

        getRangeSets();

        return null;
    }
}
