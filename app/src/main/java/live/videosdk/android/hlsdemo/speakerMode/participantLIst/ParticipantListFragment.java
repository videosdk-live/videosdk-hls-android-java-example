package live.videosdk.android.hlsdemo.speakerMode.participantLIst;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Iterator;

import live.videosdk.android.hlsdemo.common.meeting.activity.MainActivity;
import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.speakerMode.participantLIst.ParticipantListAdapter;
import live.videosdk.rtc.android.Meeting;
import live.videosdk.rtc.android.Participant;

public class ParticipantListFragment extends Fragment {

    private Context mContext;
    private Activity mActivity;
    private Meeting meeting;
    private RecyclerView participantsListView;

    public ParticipantListFragment() {

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
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_participant, container, false);
        participantsListView = view.findViewById(R.id.rvParticipantsLinearView);

        meeting = ((MainActivity) getActivity()).getMeeting();
        ArrayList<Participant> participants = getAllParticipants();
        participantsListView.setLayoutManager(new LinearLayoutManager(mContext));
        participantsListView.setAdapter(new ParticipantListAdapter(participants, meeting, mActivity));
        DividerItemDecoration dividerItemDecoration=new DividerItemDecoration(participantsListView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divider));
        participantsListView.addItemDecoration(dividerItemDecoration);
        return view;
    }


    private ArrayList<Participant> getAllParticipants() {
        ArrayList<Participant> participantList = new ArrayList();

        if (meeting != null) {
            final Iterator<Participant> participants = meeting.getParticipants().values().iterator();

            for (int i = 0; i < meeting.getParticipants().size(); i++) {
                final Participant participant = participants.next();
                if (participant.getMode().equals("CONFERENCE")) {
                    participantList.add(participant);
                }
            }

            final Iterator<Participant> participantIterator = meeting.getParticipants().values().iterator();

            for (int i = 0; i < meeting.getParticipants().size(); i++) {
                final Participant participant = participantIterator.next();
                if (participant.getMode().equals("VIEWER")) {
                    participantList.add(participant);
                }
            }
        }
        return participantList;
    }
}