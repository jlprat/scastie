package com.olegych.scastie
package remote

case class PasteProgress(
    id: Long,
    done: Boolean,
    output: String,
    compilationInfos: List[api.Problem],
    instrumentations: List[api.Instrumentation]
)

case class RunPaste(
  id: Long,
  code: String,
  sbtConfig: String,
  scalaTargetType: api.ScalaTargetType
)

case class RunPasteError(
  id: Long,
  cause: String
)
