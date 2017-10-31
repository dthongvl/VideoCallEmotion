package vn.edu.uit.videocallemotion;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
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
import org.webrtc.EglBase;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;

import butterknife.BindView;

public class CallActivity extends AppCompatActivity
        implements SdpObserver, PeerConnection.Observer, ReceiveListener {
    @BindView(R.id.localView)
    SurfaceViewRenderer localView;

    @BindView(R.id.remoteView)
    SurfaceViewRenderer remoteView;

    private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
    private PeerConnectionFactory factory;
    private PeerConnection pc;
    private String callee;
    private Signaling signaling;

    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(this)
                .createInitializationOptions());
        signaling = Signaling.getInstance();
        signaling.clearContext();
        signaling.setListener(this);

        initVideo();
        setupPeer();
        addStream();
        call();
    }

    void setupPeer() {
        this.factory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
        this.iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
        this.pc = factory.createPeerConnection(iceServers, sdpConstraints, this);
    }

    void initVideo() {
        VideoCapturer videoCapturerAndroid = createVideoCapturer();

        MediaConstraints constraints = new MediaConstraints();

        videoSource = factory.createVideoSource(videoCapturerAndroid);
        localVideoTrack = factory.createVideoTrack("100", videoSource);

        audioSource = factory.createAudioSource(constraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);

        videoCapturerAndroid.startCapture(1000, 1000, 30);

        localView.setMirror(true);
        EglBase rootEglBase = EglBase.create();
        localView.init(rootEglBase.getEglBaseContext(), null);
        localVideoTrack.addRenderer(new VideoRenderer(localView));
    }

    void addStream() {
        MediaStream stream = factory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        pc.addStream(stream);
    }

    void call() {
        Intent intent = getIntent();
        this.callee = intent.getStringExtra("callee");
        if (this.signaling.isCaller()) {
            pc.createOffer(this, sdpConstraints);
        }
    }

    // SdpObserve
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", sessionDescription.type.canonicalForm());
            payload.put("sdp", sessionDescription.description);
            signaling.sendMessage(this.callee, sessionDescription.type.canonicalForm(), payload);
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
        try {
            JSONObject payload = new JSONObject();
            payload.put("label", iceCandidate.sdpMLineIndex);
            payload.put("id", iceCandidate.sdpMid);
            payload.put("candidate", iceCandidate.sdp);
            signaling.sendMessage(this.callee, "candidate", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        final VideoTrack videoTrack = mediaStream.videoTracks.getFirst();
        final AudioTrack audioTrack = mediaStream.audioTracks.getFirst();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    remoteView.setMirror(true);
                    EglBase rootEglBase = EglBase.create();
                    remoteView.init(rootEglBase.getEglBaseContext(), null);
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

    @Override
    public void onReceiveOffer(String fromUid, JSONObject payload) {
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

    @Override
    public void onReceiveAnswer(String fromUid, JSONObject payload) {
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

    @Override
    public void onReceiveCandidate(String fromUid, JSONObject payload) {
        try {
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

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // Trying to find a front facing camera!
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // We were not able to find a front cam. Look for other cameras
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}
