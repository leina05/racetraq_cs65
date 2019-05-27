package edu.dartmouth.cs.racetraq.Fragments;

        import android.app.AlertDialog;
        import android.app.Dialog;
        import android.content.DialogInterface;
        import android.os.Bundle;
        import android.support.annotation.NonNull;
        import android.support.v4.app.DialogFragment;
        import android.text.InputType;
        import android.widget.EditText;
        import android.widget.LinearLayout;

        import java.util.Objects;

        import edu.dartmouth.cs.racetraq.NewDriveActivity;
        import edu.dartmouth.cs.racetraq.R;

public class SaveDriveDialogFragment extends DialogFragment {

    public static SaveDriveDialogFragment createInstance(int title_id)
    {
        SaveDriveDialogFragment fragment = new SaveDriveDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title_id", title_id);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getArguments() != null;
        int title_id = getArguments().getInt("title_id");

        /* Build Dialog */
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle(title_id);  // set title

        /* Set edit text view */
        final EditText dialogInput = new EditText(getActivity());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dialogInput.setLayoutParams(lp);
        if (title_id == R.string.drive_name)
        {
            dialogInput.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        }
        else
        {
            dialogInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        dialog.setView(dialogInput);

        /* Set OK button */
        dialog.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String data = dialogInput.getText().toString();
                        if (!data.isEmpty())
                        {
                            assert getArguments() != null;
                            ((NewDriveActivity) Objects.requireNonNull(getActivity())).saveDialogEntry(getArguments().getInt("title_id"), data);
                        }
                    }
                });

        dialog.setNegativeButton("Resume", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((NewDriveActivity) Objects.requireNonNull(getActivity())).resumeDrive();

            }
        });

        return dialog.create();
    }
}

