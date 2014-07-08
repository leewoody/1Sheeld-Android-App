package com.integreight.onesheeld.shields.controller;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import com.integreight.firmatabluetooth.ShieldFrame;
import com.integreight.onesheeld.Log;
import com.integreight.onesheeld.enums.UIShield;
import com.integreight.onesheeld.model.ArduinoConnectedPin;
import com.integreight.onesheeld.utils.ControllerParent;

public class SpeakerShield extends ControllerParent<ControllerParent<?>> {
	private final String VOLUME_PREF_KEY = "buzzerVolume";
	private final int MAX_VOLUME = 100;
	private SpeakerEventHandler eventHandler;
	private static final byte BUZZER_ON = (byte) 0x01;
	private static final byte BUZZER_OFF = (byte) 0x00;
	private boolean isResumed = false;
	public int connectedPin = -1;
	private boolean isLedOn;
	private MediaPlayer mp;

	public SpeakerShield() {
		super();
		requiredPinsIndex = 0;
		shieldPins = new String[] { "Buzzer" };
	}

	public SpeakerShield(Activity activity, String tag) {
		super(activity, tag);
	}

	public boolean refreshLed() {
		if (connectedPin != -1)
			isLedOn = getApplication().getAppFirmata()
					.digitalRead(connectedPin);
		else
			isLedOn = false;
		if (isLedOn)
			playSound();
		else
			stopBuzzer();
		return isLedOn;
	}

	@Override
	public void onDigital(int portNumber, int portData) {
		refreshLed();
		super.onDigital(portNumber, portData);
	}

	@Override
	public void setConnected(ArduinoConnectedPin... pins) {
		this.connectedPin = pins[0].getPinID();
		super.setConnected(pins);
	}

	public void setSpeakerEventHandler(SpeakerEventHandler eventHandler) {
		this.eventHandler = eventHandler;
		CommitInstanceTotable();
	}

	public static interface SpeakerEventHandler {
		void onSpeakerChange(boolean isOn);
	}

	@Override
	public void onNewShieldFrameReceived(ShieldFrame frame) {

		if (frame.getShieldId() == UIShield.BUZZER_SHIELD.getId()) {
			byte argumentValue = frame.getArgument(0)[0];
			switch (argumentValue) {
			case BUZZER_ON:
				// turn on bin
				playSound();
				if (isResumed)
					if (eventHandler != null)
						eventHandler.onSpeakerChange(true);
				break;
			case BUZZER_OFF:
				// turn off bin
				stopBuzzer();
				if (eventHandler != null && isResumed)
					eventHandler.onSpeakerChange(false);
				break;
			default:
				break;
			}

		}
	}

	public void doOnResume() {
		isResumed = true;
	}

	String uri;
	private boolean isPrepared = false;

	public synchronized void playSound() {
		uri = null;// getApplication().getBuzzerSound();
		if (mp == null) {
			if (uri == null) {
				mp = new MediaPlayer();
				final float volume = (float) (1 - (Math.log(MAX_VOLUME
						- getBuzzerVolume()) / Math.log(MAX_VOLUME)));
				mp.setVolume(volume, volume);
				try {
					mp.setDataSource(activity.getAssets()
							.openFd("buzzer_sound.mp3").getFileDescriptor());
					mp.prepareAsync();
					mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

						@Override
						public void onPrepared(MediaPlayer mp) {
							isPrepared = true;
							mp.start();
						}
					});
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					Log.e("TAG", "speaker::setVolume::setDataSource", e);
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					Log.e("TAG", "speaker::setVolume::setDataSource", e);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e("TAG", "speaker::setVolume::setDataSource", e);
				}
			} else {
				mp = new MediaPlayer();
				final float volume = (float) (1 - (Math.log(MAX_VOLUME
						- getBuzzerVolume()) / Math.log(MAX_VOLUME)));
				mp.setVolume(volume, volume);
				if (uri != null)
					try {
						mp = MediaPlayer.create(activity, Uri.parse(uri));
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						Toast.makeText(
								activity,
								"Can't play the current buzz! Please, replace it",
								Toast.LENGTH_SHORT).show();
						Log.e("TAG", "speaker::setVolume::setDataSource", e);
					} catch (SecurityException e) {
						Toast.makeText(
								activity,
								"Can't play the current buzz! Please, replace it",
								Toast.LENGTH_SHORT).show();
						// TODO Auto-generated catch block
						Log.e("TAG", "speaker::setVolume::setDataSource", e);
					} catch (IllegalStateException e) {
						Toast.makeText(
								activity,
								"Can't play the current buzz! Please, replace it",
								Toast.LENGTH_SHORT).show();
						// TODO Auto-generated catch block
						Log.e("TAG", "speaker::setVolume::setDataSource", e);
					}
			}
		}
		mp.setLooping(true);
		if (!mp.isPlaying() && isPrepared) {
			mp.start();
		}
	}

	public synchronized void stopBuzzer() {
		if (mp != null && mp.isPlaying()) {
			mp.setLooping(false);
			mp.stop();
			mp.release();
			mp = null;
			isPrepared = false;
		}
	}

	@Override
	public void reset() {
		if (mp != null) {
			if (mp.isPlaying())
				mp.stop();
			mp.release();
		}
		mp = null;
	}

	public int getBuzzerVolume() {
		return getApplication().getAppPreferences().getInt(VOLUME_PREF_KEY, 50);
	}

	public synchronized void setBuzzerVolume(int vol) {
		getApplication().getAppPreferences().edit()
				.putInt(VOLUME_PREF_KEY, vol).commit();
		final float volume = (float) (1 - (Math.log(MAX_VOLUME - vol) / Math
				.log(MAX_VOLUME)));
		if (mp != null) {
			try {
				mp.setVolume(volume, volume);
			} catch (IllegalStateException e) {

			}
		}
	}
}
