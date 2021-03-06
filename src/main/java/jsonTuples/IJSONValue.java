package jsonTuples;

import io.github.cruisoring.tuple.WithValues;

import java.util.Comparator;

import static io.github.cruisoring.Asserts.assertAllNotNull;

/**
 *  JSON Value representing simple kinds of JAVA values. A value can be a string in double quotes, or a number,
 *  or true or false or null, or an object or an array. These structures can be nested.
 * @param <T>   Type of the JAVA value that can be String, Number, Boolean ro null.
 * @see <a href="http://www.json.org/">http://www.json.org/</a>
 */
public interface IJSONValue<T extends Object> extends IJSONable, ISortable, WithValues<T> {

    /**
     * Get the immutable JAVA Object represented by this {@code IJSONValue}
     * @return  A Java object represented by this {@code IJSONValue}
     */
    Object getObject();

    /**
     * Get the mutable JAVA Object represented by this {@code IJSONValue} that can be manipulated easily.
     * @return  A Java object represented by this {@code IJSONValue}
     */
    Object asMutableObject();

    /**
     * Compare with another {@code IJSONValue} to find out their differences as an {code IJSONValue} with indexName
     * indicating if the element orders matter and matched indexes shall be included.
     *
     * @param other         the other {@code IJSONValue} to be compared with.
     * @param indexName     indicating how delta shall be composed:
     *                      <tt>null</tt> means elements indexes shall always be considered;
     *                      <tt>String.Empty</tt> means orders matter but no index pair included;
     *                      names including special character '+' like "+index" would always include index pairs of two matched elements if they are different with only their positions;
     *                      otherwise the index pair of two elements would be displayed if they have different values.
     * @return      Representing the differences between this {@code IJSONValue} and other {@code IJSONValue} as a {@code IJSONValue}
     */
    IJSONValue deltaWith(IJSONValue other, String indexName);

    /**
     * Compare with another {@code IJSONValue} to find out their differences as an {code IJSONValue} with default orderMatters.
     * @param other         the other {@code IJSONValue} to be compared with.
     * @return      Representing the differences between this {@code IJSONValue} and other {@code IJSONValue} as a {@code IJSONValue}
     */
    default IJSONValue deltaWith(IJSONValue other){
        return deltaWith(other, JSONArray.defaultIndexName);
    }

    /**
     * Use the given {@code Comparator<String>} to sort this {@code IJSONValue} deeply.
     * @param comparator    the {@code Comparator<String>} used to determine the orders of the sorted NamedValues.
     * @return      a {@code IJSONValue} instance sorted accordingly.
     */
    @Override
    IJSONValue<T> getSorted(Comparator<String> comparator);

    /**
     * Used the given collection of JSON names to sort this {@code IJSONValue} deeply.
     * @param orderedNames  Collection of Strings used to sort this {@code IJSONValue} deeply.
     * @return   a {@code IJSONValue} instance sorted accordingly.
     */
    @Override
    default IJSONValue<T> getSorted(String... orderedNames) {
        assertAllNotNull(orderedNames);
        Comparator<String> comparator = new OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }

    /**
     * Represent if this {@code IJSONValue} contains any elements.
     * @return  <tt>true</tt> if it does has some elements, otherwise <tt>false</tt>
     */
    default boolean isEmpty() {
        return getLength() == 0;
    }
}
