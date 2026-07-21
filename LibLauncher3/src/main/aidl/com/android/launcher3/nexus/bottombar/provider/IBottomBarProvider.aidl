package com.android.launcher3.nexus.bottombar.provider;

import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget;

interface IBottomBarProvider {
    List<SmartspaceTarget> getTargets();
    boolean getEnabled();
    void setEnabled(boolean bool);
    String getName(String localeString);
    boolean isAvailableFunction();
    List<SmartspaceTarget> getDisabledTargetsFunction();
    boolean requiresSetup();
    void startSetup();
}