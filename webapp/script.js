
document.getElementById('weatherForm').addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent default form submission

    var cityInput = document.getElementById('city');
    var errorMsg = document.getElementById('errorMsg');
    var cityValue = cityInput.value.trim();

    if (cityValue === '') {
        errorMsg.style.display = 'block'; // Show error message
        return;
    }
    
    errorMsg.style.display = 'none'; // Hide error message

    // Check if we are running in a static web environment (GitHub pages, HTML Preview, local file, etc.)
    var isStatic = window.location.protocol === 'file:' || 
                   window.location.hostname.includes('github') || 
                   window.location.hostname.includes('preview') ||
                   !window.location.port;

    if (isStatic) {
        // Direct browser API call to Open-Meteo (No server required!)
        var geocodingUrl = 'https://geocoding-api.open-meteo.com/v1/search?name=' + encodeURIComponent(cityValue) + '&count=1&language=en&format=json';

        fetch(geocodingUrl)
            .then(function(res) { return res.json(); })
            .then(function(geoData) {
                var lat = 23.25;
                var lon = 77.41;
                var resolvedName = cityValue;

                if (geoData.results && geoData.results.length > 0) {
                    var place = geoData.results[0];
                    lat = place.latitude;
                    lon = place.longitude;
                    resolvedName = place.name + (place.country ? ', ' + place.country : '');
                }

                var forecastUrl = 'https://api.open-meteo.com/v1/forecast?latitude=' + lat + 
                                  '&longitude=' + lon + 
                                  '&current=temperature_2m,relative_humidity_2m,weather_code,cloud_cover,wind_speed_10m,visibility';

                return fetch(forecastUrl)
                    .then(function(res) { return res.json(); })
                    .then(function(weatherData) {
                        var current = weatherData.current;
                        
                        // Map weather code to string
                        var weatherCode = current.weather_code;
                        var weatherCondition = "Clear";
                        if (weatherCode >= 1 && weatherCode <= 3) {
                            weatherCondition = "Clouds";
                        } else if (weatherCode === 45 || weatherCode === 48) {
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

                        // Get current date and time
                        var now = new Date();
                        var dateStr = now.toDateString();
                        var timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });

                        // Update the DOM
                        document.getElementById('displayCity').innerHTML = '<i class="fas fa-city"></i> ' + resolvedName;
                        document.getElementById('displayTemp').innerHTML = '<i class="fas fa-thermometer-half"></i> ' + Math.round(current.temperature_2m) + '&deg;C';
                        document.getElementById('displayDate').textContent = dateStr;
                        document.getElementById('displayTime').textContent = timeStr;
                        document.getElementById('displayCondition').textContent = weatherCondition;
                        document.getElementById('displayVisibility').textContent = Math.round(current.visibility / 1000);
                        document.getElementById('displayWindSpeed').textContent = current.wind_speed_10m;
                        document.getElementById('displayCloudCover').textContent = current.cloud_cover;

                        // Unhide container
                        document.getElementById('weatherDetailsContainer').style.display = 'block';
                    });
            })
            .catch(function(err) {
                console.error(err);
                alert("Failed to load weather data. Please check your internet connection.");
            });
    } else {
        // If deployed to a real Tomcat server, submit to JSP/Servlet backend
        this.submit();
    }
});