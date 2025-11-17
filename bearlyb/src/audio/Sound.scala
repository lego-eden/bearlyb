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

case class Sound(
    format: AudioSpec,
    data: IndexedSeq[Byte],
    gain: Float = 1.0f,
    freqRatio: Float = 1.0f
):
  object play:
    def apply(): Unit =
      val s = Sound.emptyStream
      on(s)

    def on(s: AudioStream): Unit =
      s.inputFormat = format
      s.gain = gain
      s.freqRatio = freqRatio
      s.put(data)
      s.flush()

object Sound:
  soundObject =>

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
  private def emptyStream: AudioStream = findEmptyStream match
    case Some(track) => track
    case None        =>
      val track = createStream()
      tracks.offer(track)
      println(s"total number of tracks: ${tracks.size()}")
      track

  private def findEmptyStream: Option[AudioStream] =
    val trackopt = tracks.stream().filter(s => s.available == 0).findFirst()
    Option(trackopt.orElse(null))

  private def createStream(): AudioStream =
    val s = AudioStream()
    s.bind(dev)
    s

  class Track private (
      private val s: AudioStream,
      private val sounds: JQueue[Sound],
      private var _onEnd: Option[Track => Unit]
  ):
    private var hasEnded = true

    s.setGetCallback((stream, additionalAmount, _) =>
      if additionalAmount > 0 && stream.available == 0 then
        if !hasEnded then
          hasEnded = true
          onEnd.foreach(_(this))
        Option(sounds.poll()) match
          case Some(snd) =>
            hasEnded = false
            snd.play.on(stream)
          case None =>
    )

    def onEnd: Option[Track => Unit] =
      s.lock()
      val res = _onEnd
      s.unlock()
      res
    def onEnd_=(cb: Option[Track => Unit]): Unit =
      s.lock()
      _onEnd = cb
      s.unlock()
    def queue(snd: Sound): Unit = sounds.offer(snd): Unit
    def play(snd: Sound): Unit =
      s.clear()
      queue(snd)
    def clear(): Unit =
      sounds.clear()
      s.clear()
    def pause(): Unit = s.pause()
    def resume(): Unit = s.resume()
    def isPlaying: Boolean =
      s.lock()
      val res = !hasEnded
      s.unlock()
      res

  object Track:
    def apply(): Track =
      val s = soundObject.createStream()
      new Track(s, JQueue(), None)
end Sound
