import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

const OPENWEATHER_API_KEY = Deno.env.get("OPENWEATHER_API_KEY");

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    console.log("OPENWEATHER_API_KEY configured:", !!OPENWEATHER_API_KEY);

    const url_obj = new URL(req.url);
    let lat = url_obj.searchParams.get("lat");
    let lon = url_obj.searchParams.get("lon");

    if (!lat || !lon) {
      return new Response(JSON.stringify({ error: "Missing lat/lon" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!OPENWEATHER_API_KEY) {
      return new Response(JSON.stringify({ error: "API key not configured" }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // Call OpenWeatherMap One Call API (or standard forecast if One Call is paid)
    // Using standard 5 day / 3 hour forecast for free tier
    const url = `https://api.openweathermap.org/data/2.5/forecast?lat=${lat}&lon=${lon}&appid=${OPENWEATHER_API_KEY}&units=metric&cnt=5`;
    const response = await fetch(url);
    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || "Failed to fetch weather");
    }

    // Process forecast to generate cleaning insights
    const current = data.list[0];
    
    // next 12 hours = next 4 blocks (each is 3 hours)
    const next_12_hours = data.list.slice(1, 5).map((item: any) => ({
      dt: item.dt,
      timestamp: item.dt_txt,
      temp: item.main.temp,
      pop: Math.round((item.pop || 0) * 100), // OpenWeather returns pop as 0 to 1
      conditions: item.weather[0].main
    }));

    const result = {
      current: {
        temp: current.main.temp,
        description: current.weather[0].description,
        icon: current.weather[0].icon
      },
      next_12_hours: next_12_hours
    };

    return new Response(JSON.stringify(result), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
