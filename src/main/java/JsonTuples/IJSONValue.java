package JsonTuples;

public interface IJSONValue extends IJSONable {

    Object getObject();

    IJSONValue deltaWith(IJSONValue other);
}
