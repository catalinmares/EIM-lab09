package ro.pub.cs.systems.eim.lab09.ngnsip.general;

public interface Constants {

    String TAG = "[NgnSIP]";

    boolean DEBUG = true;

    String USERNAME = "catalin-constantin";
    String IDENTITY_IMPI = "eimlab9";
    String IDENTITY_PASSWORD = "dr6jfZ9c5ewXeuXm";
    String DOMAIN = "eimlab9.onsip.com";
    String NETWORK_PCSCF_HOST = "sip.onsip.com";
    int NETWORK_PCSCF_PORT = 5060;
    String NETWORK_REALM = "eimlab9.onsip.com";

    boolean NETWORK_USE_3G = true;
    int NETWORK_REGISTRATION_TIMEOUT = 3600;

    String SIP_ADDRESS = "ro.pub.cs.systems.eim.lab09.ngnsip.SipAddress";

    int ACCEPT_CALL_DELAY_TIME = 2000;
}
