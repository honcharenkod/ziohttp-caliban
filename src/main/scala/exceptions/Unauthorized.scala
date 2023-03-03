package exceptions

import caliban.CalibanError.ExecutionError

object Unauthorized extends ExecutionError("Unauthorized")
