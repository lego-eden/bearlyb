import bearlyb as bl, bl.*
import bl.scancode.Scancode
import bl.pixels.Color

import scala.util.boundary, boundary.break
import scala.collection.mutable.HashSet as MutSet
import java.time.Duration
import scala.util.Random
import bl.time.Clock

val keyPressed = MutSet.empty[Scancode]

object Square:
  extension (a: Boolean)
    def -(b: Boolean): Int = (a, b) match
      case (true, true) | (false, false) => 0
      case (true, false)                 => 1
      case (false, true)                 => -1

  val (w, h) = (50.0, 50.0)
  var (x, y) = (400.0, 400.0)
  val speed = 300.0

  def draw(r: Renderer): Unit =
    r.drawColor = (255, 255, 255, 255)
    r.fillRect(
      Rect(x - w / 2, y - h / 2, w, h)
    )

  def update(dt: Double): Unit =
    x += dt * speed * (keyPressed(Scancode.D) - keyPressed(Scancode.A))
    y += dt * speed * (keyPressed(Scancode.S) - keyPressed(Scancode.W))

def mainLoop(r: Renderer)(using boundary.Label[Unit]): Unit =
  val clock = Clock()
  while true do
    Event
      .pollEvents()
      .foreach:
        case Event.Quit(_)                 => break()
        case Event.Key.Down(scancode = sc) =>
          keyPressed += sc
        case Event.Key.Up(scancode = sc) =>
          keyPressed -= sc
        case _ =>

    Square.update(clock.deltaDouble)

    r.drawColor = (0, 0, 0, 0)
    r.clear()

    Square.draw(r)

    r.drawColor = (255, 0, 0, 255)
    r.renderScale = (5f, 5f)
    r.renderDebugText(1f, 1f)(f"${clock.fps}%4.2f")
    r.renderScale = (1f, 1f)

    r.present()

    clock.tick(60)
end mainLoop

@main
def movingSquare(): Unit =
  bl.init(Init.Video)
  try
    boundary:
      val (w, r) =
        bl.createWindowAndRenderer("Move Around!", 800, 800)
      mainLoop(r)

  finally
    bl.quit()
