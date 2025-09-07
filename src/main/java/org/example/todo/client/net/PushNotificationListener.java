package org.example.todo.client.net;

import com.google.gson.JsonObject;

/**
 * An observer interface for receiving push notifications from the UdpListener.
 */
public interface PushNotificationListener {
    void onPushNotification(JsonObject payload);
}