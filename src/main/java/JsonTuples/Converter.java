package JsonTuples;

import io.github.cruisoring.TypeHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Converter {
    public static IJSONValue asJSONValue(Object object) {
        if(object == null) {
            return JSONValue.Null;
        } else if (object instanceof IJSONValue){
            return (IJSONValue)object;
        } else if (object.equals(true)){
            return JSONValue.True;
        } else if (object.equals(false)){
            return JSONValue.False;
        } else if(object instanceof String){
            return asJSONString(object);
        } else if (object instanceof Number){
            return asJSONNumber(object);
        } else if (object instanceof Map){
            return asJSONObject(object);
        } else if(object instanceof Collection){
            return asJSONArrayFromCollection(object);
        } else if(object.getClass().isArray()){
            return asJSONArrayFromArray(object);
        }

        return asJSONStringFromOthers(object);
    }

    //region default Converters of Object to IJSONValue
    protected static JSONString asJSONString(Object object){
//        checkNotNull(object);
        return new JSONString((String)object);
    }

    protected static JSONNumber asJSONNumber(Object object){
//        checkNotNull(object);
        return new JSONNumber((Number)object);
    }

    protected static JSONObject asJSONObject(Object object){
//        checkNotNull(object);

        //Let the casting throw exception if the object is not a map of String keys
        Map<String, Object> map = (Map<String, Object>)object;
        NamedValue[] namedValues = map.entrySet().stream()
                .map(entry -> new NamedValue(entry.getKey(), asJSONValue(entry.getValue())))
                .toArray(size -> new NamedValue[size]);
        return new JSONObject(namedValues);
    }

    protected static JSONArray asJSONArrayFromArray(Object array){
        checkState(array != null && array.getClass().isArray());

        Object[] objects = TypeHelper.convert(array, Object[].class);
        IJSONValue[] values = Arrays.stream(objects)
                .map(obj -> asJSONValue(obj))
                .toArray(size -> new IJSONValue[size]);
        return new JSONArray(values);
    }

    protected static JSONArray asJSONArrayFromCollection(Object collection){
        checkState(collection != null && collection instanceof Collection);

        Object[] array = ((Collection)collection).toArray();
        return asJSONArrayFromArray(array);
    }

    protected static JSONString asJSONStringFromOthers(Object object){
        checkNotNull(object);

        String string = object.toString();
        return new JSONString(string);
    }
    //endregion


}
