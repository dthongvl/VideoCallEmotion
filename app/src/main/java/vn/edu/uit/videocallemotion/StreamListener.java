package vn.edu.uit.videocallemotion;


import org.webrtc.MediaStream;

public interface StreamListener {
    void onLocalStream(MediaStream stream);
    void onRemoteStream(MediaStream stream);
}
