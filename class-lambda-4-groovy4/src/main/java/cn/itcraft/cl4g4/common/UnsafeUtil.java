package cn.itcraft.cl4g4.common;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by Helly on 2017/05/17.
 */
public final class UnsafeUtil {
    private static final Unsafe UN_SAFE;

    static {
        UN_SAFE = ForbiddenClassInitializer.newExAction(() -> {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        });
    }

    private UnsafeUtil() {
    }

    public static Unsafe getUnSafe() {
        return UN_SAFE;
    }
}
