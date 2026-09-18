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
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

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
        loading.setText("⚽ Loading next 3 days matches...");
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
                    "soccer_uefa_champs_league",
                    "soccer_france_ligue_one"
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
            
            HashMap<String, ArrayList<MatchPrediction>> matchesByDate = new HashMap<>();
            
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            
            Calendar today = Calendar.getInstance();
            String todayStr = inputFormat.format(today.getTime());
            
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DATE, 1);
            String tomorrowStr = inputFormat.format(tomorrow.getTime());
            
            Calendar dayAfter = Calendar.getInstance();
            dayAfter.add(Calendar.DATE, 2);
            String dayAfterStr = inputFormat.format(dayAfter.getTime());
            
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.DATE, 3);
            String maxDateStr = inputFormat.format(maxDate.getTime());
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String homeTeam = match.getString("home_team");
                String awayTeam = match.getString("away_team");
                String dateTime = match.getString("commence_time");
                String dateOnly = dateTime.substring(0, 10);
                
                // Skip matches beyond 3 days
                if (dateOnly.compareTo(maxDateStr) >= 0) continue;
                if (dateOnly.compareTo(todayStr) < 0) continue;
                
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
                        drawProb = (drawProb / total) * 100;
                        
                        String winner;
                        double winProb;
                        String predictedScore;
                        String category;
                        
                        if (homeProb > awayProb && homeProb > drawProb) {
                            winner = homeTeam;
                            winProb = homeProb;
                            predictedScore = getPredictedScore(homeProb, true);
                        } else if (awayProb > homeProb && awayProb > drawProb) {
                            winner = awayTeam;
                            winProb = awayProb;
                            predictedScore = getPredictedScore(awayProb, false);
                        } else {
                            winner = "Draw";
                            winProb = drawProb;
                            predictedScore = "1-1";
                        }
                        
                        if (winProb >= 70) category = "💎 Very Safe";
                        else if (winProb >= 60) category = "🟢 Safe";
                        else if (winProb >= 50) category = "🟡 Medium";
                        else category = "🔴 Risky";
                        
                        String dateLabel;
                        if (dateOnly.equals(todayStr)) dateLabel = "📅 TODAY";
                        else if (dateOnly.equals(tomorrowStr)) dateLabel = "📅 TOMORROW";
                        else if (dateOnly.equals(dayAfterStr)) dateLabel = "📅 DAY AFTER TOMORROW";
                        else {
                            try {
                                Date d = inputFormat.parse(dateOnly);
                                dateLabel = "📅 " + displayFormat.format(d);
                            } catch (Exception e) {
                                dateLabel = "📅 " + dateOnly;
                            }
                        }
                        
                        MatchPrediction pred = new MatchPrediction(
                            homeTeam,
                            awayTeam,
                            dateOnly,
                            dateLabel,
                            winner,
                            (int)winProb,
                            predictedScore,
                            category,
                            dateTime.substring(11, 16)
                        );
                        
                        if (!matchesByDate.containsKey(dateLabel)) {
                            matchesByDate.put(dateLabel, new ArrayList<>());
                        }
                        matchesByDate.get(dateLabel).add(pred);
                    }
                }
            }
            
            String[] dateOrder = {"📅 TODAY", "📅 TOMORROW", "📅 DAY AFTER TOMORROW"};
            for (String dateLabel : dateOrder) {
                if (matchesByDate.containsKey(dateLabel)) {
                    layout.addView(createDateHeader(dateLabel));
                    ArrayList<MatchPrediction> dayMatches = matchesByDate.get(dateLabel);
                    for (MatchPrediction m : dayMatches) {
                        layout.addView(createMatchCard(m));
                    }
                }
            }
            
            if (matchesByDate.isEmpty()) {
                TextView noMatches = new TextView(this);
                noMatches.setText("No matches in the next 3 days!");
                noMatches.setTextSize(16);
                layout.addView(noMatches);
            }
            
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Error: " + e.getMessage());
            layout.addView(error);
        }
    }
    
    private String getPredictedScore(double prob, boolean isHome) {
        String score;
        if (prob >= 80) {
            score = isHome ? "3-0" : "0-3";
        } else if (prob >= 70) {
            score = isHome ? "2-0" : "0-2";
        } else if (prob >= 60) {
            score = isHome ? "2-1" : "1-2";
        } else {
            score = isHome ? "1-0" : "0-1";
        }
        return score;
    }
    
    private TextView createDateHeader(String date) {
        TextView header = new TextView(this);
        header.setText(date);
        header.setTextSize(20);
        header.setTextColor(Color.WHITE);
        header.setBackgroundColor(Color.parseColor("#1976D2"));
        header.setPadding(25, 25, 25, 25);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 30, 0, 15);
        header.setLayoutParams(params);
        return header;
    }
    
    private LinearLayout createMatchCard(MatchPrediction m) {
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
        
        TextView category = new TextView(this);
        category.setText(m.category);
        category.setTextSize(14);
        category.setPadding(0, 0, 0, 15);
        card.addView(category);
        
        TextView teams = new TextView(this);
        teams.setText("⚽ " + m.homeTeam + " vs " + m.awayTeam);
        teams.setTextSize(18);
        teams.setTextColor(Color.BLACK);
        teams.setPadding(0, 0, 0, 15);
        card.addView(teams);
        
        TextView time = new TextView(this);
        time.setText("🕐 Time: " + m.time);
        time.setTextSize(14);
        time.setTextColor(Color.parseColor("#666666"));
        card.addView(time);
        
        card.addView(createInfoText(""));
        card.addView(createInfoText("🏆 Predicted Winner: " + m.winner, Color.parseColor("#4CAF50")));
        card.addView(createInfoText("⚽ Predicted Score: " + m.predictedScore, Color.parseColor("#2196F3")));
        card.addView(createInfoText("📊 Win Probability: " + m.prob + "%", Color.parseColor("#FF9800")));
        
        return card;
    }
    
    private TextView createInfoText(String text) {
        return createInfoText(text, Color.parseColor("#555555"));
    }
    
    private TextView createInfoText(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(color);
        tv.setPadding(0, 5, 0, 5);
        return tv;
    }
    
    private static class MatchPrediction {
        String homeTeam, awayTeam, date, dateLabel, winner, predictedScore, category, time;
        int prob;
        
        MatchPrediction(String homeTeam, String awayTeam, String date, String dateLabel,
                       String winner, int prob, String predictedScore, String category, String time) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.date = date;
            this.dateLabel = dateLabel;
            this.winner = winner;
            this.prob = prob;
            this.predictedScore = predictedScore;
            this.category = category;
            this.time = time;
        }
    }
          }
