package radim.locationsms;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private LinearLayout backdropLL;
    private TextView statusTV,lastStoredTV,refreshTV;
    private String locationStatus;
    private Location current;
    private float lon,lat;
    private LocationManager lm;
    private boolean enabledGPS,enabledNET;
    private Handler handler;
    private int roller = 0;
    private static final int LIMIT_MAKES_RED = 1200;
    private String dateS;
    private static final String TAG = "Location SMS: ";
    private SharedPreferences myPreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    private void initRepetitiveTask(){
        handler = new Handler();
        startRepeatingTask();
    }
    Runnable statusChecker = new Runnable() {
        @Override
        public void run() {
            long interval = 600;
            try {
                updateStatus();//this function can change value interval based on accuracy
                roller++;
                if(roller == 4) roller = 0;
            } finally {
                handler.postDelayed(statusChecker, interval);
            }
        }
    };

    void startRepeatingTask() {
        statusChecker.run();
    }

    void stopRepeatingTask() {
        handler.removeCallbacks(statusChecker);
    }

    @Override
    protected void onStart(){
        super.onStart();
        initMembers();
        initLocationService();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        initMembers();
        initLocationService();
        initRepetitiveTask();
    }

    @Override
    protected void onPause(){
        super.onPause();
        try {
            lm.removeUpdates(this);
        }catch(SecurityException se){

            String title = getString(R.string.warning_title);
            String message = getString(R.string.warning_message);

            handleProblem(title, message);
            Log.i(TAG,"CATCH SE onPause");

        }catch(Exception e){

            String title_gen = getString(R.string.warning_title_general);
            String message_gen = getString(R.string.warning_message_general)+"removeUpdates";
            handleProblem(title_gen, message_gen);
            Log.i(TAG,"CATCH GE onPause ");

        }

        stopRepeatingTask();

        //

        lm=null;
        current=null;
        myPreferences=null;
        backdropLL=null;
        statusTV=null;
        refreshTV=null;
        lastStoredTV=null;
        handler=null;

    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    private void initMembers(){
        current = null;
        backdropLL = (LinearLayout) findViewById(R.id.parent_layout);
        statusTV = (TextView) findViewById(R.id.status_TV);
        refreshTV = (TextView) findViewById(R.id.refreshTV);
        lastStoredTV = (TextView) findViewById(R.id.last_storedTV);
        myPreferences = getSharedPreferences("myPreferences", Context.MODE_PRIVATE);
        setStoredTV();
    }

    private void setStoredTV(){
        if(!myPreferences.contains("date"))lastStoredTV.setText(getResources().getString(R.string.nothingStored));
        else {
            lat = myPreferences.getFloat("lat", 0);
            lon = myPreferences.getFloat("lon", 0);
            dateS = myPreferences.getString("date","");
            String toShow = new StringBuilder()
                    .append(getResources().getString(R.string.stored)).append(" ")
                    .append(dateS).append(", Lat: ").append(String.valueOf(lat))
                    .append(", Lon: ").append(String.valueOf(lon)).append(".").toString();
            lastStoredTV.setText(toShow);
        }
    }

    private void initProviders(){
        enabledGPS =  lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        enabledNET = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void initLocationService(){
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        initProviders();
            try {
                if(enabledGPS)lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 128, 1, this);
                if(enabledNET)lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 255, 1, this);
            }catch(SecurityException se){

                String title = getString(R.string.warning_title);
                String message = getString(R.string.warning_message);

                handleProblem(title, message);
                Log.i(TAG,"CATCH SE init");

            }
            catch(Exception e){

                String title_gen = getString(R.string.warning_title_general);
                String message_gen = getString(R.string.warning_message_general)+"initLocationService";

                handleProblem(title_gen, message_gen);
                Log.i(TAG,"CATCH GE init");
            }
    }

    private void toastThis(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        current = location;
        float accur = current.getAccuracy();
        current.setAccuracy(Math.round(accur));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        initMembers();
        initLocationService();
        System.out.println("ON STATUS CHANGED provider " + provider + " STATUS " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        initMembers();
        initLocationService();
        toastThis(getResources().getString(R.string.provEnab)+" "+provider);
        System.out.println("PROVIDER ENABLED " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        initMembers();
        initLocationService();
        toastThis(getResources().getString(R.string.provDis)+" "+provider);
        System.out.println("PROVIDER DISABLED " + provider);
    }

    private void updateStatus(){
        int backGroundColor;
        float accuracy;
        String provider;
        if(current != null) {
            accuracy = current.getAccuracy();
            provider = current.getProvider();
            concatenateStatus();
            lat = (float)current.getLatitude();
            lon = (float)current.getLongitude();
            backGroundColor = findColor((int) Math.round(accuracy), LIMIT_MAKES_RED);
            backdropLL.setBackgroundColor(backGroundColor);
            locationStatus += new StringBuilder().append(", Lat: ").append(lat).append(" Lon: ")
                    .append(lon).append(", ").append(getResources().getString(R.string.prov))
                    .append(" ").append(provider).append(", ").append(getResources()
                            .getString(R.string.accur)).append(": ").append(accuracy).append(" [m].").toString();
            refreshTV.setText(formProgress());
            statusTV.setText(locationStatus);
        }else {
            concatenateStatus();
            locationStatus+=", "+getResources().getString(R.string.noLoc);
            backGroundColor = Color.LTGRAY;
            backdropLL.setBackgroundColor(backGroundColor);
            refreshTV.setText(formProgress());
            statusTV.setText(locationStatus);
        }
    }

    private String formProgress(){
        if(roller == 0) return "";
        if(roller == 1) return "*";
        if(roller == 2) return "**";
        if(roller == 3) return "***";
        return"*";
    }

    private void concatenateStatus(){
        locationStatus = getResources().getString(R.string.status);
        locationStatus+=" ";
        if(enabledGPS) locationStatus += getResources().getString(R.string.GPSEnabled);
        else locationStatus += getResources().getString(R.string.GPSDisabled);
        locationStatus +=", ";
        if(enabledNET) locationStatus += getResources().getString(R.string.NETEnabled);
        else locationStatus += getResources().getString(R.string.NETDisabled);
    }

    public void doSettings(View w){
        Intent doSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(doSettings);
    }
    /*
     */
    public void sendLastStored(View v){
        if(!myPreferences.contains("date")){
            toastThis(getResources().getString(R.string.nothingStored));
        }else{
            sendSMS(composeLink(String.valueOf(myPreferences.getFloat("lat",0)),String.valueOf(myPreferences.getFloat("lon",0))));
        }
    }
    /*
    */
    public void sendCurrent(View v){
        if(current == null){
            toastThis(getResources().getString(R.string.noCurrLoc));
        }else {
            sendSMS(composeLink(String.valueOf(current.getLatitude()),String.valueOf(current.getLongitude())));
        }
    }
    /*
    */
    private void sendSMS(String message) {
        try {
            Uri destination = Uri.parse("smsto:");
            Intent sendSMS = new Intent(Intent.ACTION_SENDTO, destination);
            sendSMS.putExtra("sms_body", message);
            if (sendSMS.resolveActivity(getPackageManager()) != null) {
                startActivity(sendSMS);
            } else toastThis(getResources().getString(R.string.noSMS));
        }catch(Exception e){
            String title_gen = getString(R.string.warning_title_general);
            String message_gen = getString(R.string.warning_message_general)+"sendSMS";
            handleProblem(title_gen, message_gen);
        }
    }
    /*
    */
    private String composeLink(String lat,String lon){
        return "https://maps.google.com/?q="+lat+","+lon;
    }
    /*
     */
    public void storeCurrent(View v){
        SharedPreferences.Editor myEditor;
        if(current == null) {
            toastThis(getResources().getString(R.string.nothingToStore));
        }else{
            Calendar calendar = Calendar.getInstance();
            dateS = calendar.getTime().toString();
            myEditor = myPreferences.edit();
            myEditor.putString("date",dateS);
            myEditor.putFloat("lat", lat);
            myEditor.putFloat("lon", lon);
            myEditor.commit();
            setStoredTV();
        }
    }

    private int findColor(int accuracy, int limit){
        int myAccuracy = accuracy;
        if (myAccuracy == 0) myAccuracy = 1;
        if (myAccuracy>limit) myAccuracy = limit-1;
        int half = limit/2;
        double coef = (double)255/half;
        int red; int green;
        if(myAccuracy>=half) red = 255; else{
            red = (int)(myAccuracy*coef);
        }
        if(myAccuracy<=half) green = 255;else{
            green = (int)(255-((myAccuracy-half)*coef));
        }
        return Color.rgb(red,green,0);
    }

    /**
     *
     *
     *
     */
    private void handleProblem(String title, String message){

        String ok = getString(R.string.ok);

        Bundle handedIn = new Bundle();

        handedIn.putString("title",title);
        handedIn.putString("message", message);
        handedIn.putString("ok", ok);

        FragmentManager manager = getFragmentManager();

        Fragment frag = manager.findFragmentByTag("fragment_warning");
        if (frag != null) {
          manager.beginTransaction().remove(frag).commit();
        }

        FragmentAlertDialog fad = new FragmentAlertDialog();
        fad.setArguments(handedIn);
        fad.setCancelable(false);
        fad.show(manager, "fragment_warning");

        manager = null;

    }
}
