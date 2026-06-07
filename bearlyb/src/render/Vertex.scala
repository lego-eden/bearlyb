package bearlyb.render

import bearlyb.rect.Point, Point.*
import bearlyb.vectors.Vec, Vec.given
import bearlyb.pixels.{FColor, Color}

import org.lwjgl.system.MemoryStack
import org.lwjgl.sdl.SDL_Vertex

import scala.annotation.publicInBinary

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

  extension (v: Vertex[Float])
    @publicInBinary
    private[bearlyb] def internal(sdlVert: SDL_Vertex): Unit =
      val Vertex((x,y), (r,g,b,a), (tx,ty)) = v
      sdlVert.position$.set(x,y)
      sdlVert.color.set(r,g,b,a)
      sdlVert.tex_coord.set(tx, ty): Unit
  end extension

  @publicInBinary
  private[bearlyb] inline def fromInternal(inline sdlVert: SDL_Vertex): Vertex[Float] =
    val pos = sdlVert.position$
    val col = sdlVert.color
    val texCoord = sdlVert.tex_coord
    Vertex(
      (pos.x, pos.y),
      (col.r, col.g, col.b, col.a),
      (texCoord.x, texCoord.y)
    )

  def zero[T: Numeric as num]: Vertex[T] =
    val orig = Vec.fill(2)(num.zero)
    Vertex(orig, (0f,0f,0f,0f), orig)
