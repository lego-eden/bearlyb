import bearlyb as bl, bl.audio.Sound
import scala.io.StdIn.readLine

@main
def helloAudio(): Unit =
  bl.init(bearlyb.Init.Audio)

  val quackPath = os.resource / "quack.wav"
  val quack = Sound.loadWAV(quackPath).get

  var play = true
  while play do
    readLine("Enter p to play or q to exit\n").toLowerCase match
      case "q" => play = false
      case "p" => quack.play()
      case _   => ()

  bl.quit()
end helloAudio
