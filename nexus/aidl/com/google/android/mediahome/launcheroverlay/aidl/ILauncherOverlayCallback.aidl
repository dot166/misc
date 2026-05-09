package com.google.android.mediahome.launcheroverlay.aidl;

import android.os.Bundle;

interface ILauncherOverlayCallback {

    oneway void overlayScrollChanged(float progress);
    oneway void overlayWindowAttached(in Bundle options); // Contains service_status (int), peeky_tab_image (splash Bitmap), minus_one_product_name (String)

}
