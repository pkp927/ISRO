package com.iirs.omap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.util.ByteArrayBuffer;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;



public class Main extends Activity implements LocationListener, OnClickListener{

	private MapView mapView;
	private MapController mapController;
	LocationManager locMan;
	ScaleBarOverlay scaleBarOverlay;
	ArrayList<OverlayItem> overlayItemArray;
	MyLocationOverlay compass;
	TextToSpeech tts;
	Overlay myOverlay;
	Routing getRoute;
	Double latitude,longitude;
	EditText et;
	Button but;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		StrictMode.ThreadPolicy Tpolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(Tpolicy);
		
		et = (EditText) findViewById(R.id.etPlace);
		but = (Button) findViewById(R.id.go);
		et.setVisibility(View.INVISIBLE);
		but.setVisibility(View.INVISIBLE);
		but.setOnClickListener(this);
		
		// define map view and map controller
		mapView = (MapView) findViewById(R.id.mapView);
		mapController = (MapController) mapView.getController();
		
		// define compass and scale bar overlay
		compass = new MyLocationOverlay(Main.this,mapView);
		scaleBarOverlay = new ScaleBarOverlay(Main.this);
		
		// set the map view
		mapView.setTileSource(TileSourceFactory.MAPNIK);
		//mapView.setUseDataConnection(false);
		mapView.setBuiltInZoomControls(true);
		mapView.setMultiTouchControls(true);
		mapView.getOverlays().add(scaleBarOverlay);
		mapView.getOverlays().add(compass);
        
		// set text to speech converter
		tts = new TextToSpeech(this,new TextToSpeech.OnInitListener(){

			public void onInit(int status) {
				if(status != TextToSpeech.ERROR){
					tts.setLanguage(Locale.US);
				}
			}
			
		});
		
