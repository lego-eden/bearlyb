import bearlyb as bl
import bl.audio.*

@main
def audioStaircase(): Unit =
  bl.init(bl.Init.Audio)

  val shootPath = os.resource/"shoot.wav"
  val shoot = Sound.loadWAV(shootPath).get
  val shootTrack = Sound.Track()
  
  for i <- 4 to 24 do
    shootTrack.queue(shoot.copy(freqRatio = i*0.1f))
  for i <- 24 to 4 by -1 do
    shootTrack.queue(shoot.copy(freqRatio = i*0.1f))
    
  io.StdIn.readLine("press q to quit")

  bl.quit()