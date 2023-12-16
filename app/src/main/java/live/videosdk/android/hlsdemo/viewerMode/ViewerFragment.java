package live.videosdk.android.hlsdemo.viewerMode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Map;
import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.common.meeting.activity.CreateOrJoinActivity;
import live.videosdk.android.hlsdemo.common.meeting.activity.MainActivity;
import live.videosdk.android.hlsdemo.common.reactions.DirectionGenerator;
import live.videosdk.android.hlsdemo.common.reactions.ZeroGravityAnimation;
import live.videosdk.android.hlsdemo.common.utils.NetworkUtils;
import live.videosdk.android.hlsdemo.common.utils.ResponseListener;
import live.videosdk.android.hlsdemo.speakerMode.manageTabs.SpeakerFragment;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.lib.JsonUtils;
import live.videosdk.rtc.android.lib.PubSubMessage;
import live.videosdk.rtc.android.listeners.MeetingEventListener;
import live.videosdk.rtc.android.listeners.PubSubMessageListener;
import live.videosdk.rtc.android.model.PubSubPublishOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ViewerFragment
  extends Fragment
  implements
    View.OnClickListener, StyledPlayerView.ControllerVisibilityListener {

  private Meeting meeting;
  private static final String KEY_TRACK_SELECTION_PARAMETERS =
    "track_selection_parameters";
  private static final String KEY_ITEM_INDEX = "item_index";
  private static final String KEY_POSITION = "position";
  private static final String KEY_AUTO_PLAY = "auto_play";

  protected StyledPlayerView playerView;
  protected LinearLayout controllerLayout;

  @Nullable
  protected ExoPlayer player;

  private boolean isShowingTrackSelectionDialog;
  private Button btnQuality;
  private DefaultHttpDataSource.Factory dataSourceFactory;
  private TrackSelectionParameters trackSelectionParameters;
  private Tracks lastSeenTracks;
  private boolean startAutoPlay;
  private int startItemIndex;
  private long startPosition;
  private ImageView btnLeave;
  private ImageButton exoPlay, exoPause;
  private boolean ended;
  private boolean hlsStarted = false;
  private String playbackHlsUrl = null;
  private PlayerEventListener playerEventListener;
  private MaterialButton btnReactions, btnAddToCart;
  private LinearLayout reactionsLayout;
  private PubSubMessageListener emojiListener, coHostListener;
  private MaterialButton viewerCount;
  private MaterialToolbar material_toolbar;
  private static Activity mActivity;
  private static Context mContext;
  private FrameLayout viewerEmojiHolder;
  private LinearLayout waitingLayout, stopLiveStreamLayout;
  private RelativeLayout liveActionsLayout;

  public ViewerFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @SuppressLint("MissingInflatedId")
  @Override
  public View onCreateView(
    LayoutInflater inflater,
    ViewGroup container,
    Bundle savedInstanceState
  ) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_viewer, container, false);
    controllerLayout = view.findViewById(R.id.viewerControllers);
    exoPlay = view.findViewById(R.id.exoPlay);
    exoPause = view.findViewById(R.id.exoPause);
    btnQuality = view.findViewById(R.id.btnQuality);
    btnQuality.setOnClickListener(this);
    btnLeave = view.findViewById(R.id.btnViewerLeave);
    reactionsLayout = view.findViewById(R.id.reactionsLayout);
    btnReactions = view.findViewById(R.id.btnReactions);
    btnAddToCart = view.findViewById(R.id.btnAddToCart);
    material_toolbar = view.findViewById(R.id.material_toolbar);
    material_toolbar.bringToFront();
    viewerCount = view.findViewById(R.id.viewerCount);
    viewerEmojiHolder = view.findViewById(R.id.viewer_emoji_holder);
    waitingLayout = view.findViewById(R.id.waiting_layout);
    stopLiveStreamLayout = view.findViewById(R.id.stop_liveStream_layout);
    liveActionsLayout = view.findViewById(R.id.live_actions_layout);

    playerView = view.findViewById(R.id.player_view);
    playerView.setControllerVisibilityListener(this);
    playerView.requestFocus();

    if (savedInstanceState != null) {
      trackSelectionParameters =
        TrackSelectionParameters.fromBundle(
          savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)
        );
      startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
      startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX);
      startPosition = savedInstanceState.getLong(KEY_POSITION);
    } else {
      trackSelectionParameters =
        new TrackSelectionParameters.Builder(/* context= */mContext).build();
      clearStartPosition();
    }

    exoPause.setOnClickListener(view1 -> {
      if (player != null) {
        player.setPlayWhenReady(false);
      }
    });

    exoPlay.setOnClickListener(view12 -> {
      if (player != null) {
        if (ended) {
          player.seekTo(0);
        }
        player.setPlayWhenReady(true);
      }
    });

    if (meeting != null) {
      emojiListener =
        new PubSubMessageListener() {
          @Override
          public void onMessageReceived(PubSubMessage pubSubMessage) {
            switch (pubSubMessage.getMessage()) {
              case "loveEyes":
                showEmoji(
                  getResources().getDrawable(R.drawable.love_eyes_emoji)
                );
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
      coHostListener =
        pubSubMessage -> {
          if (
            pubSubMessage
              .getMessage()
              .equals(meeting.getLocalParticipant().getId())
          ) {
            showCoHostRequestDialog(pubSubMessage.getSenderName());
          }
        };
      // notify user of any new emoji
      meeting.pubSub.subscribe("coHost", coHostListener);
    }

    ActionListener(view);

    NetworkUtils networkUtils = new NetworkUtils(getContext());
    if (networkUtils.isNetworkAvailable()) {
      networkUtils.getToken(
        new ResponseListener<String>() {
          @Override
          public void onResponse(String token) {
            networkUtils.checkActiveHls(
              token,
              meeting.getMeetingId(),
              url -> {
                playbackHlsUrl = url;
                ViewerFragment.this.initializePlayer();
                ViewerFragment.this.showViewerCount();
                hlsStarted = true;
                waitingLayout.setVisibility(View.GONE);
                liveActionsLayout.setVisibility(View.VISIBLE);
                playerView.setVisibility(View.VISIBLE);
                viewerEmojiHolder.setVisibility(View.VISIBLE);
                controllerLayout.setVisibility(View.VISIBLE);
                btnReactions.setEnabled(true);
                btnAddToCart.setEnabled(true);
              }
            );
          }
        }
      );
    }

    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mContext = context;
    if (context instanceof Activity) {
      mActivity = (Activity) context;
      meeting = ((MainActivity) mActivity).getMeeting();
    }
  }

  private void ActionListener(View view) {
    PubSubPublishOptions pubSubPublishOptions = new PubSubPublishOptions();
    pubSubPublishOptions.setPersist(false);

    btnReactions.setOnClickListener(v ->
      reactionsLayout.setVisibility(View.VISIBLE)
    );

    view
      .findViewById(R.id.loveEyes)
      .setOnClickListener(v -> {
        showEmoji(getResources().getDrawable(R.drawable.love_eyes_emoji));
        meeting.pubSub.publish("emoji", "love_eyes", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    view
      .findViewById(R.id.laughing)
      .setOnClickListener(v -> {
        meeting.pubSub.publish("emoji", "laughing", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    view
      .findViewById(R.id.thumbs_up)
      .setOnClickListener(v -> {
        meeting.pubSub.publish("emoji", "thumbs_up", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    view
      .findViewById(R.id.celebration)
      .setOnClickListener(v -> {
        meeting.pubSub.publish("emoji", "celebration", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    view
      .findViewById(R.id.clap)
      .setOnClickListener(v -> {
        meeting.pubSub.publish("emoji", "clap", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    view
      .findViewById(R.id.heart)
      .setOnClickListener(v -> {
        meeting.pubSub.publish("emoji", "heart", pubSubPublishOptions);
        reactionsLayout.setVisibility(View.GONE);
      });

    btnLeave.setOnClickListener(v ->
      ((MainActivity) mActivity).showLeaveDialog()
    );

    btnAddToCart.setOnClickListener(v -> showProducts());
  }

  private final MeetingEventListener meetingEventListener = new MeetingEventListener() {
    @Override
    public void onMeetingLeft() {
      if (isAdded()) {
        Intent intents = new Intent(mContext, CreateOrJoinActivity.class);
        intents.addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK |
          Intent.FLAG_ACTIVITY_CLEAR_TOP |
          Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intents);
        mActivity.finish();
      }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onHlsStateChanged(JSONObject HlsState) {
      if (HlsState.has("status")) {
        try {
          if (
            HlsState.getString("status").equals("HLS_PLAYABLE") &&
            HlsState.has("playbackHlsUrl")
          ) {
            playbackHlsUrl = HlsState.getString("playbackHlsUrl");
            initializePlayer();
            showViewerCount();
            hlsStarted = true;
            waitingLayout.setVisibility(View.GONE);
            liveActionsLayout.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.VISIBLE);
            viewerEmojiHolder.setVisibility(View.VISIBLE);
            controllerLayout.setVisibility(View.VISIBLE);
            btnReactions.setEnabled(true);
            btnAddToCart.setEnabled(true);
          }
          if (HlsState.getString("status").equals("HLS_STOPPED")) {
            if (hlsStarted) {
              releasePlayer();
              clearStartPosition();
              hlsStarted = false;
              stopLiveStreamLayout.setVisibility(View.VISIBLE);
              playerView.setVisibility(View.GONE);
              viewerEmojiHolder.setVisibility(View.GONE);
              controllerLayout.setVisibility(View.GONE);
              material_toolbar.setVisibility(View.VISIBLE);
              liveActionsLayout.setVisibility(View.GONE);
              btnReactions.setEnabled(false);
              btnAddToCart.setEnabled(false);
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void onParticipantJoined(Participant participant) {
      showViewerCount();
    }

    @Override
    public void onParticipantLeft(Participant participant) {
      showViewerCount();
    }
  };

  public void onResume() {
    super.onResume();
    meeting.addEventListener(meetingEventListener);
    if (hlsStarted && (Build.VERSION.SDK_INT <= 23 || player == null)) {
      initializePlayer();
      if (playerView != null) {
        playerView.onResume();
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Build.VERSION.SDK_INT <= 23) {
      if (playerView != null) {
        playerView.onPause();
      }
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Build.VERSION.SDK_INT > 23) {
      if (playerView != null) {
        playerView.onPause();
      }
      releasePlayer();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    updateTrackSelectorParameters();
    updateStartPosition();
    outState.putBundle(
      KEY_TRACK_SELECTION_PARAMETERS,
      trackSelectionParameters.toBundle()
    );
    outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
    outState.putInt(KEY_ITEM_INDEX, startItemIndex);
    outState.putLong(KEY_POSITION, startPosition);
  }

  @Override
  public void onClick(View view) {
    if (view == btnQuality && !isShowingTrackSelectionDialog) {
      isShowingTrackSelectionDialog = true;
      TrackSelectionDialog trackSelectionDialog = TrackSelectionDialog.createForPlayer(
        player,
        /* onDismissListener= */dismissedDialog ->
          isShowingTrackSelectionDialog = false
      );
      trackSelectionDialog.show(getFragmentManager(), /* tag= */null);
    }
  }

  @Override
  public void onVisibilityChanged(int visibility) {
    controllerLayout.setVisibility(visibility);
    material_toolbar.setVisibility(visibility);
    if (
      reactionsLayout.getVisibility() == View.VISIBLE
    ) reactionsLayout.setVisibility(visibility);
  }

  protected boolean initializePlayer() {
    if (player == null) {
      lastSeenTracks = Tracks.EMPTY;
      dataSourceFactory = new DefaultHttpDataSource.Factory();
      HlsMediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
        .createMediaSource(MediaItem.fromUri(Uri.parse(this.playbackHlsUrl)));

      ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(
        /* context= */mContext
      );
      player = playerBuilder.build();
      player.setTrackSelectionParameters(trackSelectionParameters);
      playerEventListener = new PlayerEventListener();
      player.addListener(playerEventListener);
      player.addAnalyticsListener(new EventLogger());
      player.setAudioAttributes(
        AudioAttributes.DEFAULT,
        /* handleAudioFocus= */true
      );
      player.setPlayWhenReady(startAutoPlay);
      player.setMediaSource(mediaSource);
      playerView.setPlayer(player);
    }
    boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;
    if (haveStartPosition) {
      player.seekTo(startItemIndex, startPosition);
    }
    player.prepare();
    updateButtonVisibility();

    return true;
  }

  protected void releasePlayer() {
    if (player != null) {
      updateTrackSelectorParameters();
      updateStartPosition();
      player.removeListener(playerEventListener);
      player.release();
      player = null;
      dataSourceFactory = null;
      playerView.setPlayer(/* player= */null);
    }
  }

  private void updateTrackSelectorParameters() {
    if (player != null) {
      trackSelectionParameters = player.getTrackSelectionParameters();
    }
  }

  private void updateStartPosition() {
    if (player != null) {
      startAutoPlay = player.getPlayWhenReady();
      startItemIndex = player.getCurrentMediaItemIndex();
      startPosition = Math.max(0, player.getContentPosition());
    }
  }

  protected void clearStartPosition() {
    startAutoPlay = true;
    startItemIndex = C.INDEX_UNSET;
    startPosition = C.TIME_UNSET;
  }

  private void updateButtonVisibility() {
    btnQuality.setEnabled(player != null);
  }

  private void showControls() {
    controllerLayout.setVisibility(View.VISIBLE);
    material_toolbar.setVisibility(View.VISIBLE);
  }

  private class PlayerEventListener implements Player.Listener {

    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
      if (playbackState == Player.STATE_ENDED) {
        exoPause.setVisibility(View.GONE);
        exoPlay.setVisibility(View.VISIBLE);
        ended = true;
        showControls();
      } else {
        ended = false;
      }
      updateButtonVisibility();
    }

    @Override
    public void onPlayerError(PlaybackException error) {
      if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
        player.seekToDefaultPosition();
        player.prepare();
      } else {
        updateButtonVisibility();
        showControls();
      }
    }

    @Override
    @SuppressWarnings("ReferenceEquality")
    public void onTracksChanged(Tracks tracks) {
      updateButtonVisibility();
      if (tracks == lastSeenTracks) {
        return;
      }
      if (tracks.isTypeSelected(C.TRACK_TYPE_VIDEO)) {
        for (Tracks.Group trackGroup : tracks.getGroups()) {
          // Group level information.
          if (trackGroup.getMediaTrackGroup().type == 2) {
            for (int i = 0; i < trackGroup.length; i++) {
              // Individual track information.
              boolean isSelected = trackGroup.isTrackSelected(i);
              Format trackFormat = trackGroup.getTrackFormat(i);
              if (isSelected) btnQuality.setText(
                String.valueOf(trackFormat.width).concat("p")
              );
            }
          }
        }
      }
      lastSeenTracks = tracks;
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
      if (isPlaying) {
        exoPause.setVisibility(View.VISIBLE);
        exoPlay.setVisibility(View.GONE);
      } else {
        exoPause.setVisibility(View.GONE);
        exoPlay.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override
  public void onDestroy() {
    mContext = null;
    mActivity = null;
    playbackHlsUrl = null;
    releasePlayer();
    clearStartPosition();
    if (meeting != null) {
      meeting.pubSub.unsubscribe("emoji", emojiListener);
      meeting.pubSub.unsubscribe("coHost", coHostListener);
      meeting.removeAllListeners();
      meeting = null;
    }
    super.onDestroy();
  }

  public void showProducts() {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mActivity);
    View v3 = LayoutInflater
      .from(mActivity)
      .inflate(R.layout.products_layout, null);
    bottomSheetDialog.setContentView(v3);
    RecyclerView recyclerView = v3.findViewById(R.id.productRcv);
    ImageView close = v3.findViewById(R.id.ic_product_close);
    bottomSheetDialog.show();
    close.setOnClickListener(view -> bottomSheetDialog.dismiss());

    JSONArray jsonArray = new JSONArray();
    JSONObject topDetails = new JSONObject();
    JsonUtils.jsonPut(topDetails, "productImage", R.drawable.blackcroptop);
    JsonUtils.jsonPut(topDetails, "productName", "Black Top");
    JsonUtils.jsonPut(topDetails, "productPrice", "23");

    JSONObject jeansDetails = new JSONObject();
    JsonUtils.jsonPut(jeansDetails, "productImage", R.drawable.jeans);
    JsonUtils.jsonPut(jeansDetails, "productName", "Blue Jeans");
    JsonUtils.jsonPut(jeansDetails, "productPrice", "11");

    jsonArray.put(topDetails);
    jsonArray.put(jeansDetails);
    recyclerView.setAdapter(new ProductsAdapter(jsonArray));
  }

  public void showEmoji(Drawable drawable) {
    // You can change the number of emojis that will be flying on screen
    for (int i = 0; i < 5; i++) {
      flyObject(
        drawable,
        3000,
        DirectionGenerator.Direction.BOTTOM,
        DirectionGenerator.Direction.TOP,
        1
      );
    }
  }

  public void flyObject(
    final Drawable drawable,
    final int duration,
    final DirectionGenerator.Direction from,
    final DirectionGenerator.Direction to,
    final float scale
  ) {
    ZeroGravityAnimation animation = new ZeroGravityAnimation();
    animation.setCount(1);
    animation.setScalingFactor(scale);
    animation.setOriginationDirection(from);
    animation.setDestinationDirection(to);
    animation.setImage(drawable);
    animation.setDuration(duration);
    animation.setAnimationListener(
      new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
      }
    );

    viewerEmojiHolder.bringToFront();
    animation.play(mActivity, viewerEmojiHolder);
  }

  private void showCoHostRequestDialog(String name) {
    AlertDialog alertDialog = new MaterialAlertDialogBuilder(
      mContext,
      R.style.AlertDialogCustom
    )
      .create();
    alertDialog.setCancelable(false);
    LayoutInflater inflater = getLayoutInflater();
    View dialogView = inflater.inflate(R.layout.cohost_request_dialog, null);
    alertDialog.setView(dialogView);

    TextView message = dialogView.findViewById(R.id.txtMessage1);
    TextView message2 = dialogView.findViewById(R.id.txtMessage2);
    message.setText(name.concat(" has requested you to"));
    message2.setText("join as speaker");

    Button acceptBtn = dialogView.findViewById(R.id.acceptBtn);
    acceptBtn.setOnClickListener(v -> {
      meeting.changeMode("CONFERENCE");
      alertDialog.dismiss();
      PubSubPublishOptions pubSubPublishOptions = new PubSubPublishOptions();
      pubSubPublishOptions.setPersist(false);
      meeting.pubSub.publish(
        "coHostRequestAnswer",
        "accept",
        pubSubPublishOptions
      );
      getActivity()
        .getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.mainLayout, new SpeakerFragment(), "MainFragment")
        .commit();
    });

    Button declineBtn = dialogView.findViewById(R.id.declineBtn);
    declineBtn.setOnClickListener(v -> {
      alertDialog.dismiss();
      PubSubPublishOptions pubSubPublishOptions = new PubSubPublishOptions();
      pubSubPublishOptions.setPersist(false);
      meeting.pubSub.publish(
        "coHostRequestAnswer",
        "decline",
        pubSubPublishOptions
      );
    });

    alertDialog.show();
  }

  private void showViewerCount() {
    int viewerCount = 1;
    Map<String, Participant> participants = meeting.getParticipants();
    for (Map.Entry<String, Participant> entry : participants.entrySet()) {
      Participant participant = entry.getValue();
      if (participant.getMode().equals("VIEWER")) {
        viewerCount++;
      }
    }
    this.viewerCount.setText(String.valueOf(viewerCount));
  }
}
