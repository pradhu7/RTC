package apixio.infraconfig.core;

public class InvalidSftpUserException extends Exception {
    public InvalidSftpUserException(String errorMessage) {
        super(errorMessage);
    }
}
