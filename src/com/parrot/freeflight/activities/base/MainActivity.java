package com.parrot.freeflight.activities.base;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.freeflight.R;
import com.parrot.freeflight.R.id;
import com.parrot.freeflight.activities.SplashActivity;
import com.parrot.freeflight.drone.DroneProxy.ARDRONE_LED_ANIMATION;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneConnectionChangedReceiver;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiver;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneReadyReceiver;
import com.parrot.freeflight.receivers.DroneReadyReceiverDelegate;
import com.parrot.freeflight.service.DroneControlService;

public class MainActivity extends Activity implements SensorEventListener,
		DroneReadyReceiverDelegate, DroneFlyingStateReceiverDelegate,
		DroneConnectionChangeReceiverDelegate,
		DroneBatteryChangedReceiverDelegate {

	private DroneControlService droneControlService;
	private DroneBatteryChangedReceiver droneBatteryReceiver;

	private View topView;
	private Button btnConnect, btnTakeoff, btnUp, btnDown, btnLeft, btnRigth,
			btnIdle, btnBack, btnForward, btnTurnLeft, btnTurnRight,
			btnLEDAnimation, btnSwitchMode;
	private SeekBar powerBar;
	private TextView tvSpeeed, battery_display, flipStatus;

	private boolean isDroneConnected, isOnGyroMode = false;
	private float power = 0.2f;

	private BroadcastReceiver droneReadyReceiver;
	private BroadcastReceiver droneConnectionChangeReceiver;
	private DroneFlyingStateReceiver droneFlyingStateReceiver;

	private SensorManager sensorManager;
	private boolean hideButtons = false, isConnected = false, start = false,
			manualMode = true;
	private long lastUpdate;
	private WakeLock mWakeLock;
	private int chargeCount = 0;
	ProgressBar mProgressBar;
	CountDownTimer mCountDownTimer;
	float oldPostX, oldPostY;

	ObjectAnimator animation1, animation2, animation3, animation4, fadeOut,
			fadeOut1, fadeIn, mover;
	RelativeLayout droneLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.temp);

		topView = findViewById(R.id.textViewTop);
		tvSpeeed = (TextView) findViewById(id.tv_speed);
		battery_display = (TextView) findViewById(id.battery_display);
		flipStatus = (TextView) findViewById(id.flipStatus);
		// gyro drone anime
		droneLayout = (RelativeLayout) findViewById(R.id.droneLayout);

		droneBatteryReceiver = new DroneBatteryChangedReceiver(this);

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK,
				"Drone Control");
		wl.acquire();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		lastUpdate = System.currentTimeMillis();

		powerBar = (SeekBar) findViewById(R.id.seekBarPower);
		powerBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			int progressChanged = 0;

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				progressChanged = progress;
				tvSpeeed.setText("Move Speed [ " + Integer.toString(progress)
						+ "% ]");

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				power = getConvertedValue(progressChanged);
				// Toast.makeText(seekBar.getContext(), "Value: " + power,
				// Toast.LENGTH_SHORT).show();
			}
		});

		btnConnect = (Button) findViewById(R.id.connect);
		btnConnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (isDroneConnected) {// if it is connected
					onDisconnectPressed();
					topView.setBackgroundColor(0xFFFF0000);
					topView.invalidate();
					isDroneConnected = false;
					btnConnect.setText("Connect");
				} else {// if not
					ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo mWifi = connManager
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (mWifi.isConnected()) {// check for wifi connection
						btnConnect.setText("Disconnect");
						Toast.makeText(
								getApplicationContext(),
								"WiFi is connected, make sure it is connect to the drone.",
								Toast.LENGTH_SHORT).show();
						if (onConnectPressed()) {
							topView.setBackgroundColor(0xFF00FF00);
							topView.invalidate();
						} else {
							topView.setBackgroundColor(0xFFFF0000);
							topView.invalidate();
							isDroneConnected = false;
							enableButtons(false);
						}
					} else {
						Toast.makeText(getApplicationContext(),
								"WiFi is not connected", Toast.LENGTH_SHORT)
								.show();
					}
				}
			}
		});

		btnTakeoff = (Button) findViewById(R.id.takeoff);
		btnTakeoff.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onTakeoffPressed();
			}
		});
		/*
		 * btnLand = (Button) findViewById(id.btn_land);
		 * btnLand.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { onLandPresed(); } });
		 */

		btnIdle = (Button) findViewById(id.idle);
		btnIdle.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				power = 0;
				powerBar.setProgress(0);
			}
		});

		btnUp = (Button) findViewById(id.up);
		btnUp.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					onMoveUpPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					onMoveUpPressed(0);
					return true;
				}
				return false;
			}
		});

		btnDown = (Button) findViewById(id.down);
		btnDown.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					onMoveDownPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					onMoveDownPressed(0);
					return true;
				}
				return false;
			}
		});

		btnLeft = (Button) findViewById(id.left);
		btnLeft.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					droneControlService.setProgressiveCommandEnabled(true);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(true);
					onMoveLeftPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					droneControlService.setProgressiveCommandEnabled(false);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(false);
					onMoveLeftPressed(0);
					return true;
				}
				return false;
			}
		});

		btnRigth = (Button) findViewById(id.right);
		btnRigth.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					droneControlService.setProgressiveCommandEnabled(true);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(true);
					onMoveRightPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					droneControlService.setProgressiveCommandEnabled(false);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(false);
					onMoveRightPressed(0);
					return true;
				}
				return false;
			}
		});

		btnBack = (Button) findViewById(id.back);
		btnBack.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					droneControlService.setProgressiveCommandEnabled(true);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(true);
					onMoveBackwardPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					droneControlService.setProgressiveCommandEnabled(false);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(false);
					onMoveBackwardPressed(0);
					return true;
				}
				return false;
			}
		});

		btnForward = (Button) findViewById(id.forward);
		btnForward.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					droneControlService.setProgressiveCommandEnabled(true);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(true);
					onMoveForwardPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					droneControlService.setProgressiveCommandEnabled(false);
					droneControlService
							.setProgressiveCommandCombinedYawEnabled(false);
					onMoveForwardPressed(0);
					return true;
				}
				return false;
			}
		});

		btnTurnLeft = (Button) findViewById(id.turnleft);
		btnTurnLeft.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					onTurnLeftPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					onTurnLeftPressed(0);
					return true;
				}
				return false;
			}
		});

		btnTurnRight = (Button) findViewById(id.turnright);
		btnTurnRight.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// PRESSED
					onTurnRightPressed(power);
					return true;
				case MotionEvent.ACTION_UP:
					// RELEASED
					onTurnRightPressed(0);
					return true;
				}
				return false;
			}
		});

		btnLEDAnimation = (Button) findViewById(id.led);
		btnLEDAnimation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				droneControlService
						.playLedAnimation(
								10.0f,
								3,
								ARDRONE_LED_ANIMATION.ARDRONE_LED_ANIMATION_BLINK_ORANGE
										.ordinal());
				// ARDRONE_LED_ANIMATION.ARDRONE_LED_ANIMATION_DOUBLE_MISSILE
			}
		});

		btnSwitchMode = (Button) findViewById(R.id.btnSwitchMode);
		btnSwitchMode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (isOnGyroMode == false)
					isOnGyroMode = true;
				else
					isOnGyroMode = false;

				hideButtons();
				showDrone();
				startAnimation();
			}
		});

		droneReadyReceiver = new DroneReadyReceiver(this);
		droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);
		droneFlyingStateReceiver = new DroneFlyingStateReceiver(this);
		oldPostX = btnConnect.getLeft();
		oldPostY = btnConnect.getTop();

		// onConnectPressed();

		// droneFlyingStateReceiver = new
		// DroneFlyingStateReceiverDelegate(this);

		// disable buttons until we there is no connection
		enableButtons(false);

	}

	public void onDroneBatteryChanged(int value) {
		if (value > 0) {
			battery_display.setText(Integer.toString(value));
			isConnected = true;
		}
	}

	private void registerReceivers() {
		// Local receivers
		LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager
				.getInstance(getApplicationContext());
		localBroadcastMgr.registerReceiver(droneBatteryReceiver,
				new IntentFilter(
						DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
		localBroadcastMgr.registerReceiver(droneFlyingStateReceiver,
				new IntentFilter(
						DroneControlService.DRONE_FLYING_STATE_CHANGED_ACTION));
	}

	private void unregisterReceivers() {

		// Unregistering local receivers
		LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager
				.getInstance(getApplicationContext());
		localBroadcastMgr.unregisterReceiver(droneBatteryReceiver);
		localBroadcastMgr.unregisterReceiver(droneFlyingStateReceiver);
	}

	public float getConvertedValue(int intVal) {
		float floatVal = 0.0f;
		floatVal = .01f * intVal;
		return floatVal;
	}

	public void enableButtons(boolean value) {
		// btnDisconnect.setEnabled(value);
		btnDown.setEnabled(value);
		btnUp.setEnabled(value);
		btnIdle.setEnabled(value);
		btnLeft.setEnabled(value);
		btnRigth.setEnabled(value);
		btnTakeoff.setEnabled(value);
		btnBack.setEnabled(value);
		btnForward.setEnabled(value);
		btnTurnLeft.setEnabled(value);
		btnTurnRight.setEnabled(value);
		btnLEDAnimation.setEnabled(value);
	}

	public void showDrone() {
		// called when bring out the anime drone

		if (hideButtons == true) { // if btns were hidden
			droneLayout.setVisibility(View.VISIBLE);

			fadeOut = ObjectAnimator.ofFloat(droneLayout, "alpha", 0f);
			fadeOut.setDuration(0);
			fadeIn = ObjectAnimator.ofFloat(droneLayout, "alpha", 0f, 1f);
			fadeIn.setDuration(5000);
			AnimatorSet animatorSet = new AnimatorSet();
			animatorSet.play(fadeIn).after(fadeOut);
			animatorSet.start();
		} else
			droneLayout.setVisibility(View.GONE);
	}

	public void startAnimation() {
		// rotate
		float dest = 0;
		float source = 0;
		ImageView aniView2 = (ImageView) findViewById(R.id.imageView2);
		ImageView aniView3 = (ImageView) findViewById(R.id.imageView3);
		ImageView aniView4 = (ImageView) findViewById(R.id.imageView4);
		ImageView aniView5 = (ImageView) findViewById(R.id.imageView5);

		dest = 360;
		source = 0;
		/*
		 * if (aniView2.getRotation() == 360) {
		 * System.out.println(aniView2.getAlpha()); dest = 0; source = 360; }
		 */
		/*
		 * animation1 = ObjectAnimator.ofFloat(aniView,"rotation", dest);
		 * animation1.setDuration(2000); animation1.start();
		 */

		if (start == true) { // if the animation is playing
			start = false;
			animation1.cancel();
			animation2.cancel();
			animation3.cancel();
			animation4.cancel();
		}

		if (isDroneConnected == true) {// if drone is connected
			animation1 = ObjectAnimator.ofFloat(aniView2, "rotation", source,
					dest);
			animation1.setDuration(200);
			animation1.setInterpolator(new LinearInterpolator());
			animation1.setRepeatCount(9999); // how many times
			animation1.setRepeatMode(1); // 1 restart & //2 reverse
			animation1.start();

			animation2 = ObjectAnimator.ofFloat(aniView3, "rotation", 30, dest);
			animation2.setDuration(200);
			animation2.setInterpolator(new LinearInterpolator());
			animation2.setRepeatCount(9999);
			animation2.setRepeatMode(1);
			animation2.start();

			animation3 = ObjectAnimator
					.ofFloat(aniView4, "rotation", -30, dest);
			animation3.setDuration(200);
			animation3.setInterpolator(new LinearInterpolator());
			animation3.setRepeatCount(9999);
			animation3.setRepeatMode(1);
			animation3.start();

			animation4 = ObjectAnimator.ofFloat(aniView5, "rotation", 65, dest);
			animation4.setDuration(200);
			animation4.setInterpolator(new LinearInterpolator());
			animation4.setRepeatCount(9999);
			animation4.setRepeatMode(1);
			animation4.start();

			start = true;
		}

	}

	public void moveObjects(int i, float X, float Y) {
		float dest = 0;
		float source = 0;
		ObjectAnimator animation1;
		int width = btnTakeoff.getMeasuredWidth();

		ObjectAnimator animX = null, animY = null;
		AnimatorSet animSetXY = new AnimatorSet();
		// move objects

		if (i == 1) {// connect btn
			animX = ObjectAnimator.ofFloat(btnConnect, "x", X);
			animY = ObjectAnimator.ofFloat(btnConnect, "y", Y);
		}
		if (i == 2) {// take off btn
			animX = ObjectAnimator.ofFloat(btnTakeoff, "x", X);
			animY = ObjectAnimator.ofFloat(btnTakeoff, "y", Y);
		}
		if (i == 3) {// seekBarPower
			animX = ObjectAnimator.ofFloat(powerBar, "x", X);
			animY = ObjectAnimator.ofFloat(powerBar, "y", Y);
		}
		if (i == 4) {// tv_speed
			animX = ObjectAnimator.ofFloat(tvSpeeed, "x", X);
			animY = ObjectAnimator.ofFloat(tvSpeeed, "y", Y);
		}

		animSetXY.playTogether(animX, animY);
		animSetXY.setDuration(2000);
		animSetXY.start();
	}

	public void hideButtons() {
		RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.relativeLayout1);
		if (hideButtons == false) {
			moveObjects(1, 0, 150);
			moveObjects(
					2,
					rootLayout.getMeasuredWidth()
							- btnTakeoff.getMeasuredWidth(), 150);
			moveObjects(
					3,
					rootLayout.getMeasuredWidth() / 2
							- powerBar.getMeasuredWidth() / 2, 170);
			moveObjects(
					4,
					rootLayout.getMeasuredWidth() / 2
							- tvSpeeed.getMeasuredWidth() / 2, 190);
			// hide all btns
			btnDown.setVisibility(View.GONE);
			btnUp.setVisibility(View.GONE);
			btnIdle.setVisibility(View.GONE);
			btnLeft.setVisibility(View.GONE);
			btnRigth.setVisibility(View.GONE);
			btnBack.setVisibility(View.GONE);
			btnForward.setVisibility(View.GONE);
			btnTurnLeft.setVisibility(View.GONE);
			btnTurnRight.setVisibility(View.GONE);
			btnLEDAnimation.setVisibility(View.GONE);
			hideButtons = true;
		} else {
			moveObjects(1, 530, 590);
			moveObjects(2, 985, 590);
			moveObjects(
					3,
					rootLayout.getMeasuredWidth() / 2
							- powerBar.getMeasuredWidth() / 2, 950);
			moveObjects(
					4,
					rootLayout.getMeasuredWidth() / 2
							- tvSpeeed.getMeasuredWidth() / 2, 800);
			btnDown.setVisibility(View.VISIBLE);
			btnUp.setVisibility(View.VISIBLE);
			btnIdle.setVisibility(View.VISIBLE);
			btnLeft.setVisibility(View.VISIBLE);
			btnRigth.setVisibility(View.VISIBLE);
			btnBack.setVisibility(View.VISIBLE);
			btnForward.setVisibility(View.VISIBLE);
			btnTurnLeft.setVisibility(View.VISIBLE);
			btnTurnRight.setVisibility(View.VISIBLE);
			btnLEDAnimation.setVisibility(View.VISIBLE);
			hideButtons = false;
		}

	}

	private void getAccelerometer(SensorEvent event) {
		// TODO Auto-generated method stub
		float[] values = event.values;
		// Movement
		float x = values[0];
		float y = values[1];
		float z = values[2];
		float accelationSquareRoot = (x * x + y * y + z * z)
				/ (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

		if (isDroneConnected && isOnGyroMode == true) {
			if (x <= -4) {
				// PRESSED
				droneControlService.setProgressiveCommandEnabled(true);
				droneControlService
						.setProgressiveCommandCombinedYawEnabled(true);
				onMoveForwardPressed(power);
			} else if (x >= 5) { // PRESSED
				droneControlService.setProgressiveCommandEnabled(true);
				droneControlService
						.setProgressiveCommandCombinedYawEnabled(true);
				onMoveBackwardPressed(power);
			}

			if (y <= -3) {
				droneControlService.setProgressiveCommandEnabled(true);
				droneControlService
						.setProgressiveCommandCombinedYawEnabled(true);
				onMoveLeftPressed(power);
			} else if (y >= 3) {
				droneControlService.setProgressiveCommandEnabled(true);
				droneControlService
						.setProgressiveCommandCombinedYawEnabled(true);
				onMoveRightPressed(power);
			}
		}

		long actualTime = System.currentTimeMillis();
		if (accelationSquareRoot >= 9) //
		{
			if (actualTime - lastUpdate < 5000 || isDroneConnected == false) {
				return;
			}

			// do flip here
			doLeftFlip();

			lastUpdate = actualTime;
			// Toast.makeText(this, "Device was shuffed",
			// Toast.LENGTH_SHORT).show();

			// 5 secs flip recharge
			mProgressBar.setProgress(chargeCount);
			mCountDownTimer = new CountDownTimer(6000, 1000) {

				@Override
				public void onTick(long millisUntilFinished) {
					Log.v("Log_tag", "Tick of Progress" + chargeCount
							+ millisUntilFinished);
					chargeCount++;
					mProgressBar.setProgress(chargeCount);
					flipStatus.setText("Drone flip rechaging ...");
				}

				@Override
				public void onFinish() {
					chargeCount++;
					mProgressBar.setProgress(chargeCount);
					flipStatus.setText("Flip Ready !");
					chargeCount = 0;
				}
			};
			mCountDownTimer.start();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("Drone", "onResume");

		// register accelerometer sensor
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);

		// initiate receiver to get battery and other info
		registerReceivers();

		LocalBroadcastManager manager = LocalBroadcastManager
				.getInstance(getApplicationContext());
		manager.registerReceiver(droneReadyReceiver, new IntentFilter(
				DroneControlService.DRONE_STATE_READY_ACTION));
		manager.registerReceiver(droneConnectionChangeReceiver,
				new IntentFilter(
						DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));

	}

	@Override
	public void onPause() {
		super.onPause();
		// TODO Auto-generated method stub
		unregisterReceivers();// unregister
		sensorManager.unregisterListener(this);// unregister
	}

	@Override
	public void onStop() {
		super.onStop();
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// TODO Auto-generated method stub

	}

	private boolean onConnectPressed() {
		Log.d("Drone", "onConnectPressed");
		return bindService(new Intent(this, DroneControlService.class),
				mConnection, Context.BIND_AUTO_CREATE);
	}

	private void onDisconnectPressed() {
		Log.d("Drone", "onDisconnectPressed");

		unbindService(mConnection);

		Toast.makeText(getApplicationContext(), "Disconnected to Drone",
				Toast.LENGTH_SHORT).show();

		enableButtons(false);

		Intent refresh = new Intent(MainActivity.this, SplashActivity.class);
		startActivity(refresh);
	}

	private void onTakeoffPressed() {
		Log.d("Drone", "onTakeoffPressed");
		droneControlService.triggerTakeOff();
	}

	// NEW ----------------------------------
	public void calibrateMagneto() {
		Log.d("Drone", "calibrateMagneto");
		droneControlService.calibrateMagneto();
	}

	public void trim() {
		Log.d("Drone", "calibrateMagneto");
		droneControlService.flatTrim();
	}

	public void doLeftFlip() {
		Log.d("Drone", "doLeftFlip");
		droneControlService.doLeftFlip();
	}

	public void onMoveLeftPressed(final float power) {
		Log.d("Drone", "moveLeft");
		droneControlService.moveLeft(power);
	}

	public void onMoveRightPressed(final float power) {
		Log.d("Drone", "moveRight");
		droneControlService.moveRight(power);
	}

	public void onMoveUpPressed(final float power) {
		Log.d("Drone", "moveUp");
		droneControlService.moveUp(power);
	}

	public void onMoveDownPressed(final float power) {
		Log.d("Drone", "moveDown");
		droneControlService.moveDown(power);
	}

	public void onMoveBackwardPressed(final float power) {
		Log.d("Drone", "moveBackward");
		droneControlService.moveBackward(power);
	}

	public void onMoveForwardPressed(final float power) {
		Log.d("Drone", "moveForward");
		droneControlService.moveForward(power);
	}

	public void onTurnLeftPressed(final float power) {
		Log.d("Drone", "turnLeft");
		droneControlService.turnLeft(power);
	}

	public void onTurnRightPressed(final float power) {
		Log.d("Drone", "turnRight");
		droneControlService.turnRight(power);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			droneControlService = ((DroneControlService.LocalBinder) service)
					.getService();
			onDroneServiceConnected();

			isDroneConnected = true;
			startAnimation();
			enableButtons(true);
		}

		public void onServiceDisconnected(ComponentName name) {
			droneControlService = null;
		}
	};

	/**
	 * Called when we connected to DroneControlService
	 */
	protected void onDroneServiceConnected() {
		if (droneControlService != null) {
			droneControlService.resume();
			droneControlService.requestDroneStatus();
		} else {
			Log.w("Drone",
					"DroneServiceConnected event ignored as DroneControlService is null");
		}

		Toast.makeText(getApplicationContext(), "connected to Drone",
				Toast.LENGTH_SHORT).show();

		// to calibrate the drone
		// droneControlService.flatTrim();

		// settingsDialog = new SettingsDialog(this, this, droneControlService,
		// magnetoAvailable);

		// applySettings(settings);

		// initListeners();
		// runTranscoding();

		// if (droneControlService.getMediaDir() != null) {
		// view.setRecordButtonEnabled(true);
		// view.setCameraButtonEnabled(true);
		// }
	}

	class ClearRenderer implements GLSurfaceView.Renderer {
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			// Do nothing special.]

		}

		public void onSurfaceChanged(GL10 gl, int w, int h) {
			gl.glViewport(0, 0, w, h);
		}

		public void onDrawFrame(GL10 gl) {
			gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		}
	}

	@Override
	public void onDroneFlyingStateChanged(boolean flying) {
		Log.d("Drone", "onDroneFlyingStateChanged");

	}

	@Override
	public void onDroneReady() {
		Log.d("Drone", "onDroneReady");
	}

	@Override
	public void onDroneConnected() {
		Log.d("Drone", "onDroneConnected");

	}

	@Override
	public void onDroneDisconnected() {
		Log.d("Drone", "onDroneDisconnected");

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			getAccelerometer(event);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}