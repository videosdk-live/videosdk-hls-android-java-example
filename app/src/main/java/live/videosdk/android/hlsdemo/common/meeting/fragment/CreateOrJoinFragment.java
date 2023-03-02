package live.videosdk.android.hlsdemo.common.meeting.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import live.videosdk.android.hlsdemo.common.meeting.activity.CreateOrJoinActivity;
import live.videosdk.android.hlsdemo.common.utils.NetworkUtils;
import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.common.utils.ResponseListener;

public class CreateOrJoinFragment extends Fragment {

    public CreateOrJoinFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_or_join, container, false);

        view.findViewById(R.id.btnCreateMeeting).setOnClickListener(v -> {
            NetworkUtils networkUtils = new NetworkUtils(getContext());
            if (networkUtils.isNetworkAvailable()) {
                networkUtils.getToken(new ResponseListener<String>() {
                    @Override
                    public void onResponse(String token) {
                        networkUtils.createMeeting(token, meetingId -> {
                            ((CreateOrJoinActivity) CreateOrJoinFragment.this.getActivity()).setActionBar();
                            ((CreateOrJoinActivity) CreateOrJoinFragment.this.getActivity()).setTitle("Create a meeting");
                            Bundle bundle = new Bundle();
                            bundle.putString("meetingId", meetingId);
                            bundle.putString("token", token);
                            CreateMeetingFragment createMeetingFragment = new CreateMeetingFragment();
                            createMeetingFragment.setArguments(bundle);

                            final FragmentTransaction ft = CreateOrJoinFragment.this.getFragmentManager().beginTransaction();
                            ft.replace(R.id.fragContainer, createMeetingFragment, "CreateMeeting");
                            ft.addToBackStack("CreateOrJoinFragment");
                            ft.commit();
                        });
                    }
                });
            } else {
                Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            }

        });

        view.findViewById(R.id.btnJoinSpeakerMeeting).setOnClickListener(v -> {
            ((CreateOrJoinActivity) getActivity()).setActionBar();
            ((CreateOrJoinActivity) getActivity()).setTitle("Join as a speaker");
            joinMeetingFragment("CONFERENCE");
        });

        view.findViewById(R.id.btnJoinViewerMeeting).setOnClickListener(v -> {
            ((CreateOrJoinActivity) getActivity()).setActionBar();
            ((CreateOrJoinActivity) getActivity()).setTitle("Join as a viewer");
            joinMeetingFragment("VIEWER");
        });


        // Inflate the layout for this fragment
        return view;
    }

    private void joinMeetingFragment(String mode) {
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        JoinMeetingFragment joinMeetingFragment = new JoinMeetingFragment();
        joinMeetingFragment.setArguments(bundle);

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragContainer, joinMeetingFragment, "joinMeetingFragment");
        ft.addToBackStack("CreateOrJoinFragment");
        ft.commit();
    }
}