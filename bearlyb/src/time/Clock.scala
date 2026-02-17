package bearlyb.time

import org.lwjgl.sdl.SDLTimer.*

class Clock():
  private var prev: Nanos = SDL_GetTicksNS()
  private var dt: Nanos = 0L
  private var rawpassed: Nanos = 0L

  /** @param targetFPS
    * @return
    *   the number of nanoseconds since the last frame
    */
  def tick(targetFPS: Long = 0): Long =
    if targetFPS > 0 then
      val endtime = ((1.0 / targetFPS) * 1e9).toLong
      rawpassed = SDL_GetTicksNS() - prev
      val delay = endtime - rawpassed

      SDL_DelayPrecise(delay)

    val now = SDL_GetTicksNS()
    dt = now - prev
    prev = now
    if targetFPS > 0 then rawpassed = dt

    dt

  end tick

  def deltaNanos: Nanos = dt
  def deltaDouble: Double = dt*1E-9

  def fps: Double =
    1.0 / deltaDouble

  override def toString: String =
    s"Clock(deltaDouble=$deltaDouble)"
end Clock
