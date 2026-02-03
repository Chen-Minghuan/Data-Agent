package edu.zsc.ai.plugin.manager;

import java.util.List;
import java.util.function.Function;

/**
 * Utility for trying each candidate with an operation and returning the first success.
 * Used when multiple candidates (e.g. plugins/providers) can fulfill an operation and
 * the first one that succeeds should be used.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public final class TryFirstSuccess {

    private TryFirstSuccess() {
        // Utility class, prevent instantiation
    }

    /**
     * Result of a successful attempt: the candidate that succeeded and the operation result.
     *
     * @param <T> candidate type
     * @param <R> result type
     * @param candidate the candidate for which the operation succeeded
     * @param result    the result returned by the operation
     */
    public record AttemptResult<T, R>(T candidate, R result) {
    }

    /**
     * Try each candidate with the operation in order; return the first (candidate, result) for which
     * the operation completes without throwing. If all throw, an UnsupportedOperationException is thrown.
     *
     * @param candidates non-null, non-empty list of candidates to try
     * @param operation  the operation to perform for each candidate (e.g. connect, resolve)
     * @param <T>        candidate type
     * @param <R>        result type
     * @return AttemptResult containing the first successful candidate and its result
     * @throws IllegalArgumentException         if candidates is null or empty
     * @throws UnsupportedOperationException   if every attempt throws
     */
    public static <T, R> AttemptResult<T, R> tryFirstSuccess(List<T> candidates, Function<T, R> operation) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidates list cannot be null or empty");
        }
        for (T c : candidates) {
            try {
                return new AttemptResult<>(c, operation.apply(c));
            } catch (Throwable ignored) {
                // try next candidate
            }
        }
        throw new UnsupportedOperationException("No candidate succeeded");
    }
}
