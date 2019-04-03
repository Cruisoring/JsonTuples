package JsonTuples;

import java.util.Collection;
import java.util.Comparator;

import static com.google.common.base.Preconditions.checkNotNull;

public interface ISortable {
    default ISortable getSorted(Comparator<String> comparator){
        return this;
    }

    default ISortable getSorted(Collection<String> orderedNames){
        checkNotNull(orderedNames);
        Comparator<String> comparator = new JSONObject.OrdinalComparator<>(orderedNames);
        return getSorted(comparator);
    }


}
