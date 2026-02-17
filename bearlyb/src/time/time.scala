package bearlyb.time

import org.lwjgl.sdl.SDLTimer.*

type Millis = Long
type Nanos = Long

/** @return
  *   the number of milliseconds since the library was initialized
  */
def ticksMS: Long = SDL_GetTicks()

/** @return
  *   the number of nanoseconds since the library was initialized
  */
def ticksNS: Long = SDL_GetTicksNS()

/** Get the current value of the high resolution counter.
  *
  * This function is typically used for profiling. The counter values are only
  * meaningful relative to each other. Differences between values can be
  * converted to times by using SDL_GetPerformanceFrequency().
  *
  * @return
  *   the current counter value
  */
def performanceCounter: Long = SDL_GetPerformanceCounter()

/** Get the count per second of the high resolution counter.
  *
  * @return
  *   a platform-specific count per second
  */
def performanceFreq: Long = SDL_GetPerformanceFrequency()
