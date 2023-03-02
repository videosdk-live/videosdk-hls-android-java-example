package live.videosdk.android.hlsdemo.common.meeting.activity;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.speakerMode.manageTabs.SpeakerFragment;
import live.videosdk.android.hlsdemo.viewerMode.ViewerFragment;
import live.videosdk.rtc.android.CustomStreamTrack;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.VideoSDK;
import live.videosdk.rtc.android.listeners.MeetingEventListener;

public class MainActivity extends AppCompatActivity {

    private Meeting meeting;
    private String facingMode;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String mode = getIntent().getStringExtra("mode");
        String token = getIntent().getStringExtra("token");
        boolean micEnabled = getIntent().getBooleanExtra("micEnabled", false);
        boolean webcamEnabled = getIntent().getBooleanExtra("webcamEnabled", false);

        final String meetingId = getIntent().getStringExtra("meetingId");

        String localParticipantName = getIntent().getStringExtra("participantName");
        if (localParticipantName == null) {
            localParticipantName = "John Doe";
        }

        // pass the token generated from api server
        VideoSDK.config(token);

        Map<String, CustomStreamTrack> customTracks = new HashMap<>();
        facingMode="front";

        CustomStreamTrack videoCustomTrack = VideoSDK.createCameraVideoTrack("h720p_w960p", facingMode, CustomStreamTrack.VideoMode.TEXT, this);
        customTracks.put("video", videoCustomTrack);

        CustomStreamTrack audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this);
        customTracks.put("mic", audioCustomTrack);

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(MainActivity.this, meetingId, localParticipantName,
                micEnabled, webcamEnabled, null, mode, customTracks
        );

        meeting.addEventListener(new MeetingEventListener() {
            @Override
            public void onMeetingJoined() {
                if (meeting != null) {
                    if (mode.equals("CONFERENCE")) {
                        meeting.getLocalParticipant().pin("SHARE_AND_CAM");
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainLayout, new SpeakerFragment(), "MainFragment")
                                .commit();
                        findViewById(R.id.progress_layout).setVisibility(View.GONE);

                    } else if (mode.equals("VIEWER")) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainLayout, new ViewerFragment(), "viewerFragment")
                                .commit();
                        findViewById(R.id.progress_layout).setVisibility(View.GONE);
                    }
                }
            }
        });

        checkPermissions();
    }


    private void checkPermissions() {
        List<String> permissionList = new ArrayList<>();
        permissionList.add(android.Manifest.permission.INTERNET);
        permissionList.add(android.Manifest.permission.MODIFY_AUDIO_SETTINGS);
        permissionList.add(android.Manifest.permission.RECORD_AUDIO);
        permissionList.add(android.Manifest.permission.CAMERA);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);

        String[] permissions = {};
        String rationale = "Please provide permissions";
        Permissions.Options options =
                new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
        Permissions.check(MainActivity.this, permissionList.toArray(permissions), rationale, options, permissionHandler);
    }

    private final PermissionHandler permissionHandler = new PermissionHandler() {
        @Override
        public void onGranted() {
            if (meeting != null) meeting.join();
        }
    };

    public Meeting getMeeting() {
        return meeting;
    }

    public String getFacingMode() {
        return facingMode;
    }

    @Override
    protected void onDestroy() {
        if (meeting != null) {
            // stop hls if there is no other speaker
            stopHLS();
            meeting = null;
        }
        super.onDestroy();

    }

    public void showLeaveDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogCustom).create();

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_style, null);
        alertDialog.setView(dialogView);

        TextView message = dialogView.findViewById(R.id.message);
        message.setText("Are you sure you want to leave?");

        Button positiveButton = dialogView.findViewById(R.id.positiveBtn);
        positiveButton.setText("Yes");
        positiveButton.setOnClickListener(v -> {
            meeting.leave();
            alertDialog.dismiss();
            Intent intents = new Intent(MainActivity.this, CreateOrJoinActivity.class);
            intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intents);
            finish();
        });

        Button negativeButton = dialogView.findViewById(R.id.negativeBtn);
        negativeButton.setText("No");
        negativeButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        showLeaveDialog();
    }

    private void stopHLS() {
        Iterator<Participant> participants = meeting.getParticipants().values().iterator();
        int speakers = 0;
        for (int i = 0; i < meeting.getParticipants().size(); i++) {
            final Participant participant = participants.next();
            if (participant.getMode().equals("CONFERENCE")) {
                speakers++;
            }
        }
        if (speakers == 0)
            meeting.stopHls();
    }
}

