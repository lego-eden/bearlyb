package bearlyb.render

import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.BufferUtils
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz

case class Font private[bearlyb] (
    libraryBuffPtr: Long,
    private[bearlyb] face: FT_Face,
    private[bearlyb] faceBuffPtr: Long,
    private[bearlyb] hbFontBuffPtr: Long,
    dpi: Int,
    fontSize: Long
):
  def setSize(fontSize: Long = 16): Unit =
    if FreeType.FT_Set_Char_Size(face, 0, fontSize * dpi, 0, 0) != 0 then
      throw RuntimeException("Failed to set char size")
  def destroy: Unit =
    HarfBuzz.hb_font_destroy(hbFontBuffPtr): Unit
    FreeType.FT_Done_Face(face): Unit

object Font:
  def apply(
      fontPath: String = "JetBrainsMono.ttf",
      fontSize: Long = 19,
      dpi: Int = 32,
      libraryPtr: Long
  ): Font =
    val faceBuff = BufferUtils.createPointerBuffer(1)

    if FreeType.FT_New_Face(libraryPtr, fontPath, 0, faceBuff) != 0 then
      throw RuntimeException("FT_New_Face failed")

    val faceBuffPtr = faceBuff.get(0)

    val face = FT_Face.create(faceBuffPtr)

    if FreeType.FT_Set_Char_Size(face, 0, fontSize * dpi, 0, 0) != 0 then
      throw RuntimeException("Failed to set char size")

    val hbFont = HarfBuzz.hb_ft_font_create(faceBuffPtr, null)

    new Font(
      libraryBuffPtr = libraryPtr,
      face = face,
      faceBuffPtr = faceBuffPtr,
      hbFontBuffPtr = hbFont,
      dpi = dpi,
      fontSize = fontSize
    )
