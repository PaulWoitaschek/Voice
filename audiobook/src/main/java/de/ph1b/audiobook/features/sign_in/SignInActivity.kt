package de.ph1b.audiobook.features.sign_in

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import de.ph1b.audiobook.R

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveFolder
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import de.ph1b.audiobook.uitools.ResultsAdapter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class SignInActivity : AppCompatActivity(), OnConnectionFailedListener,
        ConnectionCallbacks {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mStatusTextView: TextView? = null
    private var mProgressDialog: ProgressDialog? = null

    private val TAG = "SignInActivity"
    private val REQUEST_CODE_CAPTURE_IMAGE = 1
    private val REQUEST_CODE_CREATOR = 2
    private val REQUEST_CODE_RESOLUTION = 3
    private var mBitmapToSave: Bitmap? = null

    private var mResultsListView: ListView? = null
    private var mResultsAdapter: ResultsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGoogleApiClient = GoogleApiClient.Builder(this).addApi(Drive.API).addScope(Drive.SCOPE_FILE).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
    }

//    override fun onResume() {
//        super.onResume()
//        if (mGoogleApiClient == null) {
//            // Create the API client and bind it to an instance variable.
//            // We use this instance as the callback for connection and connection
//            // failures.
//            // Since no account name is passed, the user is prompted to choose.
//            mGoogleApiClient = GoogleApiClient.Builder(this).addApi(Drive.API).addScope(Drive.SCOPE_FILE).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()
//        }
//        // Connect the client. Once connected, the camera is launched.
//        mGoogleApiClient!!.connect()
//    }

//    override fun onPause() {
//        if (mGoogleApiClient != null) {
//            mGoogleApiClient!!.disconnect()
//        }
//        super.onPause()
//    }

    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CODE_RESOLUTION ->
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient!!.connect()
                }
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString())
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.errorCode, 0).show()
            return
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION)
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Exception while starting resolution activity", e)
        }
    }

    override fun onConnected(@Nullable p0: Bundle?) {
        Log.i(TAG, "API client connected.")


        val changeSet = MetadataChangeSet.Builder().setTitle("MaterialAudiobookPlayer").build()
        Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                mGoogleApiClient, changeSet).setResultCallback(callback)

//        val folder = File(Environment.getExternalStorageDirectory().path + File.separator + "MAPGoogleDrive")
//
//        var success = true
//        if (!folder.exists()) {
//            success = folder.mkdirs()
//
//            if (success) {
//                // Do something on success
//                Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
//            } else {
//                // Do something else on failure
//                Toast.makeText(this, "Folder creation failed", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show();
//        }



//        val query = Query.Builder().addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain")).build()
//        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(metadataCallback)
    }

    val callback: ResultCallback<DriveFolder.DriveFolderResult> = object : ResultCallback<DriveFolder.DriveFolderResult> {
        override fun onResult(result: DriveFolder.DriveFolderResult) {
            if (!result.getStatus().isSuccess()) {
                Log.i(TAG, "Error while trying to create the folder")
                return
            }
            Log.i(TAG, "Created a folder: " + result.getDriveFolder().getDriveId())
        }
    }

    override fun onConnectionSuspended(cause: Int) {
        Log.i(TAG, "GoogleApiClient connection suspended")
    }
}
