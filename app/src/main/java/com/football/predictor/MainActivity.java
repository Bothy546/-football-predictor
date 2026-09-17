package com.football.predictor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String API_KEY = "ae9fbdf09fc947ef896300853507d15c";
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        
        TextView loading = new TextView(this);
        loading.setText("⚽ Loading upcoming matches...");
        loading.setTextSize(16);
        loading.setPadding(20, 20, 20, 20);
        layout.addView(loading);
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        fetchMatches();
    }
    
    private void fetchMatches() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            String result = "";
            try {
                // Premier League = 2021, La Liga = 2014, Bundesliga = 2002
                String[] leagues = {"2021", "2014", "2002", "2019", "2015"};
                JSONArray allMatches = new JSONArray();
                
                for (String leagueId : leagues) {
                    URL url = new URL("https://api.football-data.org/v4/competitions/" + leagueId + "/matches?status=SCHEDULED");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("X-Auth-Token", API_KEY);
                    conn.setRequestMethod("GET");
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    JSONArray matches = json.getJSONArray("matches");
                    
                    // Get first 5 upcoming matches from each league
                    for (int i = 0; i < Math.min(5, matches.length()); i++) {
                        allMatches.put(matches.getJSONObject(i));
                    }
                    
                    Thread.sleep(500); // Avoid rate limit
                }
                
                result = allMatches.toString();
                
            } catch (Exception e) {
                result = "ERROR:" + e.getMessage();
            }
            
            String finalResult = result;
            handler.post(() -> {
                layout.removeAllViews();
                if (finalResult.startsWith("ERROR:")) {
                    TextView error = new TextView(this);
                    error.setText("❌ Error: " + finalResult);
                    error.setTextColor(Color.RED);
                    layout.addView(error);
                } else {
                    displayMatches(finalResult);
                }
            });
        });
    }
    
    private void displayMatches(String jsonData) {
        try {
            JSONArray matches = new JSONArray(jsonData);
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                JSONObject homeTeam = match.getJSONObject("homeTeam");
                JSONObject awayTeam = match.getJSONObject("awayTeam");
                String date = match.getString("utcDate").substring(0, 10);
                
                // Simple prediction logic (you can improve this)
                String winner = Math.random() > 0.5 ? homeTeam.getString("name") : awayTeam.getString("name");
                int prob = 50 + (int)(Math.random() * 30);
                
                layout.addView(createMatchCard(
                    homeTeam.getString("name") + " vs " + awayTeam.getString("name"),
                    date,
                    winner, prob + "%",
                    "Draw", winner,
                    winner + " or Draw"
                ));
            }
            
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Parse Error: " + e.getMessage());
            layout.addView(error);
        }
    }
    
    private LinearLayout createMatchCard(String teams, String date, 
                                         String winner, String prob,
                                         String firstHalf, String secondHalf,
                                         String doubleChance) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);
        
        TextView title = new TextView(this);
        title.setText("⚽ " + teams);
        title.setTextSize(18);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, 20);
        card.addView(title);
        
        card.addView(createInfoText("📅 Date: " + date));
        card.addView(createInfoText(""));
        card.addView(createInfoText("🏆 Predicted Winner: " + winner, Color.parseColor("#4CAF50")));
        card.addView(createInfoText("📊 Win Probability: " + prob, Color.parseColor("#2196F3")));
        card.addView(createInfoText(""));
        card.addView(createInfoText("⏱️ 1st Half: " + firstHalf));
        card.addView(createInfoText("⏱️ 2nd Half: " + secondHalf));
        card.addView(createInfoText(""));
        card.addView(createInfoText("🎲 Double Chance: " + doubleChance, Color.parseColor("#FF9800")));
        
        return card;
    }
    
    private TextView createInfoText(String text) {
        return createInfoText(text, Color.parseColor("#555555"));
    }
    
    private TextView createInfoText(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(color);
        tv.setPadding(0, 5, 0, 5);
        return tv;
    }
                               }
