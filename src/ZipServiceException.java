/**
 * Thrown by {@link ZipService} when a ZIP operation cannot be completed.
 *
 * Using a checked exception forces callers to handle extraction failures
 * explicitly — which is important because the grading loop must continue
 * processing other students even when one ZIP is corrupt or unreadable.
 */
public class ZipServiceException extends Exception {

    public ZipServiceException(String message) {
        super(message);
    }

    public ZipServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}