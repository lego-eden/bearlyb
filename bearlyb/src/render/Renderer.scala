package bearlyb.render

import bearlyb.*
import bearlyb.pixels.{Color, FColor, PixelFormat}
import bearlyb.rect.{Point, Rect}
import bearlyb.surface.FlipMode
import bearlyb.util.*
import bearlyb.vectors.Vec.{*, given}
import bearlyb.video.{BlendMode, Window}
import org.lwjgl.sdl.SDLRender.*
import org.lwjgl.sdl.{SDL_FPoint, SDL_FRect, SDL_Rect}
import org.lwjgl.system.MemoryStack.stackPush

import scala.annotation.targetName
import scala.util.Using

import Point.*
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz

class Renderer private[render] (private[bearlyb] val internal: Long):

  lazy val name: String = SDL_GetRendererName(internal)
  lazy val window: Window = new Window(SDL_GetRenderWindow(internal))

  private[bearlyb] var isFTInitialized = false
  private[bearlyb] lazy val FTLib = withStack:
    // --- Initialize FreeType and load font ---
    val libraryBuf = stack.mallocPointer(1)
    if FreeType.FT_Init_FreeType(libraryBuf) != 0 then
      throw RuntimeException("FT_Init_FreeType failed")
    isFTInitialized = true

    org.lwjgl.system.Configuration.HARFBUZZ_LIBRARY_NAME.set("freetype")

    libraryBuf.get(0)

  def isViewportSet: Boolean = SDL_RenderViewportSet(internal)

  def viewport: Rect[Int] = withStack:
    val rect = SDL_Rect.malloc(stack)
    SDL_GetRenderViewport(internal, rect).sdlErrorCheck()
    Rect.fromInternal(rect)

  def viewport_=(rect: Rect[Int]): Unit = withStack:
    val r = rect.internal(stack)
    SDL_SetRenderViewport(internal, r).sdlErrorCheck()

  def target: Option[Texture] = Option(SDL_GetRenderTarget(internal))
    .map(new Texture(_))

  def target_=(tex: Option[Texture]): Unit =
    val texture = tex match
      case None      => null
      case Some(tex) => tex.internal
    SDL_SetRenderTarget(internal, texture).sdlErrorCheck()

  def outputSize: (w: Int, h: Int) = Using(stackPush()): stack =>
    val (w, h) = mallocManyInt(2, stack)
    SDL_GetCurrentRenderOutputSize(internal, w, h).sdlErrorCheck()
    (w.get(0), h.get(0))
  .get

  def clipRect: Rect[Int] = Using(stackPush()): stack =>
    val clip = SDL_Rect.malloc(stack)
    SDL_GetRenderClipRect(internal, clip).sdlErrorCheck()
    Rect.fromInternal(clip)
  .get

  def clipRect_=(clip: Rect[Int]) = Using(stackPush()): stack =>
    val r = clip.internal(stack)
    SDL_SetRenderClipRect(internal, r).sdlErrorCheck()
  .get

  def colorScale: Float = Using(stackPush()): stack =>
    val scale = stack.mallocFloat(1)
    SDL_GetRenderColorScale(internal, scale).sdlErrorCheck()
    scale.get(0)
  .get

  def colorScale_=(scale: Float): Unit =
    SDL_SetRenderColorScale(internal, scale).sdlErrorCheck()

  def vsync: Int = Using(stackPush()): stack =>
    val vsync = stack.mallocInt(1)
    SDL_GetRenderVSync(internal, vsync).sdlErrorCheck()
    vsync.get(0)
  .get

  def vsync_=(vsync: Int): Unit = SDL_SetRenderVSync(internal, vsync)
    .sdlErrorCheck()

  def renderScale: (scaleX: Float, scaleY: Float) = Using(stackPush()): stack =>
    val (scaleX, scaleY) = mallocManyFloat(2, stack)
    SDL_GetRenderScale(internal, scaleX, scaleY).sdlErrorCheck()
    (scaleX, scaleY).vmap(_.get(0))
  .get

  def renderScale_=(scale: (scaleX: Float, scaleY: Float)): Unit =
    val (scaleX, scaleY) = scale
    SDL_SetRenderScale(internal, scaleX, scaleY).sdlErrorCheck()

  def logicalPresentation
      : (w: Int, h: Int, mode: Renderer.LogicalPresentation) =
    Using(stackPush()): stack =>
      val (w, h, mode) = mallocManyInt(3, stack)
      SDL_GetRenderLogicalPresentation(internal, w, h, mode).sdlErrorCheck()
      (
        w.get(0),
        h.get(0),
        Renderer.LogicalPresentation.fromInternal(mode.get(0))
      )
    .get

  def logicalPresentation_=(
      logicalPresentation: (w: Int, h: Int, mode: Renderer.LogicalPresentation)
  ): Unit =
    val (w, h, mode) = logicalPresentation
    SDL_SetRenderLogicalPresentation(internal, w, h, mode.internal)
      .sdlErrorCheck()

  def logicalPresentationRect: Rect[Float] = Using(stackPush()): stack =>
    val r = SDL_FRect.malloc(stack)
    SDL_GetRenderLogicalPresentationRect(internal, r).sdlErrorCheck()
    Rect.fromInternal(r)
  .get

  def drawColorFloat: FColor = Using(stackPush()): stack =>
    val (r, g, b, a) = mallocManyFloat(4, stack)
    SDL_GetRenderDrawColorFloat(internal, r, g, b, a).sdlErrorCheck()
    (r, g, b, a).vmap(_.get(0))
  .get

  def drawColorFloat_=(color: FColor): Unit =
    val (r, g, b, a) = color
    SDL_SetRenderDrawColorFloat(internal, r, g, b, a).sdlErrorCheck()

  def drawColor: Color = Using(stackPush()): stack =>
    val (r, g, b, a) = mallocMany(4, stack)
    SDL_GetRenderDrawColor(internal, r, g, b, a).sdlErrorCheck()
    (r, g, b, a).vmap(_.get(0).toInt)
  .get

  def drawColor_=(color: Color): Unit =
    val (r, g, b, a) = color.toTuple.vmap(_.toByte)
    SDL_SetRenderDrawColor(internal, r, g, b, a).sdlErrorCheck()

  def drawBlendMode: BlendMode = Using(stackPush()): stack =>
    val blendMode = stack.mallocInt(1)
    SDL_GetRenderDrawBlendMode(internal, blendMode).sdlErrorCheck()
    BlendMode.fromInternal(blendMode.get(0))
  .get

  def drawBlendMode_=(blendMode: BlendMode): Unit =
    SDL_SetRenderDrawBlendMode(internal, blendMode.internal).sdlErrorCheck()

  def clear(): Unit = SDL_RenderClear(internal).sdlErrorCheck()
  def present(): Unit = SDL_RenderPresent(internal).sdlErrorCheck()

  def drawPoint[T: Numeric as num](x: T, y: T): Unit =
    SDL_RenderPoint(internal, num.toFloat(x), num.toFloat(y)).sdlErrorCheck()

  def drawPoint[T: Numeric as num](pt: Point[T]): Unit =
    val (x, y) = pt.vmap(num.toFloat)
    SDL_RenderPoint(internal, x, y).sdlErrorCheck()

  def drawPoints[T: Numeric as num](points: Seq[Point[T]]): Unit =
    Using(stackPush()): stack =>
      val buf = SDL_FPoint.malloc(points.size, stack)
      for (p, i) <- points.zipWithIndex do
        buf.get(i).set(num.toFloat(p.x), num.toFloat(p.y))
      SDL_RenderPoints(internal, buf).sdlErrorCheck()
    .get

  @targetName("drawPointsVarargs")
  def drawPoints[T: Numeric](points: Point[T]*): Unit = drawPoints(points)

  def drawLine[T: Numeric](x1: T, y1: T, x2: T, y2: T): Unit =
    import math.Numeric.Implicits.infixNumericOps
    SDL_RenderLine(internal, x1.toFloat, y1.toFloat, x2.toFloat, y2.toFloat)
      .sdlErrorCheck()

  def drawLine[T: Numeric](from: Point[T], to: Point[T]): Unit =
    val (x1, y1) = from
    val (x2, y2) = to
    drawLine(x1, y1, x2, y2)

  def drawLines[T: Numeric as num](points: Seq[Point[T]]): Unit =
    Using(stackPush()): stack =>
      val buf = SDL_FPoint.malloc(points.size, stack)
      for ((x, y), i) <- points.zipWithIndex do
        buf.get(i).set(num.toFloat(x), num.toFloat(y))
      SDL_RenderLines(internal, buf).sdlErrorCheck()
    .get

  @targetName("drawLinesVarargs")
  def drawLines[T: Numeric](points: Point[T]*): Unit = drawLines(points)

  def drawRect[T: Numeric](rect: Rect[T]): Unit = Using(stackPush()): stack =>
    val sdlRect = rect.floatInternal(stack)
    SDL_RenderRect(internal, sdlRect).sdlErrorCheck()
  .get

  def drawRects[T: Numeric](rects: Seq[Rect[T]]): Unit =
    import math.Numeric.Implicits.infixNumericOps
    Using(stackPush()): stack =>
      val buf = SDL_FRect.malloc(rects.size, stack)
      for (Rect(x, y, w, h), i) <- rects.zipWithIndex do
        buf.get(i).set(x.toFloat, y.toFloat, w.toFloat, h.toFloat)
      SDL_RenderRects(internal, buf).sdlErrorCheck()
    .get

  end drawRects

  @targetName("drawRectsVarargs")
  def drawRects[T: Numeric](rects: Rect[T]*): Unit = drawRects(rects)

  @targetName("fillRectFloat")
  def fillRect(rect: Rect[Float]): Unit = Using(stackPush()): stack =>
    val sdlRect = rect.internal(stack)
    SDL_RenderFillRect(internal, sdlRect).sdlErrorCheck()
  .get

  def fillRect[T: Numeric](rect: Rect[T]): Unit = fillRect(rect.toFloatRect)

  @targetName("fillRectsFloat")
  def fillRects(rects: Seq[Rect[Float]]): Unit = Using(stackPush()): stack =>
    val buf = SDL_FRect.malloc(rects.size, stack)
    for (Rect(x, y, w, h), i) <- rects.zipWithIndex do
      buf.get(i).set(x, y, w, h)
    SDL_RenderFillRects(internal, buf).sdlErrorCheck()
  .get

  def fillRects[T: Numeric](rects: Seq[Rect[T]]): Unit =
    import Numeric.Implicits.infixNumericOps
    Using(stackPush()): stack =>
      val buf = SDL_FRect.malloc(rects.size, stack)
      for (Rect(x, y, w, h), i) <- rects.zipWithIndex do
        buf.get(i).set(x.toFloat, y.toFloat, w.toFloat, h.toFloat)
      SDL_RenderFillRects(internal, buf).sdlErrorCheck()
    .get

  end fillRects

  @targetName("fillRectsVarargsFloat")
  def fillRects(rects: Rect[Float]*): Unit = fillRects(rects)

  @targetName("fillRectsVarargs")
  def fillRects[T: Numeric](rects: Rect[T]*): Unit = fillRects(rects)

  def renderTexture[T: Numeric](
      tex: Texture,
      src: Rect[T] | Null = null,
      dst: Rect[T] | Null = null
  ): Unit = Using(stackPush()): stack =>
    val srcrect = src.toFloatRect.internal(stack)
    val dstrect = dst.toFloatRect.internal(stack)
    SDL_RenderTexture(internal, tex.internal, srcrect, dstrect).sdlErrorCheck()
  .get

  def renderTexture(tex: Texture): Unit = renderTexture[Float](tex)

  def renderTextureRotated[T: Numeric](
      tex: Texture,
      angle: Double,
      center: Point[T] | Null = null,
      src: Rect[T] | Null = null,
      dst: Rect[T] | Null = null,
      flip: FlipMode = FlipMode.None
  ): Unit =
    import Numeric.Implicits.infixNumericOps
    Using(stackPush()): stack =>
      val srcrect = src.toFloatRect.internal(stack)
      val dstrect = dst.toFloatRect.internal(stack)
      val centerpoint = center match
        case null   => null
        case (x, y) => SDL_FPoint.malloc(stack).set(x.toFloat, y.toFloat)
      val flipmode = flip.internal
      SDL_RenderTextureRotated(
        internal,
        tex.internal,
        srcrect,
        dstrect,
        angle,
        centerpoint,
        flipmode
      ).sdlErrorCheck()
    .get

  end renderTextureRotated

  def renderTexture9Grid[T: Numeric](
      tex: Texture,
      leftWidth: T,
      rightWidth: T,
      topHeight: T,
      bottomHeight: T,
      src: Rect[T] | Null = null,
      dst: Rect[T] | Null = null,
      scale: Float = 1.0f
  ): Unit =
    import Numeric.Implicits.infixNumericOps
    Using(stackPush()): stack =>
      val srcrect = src.floatInternal(stack)
      val dstrect = dst.floatInternal(stack)
      val leftw = leftWidth.toFloat
      val rightw = rightWidth.toFloat
      val toph = topHeight.toFloat
      val bottomh = bottomHeight.toFloat
      SDL_RenderTexture9Grid(
        internal,
        tex.internal,
        srcrect,
        leftw,
        rightw,
        toph,
        bottomh,
        scale,
        dstrect
      ).sdlErrorCheck()
    .get

  end renderTexture9Grid

  def renderTextureAffine[T: Numeric](
      tex: Texture,
      origin: Point[T] | Null = null,
      right: Point[T] | Null = null,
      down: Point[T] | Null = null,
      src: Rect[T] | Null = null
  ): Unit = Using(stackPush()): stack =>
    val (o, r, d) = (origin, right, down).vmap(_.floatInternal(stack))
    val srcrect = src.floatInternal(stack)
    SDL_RenderTextureAffine(internal, tex.internal, srcrect, o, r, d)
      .sdlErrorCheck()
  .get

  def renderTextureTiled[T: Numeric](
      tex: Texture,
      scale: Float = 1.0f,
      src: Rect[T] | Null = null,
      dst: Rect[T] | Null = null
  ): Unit = Using(stackPush()): stack =>
    val (srcrect, dstrect) = (src, dst).vmap(_.floatInternal(stack))
    SDL_RenderTextureTiled(internal, tex.internal, srcrect, scale, dstrect)
      .sdlErrorCheck()
  .get

  def createTexture(
      format: PixelFormat,
      access: TextureAccess,
      w: Int,
      h: Int
  ): Texture = Texture(this, format, access, w, h)

  // ---------------- MSDF + HarfBuzz pipeline ----------------
  def renderText(
      font: Font,
      text: String,
      x: Float,
      y: Float,
      textSize: Long
  ) =
    font.setSize(textSize)

    // --- Shape text with HarfBuzz ---
    val buffer = HarfBuzz.hb_buffer_create()
    HarfBuzz.hb_buffer_add_utf8(buffer, text, 0, -1)
    HarfBuzz.hb_buffer_guess_segment_properties(buffer)

    HarfBuzz.hb_shape(font.hbFontPtr, buffer, null)

    val count = HarfBuzz.hb_buffer_get_length(buffer)
    val infos = HarfBuzz.hb_buffer_get_glyph_infos(buffer)
    val positions = HarfBuzz.hb_buffer_get_glyph_positions(buffer)

    var penX = x
    var penY = y

    for i <- 0 until count do
      val info = infos.get(i)
      val pos = positions.get(i)
      val glyphIndex = info.codepoint()

      // Load glyph into FreeType
      if FreeType.FT_Load_Glyph(
          font.face,
          glyphIndex,
          FreeType.FT_LOAD_DEFAULT
        ) != 0
      then throw BearlybException(s"Failed to load glyph $glyphIndex")

      FreeType.FT_Render_Glyph(
        font.face.glyph(),
        FreeType.FT_RENDER_MODE_NORMAL
      )

      val slot = font.face.glyph()
      val bitmap = slot.bitmap()
      val width = bitmap.width()
      val rows = bitmap.rows()
      val pitch = bitmap.pitch()

      if width > 0 && rows > 0 then
        val bufferPtr = bitmap.buffer(rows * pitch)

        if bufferPtr != null && bufferPtr.remaining() >= rows * pitch then
          val glyphTex = bearlyb.render.Texture(
            this,
            PixelFormat.RGBA8888,
            TextureAccess.Streaming,
            width,
            rows
          )

          glyphTex.blendMode = BlendMode.Blend

          Using.resource(glyphTex.lock()): tex_w =>
            for row <- 0 until rows; col <- 0 until width do
              val alpha = (bufferPtr.get(
                row * pitch + col
              ) & 0xff)

              val (r, g, b, a) = drawColor

              val finalAlpha =
                ((alpha & 0xff) * (a & 0xff) / 255).toInt

              val color = glyphTex.format.mapColor((r, g, b, finalAlpha))

              tex_w(col, row) = color

          val bearingX = slot.bitmap_left()
          val bearingY = slot.bitmap_top()

          val renderX = (penX + bearingX + (pos.x_offset >> 6)).toInt
          val renderY = (penY - bearingY + (pos.y_offset >> 6)).toInt

          renderTexture(
            glyphTex,
            dst = Rect(renderX, renderY, width, rows)
          )

      val advanceX = pos.x_advance() / 64.0f
      val advanceY = pos.y_advance() / 64.0f

      penX += advanceX
      penY += advanceY

    // Clean up
    HarfBuzz.hb_buffer_destroy(buffer)

  end renderText

  /** Render a string to this renderer
    *
    * This function has severe limitations:
    *   - Accepts UTF8-strings, but only renders ASCII-characters
    *   - Has a single tiny size (8x8 pixels). You can use logical presentation
    *     or render scale to change it.
    *   - It does not support proper scaling or different font selections like
    *     bold or italic, because it has a single hardcoded bitmap-font.
    *   - It does not do word-wrapping or handle newlines, everything is
    *     renderered on a single line.
    *
    * @param pt
    * @param str
    * @param num
    */
  def renderDebugText[T: Numeric as num](pt: Point[T])(str: String): Unit =
    val (x, y) = pt.vmap(num.toFloat)
    SDL_RenderDebugText(internal, x, y, str).sdlErrorCheck()

end Renderer

object Renderer:

  def apply(window: Window): Renderer =
    new Renderer(SDL_CreateRenderer(window.internal, "").sdlCreationCheck())

  given Using.Releasable[Renderer]:

    def release(resource: Renderer): Unit =
      if resource.isFTInitialized then
        FreeType.FT_Done_FreeType(resource.FTLib)
        org.lwjgl.system.Configuration.HARFBUZZ_LIBRARY_NAME.set("")
      SDL_DestroyRenderer(resource.internal)

  enum LogicalPresentation:
    /** there is no logical size in effect */
    case Disabled

    /** the rendered content is stretched to the output resolution */
    case Stretch

    /** the rendered content is fit to the largest dimension and the other
      * dimension is letterboxed with the clear color
      */
    case Letterbox

    /** the rendered content is fit to the largest dimension and the other
      * dimension extends beyond the output bounds
      */
    case Overscan

    /** The rendered content is scaled up by integer multiplication */
    case IntegerScale

    private[bearlyb] def internal: Int = ordinal

  end LogicalPresentation

  object LogicalPresentation:

    private[bearlyb] def fromInternal(mode: Int): LogicalPresentation =
      fromOrdinal(mode)

end Renderer
