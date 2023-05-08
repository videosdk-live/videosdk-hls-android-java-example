package live.videosdk.android.hlsdemo.speakerMode.stage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import live.videosdk.android.hlsdemo.common.meeting.activity.CreateOrJoinActivity;
import live.videosdk.android.hlsdemo.common.meeting.activity.MainActivity;
import live.videosdk.android.hlsdemo.common.utils.NetworkUtils;
import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.viewerMode.ViewerFragment;
import live.videosdk.android.hlsdemo.common.utils.ResponseListener;
import live.videosdk.android.hlsdemo.common.reactions.DirectionGenerator;
import live.videosdk.android.hlsdemo.common.reactions.ZeroGravityAnimation;
import live.videosdk.rtc.android.CustomStreamTrack;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.Stream;
import live.videosdk.rtc.android.VideoSDK;
import live.videosdk.rtc.android.VideoView;
import live.videosdk.rtc.android.lib.AppRTCAudioManager;
import live.videosdk.rtc.android.lib.JsonUtils;
import live.videosdk.rtc.android.lib.PubSubMessage;
import live.videosdk.rtc.android.listeners.MeetingEventListener;
import live.videosdk.rtc.android.listeners.ParticipantEventListener;
import live.videosdk.rtc.android.listeners.PubSubMessageListener;

public class StageFragment extends Fragment {

    GridLayout speakerGridLayout;
    private boolean micEnabled = false;
    private boolean webcamEnabled = false;
    private boolean recordingEnabled = false;
    private boolean hlsEnabled = false;
    private static Meeting meeting;
    private Map<String, View> speakerView = new HashMap<>();
    private List<Participant> speakerList = new ArrayList<>();
    private MaterialButton btnMic, btnWebcam, btnScreenshare, btnSetting, btnRecording, btnHls, recordingIndicator, viewerCount;
    private boolean fullScreen = false;
    private Toolbar toolbar;
    private Snackbar screenShareParticipantNameSnackbar;
    private boolean screenshareEnabled;
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    private VideoView shareView;
    private FrameLayout shareLayout;
    private String selectedAudioDeviceName;
    private String facingMode;
    private RelativeLayout stageLayout;
    private LinearLayout localScreenShareView;
    private TextView tvScreenShareParticipantName;
    private static Activity mActivity;
    private static Context mContext;
    private PubSubMessageListener emojiListener, removeCoHostListener;
    private ViewGroup speakerEmojiHolder;
    private ImageView btnLeave;

