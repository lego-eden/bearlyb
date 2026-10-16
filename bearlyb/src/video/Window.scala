package bearlyb.video

import bearlyb.*
import bearlyb.surface.Surface
import bearlyb.util.*
import org.lwjgl.{sdl, system}

import scala.util.Using

import sdl.SDLVideo.*
import sdl.SDLProperties.*
import system.MemoryStack.stackPush
import render.Renderer

class Window private[bearlyb] (
    private[bearlyb] val internal: Long,
    private var windowShape: Option[Surface] = None
):
  def title: String = SDL_GetWindowTitle(internal)

  def title_=(title: String): Unit = SDL_SetWindowTitle(internal, title)
    .sdlErrorCheck()

  def position: (x: Int, y: Int) = Using(stackPush()): stack =>
    val px = stack.mallocInt(1)
    val py = stack.mallocInt(1)
    SDL_GetWindowPosition(internal, px, py).sdlErrorCheck()
    (px.get(0), py.get(0))
  .get

  def position_=(pos: (x: Int, y: Int)): Unit =
    val (width, height) = pos
    SDL_SetWindowPosition(internal, width, height).sdlErrorCheck()

  def size: (width: Int, height: Int) =  withStack:
    val (pWidth, pHeight) = mallocManyInt(2, stack)
    SDL_GetWindowSize(internal, pWidth, pHeight)
      .sdlErrorCheck((pWidth.get(0), pHeight.get(0)))


  def size_=(dim: (width: Int, height: Int)): Unit =
    val (width, height) = dim
    SDL_SetWindowSize(internal, width, height).sdlErrorCheck()

  def minimumSize: (width: Int, height: Int) = withStack:
    val (pWidth, pHeight) = mallocManyInt(2, stack)
    SDL_GetWindowMinimumSize(internal, pWidth, pHeight)
      .sdlErrorCheck((pWidth.get(0), pHeight.get(0)))

  def minimumSize_=(dim: (width: Int, height: Int)): Unit =
    val (width, height) = dim
    SDL_SetWindowMinimumSize(internal, width, height).sdlErrorCheck()

  def maximumSize: (width: Int, height: Int) = withStack:
    val (pWidth, pHeight) = mallocManyInt(2, stack)
    SDL_GetWindowMaximumSize(internal, pWidth, pHeight)
      .sdlErrorCheck((pWidth.get(0), pHeight.get(0)))

  def maximumSize_=(dim: (width: Int, height: Int)): Unit =
    val (width, height) = dim
    SDL_SetWindowMaximumSize(internal, width, height).sdlErrorCheck()

  def sizeInPixels: (width: Int, height: Int) = Using(stackPush()): stack =>
    val pWidth = stack.mallocInt(1)
    val pHeight = stack.mallocInt(1)
    SDL_GetWindowSizeInPixels(internal, pWidth, pHeight)
      .sdlErrorCheck((pWidth.get(0), pHeight.get(0)))
  .get

  def id: WindowID = SDL_GetWindowID(internal) match
    case 0  => sdlError()
    case id => id

  def icon_=(icon: Surface): Unit = SDL_SetWindowIcon(internal, icon.internal)
    .sdlErrorCheck()

  def bordered: Boolean =
    (SDL_GetWindowFlags(internal) & SDL_WINDOW_BORDERLESS) == 0

  def bordered_=(bordered: Boolean): Unit =
    SDL_SetWindowBordered(internal, bordered).sdlErrorCheck()

  def resizable: Boolean =
    (SDL_GetWindowFlags(internal) & SDL_WINDOW_RESIZABLE) == 0

  def resizable_=(resizable: Boolean): Unit =
    SDL_SetWindowResizable(internal, resizable).sdlErrorCheck()

  def alwaysOnTop: Boolean =
    (SDL_GetWindowFlags(internal) & SDL_WINDOW_ALWAYS_ON_TOP) == 0

  def alwaysOnTop_=(alwaysOnTop: Boolean): Unit =
    SDL_SetWindowAlwaysOnTop(internal, alwaysOnTop).sdlErrorCheck()

  def fullscreenMode: Option[DisplayMode] =
    Option(SDL_GetWindowFullscreenMode(internal)).map(DisplayMode.fromInternal)

  def fullscreenMode_=(mode: DisplayMode): Unit =
    SDL_SetWindowFullscreenMode(internal, mode.internal).sdlErrorCheck()

  def fullscreen: Boolean =
    (SDL_GetWindowFlags(internal) & SDL_WINDOW_FULLSCREEN) == 0

  def fullscreen_=(fullscreen: Boolean): Unit =
    SDL_SetWindowFullscreen(internal, fullscreen).sdlErrorCheck()

  def popup(
      xOffset: Int,
      yOffset: Int,
      width: Int,
      height: Int,
      flags: Window.Flag*
  ): Window = new Window(
    SDL_CreatePopupWindow(
      internal,
      xOffset,
      yOffset,
      width,
      height,
      SDL_WINDOW_POPUP_MENU | flags.combine
    ).sdlCreationCheck()
  )

  def tooltip(
      xOffset: Int,
      yOffset: Int,
      width: Int,
      height: Int,
      flags: Window.Flag*
  ): Window = new Window(
    SDL_CreatePopupWindow(
      internal,
      xOffset,
      yOffset,
      width,
      height,
      SDL_WINDOW_TOOLTIP | flags.combine
    )
  )

  def renderer: Renderer = Renderer(this)

  def surface: Surface = Surface.fromInternal(SDL_GetWindowSurface(internal))

  def presentSurface(): Unit = SDL_UpdateWindowSurface(internal).sdlErrorCheck()

  def destroySurface(): Unit = SDL_DestroyWindowSurface(internal)
    .sdlErrorCheck()

  def destroy(): Unit =
    SDL_DestroyWindow(internal)
    windowShape match
      case Some(surf) => surf.destroy()
      case None       => ()

  def aspectRatio: (minAspect: Float, maxAspect: Float) =
    Using(stackPush()): stack =>
      val minAspect = stack.mallocFloat(1)
      val maxAspect = stack.mallocFloat(1)
      SDL_GetWindowAspectRatio(internal, minAspect, maxAspect).sdlErrorCheck()
      (minAspect.get(0), maxAspect.get(0))
    .get

  def aspectRatio_=(aspectRatios: (minAspect: Float, maxAspect: Float)): Unit =
    val (minAspect, maxAspect) = aspectRatios
    SDL_SetWindowAspectRatio(internal, minAspect, maxAspect).sdlErrorCheck()

  def flash(operation: FlashOperation): Unit =
    SDL_FlashWindow(internal, operation.internal).sdlErrorCheck()

  def getDisplay: DisplayID = SDL_GetDisplayForWindow(internal)

  def vsync: Int = Using(stackPush()): stack =>
    val psync = stack.mallocInt(1)
    SDL_GetWindowSurfaceVSync(internal, psync).sdlErrorCheck(psync.get(0))
  .get

  def vsync_=(syncInterval: Int): Unit =
    SDL_SetWindowSurfaceVSync(internal, syncInterval).sdlErrorCheck()

  def shape: Option[Surface] = this.windowShape

  def shape_=(mask: Option[Surface]): Unit =
    SDL_SetWindowShape(
      internal,
      mask match
        case Some(value) => value.internal
        case None        => null
    ).sdlErrorCheck()
    windowShape = mask

  end shape_=

  def opacity: Float = SDL_GetWindowOpacity(internal) match
    case -1.0f => sdlError()
    case o     => o

  def opacity_=(opacity: Float): Unit = SDL_SetWindowOpacity(internal, opacity)
    .sdlErrorCheck()

  def mouseGrabbed: Boolean = SDL_GetWindowMouseGrab(internal)

  def mouseGrabbed_=(grabbed: Boolean): Unit =
    SDL_SetWindowMouseGrab(internal, grabbed).sdlErrorCheck()

  def keyboardGrabbed: Boolean = SDL_GetWindowKeyboardGrab(internal)

  def keyboardGrabbed_=(grabbed: Boolean): Unit =
    SDL_SetWindowKeyboardGrab(internal, grabbed).sdlErrorCheck()

  def flags: Set[Window.Flag] =
    val flags = SDL_GetWindowFlags(internal)
    Set.from(
      Window.Flag.values.iterator.filter(flag => (flag.internal & flags) != 0)
    )

  def show(): Unit = SDL_ShowWindow(internal).sdlErrorCheck()
  def hide(): Unit = SDL_HideWindow(internal).sdlErrorCheck()

  def raise(): Unit = SDL_RaiseWindow(internal).sdlErrorCheck()

  def maximize(): Unit = SDL_MaximizeWindow(internal).sdlErrorCheck()

  def minimize(): Unit = SDL_MinimizeWindow(internal).sdlErrorCheck()

  def restore(): Unit = SDL_RestoreWindow(internal).sdlErrorCheck()

  def sync(): Unit = SDL_SyncWindow(internal).sdlErrorCheck()

