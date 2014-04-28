package com.integreight.onesheeld.shields.controller;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.integreight.firmatabluetooth.ShieldFrame;
import com.integreight.onesheeld.enums.UIShield;
import com.integreight.onesheeld.utils.ControllerParent;

public class TemperatureShield extends ControllerParent<TemperatureShield>
		implements SensorEventListener {
	public static final byte TEMPERATURE_VALUE = 0x01;
	private SensorManager mSensorManager;
	private Sensor mTemperature;
	private TemperatureEventHandler eventHandler;
	private ShieldFrame frame;
	Handler handler;
	int PERIOD = 100;
	boolean flag = false;
	boolean isHandlerLive = false;
	float oldInput = 0;
	boolean isFirstTime = true;

	private final Runnable processSensors = new Runnable() {
		@Override
		public void run() {
			// Do work with the sensor values.

			flag = true;
			// The Runnable is posted to run again here:
			if (handler != null)
				handler.postDelayed(this, PERIOD);
		}
	};

	public TemperatureShield() {
	}

	public TemperatureShield(Activity activity, String tag) {
		super(activity, tag);
	}

	@Override
	public ControllerParent<TemperatureShield> setTag(String tag) {

		return super.setTag(tag);
	}

	@Override
	public ControllerParent<TemperatureShield> invalidate(
			com.integreight.onesheeld.utils.ControllerParent.SelectionAction selectionAction,
			boolean isToastable) {
		this.selectionAction = selectionAction;
		mSensorManager = (SensorManager) getApplication().getSystemService(
				Context.SENSOR_SERVICE);
		mTemperature = mSensorManager
				.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		registerSensorListener(isToastable);
		return super.invalidate(selectionAction, isToastable);
	}

	public void setTemperatureEventHandler(TemperatureEventHandler eventHandler) {
		this.eventHandler = eventHandler;
		CommitInstanceTotable();
	}

	@Override
	public void onNewShieldFrameReceived(ShieldFrame frame) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (flag && (oldInput != event.values[0] || isFirstTime)) {
			isFirstTime = false;
			frame = new ShieldFrame(UIShield.TEMPERATURE_SHIELD.getId(),
					TEMPERATURE_VALUE);
			oldInput = event.values[0];
			frame.addByteArgument((byte) Math.round(event.values[0]));
			activity.getThisApplication().getAppFirmata()
					.sendShieldFrame(frame);
			Log.d("Sensor Data of X", event.values[0] + "");
			if (eventHandler != null)
				eventHandler.onSensorValueChangedFloat(event.values[0] + "");
			//
			flag = false;
		}

	}

	// Register a listener for the sensor.
	public void registerSensorListener(boolean isToastable) {
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
			// Success! There's sensor.
			if (!isHandlerLive) {
				handler = new Handler();
				if (mTemperature != null)
					mSensorManager.registerListener(this, mTemperature,
							SensorManager.SENSOR_DELAY_GAME);
				handler.post(processSensors);
				if (eventHandler != null)
					eventHandler.isDeviceHasSensor(true);
				isHandlerLive = true;
				if (selectionAction != null) {
					selectionAction.onSuccess();
				}
			} else {
				Log.d("Your Sensor is registered", "Temperature");
			}
		} else {
			// Failure! No sensor.
			Log.d("Device dos't have Sensor ", "Temperature");
			if (selectionAction != null) {
				selectionAction.onFailure();
			}
			if (isToastable)
				activity.showToast("Device dos't have Sensor");
			if (eventHandler != null)
				eventHandler.isDeviceHasSensor(false);

		}

	}

	// Unregister a listener for the sensor .
	public void unegisterSensorListener() {
		// mSensorManager.unregisterListener(this);
		if (mSensorManager != null && handler != null && mTemperature != null) {

			mSensorManager.unregisterListener(this, mTemperature);
			mSensorManager.unregisterListener(this);
			if (processSensors != null)
				handler.removeCallbacks(processSensors);
			handler.removeCallbacksAndMessages(null);
			isHandlerLive = false;
			frame = null;
		}
	}

	public static interface TemperatureEventHandler {

		void onSensorValueChangedFloat(String value);

		void onSensorValueChangedByte(String value);

		void isDeviceHasSensor(Boolean hasSensor);

	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		this.unegisterSensorListener();

	}

}
