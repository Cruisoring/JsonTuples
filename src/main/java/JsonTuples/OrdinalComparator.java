package JsonTuples;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkWithoutNull;

/**
 * Comparator with given list of values, when new key is evaluated, it would be assigned a larger integer value.
 *
 * @param <T> Type of the keys to be compared.
 */
public class OrdinalComparator<T> implements Comparator<T> {

    final Map<T, Integer> orders = new HashMap<>();
    final List<T> orderedKeys = new ArrayList<>();

    public OrdinalComparator(Collection<T> options) {
        for (T option : checkWithoutNull(options)) {
            putIfAbsent(option, orders.size() + 1);
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
        Integer order1 = putIfAbsent(o1, orders.size() + 1);
        Integer order2 = putIfAbsent(o2, orders.size() + 1);
        return order1.compareTo(order2);
    }

    @Override
    public String toString() {
        String string = orderedKeys.stream()
                .map(k -> k.toString())
                .collect(Collectors.joining(","));
        return string;
    }
}

