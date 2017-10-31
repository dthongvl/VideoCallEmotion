package vn.edu.uit.videocallemotion;


import org.json.JSONObject;

public interface ReceiveListener {
    void onReceiveOffer(String fromUid, JSONObject payload);
    void onReceiveAnswer(String fromUid, JSONObject payload);
    void onReceiveCandidate(String fromUid, JSONObject payload);
}
