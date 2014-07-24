package com.iirs.omap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class Overlay {
	
	Context context;
	TextToSpeech tts;
	MapView mapView;
	ArrayList<OverlayItem> overlayItemArray;
	ItemizedIconOverlay<OverlayItem> myLocationOverlay;
	
	HttpClient client;
	JSONObject json;
	File file;
	XmlPullParserFactory factory;
	XmlPullParser xpp;
	
	String address;
	Boolean success1=false,success2=false;
	Boolean file_present = false;
	Boolean data_in_file = false;
	Double latitude,longitude;
	Double prev_lat = 0.0 ,prev_long = 0.0;
	ArrayList<Long> n_id = new ArrayList<Long>();
	BoundingBoxE6 bb;
	
	public Overlay(Context c, MapView mv, TextToSpeech t){
		context = c;
		tts = t;
		mapView = mv;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        xpp = factory.newPullParser();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		client = new DefaultHttpClient();
		address = "";
	}
	
	public void addOverlay(final double lat,final double lon, ArrayList<OverlayItem> al){
			Log.d("parneet","started overlay");
		
			// initialise variables
			overlayItemArray = al;
			latitude = lat;
			longitude = lon;
			
			// check for stored file on sdcard 
			if(!isNetworkAvailable()){
				Log.d("parneet","net not present");
				file = new File(Environment.getExternalStorageDirectory()+ "/osmdroid/mapdata.xml");
	            if(file.exists()) {
	            	file_present = true;
	            }else{
	            	file_present = false;
	            }
            }else{
				Log.d("parneet","net is present");
            	file_present = false;
            }
			
			// fetch address corresponding to the coordinates and add overlay
			new Read().execute();
			
			Log.d("parneet","finished overlay");
	}
	
	/*
	 * check for internet connectivity
	 */
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	/*
	 *  fetch address from nominatim.openstreetmap.org
	 */
	public JSONObject fetchAddress1(final double latitude,final double longitude) throws ClientProtocolException, IOException, JSONException{
		StringBuilder url = new StringBuilder("http://nominatim.openstreetmap.org/reverse?format=json&");
	    url.append("lat="+latitude+"&lon="+longitude+"&zoom=18&addressdetails=1");
	    Log.d("parneet",url.toString());
	    HttpGet get = new HttpGet(url.toString());
	    HttpResponse r = client.execute(get);
	    int status = r.getStatusLine().getStatusCode();
	    if(status == 200){
	    	HttpEntity e = r.getEntity();
	    	String data = EntityUtils.toString(e);
	    	JSONObject jobj = new JSONObject(data);
	    	return jobj;
	    }else{
	    	return null;
	    }
	}

	/*
	 * fetch address from maps.google.com
	 */
	public JSONObject fetchAddress2(final double latitude,final double longitude) throws ClientProtocolException, IOException, JSONException{
		StringBuilder url = new StringBuilder("http://maps.google.com/maps/api/geocode/json?");
	    url.append("latlng="+latitude+","+longitude+"&sensor=true");
	    Log.d("parneet",url.toString());
	    HttpGet get = new HttpGet(url.toString());
	    HttpResponse r = client.execute(get);
	    int status = r.getStatusLine().getStatusCode();
	    if(status == 200){
	    	HttpEntity e = r.getEntity();
	    	String data = EntityUtils.toString(e);
	    	JSONObject jobj = new JSONObject(data);
	    	return jobj;
	    }else{
	    	return null;
	    }
	}
	
	/*
	 * get address data from the file stored on sdcard
	 */
	private void parseData(double latitude, double longitude) throws Exception {
		address = "u are nearby ";
		
		// set xml pull parser
        FileInputStream fis = new FileInputStream(file);
        xpp.setInput(new InputStreamReader(fis));
        
        // set up variables for parser
        String lat_v = "";
        String lon_v = "";
        String data = "";
        ArrayList<String> ids = new ArrayList<String>();
        Boolean got_node = false;
        Boolean check_way = false;
        Boolean got_way = false;
        
        // parsing started
        int event = xpp.getEventType();
        while(event != XmlPullParser.END_DOCUMENT){
        	
        	switch(event){
        	
        	case XmlPullParser.START_TAG:
        		if(xpp.getName().equals("node")){
        			lat_v = xpp.getAttributeValue(null, "lat");
        			lon_v = xpp.getAttributeValue(null, "lon");
        			/*
        			if(((Math.round(Double.valueOf(lat_v)*100) == Math.round(latitude*100)) && (Math.round(Double.valueOf(lon_v)*1000) == Math.round(longitude*1000))) ||
        					((Math.round(Double.valueOf(lat_v)*1000) == Math.round(latitude*1000)) && (Math.round(Double.valueOf(lon_v)*100) == Math.round(longitude*100))) ){
        				Log.d("parneeet", "hurrah"+lat_v+" "+lon_v);
        				ids.add(xpp.getAttributeValue(null, "id"));
        				got_node = true;
        			}
        			*/
        			if(((Math.round(Double.valueOf(lat_v)*10000) <= Math.round(latitude*10000)+9) &&
        				(Math.round(Double.valueOf(lat_v)*10000) >= Math.round(latitude*10000)-9)) && 
        				((Math.round(Double.valueOf(lon_v)*10000) <= Math.round(longitude*10000+9) &&
        				(Math.round(Double.valueOf(lon_v)*10000) >= Math.round(longitude*10000)-9))) ){
        				Log.d("parneeet", "hurrah"+lat_v+" "+lon_v);
        				ids.add(xpp.getAttributeValue(null, "id"));
        				got_node = true;
        			}
        			
        		}else if((got_node || got_way) && xpp.getName().equals("tag")){
        			if(!data.contains(xpp.getAttributeValue(null, "k"))) data += xpp.getAttributeValue(null, "k");
        			if(!data.contains(xpp.getAttributeValue(null, "v"))) data += " - "+xpp.getAttributeValue(null, "v") + ", ";
        		}else if(xpp.getName().equals("way")){
        			check_way = true;
        		}else if(check_way && xpp.getName().equals("nd")){
        			if(ids.contains(xpp.getAttributeValue(null, "ref"))){
        				got_way = true;
        			}
        		}else if(xpp.getName().equals("bounds")){
            			if( (Double.valueOf(xpp.getAttributeValue(null, "minlat"))*10000 <= latitude*10000) &&
            				(Double.valueOf(xpp.getAttributeValue(null, "minlon"))*10000 <= longitude*10000) &&
            				(Double.valueOf(xpp.getAttributeValue(null, "maxlat"))*10000 >= latitude*10000) &&
            				(Double.valueOf(xpp.getAttributeValue(null, "maxlon"))*10000 >= longitude*10000) ){
            				data_in_file = true;
            				Log.d("parneet","data is in file");
            			}else{
            				data_in_file = false;
            				Log.d("parneet","data is not in file");
            				return;
            			}
            	}
        		break;
        		
        	case XmlPullParser.END_TAG:
        		if(got_node && xpp.getName().equals("node")){ 
        			got_node = false;
        			lat_v = "";
        			lon_v = "";
        		}else if(check_way && xpp.getName().equals("way")){
        			check_way = false;
        			if(got_way) got_way = false;
        		}
        		break;
        		
        	}
        	
        	event = xpp.next();
        	
        }
        
        // set address
        address += data;

        Log.d("parneet","got address from file: "+address);
    }
	
	/*
	 * get address for the given lat lon 
	 * either from internet or from stored file
	 */
	public class Read extends AsyncTask<Double, Integer, String>{

		protected String doInBackground(Double... arg) {

			Log.d("parneet","in background");
			// parse the xml file to get the address of the location
			try {
            	data_in_file = false;
				if(file_present){
					Log.d("parneet","file is present");
					Log.d("parneet","previous "+String.valueOf(prev_lat)+" "+String.valueOf(prev_long));
					Log.d("parneet","parse "+String.valueOf(Math.round(prev_lat*10000) - Math.round(latitude*10000))+" "+String.valueOf(Math.round(prev_long*10000) - Math.round(longitude*10000) ));
					if(Math.abs(Math.round(prev_lat*10000) - Math.round(latitude*10000)) >= 1 ||
					   Math.abs(Math.round(prev_long*10000) - Math.round(longitude*10000)) >= 1){
						Log.d("parse start","hey");
						parseData(latitude,longitude);
						Log.d("parse finish","hey");
					}else{
						data_in_file = true;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// if data in file is not present fetch the address from net
			try {
				success1 = success2 = false;
				if(!data_in_file){
					Log.d("parneet","address fetch from net");
					json = fetchAddress1(latitude, longitude);
					if(json != null){
						success1 = true;
					}else{
						json = fetchAddress2(latitude, longitude);
						success2 = true;
					}
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return null;
		}

		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Log.d("parneet","on post execute");
			
			// get address parsing json 
			if(!data_in_file && json != null){
				address = "u r nearby ";
				try {
					if(success1){
						address += json.getString("display_name");
						Log.d("parneet","success 1 address "+address);	
					}else if(success2){
						address += ((JSONArray)json.get("results")).getJSONObject(0).getString("formatted_address");
						Log.d("parneet","success 2 address "+address);
					}
					json = null;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			// remove previous overlays if present
	        if(myLocationOverlay != null) mapView.getOverlays().remove(myLocationOverlay);
			
			// instantiate Itemized icon overlay class 
	        DefaultResourceProxyImpl resourceProxy = new DefaultResourceProxyImpl(context);
	        
	        myLocationOverlay = new ItemizedIconOverlay<OverlayItem>(overlayItemArray,
	                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
	            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
	            	Toast.makeText( context, "Latitude:" +latitude+" Longitude: "+longitude+"\n"+address, Toast.LENGTH_LONG).show();
	                tts.speak(address, TextToSpeech.QUEUE_FLUSH,null);
	                return true; 
	            }
	            public boolean onItemLongPress(final int index, final OverlayItem item) {
	                Toast.makeText( context, "Latitude:" +latitude+" Longitude: "+longitude+"\n"+address,Toast.LENGTH_LONG).show();
	                tts.speak(address, TextToSpeech.QUEUE_FLUSH,null);
	                return false;
	            }
	        }, resourceProxy);
	        
	        // add overlay to OSM
	        mapView.getOverlays().add(myLocationOverlay);
	        
	        // update the map view to see the changes
	        mapView.invalidate();
	        
	        // set previous data for next parse
	   		prev_lat = latitude;
	     	prev_long = longitude;
	        
			Log.d("parneet","finished on post execute");
			
		}	
	}

}





/*
// check if road nodes coincide
int i=0;
while(Routing.nodes.iterator().hasNext()){
	RoadNode rn = Routing.nodes.get(i);
	//GeoPoint gp1 = new GeoPoint(latitude, longitude);
	GeoPoint gp2 = rn.mLocation;
	Location l1 = new Location("l1");
	l1.setLatitude(latitude);
	l1.setLongitude(longitude);
	Location l2 = new Location("l2");
	l2.setLatitude(gp2.getLatitude());
	l2.setLongitude(gp2.getLongitude());
	double distance = l1.distanceTo(l2);
	Log.d("distdistdist", String.valueOf(distance));
	Toast.makeText(context, " u reached node", Toast.LENGTH_LONG).show();
}
*/

