package com.android.launcher3.nexus.bottombar.provider;

import com.android.launcher3.nexus.bottombar.model.SmartspaceTarget;

interface IBottomBarProvider {
    List<SmartspaceTarget> getTargets();
}