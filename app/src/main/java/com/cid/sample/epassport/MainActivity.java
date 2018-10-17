package com.cid.sample.epassport;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.EpassportReaderStatusListener;
import com.credenceid.biometrics.Biometrics.OnEpassportCardStatusListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.icao.ICAODocumentData;
import com.credenceid.icao.ICAOReadIntermediateCode;

public class MainActivity extends AppCompatActivity {
	private final static String mTAG = MainActivity.class.getSimpleName();

	private BiometricsManager mBiometricsManager;
	private Context mContext;

	// Listener invoked each time C-Service detects a document change from EPassport reader.
	private OnEpassportCardStatusListener mOnEpassportCardStatusListener =
			(int previousState, int currentState) -> {
				// If currentState is not 2, then no document is present.
				if (currentState != 2) {
					Log.d(mTAG, "Document was removed, no document present.");
					return;
				}

				// If current state is 2, then a document is present on EPassport reader.
				readICAODocument();
			};

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mContext = this;
		mBiometricsManager = new BiometricsManager(this);

		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String minimumVersion,
												 String currentVersion) -> {
			if (resultCode == Biometrics.ResultCode.OK) {
				Toast.makeText(mContext, "BM Initialized.", Toast.LENGTH_SHORT).show();

				// Once Biometrics has initialized we may now open EPassport reader.
				openEPassportReader();
			}
		});
	}

	@Override
	public void
	onBackPressed() {
		super.onBackPressed();

		// If user presses back button then close EPassport reader.
		mBiometricsManager.ePassportCloseCommand();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	private void
	openEPassportReader() {
		final String localTag = mTAG + ":openEPassportReader";
		Log.d(mTAG, "openEPassportReader()");

		// Register a listener will be invoked each time EPassport reader's status changes. Meaning
		// that anytime a document is placed/removed invoke this callback.
		mBiometricsManager.registerEpassportCardStatusListener(mOnEpassportCardStatusListener);

		// Once our callback is registered we may now
		mBiometricsManager.ePassportOpenCommand(new EpassportReaderStatusListener() {
			@Override
			public void onEpassportReaderOpen(Biometrics.ResultCode resultCode) {
				if (resultCode == Biometrics.ResultCode.FAIL) {
					Log.w(localTag, "OpenEPassport: FAILED");
					return;
				}
				Log.d(localTag, "OpenEPassport: OPENED");
			}

			@Override
			public void onEpassportReaderClosed(ResultCode resultCode,
												CloseReasonCode closeReasonCode) {
				Log.d(localTag, "EPassport reader closed: " + closeReasonCode.name());
			}
		});
	}

	@SuppressWarnings("SpellCheckingInspection")
	private void
	readICAODocument() {
		final String localTag = mTAG + ":readICAODocument";
		Log.d(mTAG, "readICAODocument()");

		//								  DOB: yymmdd  Doc. Num.  DOE: yymmdd
		mBiometricsManager.readICAODocument("791022", "17FV09900", "270621",
				(ResultCode resultCode,
				 ICAOReadIntermediateCode stage,
				 String hint,
				 ICAODocumentData icaoDocumentData) -> {
					Log.d(localTag, "STAGE: " + stage.name()
							+ ", Status: "
							+ resultCode.name()
							+ "Hint: " + hint);
					Log.d(localTag, "ICAODocumentData: " + icaoDocumentData.toString());

					// Display ICAODocumentData to UI for user to see.
					((TextView) findViewById(R.id.textview)).setText(icaoDocumentData.toString());

					// If DG2 state was succesfull then display read face image to ImageView.
					if (stage == ICAOReadIntermediateCode.DG2 && resultCode == Biometrics.ResultCode.OK)
						((ImageView) findViewById(R.id.image)).setImageBitmap(icaoDocumentData.dgTwo.faceImage);
				});
	}
}
