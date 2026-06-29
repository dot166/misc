/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: out/host/linux-x86/bin/aidl -dout/soong/.intermediates/misc/nexus/Nexus/android_common/gen/aidl/misc/nexus/aidl/com/google/android/libraries/launcherclient/ILauncherOverlay.aidl.d -Iframeworks/base/core/java -Iframeworks/base/drm/java -Iframeworks/base/graphics/java -Iframeworks/base/identity/java -Iframeworks/base/keystore/java -Iframeworks/base/media/java -Iframeworks/base/media/mca/effect/java -Iframeworks/base/media/mca/filterfw/java -Iframeworks/base/media/mca/filterpacks/java -Iframeworks/base/mms/java -Iframeworks/base/opengl/java -Iframeworks/base/rs/java -Iframeworks/base/sax/java -Iframeworks/base/telephony/java -Imisc/nexus/aidl -Imisc/nexus --min_sdk_version=31 misc/nexus/aidl/com/google/android/libraries/launcherclient/ILauncherOverlay.aidl out/soong/.intermediates/misc/nexus/Nexus/android_common/gen/aidl/aidl0.tmp/misc/nexus/aidl/com/google/android/libraries/launcherclient/ILauncherOverlay.java
 *
 * DO NOT CHECK THIS FILE INTO A CODE TREE (e.g. git, etc..).
 * ALWAYS GENERATE THIS FILE FROM UPDATED AIDL COMPILER
 * AS A BUILD INTERMEDIATE ONLY. THIS IS NOT SOURCE CODE.
 */
