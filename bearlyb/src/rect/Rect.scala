package bearlyb.rect

import bearlyb.vectors.Vec
import org.lwjgl.sdl.*
import org.lwjgl.system.MemoryStack

import scala.annotation.targetName
import scala.util.boundary
import scala.util.boundary.Label

import boundary.break
import Numeric.Implicits as numext
import Ordering.Implicits as ordext
import Vec.swizzleExtensions.xyxy
import Vec.*

case class Rect[T](x: T, y: T, w: T, h: T):
  lazy val pos = (x, y)
  lazy val dim = (w, h)

object Rect:
  import EnclosepointsEpsilon.value

  inline val CodeBottom = 0b0001
  inline val CodeTop = 0b0010
  inline val CodeLeft = 0b0100
  inline val CodeRight = 0b1000

  def apply[T](pos: Point[T], dim: Point[T]): Rect[T] =
    Rect(pos.x, pos.y, dim.x, dim.y)

  def empty[T: Numeric as num] =
    new Rect(num.zero, num.zero, num.zero, num.zero)

  def enclosePoints[T: {Numeric as num, EnclosepointsEpsilon as encloseEps}](
      points: IterableOnce[Point[T]],
      clip: Rect[T] | Null = null
  ): Rect[T] = boundary:
    import numext.infixNumericOps, ordext.infixOrderingOps

    val eps = encloseEps.value

    val pointIter =
      val iter = points.iterator
      if !iter.hasNext then
        throw IllegalArgumentException("points cannot be empty")
      clip match
        case clip: Rect[T] if !clip.isEmpty =>
          lazy val clipMinx = clip.x
          lazy val clipMiny = clip.y
          lazy val clipMaxx = clip.xmax - eps
          lazy val clipMaxy = clip.ymax - eps
          iter.filter((x, y) =>
            x < clipMinx || x > clipMaxx || y < clipMiny || y > clipMaxy
          )
        case clip: Rect[T] => break(clip)
        case null          => iter
      end match
    end pointIter

    var (minx, miny, maxx, maxy) =
      val p = pointIter.next()
      p.xyxy

    for (x, y) <- pointIter do
      if x < minx then minx = x else if x > maxx then maxx = x

      if y < miny then miny = y else if y > maxy then maxy = y

    Rect(minx, miny, (maxx - minx) + eps, (maxy - miny) + eps)

  def enclosePoints[T: {Numeric, EnclosepointsEpsilon}](
      points: Point[T]*
  ): Rect[T] = enclosePoints(points)

  def enclosePoints[T: {Numeric, EnclosepointsEpsilon}](
      clip: Rect[T],
      points: Point[T]*
  ): Rect[T] = enclosePoints(points, clip)

  private[bearlyb] def fromInternal(rect: SDL_Rect): Rect[Int] =
    new Rect(rect.x(), rect.y(), rect.w(), rect.h())

  private[bearlyb] def fromInternal(rect: SDL_FRect): Rect[Float] =
    new Rect(rect.x(), rect.y(), rect.w(), rect.h())

  extension [T: Numeric as num](self: Rect[T] | Null)

    @targetName("nullableToFloatRect")
    private[bearlyb] def toFloatRect: Rect[Float] | Null = self match
      case null       => null
      case r: Rect[T] => r.toFloatRect

    private[bearlyb] def floatInternal(stack: MemoryStack) = self.toFloatRect
      .internal(stack)

  end extension

  extension [T: Numeric as num](self: Rect[T])
    def xmax = num.plus(self.x, self.w)
    def ymax = num.plus(self.y, self.h)

    def isEmpty: Boolean =
      import ordext.infixOrderingOps
      (self.w <= num.zero) || (self.h <= num.zero)

    // convert to different ints
    def toFloatRect: Rect[Float] =
      import numext.infixNumericOps
      val Rect(x, y, w, h) = self
      Rect(x.toFloat, y.toFloat, w.toFloat, h.toFloat)

    def toDoubleRect: Rect[Double] =
      import numext.infixNumericOps
      val Rect(x, y, w, h) = self
      Rect(x.toDouble, y.toDouble, w.toDouble, h.toDouble)

    def toIntRect: Rect[Int] =
      import numext.infixNumericOps
      val Rect(x, y, w, h) = self
      Rect(x.toInt, y.toInt, w.toInt, h.toInt)

    def toLongRect: Rect[Long] =
      import numext.infixNumericOps
      val Rect(x, y, w, h) = self
      Rect(x.toLong, y.toLong, w.toLong, h.toLong)

    def equalsEpsilon(other: Rect[T], epsilon: T): Boolean =
      import numext.infixNumericOps, ordext.infixOrderingOps
      self == other ||
      ((self.x - other.x).abs <= epsilon && (self.y - other.y).abs <= epsilon &&
        (self.w - other.w).abs <= epsilon && (self.h - other.h).abs <= epsilon)

    def ~==(other: Rect[T])(using epsilon: CompareEpsilon[T]): Boolean = self
      .equalsEpsilon(other, epsilon)

    private inline def div(a: T, b: T): T = inline num match
      case frac: Fractional[T] => frac.div(a, b)
      case inte: Integral[T]   => inte.quot(a, b)

    def contains(p: Point[T])(using encloseEps: EnclosepointsEpsilon[T]): Boolean =
      import numext.infixNumericOps, ordext.infixOrderingOps
      lazy val eps = encloseEps.value
      self.x <= p.x && p.x <= (self.xmax-eps)
      && self.y <= p.y && p.y <= (self.ymax-eps)

    def contains(pts: Point[T]*)(using EnclosepointsEpsilon[T]): Boolean =
      pts.forall(contains)

    def hasIntersection(other: Rect[T])(using
        EnclosepointsEpsilon[T]
    ): Boolean = !self.intersection(other).isEmpty

    def intersection(other: Rect[T])(using
        encloseEps: EnclosepointsEpsilon[T]
    ): Rect[T] =
      import numext.infixNumericOps, ordext.infixOrderingOps
      boundary:
        if self.isEmpty || other.isEmpty then break(Rect.empty)

        lazy val eps = encloseEps.value

        lazy val x = self.x max other.x
        lazy val xmax = self.xmax min other.xmax
        lazy val w = xmax - x
        if xmax - eps < x then break(Rect.empty)

        lazy val y = self.y max other.y
        lazy val ymax = self.ymax min other.ymax
        lazy val h = ymax - y
        if ymax - eps < y then break(Rect.empty)

        Rect(x, y, w, h)
    end intersection

    def union(other: Rect[T]): Rect[T] =
      import numext.infixNumericOps, ordext.infixOrderingOps

      (self.isEmpty, other.isEmpty) match
        case (true, true)   => Rect.empty
        case (true, false)  => other
        case (false, true)  => self
        case (false, false) =>
          // horizontal union
          val x = self.x min other.x
          val xmax = self.xmax max other.xmax
          val w = xmax - x

          // vertical union
          val y = self.y min other.y
          val ymax = self.ymax max other.ymax
          val h = ymax - y

          Rect(x, y, w, h)
      end match
    end union

  end extension

  extension [T: ([t] =>> Fractional[t] | Integral[t]) as num](a: T)

    inline def /(b: T): T = inline num match
      case frac: Fractional[T] => frac.div(a, b)
      case inte: Integral[T]   => inte.quot(a, b)

  extension [
      T: {([t] =>> Fractional[t] | Integral[t]) as num,
        EnclosepointsEpsilon as encloseEps}
  ](
      self: Rect[T]
  )
    def intersection(
        x1: T,
        y1: T,
        x2: T,
        y2: T
    ): Option[(near: Point[T], far: Point[T])] =
      import numext.infixNumericOps, ordext.infixOrderingOps

      val eps = encloseEps.value

      def div(a: T, b: T): T = num match
        case frac: Fractional[T] => frac.div(a, b)
        case inte: Integral[T]   => inte.quot(a, b)

      def computeOutcode(x: T, y: T): Int =
        var code = 0

        if y < self.y then code |= CodeTop
        else if y > (self.y + self.h - eps) then code |= CodeBottom

        if x < self.x then code |= CodeLeft
        else if x > (self.x + self.w - eps) then code |= CodeRight

        code
      end computeOutcode

      inline def breakPoints(
          x1: T,
          y1: T,
          x2: T,
          y2: T
      )(using Label[Option[(near: Point[T], far: Point[T])]]) = break(
        Some((x1, y1), (x2, y2))
      )

      boundary:
        if self.isEmpty then break(None)

        val (rectx1, recty1) = (self.x, self.y)
        val rectx2 = self.x + self.w - eps
        val recty2 = self.y + self.h - eps

        // Check to see if entire line is inside rect
        if x1 >= rectx1 && x1 <= rectx2 && x2 >= rectx1 && x2 <= rectx2 &&
          y1 >= recty1 && y1 <= recty2 && y2 >= recty1 && y2 <= recty2
        then breakPoints(x1, y1, x2, y2)

        // check to see if entire line is to one side of rect
        if (x1 < rectx1 && x2 < rectx1) ||
          (x1 > rectx2 && x2 > rectx2) ||
          (y1 < recty1 && y2 < recty1) ||
          (y1 > recty2 && y2 > recty2)
        then break(None)

        if y1 == y2 then // horizontal line, easy to clip
          val x1New =
            if x1 < rectx1 then rectx1 else if x1 > rectx2 then rectx2 else x1
          val x2New =
            if x2 < rectx1 then rectx1 else if x2 > rectx2 then rectx2 else x2
          breakPoints(x1New, y1, x2New, y2)

        if x1 == x2 then // vertical line, easy to clip
          val y1New =
            if y1 < recty1 then recty1 else if y1 > recty2 then recty2 else y1
          val y2New =
            if y2 < recty1 then recty1 else if y2 > recty2 then recty2 else y2
          breakPoints(x1, y1New, x2, y2New)

        // More complicated Cohen-Sutherland algorithm
        var (x1tmp, y1tmp, x2tmp, y2tmp) = (x1, y1, x2, y2)
        var (x: T, y: T) = (num.zero, num.zero)
        var outcode1 = computeOutcode(x1, y1)
        var outcode2 = computeOutcode(x2, y2)
        while (outcode1 != 0) || (outcode2 != 0) do
          if (outcode1 & outcode2) != 0 then break(None)

          if outcode1 != 0 then
            if (outcode1 & CodeTop) != 0 then
              y = recty1
              x = (x1tmp + div((x2tmp - x1tmp) * (y - y1tmp), y2tmp - y1tmp))
            else if (outcode1 & CodeBottom) != 0 then
              y = recty2
              x = (x1tmp + div((x2tmp - x1tmp) * (y - y1tmp), y2tmp - y1tmp))
            else if (outcode1 & CodeLeft) != 0 then
              x = rectx1
              y = (y1tmp + div((y2tmp - y1tmp) * (x - x1tmp), x2tmp - x1tmp))
            else if (outcode1 & CodeRight) != 0 then
              x = rectx2
              y = (y1tmp + div((y2tmp - y1tmp) * (x - x1tmp), x2tmp - x1tmp))
            end if

            x1tmp = x
            y1tmp = y
            outcode1 = computeOutcode(x, y)
          else
            if (outcode2 & CodeTop) != 0 then
              assert(y2tmp != y1tmp)
              y = recty1
              x = (x1 + div((x2tmp - x1tmp) * (y - y1tmp), y2tmp - y1tmp))
            else if (outcode2 & CodeBottom) != 0 then
              assert(y2tmp != y1tmp)
              y = recty2
              x = (x1tmp + div((x2tmp - x1tmp) * (y - y1tmp), y2tmp - y1tmp))
            else if (outcode2 & CodeLeft) != 0 then
              assert(x2tmp != x1tmp)
              x = rectx1
              y = (y1tmp + div((y2tmp - y1tmp) * (x - x1tmp), x2tmp - x1tmp))
            else if (outcode2 & CodeRight) != 0 then
              assert(x2tmp != x1tmp)
              x = rectx2
              y = (y1tmp + div((y2tmp - y1tmp) * (x - x1tmp), x2tmp - x1tmp))
            end if

            x2tmp = x
            y2tmp = y
            outcode2 = computeOutcode(x, y)
          end if
        end while

        breakPoints(x1tmp, y1tmp, x2tmp, y2tmp)

    end intersection

  end extension

  sealed private[bearlyb] trait InternalOps[T]:
    type Internal

    extension (r: Rect[T] | Null)
      def internal(stack: MemoryStack): Internal | Null

  given InternalOps[Int]:
    type Internal = SDL_Rect

    extension (r: Rect[Int] | Null)

      def internal(stack: MemoryStack): Internal | Null = r match
        case null             => null
        case Rect(x, y, w, h) => SDL_Rect.malloc(stack).set(x, y, w, h)

  end given

  given InternalOps[Float]:
    type Internal = SDL_FRect

    extension (r: Rect[Float] | Null)

      def internal(stack: MemoryStack): Internal | Null = r match
        case null             => null
        case Rect(x, y, w, h) => SDL_FRect.malloc(stack).set(x, y, w, h)

  end given

  opaque type CompareEpsilon[T] = T

  object CompareEpsilon:
    extension [T](ep: CompareEpsilon[T]) def value: T = ep

    def apply[T](value: T): CompareEpsilon[T] = value
    def unapply[T](wrapped: CompareEpsilon[T]): T = wrapped

    given CompareEpsilon[Float] = 1.1920928955078125e-07f
    given CompareEpsilon[Double] = 1.1920928955078125e-07d

  end CompareEpsilon

  opaque type EnclosepointsEpsilon[T] = T

  object EnclosepointsEpsilon:

    extension [T](ep: EnclosepointsEpsilon[T]) def value: T = ep

    def apply[T](value: T): EnclosepointsEpsilon[T] = value

    inline given [T: Integral as inte] => EnclosepointsEpsilon[T] = inte.one

    inline given [T: Fractional as frac] => EnclosepointsEpsilon[T] = frac.zero

  end EnclosepointsEpsilon

end Rect
