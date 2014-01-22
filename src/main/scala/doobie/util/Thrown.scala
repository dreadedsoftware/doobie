package doobie
package util

import argonaut._
import Argonaut._
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.IO._

/** Exteralizable representation of a `Throwable`. */
final case class Thrown(
  className: String, 
  message: String, 
  stack: List[StackTraceElement],
  cause: Option[Thrown])

object Thrown extends ThrownFunctions with ThrownInstances

trait ThrownFunctions {

  def fromThrowable(t: Throwable): Thrown =
    new Thrown(t.getClass.getName, t.getMessage, 
      t.getStackTrace.toList,
      Option(t.getCause).map(fromThrowable))

}

trait ThrownInstances {

  implicit def stackTraceElementCodec: CodecJson[StackTraceElement] =
    (codec4[String, String, String, Int, StackTraceElement](
      new StackTraceElement(_, _, _, _),
      e => (e.getClassName, e.getMethodName, e.getFileName, e.getLineNumber))
      ("class", "method", "file", "line"))

  implicit lazy val thrownEncode: EncodeJson[Thrown] =
    EncodeJson { t =>
      ("class"   := t.className) ->: 
      ("message" := t.message)   ->: 
      ("stack"   := t.stack)     ->:
      ("cause"   := t.cause)     ->:
      jEmptyObject
    }

  implicit lazy val thrownDecode: DecodeJson[Thrown] =
    DecodeJson { t =>
      for {
        n <- (t --\ "class").as[String]
        m <- (t --\ "message").as[String]
        s <- (t --\ "stack").as[List[StackTraceElement]]
        c <- (t --\ "cause").as[Option[Thrown]]
      } yield Thrown(n, m, s, c)
    }

}