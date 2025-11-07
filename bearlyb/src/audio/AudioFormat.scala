package bearlyb.audio

import org.lwjgl.sdl.SDLAudio.*
import java.nio.ByteOrder

opaque type AudioFormat = Int

object AudioFormat:
  /** Unspecified audio format */
  val Unknown: AudioFormat = SDL_AUDIO_UNKNOWN
  // SDL_DEFINE_AUDIO_FORMAT(0, 0, 0, 8),
  /** Unsigned 8-bit samples */
  val U8: AudioFormat = SDL_AUDIO_U8
  // SDL_DEFINE_AUDIO_FORMAT(1, 0, 0, 8),
  /** Signed 8-bit samples */
  val S8: AudioFormat = SDL_AUDIO_S8
  // SDL_DEFINE_AUDIO_FORMAT(1, 0, 0, 16),
  /** Signed 16-bit samples */
  val S16LE: AudioFormat = SDL_AUDIO_S16LE
  // SDL_DEFINE_AUDIO_FORMAT(1, 1, 0, 16),
  /** As above, but big-endian byte order */
  val S16BE: AudioFormat = SDL_AUDIO_S16BE
  // SDL_DEFINE_AUDIO_FORMAT(1, 0, 0, 32),
  /** 32-bit integer samples */
  val S32LE: AudioFormat = SDL_AUDIO_S32LE
  // SDL_DEFINE_AUDIO_FORMAT(1, 1, 0, 32),
  /** As above, but big-endian byte order */
  val S32BE: AudioFormat = SDL_AUDIO_S32BE
  // SDL_DEFINE_AUDIO_FORMAT(1, 0, 1, 32),
  /** 32-bit floating point samples */
  val F32LE: AudioFormat = SDL_AUDIO_F32LE
  // SDL_DEFINE_AUDIO_FORMAT(1, 1, 1, 32),
  /** As above, but big-endian byte order */
  val F32BE: AudioFormat = SDL_AUDIO_F32BE

  val S16: AudioFormat = SDL_AUDIO_S16
  val S32: AudioFormat = SDL_AUDIO_S32

  val F32: AudioFormat =
    if ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN then SDL_AUDIO_F32LE
    else SDL_AUDIO_F32BE

  def fromInternal(internal: Int): AudioFormat = internal

  extension (f: AudioFormat)
    private[bearlyb] def internal: Int = f
    def name: String                   = SDL_GetAudioFormatName(f)

end AudioFormat
