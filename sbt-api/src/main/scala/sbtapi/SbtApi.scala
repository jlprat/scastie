package sbtapi

sealed trait Severity
case object Info    extends Severity
case object Warning extends Severity
case object Error   extends Severity

case class Problem(severity: Severity, offset: Option[Int], message: String)

// TODO: stacktrace
// stack: List[StackElement]
// String  getClassName()
// String  getFileName()
// int getLineNumber()
// String  getMethodName()
// TODO: range pos ?
case class RuntimeError(message: String, offset: Option[Int])