    public StageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof Activity) {
            mActivity = (Activity) context;
            meeting = ((MainActivity) mActivity).getMeeting();
            facingMode = ((MainActivity) mActivity).getFacingMode();
        }
    }


    @Override
    public void onDestroy() {
        mContext = null;
        mActivity = null;
        for (int i = 0; i < speakerGridLayout.getChildCount(); i++) {
            View view = speakerGridLayout.getChildAt(i);
            VideoView videoView = view.findViewById(R.id.speakerVideoView);
            if (videoView != null) {
                videoView.setVisibility(View.GONE);
                videoView.releaseSurfaceViewRenderer();
            }
        }

        speakerGridLayout.removeAllViews();
        speakerList = null;
        if (meeting != null) {
            meeting.pubSub.unsubscribe("emoji", emojiListener);
            meeting.pubSub.unsubscribe("removeCoHost", removeCoHostListener);
            meeting.getLocalParticipant().removeAllListeners();
            Iterator<Participant> participants = meeting.getParticipants().values().iterator();
            for (int i = 0; i < meeting.getParticipants().size(); i++) {
                final Participant participant = participants.next();
                participant.removeAllListeners();
            }
            meeting.removeAllListeners();
            meeting = null;
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stage, container, false);
        speakerGridLayout = view.findViewById(R.id.speakerGridLayout);
        btnMic = view.findViewById(R.id.btnMic);
        btnWebcam = view.findViewById(R.id.btnWebcam);
        btnScreenshare = view.findViewById(R.id.btnScreenShare);
        btnRecording = view.findViewById(R.id.btnRecording);
        btnSetting = view.findViewById(R.id.btnSetting);
        btnLeave = view.findViewById(R.id.btnSpeakerLeave);
        btnHls = view.findViewById(R.id.btnHls);
        recordingIndicator = view.findViewById(R.id.recordingIndicator);
        viewerCount = view.findViewById(R.id.viewerCount);

        toolbar = view.findViewById(R.id.material_toolbar);

        shareLayout = view.findViewById(R.id.shareLayout);
        shareView = view.findViewById(R.id.shareView);
        localScreenShareView = view.findViewById(R.id.localScreenShareView);
        tvScreenShareParticipantName = view.findViewById(R.id.tvScreenShareParticipantName);

        stageLayout = view.findViewById(R.id.stageLayout);
        speakerEmojiHolder = view.findViewById(R.id.speaker_emoji_holder);

        if (meeting != null) {
            meeting.addEventListener(meetingEventListener);
            speakerList.add(meeting.getLocalParticipant());
            showInGUI(speakerList);
            toggleMicIcon();
            toggleWebcamIcon();
            final Iterator<Participant> participantIterator = meeting.getParticipants().values().iterator();

            for (int i = 0; i < meeting.getParticipants().size(); i++) {
                final Participant participant = participantIterator.next();
                showParticipants(participant);
            }


            emojiListener = new PubSubMessageListener() {
                @Override
                public void onMessageReceived(PubSubMessage pubSubMessage) {
                    switch (pubSubMessage.getMessage()) {
                        case "loveEyes":
                            showEmoji(getResources().getDrawable(R.drawable.love_eyes_emoji));
                            break;
                        case "laughing":
                            showEmoji(getResources().getDrawable(R.drawable.laughing));
                            break;

                        case "thumbs_up":
                            showEmoji(getResources().getDrawable(R.drawable.thumbs_up));
                            break;
                        case "celebration":
                            showEmoji(getResources().getDrawable(R.drawable.celebration));
                            break;

                        case "clap":
                            showEmoji(getResources().getDrawable(R.drawable.clap));
                            break;
                        case "heart":
                            showEmoji(getResources().getDrawable(R.drawable.heart));
                            break;
                    }

                }
            };
            // notify user of any new emoji
            meeting.pubSub.subscribe("emoji", emojiListener);

            removeCoHostListener = new PubSubMessageListener() {
                @Override
                public void onMessageReceived(PubSubMessage pubSubMessage) {
                    if (pubSubMessage.getMessage().equals(meeting.getLocalParticipant().getId())) {
                        meeting.changeMode("VIEWER");
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainLayout, new ViewerFragment(), "viewerFragment")
                                .commit();
                    }
                }
            };
            meeting.pubSub.subscribe("removeCoHost", removeCoHostListener);

            setActionListeners();

            setAudioDeviceListeners();

            NetworkUtils networkUtils = new NetworkUtils(getContext());
            if (networkUtils.isNetworkAvailable()) {
                networkUtils.getToken(new ResponseListener<String>() {
                    @Override
                    public void onResponse(String token) {
                        networkUtils.checkActiveHls(token, meeting.getMeetingId(), new ResponseListener<String>() {
                            @Override
                            public void onResponse(String response) {
                                hlsEnabled = true;
                                btnHls.setText("Stop live");
                                viewerCount.setVisibility(View.VISIBLE);
                                showViewerCount();
                            }
                        });
                    }
                });
            }

        }

        view.findViewById(R.id.controllers).bringToFront();
        view.findViewById(R.id.material_toolbar).bringToFront();

        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    if (fullScreen) {
                        toolbar.setVisibility(View.VISIBLE);
                        for (int i = 0; i < toolbar.getChildCount(); i++) {
                            toolbar.getChildAt(i).setVisibility(View.VISIBLE);
                        }

                        TranslateAnimation toolbarAnimation = new TranslateAnimation(
                                0,
                                0,
                                0,
                                10);
                        toolbarAnimation.setDuration(500);
                        toolbarAnimation.setFillAfter(true);
                        toolbar.startAnimation(toolbarAnimation);

                        RelativeLayout controllers = view.findViewById(R.id.controllers);
                        controllers.setVisibility(View.VISIBLE);
                        for (int i = 0; i < controllers.getChildCount(); i++) {
                            controllers.getChildAt(i).setVisibility(View.VISIBLE);
                        }

                        TranslateAnimation animate = new TranslateAnimation(
                                0,
                                0,
                                controllers.getHeight(),
                                0);
                        animate.setDuration(300);
                        animate.setFillAfter(true);
                        controllers.startAnimation(animate);

                        TabLayout tabLayout = mActivity.findViewById(R.id.tabLayout);
                        tabLayout.setVisibility(View.VISIBLE);
                        for (int i = 0; i < tabLayout.getChildCount(); i++) {
                            tabLayout.getChildAt(i).setVisibility(View.VISIBLE);
                        }

                        TranslateAnimation translateAnimation = new TranslateAnimation(
                                0,
                                0,
                                tabLayout.getHeight(),
                                0);
                        animate.setDuration(300);
                        animate.setFillAfter(true);
                        tabLayout.startAnimation(translateAnimation);
                    } else {
                        toolbar.setVisibility(View.GONE);
                        for (int i = 0; i < toolbar.getChildCount(); i++) {
                            toolbar.getChildAt(i).setVisibility(View.GONE);
                        }

                        TranslateAnimation toolbarAnimation = new TranslateAnimation(
                                0,
                                0,
                                0,
                                10);
                        toolbarAnimation.setDuration(500);
                        toolbarAnimation.setFillAfter(true);
                        toolbar.startAnimation(toolbarAnimation);

                        RelativeLayout controllers = view.findViewById(R.id.controllers);
                        controllers.setVisibility(View.GONE);
                        for (int i = 0; i < controllers.getChildCount(); i++) {
                            controllers.getChildAt(i).setVisibility(View.GONE);
                        }

                        TranslateAnimation animate = new TranslateAnimation(
                                0,
                                0,
                                0,
                                controllers.getHeight());
                        animate.setDuration(400);
                        animate.setFillAfter(true);
                        controllers.startAnimation(animate);

                        TabLayout tabLayout = mActivity.findViewById(R.id.tabLayout);
                        tabLayout.setVisibility(View.GONE);
                        for (int i = 0; i < tabLayout.getChildCount(); i++) {
                            tabLayout.getChildAt(i).setVisibility(View.GONE);
                        }

                        TranslateAnimation translateAnimation = new TranslateAnimation(
                                0,
                                0,
                                0,
                                tabLayout.getHeight());
                        animate.setDuration(400);
                        animate.setFillAfter(true);
                        tabLayout.startAnimation(translateAnimation);
                    }
                    fullScreen = !fullScreen;
                }
                return true;
            }
        };

        view.findViewById(R.id.speaker_linearLayout).setOnTouchListener(onTouchListener);

        view.findViewById(R.id.btnStopScreenShare).setOnClickListener(v -> {
            if (screenshareEnabled) {
                if (meeting != null)
                    meeting.disableScreenShare();
            }
        });
        return view;
    }


    private final MeetingEventListener meetingEventListener = new MeetingEventListener() {
        @Override
        public void onMeetingLeft() {
            if (isAdded()) {
                Intent intents = new Intent(mContext, CreateOrJoinActivity.class);
                intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intents);
                mActivity.finish();
            }
        }

        @Override
        public void onParticipantJoined(Participant participant) {
            showParticipants(participant);
        }

        @Override
        public void onParticipantLeft(Participant participant) {
            if (speakerList.contains(participant)) {
                participant.unpin("SHARE_AND_CAM");
                Map<String, Participant> participants = meeting.getParticipants();
                List<Participant> participantList = new ArrayList<>();
                participantList.add(meeting.getLocalParticipant());
                if (screenshareEnabled) {
                    for (Map.Entry<String, Participant> entry : participants.entrySet()) {
                        if (entry.getValue().getMode().equals("CONFERENCE")) {
                            participantList.add(entry.getValue());
                            for (Map.Entry<String, Stream> entry1 : entry.getValue().getStreams().entrySet()) {
                                Stream stream = entry1.getValue();
                                stream.resume();
                            }
                            if (participantList.size() == 2)
                                break;
                        }
                    }
                    showInGUI(participantList);

                } else {
                    for (Map.Entry<String, Participant> entry : participants.entrySet()) {
                        if (entry.getValue().getMode().equals("CONFERENCE")) {
                            participantList.add(entry.getValue());
                            for (Map.Entry<String, Stream> entry1 : entry.getValue().getStreams().entrySet()) {
                                Stream stream = entry1.getValue();
                                stream.resume();
                            }
                            if (participantList.size() == 4)
                                break;
                        }
                    }
                    showInGUI(participantList);
                }

                speakerList = new ArrayList<>();
                speakerList.add(meeting.getLocalParticipant());
                for (Map.Entry<String, Participant> entry : participants.entrySet()) {
                    if (entry.getValue().getMode().equals("CONFERENCE")) {
                        speakerList.add(entry.getValue());
                        if (speakerList.size() == 4)
                            break;
                    }
                }

                updateGridLayout(screenshareEnabled);
            }
            if (hlsEnabled)
                showViewerCount();
        }

        @Override
        public void onParticipantModeChanged(JSONObject data) {
            try {

                if (!meeting.getLocalParticipant().getId().equals(data.getString("peerId"))) {

                    Participant participant = meeting.getParticipants().get(data.getString("peerId"));

                    if (data.getString("mode").equals("CONFERENCE")) {

                        if (participant.getMode().equals("CONFERENCE") && speakerList.size() < 4) {
                            participant.pin("SHARE_AND_CAM");
                            speakerList.add(participant);
                            if (screenshareEnabled) {
                                List<Participant> participants = new ArrayList<>();
                                for (int i = 0; i < 2; i++) {
                                    participants.add(speakerList.get(i));
                                }
                                showInGUI(participants);
                                updateGridLayout(true);
                            } else {
                                showInGUI(speakerList);
                                updateGridLayout(false);
                            }
                        } else {
                            for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                                Stream stream = entry.getValue();
                                stream.pause();
                            }
                        }
                    } else {
                        if (speakerList.contains(participant)) {
                            participant.unpin("SHARE_AND_CAM");
                            Map<String, Participant> participants = meeting.getParticipants();
                            Map<String, Participant> speakers = new HashMap<>();
                            for (Map.Entry<String, Participant> entry : participants.entrySet()) {
                                if (entry.getValue().getMode().equals("CONFERENCE")) {
                                    speakers.put(entry.getKey(), entry.getValue());
                                }
                            }
                            List<Participant> participantList = new ArrayList<>();
                            participantList.add(meeting.getLocalParticipant());
                            if (screenshareEnabled) {
                                for (Map.Entry<String, Participant> entry : speakers.entrySet()) {
                                    if (entry.getValue().getMode().equals("CONFERENCE")) {
                                        participantList.add(entry.getValue());
                                        for (Map.Entry<String, Stream> entry1 : entry.getValue().getStreams().entrySet()) {
                                            Stream stream = entry1.getValue();
                                            stream.resume();
                                        }
                                        if (participantList.size() == 2)
                                            break;
                                    }
                                }
                                showInGUI(participantList);

                            } else {
                                for (Map.Entry<String, Participant> entry : speakers.entrySet()) {
                                    if (entry.getValue().getMode().equals("CONFERENCE")) {
                                        participantList.add(entry.getValue());
                                        for (Map.Entry<String, Stream> entry1 : entry.getValue().getStreams().entrySet()) {
                                            Stream stream = entry1.getValue();
                                            stream.resume();
                                        }
                                        if (participantList.size() == 4)
                                            break;
                                    }
                                }
                                showInGUI(participantList);
                            }

                            speakerList = new ArrayList<>();
                            speakerList.add(meeting.getLocalParticipant());
                            for (Map.Entry<String, Participant> entry : speakers.entrySet()) {
                                if (entry.getValue().getMode().equals("CONFERENCE")) {
                                    speakerList.add(entry.getValue());
                                    if (speakerList.size() == 4)
                                        break;
                                }
                            }

                            updateGridLayout(screenshareEnabled);
                        }
                    }

                    if (hlsEnabled)
                        showViewerCount();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRecordingStateChanged(String recordingState) {
            if (recordingState.equals("RECORDING_STARTING")) {
                Toast.makeText(mContext, "Recording is starting", Toast.LENGTH_SHORT).show();
            }
            if (recordingState.equals("RECORDING_STARTED")) {
                Toast.makeText(mContext, "Recording started", Toast.LENGTH_SHORT).show();
                recordingEnabled = true;
                toggleRecordingIcon();
                recordingIndicator.setVisibility(View.VISIBLE);

            }
            if (recordingState.equals("RECORDING_STOPPED")) {
                Toast.makeText(mContext, "Recording stopped", Toast.LENGTH_SHORT).show();
                recordingEnabled = false;
                toggleRecordingIcon();
                recordingIndicator.setVisibility(View.GONE);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onHlsStateChanged(JSONObject HlsState) {
            if (HlsState.has("status")) {
                try {
                    if (HlsState.getString("status").equals("HLS_STARTING")) {
                        Toast.makeText(mContext, "HLS is starting", Toast.LENGTH_SHORT).show();
                    }
                    if (HlsState.getString("status").equals("HLS_STARTED")) {
                        Toast.makeText(mContext, "HLS started", Toast.LENGTH_SHORT).show();
                        hlsEnabled = true;
                        btnHls.setText("Stop live");
                        viewerCount.setVisibility(View.VISIBLE);
                        showViewerCount();
                    }
                    if (HlsState.getString("status").equals("HLS_STOPPING")) {
                        Snackbar snackbar = Snackbar.make(stageLayout, "HLS will be stopped in few moments",
                                Snackbar.LENGTH_SHORT);
                        int snackbarTextId = com.google.android.material.R.id.snackbar_text;
                        TextView textView = snackbar.getView().findViewById(snackbarTextId);
                        textView.setTextSize(15);
                        textView.setTypeface(Typeface.create(null, 700, false));
                        snackbar.setGestureInsetBottomIgnored(true);
                        snackbar.getView().setOnClickListener(view -> snackbar.dismiss());
                        snackbar.show();
                    }
                    if (HlsState.getString("status").equals("HLS_STOPPED")) {
                        hlsEnabled = false;
                        btnHls.setText("Go live");
                        viewerCount.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onPresenterChanged(String participantId) {
            super.onPresenterChanged(participantId);
            if (!TextUtils.isEmpty(participantId)) {
                if (meeting.getLocalParticipant().getId().equals(participantId)) {
                    localScreenShareView.setVisibility(View.VISIBLE);
                    localScreenShareView.bringToFront();
                } else {
                    updatePresenter(meeting.getParticipants().get(participantId));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    showScreenshareSnackbar();
                }
                toolbar.bringToFront();
                screenshareEnabled = true;
                List<Participant> participants = new ArrayList<>();
                participants.add(meeting.getLocalParticipant());
                if (speakerList.size() > 1) participants.add(speakerList.get(1));
                showInGUI(participants);
                updateGridLayout(true);
                btnScreenshare.setEnabled(false);
            } else {
                if (localScreenShareView.getVisibility() == View.VISIBLE) {
                    localScreenShareView.setVisibility(View.GONE);
                } else {
                    shareView.removeTrack();
                    shareView.setVisibility(View.GONE);
                    shareLayout.setVisibility(View.GONE);
                    tvScreenShareParticipantName.setVisibility(View.GONE);
                }
                screenshareEnabled = false;
                showInGUI(speakerList);
                updateGridLayout(false);
                btnScreenshare.setEnabled(true);
            }
            toggleScreenShareIcon();
        }


        @Override
        public void onSpeakerChanged(String participantId) {
            if (!isNullOrEmpty(participantId)) {
                if (speakerView != null) {
                    if (speakerView.containsKey(participantId)) {
                        View participantView = speakerView.get(participantId);
                        CardView participantCard = participantView.findViewById(R.id.ParticipantCard);
                        participantCard.setForeground(mContext.getDrawable(R.drawable.layout_bg));
                    } else {
                        Participant activeSpeaker = meeting.getParticipants().get(participantId);
                        for (Map.Entry<String, Stream> entry : activeSpeaker.getStreams().entrySet()) {
                            Stream stream = entry.getValue();
                            stream.resume();
                        }
                        List<Participant> participants = new ArrayList<>();
                        participants.add(meeting.getLocalParticipant());
                        participants.add(activeSpeaker);
                        if (screenshareEnabled) {
                            showInGUI(participants);
                            updateGridLayout(true);
                        } else {
                            for (int i = 1; i < 3; i++) {
                                participants.add(speakerList.get(i));
                            }
                            showInGUI(participants);
                            for (Map.Entry<String, Stream> entry : speakerList.get(speakerList.size() - 1).getStreams().entrySet()) {
                                Stream stream = entry.getValue();
                                stream.pause();
                            }
                            speakerList = participants;
                            updateGridLayout(false);
                        }
                        View participantView = speakerView.get(participantId);
                        CardView participantCard = participantView.findViewById(R.id.ParticipantCard);
                        participantCard.setForeground(mContext.getDrawable(R.drawable.layout_bg));
                    }

                }
            } else {
                for (Map.Entry<String, View> entry : speakerView.entrySet()) {
                    View participantView = entry.getValue();
                    CardView participantCard = participantView.findViewById(R.id.ParticipantCard);
                    participantCard.setForeground(null);
                }
            }
        }

    };

    private void showParticipants(Participant participant) {
        if (participant.getMode().equals("CONFERENCE") && speakerList.size() < 4) {
            participant.pin("SHARE_AND_CAM");
            speakerList.add(participant);
            if (screenshareEnabled) {
                List<Participant> participants = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    participants.add(speakerList.get(i));
                }
                showInGUI(participants);
                updateGridLayout(true);
            } else {
                showInGUI(speakerList);
                updateGridLayout(false);
            }
        } else {
            for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                Stream stream = entry.getValue();
                stream.pause();
            }
        }
        if (hlsEnabled)
            showViewerCount();
    }

    private void updatePresenter(Participant participant) {
        if (participant == null) return;

        // find share stream in participant
        Stream shareStream = null;

        for (Stream stream : participant.getStreams().values()) {
            if (stream.getKind().equals("share")) {
                shareStream = stream;
                break;
            }
        }

        if (shareStream == null) return;
        tvScreenShareParticipantName.setText(participant.getDisplayName() + " is presenting");
        tvScreenShareParticipantName.setVisibility(View.VISIBLE);

        // display share video
        shareLayout.setVisibility(View.VISIBLE);
        shareLayout.bringToFront();
        shareView.setVisibility(View.VISIBLE);
        shareView.setZOrderMediaOverlay(true);
        shareView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        VideoTrack videoTrack = (VideoTrack) shareStream.getTrack();
        shareView.addTrack(videoTrack);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            showScreenshareSnackbar();
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return "null".equals(str) || "".equals(str) || null == str;
    }

    private void showInGUI(List<Participant> participantList) {
        if (speakerView != null) {
            for (Map.Entry<String, View> entry : speakerView.entrySet()) {
                String key = entry.getKey();
                Participant participant = meeting.getParticipants().get(key);
                if (participant != null)
                    participant.removeAllListeners();
                VideoView participantVideoView = speakerView.get(key).findViewById(R.id.speakerVideoView);
                participantVideoView.releaseSurfaceViewRenderer();
                speakerGridLayout.removeView(speakerView.get(key));
            }
        }
        speakerView = new HashMap<>();

        for (int i = 0; i < participantList.size(); i++) {
            Participant participant = participantList.get(i);

            if (participant != null) {
                View participantView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.item_speaker, speakerGridLayout, false);

                speakerView.put(participant.getId(), participantView);

                ImageView ivMicStatus = participantView.findViewById(R.id.ivMicStatus);

                TextView tvName = participantView.findViewById(R.id.tvName);
                TextView txtParticipantName = participantView.findViewById(R.id.txtParticipantName);

                VideoView participantVideoView = participantView.findViewById(R.id.speakerVideoView);

                if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                    tvName.setText("You");
                } else {
                    tvName.setText(participant.getDisplayName());
                }
                txtParticipantName.setText(participant.getDisplayName().substring(0, 1));

                for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
                    Stream stream = entry.getValue();
                    if (stream.getKind().equalsIgnoreCase("video")) {
                        participantVideoView.setVisibility(View.VISIBLE);
                        VideoTrack videoTrack = (VideoTrack) stream.getTrack();
                        participantVideoView.addTrack(videoTrack);
                        if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                            webcamEnabled = true;
                            toggleWebcamIcon();
                        }
                        break;
                    } else if (stream.getKind().equalsIgnoreCase("audio")) {
                        ivMicStatus.setImageResource(R.drawable.ic_audio_on);
                        if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                            micEnabled = true;
                            toggleMicIcon();
                        }

                    }
                }

                participant.addEventListener(new ParticipantEventListener() {
                    @Override
                    public void onStreamEnabled(Stream stream) {
                        if (stream.getKind().equalsIgnoreCase("video")) {
                            participantVideoView.setVisibility(View.VISIBLE);

                            VideoTrack videoTrack = (VideoTrack) stream.getTrack();
                            participantVideoView.addTrack(videoTrack);
                            if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                                webcamEnabled = true;
                                toggleWebcamIcon();
                            }
                        } else if (stream.getKind().equalsIgnoreCase("audio")) {
                            ivMicStatus.setImageResource(R.drawable.ic_audio_on);
                            if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                                micEnabled = true;
                                toggleMicIcon();
                            }
                        }
                    }

                    @Override
                    public void onStreamDisabled(Stream stream) {
                        if (stream.getKind().equalsIgnoreCase("video")) {
                            VideoTrack track = (VideoTrack) stream.getTrack();
                            if (track != null) participantVideoView.removeTrack();

                            participantVideoView.setVisibility(View.GONE);
                            if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                                webcamEnabled = false;
                                toggleWebcamIcon();
                            }

                        } else if (stream.getKind().equalsIgnoreCase("audio")) {
                            ivMicStatus.setImageResource(R.drawable.ic_audio_off);
                            if (participant.getId().equals(meeting.getLocalParticipant().getId())) {
                                micEnabled = false;
                                toggleMicIcon();
                            }
                        }
                    }
                });

                speakerGridLayout.addView(participantView);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void showScreenshareSnackbar() {
        screenShareParticipantNameSnackbar = Snackbar.make(stageLayout, "You started presenting",
                Snackbar.LENGTH_SHORT);
        int snackbarTextId = com.google.android.material.R.id.snackbar_text;
        TextView textView = (TextView) screenShareParticipantNameSnackbar.getView().findViewById(snackbarTextId);
        textView.setTextSize(15);
        textView.setTypeface(Typeface.create(null, 700, false));
        screenShareParticipantNameSnackbar.setGestureInsetBottomIgnored(true);
        screenShareParticipantNameSnackbar.getView().setOnClickListener(view -> screenShareParticipantNameSnackbar.dismiss());
        screenShareParticipantNameSnackbar.show();
    }

    public void updateGridLayout(boolean screenShareFlag) {
        if (screenShareFlag) {
            int col = 0, row = 0;
            for (int i = 0; i < speakerGridLayout.getChildCount(); i++) {
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) speakerGridLayout.getChildAt(i).getLayoutParams();
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                if (col + 1 == 2) {
                    col = 0;
                    row++;
                } else {
                    col++;
                }

            }
            speakerGridLayout.requestLayout();
        } else {
            int col = 0, row = 0;
            for (int i = 0; i < speakerGridLayout.getChildCount(); i++) {
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) speakerGridLayout.getChildAt(i).getLayoutParams();
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                if (col + 1 == getNormalLayoutColumnCount()) {
                    col = 0;
                    row++;
                } else {
                    col++;
                }

            }
            speakerGridLayout.requestLayout();
        }

    }

    private int getNormalLayoutRowCount() {
        return Math.min(Math.max(1, speakerList.size()), 2);
    }

    private int getNormalLayoutColumnCount() {
        int maxColumns = 2;
        int result = Math.max(1, (speakerList.size() + getNormalLayoutRowCount() - 1) / getNormalLayoutRowCount());
        if (result > maxColumns) {
            throw new IllegalStateException(result +
                    "videos not allowed."
            );
        }
        return result;
    }

    private void askPermissionForScreenShare() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getActivity().getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        if (resultCode != Activity.RESULT_OK) {
            Toast.makeText(mContext, "You didn't give permission to capture the screen.", Toast.LENGTH_SHORT).show();
            return;
        }

        meeting.enableScreenShare(data);
    }

    private void setActionListeners() {
        btnMic.setOnClickListener(v -> {
            if (micEnabled) {
                meeting.muteMic();
            } else {
                CustomStreamTrack audioCustomTrack = VideoSDK.createAudioTrack("high_quality", mContext);

                meeting.unmuteMic(audioCustomTrack);
            }
        });

        btnWebcam.setOnClickListener(v -> {
            if (webcamEnabled) {
                meeting.disableWebcam();
            } else {
                CustomStreamTrack videoCustomTrack = VideoSDK.createCameraVideoTrack("h720p_w960p", facingMode, CustomStreamTrack.VideoMode.DETAIL, true, mContext);
                meeting.enableWebcam(videoCustomTrack);
            }
        });

        btnRecording.setOnClickListener(v -> {
            if (recordingEnabled)
                stopRecordingConfirmDialog();
            else {
                meeting.startRecording(null, null, null);
            }

        });

        btnLeave.setOnClickListener(v -> ((MainActivity) mActivity).showLeaveDialog());

        btnScreenshare.setOnClickListener(v -> {
            if (!screenshareEnabled) {
                askPermissionForScreenShare();
            } else {
                meeting.disableScreenShare();
            }
        });

        btnHls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hlsEnabled) {
                    JSONObject config = new JSONObject();
                    JSONObject layout = new JSONObject();
                    JsonUtils.jsonPut(layout, "type", "SPOTLIGHT");
                    JsonUtils.jsonPut(layout, "priority", "PIN");
                    JsonUtils.jsonPut(layout, "gridSize", 12);
                    JsonUtils.jsonPut(config, "layout", layout);
                    JsonUtils.jsonPut(config, "orientation", "portrait");
                    JsonUtils.jsonPut(config, "theme", "DARK");
                    meeting.startHls(config);
                } else {
                    meeting.stopHls();
                }
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
    }

    private void toggleMicIcon() {
        if (micEnabled) {
            btnMic.setIcon(mContext.getResources().getDrawable(R.drawable.ic_mic_on));
            btnMic.setIconTintResource(R.color.white);
            btnMic.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.semiTransperentColor));
        } else {
            btnMic.setIcon(mContext.getResources().getDrawable(R.drawable.ic_mic_off));
            btnMic.setIconTintResource(R.color.black);
            btnMic.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.white));
        }
    }


    private void toggleWebcamIcon() {
        if (webcamEnabled) {
            btnWebcam.setIcon(mContext.getResources().getDrawable(R.drawable.ic_video_camera));
            btnWebcam.setIconTintResource(R.color.white);
            btnWebcam.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.semiTransperentColor));
        } else {
            btnWebcam.setIcon(mContext.getResources().getDrawable(R.drawable.ic_video_camera_off));
            btnWebcam.setIconTintResource(R.color.black);
            btnWebcam.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.white));
        }
    }

    private void toggleRecordingIcon() {
        if (!recordingEnabled) {
            btnRecording.setIcon(mContext.getResources().getDrawable(R.drawable.ic_recording));
            btnRecording.setIconTintResource(R.color.white);
            btnRecording.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.semiTransperentColor));
        } else {
            btnRecording.setIcon(mContext.getResources().getDrawable(R.drawable.stop_recording));
            btnRecording.setIconTintResource(R.color.black);
            btnRecording.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.white));
        }
    }

    private void toggleScreenShareIcon() {
        if (!screenshareEnabled) {
            btnScreenshare.setIcon(mContext.getResources().getDrawable(R.drawable.ic_screen_share));
            btnScreenshare.setIconTintResource(R.color.white);
            btnScreenshare.setBackgroundTintList(ContextCompat.getColorStateList(mContext, R.color.semiTransperentColor));
        } else {
            btnScreenshare.setIconTintResource(R.color.md_grey_10);
        }
    }

    private void stopRecordingConfirmDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(mContext, R.style.AlertDialogCustom).create();

        LayoutInflater inflater = mActivity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_style, null);
        alertDialog.setView(dialogView);

        TextView message = dialogView.findViewById(R.id.message);
        message.setText("Are you sure you want to " + System.getProperty("line.separator") + "          stop recording?");

        Button positiveButton = dialogView.findViewById(R.id.positiveBtn);
        positiveButton.setText("Yes");
        positiveButton.setOnClickListener(v -> {
            meeting.stopRecording();
            recordingEnabled = !recordingEnabled;
            alertDialog.dismiss();
        });

        Button negativeButton = dialogView.findViewById(R.id.negativeBtn);
        negativeButton.setText("No");
        negativeButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void showViewerCount() {
        int viewerCount = 0;
        Map<String, Participant> participants = meeting.getParticipants();
        for (Map.Entry<String, Participant> entry : participants.entrySet()) {
            Participant participant = entry.getValue();
            if (participant.getMode().equals("VIEWER")) {
                viewerCount++;
            }
        }

        this.viewerCount.setText(String.valueOf(viewerCount));
    }

    public void showSettings() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mContext);
        View v3 = LayoutInflater.from(mContext).inflate(R.layout.settings_layout, null);
        bottomSheetDialog.setContentView(v3);
        ImageView close = v3.findViewById(R.id.ic_close);
        bottomSheetDialog.show();
        close.setOnClickListener(view -> bottomSheetDialog.dismiss());

        //mic settings
        AutoCompleteTextView micDevice = v3.findViewById(R.id.micDevice);
        String selectedMic = selectedAudioDeviceName.substring(0, 1).toUpperCase() + selectedAudioDeviceName.substring(1).toLowerCase();
        selectedMic = selectedMic.replace("_", " ");
        micDevice.setText(selectedMic);

        micDevice.setDropDownBackgroundDrawable(
                ResourcesCompat.getDrawable(
                        mContext.getResources(),
                        R.drawable.dropdown_style,
                        null
                )
        );

        Set<AppRTCAudioManager.AudioDevice> mics = meeting.getMics();
        ArrayList<String> audioDeviceList = new ArrayList<>();
        // Prepare list
        String item;
        for (int i = 0; i < mics.size(); i++) {
            item = mics.toArray()[i].toString();
            String mic = item.substring(0, 1).toUpperCase() + item.substring(1).toLowerCase();
            mic = mic.replace("_", " ");
            audioDeviceList.add(mic);
        }

        ArrayAdapter micArrayAdapter = new ArrayAdapter(mContext, R.layout.custom_drop_down_item, audioDeviceList);
        micDevice.setAdapter(micArrayAdapter);

        micDevice.setOnItemClickListener((adapterView, view, i, l) -> {
            AppRTCAudioManager.AudioDevice audioDevice = null;
            switch (audioDeviceList.get(i)) {
                case "Bluetooth":
                    audioDevice = AppRTCAudioManager.AudioDevice.BLUETOOTH;
                    break;
                case "Wired headset":
                    audioDevice = AppRTCAudioManager.AudioDevice.WIRED_HEADSET;
                    break;
                case "Speaker phone":
                    audioDevice = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE;
                    break;
                case "Earpiece":
                    audioDevice = AppRTCAudioManager.AudioDevice.EARPIECE;
                    break;
            }

            meeting.changeMic(audioDevice, VideoSDK.createAudioTrack("high_quality", mContext));

        });


        // video setting
        AutoCompleteTextView facingModeTextView = v3.findViewById(R.id.facingMode);
        facingModeTextView.setText(facingMode.substring(0, 1).toUpperCase() + facingMode.substring(1).toLowerCase());

        facingModeTextView.setDropDownBackgroundDrawable(
                ResourcesCompat.getDrawable(
                        mContext.getResources(),
                        R.drawable.dropdown_style,
                        null
                )
        );

        String[] facingModes = mContext.getResources().getStringArray(R.array.facingModes);

        ArrayAdapter modeArrayAdapter = new ArrayAdapter(mContext, R.layout.custom_drop_down_item, facingModes);
        facingModeTextView.setAdapter(modeArrayAdapter);

        facingModeTextView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (i == 0) {
                if (facingMode.equals("back")) {
                    meeting.changeWebcam();
                    facingMode = facingModes[i].toLowerCase();
                }
            }
            if (i == 1) {
                if (facingMode.equals("front")) {
                    meeting.changeWebcam();
                    facingMode = facingModes[i].toLowerCase();
                }
            }
        });
    }

    private void setAudioDeviceListeners() {
        meeting.setAudioDeviceChangeListener((selectedAudioDevice, availableAudioDevices) -> selectedAudioDeviceName = selectedAudioDevice.toString());
    }

    public void showEmoji(Drawable drawable) {
        // You can change the number of emojis that will be flying on screen
        for (int i = 0; i < 5; i++) {
            flyObject(drawable, 3000, DirectionGenerator.Direction.BOTTOM, DirectionGenerator.Direction.TOP, 1);
        }
    }

    public void flyObject(final Drawable drawable, final int duration, final DirectionGenerator.Direction from, final DirectionGenerator.Direction to, final float scale) {
        ZeroGravityAnimation animation = new ZeroGravityAnimation();
        animation.setCount(1);
        animation.setScalingFactor(scale);
        animation.setOriginationDirection(from);
        animation.setDestinationDirection(to);
        animation.setImage(drawable);
        animation.setDuration(duration);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        speakerEmojiHolder.bringToFront();
        animation.play(mActivity, speakerEmojiHolder);
    }

}