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
    // def apply(): Unit =
    //   val s = Sound.emptyStream
    //   on(s)

  private def playOn(s: AudioStream, flush: Boolean): Unit =
    s.inputFormat = format
    s.gain = gain
    s.freqRatio = freqRatio
    s.put(data)
    if flush then s.flush()

  def prettyString: String =
    s"""Sound(
       |  format = ${format.toString},
       |  data.length = ${data.length},
       |  gain = $gain,
       |  freqRatio = $freqRatio
       |)""".stripMargin

object Sound:
  import scala.collection.mutable.ArrayDeque as MutDeque
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


  private def createStream(dev: AudioDevice): AudioStream =
    val s = AudioStream()
    s.bind(dev)
    s

  class Track private (
      private val s: AudioStream,
      private val sounds: MutDeque[(repeat: Int, snd: Sound)],
      private var _onEnd: Option[Track => Unit] = None
  ):
    private var hasEnded = true

    s.setGetCallback((stream, additionalAmount, _) =>
      if additionalAmount > 0 then
        lazy val peek = sounds.headOption
        lazy val sameFormat =
          peek.map(_.snd.format == s.inputFormat).getOrElse(false)

        if isPlaying && !sameFormat then stream.flush()

        if stream.available == 0 || sameFormat then
          sounds.removeHeadOption() match
            case Some(sound @ (repeat, snd)) =>
              if repeat > 0 then sounds.prepend((repeat - 1, snd)): Unit
              else if repeat == -1 then sounds.prepend(sound): Unit
              hasEnded = false
              snd.playOn(stream, flush = false)
            case None =>
          end match

          if !hasEnded && peek.map(_.repeat).getOrElse(0) == 0 then
            hasEnded = true
            onEnd.foreach(_(this))
        end if
      end if
    )

    private def withLock[A](body: => A): A =
      s.lock()
      try
        body
      finally
        s.unlock()

    def onEnd: Option[Track => Unit] = withLock(_onEnd)

    def onEnd_=(cb: Option[Track => Unit]): Unit =
      withLock:
        _onEnd = cb

    def queue(snd: Sound): Unit =
      withLock:
        sounds.append((0, snd)): Unit

    def loop(snd: Sound, n: Int = -1): Unit =
      assert(n >= 0 || n == -1)
      withLock:
        // the looping logic happens inside the get-callback of the audio stream
        sounds.append((n, snd)): Unit
    def play(snd: Sound): Unit =
      withLock:
        s.clear()
        queue(snd)

    /** Clears queued data and the queue of sounds, this makes the stream stop
      * playing. If you simply want to stop playback, but be able to continue
      * playing later with the same data, then use [[pause]] instead.
      */
    def stop(): Unit =
      withLock:
        sounds.clear()
        s.clear()

    def pause(): Unit = s.pause()

    def resume(): Unit = s.resume()

    def isPlaying: Boolean =
      withLock(!hasEnded)

  object Track:
    def create(dev: AudioDevice = AudioDevice.open.defaultPlayback()): Track =
      val s = Sound.createStream(dev)
      new Track(s, MutDeque.empty)

  end Track

  class Mixer private (
      private val dev: AudioDevice,
      private val tracks: JQueue[AudioStream],
  ):
    private def emptyStream: AudioStream = findEmptyStream match
      case Some(track) => track
      case None        =>
        val track = createStream(dev)
        tracks.offer(track)
        track

    private def findEmptyStream: Option[AudioStream] =
      val trackopt = tracks.stream().filter(s => s.available == 0).findFirst()
      Option(trackopt.orElse(null))


    def play(snd: Sound): Unit =
      snd.playOn(emptyStream, flush = true)

  object Mixer:
    def apply(dev: AudioDevice = AudioDevice.open.defaultPlayback()): Mixer =
      new Mixer(dev, JQueue())
  end Mixer

end Sound
