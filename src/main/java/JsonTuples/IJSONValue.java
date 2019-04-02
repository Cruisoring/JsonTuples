package JsonTuples;

import io.github.cruisoring.tuple.WithValues;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

public interface IJSONValue extends IJSONable, WithValues {

    Object getObject();

    int getLength();

    IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator);

    default Set<Integer> getSignatures(){
        List<Integer> elementHashCodes = IntStream.range(0, getLength())
                .mapToObj(i -> getValueAt(i).hashCode())
                .collect(Collectors.toList());
        elementHashCodes.add(hashCode());
        return new HashSet<Integer>(elementHashCodes);
    }

    default IJSONValue deltaWith(IJSONValue other){
        return deltaWith(other, null);
    }

    default IJSONValue getSorted(Comparator<String> comparator){
        return this;
    }

    default IJSONValue getSorted(Collection<String> orderedNames){
        checkNotNull(orderedNames);
        Comparator<String> comparator = new JSONObject.OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }

    default boolean isEmpty(){
        return getLength()==0;
    }
}
