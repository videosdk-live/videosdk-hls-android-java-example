
package live.videosdk.android.hlsdemo.viewerMode;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import live.videosdk.android.hlsdemo.R;

/**
 * Dialog to select tracks.
 */
public final class TrackSelectionDialog extends BottomSheetDialogFragment implements TrackSelectionView.TrackSelectionListener {

    @Override
    public void onTrackSelectionChanged(boolean isDisabled, Map<TrackGroup, TrackSelectionOverride> overrides) {
        this.isDisabled = isDisabled;
        this.overrides = overrides;
    }

    /**
     * Called when tracks are selected.
     */
    public interface TrackSelectionListener {

        /**
         * Called when tracks are selected.
         *
         * @param trackSelectionParameters A {@link TrackSelectionParameters} representing the selected
         *                                 tracks. Any manual selections are defined by {@link
         *                                 TrackSelectionParameters#disabledTrackTypes} and {@link
         *                                 TrackSelectionParameters#overrides}.
         */
        void onTracksSelected(TrackSelectionParameters trackSelectionParameters);
    }

    public static final ImmutableList<Integer> SUPPORTED_TRACK_TYPES =
            ImmutableList.of(C.TRACK_TYPE_VIDEO);

    private DialogInterface.OnClickListener onClickListener;
    private DialogInterface.OnDismissListener onDismissListener;
    private TrackSelectionView trackSelectionView;

    private List<Tracks.Group> trackGroups;
    private boolean allowAdaptiveSelections;
    private boolean allowMultipleOverrides;

    /* package */ boolean isDisabled;
    /* package */ Map<TrackGroup, TrackSelectionOverride> overrides;

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link Player}.
     */
    public static boolean willHaveContent(Player player) {
        return willHaveContent(player.getCurrentTracks());
    }

    /**
     * Returns whether a track selection dialog will have content to display if initialized with the
     * specified {@link Tracks}.
     */
    public static boolean willHaveContent(Tracks tracks) {
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (SUPPORTED_TRACK_TYPES.contains(trackGroup.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a dialog for a given {@link Player}, whose parameters will be automatically updated
     * when tracks are selected.
     *
     * @param player            The {@link Player}.
     * @param onDismissListener A {@link DialogInterface.OnDismissListener} to call when the dialog is
     *                          dismissed.
     */
    public static TrackSelectionDialog createForPlayer(
            Player player, DialogInterface.OnDismissListener onDismissListener) {
        return createForTracksAndParameters(
                player.getCurrentTracks(),
                player.getTrackSelectionParameters(),
                /* allowAdaptiveSelections= */ false,
                /* allowMultipleOverrides= */ false,
                player::setTrackSelectionParameters,
                onDismissListener);
    }

    /**
     * Creates a dialog for given {@link Tracks} and {@link TrackSelectionParameters}.
     *
     * @param tracks                   The {@link Tracks} describing the tracks to display.
     * @param trackSelectionParameters The initial {@link TrackSelectionParameters}.
     * @param allowAdaptiveSelections  Whether adaptive selections (consisting of more than one track)
     *                                 can be made.
     * @param allowMultipleOverrides   Whether tracks from multiple track groups can be selected.
     * @param trackSelectionListener   Called when tracks are selected.
     * @param onDismissListener        {@link DialogInterface.OnDismissListener} called when the dialog is
     */
    public static TrackSelectionDialog createForTracksAndParameters(
            Tracks tracks,
            TrackSelectionParameters trackSelectionParameters,
            boolean allowAdaptiveSelections,
            boolean allowMultipleOverrides,
            TrackSelectionListener trackSelectionListener,
            DialogInterface.OnDismissListener onDismissListener) {
        TrackSelectionDialog trackSelectionDialog = new TrackSelectionDialog();
        trackSelectionDialog.init(
                tracks,
                trackSelectionParameters,
                allowAdaptiveSelections,
                allowMultipleOverrides,
                /* onClickListener= */ (dialog, which) -> {
                    TrackSelectionParameters.Builder builder = trackSelectionParameters.buildUpon();
                    for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
                        int trackType = SUPPORTED_TRACK_TYPES.get(i);
                        builder.setTrackTypeDisabled(trackType, trackSelectionDialog.isDisabled);
                        builder.clearOverridesOfType(trackType);
                        Map<TrackGroup, TrackSelectionOverride> overrides = trackSelectionDialog.overrides == null ? Collections.emptyMap() : trackSelectionDialog.overrides;
                        for (TrackSelectionOverride override : overrides.values()) {
                            builder.addOverride(override);
                        }
                    }
                    trackSelectionListener.onTracksSelected(builder.build());
                },
                onDismissListener);
        return trackSelectionDialog;
    }

    public TrackSelectionDialog() {
        // Retain instance across activity re-creation to prevent losing access to init data.
        setRetainInstance(true);
    }

    private void init(
            Tracks tracks,
            TrackSelectionParameters trackSelectionParameters,
            boolean allowAdaptiveSelections,
            boolean allowMultipleOverrides,
            DialogInterface.OnClickListener onClickListener,
            DialogInterface.OnDismissListener onDismissListener) {
        this.onClickListener = onClickListener;
        this.onDismissListener = onDismissListener;

        for (int i = 0; i < SUPPORTED_TRACK_TYPES.size(); i++) {
            @C.TrackType int trackType = SUPPORTED_TRACK_TYPES.get(i);
            ArrayList<Tracks.Group> trackGroups = new ArrayList<>();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == trackType) {
                    trackGroups.add(trackGroup);
                }
            }
            if (!trackGroups.isEmpty()) {
                this.allowAdaptiveSelections = allowAdaptiveSelections;
                this.allowMultipleOverrides = allowMultipleOverrides;
                this.trackGroups = trackGroups;
                this.isDisabled = trackSelectionParameters.disabledTrackTypes.contains(trackType);
                this.overrides = trackSelectionParameters.overrides;
            }
        }
    }


    /**
     * if you want use dialog instead of BottomSheetDialog then change TrackSelectionDialog extends DialogFragment.
     * And use this method.
     **/

/*  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    // We need to own the view to let tab layout work correctly on all API levels. We can't use
    // AlertDialog because it owns the view itself, so we use AppCompatDialog instead, themed using
    // the AlertDialog theme overlay with force-enabled title.

    AppCompatDialog dialog =
        new AppCompatDialog(getActivity(), R.style.TrackSelectionDialog);
    dialog.setTitle("title");
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }*/


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDismissListener.onDismiss(dialog);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false);
        ImageView cancelButton = dialogView.findViewById(R.id.cancelBtn);
         Button applyBtn = dialogView.findViewById(R.id.applyBtn);
        trackSelectionView = dialogView.findViewById(R.id.exo_track_selection_init_view);
        trackSelectionView.setShowDisableOption(true);

        trackSelectionView.setAllowMultipleOverrides(allowMultipleOverrides);
        trackSelectionView.setAllowAdaptiveSelections(allowAdaptiveSelections);
        trackSelectionView.init(
                trackGroups,
                isDisabled,
                overrides,
                /* trackFormatComparator= */ null,
                /* listener= */ this);
        cancelButton.setOnClickListener(view -> dismiss());
        applyBtn.setOnClickListener(
                view -> {
                    onClickListener.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    dismiss();
                });
        return dialogView;
    }
}
