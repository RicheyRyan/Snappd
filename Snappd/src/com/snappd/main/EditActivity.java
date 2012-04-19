package com.snappd.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class EditActivity extends Activity {
	private ImageView mImage = null;
	private String mFilePath;
	private Bitmap mShareImage;
	private final String TAG = "EditActivity";
	private CameraTools mTools;
	private Bitmap mBitmap;
	private String mCropPic = "croppedImage";

	/**
	 * 
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit);

		Bundle extras = getIntent().getExtras();
		mFilePath = extras.getString("com.snappd.main.PhotoActivity.filePath");
		if (mFilePath != null) {
			try {
				// size the image view based on the size of the image captured
				mImage = (ImageView) findViewById(R.id.preview);
				LoadImage imageLoader = new LoadImage();
				imageLoader.execute(mFilePath);
				int minHeight = extras.getInt("imageHeight");
				int minWidth = extras.getInt("imageWidth");

				RelativeLayout.LayoutParams params = (LayoutParams) mImage
						.getLayoutParams();
				params.height = minHeight;
				params.width = minWidth;
				mImage.setLayoutParams(params);
				Log.d("Height", Integer.toString(mImage.getHeight()));
				Log.d("Width", Integer.toString(mImage.getWidth()));

				buttonListeners();

			} catch (Exception e) {
				Log.d(TAG, "Failed to load image.", e);
			}
		} else {
			Log.d(TAG, "File path could not be found.");
		}
	}

	/**
	 * Method to add the event listeners to buttons
	 */
	private void buttonListeners() {
		// Set the share button
		ImageButton share = (ImageButton) findViewById(R.id.share);
		share.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				LoadImage loader = new LoadImage();
				loader.execute(mFilePath);
				startShareMedia(mShareImage);

			}
		});

		ImageButton rotatePlus = (ImageButton) findViewById(R.id.rotate_right);
		rotatePlus.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final int NINETY = 90;
				mBitmap = mTools.setRotate(NINETY);
				mImage.setImageBitmap(mBitmap);
			}
		});

		ImageButton rotateMinus = (ImageButton) findViewById(R.id.rotate_left);
		rotateMinus.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				final int NINETY = -90;
				mBitmap = mTools.setRotate(NINETY);
				mImage.setImageBitmap(mBitmap);
			}
		});

		ImageButton undo = (ImageButton) findViewById(R.id.undo);
		undo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mBitmap = mTools.undo();
				mImage.setImageBitmap(mBitmap);
			}
		});

		ImageButton redo = (ImageButton) findViewById(R.id.redo);
		redo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mBitmap = mTools.redo();
				mImage.setImageBitmap(mBitmap);
			}
		});

		ImageButton crop = (ImageButton) findViewById(R.id.crop);
		crop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(EditActivity.this,
						com.snappd.main.CropImage.class);
				intent.putExtra("image-path", mFilePath);
				EditActivity.this.startActivity(intent);
			}

		});
	}
	
	
	/**
	 * The actions to be executed when an activity launched from this one concludes
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try {
			Log.d(TAG, "cropped pic returned");
			Uri uri = Uri.fromFile(new File(mCropPic));
			InputStream in = null;
			ContentResolver r = null;

			in = r.openInputStream(uri);
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			in = r.openInputStream(uri);
			mBitmap = BitmapFactory.decodeStream(in, null, o2);
			in.close();
			mImage.setImageBitmap(mBitmap);

		} catch (FileNotFoundException e) {
			Log.d(TAG, e.getMessage(), e);
		} catch (IOException e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	/**
	 * What happens when the activity finishes
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Store the bitmap in case it has been edited.
		ImageStorageTask store = new ImageStorageTask();
		store.execute(mBitmap);
		RelativeLayout root = (RelativeLayout) findViewById(R.id.edit_root);
		//Clear resources and request garbage collector.
		root.removeAllViews();
		System.gc();
	}

	/**
	 * Share image via share intent
	 * 
	 * @param image image to be shared
	 */
	private void startShareMedia(Bitmap image) {
		try {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("image/jpeg");

			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(mFilePath));

			startActivity(Intent.createChooser(intent, "Share via"));
		} catch (android.content.ActivityNotFoundException e) {
			Log.d(TAG, e.getMessage(), e);
		} catch (Exception e) {
			Log.d(TAG, e.getMessage(), e);
		}
	}

	/**
	 * Async task to load image from SD card.
	 */
	private class LoadImage extends AsyncTask<String, Void, Bitmap> {

		/**
		 * Logic to be carried out on background thread
		 */
		@Override
		protected Bitmap doInBackground(String... arg0) {
			Bitmap image = BitmapFactory.decodeFile(mFilePath);
			return image;
		}

		protected void onPostExecute(Bitmap img) {
			mTools = new CameraTools(img);
			mImage.setImageBitmap(img);
			mBitmap = img;
		}

	}

	/**
	 * A class to handle the saving of the edited image at the end of the activity 
	 * @author richeyryan
	 *
	 */
	private class ImageStorageTask extends AsyncTask<Bitmap, Void, Boolean> {

		/**
		 * The method which runs on a background thread
		 * 
		 * @param image
		 *            the byte array containing image information
		 * @return filePath the path of the saved image
		 */
		@Override
		protected Boolean doInBackground(Bitmap... image) {
			// Store image and receive the filePath where it is stored
			Bitmap data = image[0];
			StoreByteImage(data);
			return true;
		}

		/**
		 * A method which handles the writing of the file to the sdcard
		 * @param img the image being saved.
		 */
		private void StoreByteImage(Bitmap img) {
			try {
				FileOutputStream fileOutputStream = null;
				// Prepare the file to be buffered out
				fileOutputStream = new FileOutputStream(mFilePath);

				BufferedOutputStream bos = new BufferedOutputStream(
						fileOutputStream);

				// Compress the image into a jpeg at the defined quality
				// and write it out.
				img.compress(CompressFormat.JPEG, 100, bos);

				bos.flush();
				bos.close();
				img.recycle();

			} catch (FileNotFoundException e) {
				Log.d(TAG, e.getMessage(), e);
			} catch (IOException e) {
				Log.d(TAG, e.getMessage(), e);
			}
		}
	}
}
