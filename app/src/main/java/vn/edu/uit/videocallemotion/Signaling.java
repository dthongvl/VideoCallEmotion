package vn.edu.uit.videocallemotion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.LinkedList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class Signaling implements SdpObserver, PeerConnection.Observer {
    private PeerConnectionFactory factory;
    private PeerConnection pc;
    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();

    private MediaConstraints sdpConstraints;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;

    private boolean isInitiator = true;
    private String initiatorName = "";
    private String calleeName = "";
    private Activity activity;
    private boolean registered = false;

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
                            isInitiator = false;
                            calleeName = from;
                            Intent intent = new Intent(activity, CallActivity.class);
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

    public void setCalleeName(String calleeName) {
        this.calleeName = calleeName;
    }

    public void setContext(Activity context) {
        this.activity = context;
    }

    public void clearContext() {
        this.activity = null;
    }

    public void register(String username) {
        this.initiatorName = username;
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
        message.put("from", initiatorName);
        this.client.emit("message", message);
    }

    public boolean isInitiator() {
        return this.isInitiator;
    }

    public void setInitiator(boolean isInitiator) {
        this.isInitiator = isInitiator;
    }

    public void init(Context context, SurfaceViewRenderer localView, SurfaceViewRenderer remoteView) {
        Log.d("EMOTION", "init");
        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(context)
                .createInitializationOptions());
        this.factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
        this.iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
        this.pc = factory.createPeerConnection(iceServers, sdpConstraints, this);
        this.localView = localView;
        this.remoteView = remoteView;
    }

    public void initVideo(VideoCapturer videoCapturerAndroid) {
        Log.d("EMOTION", "initVideo");
        videoSource = factory.createVideoSource(videoCapturerAndroid);
        localVideoTrack = factory.createVideoTrack("100", videoSource);
        localVideoTrack.setEnabled(true);

        videoCapturerAndroid.startCapture(1000, 1000, 30);

        audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("101", audioSource);
        localAudioTrack.setEnabled(true);

        localView.setMirror(true);
        localVideoTrack.addRenderer(new VideoRenderer(localView));
    }

    public void addStream() {
        Log.d("EMOTION", "addStream");
        MediaStream stream = factory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        pc.addStream(stream);
    }

    public void call() {
        Log.d("EMOTION", "call");
        pc.createOffer(this, sdpConstraints);
    }

    public void onReceiveOffer(JSONObject payload) {
        Log.d("EMOTION", "onReceiveOffer");
        try {
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            pc.setRemoteDescription(this, sdp);
            pc.createAnswer(this, sdpConstraints);
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
            pc.setRemoteDescription(this, sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onReceiveCandidate(JSONObject payload) {
        Log.d("EMOTION", "onReceiveCandidate");
        try {
            if (pc == null) return;
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // SdpObserve
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d("EMOTION", "onCreateSuccess");
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", sessionDescription.type.canonicalForm());
            payload.put("sdp", sessionDescription.description);
            sendMessage(this.calleeName, sessionDescription.type.canonicalForm(), payload);
            pc.setLocalDescription(this, sessionDescription);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {

    }

    @Override
    public void onSetFailure(String s) {

    }

    // PeerConnection.Observe

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d("EMOTION", "onIceCandidate");
        try {
            JSONObject payload = new JSONObject();
            payload.put("label", iceCandidate.sdpMLineIndex);
            payload.put("id", iceCandidate.sdpMid);
            payload.put("candidate", iceCandidate.sdp);
            sendMessage(this.calleeName, "candidate", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d("EMOTION", "onAddStream");
        final VideoTrack videoTrack = mediaStream.videoTracks.getFirst();
        final AudioTrack audioTrack = mediaStream.audioTracks.getFirst();
        remoteView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteView.setMirror(true);
                    videoTrack.addRenderer(new VideoRenderer(remoteView));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

}
