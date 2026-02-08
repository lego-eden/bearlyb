package bearlyb.render

import bearlyb.rect.Point, Point.*
import bearlyb.pixels.{FColor, Color}
import org.lwjgl.system.MemoryStack
import org.lwjgl.sdl.SDL_Vertex

case class Vertex[T](
    pos: Point[T],
    color: FColor,
    texCoord: Point[T]
)
object Vertex:
  extension [T: Numeric](v: Vertex[T])
    private[bearlyb] def internal(stack: MemoryStack): SDL_Vertex =
      val sdlv = SDL_Vertex.malloc(stack)
      val pos = v.pos.floatInternal(stack)
      val col = Color.internal(v.color)(stack)
      val texCoord = v.texCoord.floatInternal(stack)
      sdlv.set(pos, col, texCoord)
  end extension

  private[bearlyb] def fromInternal(sdlVert: SDL_Vertex): Vertex[Float] =
    val pos = sdlVert.position$
    val col = sdlVert.color
    val texCoord = sdlVert.tex_coord
    Vertex(
      (pos.x, pos.y),
      (col.r, col.g, col.b, col.a),
      (texCoord.x, texCoord.y)
    )
