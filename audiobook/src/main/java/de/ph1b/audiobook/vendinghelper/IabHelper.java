/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.ph1b.audiobook.vendinghelper;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

import java.util.List;

import timber.log.Timber;


/**
 * Provides convenience methods for in-app billing. You can create one instance of this
 * class for your application and use it to process in-app billing operations.
 * It provides synchronous (blocking) and asynchronous (non-blocking) methods for
 * many common in-app billing operations, as well as automatic signature
 * verification.
 * <p>
 * After instantiating, you must perform setup in order to start using the object.
 * To perform setup, call the {@link #startSetup} method and provide a listener;
 * that listener will be notified when setup is complete, after which (and not before)
 * you may call other methods.
 * <p>
 * After setup is complete, you will typically want to request an inventory of owned
 * items and subscriptions. See queryInventory, queryInventoryAsync
 * and related methods.
 * <p>
 * When you are done with this object, don't forget to call {@link #dispose}
 * to ensure proper cleanup. This object holds a binding to the in-app billing
 * service, which will leak unless you dispose of it correctly. If you created
 * the object on an Activity's onCreate method, then the recommended
 * place to dispose of it is the Activity's onDestroy method.
 * <p>
 * A note about threading: When using this object from a background thread, you may
 * call the blocking versions of methods; when using from a UI thread, call
 * only the asynchronous versions and handle the results via callbacks.
 * Also, notice that you can only call one asynchronous operation at a time;
 * attempting to start a second asynchronous operation while the first one
 * has not yet completed will result in an exception being thrown.
 *
 * @author Bruno Oliveira (Google)
 */
public class IabHelper {
    // Billing response codes
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    // Item types
    public static final String ITEM_TYPE_INAPP = "inapp";
    private static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    // IAB Helper error codes
    private static final int IABHELPER_ERROR_BASE = -1000;
    private static final int IABHELPER_REMOTE_EXCEPTION = -1001;
    private static final int IABHELPER_BAD_RESPONSE = -1002;
    private static final int IABHELPER_VERIFICATION_FAILED = -1003;
    private static final int IABHELPER_SEND_INTENT_FAILED = -1004;
    private static final int IABHELPER_USER_CANCELLED = -1005;
    private static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;
    private static final int IABHELPER_UNKNOWN_ERROR = -1008;
    private static final int IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009;
    // Keys for the responses from InAppBillingService
    private static final String RESPONSE_CODE = "RESPONSE_CODE";
    private static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    private static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    private static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    private static final String ITEM_TYPE_SUBS = "subs";
    // some fields on the getSkuDetails response bundle
    // Is setup done?
    private boolean mSetupDone = false;
    // Has this object been disposed of? (If so, we should ignore callbacks, etc)
    private boolean mDisposed = false;
    // Are subscriptions supported?
    private boolean mSubscriptionsSupported = false;
    // Is an asynchronous operation in progress?
    // (only one at a time can be in progress)
    private boolean mAsyncInProgress = false;
    // (for logging/debugging)
    // if mAsyncInProgress == true, what asynchronous operation is in progress?
    private String mAsyncOperation = "";
    // Context we were passed during initialization
    private Context mContext;
    // Connection to the service
    private IInAppBillingService mService;
    private ServiceConnection mServiceConn;
    // The request code used to launch purchase flow
    private int mRequestCode;
    // The item type of the current purchase flow
    private String mPurchasingItemType;
    // Public key for verifying signature, in base64 encoding
    private String mSignatureBase64 = null;
    // The listener registered on launchPurchaseFlow, which we have to call back when
    // the purchase finishes
    private OnIabPurchaseFinishedListener mPurchaseListener;
    private boolean mBound = false;

    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does not
     * block and is safe to call from a UI thread.
     *
     * @param ctx             Your application or Activity context. Needed to bind to the in-app billing service.
     * @param base64PublicKey Your application's public key, encoded in base64.
     *                        This is used for verification of purchase signatures. You can find your app's base64-encoded
     *                        public key in your application's page on Google Play Developer Console. Note that this
     *                        is NOT your "developer public key".
     */
    public IabHelper(Context ctx, String base64PublicKey) {
        mContext = ctx.getApplicationContext();
        mSignatureBase64 = base64PublicKey;
        Timber.d("IAB helper created.");
    }

