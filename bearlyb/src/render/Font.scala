package bearlyb.render

import bearlyb.util.*
import org.lwjgl.util.freetype.FT_Face
import org.lwjgl.BufferUtils
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.harfbuzz.HarfBuzz
import java.nio.ByteBuffer

class Font private[bearlyb] (
    private[bearlyb] val face: FT_Face,
    private[bearlyb] val hbFontPtr: Long,
    private[bearlyb] val fontBuffer: ByteBuffer,
):
  private[bearlyb] def setSize(fontSize: Long, dpi: Int): Unit =
    if FreeType.FT_Set_Char_Size(face, 0, fontSize << 6, dpi, dpi) != 0 then
      throw RuntimeException("Failed to set char size")
    HarfBuzz.hb_ft_font_changed(hbFontPtr)

  def destroy(): Unit =
    HarfBuzz.hb_font_destroy(hbFontPtr): Unit
    FreeType.FT_Done_Face(face): Unit

object Font:
  def fromFile(
      path: os.ReadablePath,
      faceIndex: Long = 0,
  ): Font =
    fromBytes(os.read.bytes(path), faceIndex)

  def fromBytes(
      fontBytes: Array[Byte],
      faceIndex: Long = 0,
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
      fontBuffer = fontBuffer,
    )
  end fromBytes

  lazy val defaultMono: Font =
    Font.fromFile(os.resource/"JetBrainsMono.ttf")

  private lazy val FTLib = withStack:
    val libraryBuf = stack.mallocPointer(1)
    if FreeType.FT_Init_FreeType(libraryBuf) != 0 then
      throw RuntimeException("FT_Init_FreeType failed")

    val ft = libraryBuf.get(0)
    val shutdown = Thread.ofVirtual().unstarted(() => assert(FreeType.FT_Done_FreeType(ft) == 0))
    Runtime.getRuntime().addShutdownHook(shutdown)

    ft
  end FTLib
  
end Font
