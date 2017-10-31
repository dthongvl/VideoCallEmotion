package vn.edu.uit.videocallemotion;

import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Signaling {
    private boolean caller = true;
    private String callerName = "";
    private Context context;
    private ReceiveListener receiveListener;
    private boolean registered = false;
    private Socket client;
    private static final Signaling ourInstance = new Signaling();

    public static Signaling getInstance() {
        return ourInstance;
    }

    private Signaling() {
        try {
            client = IO.socket("ws://127.0.0.1:3000");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Emitter.Listener messageListener = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String from = data.getString("from");
                    String type = data.getString("type");
                    JSONObject payload = data.getJSONObject("payload");
                    switch (type) {
                        case "offer":
                            caller = false;
                            Intent intent = new Intent(context, CallActivity.class);
                            intent.putExtra("callee", from);
                            context.startActivity(intent);
                            receiveListener.onReceiveOffer(from, payload);
                            break;
                        case "answer":
                            receiveListener.onReceiveAnswer(from, payload);
                            break;
                        case "candidate":
                            receiveListener.onReceiveCandidate(from, payload);
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        client.on("message", messageListener);
        Emitter.Listener registerListener = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                registered = (boolean) args[0];
            }
        };
        client.on("register", registerListener);
        client.connect();
    }

    public void setListener(ReceiveListener receiveListener) {
        this.receiveListener = receiveListener;
    }

    public void clearListener() {
        this.receiveListener = null;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void clearContext() {
        this.context = null;
    }

    public void register(String username) {
        this.callerName = username;
        this.client.emit("register", username);
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        message.put("from", callerName);
        this.client.emit("message", message);
    }

    public boolean isCaller() {
        return this.caller;
    }

    public void setCaller(boolean caller) {
        this.caller = caller;
    }
}
