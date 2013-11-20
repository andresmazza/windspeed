package com.furtivo.windSpeed;

import java.text.DecimalFormat;

import com.furtivo.windSpeed.R;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity implements MicrophoneInputListener {
	MicrophoneInput micInput; // The micInput object provides real time audio.

	TextView mWindTextView;
	TextView mWindFractionTextView;

	private int mSampleRate = 44000; // The audio sampling rate to use.
	private int mAudioSource = MediaRecorder.AudioSource.DEFAULT; // Audio source

	double mAlpha = 0.9; // Coefficient of IIR smoothing filter for RMS.
	double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
	double mRmsSmoothed; // Temporally filtered version of RMS.

	// Variables to monitor UI update and check for slow updates.
	private volatile boolean mDrawing;
	private volatile int mDrawingCollided;

	private static final String TAG = "WindSpeed";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		micInput = new MicrophoneInput(this);
		setContentView(R.layout.activity_main);

		micInput.setSampleRate(mSampleRate);
		micInput.setAudioSource(mAudioSource);
		micInput.start();

		mWindTextView = (TextView) findViewById(R.id.decimal);
		mWindFractionTextView = (TextView) findViewById(R.id.fraction);

	}

	@Override
	public void processAudioFrame(short[] audioFrame) {
		if (!mDrawing) {
			mDrawing = true;

			// Compute the RMS value. (Note that this does not remove DC).
			double rms = 0;
			for (int i = 0; i < audioFrame.length; i++) {
				rms += audioFrame[i] * audioFrame[i];
			}
			rms = Math.sqrt(rms / audioFrame.length);

			// Compute a smoothed version for less flickering of the display.
			mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
			final double rmsdB = 0.5 * Math.log10(mGain * mRmsSmoothed);
			// final double rmsdB = 20 * Math.log10(mGain * mRmsSmoothed);

			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					DecimalFormat df = new DecimalFormat("##");
					// mWindTextView.setText(df.format(20 + rmsdB));
					mWindTextView.setText(df.format(rmsdB));
					// DecimalFormat df_fraction = new DecimalFormat("#");
					int one_decimal = (int) (Math.round(Math.abs(rmsdB * 10))) % 10;
					mWindFractionTextView.setText(Integer.toString(one_decimal));
					mDrawing = false;
				}
			});

		} else {
			mDrawingCollided++;
			Log.v(TAG,
					"runOnUiThread" + "than 20ms. Collision count"
							+ Double.toString(mDrawingCollided));
		}

	}
	   @Override
	    public void onDestroy()
	    {
	        super.onDestroy();
	        finish();
	        //MainActivity.remove(this);
	    }

}
