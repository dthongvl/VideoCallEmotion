package vn.edu.uit.videocallemotion;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.EglBase;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;


import butterknife.BindView;
import butterknife.ButterKnife;

public class CallActivity extends AppCompatActivity implements StreamListener {
    @BindView(R.id.localView)
    SurfaceViewRenderer localView;

    @BindView(R.id.remoteView)
    SurfaceViewRenderer remoteView;

    private Signaling signaling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        ButterKnife.bind(this);

        EglBase rootEglBase = EglBase.create();
        localView.init(rootEglBase.getEglBaseContext(), null);
        remoteView.init(rootEglBase.getEglBaseContext(), null);

        Intent intent = getIntent();
        String calleeName = intent.getStringExtra("callee");

        signaling = Signaling.getInstance();
        signaling.clearContext();
        signaling.initPeer(this, calleeName);
        if (signaling.isInitiator()) {
            signaling.call();
        } else {
            try {
                JSONObject payload = new JSONObject(intent.getStringExtra("payload"));
                signaling.onReceiveOffer(payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocalStream(MediaStream stream) {
        VideoTrack videoTrack = stream.videoTracks.getFirst();
        localView.setMirror(true);
        videoTrack.addRenderer(new VideoRenderer(localView));
    }

    @Override
    public void onRemoteStream(MediaStream stream) {
        VideoTrack videoTrack = stream.videoTracks.getFirst();
        remoteView.setMirror(true);
        videoTrack.addRenderer(new VideoRenderer(remoteView));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        signaling.close();
    }
}