package com.google.android.libraries.launcherclient;
public interface ILauncherOverlay extends android.os.IInterface
{
    /** Default implementation for ILauncherOverlay. */
    public static class Default implements com.google.android.libraries.launcherclient.ILauncherOverlay
    {
        @Override public void startScroll() throws android.os.RemoteException
        {
        }
        @Override public void onScroll(float progress) throws android.os.RemoteException
        {
        }
        @Override public void endScroll() throws android.os.RemoteException
        {
        }
        @Override public void windowAttached(android.view.WindowManager.LayoutParams lp, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb, int flags) throws android.os.RemoteException
        {
        }
        @Override public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException
        {
        }
        @Override public void closeOverlay(int flags) throws android.os.RemoteException
        {
        }
        @Override public void onPause() throws android.os.RemoteException
        {
        }
        @Override public void onResume() throws android.os.RemoteException
        {
        }
        @Override public void openOverlay(int flags) throws android.os.RemoteException
        {
        }
        @Override public void requestVoiceDetection(boolean start) throws android.os.RemoteException
        {
        }
        @Override public java.lang.String getVoiceSearchLanguage() throws android.os.RemoteException
        {
            return null;
        }
        @Override public boolean isVoiceDetectionRunning() throws android.os.RemoteException
        {
            return false;
        }
        @Override public boolean hasOverlayContent() throws android.os.RemoteException
        {
            return false;
        }
        @Override public void windowAttached2(android.os.Bundle bundle, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb) throws android.os.RemoteException
        {
        }
        @Override public void unusedMethod() throws android.os.RemoteException
        {
        }
        @Override public void setActivityState(int flags) throws android.os.RemoteException
        {
        }
        @Override public boolean startSearch(byte[] data, android.os.Bundle bundle) throws android.os.RemoteException
        {
            return false;
        }
        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements com.google.android.libraries.launcherclient.ILauncherOverlay
    {
        /** Construct the stub and attach it to the interface. */
        @SuppressWarnings("this-escape")
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an com.google.android.libraries.launcherclient.ILauncherOverlay interface,
         * generating a proxy if needed.
         */
        public static com.google.android.libraries.launcherclient.ILauncherOverlay asInterface(android.os.IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof com.google.android.libraries.launcherclient.ILauncherOverlay))) {
                return ((com.google.android.libraries.launcherclient.ILauncherOverlay)iin);
            }
            return new com.google.android.libraries.launcherclient.ILauncherOverlay.Stub.Proxy(obj);
        }
        @Override public android.os.IBinder asBinder()
        {
            return this;
        }
        @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        {
            if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code)
            {
                case TRANSACTION_startScroll:
                {
                    this.startScroll();
                    break;
                }
                case TRANSACTION_onScroll:
                {
                    float _arg0;
                    _arg0 = data.readFloat();
                    this.onScroll(_arg0);
                    break;
                }
                case TRANSACTION_endScroll:
                {
                    this.endScroll();
                    break;
                }
                case TRANSACTION_windowAttached:
                {
                    android.view.WindowManager.LayoutParams _arg0;
                    _arg0 = data.readTypedObject(android.view.WindowManager.LayoutParams.CREATOR);
                    com.google.android.libraries.launcherclient.ILauncherOverlayCallback _arg1;
                    _arg1 = com.google.android.libraries.launcherclient.ILauncherOverlayCallback.Stub.asInterface(data.readStrongBinder());
                    int _arg2;
                    _arg2 = data.readInt();
                    this.windowAttached(_arg0, _arg1, _arg2);
                    break;
                }
                case TRANSACTION_windowDetached:
                {
                    boolean _arg0;
                    _arg0 = data.readBoolean();
                    this.windowDetached(_arg0);
                    break;
                }
                case TRANSACTION_closeOverlay:
                {
                    int _arg0;
                    _arg0 = data.readInt();
                    this.closeOverlay(_arg0);
                    break;
                }
                case TRANSACTION_onPause:
                {
                    this.onPause();
                    break;
                }
                case TRANSACTION_onResume:
                {
                    this.onResume();
                    break;
                }
                case TRANSACTION_openOverlay:
                {
                    int _arg0;
                    _arg0 = data.readInt();
                    this.openOverlay(_arg0);
                    break;
                }
                case TRANSACTION_requestVoiceDetection:
                {
                    boolean _arg0;
                    _arg0 = data.readBoolean();
                    this.requestVoiceDetection(_arg0);
                    break;
                }
                case TRANSACTION_getVoiceSearchLanguage:
                {
                    java.lang.String _result = this.getVoiceSearchLanguage();
                    reply.writeNoException();
                    reply.writeString(_result);
                    break;
                }
                case TRANSACTION_isVoiceDetectionRunning:
                {
                    boolean _result = this.isVoiceDetectionRunning();
                    reply.writeNoException();
                    reply.writeBoolean(_result);
                    break;
                }
                case TRANSACTION_hasOverlayContent:
                {
                    boolean _result = this.hasOverlayContent();
                    reply.writeNoException();
                    reply.writeBoolean(_result);
                    break;
                }
                case TRANSACTION_windowAttached2:
                {
                    android.os.Bundle _arg0;
                    _arg0 = data.readTypedObject(android.os.Bundle.CREATOR);
                    com.google.android.libraries.launcherclient.ILauncherOverlayCallback _arg1;
                    _arg1 = com.google.android.libraries.launcherclient.ILauncherOverlayCallback.Stub.asInterface(data.readStrongBinder());
                    this.windowAttached2(_arg0, _arg1);
                    break;
                }
                case TRANSACTION_unusedMethod:
                {
                    this.unusedMethod();
                    break;
                }
                case TRANSACTION_setActivityState:
                {
                    int _arg0;
                    _arg0 = data.readInt();
                    this.setActivityState(_arg0);
                    break;
                }
                case TRANSACTION_startSearch:
                {
                    byte[] _arg0;
                    _arg0 = data.createByteArray();
                    android.os.Bundle _arg1;
                    _arg1 = data.readTypedObject(android.os.Bundle.CREATOR);
                    boolean _result = this.startSearch(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeBoolean(_result);
                    break;
                }
                default:
                {
                    return super.onTransact(code, data, reply, flags);
                }
            }
            return true;
        }
        /** @hide */
        public static class Proxy implements com.google.android.libraries.launcherclient.ILauncherOverlay
        {
            protected android.os.IBinder mRemote;
            protected Proxy(android.os.IBinder remote)
            {
                mRemote = remote;
            }
            @Override public android.os.IBinder asBinder()
            {
                return mRemote;
            }
            public final java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }
            @Override public void startScroll() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_startScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void onScroll(float progress) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeFloat(progress);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void endScroll() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_endScroll, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void windowAttached(android.view.WindowManager.LayoutParams lp, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb, int flags) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(lp, 0);
                    _data.writeStrongInterface(cb);
                    _data.writeInt(flags);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_windowAttached, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(isChangingConfigurations);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_windowDetached, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void closeOverlay(int flags) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(flags);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_closeOverlay, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void onPause() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onPause, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void onResume() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_onResume, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void openOverlay(int flags) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(flags);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_openOverlay, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void requestVoiceDetection(boolean start) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(start);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_requestVoiceDetection, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public java.lang.String getVoiceSearchLanguage() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getVoiceSearchLanguage, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public boolean isVoiceDetectionRunning() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_isVoiceDetectionRunning, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readBoolean();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public boolean hasOverlayContent() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_hasOverlayContent, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readBoolean();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void windowAttached2(android.os.Bundle bundle, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedObject(bundle, 0);
                    _data.writeStrongInterface(cb);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_windowAttached2, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void unusedMethod() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_unusedMethod, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public void setActivityState(int flags) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(flags);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_setActivityState, _data, null, android.os.IBinder.FLAG_ONEWAY);
                }
                finally {
                    _data.recycle();
                }
            }
            @Override public boolean startSearch(byte[] data, android.os.Bundle bundle) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(data);
                    _data.writeTypedObject(bundle, 0);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_startSearch, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readBoolean();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
        /** @hide */
        public static final int TRANSACTION_startScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        /** @hide */
        public static final int TRANSACTION_onScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        /** @hide */
        public static final int TRANSACTION_endScroll = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        /** @hide */
        public static final int TRANSACTION_windowAttached = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        /** @hide */
        public static final int TRANSACTION_windowDetached = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
        /** @hide */
        public static final int TRANSACTION_closeOverlay = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
        /** @hide */
        public static final int TRANSACTION_onPause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
        /** @hide */
        public static final int TRANSACTION_onResume = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
        /** @hide */
        public static final int TRANSACTION_openOverlay = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
        /** @hide */
        public static final int TRANSACTION_requestVoiceDetection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
        /** @hide */
        public static final int TRANSACTION_getVoiceSearchLanguage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
        /** @hide */
        public static final int TRANSACTION_isVoiceDetectionRunning = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
        /** @hide */
        public static final int TRANSACTION_hasOverlayContent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
        /** @hide */
        public static final int TRANSACTION_windowAttached2 = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
        /** @hide */
        public static final int TRANSACTION_unusedMethod = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
        /** @hide */
        public static final int TRANSACTION_setActivityState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
        /** @hide */
        public static final int TRANSACTION_startSearch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
    }
    /** @hide */
    public static final java.lang.String DESCRIPTOR = "com.google.android.libraries.launcherclient.ILauncherOverlay";
    public void startScroll() throws android.os.RemoteException;
    public void onScroll(float progress) throws android.os.RemoteException;
    public void endScroll() throws android.os.RemoteException;
    public void windowAttached(android.view.WindowManager.LayoutParams lp, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb, int flags) throws android.os.RemoteException;
    public void windowDetached(boolean isChangingConfigurations) throws android.os.RemoteException;
    public void closeOverlay(int flags) throws android.os.RemoteException;
    public void onPause() throws android.os.RemoteException;
    public void onResume() throws android.os.RemoteException;
    public void openOverlay(int flags) throws android.os.RemoteException;
    public void requestVoiceDetection(boolean start) throws android.os.RemoteException;
    public java.lang.String getVoiceSearchLanguage() throws android.os.RemoteException;
    public boolean isVoiceDetectionRunning() throws android.os.RemoteException;
    public boolean hasOverlayContent() throws android.os.RemoteException;
    public void windowAttached2(android.os.Bundle bundle, com.google.android.libraries.launcherclient.ILauncherOverlayCallback cb) throws android.os.RemoteException;
    public void unusedMethod() throws android.os.RemoteException;
    public void setActivityState(int flags) throws android.os.RemoteException;
    public boolean startSearch(byte[] data, android.os.Bundle bundle) throws android.os.RemoteException;
}
