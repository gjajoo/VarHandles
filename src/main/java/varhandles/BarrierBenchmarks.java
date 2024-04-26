package varhandles;

import org.openjdk.jmh.annotations.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(1)
@State(Scope.Benchmark)
public class BarrierBenchmarks {
    private static AtomicLong s_atomicLong = new AtomicLong();
    private static volatile long s_vLong;
    private static long s_long;
    private static VarHandle S_LONG;
    private static AtomicReference<Object> s_atomicObject = new AtomicReference<>();
    private static volatile Object s_vObject;
    private static Object s_object;
    private static VarHandle S_OBJECT;

    static {
        try {
            S_LONG =
              MethodHandles.privateLookupIn(BarrierBenchmarks.class, MethodHandles.lookup()).findStaticVarHandle(BarrierBenchmarks.class, "s_long", long.class);
            S_OBJECT =
              MethodHandles.privateLookupIn(BarrierBenchmarks.class, MethodHandles.lookup()).findStaticVarHandle(BarrierBenchmarks.class, "s_object", Object.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // irrelevant to the benchmarks, just some silly sets to pretend there is activity out there
    private static Thread S_RUNNER = new Thread(() -> {
        Random rnd = new Random(1);
        while (true) {
            long newValue = rnd.nextLong();
            s_atomicLong.setRelease(newValue);
            S_LONG.setRelease(newValue);
            s_vLong = newValue;
            var str = Long.toString(newValue);
            Object o = new Object() {
                @Override
                public String toString() {return str;}
            };
            S_OBJECT.setRelease(o);
            s_atomicObject.setRelease(o);
            s_vObject = o;
            LockSupport.parkNanos(10_000_000);
        }
    });

    static {
        S_RUNNER.setDaemon(true);
        S_RUNNER.start();
    }

    @Benchmark
    public long longVarHandleAcquire() {
        return (long) S_LONG.getAcquire();
    }

    @Benchmark
    public long longAcquireFence() {
        try {
            return s_long;
        } finally {
            VarHandle.acquireFence();
        }
    }

    @Benchmark
    public long longVolatileRead() {
        return s_vLong;
    }

    @Benchmark
    public long longAtomicRead() {
        return s_atomicLong.get();
    }

    @Benchmark
    public void longVarHandleRelease() {
        S_LONG.setRelease(100);
    }

    @Benchmark
    public void longReleaseFence() {
        VarHandle.releaseFence();
        s_long = 100;
    }

    @Benchmark
    public void longVolatileWrite() {
        s_vLong = 100;
    }

    @Benchmark
    public void longAtomicWrite() {
        s_atomicLong.set(100);
    }

    @Benchmark
    public void longAtomicGetAcquire() {
        s_atomicLong.setRelease(100);
    }

    @Benchmark
    public Object objectVarHandleAcquire() {
        // the varhandle getAcquire issue isn't primitive data types specific
        return S_OBJECT.getAcquire();
    }

    @Benchmark
    public Object objectVolatileRead() {
        return s_vObject;
    }
}
