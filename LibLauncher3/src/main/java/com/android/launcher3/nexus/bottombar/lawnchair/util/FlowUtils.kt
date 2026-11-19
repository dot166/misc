package com.android.launcher3.nexus.bottombar.lawnchair.util

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import androidx.annotation.Discouraged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

@Discouraged("This is a blocking read, use firstCached() for non-blocking reads")
fun <T> Flow<T>.firstBlocking() = runBlocking { first() }

fun broadcastReceiverFlow(context: Context, filter: IntentFilter, withInitial: Boolean = false) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent)
        }
    }
    if (withInitial) {
        trySend(Intent())
    }
    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    awaitClose { context.unregisterReceiver(receiver) }
}

fun <T> Flow<T>.dropWhileBusy(): Flow<T> = channelFlow {
    collect { trySend(it) }
}.buffer(0)

fun <T> Flow<T>.subscribeBlocking(
    scope: CoroutineScope,
    block: (T) -> Unit,
) {
    block(firstBlocking())
    this
        .onEach { block(it) }
        .drop(1)
        .distinctUntilChanged()
        .launchIn(scope = scope)
}

fun ContentResolver.observerAsFlow(uri: Uri) = callbackFlow {
    val observer = object: ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(Unit)
        }
    }
    trySend(Unit)
    registerContentObserver(uri, true, observer)
    awaitClose {
        unregisterContentObserver(observer)
    }
}

fun ContentResolver.queryAsFlow(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Flow<Cursor> {
    val client = acquireContentProviderClient(uri) ?: return flowOf()
    return observerAsFlow(uri).mapNotNull {
        try {
            client.query(uri, projection, selection, selectionArgs, sortOrder)
        }catch (e: DeadObjectException){
            null
        }catch (e: SecurityException){
            null
        }
    }.onCompletion {
        try {
            client.close()
        }catch (e: DeadObjectException){
            e.printStackTrace()
        }
    }
}

