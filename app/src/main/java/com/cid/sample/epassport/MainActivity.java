package com.cid.sample.epassport;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.EpassportReaderStatusListener;
import com.credenceid.biometrics.Biometrics.MRZStatusListener;
import com.credenceid.biometrics.Biometrics.OnEpassportCardStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzDocumentStatusListener;
import com.credenceid.biometrics.Biometrics.OnMrzReadListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.icao.ICAODocumentData;
import com.credenceid.icao.ICAOReadIntermediateCode;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressWarnings("unused")
@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
	private final static String mTAG = MainActivity.class.getSimpleName();

	private final int mDATE_OF_BIRTH = 0;
	private final int mDATE_OF_EXPIRY = 1;
	private final int mISSUER = 2;
	private final int mDOCUMENT_TYPE = 3;
	private final int mLAST_NAME = 4;
	private final int mFIRST_NAME = 5;
	private final int mNATIONALITY = 6;
	private final int mPRIMARY_IDENTIFIER = 7;
	private final int mSECONDARY_IDENTIFIER = 8;
	private final int mDOCUMENT_NUMBER = 9;
	private final int mGENDER = 10;

	private TextView mStatusTextView;
	private ImageView mICAOImageView;
	private TextView mICAOTextView;

	private BiometricsManager mBiometricsManager;

	// Listener invoked each time MRZ reader is able to read MRZ text from document.
	private OnMrzReadListener mOnMrzReadListener = (ResultCode resultCode,
													String hint,
													byte[] rawData,
													String data,
													String parsedData) -> {
		Log.d(mTAG, "OnMrzReadListener: Hit: " + hint);
		Log.d(mTAG, "OnMrzReadListener: ResultCode: " + resultCode.name());
		Log.d(mTAG, "OnMrzReadListener: Data: " + data);
		Log.d(mTAG, "OnMrzReadListener: ParsedData: " + parsedData);

		if (resultCode == FAIL)
			mStatusTextView.setText("FAILED to read MRZ, please re-swipe document.");
		else if (resultCode == INTERMEDIATE)
			mStatusTextView.setText("Reading MRZ, please wait...");
		else {
			// Once data is read, C-Service auto parses it and returns it as one big string of data.
			if (parsedData == null || parsedData.isEmpty()) {
				mStatusTextView.setText("FAILED to read MRZ, please re-swipe document.");
				return;
			}

			// Each section of data is separated by a "\r\n" character. If we split this data up, we
			// should have TEN elements of data. Please see the constants defined at the top of this
			// class to see the different pieces of information MRZ contains.
			final String[] splitData = parsedData.split("\r\n");
			if (splitData.length < 10) {
				mStatusTextView.setText("FAILED to read MRZ, please re-swipe document.");
				return;
			}

			mStatusTextView.setText("Successful MRZ read.");
			mICAOTextView.setText(parsedData);

			readICAODocument(splitData[mDATE_OF_BIRTH],
					splitData[mDOCUMENT_NUMBER],
					splitData[mDATE_OF_EXPIRY]);
		}
	};

	// Listener invoked each time C-Service detects a document change from MRZ reader.
	private OnMrzDocumentStatusListener mOnMrzDocumentStatusListener =
			(int previousState, int currentState) -> {
				// If currentState is not 2, then no document is present.
				if (currentState != 2) {
					Log.d(mTAG, "OnMrzDocumentStatusListener: No document present.");
					return;
				}

				mStatusTextView.setText("Reading MRZ string from document.");

				// If current state is 2, then a document is present on MRZ reader. If a document
				// is present we must read it to obtain MRZ field data. Once we have read MRZ we
				// can then pass along this information to the "readICAODocument()" API.
				//
				// When MRZ is read this callback is invoked "mOnMrzReadListener".
				mBiometricsManager.readMRZ(mOnMrzReadListener);
			};

	// Listener invoked each time C-Service detects a document change from EPassport reader.
	private OnEpassportCardStatusListener mOnEpassportCardStatusListener =
			(int previousState, int currentState) -> {
				// If currentState is not 2, then no document is present.
				if (currentState != 2) {
					Log.d(mTAG, "Document was removed, no document present.");
					return;
				}

				// TODO:
			};

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBiometricsManager = new BiometricsManager(this);

		this.initializeLayoutComponents();

		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String minimumVersion,
												 String currentVersion) -> {
			if (resultCode == OK) {
				mStatusTextView.setText("Biometrics initialized.");

				// Once Biometrics has initialized we may now open MRZ reader. Once MRZ reader is
				// opened, then EPassport reader may be opened.
				openMRZReader();
			} else mStatusTextView.setText("Biometrics FAILED to initialize, restart application.");
		});
	}

	@Override
	public void
	onBackPressed() {
		super.onBackPressed();

		// If user presses back button then close all open peripherals.
		mBiometricsManager.ePassportCloseCommand();
		mBiometricsManager.closeMRZ();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	@Override
	protected void
	onDestroy() {
		super.onDestroy();

		// If application is being killed then close all open peripherals.
		mBiometricsManager.ePassportCloseCommand();
		mBiometricsManager.closeMRZ();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	private void
	initializeLayoutComponents() {
		mStatusTextView = findViewById(R.id.status_textview);

		mICAOImageView = findViewById(R.id.icao_dg2_imageview);
		mICAOTextView = findViewById(R.id.icao_textview);
	}

	private void
	openMRZReader() {
		final String localTAG = mTAG + ":openMRZReader";
		Log.d(localTAG, "openMRZReader()");

		mStatusTextView.setText("Opening MRZ reader...");

		// Register a listener that will be invoked each time MRZ reader's status changes. Meaning
		// that anytime a document is placed/removed invoke this callback.
		mBiometricsManager.registerMrzDocumentStatusListener(mOnMrzDocumentStatusListener);

		// Once our callback is registered we may now open the reader.
		mBiometricsManager.openMRZ(new MRZStatusListener() {
			@Override
			public void onMRZOpen(ResultCode resultCode) {
				if (resultCode != OK) {
					mStatusTextView.setText("OpenMRZReader: FAILED");
					Log.w(localTAG, "OpenMRZReader: FAILED");
					return;
				}

				// If MRZ opened, now EPassport reader may be opened.
				openEPassportReader();
			}

			@Override
			public void onMRZClose(ResultCode resultCode,
								   CloseReasonCode closeReasonCode) {
				Log.d(localTAG, "MRZ reader closed: " + closeReasonCode.name());
			}
		});
	}

	private void
	openEPassportReader() {
		final String localTAG = mTAG + ":openEPassportReader";
		Log.d(localTAG, "openEPassportReader()");

		mStatusTextView.setText("Opening EPassport reader...");

		// Register a listener will be invoked each time EPassport reader's status changes. Meaning
		// that anytime a document is placed/removed invoke this callback.
		mBiometricsManager.registerEpassportCardStatusListener(mOnEpassportCardStatusListener);

		// Once our callback is registered we may now open the reader.
		mBiometricsManager.ePassportOpenCommand(new EpassportReaderStatusListener() {
			@Override
			public void onEpassportReaderOpen(Biometrics.ResultCode resultCode) {
				if (resultCode == FAIL) {
					mStatusTextView.setText("OpenEPassport: FAILED");
					Log.w(localTAG, "OpenEPassport: FAILED");
					return;
				}
				mStatusTextView.setText("OpenEPassport: OPENED");
				Log.d(localTAG, "OpenEPassport: OPENED");
			}

			@Override
			public void onEpassportReaderClosed(ResultCode resultCode,
												CloseReasonCode closeReasonCode) {
				Log.d(localTAG, "EPassport reader closed: " + closeReasonCode.name());
			}
		});
	}

	private void
	readICAODocument(String dateOfBirth,
					 String documentNumber,
					 String dateOfExpiry) {
		final String localTAG = mTAG + ":readICAODocument";
		Log.d(localTAG, "readICAODocument()");

		if (dateOfBirth == null || dateOfBirth.isEmpty()) {
			Log.w(localTAG, "DateOfBirth parameter INVALID, will not read ICAO document.");
			return;
		}
		if (documentNumber == null || documentNumber.isEmpty()) {
			Log.w(localTAG, "DocumentNumber parameter INVALID, will not read ICAO document.");
			return;
		}
		if (dateOfExpiry == null || dateOfExpiry.isEmpty()) {
			Log.w(localTAG, "DateOfExpiry parameter INVALID, will not read ICAO document.");
			return;
		}

		// DateOfBirth and DateOfExpiry must be in ""YYMMDD" format.
		mBiometricsManager.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry,
				(ResultCode resultCode,
				 ICAOReadIntermediateCode stage,
				 String hint,
				 ICAODocumentData icaoDocumentData) -> {
					Log.d(localTAG, "STAGE: " + stage.name()
							+ ", Status: "
							+ resultCode.name()
							+ "Hint: " + hint);
					Log.d(localTAG, "ICAODocumentData: " + icaoDocumentData.toString());

					// Display ICAODocumentData to UI for user to see.
					mICAOTextView.setText(icaoDocumentData.toString());

					// If DG2 state was succesfull then display read face image to ImageView.
					if (stage == ICAOReadIntermediateCode.DG2 && resultCode == OK)
						mICAOImageView.setImageBitmap(icaoDocumentData.dgTwo.faceImage);
				});
	}
}
