package live.videosdk.android.hlsdemo.speakerMode.adapter;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import live.videosdk.android.hlsdemo.R;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;
import live.videosdk.rtc.android.Stream;
import live.videosdk.rtc.android.lib.PubSubMessage;
import live.videosdk.rtc.android.listeners.MeetingEventListener;
import live.videosdk.rtc.android.listeners.ParticipantEventListener;
import live.videosdk.rtc.android.listeners.PubSubMessageListener;
import live.videosdk.rtc.android.model.PubSubPublishOptions;

public class ParticipantListAdapter extends RecyclerView.Adapter<ParticipantListAdapter.ViewHolder> {

    private ArrayList<Participant> participants = new ArrayList<>();
    private Context context;
    private Meeting meeting;
    private PubSubMessageListener coHostAnswerListener;

    public ParticipantListAdapter(ArrayList<Participant> items, Meeting meeting, Context context) {
        this.context = context;
        this.meeting = meeting;
        participants.add(this.meeting.getLocalParticipant());
        participants.addAll(items);
        meeting.addEventListener(new MeetingEventListener() {
            @Override
            public void onParticipantJoined(Participant participant) {
                super.onParticipantJoined(participant);
                participants = new ArrayList<>();
                participants.add(meeting.getLocalParticipant());
                participants.addAll(getAllParticipants());
                notifyDataSetChanged();
            }

            @Override
            public void onParticipantLeft(Participant participant) {
                super.onParticipantLeft(participant);
                participants = new ArrayList<>();
                participants.add(meeting.getLocalParticipant());
                participants.addAll(getAllParticipants());
                notifyDataSetChanged();
            }

            @Override
            public void onParticipantModeChanged(JSONObject data) {
                participants = new ArrayList<>();
                participants.add(meeting.getLocalParticipant());
                participants.addAll(getAllParticipants());
                notifyDataSetChanged();
            }
        });

    }


    private ArrayList<Participant> getAllParticipants() {
        ArrayList<Participant> participantList = new ArrayList();
        Iterator<Participant> participants = meeting.getParticipants().values().iterator();

        for (int i = 0; i < meeting.getParticipants().size(); i++) {
            final Participant participant = participants.next();
            if (participant.getMode().equals("CONFERENCE")) {
                participantList.add(participant);
            }
        }

        participants = meeting.getParticipants().values().iterator();

        for (int i = 0; i < meeting.getParticipants().size(); i++) {
            final Participant participant = participants.next();
            if (participant.getMode().equals("VIEWER")) {
                participantList.add(participant);
            }
        }
        return participantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_list_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Participant participant = participants.get(position);
        if (participants.get(position).isLocal()) {
            holder.participantName.setText("You");
        } else {
            String participantName = participants.get(position).getDisplayName();
            if (participantName.length() > 10) {
                participantName = participantName.substring(0, 10).concat("..");
            }
            holder.participantName.setText(participantName);
        }

        holder.participantNameFirstLetter.setText(participants.get(position).getDisplayName().subSequence(0, 1));

        if (participant.getMode().equals("VIEWER")) {
            holder.micStatus.setVisibility(View.GONE);
            holder.camStatus.setVisibility(View.GONE);
            holder.hostIndicator.setVisibility(View.GONE);
        } else {
            holder.micStatus.setVisibility(View.VISIBLE);
            holder.camStatus.setVisibility(View.VISIBLE);
            holder.hostIndicator.setVisibility(View.VISIBLE);
        }

        for (Map.Entry<String, Stream> entry : participant.getStreams().entrySet()) {
            Stream stream = entry.getValue();
            if (stream.getKind().equalsIgnoreCase("video")) {
                holder.camStatus.setImageResource(R.drawable.ic_video_camera);
                holder.camStatus.getLayoutParams().width = 60;
                holder.camStatus.requestLayout();
                break;
            }
            if (stream.getKind().equalsIgnoreCase("audio")) {
                holder.micStatus.setImageResource(R.drawable.ic_mic_on);
                holder.micStatus.getLayoutParams().width = 60;
                holder.micStatus.requestLayout();
            }
        }

        participant.addEventListener(new ParticipantEventListener() {
            @Override
            public void onStreamEnabled(Stream stream) {
                if (stream.getKind().equalsIgnoreCase("video")) {
                    holder.camStatus.setImageResource(R.drawable.ic_video_camera);
                    holder.camStatus.getLayoutParams().width = 60;
                    holder.camStatus.requestLayout();
                }
                if (stream.getKind().equalsIgnoreCase("audio")) {
                    holder.micStatus.setImageResource(R.drawable.ic_mic_on);
                    holder.micStatus.getLayoutParams().width = 60;
                    holder.micStatus.requestLayout();
                }
            }

            @Override
            public void onStreamDisabled(Stream stream) {
                if (stream.getKind().equalsIgnoreCase("video")) {
                    holder.camStatus.setImageResource(R.drawable.ic_webcam_off_style);
                    holder.camStatus.getLayoutParams().width = 110;
                    holder.camStatus.requestLayout();
                }
                if (stream.getKind().equalsIgnoreCase("audio")) {
                    holder.micStatus.setImageResource(R.drawable.ic_mic_off_style);
                    holder.micStatus.getLayoutParams().width = 110;
                    holder.micStatus.requestLayout();
                }
            }
        });

        if (participant.isLocal()) {
            holder.btnParticipantMoreOptions.setVisibility(View.GONE);
        }

        //
        final Participant finalParticipant = participant;
        holder.btnParticipantMoreOptions.setOnClickListener(v -> showPopup(holder, finalParticipant));

        coHostAnswerListener = new PubSubMessageListener() {
            @Override
            public void onMessageReceived(PubSubMessage pubSubMessage) {
                if (pubSubMessage.getSenderId().equals(participant.getId())) {
                    if (pubSubMessage.getMessage().equals("decline")) {
                        Toast.makeText(context, pubSubMessage.getSenderName() + " has declined your request.", Toast.LENGTH_LONG).show();
                    }
                    if (pubSubMessage.getMessage().equals("accept")) {
                        Toast.makeText(context, pubSubMessage.getSenderName() + " has accept your request.", Toast.LENGTH_LONG).show();
                    }
                    holder.requestedIndicator.setVisibility(View.GONE);
                    holder.btnParticipantMoreOptions.setEnabled(true);
                }
            }
        };

        // notify user when viewer answer the request
        meeting.pubSub.subscribe("coHostRequestAnswer", coHostAnswerListener);
    }