end Window

object Window:

  def apply(title: String, width: Int, height: Int, flags: Flag*): Window =
    new Window(
      SDL_CreateWindow(title, width, height, flags.combine).sdlCreationCheck()
    )

  def withProperties(props: IterableOnce[Property]): Window =
    val id = SDL_CreateProperties() match
      case 0  => sdlError()
      case id => id

    for prop <- props.iterator do
      prop.value match
        case bool: Boolean => SDL_SetBooleanProperty(id, prop.name, bool)
        case str: String   => SDL_SetStringProperty(id, prop.name, str)
        case nbr: Long     => SDL_SetNumberProperty(id, prop.name, nbr)
        case flt: Float    => SDL_SetFloatProperty(id, prop.name, flt)
        case win: Window => SDL_SetPointerProperty(id, prop.name, win.internal)

    new Window(SDL_CreateWindowWithProperties(id).sdlCreationCheck())

  end withProperties

  def fromID(id: WindowID): Window =
    val pWindow = SDL_GetWindowFromID(id).sdlCreationCheck()
    new Window(pWindow)

  final val VSyncDisabled = SDL_WINDOW_SURFACE_VSYNC_DISABLED

  final val VSyncAdaptive = SDL_WINDOW_SURFACE_VSYNC_ADAPTIVE

  enum Flag(private[bearlyb] val internal: Long):
    /** window is in fullscreen mode */
    case Fullscreen extends Flag(0x0000000000000001)

    /** window usable with OpenGL context */
    case OpenGl extends Flag(0x0000000000000002)

    /** window is occluded */
    case Occluded extends Flag(0x0000000000000004)

    /** window is neither mapped onto the desktop nor shown in the
      * taskbar/dock/window list; SDL_ShowWindow() is required for it to become
      * visible
      */
    case Hidden extends Flag(0x0000000000000008)

    /** no window decoration */
    case Borderless extends Flag(0x0000000000000010)

    /** window can be resized */
    case Resizable extends Flag(0x0000000000000020)

    /** window is minimized */
    case Minimized extends Flag(0x0000000000000040)

    /** window is maximized */
    case Maximized extends Flag(0x0000000000000080)

    /** window has grabbed mouse input */
    case MouseGrabbed extends Flag(0x0000000000000100)

    /** window has input focus */
    case InputFocus extends Flag(0x0000000000000200)

    /** window has mouse focus */
    case MouseFocus extends Flag(0x0000000000000400)

    /** window not created by SDL */
    case External extends Flag(0x0000000000000800)

    /** window is modal */
    case Modal extends Flag(0x0000000000001000)

    /** window uses high pixel density back buffer if possible */
    case HighPixelDensity extends Flag(0x0000000000002000)

    /** window has mouse captured (unrelated to MOUSE_GRABBED) */
    case MouseCapture extends Flag(0x0000000000004000)

    /** window has relative mode enabled */
    case MouseRelativeMode extends Flag(0x0000000000008000)

    /** window should always be above others */
    case AlwaysOnTop extends Flag(0x0000000000010000)

    /** window should be treated as a utility window, not showing in the task
      * bar and window list
      */
    case Utility extends Flag(0x0000000000020000)
    /* /** window should be treated as a tooltip and does not get mouse or
     * keyboard focus, requires a parent window */ */
    case Tooltip extends Flag(0x0000000000040000)
    /* /** window should be treated as a popup menu,
     * requires a parent window */ */
    case PopupMenu extends Flag(0x0000000000080000)

    /** window has grabbed keyboard input */
    case KeyboardGrabbed extends Flag(0x0000000000100000)

    /** window usable for Vulkan surface */
    case Vulkan extends Flag(0x0000000010000000)

    /** window usable for Metal view */
    case Metal extends Flag(0x0000000020000000)

    /** window with transparent buffer */
    case Transparent extends Flag(0x0000000040000000)

    /** window should not be focusable */
    case NotFocusable extends Flag(0x0000000080000000)

  end Flag

  object Flag:

    extension (flags: IterableOnce[Flag])

      private[bearlyb] def combine: Long = flags.iterator
        .foldLeft(0L)(_ | _.internal)

    extension (internal: Long)

      private[bearlyb] def fromInternal: Flag = Flag.values
        .find(_.internal == internal)
        .get

  end Flag

  enum Property(val name: String):
    def value: String | Long | Float | Boolean | Window

    /** : true if the window should be always on top */
    case AlwaysOnTop(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_ALWAYS_ON_TOP_BOOLEAN)

    /** : true if the window has no window decoration */
    case BorderlessBoolean(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_BORDERLESS_BOOLEAN)

    /** : true if the "tooltip" and "menu" window types should be automatically
      * constrained to be entirely within display bounds (default), false if no
      * constraints on the position are desired.
      */
    /* case ConstrainPopup(value: Boolean) extends
     * Property(SDL_PROP_WINDOW_CREATE_CONSTRAIN_POPUP_BOOLEAN) // TODO: This
     * seems to be missing from lwjgl? */
    /** : true if the window will be used with an externally managed graphics
      * context.
      */
    case ExternalGraphicsContext(value: Boolean)
        extends Property(
          SDL_PROP_WINDOW_CREATE_EXTERNAL_GRAPHICS_CONTEXT_BOOLEAN
        )

    /** : true if the window should accept keyboard input (defaults true) */
    case Focusable(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_FOCUSABLE_BOOLEAN)

    /** : true if the window should start in fullscreen mode at desktop
      * resolution
      */
    case Fullscreen(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_FULLSCREEN_BOOLEAN)

    /** : the height of the window */
    case Height(value: Long)
        extends Property(SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER)

    /** : true if the window should start hidden */
    case Hidden(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN)

    /** : true if the window uses a high pixel density buffer if possible */
    case HighPixelDensity(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_HIGH_PIXEL_DENSITY_BOOLEAN)

    /** : true if the window should start maximized */
    case Maximized(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_MAXIMIZED_BOOLEAN)

    /** : true if the window is a popup menu */
    case PopupMenu(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_MENU_BOOLEAN)

    /** : true if the window will be used with Metal rendering */
    case Metal(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_METAL_BOOLEAN)

    /** : true if the window should start minimized */
    case Minimized(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_MINIMIZED_BOOLEAN)

    /** : true if the window is modal to its parent */
    case Modal(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_MODAL_BOOLEAN)

    /** : true if the window starts with grabbed mouse focus */
    case MouseGrabbed(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_MOUSE_GRABBED_BOOLEAN)

    /** : true if the window will be used with OpenGL rendering */
    case OpenGL(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_OPENGL_BOOLEAN)

    /** : an SDL_Window that will be the parent of this window, required for
      * windows with the "tooltip", "menu", and "modal" properties
      */
    case Parent(value: Window)
        extends Property(SDL_PROP_WINDOW_CREATE_PARENT_POINTER)

    /** : true if the window should be resizable */
    case Resizable(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_RESIZABLE_BOOLEAN)

    /** : the title of the window, in UTF-8 encoding */
    case Title(value: String)
        extends Property(SDL_PROP_WINDOW_CREATE_TITLE_STRING)

    /** : true if the window show transparent in the areas with alpha of 0 */
    case Transparent(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_TRANSPARENT_BOOLEAN)

    /** : true if the window is a tooltip */
    case Tooltip(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_TOOLTIP_BOOLEAN)

    /** : true if the window is a utility window, not showing in the task bar
      * and window list
      */
    case Utility(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_UTILITY_BOOLEAN)

    /** : true if the window will be used with Vulkan rendering */
    case Vulkan(value: Boolean)
        extends Property(SDL_PROP_WINDOW_CREATE_VULKAN_BOOLEAN)

    /** : the width of the window */
    case Width(value: Long)
        extends Property(SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER)

    /** : the x position of the window, or SDL_WINDOWPOS_CENTERED, defaults to
      * SDL_WINDOWPOS_UNDEFINED. This is relative to the parent for windows with
      * the "tooltip" or "menu" property set.
      */
    case X(value: Long) extends Property(SDL_PROP_WINDOW_CREATE_X_NUMBER)

    /** : the y position of the window, or SDL_WINDOWPOS_CENTERED, defaults to
      * SDL_WINDOWPOS_UNDEFINED. This is relative to the parent for windows with
      * the "tooltip" or "menu" property set.
      */
    case Y(value: Long) extends Property(SDL_PROP_WINDOW_CREATE_Y_NUMBER)

  end Property

end Window
