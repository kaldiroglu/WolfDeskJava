package dev.kaldiroglu.wolfdesk.ticketing;

/** What an application service operation reports back: success, or failure with its reason. */
public sealed interface ExecutionResult {

    record Success() implements ExecutionResult {
    }

    record Failure(String reason) implements ExecutionResult {
    }

    static ExecutionResult success() {
        return new Success();
    }

    static ExecutionResult failure(Exception e) {
        return new Failure(e.getMessage());
    }
}
