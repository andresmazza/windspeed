package com.furtivo.windSpeed;

import java.text.DecimalFormat;

import com.furtivo.windSpeed.R;

import android.R.string;
import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements MicrophoneInputListener {
	MicrophoneInput micInput; // The micInput object provides real time audio.

	TextView realTimeDecimal;
	TextView realTimeFraction;

	TextView gustDecimal;
	TextView gustFraction;

	TextView labelTest;

	Button resetButton;

	private int mSampleRate = 44000; // The audio sampling rate to use.
	private int mAudioSource = MediaRecorder.AudioSource.DEFAULT; // Audio
																	// source
	double maxWind = 0.0;
	double currentWind = 0.0;
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

		realTimeDecimal = (TextView) findViewById(R.id.realTimeDecimal);
		realTimeFraction = (TextView) findViewById(R.id.realTimeFraction);

		gustDecimal = (TextView) findViewById(R.id.GustDecimal);
		gustFraction = (TextView) findViewById(R.id.GustFraction);

		labelTest = (TextView) findViewById(R.id.LabelTest);
		resetButton = (Button) findViewById(R.id.ResetButton);
		
		resetButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Toast msg = Toast.makeText(getBaseContext(),
						"You have clicked Reset Button", Toast.LENGTH_LONG);
				msg.show();
				maxWind = 0.0;
				currentWind = 0.0;
			}
		});

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
			final double rmsdB = 20 * Math.log10(mGain * mRmsSmoothed);

			this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					DecimalFormat df = new DecimalFormat("##");
					int decimal = (int) Math.round(rmsdB - 9);
					if (decimal <= 1) {
						realTimeDecimal.setText("0");
						realTimeFraction.setText("0");

					} else {
						realTimeDecimal.setText(df.format(decimal));
						setAverageWind(mRmsSmoothed);
						int one_decimal = (int) (Math.round(Math.abs(rmsdB))) % 10;
						realTimeFraction.setText(Integer.toString(one_decimal));

						currentWind = Double.parseDouble(realTimeDecimal
								.getText() + "." + realTimeDecimal.getText());
						if (currentWind > maxWind) {
							maxWind = currentWind;
							setMaxWind(maxWind);
						}

					}
					mDrawing = false;
				}
			});

		} else {
			mDrawingCollided++;
			// Log.v(TAG,
			// "runOnUiThread" + "than 20ms. Collision count"
			// + Double.toString(mDrawingCollided));
		}

	}

	private void setMaxWind(double maxWind) {
		int one_decimal = (int) (Math.round(Math.abs(maxWind) * 10)) % 10;
		gustDecimal.setText(String.format("%.0f", maxWind));
		gustFraction.setText(Integer.toString(one_decimal));
	}

	private void setAverageWind(double wind) {
		labelTest.setText(new DecimalFormat("##.#").format(wind));
	}

	@Override
	public void onPause() {
		super.onPause();
		finish();
		// MainActivity.remove(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		finish();
		// MainActivity.remove(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finish();
		// MainActivity.remove(this);
	}

}
