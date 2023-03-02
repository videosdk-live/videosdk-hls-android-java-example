package live.videosdk.android.hlsdemo.speakerMode.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import live.videosdk.android.hlsdemo.speakerMode.fragment.ParticipantListFragment;
import live.videosdk.android.hlsdemo.speakerMode.fragment.StageFragment;

public class TabAdapter extends FragmentStateAdapter {


    int totalTabs;

    public TabAdapter(FragmentManager fm, Lifecycle lifecycle, int totalTabs) {
        super(fm, lifecycle);
        this.totalTabs = totalTabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StageFragment();
            case 1:
                return new ParticipantListFragment();
            default:
                return new StageFragment();
        }
    }

    @Override
    public int getItemCount() {
        return this.totalTabs;
    }


}
