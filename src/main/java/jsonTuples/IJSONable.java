package jsonTuples;

/**
 * Something that can be represented as JSON text.
 */
public interface IJSONable {

    /**
     * The <code>toJSONString</code> method allows a class to produce its own JSON
     * serialization.
     *
     * @param indent The spaces added to each lines.
     * @return A strictly syntactically correct JSON text.
     */
    String toJSONString(String indent);

    /**
     * Get the number of {@code JSONValue} contained by this IJSONable.
     *
     * @return number of {@code JSONValue}.
     */
    int getLeafCount();

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
