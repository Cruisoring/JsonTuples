package JsonTuples;

/**
 * Something that can be represented as JSON text.
 */
public interface IJSONable {

    //region static variables
    String JSON_NULL = "null";
    String JSON_TRUE = "true";
    String JSON_FALSE = "false";

    //Special keys to mark the value boundary or escaped sequences
    char LEFT_BRACE = '{';
    char RIGHT_BRACE = '}';
    char LEFT_BRACKET = '[';
    char RIGHT_BRACKET = ']';
    char COMMA = ',';
    char COLON = ':';
    char QUOTE = '"';
    char BACK_SLASH = '\\';

    String SPACE = "  ";
    String NEW_LINE = "\n";
    String COMMA_NEWLINE = COMMA + NEW_LINE;
    String COMMA_NEWLINE_SPACE = COMMA + NEW_LINE + SPACE;

    //endregion

    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     *
     * @param indent The spaces added to each lines.
     * @return A strictly syntactically correct JSON text.
     */
    String toJSONString(String indent);

    /**
     * Get the number of {@code JSONValue} contained by this IJSONable by specifying if null shall be counted.
     *
     * @param countNulls <tt>true</tt> to count JSONValue.NULL as 1
     * @return number of {@code JSONValue}.
     */
    int getLeafCount(boolean countNulls);

    /**
     * Get the number of {@code JSONValue} contained by this IJSONable by counting 'null' as 1 or not depending on if JSONValue.MISSING is JSONValue.Null.
     *
     * @return number of {@code JSONValue}: count JSONValue.Null as 1 when JSONValue.MISSing is not JSON.MISSING.
     */
    default int getLeafCount() {
        return this.equals(JSONValue.MISSING) ? 0 : 1;
    }

    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     *
     * @return A strictly syntactically correct JSON text.
     */
    default String toJSONString() {
        return toJSONString("");
    }

}
