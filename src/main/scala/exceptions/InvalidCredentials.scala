package exceptions

import caliban.CalibanError.ExecutionError

object InvalidCredentials extends ExecutionError("Invalid username or password")
