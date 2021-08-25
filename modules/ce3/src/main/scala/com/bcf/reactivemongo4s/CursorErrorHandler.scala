package com.bcf.reactivemongo4s

sealed trait CursorErrorHandler

object CursorErrorHandler {
  case object Fail extends CursorErrorHandler
  case object Done extends CursorErrorHandler
  case object Cont extends CursorErrorHandler
}
