package com.ranull.graves.util;

import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;

public final class TestUtilities {
    private static final Random random = new Random();

    @ParametersAreNonnullByDefault
    public static int randomInt() {
        return random.nextInt(Integer.MAX_VALUE);
    }

    @ParametersAreNonnullByDefault
    public static int randomInt(int upperBound) {
        return random.nextInt(upperBound);
    }
}
