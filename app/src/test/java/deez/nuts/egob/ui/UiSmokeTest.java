package deez.nuts.egob.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import deez.nuts.egob.MainActivity;
import deez.nuts.egob.R;
import deez.nuts.egob.data.AppRepository;
import deez.nuts.egob.util.SessionManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UiSmokeTest {

    @Test
    public void registerScreenOpensWithoutCrash() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        View register = activity.findViewById(R.id.btn_register);
        assertNotNull("Login register button", register);
        register.performClick();
        shadowOf(Looper.getMainLooper()).idle();

        View submit = activity.findViewById(R.id.btn_register);
        assertNotNull("Register submit button", submit);

        ((TextInputEditText) activity.findViewById(R.id.input_nin))
                .setText("NIN-7777-6666-55");
        ((TextInputEditText) activity.findViewById(R.id.input_full_name))
                .setText("Prueba Ciudadana");
        ((TextInputEditText) activity.findViewById(R.id.input_phone))
                .setText("+1 555 123 4567");
        ((TextInputEditText) activity.findViewById(R.id.input_password))
                .setText("claveSegura1");
        ((TextInputEditText) activity.findViewById(R.id.input_password_confirm))
                .setText("claveSegura1");
        ((MaterialCheckBox) activity.findViewById(R.id.check_consent)).setChecked(true);

        submit.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void everyNavigationEntryPointOpensWithoutCrash() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 1, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        int[] tabs = {R.id.nav_home, R.id.nav_services, R.id.nav_documents,
                R.id.nav_requests, R.id.nav_profile};
        for (int tab : tabs) {
            activity.findViewById(tab).performClick();
            shadowOf(Looper.getMainLooper()).idle();
        }

        activity.openNotifications();
        shadowOf(Looper.getMainLooper()).idle();
        activity.openAssistant();
        shadowOf(Looper.getMainLooper()).idle();
        activity.openAdmin();
        shadowOf(Looper.getMainLooper()).idle();
        activity.openServiceDetail(1, "Pasaporte", "Renovación");
        shadowOf(Looper.getMainLooper()).idle();
        activity.openProfile();
        shadowOf(Looper.getMainLooper()).idle();
        activity.openRegister();
        shadowOf(Looper.getMainLooper()).idle();
        activity.openLogin();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void homeQrDialogOpensWithoutCrash() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 1, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        View qr = activity.findViewById(R.id.btn_show_qr);
        assertNotNull("Home QR button", qr);
        qr.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void servicesScreenOpensWithoutCrash() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 1, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        activity.openServices();
        shadowOf(Looper.getMainLooper()).idle();

        RecyclerView list = activity.findViewById(R.id.list_services);
        assertNotNull("Services list", list);
        ServicesAdapter adapter = (ServicesAdapter) list.getAdapter();
        assertNotNull("Services adapter", adapter);
        assertTrue("Services should be seeded", adapter.getItemCount() > 0);

        ServicesAdapter.ServiceViewHolder holder = adapter.createViewHolder(list,
                adapter.getItemViewType(0));
        adapter.bindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.btn_bookmark).performClick();
        holder.itemView.performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void adminHomeEntryPointOpensOperationsPanel() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 1, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        View staffCard = activity.findViewById(R.id.card_staff_access);
        assertNotNull("Staff access card", staffCard);
        assertEquals(View.VISIBLE, staffCard.getVisibility());

        staffCard.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull("Operations panel", activity.findViewById(R.id.list_admin_requests));
    }

    @Test
    public void workerHomeEntryPointShowsOfficerRoleAndOpensOperationsPanel() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 5, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        View staffCard = activity.findViewById(R.id.card_staff_access);
        assertNotNull("Staff access card", staffCard);
        assertEquals(View.VISIBLE, staffCard.getVisibility());
        assertEquals("OFICIAL",
                ((android.widget.TextView) activity.findViewById(R.id.text_staff_badge))
                        .getText().toString());

        staffCard.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull("Operations panel", activity.findViewById(R.id.list_admin_requests));
    }

    @Test
    public void workerProfileEntryPointOpensOperationsPanel() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 5, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        activity.openProfile();
        shadowOf(Looper.getMainLooper()).idle();

        View adminRow = activity.findViewById(R.id.row_admin);
        assertNotNull("Profile operations row", adminRow);
        assertEquals(View.VISIBLE, adminRow.getVisibility());

        adminRow.performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull("Operations panel", activity.findViewById(R.id.list_admin_requests));
    }

    @Test
    public void citizenDoesNotSeeStaffEntryPoint() {
        Context context = RuntimeEnvironment.getApplication();
        SessionManager.clear(context);
        AppRepository.get(context).seedBlocking();
        SessionManager.start(context, 2, true);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        shadowOf(Looper.getMainLooper()).idle();

        View staffCard = activity.findViewById(R.id.card_staff_access);
        assertNotNull("Staff access card", staffCard);
        assertEquals(View.GONE, staffCard.getVisibility());
    }
}
