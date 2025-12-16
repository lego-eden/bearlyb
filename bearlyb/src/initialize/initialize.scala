package bearlyb.initialize

import bearlyb.util.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.BufferUtils
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.util.freetype.FT_Face

def init(flag: Flags, flags: Flags*): Unit = SDL_Init((flag +: flags).combine)
  .sdlErrorCheck()

def initFontRenderer(
    font: String = "JetBrainsMono"
): Long =
  // --- Initialize FreeType and load font ---
  val libraryBuf = BufferUtils.createPointerBuffer(1)
  if FreeType.FT_Init_FreeType(libraryBuf) != 0 then
    throw RuntimeException("FT_Init_FreeType failed")
  val library: Long = libraryBuf.get(0)

  org.lwjgl.system.Configuration.HARFBUZZ_LIBRARY_NAME.set("freetype")

  library

def deInitFontRenderer(libraryBuffPtr: Long): Unit =
  FreeType.FT_Done_FreeType(libraryBuffPtr)
  org.lwjgl.system.Configuration.HARFBUZZ_LIBRARY_NAME.set("")

def quit(): Unit = SDL_Quit()
