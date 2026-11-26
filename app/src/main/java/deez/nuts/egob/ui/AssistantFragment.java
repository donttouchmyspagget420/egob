package deez.nuts.egob.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Locale;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.data.ChatExchange;
import deez.nuts.egob.data.RepositoryCallback;
import deez.nuts.egob.database.entities.Message;
import deez.nuts.egob.database.entities.Service;
import deez.nuts.egob.util.FileStorage;
import deez.nuts.egob.util.SessionManager;

public class AssistantFragment extends Fragment {

    private LinearLayout chatContainer;
    private NestedScrollView chatScroll;
    private EditText input;
    private View disclaimer;
    private View sendButton;
    private AppRepository repository;
    private int userId = -1;
    private boolean sending;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::attachFile);
    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null) {
                    List<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty() && input != null) {
                        input.setText(matches.get(0));
                        input.setSelection(input.length());
                    }
                }
            });

    public AssistantFragment() {
        super(R.layout.fragment_assistant);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = AppRepository.get(requireContext());
        userId = SessionManager.currentUserId(requireContext());

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        chatContainer = view.findViewById(R.id.chat_container);
        chatScroll = view.findViewById(R.id.chat_scroll);
        input = view.findViewById(R.id.input_message);
        disclaimer = view.findViewById(R.id.chat_disclaimer);
        sendButton = view.findViewById(R.id.btn_send);

        int[] promptIds = {R.id.prompt_passport, R.id.prompt_tax, R.id.prompt_dmv, R.id.prompt_address};
        for (int id : promptIds) {
            Chip chip = view.findViewById(id);
            chip.setOnClickListener(v -> sendMessage(chip.getText().toString()));
        }

        view.findViewById(R.id.btn_attach).setOnClickListener(v ->
                filePicker.launch(new String[]{"application/pdf", "image/*"}));
        view.findViewById(R.id.btn_mic).setOnClickListener(v -> startSpeech());
        sendButton.setOnClickListener(v -> {
            String text = input.getText() == null ? "" : input.getText().toString().trim();
            sendMessage(text);
        });

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String text = input.getText() == null ? "" : input.getText().toString().trim();
                sendMessage(text);
                return true;
            }
            return false;
        });

        if (userId <= 0) {
            ((MainActivity) requireActivity()).openLogin();
            return;
        }
        loadMessages();
    }

    private void loadMessages() {
        repository.getMessages(userId, new RepositoryCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> value) {
                if (!isAdded() || chatContainer == null) {
                    return;
                }
                clearMessages();
                for (Message message : value) {
                    addBubble("assistant".equals(message.role)
                            ? R.layout.view_chat_bot : R.layout.view_chat_user,
                            message.message, 0);
                }
                scrollToBottom();
            }

            @Override
            public void onError(String message) {
                if (isAdded() && getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void clearMessages() {
        View prompts = chatContainer.findViewById(R.id.quick_prompts);
        if (prompts == null || disclaimer == null) {
            return;
        }
        View promptsScroll = (View) prompts.getParent();
        int start = chatContainer.indexOfChild(promptsScroll) + 1;
        int end = chatContainer.indexOfChild(disclaimer);
        while (end > start) {
            chatContainer.removeViewAt(start);
            end--;
        }
    }

    private void sendMessage(@NonNull String raw) {
        String text = raw.trim();
        if (text.isEmpty() || sending) {
            return;
        }
        sending = true;
        sendButton.setEnabled(false);
        input.setText("");
        View userBubble = addBubble(R.layout.view_chat_user, text, 0);
        repository.sendAssistantMessage(userId, text, new RepositoryCallback<ChatExchange>() {
            @Override
            public void onSuccess(ChatExchange value) {
                if (!isAdded()) {
                    return;
                }
                sending = false;
                sendButton.setEnabled(true);
                addBubble(R.layout.view_chat_bot, value.assistantMessage.message, value.serviceId);
                scrollToBottom();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) {
                    return;
                }
                sending = false;
                sendButton.setEnabled(true);
                if (userBubble != null) {
                    chatContainer.removeView(userBubble);
                }
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void attachFile(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        Snackbar.make(getView(), R.string.common_loading, Snackbar.LENGTH_SHORT).show();
        repository.copyFile(uri, new RepositoryCallback<FileStorage.StoredFile>() {
            @Override
            public void onSuccess(FileStorage.StoredFile value) {
                if (!isAdded()) {
                    return;
                }
                repository.sendAssistantAttachment(userId, value.name,
                        new RepositoryCallback<ChatExchange>() {
                            @Override
                            public void onSuccess(ChatExchange exchange) {
                                if (!isAdded()) {
                                    return;
                                }
                                addBubble(R.layout.view_chat_user, exchange.userMessage.message, 0);
                                addBubble(R.layout.view_chat_bot, exchange.assistantMessage.message, 0);
                                Snackbar.make(getView(), R.string.assistant_attachment_received,
                                        Snackbar.LENGTH_SHORT).show();
                                scrollToBottom();
                            }

                            @Override
                            public void onError(String message) {
                                if (isAdded()) {
                                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("es", "ES").toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.assistant_title));
        try {
            speechLauncher.launch(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Snackbar.make(getView(), R.string.assistant_voice_unavailable, Snackbar.LENGTH_LONG).show();
        }
    }

    @Nullable
    private View addBubble(int layoutRes, @Nullable String text, int serviceId) {
        if (chatContainer == null || text == null) {
            return null;
        }
        View bubble = getLayoutInflater().inflate(layoutRes, chatContainer, false);
        TextView content = bubble.findViewById(R.id.chat_text);
        content.setText(text);

        View action = bubble.findViewById(R.id.chat_action);
        if (action != null) {
            if (serviceId > 0) {
                action.setVisibility(View.VISIBLE);
                action.setOnClickListener(v -> openService(serviceId));
            } else {
                action.setVisibility(View.GONE);
            }
        }

        int index = disclaimer == null ? chatContainer.getChildCount()
                : chatContainer.indexOfChild(disclaimer);
        chatContainer.addView(bubble, index);
        scrollToBottom();
        return bubble;
    }

    private void openService(int serviceId) {
        repository.getService(serviceId, new RepositoryCallback<Service>() {
            @Override
            public void onSuccess(Service value) {
                if (isAdded()) {
                    ((MainActivity) requireActivity()).openServiceDetail(
                            value.id, value.name, value.description);
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void scrollToBottom() {
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}
