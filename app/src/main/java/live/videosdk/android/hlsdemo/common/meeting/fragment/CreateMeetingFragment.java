package live.videosdk.android.hlsdemo.common.meeting.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import live.videosdk.android.hlsdemo.common.meeting.activity.CreateOrJoinActivity;
import live.videosdk.android.hlsdemo.common.meeting.activity.MainActivity;
import live.videosdk.android.hlsdemo.R;

public class CreateMeetingFragment extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_meeting, container, false);

        EditText etName = view.findViewById(R.id.etName);
        TextView txtMeetingId = view.findViewById(R.id.txtMeetingId);
        String meetingId = null;
        String token = null;

        Button btnCreate = view.findViewById(R.id.btnCreate);
        ((CreateOrJoinActivity) getActivity()).setVisibilityOfPreview(View.VISIBLE);
        Bundle bundle = getArguments();
        if (bundle != null) {
            meetingId = bundle.getString("meetingId");
            token = bundle.getString("token");
            txtMeetingId.setText("Meeting code: ".concat(meetingId));
        }

        String finalMeetingId = meetingId;
        txtMeetingId.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (event.getRawX() >= txtMeetingId.getRight() - txtMeetingId.getCompoundDrawables()[2].getBounds().width()) {
                    copyTextToClipboard(finalMeetingId);
                    return true;
                }
            }
            return false;

        });

        String finalToken = token;
        btnCreate.setOnClickListener(v -> {
            if ("".equals(etName.getText().toString())) {
                Toast.makeText(getContext(), "Please Enter Name", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent((CreateOrJoinActivity) getActivity(), MainActivity.class);
                intent.putExtra("token", finalToken);
                intent.putExtra("meetingId", finalMeetingId);
                intent.putExtra("webcamEnabled", ((CreateOrJoinActivity) getActivity()).isWebcamEnabled());
                intent.putExtra("micEnabled", ((CreateOrJoinActivity) getActivity()).isMicEnabled());
                intent.putExtra("participantName", etName.getText().toString().trim());
                intent.putExtra("mode", "CONFERENCE");
                startActivity(intent);
                ((CreateOrJoinActivity) getActivity()).finish();
            }


        });
        return view;
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied text", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
}