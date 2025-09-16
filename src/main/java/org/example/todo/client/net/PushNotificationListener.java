package org.example.todo.client.net;

import com.google.gson.JsonObject;


public interface PushNotificationListener {
    void onPushNotification(JsonObject payload);
}