package com.apixio.util.nassembly;

import com.apixio.datacatalog.DefaultValueWrappers;


public class DefaultValueWrapperUtils {

    public static DefaultValueWrappers.BoolWrapper wrapBoolean(boolean val) {
        return DefaultValueWrappers.BoolWrapper.newBuilder().setValue(val).build();
    }

    public static DefaultValueWrappers.IntWrapper wrapInt(int val) {
        return DefaultValueWrappers.IntWrapper.newBuilder().setValue(val).build();
    }

    public static DefaultValueWrappers.LongWrapper wrapLong(long val) {
        return DefaultValueWrappers.LongWrapper.newBuilder().setValue(val).build();
    }

    public static DefaultValueWrappers.DoubleWrapper wrapDouble(double val) {
        return DefaultValueWrappers.DoubleWrapper.newBuilder().setValue(val).build();
    }
}
