package JsonTuples;

import java.util.Comparator;

public interface IJSONValue extends IJSONable {

    Object getObject();

    int getLength();

    IJSONValue deltaWith(IJSONValue other);

    IJSONValue deltaWith(Comparator<String> comparator, IJSONValue other);

    default boolean isEmpty(){
        return getLength()==0;
    }
}
