// ---------------- Text Rendering Example ----------------
import bearlyb as bl, bl.{Event, Init, Keycode, Rect}
import bl.render.Font
import bl.video.BlendMode

@main
def textTest(): Unit =
  bl.init(Init.Video)

  val (window, renderer) =
    bearlyb.createWindowAndRenderer("Hello Text!", 800, 600)

  val textSize = 14f
  val dpi = 96
  val font = Font.default
    .withTextSize(textSize)
    .withDPI(dpi)
  // val font = Font.defaultBoldItalic
  // val font = Font.fromFile(os.resource/"fonts"/"PlaywriteDKUloopetGuides-Regular.ttf")

  renderer.drawBlendMode = BlendMode.Blend

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

    // val text = "Hello, World!"
    val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris quis arcu erat. In cursus blandit orci non euismod. Nam blandit a ipsum quis rutrum. Quisque pretium, sapien id lobortis fringilla, quam nulla faucibus felis, eget vestibulum neque lacus non dolor. Integer ut aliquam nunc. Phasellus odio elit, pulvinar vitae risus." 
    val (textWidth, textHeight) = font.measure(text)

    // center the text
    val (w, h) = window.size
    // val (x, y) = (w/2 - (textWidth/2), h/2 - (textHeight/2))
    val (x, y) = (10f, 10f)

    renderer.drawColor = (0, 0, 255, 255)
    renderer.drawRect(Rect(x, y, 300f, h.toFloat))

    renderer.drawColor = (0, 0, 0, 255)
    renderer.renderText(font, text, x, y, maxWidth = 300)

    renderer.present()
  end while

  font.destroy()

  bl.quit()
end textTest
