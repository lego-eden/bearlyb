package bearlyb.initialize

import bearlyb.util.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.util.freetype.FreeType
import bearlyb.render.Renderer

def init(flag: Flags, flags: Flags*): Unit = SDL_Init((flag +: flags).combine)
  .sdlErrorCheck()

def quit(): Unit =
  SDL_Quit()
