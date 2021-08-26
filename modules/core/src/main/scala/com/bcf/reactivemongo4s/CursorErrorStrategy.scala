package com.bcf.reactivemongo4s

sealed trait CursorErrorStrategy

object CursorErrorStrategy {
  case object Fail extends CursorErrorStrategy
  case object Done extends CursorErrorStrategy
  case object Cont extends CursorErrorStrategy
}
