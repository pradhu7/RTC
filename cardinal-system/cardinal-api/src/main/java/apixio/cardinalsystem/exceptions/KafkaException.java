package apixio.cardinalsystem.exceptions;

public class KafkaException extends RuntimeException {
    public KafkaException(String message, Throwable e) {
        super(message, e);
    }
}
