package JsonTuples;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class LazyFactory<T, R> {
    private static final AtomicInteger counter = new AtomicInteger();

    private Function<T[], R> function;
    private T[] inputs;
    private R value = null;
    private boolean isValueReady = false;
    private boolean isEvaluating = false;

    public LazyFactory(Function<T[], R> function, T... inputs){
        this.function = function;
        this.inputs = inputs;
    }

    public R get() {
        if(isValueReady){
            return value;
        }
        if(!isEvaluating){
            isEvaluating = true;
            R _value = function.apply(inputs);
            if(!this.isValueReady){
                this.isValueReady = true;
                this.value = _value;
                return _value;
            }
        }
        String s = String.valueOf(counter.incrementAndGet()) + ".";
        while (!this.isValueReady) {
            System.out.print(s);
        }
        return value;
    }
}
