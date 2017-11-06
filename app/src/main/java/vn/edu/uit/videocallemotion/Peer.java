package vn.edu.uit.videocallemotion;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.LinkedList;

public class Peer implements SdpObserver, PeerConnection.Observer {
    private String calleeName;
    private PeerConnection pc;
    private StreamListener streamListener;
    private Signaling signaling;
    private VideoSource videoSource;
    private AudioSource audioSource;

    public Peer(String calleeName, PeerConnectionFactory factory, LinkedList<PeerConnection.IceServer> iceServers, MediaConstraints mediaConstraints, StreamListener streamListener) {
        Log.d("EMOTION", "new Peer");
        this.calleeName = calleeName;
        this.streamListener = streamListener;
        this.pc = factory.createPeerConnection(iceServers, mediaConstraints, this);
        this.signaling = Signaling.getInstance();
    }

    public void initLocalVideo(Context context, PeerConnectionFactory factory) {
        Log.d("EMOTION", "initLocalVideo");
        videoSource = factory.createVideoSource(Util.createVideoCapturer(context));
        VideoTrack localVideoTrack = factory.createVideoTrack("100", videoSource);
        localVideoTrack.setEnabled(true);

        audioSource = factory.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = factory.createAudioTrack("101", audioSource);
        localAudioTrack.setEnabled(true);

        MediaStream stream = factory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);

        streamListener.onLocalStream(stream);
        pc.addStream(stream);
    }

    public PeerConnection getPc() {
        return this.pc;
    }

    // SdpObserver
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d("EMOTION", "onCreateSuccess");
        try {
            JSONObject payload = new JSONObject();
            payload.put("type", sessionDescription.type.canonicalForm());
            payload.put("sdp", sessionDescription.description);
            signaling.sendMessage(this.calleeName, sessionDescription.type.canonicalForm(), payload);
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

    // PeerConnection.Observer

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
            signaling.sendMessage(this.calleeName, "candidate", payload);
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
        streamListener.onRemoteStream(mediaStream);
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

    public void close() {
        if (videoSource != null) {
            videoSource.dispose();
        }
        if (audioSource != null) {
            audioSource.dispose();
        }
        pc.dispose();
    }
}
