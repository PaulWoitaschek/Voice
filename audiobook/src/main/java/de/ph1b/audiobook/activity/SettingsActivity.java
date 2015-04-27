package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.vendinghelper.IabHelper;
import de.ph1b.audiobook.vendinghelper.IabResult;
import de.ph1b.audiobook.vendinghelper.Purchase;

public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        iabHelper = new IabHelper(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApfo7lNYf9Mh" +
                "GiHAZO8iG/LX3SDGg7Gv7s41FEf08rxCuIuE+6QdQ0u+yZEoirislWV7jMqHY3XlyJMrH+/nKqrtYgw" +
                "qnFtwuwckS/5R+0dtSKL4F/aVm6a3p00BtCjqe7tXrEg90gpVk59p5qr1cOnOAAc/xmerFG9VCv8QHw" +
                "I9arlShCcXz7eTKemxjkHMO3dTkTKDjYZMIozr0t9qTvTxPz1aV6TWAGs5E6Dt7UF78pntgG9bMwmIgL" +
                "N6fOYuBaKd8IxA3iQ5IhWGVB8WG65Ax+u0RXsx0r8BC53JQq91lItka7b1OeBe6uPHeqk8IQWY0l57AW" +
                "fjZOFlNyWQB4QIDAQAB");
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                donationAvailable = result.isSuccess();
            }
        });

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment(), SettingsFragment.TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    private void launchSupport() {
        final MaterialDialog.ListCallback donationListCallback = new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog materialDialog, View view, int i,
                                    CharSequence charSequence) {
                String item;

                switch (i) {
                    case 0:
                        item = "1donation";
                        break;
                    case 1:
                        item = "2donation";
                        break;
                    case 2:
                        item = "3donation";
                        break;
                    case 3:
                        item = "5donation";
                        break;
                    case 4:
                        item = "10donation";
                        break;
                    case 5:
                        item = "20donation";
                        break;
                    default:
                        throw new AssertionError("There are only 4 items");
                }
                if (donationAvailable) {
                    iabHelper.launchPurchaseFlow(SettingsActivity.this, item, 10001,
                            new IabHelper.OnIabPurchaseFinishedListener() {
                                @Override
                                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                    String message;
                                    if (result.isSuccess()) {
                                        message = getString(R.string.donation_worked_thanks);
                                    } else {
                                        message = getString(R.string.donation_not_worked) + ":\n"
                                                + result.getMessage();
                                    }
                                    Toast.makeText(SettingsActivity.this, message,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        };
        final MaterialDialog.ListCallback onSupportListItemClicked =
                new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog materialDialog, View view, int i,
                                            CharSequence charSequence) {
                        switch (i) {
                            case 0: //dev and support
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                        ("https://github.com/Ph1b/MaterialAudiobookPlayer/" +
                                                "issues")));
                                break;
                            case 1: //translations
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
                                        ("https://www.transifex.com/projects/p/" +
                                                "material-audiobook-player/")));
                                break;
                            case 2:
                                new MaterialDialog.Builder(SettingsActivity.this)
                                        .title(R.string.pref_support_donation)
                                        .items(R.array.pref_support_money)
                                        .itemsCallback(donationListCallback)
                                        .show();
                                break;
                            default:
                                throw new AssertionError("There are just 3 items");
                        }
                    }
                };


        new MaterialDialog.Builder(SettingsActivity.this)
                .title(R.string.pref_support_title)
                .items(R.array.pref_support_values)
                .itemsCallback(onSupportListItemClicked)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_contribute:
                launchSupport();
                return true;
            default:
                return false;
        }
    }

    private boolean donationAvailable = false;
    private IabHelper iabHelper;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        iabHelper.handleActivityResult(requestCode, resultCode, data);
    }
}
