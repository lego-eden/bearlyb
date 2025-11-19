import bearlyb as bl, bl.audio.Sound
import scala.io.StdIn.readLine

@main
def loopingAudio(): Unit =
  bl.init(bearlyb.Init.Audio)

  val shootPath = os.resource / "shoot.wav"
  val shoot = Sound.loadWAV(shootPath).get
  val shootTrack = Sound.Track.create()
  shootTrack.onEnd = Some(_ => println("stopped playing"))

  var play = true
  while play do
    readLine(
      "Enter p to start playing, s to stop playing, q to exit\n"
    ).toLowerCase match
      case "q" => play = false
      case "p" => shootTrack.loop(shoot)
      case "s" => shootTrack.stop()
      case _   => ()

  bl.quit()
end loopingAudio
