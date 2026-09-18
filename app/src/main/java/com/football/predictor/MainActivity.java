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
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String API_KEY = "aa08b1c3440c5a236517524d75d0b2a8";
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
        loading.setText("⚽ Loading safe matches with real odds...");
        loading.setTextSize(16);
        loading.setPadding(20, 20, 20, 20);
        layout.addView(loading);
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        fetchOdds();
    }
    
    private void fetchOdds() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            String result = "";
            try {
                String[] sports = {
                    "soccer_england_epl",
                    "soccer_spain_la_liga",
                    "soccer_germany_bundesliga",
                    "soccer_italy_serie_a",
                    "soccer_uefa_champs_league"
                };
                
                JSONArray allMatches = new JSONArray();
                
                for (String sport : sports) {
                    try {
                        URL url = new URL("https://api.the-odds-api.com/v4/sports/" + sport + 
                                         "/odds/?apiKey=" + API_KEY + 
                                         "&regions=eu&markets=h2h&oddsFormat=decimal");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        
                        JSONArray matches = new JSONArray(response.toString());
                        for (int i = 0; i < matches.length(); i++) {
                            allMatches.put(matches.getJSONObject(i));
                        }
                        
                        Thread.sleep(300);
                    } catch (Exception e) {
                    }
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
            
            ArrayList<MatchPrediction> verySafe = new ArrayList<>();
            ArrayList<MatchPrediction> safe = new ArrayList<>();
            ArrayList<MatchPrediction> medium = new ArrayList<>();
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String homeTeam = match.getString("home_team");
                String awayTeam = match.getString("away_team");
                String date = match.getString("commence_time").substring(0, 10);
                
                JSONArray bookmakers = match.getJSONArray("bookmakers");
                if (bookmakers.length() > 0) {
                    JSONObject bookmaker = bookmakers.getJSONObject(0);
                    JSONArray markets = bookmaker.getJSONArray("markets");
                    if (markets.length() > 0) {
                        JSONArray outcomes = markets.getJSONObject(0).getJSONArray("outcomes");
                        
                        double homeOdds = 0, awayOdds = 0, drawOdds = 0;
                        for (int j = 0; j < outcomes.length(); j++) {
                            JSONObject outcome = outcomes.getJSONObject(j);
                            String name = outcome.getString("name");
                            double odds = outcome.getDouble("price");
                            
                            if (name.equals(homeTeam)) homeOdds = odds;
                            else if (name.equals(awayTeam)) awayOdds = odds;
                            else drawOdds = odds;
                        }
                        
                        double homeProb = (1 / homeOdds) * 100;
                        double awayProb = (1 / awayOdds) * 100;
                        double drawProb = (1 / drawOdds) * 100;
                        
                        double total = homeProb + awayProb + drawProb;
                        homeProb = (homeProb / total) * 100;
                        awayProb = (awayProb / total) * 100;
                        
                        String winner;
                        double winProb;
                        if (homeProb > awayProb) {
                            winner = homeTeam;
                            winProb = homeProb;
                        } else {
                            winner = awayTeam;
                            winProb = awayProb;
                        }
                        
                        MatchPrediction pred = new MatchPrediction(
                            homeTeam + " vs " + awayTeam,
                            date,
                            winner,
                            (int)winProb,
                            winProb > 60 ? winner : "Draw",
                            winner,
                            winner + " or Draw"
                        );
                        
                        if (winProb >= 75) verySafe.add(pred);
                        else if (winProb >= 65) safe.add(pred);
                        else if (winProb >= 55) medium.add(pred);
                    }
                }
            }
            
            if (verySafe.size() > 0) {
                layout.addView(createCategoryHeader("💎 VERY SAFE MATCHES (75-85% Win Probability)", "#4CAF50"));
                for (MatchPrediction m : verySafe) {
                    layout.addView(createMatchCard(m.teams, m.date, m.winner, m.prob + "%", m.firstHalf, m.secondHalf, m.doubleChance));
                }
            }
            
            if (safe.size() > 0) {
                layout.addView(createCategoryHeader("🟢 SAFE MATCHES (65-74% Win Probability)", "#8BC34A"));
                for (MatchPrediction m : safe) {
                    layout.addView(createMatchCard(m.teams, m.date, m.winner, m.prob + "%", m.firstHalf, m.secondHalf, m.doubleChance));
                }
            }
            
            if (medium.size() > 0) {
                layout.addView(createCategoryHeader("🟡 MEDIUM MATCHES (55-64% Win Probability)", "#FFC107"));
                for (MatchPrediction m : medium) {
                    layout.addView(createMatchCard(m.teams, m.date, m.winner, m.prob + "%", m.firstHalf, m.secondHalf, m.doubleChance));
                }
            }
            
            if (verySafe.size() == 0 && safe.size() == 0 && medium.size() == 0) {
                TextView noMatches = new TextView(this);
                noMatches.setText("No upcoming matches found. Check back later!");
                noMatches.setTextSize(16);
                layout.addView(noMatches);
            }
            
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Parse Error: " + e.getMessage());
            layout.addView(error);
        }
    }
    
    private TextView createCategoryHeader(String text, String color) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(16);
        header.setTextColor(Color.WHITE);
        header.setBackgroundColor(Color.parseColor(color));
        header.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 30, 0, 10);
        header.setLayoutParams(params);
        return header;
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
        params.setMargins(0, 0, 0, 15);
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
    
    private static class MatchPrediction {
        String teams, date, winner, firstHalf, secondHalf, doubleChance;
        int prob;
        
        MatchPrediction(String teams, String date, String winner, int prob,
                       String firstHalf, String secondHalf, String doubleChance) {
            this.teams = teams;
            this.date = date;
            this.winner = winner;
            this.prob = prob;
            this.firstHalf = firstHalf;
            this.secondHalf = secondHalf;
            this.doubleChance = doubleChance;
        }
    }
                 }
