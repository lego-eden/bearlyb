package bearlyb.audio

import org.lwjgl.sdl.SDLAudio.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.sdl.SDLStdinc.SDL_free
import scala.util.Using
import org.lwjgl.sdl.SDL_AudioStreamCallbackI
import bearlyb.util.*
import org.lwjgl.sdl.SDL_AudioSpec
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer

type AudioDeviceID = Int

object AudioDeviceID:
  val DefaultPlayback: AudioDeviceID  = SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK
  val DefaultRecording: AudioDeviceID = SDL_AUDIO_DEVICE_DEFAULT_RECORDING

opaque type AudioDevice = Int

object AudioDevice:
  private[bearlyb] inline def fromInternal(dev: Int): AudioDevice = dev

  def formatOf(devid: AudioDeviceID): AudioSpec = withStack:
    val spec = SDL_AudioSpec.malloc
    SDL_GetAudioDeviceFormat(devid, spec, null).sdlErrorCheck()
    AudioSpec.fromInternal(spec)

  object open:

    def apply(dev: AudioDeviceID, spec: AudioSpec | Null = null): AudioDevice =
      Using.resource(stackPush()): stack =>
        val ispec = spec.internal(stack)
        val id    = SDL_OpenAudioDevice(dev, ispec)
        if id == 0 then sdlError() else id

    inline def defaultPlayback(spec: AudioSpec | Null = null): AudioDevice =
      apply(AudioDeviceID.DefaultPlayback, spec)

    inline def defaultRecording(spec: AudioSpec | Null = null): AudioDevice =
      apply(AudioDeviceID.DefaultRecording, spec)

    object stream:

      def apply(
          dev: AudioDeviceID,
          spec: AudioSpec | Null = null,
          callback: AudioStreamCallback | Null = null
        ): AudioStream =
        val internalCallback: SDL_AudioStreamCallbackI | Null = callback match
          case null                    => null
          case cb: AudioStreamCallback =>
            (_, stream, additionalAmount, totalAmount) =>
              cb(
                AudioStream.fromInternal(stream), additionalAmount, totalAmount
              )
        Using.resource(stackPush()): stack =>
          val sdlspec = spec match
            case null         => null
            case s: AudioSpec => s.internal(stack)
          AudioStream.fromInternal(
            SDL_OpenAudioDeviceStream(dev, sdlspec, internalCallback, NullPtr)
              .sdlCreationCheck()
          )
      end apply

      def defaultPlayback(
          spec: AudioSpec | Null = null,
          callback: AudioStreamCallback | Null = null
        ): AudioStream = apply(AudioDeviceID.DefaultPlayback, spec, callback)

      def defaultRecording(
          spec: AudioSpec | Null = null,
          callback: AudioStreamCallback | Null = null
        ): AudioStream = apply(AudioDeviceID.DefaultRecording, spec, callback)

    end stream

  end open

  extension (dev: AudioDevice)
    private[bearlyb] inline def internal: Int = dev
    // def isPhysical: Boolean = SDL_IsAudioDevicePhysical(dev)
    def isPlayback: Boolean  = SDL_IsAudioDevicePlayback(dev)
    def isRecording: Boolean = !dev.isPlayback
    def isPaused: Boolean    = SDL_AudioDevicePaused(internal)
    def pause(): Unit        = SDL_PauseAudioDevice(dev).sdlErrorCheck()
    def resume(): Unit       = SDL_ResumeAudioDevice(dev).sdlErrorCheck()
    def close(): Unit        = SDL_CloseAudioDevice(dev)
    def name: String         = SDL_GetAudioDeviceName(dev).sdlCreationCheck()

    def bind(stream: AudioStream): Unit =
      SDL_BindAudioStream(dev, stream.internal).sdlErrorCheck()

    def bind(stream: AudioStream, streams: AudioStream*): Unit =
      bind(stream +: streams)

    def bind(streams: Seq[AudioStream]): Unit = withStack:
      val targetBuf = stack.mallocPointer(streams.length)
      val inputBuf  = streams.iterator.map(_.internal).toArray
      targetBuf.put(inputBuf)
      targetBuf.rewind()
      SDL_BindAudioStreams(dev, targetBuf).sdlErrorCheck()

    def channelMap: Option[IArray[Int]] = SDL_GetAudioDeviceChannelMap(internal)
      .asInstanceOf[java.nio.IntBuffer | Null] match
        case null        => None
        case internalMap =>
          val result: Array[Int] = Array.ofDim(internalMap.remaining)
          internalMap.get(0, result)
          SDL_free(internalMap)
          Some(IArray.unsafeFromArray(result))

    def format: AudioSpec = formatOf(dev)

    def setPostmixCallback(cb: (AudioSpec, FloatBuffer) => Unit): Unit =
      SDL_SetAudioPostmixCallback(
        dev,
        (_, specptr, bufptr, buflen) =>
          val buf = MemoryUtil.memFloatBufferSafe(bufptr, buflen)
            .sdlCreationCheck()
          val spec = AudioSpec
            .fromInternal(SDL_AudioSpec.createSafe(specptr).sdlCreationCheck())
          cb(spec, buf)
        ,
        NullPtr
      ).sdlErrorCheck()

  end extension

end AudioDevice
