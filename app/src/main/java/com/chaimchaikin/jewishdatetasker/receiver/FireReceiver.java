/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.chaimchaikin.jewishdatetasker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.chaimchaikin.jewishdatetasker.Constants;
import com.chaimchaikin.jewishdatetasker.bundle.BundleScrubber;
import com.chaimchaikin.jewishdatetasker.helper.JewishDateHelper;
import com.chaimchaikin.jewishdatetasker.helper.LocationHelper;
import com.chaimchaikin.jewishdatetasker.helper.TaskerPlugin;
import com.chaimchaikin.jewishdatetasker.ui.EditActivity;

import java.util.Locale;
import java.util.Set;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 *
 * @see com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class FireReceiver extends BroadcastReceiver
{

    /**
     * @param context {@inheritDoc}.
     * @param intent the incoming {@link com.twofortyfouram.locale.Intent#ACTION_FIRE_SETTING} Intent. This
     *            should contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by
     *            {@link EditActivity} and later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {

        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG,
                      String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        BundleScrubber.scrub(intent);

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        BundleScrubber.scrub(bundle);

/*
        if (PluginBundleManager.isBundleValid(bundle)) {

        }
*/

        if ( isOrderedBroadcast() ) {


            // Set result OK
            setResultCode(TaskerPlugin.Setting.RESULT_CODE_OK);


            // Create a new JewishDateHelper to calculate times and dates
            JewishDateHelper jewishDate = new JewishDateHelper();


            // Initialize Variables to values in settings
            boolean autoLocation = bundle.getBoolean("loc_auto", false);
            String locName = bundle.getString("loc_name");
            double lat = bundle.getDouble("loc_lat");
            double lng = bundle.getDouble("loc_lng");
            String timezone = bundle.getString("timezone");

            // If auto location is set, try to find a more up to date location
            if(autoLocation) {
                LocationHelper locHelper = new LocationHelper(context);

                String currentLocation = bundle.getString("tasker_location");
                String[] parts = currentLocation.split(",");
                lat = Double.parseDouble(parts[0]);
                lng = Double.parseDouble(parts[1]);

                locName = locHelper.getLocationName(lat, lng);
                timezone = locHelper.getTimezoneFromLocation(lat, lng);
            }

            // Set the location for the JewishDateHelper
            jewishDate.setLocation(locName, lat, lng, timezone);

            // Update the dates
            jewishDate.updateDates();

            // Check support for returning variables
            if ( TaskerPlugin.Setting.hostSupportsVariableReturn( intent.getExtras() ) ) {

                // Create a new bundle
                Bundle vars = new Bundle();
                // Add all the variables to return
                vars.putString( "%jd_short", jewishDate.vars.getString("shortDate") );
                vars.putString( "%jd_long", jewishDate.vars.getString("longDate") );
                vars.putString( "%jd_hebrew_short", jewishDate.vars.getString("shortHebrewDate") );
                vars.putString( "%jd_hebrew_long", jewishDate.vars.getString("longHebrewDate") );

                vars.putString( "%jd_desc", jewishDate.vars.getString("longText") );
                vars.putString( "%jd_loc", locName );

                vars.putBoolean( "%jd_after_sunset", jewishDate.vars.getBoolean("afterSunset") );

                vars.putString( "%jd_parsha", jewishDate.vars.getString("englishParsha") );
                vars.putString( "%jd_hebrew_parsha", jewishDate.vars.getString("hebrewParsha") );

                vars.putString( "%jd_day", jewishDate.vars.getString("englishDay") );
                vars.putString( "%jd_month", jewishDate.vars.getString("englishMonth") );
                vars.putString( "%jd_year", jewishDate.vars.getString("englishYear") );

                vars.putString( "%jd_hebrew_day", jewishDate.vars.getString("hebrewDay") );
                vars.putString( "%jd_hebrew_month", jewishDate.vars.getString("hebrewMonth") );
                vars.putString( "%jd_hebrew_year", jewishDate.vars.getString("hebrewYear") );


                // Get a bundle of the zmanim
                Bundle zmanim = jewishDate.vars.getBundle("zmanim");

                // Loop through all the zmanim
                Set<String> ks = zmanim.keySet();
                for (String key: ks) {
                    // Get the next key
                    String formattedKey = key.toLowerCase();

                    // Put a variable for each zman
                    vars.putString( "%jd_zmanim_" + formattedKey, zmanim.getString(key) );
                }


                // Return the bundle of variables
                TaskerPlugin.addVariableBundle( getResultExtras( true ), vars );
            }
        }
    }
}