    private void showPopup(ViewHolder holder, Participant participant) {
        PopupMenu popup = new PopupMenu(context, holder.btnParticipantMoreOptions);

        if (participant.getMode().equals("VIEWER")) {
            popup.getMenu().add("Add as a co-host");
        } else {
            holder.micStatus.setVisibility(View.VISIBLE);
            holder.camStatus.setVisibility(View.VISIBLE);
            holder.hostIndicator.setVisibility(View.VISIBLE);
            popup.getMenu().add("Remove from co-host");
        }

        popup.getMenu().add("Remove Participant");

        popup.setOnMenuItemClickListener(item -> {
            if (item.toString().equals("Remove Participant")) {
                participant.remove();
                return true;
            } else if (item.toString().equals("Add as a co-host")) {
                PubSubPublishOptions pubSubPublishOptions = new PubSubPublishOptions();
                pubSubPublishOptions.setPersist(false);
                meeting.pubSub.publish("coHost", participant.getId(), pubSubPublishOptions);
                holder.requestedIndicator.setVisibility(View.VISIBLE);
                holder.btnParticipantMoreOptions.setEnabled(false);
                return true;
            } else if (item.toString().equals("Remove from co-host")) {
                PubSubPublishOptions pubSubPublishOptions = new PubSubPublishOptions();
                pubSubPublishOptions.setPersist(false);
                meeting.pubSub.publish("removeCoHost", participant.getId(), pubSubPublishOptions);
                return true;
            }

            return false;
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popup.setGravity(Gravity.END);
        }

        popup.show();

    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView participantName;
        ImageView micStatus, camStatus;
        TextView participantNameFirstLetter;
        ImageButton btnParticipantMoreOptions;
        TextView hostIndicator;
        TextView requestedIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            participantName = itemView.findViewById(R.id.participant_Name);
            micStatus = itemView.findViewById(R.id.mic_status);
            camStatus = itemView.findViewById(R.id.cam_status);
            btnParticipantMoreOptions = itemView.findViewById(R.id.btnParticipantMoreOptions);
            participantNameFirstLetter = itemView.findViewById(R.id.participantNameFirstLetter);
            hostIndicator = itemView.findViewById(R.id.hostIndicator);
            requestedIndicator = itemView.findViewById(R.id.requestedIndicator);
        }
    }

}


