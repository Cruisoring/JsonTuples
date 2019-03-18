package JsonTuples;


import io.github.cruisoring.tuple.Set;
import io.github.cruisoring.tuple.Tuple;
import io.github.cruisoring.tuple.Tuple2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TupleMap<K extends Comparable<K>,V> extends HashMap<K, V> {
    private static final String NEW_LINE = IJSONValue.NEW_LINE;
    private static final String SPACE = IJSONValue.SPACE;

    public Set<Tuple2<K,V>> asTupleSet(){
        Tuple2<K, V>[] sortedTuples = entrySet().stream()
                .map(entry -> Tuple.create(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(tuple -> tuple.getFirst()))
                .toArray(size -> new Tuple2[size]);

        return Set.setOf(sortedTuples);
    }



    @Override
    public boolean equals(Object o) {
        if(o instanceof TupleMap){
            return asTupleSet().equals(((TupleMap) o).asTupleSet());
        }

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return asTupleSet().hashCode();
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        Set<Tuple2<K,V>> tupleSet = asTupleSet();
        final int length = tupleSet.getLength();
        if(length == 0) {
            return "{}";
        }

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            V value = tupleSet.get(i).getSecond();
            if(value == null) {
                break;
            }
            String valueLines = value.toString();
            valueLines = valueLines.replaceAll(NEW_LINE, NEW_LINE+SPACE);
            String line = String.format(indent + "%s\"%s\": %s%s",
                        SPACE, tupleSet.get(i).getFirst(), valueLines, i==length-1?"":",");
            lines.add(line);
        }
        lines.add(0, "{");
        lines.add("}");

        String string = String.join(IJSONValue.NEW_LINE, lines);
        string = string.replaceAll(NEW_LINE, NEW_LINE+indent);
        return string;
    }
}

