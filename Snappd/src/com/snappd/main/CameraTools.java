package com.snappd.main;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * Class to handle the editing tools
 *
 */
public class CameraTools {
	private Bitmap mBitmap;
	private List<byte[]> mEdits = new ArrayList<byte[]>();
	private int iterator = 0;

	/**
	 * Constructor for the class
	 * @param b the bitmap to be edited
	 */
	public CameraTools(Bitmap b) {
		mBitmap = b;
	}

	/**
	 * Rotates the bitmap a certain number of degrees
	 * @param deg the value for the rotation
	 * @return mBitmap the rotated bitmap
	 */
	public Bitmap setRotate(int deg) {
		//Save the current version of the bitmap
		saveVersion();
		//Create a matrix and set the rotation
		Matrix m = new Matrix();
		m.setRotate(deg, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);
		//Create a new bitmap using the matrix and return it
		mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(),
				mBitmap.getHeight(), m, false);
		return mBitmap;
	}

	/**
	 * A method to save a version of the image before it is
	 * changed to be used for the undo/redo functionality
	 */
	private void saveVersion() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		mBitmap.compress(CompressFormat.JPEG, 100, bos);
		byte[] bitmapData = bos.toByteArray();
		if (mEdits.size() == 5) {
			mEdits.remove(0);
			mEdits.add(bitmapData);
		} else {
			mEdits.add(bitmapData);
		}
	}

	/**
	 * Undo a change made to the image
	 * @return mBitmap the previous version of the image
	 */
	public Bitmap undo() {
		if (mEdits.size() > 0) {
			byte[] b = mEdits.get(mEdits.size() - 1);
			mBitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
		}
		return mBitmap;
	}

	/**
	 * Redo a change made to the bitmap
	 * @return mBitmap the image being restored
	 */
	public Bitmap redo() {
		if (mEdits.size() > 0) {
			byte[] b = mEdits.get(mEdits.size() - 1);
			mBitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
		}
		return mBitmap;
	}
}
