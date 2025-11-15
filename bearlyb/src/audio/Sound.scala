package bearlyb.audio

import bearlyb.util.*
import org.lwjgl.PointerBuffer
import org.lwjgl.sdl.SDLAudio.{SDL_LoadWAV, SDL_LoadWAV_IO}
import org.lwjgl.sdl.SDLIOStream.*
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.sdl.SDL_AudioSpec
import org.lwjgl.system.MemoryUtil.{memAlloc, memFree}

import java.nio.IntBuffer
import scala.collection.immutable.ArraySeq

case class Sound(format: AudioSpec, data: IndexedSeq[Byte], gain: Float = 1.0f):
  def play(): Unit =
    val s = Sound.emptyTrack
    s.inputFormat = format
    s.gain = gain
    s.put(data)

object Sound:
  import java.util.concurrent.ConcurrentLinkedQueue as JQueue

  object loadWAV:
    private def loadhelper(
        load: (SDL_AudioSpec, PointerBuffer, IntBuffer) => Boolean
    ): Option[Sound] = withStack:
      val format = SDL_AudioSpec.malloc(stack)
      val audioBuf = stack.mallocPointer(1)
      val audioLen = stack.mallocInt(1)
      if !load(format, audioBuf, audioLen) then None
      else
        val len = audioLen.get(0)
        val dataBuf = audioBuf.getByteBuffer(0, len)
        try
          val data = Array.ofDim[Byte](len)
          dataBuf.get(0, data)
          Some(
            Sound(
              AudioSpec.fromInternal(format),
              ArraySeq.unsafeWrapArray(data)
            )
          )
        finally SDL_free(dataBuf)

    def apply(filepath: String): Option[Sound] =
      loadhelper(SDL_LoadWAV(filepath, _, _, _))

    def apply(filepath: os.ReadablePath): Option[Sound] =
      val bytes = os.read.bytes(filepath)
      fromBytes(bytes)

    def fromBytes(bytes: Array[Byte]): Option[Sound] =
      val buf = memAlloc(bytes.size)
      try
        buf.put(0, bytes)
        val ioStream = SDL_IOFromConstMem(buf).sdlCreationCheck()
        loadhelper(SDL_LoadWAV_IO(ioStream, true, _, _, _))
      finally memFree(buf)

  // simplified audio system
  private val dev = AudioDevice.open.defaultPlayback()
  private val tracks = JQueue[AudioStream]()
  private def emptyTrack: AudioStream = findEmptyTrack match
    case Some(track) => track
    case None        =>
      val track = createTrack()
      tracks.offer(track)
      println(s"total number of tracks: ${tracks.size()}")
      track

  private def findEmptyTrack: Option[AudioStream] =
    val trackopt = tracks.stream().filter(s => s.available == 0).findFirst()
    Option(trackopt.orElse(null))

  private def createTrack(): AudioStream =
    val s = AudioStream()
    s.bind(dev)
    s

end Sound
