package de.ph1b.audiobook.features.sign_in

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Bundle
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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class SignInActivity : AppCompatActivity(), OnConnectionFailedListener,
        View.OnClickListener, ConnectionCallbacks {

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


    private fun saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.")
        val image = mBitmapToSave
        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(object : ResultCallback<DriveContentsResult> {

            override fun onResult(result: DriveContentsResult) {
                // If the operation was not successful, we cannot do anything
                // and must
                // fail.
                if (!result.status.isSuccess) {
                    Log.i(TAG, "Failed to create new contents.")
                    return
                }
                // Otherwise, we can write our data to the new contents.
                Log.i(TAG, "New contents created.")
                // Get an output stream for the contents.
                val outputStream = result.driveContents.outputStream
                // Write the bitmap data from it.
                val bitmapStream = ByteArrayOutputStream()
                image!!.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream)
                try {
                    outputStream.write(bitmapStream.toByteArray())
                } catch (e1: IOException) {
                    Log.i(TAG, "Unable to write file contents.")
                }

                // Create the initial metadata - MIME type and title.
                // Note that the user will be able to change the title later.
                val metadataChangeSet = MetadataChangeSet.Builder().setMimeType("image/jpeg").setTitle("Android Photo.png").build()
                // Create an intent for the file chooser, and start it.
                val intentSender = Drive.DriveApi.newCreateFileActivityBuilder().setInitialMetadata(metadataChangeSet).setInitialDriveContents(result.driveContents).build(mGoogleApiClient)
                try {
                    startIntentSenderForResult(
                            intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0)
                } catch (e: IntentSender.SendIntentException) {
                    Log.i(TAG, "Failed to launch file chooser.")
                }

            }
        })
    }

    protected override fun onResume() {
        super.onResume()
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = GoogleApiClient.Builder(this).addApi(Drive.API).addScope(Drive.SCOPE_FILE).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build()
        }
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient!!.connect()
    }

    protected override fun onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.disconnect()
        }
        super.onPause()
    }

    protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CODE_CAPTURE_IMAGE ->
                // Called after a photo has been taken.
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = data.extras.get("data") as Bitmap
                }
            REQUEST_CODE_CREATOR ->
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.")
                    mBitmapToSave = null
                    // Just start the camera again for another photo.
                    startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                            REQUEST_CODE_CAPTURE_IMAGE)
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
//        if (mBitmapToSave == null) {
//            // This activity has no UI of its own. Just start the camera.
//            startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE),
//                    REQUEST_CODE_CAPTURE_IMAGE)
//            return
//        }
//        saveFileToDrive()


        val changeSet = MetadataChangeSet.Builder().setTitle("MaterialAudiobookPlayer").build()
        Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                mGoogleApiClient, changeSet).setResultCallback(callback)
        Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();

        val query = Query.Builder().addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain")).build()
        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(metadataCallback)
    }

    private val metadataCallback = object : ResultCallback<DriveApi.MetadataBufferResult> {
        override fun onResult(result: DriveApi.MetadataBufferResult) {
            if (!result.getStatus().isSuccess()) {
                Log.i(TAG, "Problem while retrieving results")
                return
            }
            mResultsAdapter?.clear()
            mResultsAdapter?.append(result.getMetadataBuffer())
        }
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

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // [END signIn]

    // [START signOut]
    private fun signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback {
            // [START_EXCLUDE]
            updateUI(false)
            // [END_EXCLUDE]
        }
    }
    // [END signOut]

    // [START revokeAccess]
    private fun revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback {
            // [START_EXCLUDE]
            updateUI(false)
            // [END_EXCLUDE]
        }
    }
    // [END revokeAccess]

    private fun showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(this)
            mProgressDialog!!.setMessage(getString(R.string.loading))
            mProgressDialog!!.isIndeterminate = true
        }

        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.hide()
        }
    }

    private fun updateUI(signedIn: Boolean) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).visibility = View.GONE
            findViewById(R.id.sign_out_and_disconnect).visibility = View.VISIBLE
        } else {
            mStatusTextView!!.setText(R.string.signed_out)

            findViewById(R.id.sign_in_button).visibility = View.VISIBLE
            findViewById(R.id.sign_out_and_disconnect).visibility = View.GONE
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.sign_in_button -> signIn()
            R.id.sign_out_button -> signOut()
        }
    }

    companion object {
        private val RC_SIGN_IN = 9001
    }


    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        if (activeNetwork != null && activeNetwork.isConnected) {
            try {
                val url = URL("http://www.google.com/")
                val urlc = url.openConnection() as HttpURLConnection
                urlc.setRequestProperty("User-Agent", "test")
                urlc.setRequestProperty("Connection", "close")
                urlc.connectTimeout = 1000 // mTimeout is in seconds
                urlc.connect()
                if (urlc.responseCode == 200) {
                    return true
                } else {
                    return false
                }
            } catch (e: IOException) {
                Log.i("warning", "Error checking internet connection", e)
                return false
            }

        }

        return false

    }

    fun isNetworkAvailable(handler: Handler, timeout: Int, context: Context) {
        // ask fo message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be send before before within the 'timeout' (in milliseconds)

        object : Thread() {
            private var responded = false

            override fun run() {
                // set 'responded' to TRUE if is able to connect with google mobile (responds fast)
                object : Thread() {
                    override fun run() {
                        try {
                            responded = isConnected(context)
                        } catch (e: Exception) {
                        }

                    }
                }.start()

                try {
                    var waited = 0
                    while (!responded && waited < timeout) {
                        Thread.sleep(100)
                        if (!responded) {
                            waited += 100
                        }
                    }
                } catch (e: InterruptedException) {
                } // do nothing
                finally {
                    if (!responded) {
                        handler.sendEmptyMessage(0)
                    } else {
                        handler.sendEmptyMessage(1)
                    }

                }
            }
        }.start()
    }
}