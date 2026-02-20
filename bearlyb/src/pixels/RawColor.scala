package bearlyb.pixels

opaque type RawColor = Int

object RawColor:
  def fromInt(inner: Int): RawColor = inner
  private[bearlyb] def apply(inner: Int): RawColor = inner

  extension (color: RawColor)
    def toInt: Int = color
    private[bearlyb] def internal: Int = color
