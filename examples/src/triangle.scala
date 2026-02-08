import bearlyb as bl, bl.{Event, Init, Keycode, Rect}
import bl.render.{Vertex, VertexBuffer}
import scala.util.Using

@main
def triangle(): Unit =
  bl.init(Init.Video)

  val (w, h) = (800, 600)
  val (window, renderer) = bearlyb
    .createWindowAndRenderer("hello triangle!", w, h)

  Using(
    VertexBuffer[Double](
      Vertex(
        pos = (0.25 * w, 0.75 * h),
        color = (1f, 0f, 0f, 1f),
        texCoord = (0, 0)
      ),
      Vertex(
        pos = (0.50 * w, 0.25 * h),
        color = (0f, 1f, 0f, 1f),
        texCoord = (0, 0)
      ),
      Vertex(
        pos = (0.75 * w, 0.75 * h),
        color = (0f, 0f, 1f, 1f),
        texCoord = (0, 0)
      )
    )
  ) { verts =>

    var running = true
    while running do
      Event
        .pollEvents()
        .foreach:
          case Event.Quit(_) =>
            println("quitting")
            running = false
          case other => println(other)

      renderer.drawColor = (0, 0, 0, 255)
      renderer.clear()
      renderer.renderGeometry(verts)
      renderer.present()
    end while

  }
  bl.quit()
end triangle
