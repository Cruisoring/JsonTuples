package JsonTuples;

import org.apache.commons.lang3.StringUtils;

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
    //endregion

    /**
     * Get the filling spaces of lines of JSON string.
     * @param indentFactor  level to be indented, 0 means no indent.
     * @return      spaces of the concerned indent level.
     */
    static String getIndent(int indentFactor) {
        return StringUtils.repeat(SPACE, indentFactor);
    }

    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     *
     * @param indent The spaces added to each lines.
     * @return A strictly syntactically correct JSON text.
     */
    String toJSONString(String indent);

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
