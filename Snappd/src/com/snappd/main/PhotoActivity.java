package com.snappd.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class PhotoActivity extends Activity {

	private Camera mCamera;
	private CameraPreview mPreview;
	private Camera.Parameters mParams;
	private CameraSettings mSettings;
	private String mFilePath;
	private final String TAG = "PhotoActivity";
	private double mPreviewWidth, mPreviewHeight;
	private int mCurrentCamera = 0;
	private final int FOCUS_ID = 0;
	private final int EXP_ID = 1;
	private final int COLOUR_ID = 2;
	private final int QUALITY_ID = 3;
	private final int SCENE_ID = 4;
	private final int SUPERFINE = 100;
	private final int FINE = 90;
	private final int NORMAL = 60;
	private final int BASIC = 40;
	private int mQuality = FINE;
	private ImageButton mResolution = null;

	// Callback for when a picture is taken
	// provides logic for when the picture was taken
	PictureCallback mPicture = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			try {
				// restart the preview
				mPreview.refreshPreview();
				// launch async task to save image.
				ImageStorageTask saveToSd = new ImageStorageTask();
				saveToSd.execute(data);

				// Take the bytes and put them into a bitmap
				// which is then added to the image button that
				// launches the EditActivity activity.

				Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
				int width = b.getWidth() - (b.getWidth() - b.getHeight());
				int height = b.getHeight();
				b = Bitmap.createBitmap(b, 0, 0, width, height);
				b = Bitmap.createScaledBitmap(b, 100, 100, true);
				ImageView last = (ImageView) findViewById(R.id.last);
				last.setOnClickListener(new View.OnClickListener() {

					public void onClick(View v) {
						Intent intent = new Intent(PhotoActivity.this,
								EditActivity.class);
						if (mFilePath != null) {
							intent.putExtra(
									"com.snappd.main.PhotoActivity.filePath",
									mFilePath);
							intent.putExtra("imageWidth", (int) mPreviewWidth);
							intent.putExtra("imageHeight", (int) mPreviewHeight);
							PhotoActivity.this.startActivity(intent);
						} else {
							Log.d(TAG,
									"Cannot start ImageReview, the filepath is null.");
						}

					}
				});
				last.setImageBitmap(b);
				System.gc();

			} catch (Exception e) {
				Log.d(TAG, e.getMessage(), e);
			}
		}

	};

	/**
	 * Method to instantiate all the main features of the activity
	 * 
	 * @param savedInstanceState
	 *            the saved state if the activity was exited
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			// set content view to preview screen
			setContentView(R.layout.main);

			// Create our Preview view and set it as the content of our
			// activity.
			mPreview = new CameraPreview(this);
			FrameLayout preview = (FrameLayout) findViewById(R.id.preview);
			preview.addView(mPreview);

			// figure out appropriate preview size
			setLayoutSize(preview);

			// add listeners to buttons
			buttonListeners();

		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	/**
	 * A method to add logic to all the buttons in the main layout
	 */
	public void buttonListeners() {
		// Camera shutter button
		ImageButton capture = (ImageButton) findViewById(R.id.shutter);
		capture.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {

					mCamera.autoFocus(new Camera.AutoFocusCallback() {

						public void onAutoFocus(boolean success, Camera camera) {
							mCamera.takePicture(null, null, mPicture);
						}

					});

					Log.d(TAG, "Picture taken.");
				} catch (Exception e) {
					Log.d(TAG, e.getMessage(), e);
				}

			}
		});

		// Set flash mode button
		final ImageButton flash = (ImageButton) findViewById(R.id.flash);
		flash.setOnClickListener(new View.OnClickListener() {
			boolean clicked = false;

			@Override
			public void onClick(View v) {

				final List<String> lowerCase = mParams.getSupportedFlashModes();
				final CharSequence[] items = capitalise(lowerCase).toArray(
						new CharSequence[lowerCase.size()]);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						PhotoActivity.this);
				builder.setTitle(R.string.flash_dialog);
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						final String nextFlash;
						nextFlash = lowerCase.get(item);
						mSettings.setFlash(nextFlash);
						// mPreview.refreshPreview();

						// Set image depending on current mode
						if (nextFlash.equals(Parameters.FLASH_MODE_AUTO)) {
							flash.setImageResource(R.drawable.flash_auto_button);
						} else if (nextFlash.equals(Parameters.FLASH_MODE_OFF)) {
							flash.setImageResource(R.drawable.flash_off_button);
						} else if (nextFlash.equals(Parameters.FLASH_MODE_ON)) {
							flash.setImageResource(R.drawable.flash_on_button);
						} else if (nextFlash
								.equals(Parameters.FLASH_MODE_RED_EYE)) {
							flash.setImageResource(R.drawable.flash_eye_button);
						} else if (nextFlash
								.equals(Parameters.FLASH_MODE_TORCH)) {
							flash.setImageResource(R.drawable.flash_torch_button);
						}
						clicked = true;

					}

				});
				AlertDialog alert = builder.create();
				alert.show();
				if (clicked) {
					alert.dismiss();
					clicked = false;
				}
			}

		});

		// Set white balance mode
		final ImageButton whiteBalance = (ImageButton) findViewById(R.id.wb);
		whiteBalance.setOnClickListener(new View.OnClickListener() {
			boolean clicked = false;

			@Override
			public void onClick(View v) {
				Log.d("whiteBalanceButton", "clicked");

				final List<String> lowerCase = mParams
						.getSupportedWhiteBalance();
				final CharSequence[] items = capitalise(lowerCase).toArray(
						new CharSequence[lowerCase.size()]);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						PhotoActivity.this);
				builder.setTitle(R.string.wb_dialog);
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						final String nextWhiteBalance = lowerCase.get(item);
						mSettings.setWhiteBalanceMode(nextWhiteBalance);

						// Set image depending on current mode
						if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_AUTO)) {
							whiteBalance
									.setImageResource(R.drawable.awb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)) {
							whiteBalance
									.setImageResource(R.drawable.cwb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_DAYLIGHT)) {
							whiteBalance
									.setImageResource(R.drawable.swb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_FLUORESCENT)) {
							whiteBalance
									.setImageResource(R.drawable.fwb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_INCANDESCENT)) {
							whiteBalance
									.setImageResource(R.drawable.iwb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_SHADE)) {
							whiteBalance
									.setImageResource(R.drawable.cwb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_TWILIGHT)) {
							whiteBalance
									.setImageResource(R.drawable.cwb_button);
						} else if (nextWhiteBalance
								.equals(Parameters.WHITE_BALANCE_WARM_FLUORESCENT)) {
							whiteBalance
									.setImageResource(R.drawable.fwb_button);

						}
						clicked = true;
					}

				});
				AlertDialog alert = builder.create();
				alert.show();
				if (clicked) {
					alert.dismiss();
					clicked = false;
				}
			}

		});

		// Increase zoom
		ImageButton plusZoom = (ImageButton) findViewById(R.id.plus);
		plusZoom.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mSettings.increaseZoom();
			}
		});

		// Decrease zoom
		ImageButton minusZoom = (ImageButton) findViewById(R.id.minus);
		minusZoom.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mSettings.decreaseZoom();
			}
		});


		// Set picture size
		mResolution = (ImageButton) findViewById(R.id.res);
		mResolution.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("resolutionButton", "clicked");

				final List<Size> res = mParams.getSupportedPictureSizes();
				final List<String> listRes = mSettings.getResolutions(res);

				final CharSequence[] items = listRes
						.toArray(new CharSequence[listRes.size()]);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						PhotoActivity.this);
				builder.setTitle("Choose a picture resolution:");
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						final Camera.Size newRes = res.get(item);
						mSettings.setResMode(newRes);
						calcRes(newRes);

						// Log.d(TAG, newRes);

					}

				});
				AlertDialog alert = builder.create();
				alert.show();
			}

		});

	}

	/**
	 * Method used to figure out what what drawable should be applied to 
	 * the resolution button based on the current resolution.
	 * @param newRes
	 */
	private void calcRes(Camera.Size newRes) {
		// Set image depending on current mode
		if (newRes.width >= 3264 && newRes.height >= 2448) {
			mResolution.setImageResource(R.drawable.eight_mp_button);
		} else if (newRes.width >= 3072 && newRes.height >= 2304) {
			mResolution.setImageResource(R.drawable.seven_mp_button);
		} else if (newRes.width >= 3032 && newRes.height >= 2008) {
			mResolution.setImageResource(R.drawable.six_mp_button);
		} else if (newRes.width >= 2560 && newRes.height >= 1920) {
			mResolution.setImageResource(R.drawable.five_mp_button);
		} else if (newRes.width >= 2240 && newRes.height >= 1680) {
			mResolution.setImageResource(R.drawable.four_mp_button);
		} else if (newRes.width >= 2048 && newRes.height >= 1386) {
			mResolution.setImageResource(R.drawable.three_mp_button);
		} else if (newRes.width >= 1600 && newRes.height >= 1200) {
			mResolution.setImageResource(R.drawable.two_mp_button);
		} else if (newRes.width >= 1024 && newRes.height >= 768) {
			mResolution.setImageResource(R.drawable.one_mp_button);
		} else if (newRes.width >= 640 && newRes.height >= 480) {
			mResolution.setImageResource(R.drawable.vga_button);
		} else if (newRes.width < 640 && newRes.height < 480) {
			mResolution.setImageResource(R.drawable.vga_button);
		}
	}

	/**
	 * Takes a list of Strings and returns the list with the first letter of
	 * each word capitalized. Used for aesthetics when creating menus.
	 * 
	 * @param strings
	 *            list of strings to be altered
	 * @return capitalised the altered list of strings
	 */
	private List<String> capitalise(List<String> strings) {
		// create variables
		String s;
		List<String> capitalised = new ArrayList<String>();

		// loop through list
		try {
			for (int i = 0; i < strings.size(); i++) {
				s = strings.get(i).toString();

				// Split string into two strings, one with the first
				// letter and a second with the rest of the word
				String first = s.substring(0, 1);
				String rest = s.substring(1, s.length());

				// Make the first letter upper case and stick the string
				// back together
				first = first.toUpperCase();
				s = first + rest;

				// Add string into list
				capitalised.add(i, s);
			}
		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
		return capitalised;
	}

	/**
	 * Set the size of the frame layout based on screen size
	 * 
	 * @param preview
	 *            the layout to be sized
	 */
	public void setLayoutSize(FrameLayout preview) {
		try {
			// Get the dimensions of the screen.
			DisplayMetrics metrics = new DisplayMetrics();
			metrics = getScreenSize();

			int screenWidth = metrics.widthPixels;
			int screenHeight = metrics.heightPixels;

			// Find what measurements match the desired ratio
			double ratio = 0.8;
			Log.d("ratio", Double.toString(ratio));
			mPreviewWidth = screenWidth * ratio;
			mPreviewHeight = screenHeight * ratio;
			Log.d("Height", Double.toString(mPreviewHeight));
			Log.d("Width", Double.toString(mPreviewWidth));

			// Apply the measurements to the frame layout.
			RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) preview
					.getLayoutParams();
			params.height = (int) mPreviewHeight;
			params.width = (int) mPreviewWidth;

		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Return the measurements for the screen of the device
	 * 
	 * @return metrics the metrics object which holds the measurements
	 */
	public DisplayMetrics getScreenSize() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics;
	}

	/**
	 * Safely acquire a reference to the camera object,
	 * 
	 * @return c the camera object.
	 */
	public Camera getCameraInstance(int camera) {
		Camera c = null;
		try {
			// Attempt to get a Camera instance
			c = Camera.open(camera);
		} catch (Exception e) {
			// If it fails catch exception and print details
			Log.d("getCameraInstance", e.getMessage(), e);
		}
		// returns null if camera is unavailable
		return c;
	}

	/**
	 * Logic for when the activity is resumed
	 */
	@Override
	protected void onResume() {

		try {
			super.onResume();

			// Open the default i.e. the first rear facing camera
			mCamera = getCameraInstance(mCurrentCamera);
			mPreview.setCamera(mCamera);
			mParams = mCamera.getParameters();
			mSettings = new CameraSettings(mCamera);
			mParams.setFocusMode(Parameters.FOCUS_MODE_AUTO);
			calcRes(mParams.getPictureSize());

		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Logic for when the activity is paused
	 */
	@Override
	protected void onPause() {
		super.onPause();

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null) {
			mPreview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}

	/**
	 * Creates the menu pane and adds the various options for adjusting camera
	 * settings.
	 * 
	 * @param menu
	 *            the menu that is created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, FOCUS_ID, 0, R.string.menu_focus);
		menu.add(0, EXP_ID, 0, R.string.menu_exp);
		menu.add(0, COLOUR_ID, 0, R.string.menu_colour);
		menu.add(0, QUALITY_ID, 0, R.string.menu_quality);
		menu.add(0, SCENE_ID, 0, R.string.menu_scene);

		return true;
	}

	/**
	 * When an item is selected, it launches the appropriate selection menu
	 * 
	 * @param featureId
	 *            the item block in the menu
	 * @param item
	 *            the MenuItem object that is selected
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		final CharSequence[] items;
		AlertDialog.Builder builder;
		AlertDialog alert;

		switch (item.getItemId()) {
		case FOCUS_ID:
			// choose a focus mode
			Log.d("focusModeButton", "clicked");

			final List<String> focusModes = mParams.getSupportedFocusModes();
			items = capitalise(focusModes).toArray(
					new CharSequence[focusModes.size()]);

			builder = new AlertDialog.Builder(PhotoActivity.this);
			builder.setTitle(R.string.menu_focus);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					final String newFocusMode = focusModes.get(item);
					mSettings.setFocusMode(newFocusMode);

				}
			});
			alert = builder.create();
			alert.show();

			break;

		case EXP_ID:
			Log.d("exModeButton", "clicked");

			final int maxMode = mParams.getMaxExposureCompensation();
			final int minMode = mParams.getMinExposureCompensation();

			final String[] modes = mSettings.getExposures(minMode, maxMode);

			Log.d("Min EX", Integer.toString(minMode));
			Log.d("Max EX", Integer.toString(maxMode));
			for (int i = 0; i < modes.length; i++) {
				Log.d("element", modes[i]);
			}

			items = Arrays.copyOf(modes, modes.length, CharSequence[].class);

			builder = new AlertDialog.Builder(PhotoActivity.this);
			builder.setTitle(R.string.menu_exp);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					final String newExMode = modes[item];
					mSettings.setExMode(newExMode);

				}
			});
			alert = builder.create();
			alert.show();

			break;

		case COLOUR_ID:
			// choose a colour mode
			Log.d("colourModeButton", "clicked");

			final List<String> colourModes = mParams.getSupportedColorEffects();
			items = capitalise(colourModes).toArray(
					new CharSequence[colourModes.size()]);

			builder = new AlertDialog.Builder(PhotoActivity.this);
			builder.setTitle(R.string.menu_colour);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					final String nextColourMode = colourModes.get(item);
					mSettings.setColourMode(nextColourMode);

				}
			});
			alert = builder.create();
			alert.show();

			break;

		case QUALITY_ID:
			// choose a predefined jpeg compression quality
			Resources res = getResources();
			String[] strings = res.getStringArray(R.array.quality_settings);
			items = Arrays
					.copyOf(strings, strings.length, CharSequence[].class);
			Log.d("quality options length", Integer.toString(items.length));

			builder = new AlertDialog.Builder(PhotoActivity.this);
			builder.setTitle(R.string.quality_dialog);
			builder.setItems(items, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String qual = items[which].toString();
					if (qual.equals("Superfine")) {
						mQuality = SUPERFINE;
					} else if (qual.equals("Fine")) {
						mQuality = FINE;
					} else if (qual.equals("Normal")) {
						mQuality = NORMAL;
					} else if (qual.equals("Basic")) {
						mQuality = BASIC;
					}
				}
			});
			alert = builder.create();
			alert.show();
			break;

		case SCENE_ID:

			// choose a scene mode
			Log.d("sceneModeButton", "clicked");

			final List<String> sceneModes = mParams.getSupportedSceneModes();
			items = capitalise(sceneModes).toArray(
					new CharSequence[sceneModes.size()]);

			builder = new AlertDialog.Builder(PhotoActivity.this);
			builder.setTitle(R.string.menu_scene);
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					final String newSceneMode = sceneModes.get(item);
					mSettings.setSceneMode(newSceneMode);

				}
			});
			alert = builder.create();
			alert.show();

			break;

		}

		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * Class to store an image asynchronously in external storage
	 */
	private class ImageStorageTask extends AsyncTask<byte[], Void, String> {

		/**
		 * The method which runs on a background thread
		 * 
		 * @param image
		 *            the byte array containing image information
		 * @return filePath the path of the saved image
		 */
		@Override
		protected String doInBackground(byte[]... image) {
			// Store image and receive the filePath where it is stored
			byte[] data = image[0];
			String filePath = StoreByteImage(data, 100);

			// If it is successful return the file path,
			// otherwise just return null
			if (filePath != null) {
				Log.d(TAG, "Image saved successfully.");
				return filePath;
			} else {
				Log.d(TAG, "Failed to save image.");
				return null;
			}
		}

		/**
		 * Method runs on the UI thread but can receive info from the background
		 * thread. Used to pass the file path to a member variable.
		 * 
		 * @param path
		 *            the file path passed from the background thread
		 */
		protected void onPostExecute(String path) {
			// If the path is set, set the member variable otherwise log
			// log that the path is not set.
			if (path != null) {
				mFilePath = path;
				Log.d("filepath", mFilePath);
			} else {
				Log.d("ImageStoreTask", "No path has been returned.");
			}
			System.gc();
		}

		/**
		 * Handles the encoding of an image and saving it to external storage
		 * 
		 * @param imageData
		 *            byte array of the image to be saved.
		 * @param quality
		 *            the desired quality setting
		 * @return
		 */
		private String StoreByteImage(byte[] imageData, int quality) {
			String filePath = null;

			String state = Environment.getExternalStorageState();
			Log.v(TAG, "storage state is " + state);
			try {
				// Create the file in the public pictures directory
				// so the saved images will persist after app is deleted
				File sdImageMainDirectory = new File(
						Environment.getExternalStorageDirectory()
								+ File.separator + Environment.DIRECTORY_DCIM);

				Boolean exists = sdImageMainDirectory.exists();
				Log.d("exists", exists.toString());
				Boolean writeable = sdImageMainDirectory.canWrite();
				Log.d("Can the file be written to", writeable.toString());

				FileOutputStream fileOutputStream = null;

				// Get the time at that moment.
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());

				// Create the file path with the file and the unique file name
				// based on time
				filePath = sdImageMainDirectory.toString() + File.separator
						+ "IMG_" + timeStamp + ".jpg";
				Log.d("The filepath", filePath);

				// Prepare the file to be buffered out
				fileOutputStream = new FileOutputStream(filePath);

				BufferedOutputStream bos = new BufferedOutputStream(
						fileOutputStream);

				// Sub sample the image to make it smaller
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				options.inInputShareable = true;
				options.inPurgeable = true;
				options.inTempStorage = new byte[32 * 1024];
				options.inPreferQualityOverSpeed = true;

				// Decode the byte array into a bitmap
				Bitmap myImage = BitmapFactory.decodeByteArray(imageData, 0,
						imageData.length, options);

				// Compress the image into a jpeg at the defined quality
				// and write it out.
				myImage.compress(CompressFormat.JPEG, mQuality, bos);

				bos.flush();
				bos.close();
				options = null;
				myImage = null;

			} catch (FileNotFoundException e) {
				Log.d(TAG, e.getMessage(), e);
			} catch (IOException e) {
				Log.d(TAG, e.getMessage(), e);
			}

			return filePath;

		}
	}

}
