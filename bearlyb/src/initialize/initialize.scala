package bearlyb.initialize

import bearlyb.util.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.system.Configuration as LwjglConfiguration

def init(flag: Flags, flags: Flags*): Unit =
  LwjglConfiguration.HARFBUZZ_LIBRARY_NAME
    .set(FreeType.getLibrary())

  SDL_Init((flag +: flags).combine)
    .sdlErrorCheck()

def quit(): Unit =
  SDL_Quit()
