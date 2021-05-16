package ro.pub.cs.systems.eim.lab09.ngnsip.view;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.doubango.ngn.NgnEngine;
import org.doubango.ngn.events.NgnInviteEventArgs;
import org.doubango.ngn.events.NgnRegistrationEventArgs;
import org.doubango.ngn.media.NgnMediaType;
import org.doubango.ngn.services.INgnConfigurationService;
import org.doubango.ngn.services.INgnSipService;
import org.doubango.ngn.sip.NgnAVSession;
import org.doubango.ngn.utils.NgnConfigurationEntry;
import org.doubango.ngn.utils.NgnUriUtils;

import ro.pub.cs.systems.eim.lab09.ngnsip.R;
import ro.pub.cs.systems.eim.lab09.ngnsip.broadcastreceiver.RegistrationBroadcastReceiver;
import ro.pub.cs.systems.eim.lab09.ngnsip.broadcastreceiver.VoiceCallBroadcastReceiver;
import ro.pub.cs.systems.eim.lab09.ngnsip.general.Constants;

public class VoiceCallActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private NgnEngine ngnEngine = null;
    private INgnSipService ngnSipService = null;

    private NgnAVSession ngnAVSession = null;

    private IntentFilter registrationIntentFilter;
    private RegistrationBroadcastReceiver registrationBroadcastReceiver;

    private IntentFilter voiceCallIntentFilter;
    private VoiceCallBroadcastReceiver voiceCallBroadcastReceiver;

    private Button registerButton = null;
    private Button unregisterButton = null;
    private TextView registrationStatusTextView = null;

    private EditText SIPAddressEditText = null;
    private Button makeCallButton = null;
    private Button hangUpCallButton = null;
    private TextView callStatusTextView = null;

    private Button dtmfButton = null;
    private EditText dtmfEditText = null;

    private Button chatButton = null;

    private static VoiceCallActivity instance;

    public static VoiceCallActivity getInstance() {
        return instance;
    }

    private final RegistrationButtonClickListener registrationButtonClickListener = new RegistrationButtonClickListener();
    private class RegistrationButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            } else {
                configureStack();

                if (!startNgnEngine())
                    return;

                registerSipService();
            }
        }

    }

    private final UnregisterButtonClickListener unregisterButtonClickListener = new UnregisterButtonClickListener();
    private class UnregisterButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            unregisterSipService();
        }

    }

    private final MakeCallButtonClickListener makeCallButtonClickListener = new MakeCallButtonClickListener();
    private class MakeCallButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            String validUri = NgnUriUtils.makeValidSipUri(SIPAddressEditText.getText().toString());
            if (validUri == null) {
                Log.e(Constants.TAG, "Invalid SIP Address");
                return;
            }
            if (!ngnEngine.isStarted() || !ngnSipService.isRegistered()) {
                Log.e(Constants.TAG, "NGN Engine is not started or NGN Sip Service is not registered");
                return;
            }

            ngnAVSession = NgnAVSession.createOutgoingSession(
                    NgnEngine.getInstance().getSipService().getSipStack(),
                    NgnMediaType.Audio
            );

            if (ngnAVSession.makeCall(validUri)) {
                callStatusTextView.setText(getResources().getString(R.string.calling));
                Log.d(Constants.TAG, "Call succeeded");
            } else {
                Log.d(Constants.TAG, "Call failed");
            }
        }
    }

    private final HangupCallButtonClickListener hangupCallButtonClickListener = new HangupCallButtonClickListener();
    private class HangupCallButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View viw) {
            if (ngnAVSession != null) {
                ngnAVSession.hangUpCall();
                Log.d(Constants.TAG, "Hang Up");
            }
        }

    }

    private final DTMFButtonClickListener dtmfButtonClickListener = new DTMFButtonClickListener();
    private class DTMFButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            if (ngnAVSession != null) {
                int character = dtmfEditText.getText().toString().charAt(0);

                switch (character) {
                    case '*':
                        character = 10;
                        break;
                    case '#':
                        character = 11;
                        break;
                    default:
                        if (character >= '0' && character < '9') {
                            character -= '0';
                        }
                }

                if (!ngnAVSession.sendDTMF(character)) {
                    Log.e(Constants.TAG, "Failed to send DTMF " + character);
                } else {
                    Log.d(Constants.TAG, "Succeeded to send DTMF " + character);
                }
            }
        }

    }

    private final ChatButtonClickListener chatButtonClickListener = new ChatButtonClickListener();
    private class ChatButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getApplicationContext(), InstantMessagingActivity.class);
            intent.putExtra(Constants.SIP_ADDRESS, SIPAddressEditText.getText().toString());
            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG, "onCreate() callback method was invoked");
        setContentView(R.layout.activity_voice_call);
        instance = this;

        ngnEngine = NgnEngine.getInstance();
        if (ngnEngine == null) {
            Log.e(Constants.TAG, "Failed to obtain the NGN engine");
        }
        ngnSipService = ngnEngine.getSipService();

        registerButton = findViewById(R.id.register_button);
        registerButton.setOnClickListener(registrationButtonClickListener);
        unregisterButton = findViewById(R.id.unregister_button);
        unregisterButton.setOnClickListener(unregisterButtonClickListener);
        registrationStatusTextView = findViewById(R.id.registration_status_text_view);

        SIPAddressEditText = findViewById(R.id.SIP_address_edit_text);
        makeCallButton = findViewById(R.id.make_call_button);
        makeCallButton.setOnClickListener(makeCallButtonClickListener);
        hangUpCallButton = findViewById(R.id.hang_up_call_button);
        hangUpCallButton.setOnClickListener(hangupCallButtonClickListener);
        callStatusTextView = findViewById(R.id.call_status_text_view);

        dtmfButton = findViewById(R.id.dtmf_button);
        dtmfButton.setOnClickListener(dtmfButtonClickListener);
        dtmfEditText = findViewById(R.id.dtmf_edit_text);

        chatButton = findViewById(R.id.chat_button);
        chatButton.setOnClickListener(chatButtonClickListener);

        enableRegistrationBroadcastReceiver();
        enableVoiceCallBroadcastReceiver();
    }

    public void configureStack() {
        NgnEngine ngnEngine = NgnEngine.getInstance();
        INgnConfigurationService ngnConfigurationService = ngnEngine.getConfigurationService();

        ngnConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPI, Constants.IDENTITY_IMPI);
        ngnConfigurationService.putString(NgnConfigurationEntry.IDENTITY_IMPU, String.format("sip:%s@%s", Constants.USERNAME, Constants.DOMAIN));
        ngnConfigurationService.putString(NgnConfigurationEntry.IDENTITY_PASSWORD, Constants.IDENTITY_PASSWORD);
        ngnConfigurationService.putString(NgnConfigurationEntry.NETWORK_PCSCF_HOST, Constants.NETWORK_PCSCF_HOST);
        ngnConfigurationService.putInt(NgnConfigurationEntry.NETWORK_PCSCF_PORT, Constants.NETWORK_PCSCF_PORT);
        ngnConfigurationService.putString(NgnConfigurationEntry.NETWORK_REALM, Constants.NETWORK_REALM);

        ngnConfigurationService.putBoolean(NgnConfigurationEntry.NETWORK_USE_3G, Constants.NETWORK_USE_3G);
        ngnConfigurationService.putInt(NgnConfigurationEntry.NETWORK_REGISTRATION_TIMEOUT, Constants.NETWORK_REGISTRATION_TIMEOUT);

        ngnConfigurationService.commit();
    }

    public boolean startNgnEngine() {
        if (!ngnEngine.isStarted()) {
            if (!ngnEngine.start()) {
                Log.e(Constants.TAG, "Failed to start the NGN engine");
                return false;
            }
        }
        return true;
    }

    public boolean stopNgnEngine() {
        if (ngnEngine.isStarted()) {
            if (!ngnEngine.stop()) {
                Log.e(Constants.TAG, "Failed to stop the NGN engine");
                return false;
            }
        }
        return true;
    }

    public void registerSipService() {
        if (!ngnSipService.isRegistered()) {
            ngnSipService.register(this);
        }
    }

    public void unregisterSipService() {
        if (ngnSipService.isRegistered()) {
            ngnSipService.unRegister();
        }
    }

    public void enableRegistrationBroadcastReceiver() {
        registrationBroadcastReceiver = new RegistrationBroadcastReceiver(registrationStatusTextView);
        registrationIntentFilter = new IntentFilter();
        registrationIntentFilter.addAction(NgnRegistrationEventArgs.ACTION_REGISTRATION_EVENT);
        registerReceiver(registrationBroadcastReceiver, registrationIntentFilter);
    }

    public void disableRegistrationStateBroadcastReceiver() {
        unregisterReceiver(registrationBroadcastReceiver);
    }

    public void enableVoiceCallBroadcastReceiver() {
        voiceCallBroadcastReceiver = new VoiceCallBroadcastReceiver(SIPAddressEditText, callStatusTextView);
        voiceCallIntentFilter = new IntentFilter();
        voiceCallIntentFilter.addAction(NgnInviteEventArgs.ACTION_INVITE_EVENT);
        registerReceiver(voiceCallBroadcastReceiver, voiceCallIntentFilter);
    }

    public void disableVoiceCallBroadcastReceiver() {
        unregisterReceiver(voiceCallBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(Constants.TAG, "onResume() callback method was invoked");
    }

    @Override
    protected void onPause() {
        Log.i(Constants.TAG, "onPause() callback method was invoked");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, "onDestroy() callback method was invoked");
        stopNgnEngine();
        disableRegistrationStateBroadcastReceiver();
        disableVoiceCallBroadcastReceiver();
        super.onDestroy();
    }

    public INgnSipService getNgnSipService() {
        return ngnSipService;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureStack();

                if (!startNgnEngine())
                    return;

                registerSipService();
            } else {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show();
            }
        }
    }
}
