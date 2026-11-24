package deez.nuts.egob.ui;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;

public class AssistantFragment extends Fragment {

    private LinearLayout chatContainer;
    private NestedScrollView chatScroll;
    private EditText input;
    private View disclaimer;

    public AssistantFragment() {
        super(R.layout.fragment_assistant);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        chatContainer = view.findViewById(R.id.chat_container);
        chatScroll = view.findViewById(R.id.chat_scroll);
        input = view.findViewById(R.id.input_message);
        disclaimer = view.findViewById(R.id.chat_disclaimer);

        int[] promptIds = {R.id.prompt_passport, R.id.prompt_tax, R.id.prompt_dmv, R.id.prompt_address};
        for (int id : promptIds) {
            Chip chip = view.findViewById(id);
            chip.setOnClickListener(v -> {
                input.setText(chip.getText());
                input.setSelection(input.length());
                Snackbar.make(view, R.string.assistant_quick_fill, Snackbar.LENGTH_SHORT).show();
            });
        }

        view.findViewById(R.id.btn_start_application).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openServiceDetail(
                        getString(R.string.assistant_parking),
                        getString(R.string.assistant_parking_zone)));

        view.findViewById(R.id.btn_attach).setOnClickListener(v ->
                Snackbar.make(view, R.string.detail_file_picker, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_mic).setOnClickListener(v ->
                Snackbar.make(view, R.string.assistant_mic, Snackbar.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_send).setOnClickListener(v -> sendMessage());

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        addBubble(R.layout.view_chat_user, text);
        input.setText("");
        chatScroll.postDelayed(() ->
                addBubble(R.layout.view_chat_bot, getString(R.string.assistant_typing_reply)), 800);
    }

    private void addBubble(int layoutRes, @NonNull String text) {
        View bubble = getLayoutInflater().inflate(layoutRes, chatContainer, false);
        TextView content = bubble.findViewById(R.id.chat_text);
        content.setText(text);

        int index = disclaimer == null ? chatContainer.getChildCount()
                : chatContainer.indexOfChild(disclaimer);
        chatContainer.addView(bubble, index);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}
