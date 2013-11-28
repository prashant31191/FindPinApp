package szymon.cierniewski.findpin;

import szymon.cierniewski.findpin.data.JsonData;
import szymon.cierniewski.findpin.data.JsonParser;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.FIFOLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;



public class MainActivity extends Activity {
	
	private JsonData data;
	private Location myLocation;
	private boolean locationIsCurrent = false;
	private GoogleMap googleMap;
	
    private ImageLoader imageLoader;
    private DisplayImageOptions options;
    private Marker marker;
    


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setLocalization();
		
		downloadJsonAndSetMarker();		
	}
	
	
	
	/**
	 * Function downloads JSON data, initializes Image Loader and sets map Marker.
	 * Function is called in onCreate, but if network is disabled this function is called
	 * again after turning on the network (in callback) 
	 */
	private void downloadJsonAndSetMarker() {
		// set network policy - necessary to avoid NetworkOnMainThreadException 
	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	    StrictMode.setThreadPolicy(policy);
	    
		// download JSON file
		JsonParser parser = new JsonParser();
		data = parser.getJSON();
		
		if (data == null) {
			Toast.makeText(this, "Can't download data. Please check your internet connetion.", Toast.LENGTH_LONG).show();
			
			// wait on network is turned on - register receiver
			IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			registerReceiver(mNetworkStateReceiver , mNetworkStateFilter);
		}
		else {		// successfully download JSON file.
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
	         
			
			// set image loader
	        initImageLoader();
	        imageLoader = ImageLoader.getInstance();
	         
	        options = new DisplayImageOptions.Builder()
	            .showStubImage(R.drawable.ic_launcher)       
	            .showImageForEmptyUri(R.drawable.ic_launcher)  
	            .cacheInMemory()
	            .cacheOnDisc().bitmapConfig(Bitmap.Config.RGB_565).build();
	         
	        
	        // add first marker (from JSON position)
	        if ( googleMap != null ) {
	            
	            googleMap.setInfoWindowAdapter(new ImageMarkerAdapter());
	            
	            LatLng position = new LatLng(data.getLatitude(), data.getLongitude());
	    		
	            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
	            googleMap.addMarker(new MarkerOptions()
	    								.position(position)
	    								.title(data.getText())
	    								.snippet(data.getImage())); 
	        }
		}	
	}
	
	
	
	/**
	 * This Broadcast Receiver is called when network status has changed.
	 * After network connection is enabled, Receiver retries download JSON file and unregisters Receiver.
	 */
  	private final BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {
  	    @Override
  	        public void onReceive(Context context, Intent intent) {
  	    	
	  	    	ConnectivityManager cm =
	  	    	        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
	  	    	 
	  	    	NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	  	    	if (activeNetwork != null && activeNetwork.isConnectedOrConnecting())
	  	    	{
	  	    		unregisterReceiver(mNetworkStateReceiver);
  	    		
	  	    		downloadJsonAndSetMarker();
	  	    	}
  	        }
  	};
	
	
	/**
	 * Marker Adapter is used to set image in Marker Window (after tap on pin)
	 */	
	private class ImageMarkerAdapter implements InfoWindowAdapter {
		 
        private View view;
 
        public ImageMarkerAdapter() {
            view = getLayoutInflater().inflate(R.layout.image_marker,
                    null);
        }
 
        
        private View resetMarker(Marker marker) {
 
            if (MainActivity.this.marker != null
                    && MainActivity.this.marker.isInfoWindowShown()) {
                MainActivity.this.marker.hideInfoWindow();
                MainActivity.this.marker.showInfoWindow();
            }
            return null;
        }
 
        @Override
        public View getInfoContents(final Marker marker) {
            MainActivity.this.marker = marker;
 
            String url = marker.getSnippet();
 
            final ImageView image = ((ImageView) view.findViewById(R.id.image));
 
            if (url != null && !url.equalsIgnoreCase("null")
                    && !url.equalsIgnoreCase("")) {
                imageLoader.displayImage(url, image, options,
                        new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri,
                                    View view, Bitmap loadedImage) {
                                super.onLoadingComplete(imageUri, view,
                                        loadedImage);
                                resetMarker(marker);
                            }
                        });
            } else {
                image.setImageResource(R.drawable.ic_launcher);
            }
 
            final String title = marker.getTitle();
            final TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                titleUi.setText(title);
            } else {
                titleUi.setText("");
            }
 
            return view;
        }


        @Override
		public View getInfoWindow(Marker arg0) {
			return null;  // must be null if not used
		}
    }
	
	
	
	/**
	 * Initialize Image Loader
	 */
	private void initImageLoader() {
        int memoryCacheSize;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            int memClass = ((ActivityManager)
                    getSystemService(Context.ACTIVITY_SERVICE))
                    .getMemoryClass();
            memoryCacheSize = (memClass / 8) * 1024 * 1024;
        } else {
            memoryCacheSize = 2 * 1024 * 1024;
        }
 
        final ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                this).threadPoolSize(5)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .memoryCacheSize(memoryCacheSize)
                .memoryCache(new FIFOLimitedMemoryCache(memoryCacheSize-1000000))
                .denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO).enableLogging()
                .build();
 
        ImageLoader.getInstance().init(config);
    }
	
	
	/**
	 * Get last (historical) location and start finding current location
	 */
	private void setLocalization()	{
		// ####### FIND LAST LOCATION ########
		// Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		Location lastKnownLocationByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownLocationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (lastKnownLocationByGps != null && lastKnownLocationByNetwork != null) {
            if (lastKnownLocationByGps.getTime() > lastKnownLocationByNetwork.getTime()) {
            	myLocation = lastKnownLocationByGps;
            } else {
            	myLocation = lastKnownLocationByNetwork;
            }
        } else if (lastKnownLocationByGps != null) {
        	myLocation = lastKnownLocationByGps;
        } else {
        	myLocation = lastKnownLocationByNetwork;
        }    
        
		// ############# FIND CURRENT LOCATION #################
         LocationListener locationListener = new LocationListener() {    // Define a listener that responds to location updates
        	    
			@Override
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location provider.
				myLocation = location;  
				locationIsCurrent = true;
				//Toast.makeText(MainActivity.this, "Current location has been found.", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onProviderDisabled(String provider) {}

			@Override
			public void onProviderEnabled(String provider) {}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {}
            
        };
          
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {		
        	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3 * 60 * 1000, 10, locationListener);
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {		
    		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3 * 60 * 1000, 10, locationListener);
		}
	}
	
	
	/**
	 * Function moves map camera to user location (after clicked "Find my location" button) 
	 */
	public void bFindMyClicked(View view) {
		if (myLocation != null) {
			if ( googleMap != null ) {
	            
	            LatLng position = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
	    		
	            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
	            googleMap.addMarker(new MarkerOptions().position(position));
	            
	            if (locationIsCurrent == false)
	            	Toast.makeText(this, "Selected location is based on historical data.", Toast.LENGTH_LONG).show();
			}
		}
		else {
			setLocalization();
			Toast.makeText(this, "Can't find your location. Please check your GPS and try again.", Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * Function opens Google Maps Navigation (after clicked "Navigate" button ) 
	 */
	public void bNavigationClicked(View view) {
		if (myLocation != null && data != null) {
			if (locationIsCurrent == false)
            	Toast.makeText(this, "Selected location is based on historical data.", Toast.LENGTH_LONG).show();
			
			Uri uri;
    		uri = Uri.parse("http://maps.google.com/maps?saddr="+ myLocation.getLatitude() +","+ myLocation.getLongitude() 
			    		+"&daddr="+ data.getLatitude() +","+ data.getLongitude());
    		
    		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
    		intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
			startActivity(intent);
		}
		else {
			setLocalization();
			Toast.makeText(this, "Can't find your location. Please check your GPS and try again.", Toast.LENGTH_LONG).show();
		}
	}
	
	
	
}
