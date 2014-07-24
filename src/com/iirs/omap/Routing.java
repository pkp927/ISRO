package com.iirs.omap;

import java.io.IOException;
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
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class Routing {

	Context context;
	TextToSpeech tts;
	private MapView mapView;
	ArrayList<OverlayItem> overlayItemArray;
	RoadManager roadManager;
	Polyline roadOverlay;
	FolderOverlay poiMarkers;
	ArrayList<Marker> roadMarkers = new ArrayList<Marker>();
	static ArrayList<RoadNode> nodes = new ArrayList<RoadNode>();
	
	HttpClient client;
	JSONObject json;
	JSONArray json_arr;
	Double s_lat,s_long;
	Double f_lat,f_long;
	Boolean success1=false,success2=false;
	
	public Routing(Context c, MapView mv, TextToSpeech t){
		context = c;
		tts = t;
		mapView = mv;
	}
	
	public void drawRoadRoute(double latitude,double longitude,String addr){
		roadManager = new OSRMRoadManager();
		
		s_lat = latitude;
		s_long = longitude;
		
		f_lat = null;
		f_long = null;
		
		// get coordinates from location name
		client = new DefaultHttpClient();
		new Read().execute(addr);
		
	}
	
	public void draw(){
		
				// define the points in the arraylist
				ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
				GeoPoint startPoint = new GeoPoint(s_lat,s_long);
				waypoints.add(startPoint);
				GeoPoint endPoint = new GeoPoint(f_lat,f_long);
				waypoints.add(endPoint);
				
				// get road between points from road manager
				Road road = roadManager.getRoad(waypoints);
				
				if(road.mStatus != Road.STATUS_OK){
					Toast.makeText(context, "error in getting road", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// draw road overlay on the map
				roadOverlay = RoadManager.buildRoadOverlay(road, context);
				mapView.getOverlays().add(roadOverlay);
				
				// draw nodes along the route
				Drawable nodeIcon = context.getResources().getDrawable(R.drawable.marker_node);
		        Drawable icon = context.getResources().getDrawable(R.drawable.ic_continue);
				for (int i=0; i<road.mNodes.size(); i++){
				        RoadNode node = road.mNodes.get(i);
				        Marker nodeMarker = new Marker(mapView);
				        nodeMarker.setPosition(node.mLocation);
				        nodeMarker.setIcon(nodeIcon);
				        nodeMarker.setTitle("Step "+i);
				        nodeMarker.setSnippet(node.mInstructions);
				        nodeMarker.setSubDescription(Road.getLengthDurationText(node.mLength, node.mDuration));
				        nodeMarker.setImage(icon);
				        mapView.getOverlays().add(nodeMarker);
				        nodes.add(node);
				        roadMarkers.add(nodeMarker);
				}
				        
				mapView.invalidate();
	}

	public JSONArray getLatLongFromAddress1(String youraddress) throws ClientProtocolException, IOException, JSONException{
	    StringBuilder url = new StringBuilder("http://nominatim.openstreetmap.org/search?format=json&addressdetails=1&q=");
	    youraddress = youraddress.replace(" ", "%20");
	    Log.d("fnjsdnfkj",youraddress);
	    url.append(youraddress);
	    HttpGet get = new HttpGet(url.toString());
	    HttpResponse r = client.execute(get);
	    int status = r.getStatusLine().getStatusCode();
	    if(status == 200){
	    	HttpEntity e = r.getEntity();
	    	String data = EntityUtils.toString(e);
	    	JSONArray jobj = new JSONArray(data);
	    	return jobj;
	    }else{
	    	return null;
	    }

	}
	
	public JSONObject getLatLongFromAddress2(String youraddress) throws ClientProtocolException, IOException, JSONException{
	    StringBuilder url = new StringBuilder("http://maps.google.com/maps/api/geocode/json?address=");
	    youraddress = youraddress.replace(" ", "%20");
	    url.append(youraddress + "&sensor=false");
	    Log.d("bjhhb",url.toString());
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
	
	public class Read extends AsyncTask<String, Integer, String>{

		protected String doInBackground(String... arg) {
			try {
				json_arr = getLatLongFromAddress1(arg[0]);
				if(json_arr != null && !json_arr.isNull(0)){
					success1 = true;
				}else{
					json = getLatLongFromAddress2(arg[0]);
					success2 = true;
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
			
			if(json_arr != null || json != null){
				try {
					if(success1){
						Log.d("parneet","1111111");
						f_lat = json_arr.getJSONObject(0).getDouble("lat");
						f_long = json_arr.getJSONObject(0).getDouble("lon");
					}else if(success2){
						Log.d("parneet","2222222"+json.toString());
						f_lat = ((JSONArray)json.get("results")).getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lat");
						f_long = ((JSONArray)json.get("results")).getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getDouble("lng");
					}
					success1=false;
					success2=false;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}else{
			}
			Log.d(String.valueOf(f_lat),String.valueOf(f_long)+"rgrtgrtgrtgrtgrt");
			// clear previous map
			clear_route();
			// draw new map
			if(f_lat != null && f_long != null) draw();
			else Toast.makeText(context, "error in getting route", Toast.LENGTH_LONG).show();
			
		}	
	}
	
	public void drawPOI(double latitude,double longitude,String str){
		// clear previous pois
		if(poiMarkers != null) mapView.getOverlays().remove(poiMarkers);
		// create instance of poi provider
		NominatimPOIProvider poiProvider = new NominatimPOIProvider();
		GeoPoint startPoint = new GeoPoint(latitude, longitude);
		// get list of pois nearby
		ArrayList<POI> pois = poiProvider.getPOICloseTo(startPoint, str, 50, 0.1);
		// draw pois on the map 
		if(pois != null){
			poiMarkers = new FolderOverlay(context);
			mapView.getOverlays().add(poiMarkers);
			Drawable poiIcon = context.getResources().getDrawable(R.drawable.marker_poi);
			for (POI poi:pois){
		        Marker poiMarker = new Marker(mapView);
		        poiMarker.setTitle(poi.mType);
		        poiMarker.setSnippet(poi.mDescription);
		        poiMarker.setPosition(poi.mLocation);
		        poiMarker.setIcon(poiIcon);
		        poiMarkers.add(poiMarker);
			}
		}else{
			Toast.makeText(context, "error in getting pois", Toast.LENGTH_LONG).show();
		}
	}
	
	public void clear(){
		mapView.getOverlays().clear();
		mapView.invalidate();
	}
	
	public void clear_poi(){
		if(poiMarkers != null) mapView.getOverlays().remove(poiMarkers);
		mapView.invalidate();
	}
	
	public void clear_route(){
		if(roadOverlay != null) mapView.getOverlays().remove(roadOverlay);
		if(!roadMarkers.isEmpty()){
			for(Marker m: roadMarkers)
				mapView.getOverlays().remove(m);
		}
		mapView.invalidate();
	}
	
}
