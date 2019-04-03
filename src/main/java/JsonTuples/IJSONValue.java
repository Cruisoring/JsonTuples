package JsonTuples;

import io.github.cruisoring.tuple.WithValues;

import java.util.Collection;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;

public interface IJSONValue<T extends Object> extends IJSONable, ISortable, WithValues<T> {

    Object getObject();

    IJSONValue deltaWith(IJSONValue other, Comparator<String> comparator);

    @Override
    default IJSONValue<T> getSorted(Comparator<String> comparator){
        return this;
    }

    @Override
    default IJSONValue<T> getSorted(Collection<String> orderedNames){
        checkNotNull(orderedNames);
        Comparator<String> comparator = new JSONObject.OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }


    default IJSONValue deltaWith(IJSONValue other){
        return deltaWith(other, null);
    }

    default boolean isEmpty(){
        return getLength()==0;
    }
}
