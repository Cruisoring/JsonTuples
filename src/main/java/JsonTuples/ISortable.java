package JsonTuples;

import java.util.Comparator;

import static io.github.cruisoring.Asserts.checkWithoutNull;

/**
 * Represent something that can be sorted with a given {@code Comparator<String>}.
 */
public interface ISortable {

    /**
     * Use the given {@code Comparator<String>} to sort this {@code ISortable} deeply.
     * @param comparator    the {@code Comparator<String>} used to determine the orders of the sorted NamedValues.
     * @return      a {@code ISortable} instance sorted accordingly.
     */
    default ISortable getSorted(Comparator<String> comparator) {
        return this;
    }

    /**
     * Used the given collection of JSON names to sort this {@code ISortable} deeply.
     * @param orderedNames  Array of Strings used to sort this {@code ISortable} deeply.
     * @returnthis  a {@code ISortable} instance sorted accordingly.
     */
    default ISortable getSorted(String... orderedNames) {
        checkWithoutNull(orderedNames);
        Comparator<String> comparator = new OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }
}
