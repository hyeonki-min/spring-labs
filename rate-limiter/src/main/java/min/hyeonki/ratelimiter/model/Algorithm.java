package min.hyeonki.ratelimiter.model;

public enum Algorithm {
    TOKEN,
    LEAKY,
    LEAKY_WATER,
    FIXED,
    SLIDING_LOG
}