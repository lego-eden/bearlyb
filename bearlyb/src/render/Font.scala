package bearlyb.render

import bearlyb.util.*
import bearlyb.vectors.Vec.given
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.BufferUtils
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz
import java.nio.ByteBuffer
import bearlyb.BearlybException
import org.lwjgl.util.harfbuzz.hb_glyph_info_t
import org.lwjgl.util.harfbuzz.hb_glyph_position_t

class Font private[bearlyb] (
    private[bearlyb] val face: FT_Face,
    private[bearlyb] val hbFontPtr: Long,
    private[bearlyb] val fontBuffer: ByteBuffer
):
  private[bearlyb] def setSize(textSize: Long, dpi: Int): Unit =
    if FreeType.FT_Set_Char_Size(face, 0, textSize << 6, dpi, dpi) != 0 then
      throw RuntimeException("Failed to set char size")
    HarfBuzz.hb_ft_font_changed(hbFontPtr)

  private[bearlyb] def renderGlyph(
      info: hb_glyph_info_t
  ): (
      bitmap: ByteBuffer,
      bitmapLeft: Int,
      bitmapTop: Int,
      width: Int,
      height: Int,
      pitch: Int
  ) =
    val glyphIndex = info.codepoint()

    // Load glyph into FreeType
    if FreeType.FT_Load_Glyph(
        face,
        glyphIndex,
        FreeType.FT_LOAD_DEFAULT | FreeType.FT_LOAD_COLOR
      ) != 0
    then throw BearlybException(s"Failed to load glyph $glyphIndex")

    val slot = face.glyph()
    val ret = FreeType.FT_Render_Glyph(
      face.glyph(),
      FreeType.FT_RENDER_MODE_NORMAL
    )
    if ret == FreeType.FT_Err_Missing_SVG_Hooks then
      throw BearlybException(
        "Cannot render emojis and other SVG's, maybe this will be supported by bearlyb in the future"
      )
    else if ret != 0 then
      throw BearlybException(
        s"Failed to render glyph: ${FreeType.FT_Error_String(ret)}"
      )

    val bitmap = slot.bitmap()
    val width = bitmap.width()
    val rows = bitmap.rows()
    val pitch = bitmap.pitch()

    (bitmap.buffer(rows * pitch), slot.bitmap_left(), slot.bitmap_top(), width, rows, pitch)
  end renderGlyph
    

  def metrics(
      textSize: Long,
      dpi: Int = DefaultDPI
  ): (
      ascender: Float,
      descender: Float,
      lineSpacing: Float
  ) =
    setSize(textSize, dpi)
    val metrics = face.size.metrics
    (metrics.ascender, metrics.descender, metrics.height)
      .vmap(_.toFloat / 64.0f)

  def glyphHeight(textSize: Long, dpi: Int = DefaultDPI): Float =
    setSize(textSize, dpi)
    val metrics = face.size.metrics
    (metrics.ascender - metrics.descender).toFloat / 64.0f

  private[bearlyb] def foreachGlyph(
      text: String,
      textSize: Long,
      dpi: Int
  )(
      body: (Int, Int, hb_glyph_position_t, hb_glyph_info_t) => Unit
  ): Unit =
    setSize(textSize, dpi)

    // --- Shape text with HarfBuzz ---
    val buffer = HarfBuzz.hb_buffer_create()
    HarfBuzz.hb_buffer_add_utf8(buffer, text, 0, -1)
    HarfBuzz.hb_buffer_guess_segment_properties(buffer)

    HarfBuzz.hb_shape(hbFontPtr, buffer, null)

    val count = HarfBuzz.hb_buffer_get_length(buffer)
    val positions = HarfBuzz.hb_buffer_get_glyph_positions(buffer)
    val infos = HarfBuzz.hb_buffer_get_glyph_infos(buffer)

    for i <- 0 until count do
      body(i, count, positions.get(i), infos.get(i))

    HarfBuzz.hb_buffer_destroy(buffer)
  end foreachGlyph

  def measure(
      text: String,
      textSize: Long,
      dpi: Int = DefaultDPI
  ): (
      width: Float,
      height: Float
  ) =
    var width = 0L
    foreachGlyph(text, textSize, dpi){ (i, count, pos, info) =>
      if i == 0 || i == count - 1 then
        val (_, bearingX, _, bitmapW, _, _) = renderGlyph(info)

        if i == 0 && bearingX < 0 then
          width -= bearingX.toLong << 6

        if i == count - 1 then
          width += (bitmapW.toLong << 6) max pos.x_advance().toLong
        else
          width += pos.x_advance()
      else
        width += pos.x_advance()
    }
    (width.toFloat / 64.0f, glyphHeight(textSize, dpi))

  def destroy(): Unit =
    HarfBuzz.hb_font_destroy(hbFontPtr): Unit
    FreeType.FT_Done_Face(face): Unit

object Font:
  def fromFile(
      path: os.ReadablePath,
      faceIndex: Long = 0
  ): Font =
    fromBytes(os.read.bytes(path), faceIndex)

  def fromBytes(
      fontBytes: Array[Byte],
      faceIndex: Long = 0
  ): Font =
    val ftlib = Font.FTLib

    val fontBuffer =
      ByteBuffer
        .allocateDirect(fontBytes.length)
        .put(fontBytes)
        .flip()

    val faceBuff = BufferUtils.createPointerBuffer(1)

    if FreeType.FT_New_Memory_Face(
        ftlib,
        fontBuffer,
        faceIndex,
        faceBuff
      ) != 0
    then throw RuntimeException("FT_New_Face failed")

    val faceBuffPtr = faceBuff.get(0)

    val face = FT_Face.create(faceBuffPtr)

    val hbFont = HarfBuzz.hb_ft_font_create(faceBuffPtr, null)

    new Font(
      face = face,
      hbFontPtr = hbFont,
      fontBuffer = fontBuffer
    )
  end fromBytes

  lazy val defaultMono: Font =
    Font.fromFile(os.resource / "JetBrainsMono.ttf")

  lazy val default: Font =
    Font.fromFile(os.resource / "Nunito-Medium.ttf")

  lazy val defaultBold: Font =
    Font.fromFile(os.resource / "Nunito-ExtraBold.ttf")

  lazy val defaultItalic: Font =
    Font.fromFile(os.resource / "Nunito-MediumItalic.ttf")

  lazy val defaultBoldItalic: Font =
    Font.fromFile(os.resource / "Nunito-ExtraBoldItalic.ttf")

  private lazy val FTLib = withStack:
    val libraryBuf = stack.mallocPointer(1)
    if FreeType.FT_Init_FreeType(libraryBuf) != 0 then
      throw RuntimeException("FT_Init_FreeType failed")

    val ft = libraryBuf.get(0)
    bearlyb.initialize.addShutdownHook:
      assert(FreeType.FT_Done_FreeType(ft) == 0)

    ft
  end FTLib
end Font
