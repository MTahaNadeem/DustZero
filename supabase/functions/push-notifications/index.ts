import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const FCM_SERVER_KEY = Deno.env.get("FCM_SERVER_KEY")

serve(async (req) => {
  try {
    const payload = await req.json()
    // payload is the webhook payload from Supabase
    const { type, table, record, old_record } = payload
    
    if (table === 'devices' && type === 'UPDATE') {
      if (!old_record.fault && record.fault) {
        // Fault occurred
        await sendPushNotification(record.device_id, "System Fault", "Hardware fault detected on your device!")
      }
    }
    
    return new Response(JSON.stringify({ success: true }), { headers: { "Content-Type": "application/json" } })
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), { status: 500 })
  }
})

async function sendPushNotification(deviceId: string, title: string, body: string) {
  if (!FCM_SERVER_KEY) return
  
  // Here we would lookup the FCM token from a user_tokens table or device_settings
  // For now, assume a static topic or lookup is handled
  
  const response = await fetch("https://fcm.googleapis.com/fcm/send", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `key=${FCM_SERVER_KEY}`
    },
    body: JSON.stringify({
      to: `/topics/device_${deviceId}`,
      notification: {
        title,
        body
      }
    })
  })
  
  await response.json()
}
