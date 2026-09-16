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
    const { lat, lon } = await req.json();

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
    const next3h = data.list[1];
    const next6h = data.list[2];
    
    const isRainingNow = current.weather[0].main === 'Rain' || current.weather[0].main === 'Drizzle';
    const isRainingSoon = (next3h && (next3h.weather[0].main === 'Rain' || next3h.weather[0].main === 'Drizzle')) || 
                          (next6h && (next6h.weather[0].main === 'Rain' || next6h.weather[0].main === 'Drizzle'));
                          
    let cleaning_insight = "Clear skies expected — good cleaning conditions.";
    if (isRainingNow) {
        cleaning_insight = "Currently raining — automatic cleaning is blocked.";
    } else if (isRainingSoon) {
        cleaning_insight = "Rain expected soon — automatic cleaning will be blocked during rainfall.";
    }

    const result = {
      current: {
        condition: current.weather[0].main,
        description: current.weather[0].description,
        temp: current.main.temp,
        icon: current.weather[0].icon
      },
      forecast: {
        next3h: next3h ? { condition: next3h.weather[0].main, description: next3h.weather[0].description } : null,
      },
      cleaning_insight
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
