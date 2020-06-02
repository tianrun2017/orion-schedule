package com.orion.schedule.common.util;

import java.util.UUID;

public class UUIDUtils {
    public static String next() {
        return UUID.randomUUID().toString();
    }
}