        // define location manager
        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);
        locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 100, this);
        
        // check if GPS is on
        boolean enabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
        	  Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        	  startActivity(intent);
        }
        
        // instance of overlay class to add overlays
        myOverlay = new Overlay(getApplicationContext(), mapView, tts);
        
        // instance of routing class to manage routing
        getRoute = new Routing(getApplicationContext(), mapView, tts);
        
        // get location and create and add overlay item to the overlay item array
        Location lastLocation = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        GeoPoint overlayPoint;
        if(lastLocation != null){
        	overlayPoint = new GeoPoint(lastLocation.getLatitude()*1E6, lastLocation.getLongitude()*1E6);
        	latitude = lastLocation.getLatitude();
        	longitude = lastLocation.getLongitude();
        	Log.d(String.valueOf(latitude),String.valueOf(longitude));
        }else{
        	overlayPoint = new GeoPoint(30332987,78050640);
        	latitude = 30.332987;
        	longitude = 78.050640;
        	Toast.makeText(this, "make sure your gps is on", Toast.LENGTH_LONG).show();
        }
		overlayItemArray = new ArrayList<OverlayItem>();
        overlayItemArray.add(new OverlayItem("New Overlay", "My Location", overlayPoint));
        
        // center the map and add overlay to OSM
		mapController.setZoom(15);
        mapController.setCenter(overlayPoint);
        mapController.animateTo(overlayPoint);
		myOverlay.addOverlay(latitude,longitude,overlayItemArray);

        
	}
	
	public void onLocationChanged(Location location) {
		latitude = (Double) (location.getLatitude());
        longitude = (Double) (location.getLongitude());
        GeoPoint geopoint = new GeoPoint(latitude, longitude);
        
        // center the map to the current location
        mapController.animateTo(geopoint);
        
        // clear and add overlay item to the overlay item array
		overlayItemArray.clear();
		OverlayItem newMyLocationItem = new OverlayItem("My Location", "My Location", geopoint);
		overlayItemArray.add(newMyLocationItem);
		
		// add overlay to OSM
		myOverlay.addOverlay(latitude,longitude,overlayItemArray);
	}

	public void onProviderDisabled(String arg0) {
	}

	public void onProviderEnabled(String arg0) {
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater blowUp = getMenuInflater();
		blowUp.inflate(R.menu.action_bar, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		//return super.onOptionsItemSelected(item);
		switch(item.getItemId()){
		case R.id.go_to:
			et.setVisibility(View.VISIBLE);
			but.setVisibility(View.VISIBLE);
			break;
		case R.id.cinema:
			getRoute.drawPOI(latitude, longitude,"cinema");
			break;
		case R.id.atm:
			getRoute.drawPOI(latitude, longitude,"ATM");
			break;
		case R.id.bus_stand:
			getRoute.drawPOI(latitude, longitude,"Bus Station");
			break;
		case R.id.mall:
			getRoute.drawPOI(latitude, longitude,"Mall");
			break;
		case R.id.bank:
			getRoute.drawPOI(latitude, longitude,"Bank");
			break;
		case R.id.hospital:
			getRoute.drawPOI(latitude, longitude,"Hospital");
			break;
		case R.id.airport:
			getRoute.drawPOI(latitude, longitude,"Airport");
			break;
		case R.id.station:
			getRoute.drawPOI(latitude, longitude,"Station");
			break;
		case R.id.post:
			getRoute.drawPOI(latitude, longitude,"Post office");
			break;
		case R.id.school:
			getRoute.drawPOI(latitude, longitude,"School");
			break;
		case R.id.hotel:
			getRoute.drawPOI(latitude, longitude,"Hotel");
			break;
		case R.id.c_all:
			getRoute.clear();
			break;
		case R.id.c_poi:
			getRoute.clear_poi();
			break;
		case R.id.c_route:
			getRoute.clear_route();
			break;
		case R.id.download:
			if(isNetworkAvailable()) DownloadFromUrl(null, "mapdata.xml");
			else Toast.makeText(this, "internet is required", Toast.LENGTH_LONG).show();
			break;
		}
		return false;
	}
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
    public void DownloadFromUrl(String imageURL, String fileName) {
        try {
        	
        		String north = String.valueOf(longitude-0.100);
        		String west = String.valueOf(latitude-0.050);
        		String south = String.valueOf(longitude+0.100);
        		String east = String.valueOf(latitude+0.050);
        		/*
        		if(latitude >= 0){
        			west = String.valueOf(latitude-0.050);
        			east = String.valueOf(latitude+0.050);
        		}else{
        			west = String.valueOf(latitude+0.050);
        			east = String.valueOf(latitude-0.050);
        		}
        		
        		if(longitude >= 0){
        			north = String.valueOf(longitude-0.100);
        			south = String.valueOf(longitude+0.100);
        		}else{
        			north = String.valueOf(longitude+0.100);
        			south = String.valueOf(longitude-0.100);
        		}
        		*/
                URL url = new URL("http://api.openstreetmap.org/api/0.6/map?bbox="+
                					north+","+west+","+south+","+east); 
                
                File root = android.os.Environment.getExternalStorageDirectory();               

                File dir = new File (root.getAbsolutePath() + "/osmdroid");
                if(dir.exists()==false) {
                     dir.mkdirs();
                }

                File file = new File(dir, fileName);
                
                long startTime = System.currentTimeMillis();
                Log.d("ImageManager", "download begining");
                Log.d("ImageManager", "download url:" + url);
                Log.d("ImageManager", "downloaded file name:" + fileName);
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();

                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = null;
                try{
                 is = ucon.getInputStream();
                }catch(FileNotFoundException e){
                	Toast.makeText(this, "file not available", Toast.LENGTH_LONG).show();
                	return;
                }

                BufferedInputStream bis = new BufferedInputStream(is);

                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                }

                /* Convert the Bytes read to a String. */
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(baf.toByteArray());
                fos.close();
                Log.d("ImageManager", "download ready in"
                                + ((System.currentTimeMillis() - startTime) / 1000)
                                + " sec");
                Toast.makeText(this, "file download complete", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
                Log.d("ImageManager", "Error: " + e);
        }

    }

	public void onClick(View v) {
		if(v.getId() == R.id.go){
			getRoute.drawRoadRoute(latitude, longitude,et.getText().toString());
			et.setVisibility(View.INVISIBLE);
			but.setVisibility(View.INVISIBLE);
		}
	}
	
	protected void onResume() {
		compass.enableCompass();
		super.onResume();
		locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		//locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	protected void onPause() {
		compass.disableCompass();
		if(tts != null){
			tts.stop();
			tts.shutdown();
		}
		super.onPause();
		locMan.removeUpdates(this);
	}

	
}
