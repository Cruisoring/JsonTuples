package JsonTuples;

import com.google.common.base.Strings;

public interface IJSONable {

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

    static final String SPACE = "  ";
    static final String NEW_LINE = "\n";
    //endregion


    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     * @param indent    The spaces added to each lines.
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

    static String getIndent(int indentFactor){
        return Strings.repeat(SPACE, indentFactor);
    }

}
