package MyPackage;

import jakarta.servlet.ServletException;
import java.util.Date;
import java.text.SimpleDateFormat;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Servlet implementation class MyServlet
 */
public class MyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MyServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String city = request.getParameter("city");
		if (city == null || city.trim().isEmpty()) {
			city = "Bhopal";
		}
		
		// Fetch weather using free Open-Meteo API (No API key required!)
		try {
			String encodedCity = java.net.URLEncoder.encode(city, "UTF-8");
			String geocodingUrlStr = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1&language=en&format=json";
			
			URL geocodingUrl = new URL(geocodingUrlStr);
			HttpURLConnection conn = (HttpURLConnection) geocodingUrl.openConnection();
			conn.setRequestMethod("GET");
			
			InputStream inpStream = conn.getInputStream();
			InputStreamReader reader = new InputStreamReader(inpStream);
			StringBuilder responseContent = new StringBuilder();
			Scanner scanner = new Scanner(reader);
			while (scanner.hasNext()) {
				responseContent.append(scanner.nextLine());
			}
			scanner.close();
			conn.disconnect();
			
			Gson gson = new Gson();
			JsonObject geoJson = gson.fromJson(responseContent.toString(), JsonObject.class);
			
			double latitude = 23.25; // Default to Bhopal coords
			double longitude = 77.41;
			String resolvedCity = city;
			
			if (geoJson.has("results") && geoJson.getAsJsonArray("results").size() > 0) {
				JsonObject firstResult = geoJson.getAsJsonArray("results").get(0).getAsJsonObject();
				latitude = firstResult.get("latitude").getAsDouble();
				longitude = firstResult.get("longitude").getAsDouble();
				resolvedCity = firstResult.get("name").getAsString();
				if (firstResult.has("country")) {
					resolvedCity += ", " + firstResult.get("country").getAsString();
				}
			}
			
			// Now call Forecast API
			String forecastUrlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude 
					+ "&longitude=" + longitude 
					+ "&current=temperature_2m,relative_humidity_2m,weather_code,cloud_cover,wind_speed_10m,visibility";
			
			URL forecastUrl = new URL(forecastUrlStr);
			HttpURLConnection forecastConn = (HttpURLConnection) forecastUrl.openConnection();
			forecastConn.setRequestMethod("GET");
			
			InputStream forecastInp = forecastConn.getInputStream();
			InputStreamReader forecastReader = new InputStreamReader(forecastInp);
			StringBuilder forecastResponse = new StringBuilder();
			Scanner forecastScanner = new Scanner(forecastReader);
			while (forecastScanner.hasNext()) {
				forecastResponse.append(forecastScanner.nextLine());
			}
			forecastScanner.close();
			forecastConn.disconnect();
			
			JsonObject forecastJson = gson.fromJson(forecastResponse.toString(), JsonObject.class);
			JsonObject current = forecastJson.getAsJsonObject("current");
			
			double temp = current.get("temperature_2m").getAsDouble();
			int tempInCelsius = (int) Math.round(temp);
			int humidity = current.get("relative_humidity_2m").getAsInt();
			double windSpeed = current.get("wind_speed_10m").getAsDouble();
			double visibilityInMeter = current.has("visibility") ? current.get("visibility").getAsDouble() : 10000.0;
			int visibility = (int) (visibilityInMeter / 1000);
			int cloudCover = current.get("cloud_cover").getAsInt();
			int weatherCode = current.get("weather_code").getAsInt();
			
			// Map weather code to condition string
			String weatherCondition = "Clear";
			if (weatherCode == 0) {
				weatherCondition = "Clear";
			} else if (weatherCode >= 1 && weatherCode <= 3) {
				weatherCondition = "Clouds";
			} else if (weatherCode == 45 || weatherCode == 48) {
				weatherCondition = "Fog";
			} else if (weatherCode >= 51 && weatherCode <= 55) {
				weatherCondition = "Drizzle";
			} else if (weatherCode >= 61 && weatherCode <= 65) {
				weatherCondition = "Rain";
			} else if (weatherCode >= 71 && weatherCode <= 77) {
				weatherCondition = "Snow";
			} else if (weatherCode >= 80 && weatherCode <= 82) {
				weatherCondition = "Rain";
			} else if (weatherCode >= 95 && weatherCode <= 99) {
				weatherCondition = "Thunderstorm";
			}
			
			// Date & Time
			long currentTimeMillis = System.currentTimeMillis();
			SimpleDateFormat sdfDate = new SimpleDateFormat("EEE MMM dd yyyy");
			String date = sdfDate.format(new Date(currentTimeMillis));
			
			SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
			String formattedTime = sdfTime.format(new Date(currentTimeMillis));
			
			// Set attributes
			request.setAttribute("date", date);
			request.setAttribute("city", resolvedCity);
			request.setAttribute("visibility", visibility);
			request.setAttribute("temperature", tempInCelsius);
			request.setAttribute("weatherCondition", weatherCondition); 
			request.setAttribute("humidity", humidity);    
			request.setAttribute("windSpeed", windSpeed);
			request.setAttribute("cloudCover", cloudCover);
			request.setAttribute("currentTime", formattedTime);
			request.setAttribute("weatherData", forecastResponse.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			// Fallback attributes for error handling
			request.setAttribute("date", new SimpleDateFormat("EEE MMM dd yyyy").format(new Date()));
			request.setAttribute("city", city + " (Offline)");
			request.setAttribute("visibility", 10);
			request.setAttribute("temperature", 25);
			request.setAttribute("weatherCondition", "Clouds"); 
			request.setAttribute("humidity", 50);    
			request.setAttribute("windSpeed", 10.0);
			request.setAttribute("cloudCover", 40);
			request.setAttribute("currentTime", new SimpleDateFormat("HH:mm").format(new Date()));
			request.setAttribute("weatherData", "Error: " + e.getMessage());
		}
		
		request.getRequestDispatcher("index.jsp").forward(request, response);
	}

}
