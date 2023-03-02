package live.videosdk.android.hlsdemo.common.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import live.videosdk.android.hlsdemo.common.activity.CreateOrJoinActivity;
import live.videosdk.android.hlsdemo.common.activity.MainActivity;
import live.videosdk.android.hlsdemo.common.NetworkUtils;
import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.common.listener.ResponseListener;

public class JoinMeetingFragment extends Fragment {

    public JoinMeetingFragment() {
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_join_meeting, container, false);

        EditText etName = view.findViewById(R.id.etName);
        EditText etMeetingId = view.findViewById(R.id.etMeetingId);
        Button btnJoin = view.findViewById(R.id.btnJoin);
        final boolean[] webcamEnabled = {false};
        final boolean[] micEnabled = {false};


        Bundle bundle = getArguments();
        if (bundle != null && bundle.getString("mode").equals("VIEWER")) {
            btnJoin.setText("Join as a viewer");
            ((CreateOrJoinActivity) getActivity()).setVisibilityOfPreview(View.GONE);
        } else {
            btnJoin.setText("Join as a speaker");
            ((CreateOrJoinActivity) getActivity()).setVisibilityOfPreview(View.VISIBLE);
        }

        btnJoin.setOnClickListener(v -> {
            if ("".equals(etMeetingId.getText().toString().trim())) {
                Toast.makeText(getContext(), "Please enter meeting ID",
                        Toast.LENGTH_SHORT).show();
            } else if (!etMeetingId.getText().toString().trim().matches("\\w{4}\\-\\w{4}\\-\\w{4}")) {
                Toast.makeText(getContext(), "Please enter valid meeting ID",
                        Toast.LENGTH_SHORT).show();
            } else if ("".equals(etName.getText().toString())) {
                Toast.makeText(getContext(), "Please Enter Name", Toast.LENGTH_SHORT).show();
            } else {
                NetworkUtils networkUtils = new NetworkUtils(getContext());
                if (networkUtils.isNetworkAvailable()) {
                    networkUtils.getToken(new ResponseListener<String>() {
                        @Override
                        public void onResponse(String token) {
                            networkUtils.joinMeeting(token, etMeetingId.getText().toString().trim(), new ResponseListener<String>() {
                                @Override
                                public void onResponse(String meetingId) {
                                    Intent intent = new Intent((CreateOrJoinActivity) getActivity(), MainActivity.class);
                                    String mode = bundle != null ? bundle.getString("mode") : "CONFERENCE";
                                    if (mode.equals("CONFERENCE")) {
                                        webcamEnabled[0] = ((CreateOrJoinActivity) getActivity()).isWebcamEnabled();
                                        micEnabled[0] = ((CreateOrJoinActivity) getActivity()).isMicEnabled();
                                    }
                                    intent.putExtra("token", token);
                                    intent.putExtra("meetingId", meetingId);
                                    intent.putExtra("webcamEnabled", webcamEnabled[0]);
                                    intent.putExtra("micEnabled", micEnabled[0]);
                                    intent.putExtra("participantName", etName.getText().toString().trim());
                                    intent.putExtra("mode", mode);
                                    startActivity(intent);
                                    ((CreateOrJoinActivity) getActivity()).finish();
                                }
                            });
                        }
                    });

                } else {
                    Toast.makeText(getContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }

        });
        return view;
    }
}