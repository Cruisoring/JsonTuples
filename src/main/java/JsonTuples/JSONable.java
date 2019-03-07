package JsonTuples;

import com.google.common.base.Strings;

public interface JSONable {

    static final String Indent = "  ";
    static final String NewLine = "\r\n";


    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     * @param indentFactor
     *          The number of spaces to add to each level of indentation.
     * @return A strictly syntactically correct JSON text.
     */
    String toJSONString(int indentFactor);

    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     *
     * @return A strictly syntactically correct JSON text.
     */
    default String toJSONString() {
        return toJSONString(0);
    }

    static String getIndent(int indentFactor){
        return Strings.repeat(Indent, indentFactor);
    }

}
