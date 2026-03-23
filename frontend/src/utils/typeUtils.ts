/**
 * Use this in the `default` block of a switch statement to ensure
 * all enum cases are handled at compile time.
 */
export const checkExhaustive = (toCheck: never) => {
    void toCheck;
};