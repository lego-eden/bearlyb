package bearlyb.audio

import org.lwjgl.sdl.SDLAudio.*
import org.lwjgl.sdl.SDL_AudioStreamCallbackI
import org.lwjgl.system.MemoryStack.stackPush
import bearlyb.util.*
import scala.util.Using

type AudioStreamCallback = (AudioStream, Int, Int) => Unit

def openAudioDeviceStream(
    devid: AudioDevice,
    spec: AudioSpec | Null = null,
    callback: AudioStreamCallback | Null = null
  ): AudioStream =
  val internalCallback: SDL_AudioStreamCallbackI | Null = callback match
    case null                    => null
    case cb: AudioStreamCallback =>
      (_, stream, additionalAmount, totalAmount) =>
        cb(AudioStream.fromInternal(stream), additionalAmount, totalAmount)
  Using.resource(stackPush()): stack =>
    val sdlspec = spec match
      case null         => null
      case s: AudioSpec => s.internal(stack)
    AudioStream.fromInternal(
      SDL_OpenAudioDeviceStream(
        devid.internal, sdlspec, internalCallback, NullPtr
      ).sdlCreationCheck()
    )

end openAudioDeviceStream

def currentAudioDriver: String | Null = SDL_GetCurrentAudioDriver()

def audioDrivers: Iterator[String] = new Iterator[String]:
  private val numAudioDrivers = SDL_GetNumAudioDrivers()
  private var i               = 0

  override def next(): String =
    if hasNext then
      val result = SDL_GetAudioDriver(i).sdlCreationCheck()
      i += 1
      result
    else throw java.util.NoSuchElementException("No more audio drivers")

  override def hasNext: Boolean = i < numAudioDrivers
