package com.google.android.libraries.launcherclient;

import android.os.Bundle;
import android.content.Intent;

interface ILauncherOverlayCallback {

    oneway void overlayScrollChanged(float progress);

    oneway void overlayStatusChanged(int status);

    oneway void startActivity(in Intent intent, in Bundle bundle);

}