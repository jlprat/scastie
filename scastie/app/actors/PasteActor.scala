package actors

class PasteActor(progressActor: ActorRef) extends Actor {

  private val container = PastesContainer(
    Paths.get(Play.configuration.getString("pastes.data.dir").get)
  )

  private val router = {
    val routees =
      Vector(
        ActorSelectionRoutee(
          context.actorSelection(
            s"akka.tcp://SbtRemote@127.0.0.1:5150/user/SbtActor"
          )
        )
      )
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case add: AddPaste => {
      val id = container.write(add)
      router.route(add.toRunPaste(id), sender())
    }

    case GetPaste(id) => {
      sender ! container.read(id)
    }

    case progress: PasteProgress => {
      progressActor ! progress
    }
  }
}

/* autowire => PasteActor */
case class AddPaste(
  code: String,
  sbtConfig: String,
  scalaTargetType: ScalaTargetType
) {
  def toRunPaste(id: Long) = RunPaste(id, code, sbtConfig, scalaTargetType)
}


case class GetPaste(id: Long)
case class Paste(
  id: Long,
  code: String,
  sbtConfig: String
)
