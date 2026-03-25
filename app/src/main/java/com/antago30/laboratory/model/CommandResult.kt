package com.antago30.laboratory.model

sealed class CommandResult {
    object Success : CommandResult()
    object WriteFailed : CommandResult()
    data class Error(val message: String) : CommandResult()
}