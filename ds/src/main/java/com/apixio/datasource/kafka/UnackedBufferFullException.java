package com.apixio.datasource.kafka;

/**
 * This exception is thrown if either the consumer or the producer is in acknowledging mode and the number of unacknowledged messages
 * exceeds the configured limit
 */
/**
 * @author slydon
 */
public class UnackedBufferFullException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnackedBufferFullException(String message) {
        super(message);
    }

}
