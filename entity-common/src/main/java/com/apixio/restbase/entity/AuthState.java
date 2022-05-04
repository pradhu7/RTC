package com.apixio.restbase.entity;

/**
 * AuthState defines how authenticated a Token is.  The only tricky state is
 * "partially authenticated".  This state means that some amount of authentication
 * has been done, but more needs to be done in order to get full access to the system.
 *
 * The typical example is that of two-factor authentication:  the first step of
 * providing the email address and password returns a partially authenticated
 * token that then must be used when submitting the code received by the second
 * factor.  A token is required to prove that the first factor was successful but
 * should not be able to be used for anything other than continuing the authentication.
 *
 * Since the partially authenticated state has been used for forgot/reset password
 * during validation for the case of two factor authentication we add a password
 * authenticated state since we need a state after the token has been partially authenticated.
 */
public enum AuthState {
    UNAUTHENTICATED,
    PARTIALLY_AUTHENTICATED,
    PASSWORD_AUTHENTICATED,
    AUTHENTICATED    
}
