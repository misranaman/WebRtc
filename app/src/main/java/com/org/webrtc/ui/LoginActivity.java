package com.org.webrtc.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import com.org.webrtc.R;
import com.org.webrtc.databinding.ActivityLoginBinding;
import com.org.webrtc.repository.MainRepository;
import com.permissionx.guolindev.PermissionX;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding views;
    private MainRepository mainRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        views = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(views.getRoot());
        init();
    }

    private void init() {

        mainRepository = MainRepository.getInstance();

        views.enterBtn.setOnClickListener(v -> {
            PermissionX.init(this)
                    .permissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    .request((allGranted, grantedList, deniedList) -> {
                        if (allGranted) {
                            //login to firebase here
                            mainRepository.login(views.username.getText().toString(), getApplicationContext(), () -> {
                                //If success we want to move to call activity
                                startActivity(new Intent(LoginActivity.this, CallActivity.class));
                            });
                        }
                    });
        });
    }
}