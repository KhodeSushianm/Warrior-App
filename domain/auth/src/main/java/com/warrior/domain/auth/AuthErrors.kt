package com.warrior.domain.auth

/** Thrown when registering a username that already exists on this device. */
class DuplicateUsernameException(username: String) :
    IllegalStateException("username '$username' already exists")

/** Thrown when login credentials do not match any local account. */
class InvalidCredentialsException :
    SecurityException("invalid username or password")
