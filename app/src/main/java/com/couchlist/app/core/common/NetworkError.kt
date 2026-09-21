package com.couchlist.app.core.common

import java.io.IOException
import retrofit2.HttpException

fun networkErrorMessage(throwable: Throwable): String = when (throwable) {
    is IOException -> "Check your connection and try again."
    is HttpException -> "The metadata service is unavailable right now. Try again."
    else -> "Something went wrong. Try again."
}
