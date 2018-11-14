package com.cid.sample.epassport;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cid.sample.epassport.models.DeviceFamily;
import com.cid.sample.epassport.models.DeviceType;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static com.cid.sample.epassport.models.DeviceType.CTAB_V4;
import static com.cid.sample.epassport.models.DeviceType.CTWO_V2;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

public class LaunchActivity
		extends Activity {

	private static final String TAG = LaunchActivity.class.getSimpleName();

	/* CredenceSDK biometrics object, used to interface with APIs. */
	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;
	/* Stores which Credence family of device's this app is running on. */
	private static DeviceFamily mDeviceFamily = DeviceFamily.CID_PRODUCT;
	/* Stores which specific device this app is running on. */
	private static DeviceType mDeviceType = DeviceType.CID_PRODUCT;

	public static BiometricsManager
	getBiometricsManager() {
		return mBiometricsManager;
	}

	@SuppressWarnings("unused")
	public static DeviceFamily
	getDeviceFamily() {
		return mDeviceFamily;
	}

	@SuppressWarnings("unused")
	public static DeviceType
	getDeviceType() {
		return mDeviceType;
	}

	/* Based on given product name, defines DeviceType and DeviceFamily.
	 *
	 * @param productName Product name returned via BiometricsManager.getProductName()
	 */
	@SuppressWarnings({"IfCanBeSwitch", "SpellCheckingInspection"})
	private void
	setDeviceType(String productName) {
		Log.d(TAG, "setDeviceType(" + productName + ")");

		if (productName == null || productName.length() == 0) {
			Log.e(TAG, "Invalid product name!");
			return;
		}

		if (productName.equals("Twizzler")) {
			mDeviceType = DeviceType.TWIZZLER;
			mDeviceFamily = DeviceFamily.TWIZZLER;
		} else if (productName.equals("Trident-1")) {
			mDeviceType = DeviceType.TRIDENT_1;
			mDeviceFamily = DeviceFamily.TRIDENT;
		} else if (productName.equals("Trident-2")) {
			mDeviceType = DeviceType.TRIDENT_2;
			mDeviceFamily = DeviceFamily.TRIDENT;
		} else if (productName.equals("Credence One V1")) {
			mDeviceType = DeviceType.CONE_V1;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence One V2")) {
			mDeviceType = DeviceType.CONE_V2;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence One V3")) {
			mDeviceType = DeviceType.CONE_V3;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence Two V1")) {
			mDeviceType = DeviceType.CTWO_V1;
			mDeviceFamily = DeviceFamily.CTWO;
		} else if (productName.equals("Credence Two V2")) {
			mDeviceType = CTWO_V2;
			mDeviceFamily = DeviceFamily.CTWO;
		} else if (productName.equals("Credence TAB V1")) {
			mDeviceType = DeviceType.CTAB_V1;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V2")) {
			mDeviceType = DeviceType.CTAB_V2;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V3")) {
			mDeviceType = DeviceType.CTAB_V3;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V4")) {
			mDeviceType = CTAB_V4;
			mDeviceFamily = DeviceFamily.CTAB;
		}
	}

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*  Create new biometrics object. */
		mBiometricsManager = new BiometricsManager(this);

		/* Initialize object, meaning tell CredenceService to bind to this application. */
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String minimumVersion,
												 String currentVersion) -> {
			if (resultCode == OK) {
				Toast.makeText(this, "Biometrics initialized.", LENGTH_SHORT).show();

				/* Save DeviceType/DeviceFamily so other activities may more easily identify on
				 * what devices they are running on. This is used for activities to set up their
				 * layouts, etc.
				 */
				this.setDeviceType(mBiometricsManager.getProductName());

				/* Launch main activity. */
				Intent intent = new Intent(this, MRZActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(intent);
			} else Toast.makeText(this, "Biometrics FAILED to initialize.", LENGTH_LONG).show();
		});
	}
}
