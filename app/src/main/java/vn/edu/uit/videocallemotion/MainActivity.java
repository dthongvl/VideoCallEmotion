package vn.edu.uit.videocallemotion;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements CallListener {
    private Signaling signaling;

    @BindView(R.id.caller)
    TextView caller;

    @BindView(R.id.callee)
    TextView callee;

    @OnClick(R.id.register) void register() {
        signaling.register(caller.getText().toString());
    }

    @OnClick(R.id.call) void call() {
        if (!signaling.isRegistered()) {
            Toast.makeText(this, "Please register", Toast.LENGTH_SHORT).show();
            return;
        }
        signaling.setInitiator(true);
        Intent intent = new Intent(MainActivity.this, CallActivity.class);
        intent.putExtra("callee", callee.getText().toString());
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        signaling = Signaling.getInstance();
        signaling.setCallListener(this);
        ButterKnife.bind(this);
    }

    @Override
    public void onCall(String from, String payload) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("callee", from);
        intent.putExtra("payload", payload);
        startActivity(intent);
    }
}
