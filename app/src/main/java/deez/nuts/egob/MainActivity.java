package deez.nuts.egob;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import deez.nuts.egob.ui.AdminFragment;
import deez.nuts.egob.ui.AssistantFragment;
import deez.nuts.egob.ui.DocumentsFragment;
import deez.nuts.egob.ui.HomeFragment;
import deez.nuts.egob.ui.LoginFragment;
import deez.nuts.egob.ui.NotificationsFragment;
import deez.nuts.egob.ui.ProfileFragment;
import deez.nuts.egob.ui.RegisterFragment;
import deez.nuts.egob.ui.RequestsFragment;
import deez.nuts.egob.ui.ServiceDetailFragment;
import deez.nuts.egob.ui.ServicesFragment;
import deez.nuts.egob.ui.TabFragment;
import deez.nuts.egob.util.SessionManager;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private BottomNavigationView bottomNavigationView;
    private boolean syncingNavigation;
    private int selectedTab = R.id.nav_home;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (syncingNavigation) {
                return true;
            }
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchTab(new HomeFragment());
            } else if (id == R.id.nav_services) {
                switchTab(new ServicesFragment());
            } else if (id == R.id.nav_documents) {
                switchTab(new DocumentsFragment());
            } else if (id == R.id.nav_requests) {
                switchTab(new RequestsFragment());
            } else if (id == R.id.nav_profile) {
                switchTab(new ProfileFragment());
            }
            return true;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncNavigation);

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home);
            syncNavigation();
        } else if (SessionManager.currentUserId(this) > 0) {
            openHome();
        } else {
            openLogin();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
    }

    private void syncNavigation() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        boolean tab = current instanceof TabFragment;
        bottomNavigationView.setVisibility(tab ? View.VISIBLE : View.GONE);
        if (tab) {
            int tabId = ((TabFragment) current).getTabId();
            selectedTab = tabId;
            syncingNavigation = true;
            bottomNavigationView.setSelectedItemId(tabId);
            syncingNavigation = false;
        }
    }

    private void replaceFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentManager manager = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction transaction =
                manager.beginTransaction().replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commitNow();
        syncNavigation();
    }

    public void switchTab(@NonNull Fragment fragment) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        selectedTab = fragment instanceof TabFragment
                ? ((TabFragment) fragment).getTabId()
                : selectedTab;
        replaceFragment(fragment, false);
    }

    public void openLogin() {
        selectedTab = R.id.nav_home;
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        replaceFragment(new LoginFragment(), false);
    }

    public void openRegister() {
        replaceFragment(new RegisterFragment(), true);
    }

    public void openHome() {
        switchTab(new HomeFragment());
    }

    public void openServices() {
        switchTab(new ServicesFragment());
    }

    public void openDocuments() {
        switchTab(new DocumentsFragment());
    }

    public void openRequests() {
        switchTab(new RequestsFragment());
    }

    public void openProfile() {
        switchTab(new ProfileFragment());
    }

    public void openNotifications() {
        replaceFragment(new NotificationsFragment(), true);
    }

    public void openAssistant() {
        replaceFragment(new AssistantFragment(), true);
    }

    public void openAdmin() {
        replaceFragment(new AdminFragment(), true);
    }

    public void openServiceDetail(@NonNull String title, @NonNull String description) {
        openServiceDetail(0, title, description);
    }

    public void openServiceDetail(int serviceId, @NonNull String title, @NonNull String description) {
        ServiceDetailFragment fragment = new ServiceDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ServiceDetailFragment.ARG_SERVICE_ID, serviceId);
        args.putString(ServiceDetailFragment.ARG_TITLE, title);
        args.putString(ServiceDetailFragment.ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        replaceFragment(fragment, true);
    }
}
