package bearlyb.initialize

import bearlyb.util.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.util.freetype.FreeType
import org.lwjgl.system.Configuration as LwjglConfiguration
import scala.collection.mutable.Buffer as MutBuf

def init(flag: Flags, flags: Flags*): Unit =
  LwjglConfiguration.HARFBUZZ_LIBRARY_NAME
    .set(FreeType.getLibrary())

  SDL_Init((flag +: flags).combine)
    .sdlErrorCheck()

private[initialize] val shutdownHooks =
  MutBuf.empty[() => Unit]

private[bearlyb] def addShutdownHook(hook: => Unit): Unit =
  shutdownHooks += (() => hook)

def quit(): Unit =
  for hook <- shutdownHooks do
    hook()
  SDL_Quit()
