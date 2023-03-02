package live.videosdk.android.hlsdemo.speakerMode.manageTabs;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import live.videosdk.android.hlsdemo.R;
import live.videosdk.android.hlsdemo.common.meeting.activity.MainActivity;

public class SpeakerFragment extends Fragment {


    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TabAdapter adapter;

    public SpeakerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_speaker, container, false);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        tabLayout.addTab(tabLayout.newTab().setText("Stage"));
        tabLayout.addTab(tabLayout.newTab().setText("Participants"));
        TextView tabOne = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.custom_tab_layout, null);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabOne.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (event.getRawX() >= tabOne.getRight() - tabOne.getCompoundDrawables()[2].getBounds().width()) {
                    showMeetingInfo();
                    return true;
                }
            }
            return false;

        });
        adapter = new TabAdapter(getChildFragmentManager(), getLifecycle(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tabLayout.selectTab(tab);
                if (position == 0) {
                    Drawable drawable = getResources().getDrawable(R.drawable.ic_info);
                    int size = dpToPx(23, getContext());
                    if (drawable != null) {
                        drawable.setBounds(0, 0, size, size);
                    }
                    tabOne.setCompoundDrawables(null, null, drawable, null);
                    tab.setCustomView(tabOne);
                    tabOne.setText("Stage");
                } else
                    tab.setText("Participants");
            }
        });
        mediator.attach();
        return view;
    }

    private static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void showMeetingInfo() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(getContext(), R.style.AlertDialogCustom).create();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_style, null);
        alertDialog.setView(dialogView);

        TextView message = dialogView.findViewById(R.id.message);
        TextView message1 = dialogView.findViewById(R.id.message1);
        message1.setVisibility(View.VISIBLE);
        message.setText("Meeting Code");
        message.setTextSize(15);
        message.setTextColor(getResources().getColor(R.color.md_grey_10A));
        message1.setText("               " + ((MainActivity) getActivity()).getMeeting().getMeetingId());

        message1.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (event.getRawX() >= message1.getRight() - message1.getCompoundDrawables()[2].getBounds().width()) {
                    copyTextToClipboard(((MainActivity) getActivity()).getMeeting().getMeetingId());
                    return true;
                }
            }
            return false;

        });
        LinearLayout btnLayout = dialogView.findViewById(R.id.btnLayout);
        btnLayout.setVisibility(View.GONE);
        alertDialog.show();
    }

    private void copyTextToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copied text", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getActivity(), "Copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

}