package org.cthub.backend.service;

import org.cthub.backend.model.UserToken;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates the two-digit numbers used by the cross-device registration confirmation.
 *
 * <p>The security value here is deliberately modest: a wrong pick is possible by chance, so this is
 * not a secret. What it buys is <em>deliberation</em> - someone who never started a registration has
 * to stop and think before acting, with "this wasn't me" sitting right next to the choices. The real
 * protection comes from a wrong pick burning the link and three wrong picks killing the attempt.
 *
 * <p>Because of that, the thing worth engineering carefully is not entropy but <b>misreadability</b>:
 * a legitimate user must never lose their registration to a glance-alike. {@link #optionsFor} therefore
 * guarantees that all three numbers differ in <em>both</em> digit positions, which rules out
 * transpositions (69/96), shared-digit twins (66/69) and off-by-one neighbours (47/48) in one go.
 */
public final class RegistrationChallenge {

    /** Number of options shown, including the correct one. */
    public static final int OPTION_COUNT = 3;

    /** Wrong picks tolerated on one attempt before it is cancelled outright. */
    public static final int MAX_ATTEMPTS = 3;

    private static final int MIN_NUMBER = 10;
    private static final int MAX_NUMBER = 99;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RegistrationChallenge() {
    }

    /** Picks the correct number for a new attempt. */
    public static short newNumber() {
        return (short) (MIN_NUMBER + SECURE_RANDOM.nextInt(MAX_NUMBER - MIN_NUMBER + 1));
    }

    /**
     * Builds the shuffled option list shown next to the e-mailed link.
     *
     * <p>Derived deterministically from the token hash rather than stored, so reloading the link
     * always shows the same three numbers in the same order without costing extra columns. The hash
     * never leaves the server, so this is not a hint to anyone.
     */
    public static List<Short> optionsFor(UserToken attempt) {
        Short correct = attempt.getChallengeNumber();
        if (correct == null) {
            return List.of();
        }

        // Seeded from the stored hash: stable per attempt, unpredictable from outside.
        Random random = new Random(attempt.getTokenHash().hashCode());

        List<Short> options = new ArrayList<>(OPTION_COUNT);
        options.add(correct);

        int guard = 0;
        while (options.size() < OPTION_COUNT && guard++ < 10_000) {
            short candidate = (short) (MIN_NUMBER + random.nextInt(MAX_NUMBER - MIN_NUMBER + 1));
            if (options.stream().allMatch(existing -> isClearlyDistinct(existing, candidate))) {
                options.add(candidate);
            }
        }

        Collections.shuffle(options, random);
        return List.copyOf(options);
    }

    /**
     * True when two numbers differ in both digit positions, i.e. cannot plausibly be misread for one
     * another at a glance.
     */
    static boolean isClearlyDistinct(short a, short b) {
        return (a / 10) != (b / 10) && (a % 10) != (b % 10);
    }
}
