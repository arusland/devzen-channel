package io.arusland.devzen.error

import java.lang.RuntimeException

open class TgDevzenException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) :
            super(message, cause, enableSuppression, writableStackTrace)
}


class AuthNotRegisteredException(cause: Throwable) : TgDevzenException(cause)
