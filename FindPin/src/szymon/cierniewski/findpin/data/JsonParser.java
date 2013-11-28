package szymon.cierniewski.findpin.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

public class JsonParser extends Activity {
	
	/**
	 * Function downloads JSON file and parses it to JsonData object.
	 * @return Parsed JSON object.
	 */
	public JsonData getJSON() {
		String jsonString = downloadJsonFromUrl("https://dl.dropboxusercontent.com/u/6556265/test.json");
	  
	  try {
		  JSONObject jObj = new JSONObject(jsonString);
		  
		  JsonData data = new JsonData();
		  data.setLatitude(jObj.getJSONObject("location").getDouble("latitude"));
		  data.setLongitude(jObj.getJSONObject("location").getDouble("longitude"));
		  data.setText(jObj.getString("text"));
		  data.setImage(jObj.getString("image"));
		  
		  return data;
      } catch (JSONException e) {
          Log.e("JSON Parser", "Error parsing data " + e.toString());
          return null;
      }
  }

  
  /**
   * Function downloads JSON from given address.
   * @return Downloaded JSON file in String.
   */
  private String downloadJsonFromUrl(String address) {
    StringBuilder builder = new StringBuilder();
    HttpClient client = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(address);
    try {
      HttpResponse response = client.execute(httpGet);
      StatusLine statusLine = response.getStatusLine();
      int statusCode = statusLine.getStatusCode();
      if (statusCode == 200) {
        HttpEntity entity = response.getEntity();
        InputStream content = entity.getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
        String line;
        while ((line = reader.readLine()) != null) {
          builder.append(line);
        }
      } else {
        Log.e(JsonParser.class.toString(), "Failed to download file");
      }
    } catch (ClientProtocolException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }
} 
