package services.errorreporting

import zio.ZIO
import zio.Cause

def showError(err: Throwable) = ZIO.serviceWithZIO[ErrorReporting](_.showError(err))

def showCause[E <: Throwable](cause: Cause[E]) = showError(cause.squashTrace)
