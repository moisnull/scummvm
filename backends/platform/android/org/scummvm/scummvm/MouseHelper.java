package org.scummvm.scummvm;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Contains helper methods for mouse/hover events that were introduced in Android 4.0.
 */
public class MouseHelper implements View.OnHoverListener {
	//private final View.OnHoverListener _listener;
	private final ScummVM _scummvm;
	private boolean _rmbPressed;
	private boolean _lmbPressed;
	private boolean _mmbPressed;
	private boolean _bmbPressed;
	private boolean _fmbPressed;
	private boolean _srmbPressed;
	private boolean _smmbPressed;

	//
	// Class initialization fails when this throws an exception.
	// Checking hover availability is done on static class initialization for Android 1.6 compatibility.
	//
	static {
		try {
			Class.forName("android.view.View$OnHoverListener");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Calling this forces class initialization
	 */
	public static void checkHoverAvailable() {}

	public MouseHelper(ScummVM scummvm) {
		_scummvm = scummvm;
		//_listener = createListener();
	}

//	private View.OnHoverListener createListener() {
//		return new View.OnHoverListener() {
//			@Override
//			public boolean onHover(View view, MotionEvent e) {
//				Log.d(ScummVM.LOG_TAG, "onHover mouseEvent");
//				return onMouseEvent(e, true);
//			}
//		};
//	}

	@Override
	public boolean onHover(View view, MotionEvent motionEvent) {
		//Log.d(ScummVM.LOG_TAG, "onHover mouseEvent");
		return onMouseEvent(motionEvent, true);
//		return false;
	}

//	public void attach(SurfaceView main_surface) {
//		main_surface.setOnHoverListener(_listener);
//	}

	// isTrackball is a subcase of isMouse (meaning isMouse will also return true)
	public static boolean isTrackball(KeyEvent e) {
		if (e == null) {
			return false;
		}

		int source = e.getSource();
		return ((source & InputDevice.SOURCE_TRACKBALL) == InputDevice.SOURCE_TRACKBALL) ||
		       (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ((source & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE));


	}

	// isTrackball is a subcase of isMouse (meaning isMouse will also return true)
	public static boolean isTrackball(MotionEvent e) {
		if (e == null) {
			return false;
		}
		//int source = e.getSource();

		InputDevice device = e.getDevice();

		if (device == null) {
			return false;
		}

		int sources = device.getSources();

		return ((sources & InputDevice.SOURCE_TRACKBALL) == InputDevice.SOURCE_TRACKBALL) ||
		       (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ((sources & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE));

	}

	// "Checking against SOURCE_STYLUS only indicates "an input device is capable of obtaining input
	// from a stylus. To determine whether a given touch event was produced by a stylus, examine
	// the tool type returned by MotionEvent#getToolType(int) for each individual pointer."
	// https://developer.android.com/reference/android/view/InputDevice#SOURCE_STYLUS
	public static boolean isStylus(MotionEvent e){
		if (e == null) {
			return false;
		}

		for(int idx = 0; idx < e.getPointerCount(); idx++) {
			if (e.getToolType(idx) == MotionEvent.TOOL_TYPE_STYLUS)
				return true;
		}

		return false;
	}

	public static boolean isMouse(KeyEvent e) {
		if (e == null) {
			return false;
		}

		int source = e.getSource();

		//Log.d(ScummVM.LOG_TAG, "isMouse keyEvent source: " + source);

		// SOURCE_MOUSE_RELATIVE is sent when mouse is detected as trackball
		// TODO: why does this happen? Do we need to also check for SOURCE_TRACKBALL here?
		return ((source & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)
		        || ((source & InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS)
		        || ((source & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD)
				||  isTrackball(e);
	}

	public static boolean isMouse(MotionEvent e) {
		if (e == null) {
			return false;
		}

		InputDevice device = e.getDevice();

		if (device == null) {
			return false;
		}

		int sources = device.getSources();

		// SOURCE_MOUSE_RELATIVE is sent when mouse is detected as trackball
		// TODO: why does this happen? Do we need to also check for SOURCE_TRACKBALL here?
		// TODO: should these all be checks against TOOL_TYPEs instead of SOURCEs?
		return ((sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)
		       || ((sources & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD)
		       ||  isStylus(e)
		       ||  isTrackball(e);
	}

	private boolean handleButton(MotionEvent e, boolean mbPressed, int mask, int downEvent, int upEvent) {
		boolean mbDown = (e.getButtonState() & mask) == mask;
		if ((e.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
		    && (e.getButtonState() & MotionEvent.BUTTON_BACK) == MotionEvent.BUTTON_BACK) {
			mbDown = (mask == MotionEvent.BUTTON_SECONDARY);
		}

		if (mbDown) {
			if (!mbPressed) {
				// mouse button was pressed just now
				//Log.d(ScummVM.LOG_TAG, "handleButton mbDown, not mbPressed, mask = " + mask);
				_scummvm.pushEvent(downEvent, (int)e.getX(), (int)e.getY(), e.getButtonState(), 0, 0, 0);
			}

			return true;
		} else {
			if (mbPressed) {
				//Log.d(ScummVM.LOG_TAG, "handleButton not mbDown, mbPressed, mask = " + mask);
				// mouse button was released just now
				_scummvm.pushEvent(upEvent, (int)e.getX(), (int)e.getY(), e.getButtonState(), 0, 0, 0);
			}

			return false;
		}
	}

	@SuppressLint("InlinedApi")
	public boolean onMouseEvent(MotionEvent e, boolean hover) {

		_scummvm.pushEvent(ScummVMEvents.JE_MOUSE_MOVE,
			(int) e.getX(),
			(int) e.getY(),
			0,
			0, 0, 0);

		if (e.getActionMasked() == MotionEvent.ACTION_SCROLL) {
			// The call is coming from ScummVMEvents, from a GenericMotionEvent (scroll wheel movement)
			// TODO Do we want the JE_MOUSE_MOVE event too in this case?
			int eventJEWheelUpDown = ScummVMEvents.JE_MOUSE_WHEEL_UP;
			if (e.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
				eventJEWheelUpDown = ScummVMEvents.JE_MOUSE_WHEEL_DOWN;
			}
			//Log.d(ScummVM.LOG_TAG, "onMouseEvent Wheel Up/Down = " + eventJEWheelUpDown);
			_scummvm.pushEvent(eventJEWheelUpDown,
			(int) e.getX(),
			(int) e.getY(),
			0,
			0, 0, 0);
		} else {

			int buttonState = e.getButtonState();

			//Log.d(ScummVM.LOG_TAG, "onMouseEvent buttonState = " + buttonState);

			boolean lmbDown = (buttonState & MotionEvent.BUTTON_PRIMARY) == MotionEvent.BUTTON_PRIMARY;

			if (!hover && e.getActionMasked() != MotionEvent.ACTION_UP && buttonState == 0) {
				// On some device types, ButtonState is 0 even when tapping on the touch-pad or using the stylus on the screen etc.
				lmbDown = true;
			}

			if (lmbDown) {
				if (!_lmbPressed) {
					// left mouse button was pressed just now
					_scummvm.pushEvent(ScummVMEvents.JE_LMB_DOWN, (int)e.getX(), (int)e.getY(), e.getButtonState(), 0, 0, 0);
				}

				_lmbPressed = true;
			} else {
				if (_lmbPressed) {
					// left mouse button was released just now
					_scummvm.pushEvent(ScummVMEvents.JE_LMB_UP, (int)e.getX(), (int)e.getY(), e.getButtonState(), 0, 0, 0);
				}

				_lmbPressed = false;
			}

			_rmbPressed = handleButton(e, _rmbPressed, MotionEvent.BUTTON_SECONDARY, ScummVMEvents.JE_RMB_DOWN, ScummVMEvents.JE_RMB_UP);
			_mmbPressed = handleButton(e, _mmbPressed, MotionEvent.BUTTON_TERTIARY, ScummVMEvents.JE_MMB_DOWN, ScummVMEvents.JE_MMB_UP);
			_bmbPressed = handleButton(e, _bmbPressed, MotionEvent.BUTTON_BACK, ScummVMEvents.JE_BMB_DOWN, ScummVMEvents.JE_BMB_UP);
			_fmbPressed = handleButton(e, _fmbPressed, MotionEvent.BUTTON_FORWARD, ScummVMEvents.JE_FMB_DOWN, ScummVMEvents.JE_FMB_UP);
			// Lint warning for BUTTON_STYLUS... "
			//  Field requires API level 23 (current min is 16): android.view.MotionEvent#BUTTON_STYLUS_PRIMARY"
			//  Field requires API level 23 (current min is 16): android.view.MotionEvent#BUTTON_STYLUS_SECONDARY"
			// We suppress it:
			//
			// https://stackoverflow.com/a/48588149
			_srmbPressed = handleButton(e, _srmbPressed, MotionEvent.BUTTON_STYLUS_PRIMARY, ScummVMEvents.JE_RMB_DOWN, ScummVMEvents.JE_RMB_UP);
			_smmbPressed = handleButton(e, _smmbPressed, MotionEvent.BUTTON_STYLUS_SECONDARY, ScummVMEvents.JE_MMB_DOWN, ScummVMEvents.JE_MMB_UP);
		}
		return true;
	}


}
