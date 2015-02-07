package de.ph1b.audiobook.tests;


import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;

import de.ph1b.audiobook.utils.CoverDownloader;
import de.ph1b.audiobook.utils.L;

public class CoverDownloadingTest extends InstrumentationTestCase {

    private static final String TAG = "CoverDownloadingTest";


    /**
     * This tests if the results from google are unique and no url is being returned twice because
     * of an error in testing.
     */
    @SmallTest
    public void testCoverFetch() {

        Context c = getInstrumentation().getTargetContext();

        CoverDownloader coverDownloader = new CoverDownloader(c);

        String searchText = "harry potter";
        ArrayList<String> results = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            String url = coverDownloader.getBitmapUrl(searchText, i);
            if (url != null) {
                assertFalse("Url=" + url + " at position=" + i + " already in results", results.contains(url));
                results.add(url);
            }
        }

        for (String s : results) {
            L.d(TAG, s);
        }
    }
}
