package com.snappd.main;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

/**
 * A class to handle the different settings applicable to the
 * camera object.
 */
public class CameraSettings {

	private Camera.Parameters mParams;
	private Camera mCamera;

	/**
	 * A constructor containing the camera being manipulated.
	 * @param c the camera being manipulated
	 */
	public CameraSettings(Camera c) {
		mCamera = c;
		mParams = c.getParameters();
	}

	/**
	 * Set the flash mode
	 * @param newFlash the new flash mode
	 */
	public void setFlash(String newFlash) {
		mParams.setFlashMode(newFlash);
		update();
	}

	/**
	 * Set the white balance
	 * @param newWhiteBalance the new white balance mode
	 */
	public void setWhiteBalanceMode(String newWhiteBalance) {
		mParams.setWhiteBalance(newWhiteBalance);
		update();
	}

	/**
	 * Increase the zoom of the camera
	 */
	public void increaseZoom() {
		try {
			//Check if zoom is supported
			if (mParams.isZoomSupported()) {

				//Find the max zoom and the value of zoom now.
				final int mMaxZoom = mParams.getMaxZoom();
				int inc = 1;
				int currentZoom = mParams.getZoom();

				//If the current zoom is less than the max zoom increment it by
				//a set amount
				if (currentZoom < mMaxZoom) {

					mParams.setZoom(currentZoom + inc);
					update();

				}
				//Otherwise just log that zoom is not available
			} else {
				Log.d("cannot ZOOM", "CANNOT ZOOM");
			}
		} catch (Exception e) {
			Log.d("Zoom", e.getMessage(), e);
		}

	}

	/**
	 * Decrease the zoom of the camera
	 */
	public void decreaseZoom() {
		try {
			//Check if zoom is supported
			if (mParams.isZoomSupported()) {
				//find the current zoom
				int inc = 1;
				int currentZoom = mParams.getZoom();
				//If the zoom is greater than 0 decrease it
				if (currentZoom > 0) {

					mParams.setZoom(currentZoom - inc);
					update();

				}
				//Otherwise just log that zoom is not available
			} else {
				Log.d("cannot ZOOM", "CANNOT ZOOM");
			}
		} catch (Exception e) {
			Log.d("Zoom", e.getMessage(), e);
		}

	}

	/**
	 * Return a string array of exposure settings based on
	 * the minimum and maximum exposure available
	 * 
	 * @param minEx min exposure
	 * @param maxEx max exposure
	 * @return list the list of possible exposure values
	 */
	public String[] getExposures(int minEx, int maxEx) {
		// finds the length of the array
		int l = maxEx - (minEx) + 1;
		String[] list = new String[l];
		//Go through the array adding all the values
		for (int i = 0; i < list.length; i++) {
			list[i] = Integer.toString(minEx + i);
		}
		return list;
	}

	/**
	 * Set the exposure value
	 * @param newExp the new exposure value as a string
	 */
	public void setExMode(String newExp) {
		int exp = Integer.parseInt(newExp);
		mParams.setExposureCompensation(exp);
		update();

	}

	/**
	 * Set the picture resolution
	 * @param newRes the new resolution held in a Size object
	 */
	public void setResMode(Size newRes) {
		mParams.setPictureSize(newRes.width, newRes.height);

		Log.d("picture width", Integer.toString(newRes.width));
		Log.d("picture height", Integer.toString(newRes.height));
		update();

	}

	/**
	 * Returns a list of strings which are the available resolutions
	 * @param resSizes the list of resolutions in Size objects
	 * @return list the string list
	 */
	public List<String> getResolutions(List<Size> resSizes) {
		List<String> list = new ArrayList<String>();
		//Go through the list taking each size and make them
		//into a string with a 'x' separating the width and height
		//e.g. 640x480
		for (int i = 0; i < resSizes.size(); i++) {
			Size res = resSizes.get(i);
			String label = res.width + "x" + res.height;
			list.add(label);
		}

		return list;
	}

	public void setColourMode(String newColourMode) {
		mParams.setColorEffect(newColourMode);
		update();
	}

	/**
	 * Set the scene mode 
	 * @param newSceneMode string value of the new scene mode
	 */
	public void setSceneMode(String newSceneMode) {
		mParams.setSceneMode(newSceneMode);
		update();
	}

	/**
	 * Set the focus mode
	 * @param newFocusMode string value of the new focus mode
	 */
	public void setFocusMode(String newFocusMode) {
		mParams.setFocusMode(newFocusMode);
		update();
	}

	/**
	 * Method used to ensure the camera object holds the latest
	 * parameters object.
	 */
	private void update() {
		mCamera.setParameters(mParams);
	}

}
