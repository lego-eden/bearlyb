// ---------------- MSDF + HarfBuzz pipeline ----------------
import bearlyb as bl, bl.{Event, Init, Keycode, Rect}
import bearlyb.vectors.given
import org.lwjgl.util.freetype.*
import org.lwjgl.util.harfbuzz.*
import org.lwjgl.BufferUtils
import bl.render.Renderer
import bl.pixels.Color

import java.nio.file.{Files, Paths}
import java.nio.ByteBuffer
import scala.collection.mutable
import java.awt.image.BufferedImage
import bl.vectors.Vec
import bl.pixels.PixelFormat
import bl.render.TextureAccess
import bl.pixels.RawColor
import scala.util.Using
import bl.render.Font

@main
def textTest(): Unit =
  bl.init(Init.Video)

  val libraryBuffPtr = bl.initialize.initFontRenderer()

  val font = Font(libraryPtr = libraryBuffPtr, dpi = 64)

  val (window, renderer) =
    bearlyb.createWindowAndRenderer("Hello MSDF!", 960, 540)

  var running = true
  while running do
    Event.pollEvents().foreach {
      case Event.Quit(_) | Event.Key.Down(key = Keycode.Escape) =>
        println("Quitting")
        running = false
      case _ =>
    }

    renderer.drawColor = (255, 255, 255, 255)
    renderer.clear()

    renderer.drawColor = (0, 0, 0, 255)
    renderer.renderText(
      renderer,
      font,
      "Hello, World <- - ->",
      25,
      100,
      79
    )

    renderer.present()
  end while

  font.destroy

  bl.initialize.deInitFontRenderer(font.libraryBuffPtr)
  bl.quit()
end textTest
