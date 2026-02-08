import bearlyb as bl, bl.*
import bl.scancode.Scancode
import bl.pixels.Color

import scala.util.boundary, boundary.break
import scala.collection.mutable.HashSet as MutSet
import java.time.Duration
import scala.util.Random

object Colors:
  val clear = (0, 0, 0, 0)
  val square = (255, 255, 255, 255)

val keyPressed = MutSet.empty[Scancode]
val fps = 60L
val nanosPerFrame = 1_000_000_000L / fps

object Square:
  extension (a: Boolean)
    def -(b: Boolean): Int = (a, b) match
      case (true, true) | (false, false) => 0
      case (true, false)                 => 1
      case (false, true)                 => -1

  object Key:
    val (left, right, up, down) = (
      Scancode.A,
      Scancode.D,
      Scancode.W,
      Scancode.S
    )

  val (w, h) = (50.0, 50.0)
  var (x, y) = (400.0, 400.0)
  val speed = 300.0

  def draw(r: Renderer): Unit =
    r.drawColor = Colors.square
    r.fillRect(
      Rect(x - w / 2, y - h / 2, w, h)
    )

  def update(dt: Double): Unit =
    x += dt * speed * (keyPressed(Key.right) - keyPressed(Key.left))
    y += dt * speed * (keyPressed(Key.down) - keyPressed(Key.up))
    Thread.sleep(Duration.ofNanos(Random.between(0L, 2000000L)))

@main
def movingSquare(): Unit =
  bl.init(Init.Video)
  try
    boundary:
      val (w, r) =
        bl.createWindowAndRenderer("Move Around!", 800, 800)

      var past = System.nanoTime()
      var dt = 0.0
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

        Square.update(dt)

        r.drawColor = Colors.clear
        r.clear()

        Square.draw(r)

        r.present()

        val now = System.nanoTime()
        dt = (now - past).toDouble / 1e9
        past = now
      end while

  finally
    bl.quit()
