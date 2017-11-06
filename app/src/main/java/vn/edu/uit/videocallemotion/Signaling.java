package vn.edu.uit.videocallemotion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Signaling {
    private PeerConnectionFactory factory;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private MediaConstraints sdpConstraints;

    private boolean isInitiator = true;
    private String initiatorName = "";
    private Activity activity;
    private boolean registered = false;
    private Peer peer;

    private Socket client;
    private static final Signaling ourInstance = new Signaling();

    public static Signaling getInstance() {
        return ourInstance;
    }

    private Signaling() {
        try {
            client = IO.socket("http://c4c45946.ngrok.io");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        client.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String from = data.getString("from");
                    String type = data.getString("type");
                    JSONObject payload = data.getJSONObject("payload");
                    switch (type) {
                        case "offer":
                            isInitiator = false;
                            Intent intent = new Intent(activity, CallActivity.class);
                            intent.putExtra("callee", from);
                            intent.putExtra("payload", payload.toString());
                            activity.startActivity(intent);
                            break;
                        case "answer":
                            onReceiveAnswer(payload);
                            break;
                        case "candidate":
                            onReceiveCandidate(payload);
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        client.on("register", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                registered = (boolean) args[0];
            }
        });
        client.connect();

        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));

    }

    public void setContext(Activity context) {
        this.activity = context;
    }

    public void clearContext() {
        this.activity = null;
    }

    public void register(String username) {
        initiatorName = username;
        client.emit("register", username);
    }

    public boolean isRegistered() {
        return this.registered;
    }

    public boolean isInitiator() {
        return this.isInitiator;
    }

    public void setInitiator(boolean isInitiator) {
        this.isInitiator = isInitiator;
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        message.put("from", initiatorName);
        client.emit("message", message);
    }

    public void initPeer(Context context, String calleeName) {
        Log.d("EMOTION", "initPeer");
        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(context)
                .createInitializationOptions());
        factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
        peer = new Peer(calleeName, factory, iceServers, sdpConstraints, (StreamListener)context);
        peer.initLocalVideo(context, factory);
    }

    public void call() {
        Log.d("EMOTION", "call");
        peer.getPc().createOffer(peer, sdpConstraints);
    }

    public void onReceiveOffer(JSONObject payload) {
        Log.d("EMOTION", "onReceiveOffer");
        try {
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.getPc().setRemoteDescription(peer, sdp);
            peer.getPc().createAnswer(peer, sdpConstraints);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onReceiveAnswer(JSONObject payload) {
        Log.d("EMOTION", "onReceiveAnswer");
        try {
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.getPc().setRemoteDescription(peer, sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onReceiveCandidate(JSONObject payload) {
        Log.d("EMOTION", "onReceiveCandidate");
        try {
            if (peer.getPc().getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                peer.getPc().addIceCandidate(candidate);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        peer.close();
        factory.dispose();
        client.disconnect();
        client.close();
    }

}
