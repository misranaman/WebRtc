package com.org.webrtc.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.org.webrtc.remote.FirebaseClient;
import com.org.webrtc.util.DataModel;
import com.org.webrtc.util.DataModelType;
import com.org.webrtc.util.ErrorCallBack;
import com.org.webrtc.util.NewEventCallBack;
import com.org.webrtc.util.SuccessCallBack;
import com.org.webrtc.webrtc.MyPeerConnectionObserver;
import com.org.webrtc.webrtc.WebRtcClient;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

public class MainRepository implements WebRtcClient.Listener {

    public Listener listener;
    private final Gson gson = new Gson();
    private WebRtcClient webRtcClient;
    private FirebaseClient firebaseClient;
    private String currentUsername;
    private SurfaceViewRenderer remoteView;
    private String target;

    private MainRepository() {
        this.firebaseClient = new FirebaseClient();
    }

    private void updateCurrentUsername(String username) {

        this.currentUsername = username;
    }

    private static MainRepository instance;

    public static MainRepository getInstance() {

        if (instance == null) {
            instance = new MainRepository();
        }
        return instance;
    }

    public void login(String username, Context context, SuccessCallBack callBack) {
        firebaseClient.login(username, () -> {
            updateCurrentUsername(username);
            this.webRtcClient = new WebRtcClient(context, new MyPeerConnectionObserver() {

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    super.onAddStream(mediaStream);
                    try {
                        mediaStream.videoTracks.get(0).addSink(remoteView);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                    super.onConnectionChange(newState);
                    if (newState == PeerConnection.PeerConnectionState.CONNECTED && listener != null) {
                        listener.webrtcConnected();
                    }

                    if (newState == PeerConnection.PeerConnectionState.CLOSED ||
                            newState == PeerConnection.PeerConnectionState.DISCONNECTED) {

                        if (listener != null) {
                            listener.webrtcClosed();
                        }

                    }
                }

                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    super.onIceCandidate(iceCandidate);
                    webRtcClient.sendIceCandidate(iceCandidate, target);
                }
            }, username);
            webRtcClient.listener = this;
            callBack.onSuccess();
        });
    }

    public void initLocalView(SurfaceViewRenderer view) {

        webRtcClient.initLocalSurfaceView(view);
    }

    public void initRemoteView(SurfaceViewRenderer view) {
        webRtcClient.initRemoteSurfaceView(view);
        this.remoteView = view;
    }

    public void startCall(String target) {
        webRtcClient.call(target);
    }

    public void switchCamera() {
        webRtcClient.switchCamera();
    }

    public void toggleAudio(Boolean shouldBeMuted) {
        webRtcClient.toggleAudio(shouldBeMuted);
    }

    public void toggleVide(Boolean shouldBeMuted) {
        webRtcClient.toggleVideo(shouldBeMuted);
    }

    public void endCall() {
        webRtcClient.closeConnection();
    }

    public void sendCallRequest(String target, ErrorCallBack callBack) {

        firebaseClient.sendMessageToOtherUser(
                new DataModel(target,
                        currentUsername,
                        null,
                        DataModelType.StartCall),
                callBack);
    }

    public void subscribeForLatestEvent(NewEventCallBack callBack) {

        firebaseClient.observeIncomingLatestEvent(model -> {
            switch (model.getType()) {
                case Offer:
                    this.target = model.getSender();
                    webRtcClient.onRemoteSessionReceived(new SessionDescription(
                            SessionDescription.Type.OFFER, model.getData()
                    ));
                    webRtcClient.answer(model.getSender());
                    break;
                case Answer:
                    this.target = model.getSender();
                    webRtcClient.onRemoteSessionReceived(new SessionDescription(
                            SessionDescription.Type.ANSWER, model.getData()
                    ));
                    break;
                case IceCandidate:
                    try {
                        IceCandidate candidate = gson.fromJson(model.getData(), IceCandidate.class);
                        webRtcClient.addIceCandidate(candidate);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case StartCall:
                    this.target = model.getSender();
                    callBack.onNewEventReceived(model);
                    break;
            }
        });

    }

    @Override
    public void onTransferDataToOtherPeer(DataModel model) {
        firebaseClient.sendMessageToOtherUser(model, () -> {
        });
    }

    public interface Listener {
        void webrtcConnected();

        void webrtcClosed();
    }
}
