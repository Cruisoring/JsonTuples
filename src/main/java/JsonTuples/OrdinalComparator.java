package JsonTuples;

import io.github.cruisoring.utility.SimpleTypedList;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.github.cruisoring.Asserts.checkNoneNulls;

/**
 * Comparator with given list of values, when new key is evaluated, it would be assigned a larger integer value.
 *
 * @param <T> Type of the keys to be compared.
 */
public class OrdinalComparator<T> implements Comparator<T> {

    final Map<T, Integer> orders = new HashMap<>();
    final List<T> orderedKeys = new SimpleTypedList<>();

    /**
     * Construct an {@code OrdinalComparator} with known options in order, which would used to maintain the orders when sorting an Array.
     * @param options   the known options to be kept by this {@code OrdinalComparator}
     */
    public OrdinalComparator(T... options) {
        for (T option : options) {
            putIfAbsent(checkNoneNulls(option), orders.size() + 1);
        }
    }

    /**
     * Record a new key and keep it as the last of known keys.
     * @param key   the new key to be recorded.
     */
    public void putIfAbsent(T key) {
        if(!orders.containsKey(key)) {
            orders.put(key, orders.size()+1);
            orderedKeys.add(key);
        }
    }

    Integer putIfAbsent(T key, Integer order) {
        if (!orders.containsKey(key)) {
            orders.put(key, order);
            orderedKeys.add(key);
            return order;
        } else {
            return orders.get(key);
        }
    }

    @Override
    public int compare(T o1, T o2) {
        //Notice: Arrays.sort() seems to refer first values as o2, thus o2 shall be cached first
        Integer order2 = putIfAbsent(o2, orders.size() + 1);
        Integer order1 = putIfAbsent(o1, orders.size() + 1);
        return order1.compareTo(order2);
    }

    @Override
    public String toString() {
        String string = orderedKeys.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return string;
    }
}

