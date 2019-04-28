package JsonTuples;

import io.github.cruisoring.tuple.WithValues;

import java.util.Collection;
import java.util.Comparator;

import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
 *  JSON Value representing simple kinds of JAVA values. A value can be a string in double quotes, or a number,
 *  or true or false or null, or an object or an array. These structures can be nested.
 * @param <T>   Type of the JAVA value that can be String, Number, Boolean ro null.
 * @see <a href="http://www.json.org/">http://www.json.org/</a>
 */
public interface IJSONValue<T extends Object> extends IJSONable, ISortable, WithValues<T> {

    /**
     * Get the JAVA Object represented by this {@code IJSONValue}
     * @return
     */
    Object getObject();

    /**
     * Find the differences with another IJSONValue other after sorting both with the given {@code Comparator<String>}
     * @param other         the other {@code IJSONValue} to be compared with.
     * @param comparator    {@code Comparator<String>} used to sort both {@code IJSONValue} instances
     * @return      Representing the differences between this {@code IJSONValue} and other {@code IJSONValue} as a {@code IJSONValue}
     */
    IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator);

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
     * @returnthis  a {@code IJSONValue} instance sorted accordingly.
     */
    @Override
    default IJSONValue<T> getSorted(Collection<String> orderedNames) {
        checkWithoutNull(orderedNames);
        Comparator<String> comparator = new OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }


    /**
     * Find the differences with another IJSONValue other without sorting them first.
     * @param other         the other {@code IJSONValue} to be compared with.
     * @return      Representing the differences between this {@code IJSONValue} and other {@code IJSONValue} as a {@code IJSONValue}
     */
    default IJSONValue deltaWith(IJSONValue other) {
        return deltaWith(other, null);
    }

    /**
     * Represent if this {@code IJSONValue} contains any elements.
     * @return  <tt>true</tt> if it does has some elements, otherwise <tt>false</tt>
     */
    default boolean isEmpty() {
        return getLength() == 0;
    }
}