    /**
     * Returns a human-readable description for the given response code.
     *
     * @param code The response code
     * @return A human-readable string explaining the result code.
     * It also includes the result code numerically.
     */
    public static String getResponseDesc(int code) {
        String[] iab_msgs = ("0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = ("0:OK/-1001:Remote exception during initialization/" +
                "-1002:Bad response received/" +
                "-1003:Purchase signature verification failed/" +
                "-1004:Send intent failed/" +
                "-1005:User cancelled/" +
                "-1006:Unknown purchase response/" +
                "-1007:Missing token/" +
                "-1008:Unknown error/" +
                "-1009:Subscriptions not available/" +
                "-1010:Invalid consumption attempt").split("/");

        if (code <= IABHELPER_ERROR_BASE) {
            int index = IABHELPER_ERROR_BASE - code;
            if (index >= 0 && index < iabhelper_msgs.length) return iabhelper_msgs[index];
            else return String.valueOf(code) + ":Unknown IAB Helper Error";
        } else if (code < 0 || code >= iab_msgs.length) {
            return String.valueOf(code) + ":Unknown";
        } else
            return iab_msgs[code];
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    private static int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            Timber.d("Bundle with null response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Long) return (int) ((Long) o).longValue();
        else {
            logError("Unexpected type for bundle response code.");
            logError(o.getClass().getName());
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    // Workaround to bug where sometimes response codes come as Long instead of Integer
    private static int getResponseCodeFromIntent(Intent i) {
        Object o = i.getExtras().get(RESPONSE_CODE);
        if (o == null) {
            logError("Intent with no response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Long) return (int) ((Long) o).longValue();
        else {
            logError("Unexpected type for intent response code.");
            logError(o.getClass().getName());
            throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
        }
    }

    private static void logError(String msg) {
        String mDebugTag = "IabHelper";
        Log.e(mDebugTag, "In-app billing error: " + msg);
    }

    /**
     * Starts the setup process. This will start up the setup process asynchronously.
     * You will be notified through the listener when the setup process is complete.
     * This method is safe to call from a UI thread.
     *
     * @param listener The listener to notify when the setup process is complete.
     */
    public void startSetup(final OnIabSetupFinishedListener listener) {
        // If already set up, can't do it again.
        checkNotDisposed();
        if (mSetupDone) throw new IllegalStateException("IAB helper is already set up.");

        // Connection to IAB service
        Timber.d("Starting in-app billing setup.");
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Timber.d("Billing service disconnected.");
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (mDisposed) return;
                Timber.d("Billing service connected.");
                mService = IInAppBillingService.Stub.asInterface(service);
                String packageName = mContext.getPackageName();
                try {
                    Timber.d("Checking for in-app billing 3 support.");

                    // check for in-app billing v3 support
                    int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) listener.onIabSetupFinished(new IabResult(response,
                                "Error checking for billing v3 support."));

                        // if in-app purchases aren't supported, neither are subscriptions.
                        mSubscriptionsSupported = false;
                        return;
                    }
                    Timber.d("In-app billing version 3 supported for %s", packageName);

                    // check for v3 subscriptions support
                    response = mService.isBillingSupported(3, packageName, ITEM_TYPE_SUBS);
                    if (response == BILLING_RESPONSE_RESULT_OK) {
                        Timber.d("Subscriptions AVAILABLE.");
                        mSubscriptionsSupported = true;
                    } else {
                        Timber.d("Subscriptions NOT AVAILABLE. Response=%s", response);
                    }

                    mSetupDone = true;
                } catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION,
                                "RemoteException while setting up in-app billing."));
                    }
                    e.printStackTrace();
                    return;
                }

