package com.apixio.dao.vault;

/**
 * May 2019:  copied from https://github.com/BetterCloud/vault-java-driver/blob/master/src/main/java/com/bettercloud/vault/rest/RestException.java
 * Note:  copied code is MIT license:  https://github.com/BetterCloud/vault-java-driver#license
 */

public class RestException extends Exception {

    RestException(final String message) {
        super(message);
    }

    RestException(final Throwable t) {
        super(t);
    }

}
