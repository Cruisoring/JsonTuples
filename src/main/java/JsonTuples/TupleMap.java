package JsonTuples;


import io.github.cruisoring.Lazy;
import io.github.cruisoring.TypeHelper;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;
import io.github.cruisoring.tuple.TupleSet;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class TupleMap<K extends Comparable<K>> extends TupleSet<Tuple2>
        implements Map<K, Object> {

    public static <K> Tuple2[] toTuples(final Map.Entry<K, Object>[] entries){
        checkNotNull(entries);
        Tuple2<K, Object>[] tuples = Arrays.stream(entries)
                .map(entry -> Tuple.create(entry.getKey(), entry.getValue()))
                .toArray(size -> new Tuple2[size]);
        return tuples;
    }

    public static <K> boolean mapEquals(Map<K, Object> left, Map<K, Object> right){
        if(left == null && right == null){
            return true;
        } else if (left == null || right == null || left.size() != right.size()){
            return false;
        }

        for (K key : left.keySet()) {
            Object leftValue = left.get(key);
            Object rightValue = right.get(key);
            if(leftValue instanceof Map){
                if(!(rightValue instanceof Map) || !mapEquals((Map)leftValue, (Map)rightValue)){
                    return false;
                }
            }else {
                if((rightValue instanceof Map) || TypeHelper.valueEquals(leftValue, rightValue)){
                    return false;
                }
            }
        }
        return true;
    }

    public static <K> Map<K, Object> getDifferences(Map<K, Object> left, Map<K, Object> right){
        checkNotNull(left, right);

        Map<K, Object> differences = new HashMap<>();
        Set<K> keys = new HashSet<>(left.keySet());
        keys.addAll(right.keySet());

        for (K key : keys) {
            Object leftValue = left.containsKey(key) ? left.get(key) : null;
            Object rightValue = right.containsKey(key) ? right.get(key) : null;
            if(leftValue instanceof Map && rightValue instanceof Map){
                Map<K, Object> valueDif = getDifferences((Map)leftValue, (Map)rightValue);
                if(!valueDif.isEmpty()){
                    differences.put(key, valueDif);
                }
            }else {
                if(!TypeHelper.valueEquals(leftValue, rightValue)){
                    differences.put(key, Tuple.create(leftValue, rightValue));
                }
            }
        }
        return differences;
    }

    final Lazy<Map<K, Object>> lazyMap;

    final Lazy<Set<K>> lazyKeySet;
    final Lazy<Collection<Object>> lazyValues;
    final Lazy<Set<Entry<K, Object>>> lazyEntrySet;

    public TupleMap(final Tuple2<K, IJSONValue>[] nodes){
        super(checkNotNull(nodes));
        lazyMap = new Lazy<>(() -> {
            Map<K, Object> m = new LinkedHashMap<>();

            //Cannot use Collectors.toMap which would throw NullPointException when the value is null?
            for (Tuple2<K, IJSONValue> node : asArray()) {
                m.put(node.getFirst(), node.getSecond().getObject());
            }
            return m;
        });

        lazyKeySet = new Lazy<>(() -> Collections.unmodifiableSet(lazyMap.getValue().keySet()));
        lazyEntrySet = new Lazy<>(() -> Collections.unmodifiableSet(lazyMap.getValue().entrySet()));
        lazyValues = new Lazy<>(() -> Collections.unmodifiableCollection(lazyMap.getValue().values()));
    }

    public TupleMap(final Map.Entry<K, Object>[] entries){
        super(toTuples(entries));
        lazyMap = new Lazy<>(()-> {
            Map<K, Object> m = new LinkedHashMap<>();

            //Cannot use Collectors.toMap which would throw NullPointException when the value is null?
            for (Tuple2<K, Object> node : asArray()) {
                m.put(node.getFirst(), node.getSecond());
            }
            return m;
        });

        lazyKeySet = new Lazy<>(() -> Collections.unmodifiableSet(lazyMap.getValue().keySet()));
        lazyEntrySet = new Lazy<>(() -> Collections.unmodifiableSet(lazyMap.getValue().entrySet()));
        lazyValues = new Lazy<>(() -> Collections.unmodifiableCollection(lazyMap.getValue().values()));
    }

    public TupleMap(final Map<K, Object> map){
        this(checkNotNull(map).entrySet().toArray(new Map.Entry[0]));
    }

    @Override
    public int size() {
        return lazyMap.getValue().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return lazyMap.getValue().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return lazyMap.getValue().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return lazyMap.getValue().get(key);
    }

    @Override
    public Object put(K key, Object value) {
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ?> m) {
    }

    @Override
    public void clear() {
    }

    @Override
    public Set<K> keySet() {
        return lazyKeySet.getValue();
    }

    @Override
    public Collection<Object> values() {
        return lazyValues.getValue();
    }

    @Override
    public Set<Entry<K, Object>> entrySet() {
        return lazyEntrySet.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof Map)){
            return false;
        } else if(o == this){
            return true;
        }

        return mapEquals(this, (Map) o);
    }
}

