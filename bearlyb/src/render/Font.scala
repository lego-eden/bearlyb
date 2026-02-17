package bearlyb.render

import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.BufferUtils
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz
import java.nio.ByteBuffer

case class Font private[bearlyb] (
    libraryPtr: Long,
    fontBuffer: ByteBuffer,
    private[bearlyb] face: FT_Face,
    private[bearlyb] facePtr: Long,
    private[bearlyb] hbFontPtr: Long,
    dpi: Int,
    fontSize: Long
):
  def setSize(fontSize: Long = 19): Unit =
    if FreeType.FT_Set_Char_Size(face, 0, fontSize << 6, dpi, dpi) != 0 then
      throw RuntimeException("Failed to set char size")
    HarfBuzz.hb_ft_font_changed(hbFontPtr)

  def destroy: Unit =
    HarfBuzz.hb_font_destroy(hbFontPtr): Unit
    FreeType.FT_Done_Face(face): Unit

object Font:
  def fromFile(
      renderer: Renderer,
      path: os.ReadablePath = os.resource / "fonts" / "JetBrainsMono.ttf",
      fontSize: Long = 19,
      dpi: Int = 72
  ): Font =
    if !renderer.isFTInitialized then renderer.FTLib: Unit

    val fontBytes = path.getInputStream.readAllBytes()

    val fontBuffer =
      ByteBuffer
        .allocateDirect(fontBytes.length)
        .put(fontBytes)
        .flip()

    val faceBuff = BufferUtils.createPointerBuffer(1)

    if FreeType.FT_New_Memory_Face(
        renderer.FTLib,
        fontBuffer,
        0,
        faceBuff
      ) != 0
    then throw RuntimeException("FT_New_Face failed")

    val faceBuffPtr = faceBuff.get(0)

    val face = FT_Face.create(faceBuffPtr)

    val hbFont = HarfBuzz.hb_ft_font_create(faceBuffPtr, null)

    if FreeType.FT_Set_Char_Size(face, 0, fontSize << 6, dpi, dpi) != 0 then
      throw RuntimeException("Failed to set char size")

    HarfBuzz.hb_ft_font_changed(hbFont)

    new Font(
      libraryPtr = renderer.FTLib,
      fontBuffer = fontBuffer,
      face = face,
      facePtr = faceBuffPtr,
      hbFontPtr = hbFont,
      dpi = dpi,
      fontSize = fontSize
    )

  def fromBytes(
      renderer: Renderer,
      fontBytes: Array[Byte],
      fontSize: Long = 19,
      dpi: Int = 72
  ): Font =
    if !renderer.isFTInitialized then renderer.FTLib: Unit

    val fontBuffer =
      ByteBuffer
        .allocateDirect(fontBytes.length)
        .put(fontBytes)
        .flip()

    val faceBuff = BufferUtils.createPointerBuffer(1)

    if FreeType.FT_New_Memory_Face(
        renderer.FTLib,
        fontBuffer,
        0,
        faceBuff
      ) != 0
    then throw RuntimeException("FT_New_Face failed")

    val faceBuffPtr = faceBuff.get(0)

    val face = FT_Face.create(faceBuffPtr)

    val hbFont = HarfBuzz.hb_ft_font_create(faceBuffPtr, null)

    if FreeType.FT_Set_Char_Size(face, 0, fontSize << 6, dpi, dpi) != 0 then
      throw RuntimeException("Failed to set char size")

    HarfBuzz.hb_ft_font_changed(hbFont)

    new Font(
      libraryPtr = renderer.FTLib,
      fontBuffer = fontBuffer,
      face = face,
      facePtr = faceBuffPtr,
      hbFontPtr = hbFont,
      dpi = dpi,
      fontSize = fontSize
    )
