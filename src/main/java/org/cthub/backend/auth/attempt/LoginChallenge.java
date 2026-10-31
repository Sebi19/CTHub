package org.cthub.backend.auth.attempt;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The number-matching part of a login attempt. Browser A shows the correct two-digit number; the
 * approving device shows it among two decoys and the owner must pick it.
 *
 * <p>The three options are pairwise distinct in <em>both</em> digit positions, so a glance at
 * either digit is enough to tell them apart. They are derived from the attempt id rather than
 * stored, so reloading the approval page shows the same options in the same order - otherwise
 * reloading would be a way to shop for a layout that makes guessing easier.
 */
public final class LoginChallenge {

    public static final int OPTION_COUNT = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LoginChallenge() {
    }

    public static short newNumber() {
        return (short) (10 + RANDOM.nextInt(90));
    }

    public static List<Integer> optionsFor(int correct, String seed) {
        Random random = new Random(seed.hashCode());
        List<Integer> options = new ArrayList<>(OPTION_COUNT);
        options.add(correct);
        while (options.size() < OPTION_COUNT) {
            int candidate = 10 + random.nextInt(90);
            if (options.stream().allMatch(existing -> isClearlyDistinct(existing, candidate))) {
                options.add(candidate);
            }
        }
        Collections.shuffle(options, random);
        return List.copyOf(options);
    }

    static boolean isClearlyDistinct(int a, int b) {
        return a / 10 != b / 10 && a % 10 != b % 10;
    }
}
