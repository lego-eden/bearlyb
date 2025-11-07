package bearlyb.initialize

import org.lwjgl.sdl.SDLInit.*
import bearlyb.util.*

def init(flag: Flags, flags: Flags*): Unit = SDL_Init((flag +: flags).combine)
  .sdlErrorCheck()

def quit(): Unit = SDL_Quit()
