package com.apixio.restbase.entity;

/**
 * TokenType:  external tokens are long-lived and can have their TTL extended by
 * using it; internal tokens are short-lived and cannot have their TTL extended.
 */
public enum TokenType {
    EXTERNAL,
    INTERNAL
}
