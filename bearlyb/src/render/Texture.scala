package bearlyb.render

import bearlyb.pixels.{PixelFormat, RawColor}
import bearlyb.rect.{Point, Rect}
import bearlyb.surface.ScaleMode
import bearlyb.util.*
import bearlyb.vectors.Vec.*
import bearlyb.video.BlendMode
import bearlyb.video.imghelper
import org.lwjgl.sdl.SDLPixels.*
import org.lwjgl.sdl.SDLRender.*
import org.lwjgl.sdl.{SDL_PixelFormatDetails, SDL_Texture}
import org.lwjgl.system.MemoryStack.stackPush

import java.nio.ByteBuffer
import scala.util.Using
import scala.util.Using.Releasable

class Texture private[bearlyb] (private[bearlyb] val internal: SDL_Texture):

  lazy val w: Int = internal.w
  lazy val h: Int = internal.h
  lazy val format = PixelFormat.fromInternal(internal.format)

  def blendMode: BlendMode = Using(stackPush()): stack =>
    val mode = stack.mallocInt(1)
    SDL_GetTextureBlendMode(internal, mode).sdlErrorCheck()
    BlendMode.fromInternal(mode.get(0))
  .get

  def blendMode_=(mode: BlendMode): Unit =
    SDL_SetTextureBlendMode(internal, mode.internal).sdlErrorCheck()

  private def internalFormat: SDL_PixelFormatDetails =
    SDL_GetPixelFormatDetails(internal.format)

  /** Get the renderer that created this texture
    *
    * @return
    *   the renderer for this texture
    */
  def renderer: Renderer =
    new Renderer(SDL_GetRendererFromTexture(internal).sdlCreationCheck())

  def scaleMode: ScaleMode = Using(stackPush()): stack =>
    val mode = stack.mallocInt(1)
    SDL_GetTextureScaleMode(internal, mode).sdlErrorCheck()
    ScaleMode.fromOrdinal(mode.get(0))
  .get

  def scaleMode_=(mode: ScaleMode): Unit =
    SDL_SetTextureScaleMode(internal, mode.ordinal).sdlErrorCheck()

  def lock(rect: Rect[Int] | Null = null): Texture.Writer =
    val (pixels, pitch, w, h) = Using(stackPush()): stack =>
      val pixels = stack.mallocPointer(1)
      val pitch = stack.mallocInt(1)
      val r = rect.internal(stack)
      SDL_LockTexture(internal, r, pixels, pitch).sdlErrorCheck()
      val p = pitch.get(0)
      val (w, h) = rect match
        case null => (this.w, this.h)
        case r    => (r.w, r.h)
      (pixels.getByteBuffer(0, p * h), p, w, h)
    .get

    Texture.Writer(this, pixels, pitch, w, h)

  end lock

  def unlock(): Unit = SDL_UnlockTexture(internal)

end Texture

object Texture:

  def apply(
      renderer: Renderer,
      format: PixelFormat,
      access: TextureAccess,
      w: Int,
      h: Int
  ): Texture = new Texture(
    SDL_CreateTexture(renderer.internal, format.internal, access.internal, w, h)
      .sdlCreationCheck()
  )

  def loadImage(file: String, renderer: Renderer): Option[Texture] = imghelper
    .loadTexture(file, renderer.internal)
    .map(new Texture(_))

  case class Writer private[render] (
      tex: Texture,
      private val pixels: ByteBuffer,
      private val pitch: Int,
      private val w: Int,
      private val h: Int
  ):
    private lazy val internalFormat = tex.internalFormat

    def update(pos: Point[Int], color: RawColor): Unit =
      if !(pos.x < w && pos.y < h && pos.x >= 0 && pos.y >= 0) then
        throw IndexOutOfBoundsException(
          s"The coordinate ${pos.toString} must be within 0..$w and 0..$h"
        )
      assert(pos.x < w && pos.y < h)
      val bpp = internalFormat.bits_per_pixel
      val Bpp = internalFormat.bytes_per_pixel
      require(bpp == 8 || bpp == 16 || bpp == 32, s"Unknown bpp: $bpp")

      val idx = pos.y * pitch + pos.x * Bpp
      Using(stackPush()): stack =>
        val raw = stack.malloc(4).putInt(color.internal)
        for i <- 0 until Bpp do pixels.put(idx + i, raw.get(i))
      .get

    end update

    def update(x: Int, y: Int, color: RawColor): Unit = update((x, y), color)

  end Writer

  object Writer:

    given Releasable[Writer]:
      def release(writer: Writer): Unit = writer.tex.unlock()

end Texture
