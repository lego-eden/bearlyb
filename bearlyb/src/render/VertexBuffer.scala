package bearlyb.render

import org.lwjgl.sdl.SDL_Vertex
import scala.util.Using.Releasable
import scala.collection.IterableOnceOps

opaque type VertexBuffer = SDL_Vertex.Buffer
object VertexBuffer:
  def apply[T: Numeric](vs: Vertex[T]*): VertexBuffer = from(vs)
  def from[T: Numeric as num](vs: Seq[Vertex[T]]): VertexBuffer =
    val sdlVerts = SDL_Vertex.calloc(vs.size)
    for (v, i) <- vs.zipWithIndex do
      val Vertex((px, py), (r, g, b, a), (tx, ty)) = v
      val current = sdlVerts.get(i)
      current.position$.set(num.toFloat(px), num.toFloat(py))
      current.color.set(r, g, b, a)
      current.tex_coord.set(num.toFloat(tx), num.toFloat(ty))

    sdlVerts

  def fill[T: Numeric](len: Int)(v: Vertex[T]): VertexBuffer =
    from(Seq.fill(len)(v))

  def zeros[T: Numeric](len: Int): VertexBuffer =
    fill(len)(Vertex.zero)

  extension (buf: VertexBuffer)
    private[bearlyb] def internal: SDL_Vertex.Buffer = buf

    inline def toSeq: Seq[Vertex[Float]] = buf.toIndexedSeq

    def toIndexedSeq: IndexedSeq[Vertex[Float]] =
      IndexedSeq.tabulate(buf.capacity): i =>
        Vertex.fromInternal(buf.get(i))

    def destroy(): Unit = releasable.release(buf)

    def iterator: Iter[Vertex[Float]] =
      Iter(Iterator.range(0, buf.limit).map(i =>
        Vertex.fromInternal(buf.get(i))
      ), buf)

  extension (inline buf: VertexBuffer)
    inline def mapInPlace(inline f: Vertex[Float] => Vertex[Float]): VertexBuffer =
      for i <- 0 until buf.limit do
        val internalVert = buf.get(i)
        val newVert = f(Vertex.fromInternal(internalVert))
        newVert.internal(internalVert)
      buf

  given releasable: Releasable[VertexBuffer] with
    def release(buf: VertexBuffer): Unit = buf.free()

  class Iter[A] private[VertexBuffer] (it: Iterator[A], private val buf: VertexBuffer) extends IterableOnce[A], IterableOnceOps[A, Iter, Iter[A]]:
    private def withIt[B](it: Iterator[B]): Iter[B] = Iter(it, buf)
    def iterator: Iterator[A] = it
    def map[B](f: A => B): Iter[B] = withIt(it.map(f))
    def flatMap[B](f: A => IterableOnce[B]): Iter[B] = withIt(it.flatMap(f))
    def collect[B](pf: PartialFunction[A, B]): Iter[B] = withIt(it.collect(pf))
    def drop(n: Int): Iter[A] = withIt(it.drop(n))
    def dropWhile(p: A => Boolean): Iter[A] = withIt(it.dropWhile(p))
    def filter(p: A => Boolean): Iter[A] = withIt(it.filter(p))
    def filterNot(pred: A => Boolean): Iter[A] = withIt(it.filterNot(pred))
    def flatten[B](using asIterable: A => IterableOnce[B]): Iter[B] = withIt(it.flatten)
    def scanLeft[B](z: B)(op: (B, A) => B): Iter[B] = withIt(it.scanLeft(z)(op))
    def slice(from: Int, until: Int): Iter[A] = withIt(it.slice(from, until))
    def span(p: A => Boolean): (Iter[A], Iter[A]) =
      val (ok, notOk) = it.span(p)
      (withIt(ok), withIt(notOk))
    def take(n: Int): Iter[A] = withIt(it.take(n))
    def takeWhile(p: A => Boolean): Iter[A] = withIt(it.takeWhile(p))
    def tapEach[U](f: A => U): Iter[A] = withIt(it.tapEach(f))
    def zipWithIndex: Iter[(A, Int)] = withIt(it.zipWithIndex)
    def zip[B](other: IterableOnce[B]) = withIt(it.zip(other))

  object Iter:
    extension [T: Numeric as num](it: Iter[Vertex[T]])
      def consume(): Unit =
        for (v, i) <- it.zip(0 until it.buf.limit) do
          val Vertex((px, py), (r, g, b, a), (tx, ty)) = v
          val current = it.buf.get(i)
          current.position$.set(num.toFloat(px), num.toFloat(py))
          current.color.set(r, g, b, a)
          current.tex_coord.set(num.toFloat(tx), num.toFloat(ty))
  end Iter

end VertexBuffer
