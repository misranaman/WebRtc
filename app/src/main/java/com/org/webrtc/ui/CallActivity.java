package com.org.webrtc.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.org.webrtc.R;
import com.org.webrtc.databinding.ActivityCallBinding;
import com.org.webrtc.repository.MainRepository;
import com.org.webrtc.util.DataModelType;

public class CallActivity extends AppCompatActivity implements MainRepository.Listener {

    private ActivityCallBinding views;
    private MainRepository mainRepository;
    private Boolean isMicMuted = false;
    private Boolean isCameraMuted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());
        init();
    }

    private void init() {
        mainRepository = MainRepository.getInstance();
        views.callBtn.setOnClickListener(v -> {
            mainRepository.sendCallRequest(views.targetUserNameEt.getText().toString(), () -> {
                Toast.makeText(CallActivity.this, "Couldn't find the target", Toast.LENGTH_SHORT).show();
            });
        });

        mainRepository.initLocalView(views.localView);
        mainRepository.initRemoteView(views.remoteView);
        mainRepository.listener = this;

        mainRepository.subscribeForLatestEvent(data -> {
            if (data.getType() == DataModelType.StartCall) {
                runOnUiThread(() -> {
                    views.incomingNameTV.setText(data.getSender() + "is calling you");
                    views.incomingCallLayout.setVisibility(View.VISIBLE);
                    views.acceptButton.setOnClickListener(v -> {
                        //start the call
                        mainRepository.startCall(data.getSender());
                        views.incomingCallLayout.setVisibility(View.GONE);
                    });
                    views.rejectButton.setOnClickListener(v -> {
                        views.incomingCallLayout.setVisibility(View.GONE);
                    });
                });
            }
        });

        views.switchCameraButton.setOnClickListener(v -> {
            mainRepository.switchCamera();
        });

        views.micButton.setOnClickListener(v -> {
            if (isMicMuted) {
                views.micButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
            } else {
                views.micButton.setImageResource(R.drawable.ic_baseline_mic_24);
            }
            mainRepository.toggleAudio(isMicMuted);
            isMicMuted = !isMicMuted;
        });

        views.videoButton.setOnClickListener(v -> {
            if (isCameraMuted) {
                views.videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24);
            } else {
                views.videoButton.setImageResource(R.drawable.ic_baseline_videocam_24);
            }
            mainRepository.toggleVide(isCameraMuted);
            isCameraMuted = !isCameraMuted;
        });

        views.endCallButton.setOnClickListener(v -> {
            mainRepository.endCall();
            finish();
        });
    }

    @Override
    public void webrtcConnected() {
        runOnUiThread(() -> {
            views.incomingCallLayout.setVisibility(View.GONE);
            views.whoToCallLayout.setVisibility(View.GONE);
            views.callLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void webrtcClosed() {
        runOnUiThread(this::finish);
    }
}