                if (listener != null) {
                    listener.onIabSetupFinished(new IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful."));
                }
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        List<ResolveInfo> intentServices = mContext.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (intentServices != null && !intentServices.isEmpty()) {
            // service available to handle that Intent
            mBound = mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        } else {
            // no service available to handle that Intent
            if (listener != null) {
                listener.onIabSetupFinished(
                        new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                                "Billing service unavailable on device."));
            }
        }
    }

    /**
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
     * used by it such as service connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() {
        Timber.d("Disposing.");
        mSetupDone = false;
        if (mServiceConn != null && mBound) {
            Timber.d("Unbinding from service.");
            if (mContext != null) {
                mContext.unbindService(mServiceConn);
                mBound = false;
            }
        }
        mDisposed = true;
        mContext = null;
        mServiceConn = null;
        mService = null;
        mPurchaseListener = null;
    }

    private void checkNotDisposed() {
        if (mDisposed) {
            throw new IllegalStateException("IabHelper was disposed of, so it cannot be used.");
        }
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up the Google Play screen. The calling activity will be paused while
     * the user interacts with Google Play, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param act      The calling activity.
     * @param sku      The sku of the item to purchase.
     * @param listener The listener to notify when the purchase process finishes
     */
    public void launchPurchaseFlow(Activity act, String sku,
                                   OnIabPurchaseFinishedListener listener) {
        checkNotDisposed();
        checkSetupDone("launchPurchaseFlow");
        flagStartAsync();
        IabResult result;

        if (ITEM_TYPE_INAPP.equals(ITEM_TYPE_SUBS) && !mSubscriptionsSupported) {
            IabResult r = new IabResult(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE,
                    "Subscriptions are not available.");
            flagEndAsync();
            if (listener != null) listener.onIabPurchaseFinished(r);
            return;
        }

        try {
            Timber.d("Constructing buy intent for %s, item type is %s", IabHelper.ITEM_TYPE_INAPP, sku);
            Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), sku, IabHelper.ITEM_TYPE_INAPP, "");
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logError("Unable to buy item, Error response: " + getResponseDesc(response));
                flagEndAsync();
                result = new IabResult(response, "Unable to buy item");
                if (listener != null) listener.onIabPurchaseFinished(result);
                return;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
            Timber.d("Launching buy intent for %s with request code %d", sku, 10001);
            mRequestCode = 10001;
            mPurchaseListener = listener;
            mPurchasingItemType = IabHelper.ITEM_TYPE_INAPP;
            assert pendingIntent != null;
            act.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    10001, new Intent(),
                    0, 0,
                    0);
        } catch (SendIntentException e) {
            logError("SendIntentException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            flagEndAsync();

            result = new IabResult(IABHELPER_SEND_INTENT_FAILED, "Failed to send intent.");
            if (listener != null) listener.onIabPurchaseFinished(result);
        } catch (RemoteException e) {
            logError("RemoteException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            flagEndAsync();

            result = new IabResult(IABHELPER_REMOTE_EXCEPTION, "Remote exception while starting purchase flow");
            if (listener != null) listener.onIabPurchaseFinished(result);
        }
    }

    /**
     * Handles an activity result that's part of the purchase flow in in-app billing. If you
     * are calling {@link #launchPurchaseFlow}, then you must call this method from your
     * Activity's {@link android.app.Activity@onActivityResult} method. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param requestCode The requestCode as you received it.
     * @param resultCode  The resultCode as you received it.
     * @param data        The data (Intent) as you received it.
     *                    false if the result was not related to a purchase, in which case you should
     *                    handle it normally.
     */
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        IabResult result;
        if (requestCode != mRequestCode) return;

        checkNotDisposed();
        checkSetupDone("handleActivityResult");

        // end of async purchase operation that started on launchPurchaseFlow
        flagEndAsync();

        if (data == null) {
            logError("Null data in IAB activity result.");
            result = new IabResult(IABHELPER_BAD_RESPONSE, "Null data in IAB result");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result);
            return;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if (resultCode == Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
            Timber.d("Successful resultcode from purchase activity.");
            Timber.d("Purchase data %s", purchaseData);
            Timber.d("Data signature %s", dataSignature);
            Timber.d("Extras %s", data.getExtras());
            Timber.d("Expected item type %s", mPurchasingItemType);

            if (purchaseData == null || dataSignature == null) {
                logError("BUG: either purchaseData or dataSignature is null.");
                Timber.d("Extras %s", data.getExtras().toString());
                result = new IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature");
                if (mPurchaseListener != null)
                    mPurchaseListener.onIabPurchaseFinished(result);
                return;
            }

            Purchase purchase;
            try {
                purchase = new Purchase(mPurchasingItemType, purchaseData, dataSignature);
                String sku = purchase.getSku();

                // Verify signature
                if (!Security.verifyPurchase(mSignatureBase64, purchaseData, dataSignature)) {
                    logError("Purchase signature verification FAILED for sku " + sku);
                    result = new IabResult(IABHELPER_VERIFICATION_FAILED, "Signature verification failed for sku " + sku);
                    if (mPurchaseListener != null)
                        mPurchaseListener.onIabPurchaseFinished(result);
                    return;
                }
                Timber.d("Purchase signature successfully verified.");
            } catch (JSONException e) {
                logError("Failed to parse purchase data.");
                e.printStackTrace();
                result = new IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data.");
                if (mPurchaseListener != null)
                    mPurchaseListener.onIabPurchaseFinished(result);
                return;
            }

            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(new IabResult(BILLING_RESPONSE_RESULT_OK, "Success"));
            }
        } else if (resultCode == Activity.RESULT_OK) {
            // result code was OK, but in-app billing response was not OK.
            Timber.d("Result code was OK but in-app billing response was not OK: %s", getResponseDesc(responseCode));
            if (mPurchaseListener != null) {
                result = new IabResult(responseCode, "Problem purchashing item.");
                mPurchaseListener.onIabPurchaseFinished(result);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("Purchase canceled - Response: %s", getResponseDesc(responseCode));
            result = new IabResult(IABHELPER_USER_CANCELLED, "User canceled.");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result);
        } else {
            logError("Purchase failed. Result code: " + Integer.toString(resultCode)
                    + ". Response: " + getResponseDesc(responseCode));
            result = new IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");
            if (mPurchaseListener != null) mPurchaseListener.onIabPurchaseFinished(result);
        }
    }

    // Checks that setup was done; if not, throws an exception.
    private void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logError("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    private void flagStartAsync() {
        if (mAsyncInProgress) throw new IllegalStateException("Can't start async operation (" +
                "launchPurchaseFlow" + ") because another async operation(" + mAsyncOperation + ") is in progress.");
        mAsyncOperation = "launchPurchaseFlow";
        mAsyncInProgress = true;
        Timber.d("Starting async operation: launchPurchaseFlow");
    }

    private void flagEndAsync() {
        Timber.d("Ending async operation %s", mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    /**
     * Callback for setup process. This listener's {@link #onIabSetupFinished} method is called
     * when the setup process is complete.
     */
    public interface OnIabSetupFinishedListener {
        /**
         * Called to notify that setup is complete.
         *
         * @param result The result of the setup process.
         */
        void onIabSetupFinished(IabResult result);
    }

    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnIabPurchaseFinishedListener {
        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the sku parameter specifies which item was purchased. If the purchase failed,
         * the sku and extraData parameters may or may not be null, depending on how far the purchase
         * process went.
         *
         * @param result The result of the purchase.
         */
        void onIabPurchaseFinished(IabResult result);
    }
}
