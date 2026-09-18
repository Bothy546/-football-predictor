package com.football.predictor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final String API_KEY = "aa08b1c3440c5a236517524d75d0b2a8";
    private LinearLayout layout;
    private SharedPreferences prefs;
    
    private ArrayList<MatchPrediction> allMatches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("BettingTracker", MODE_PRIVATE);
        
        ScrollView scrollView = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        
        TextView loading = new TextView(this);
        loading.setText("⚽ Loading daily tickets...");
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
                    "soccer_france_ligue_one",
                    "soccer_netherlands_eredivisie",
                    "soccer_portugal_primeira_liga",
                    "soccer_brazil_campeonato",
                    "soccer_argentina_primera_division"
                };
                
                JSONArray allMatchesJson = new JSONArray();
                
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
                            allMatchesJson.put(matches.getJSONObject(i));
                        }
                        
                        Thread.sleep(300);
                    } catch (Exception e) {
                    }
                }
                
                result = allMatchesJson.toString();
                
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
            
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String todayStr = inputFormat.format(new Date());
            
            allMatches.clear();
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String homeTeam = match.getString("home_team");
                String awayTeam = match.getString("away_team");
                String dateTime = match.getString("commence_time");
                String dateOnly = dateTime.substring(0, 10);
                
                if (!dateOnly.equals(todayStr)) continue;
                
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
                        double winOdds;
                        
                        if (homeProb > awayProb && homeProb > drawProb) {
                            winner = homeTeam;
                            winProb = homeProb;
                            winOdds = homeOdds;
                        } else if (awayProb > homeProb && awayProb > drawProb) {
                            winner = awayTeam;
                            winProb = awayProb;
                            winOdds = awayOdds;
                        } else {
                            winner = "Draw";
                            winProb = drawProb;
                            winOdds = drawOdds;
                        }
                        
                        MatchPrediction pred = new MatchPrediction(
                            homeTeam,
                            awayTeam,
                            winner,
                            (int)winProb,
                            winOdds,
                            dateTime.substring(11, 16)
                        );
                        
                        allMatches.add(pred);
                    }
                }
            }
            
            allMatches.sort((m1, m2) -> Integer.compare(m2.prob, m1.prob));
            
            displayStats();
            displayDailyTickets();
            displayAllMatches();
            
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("Error: " + e.getMessage());
            layout.addView(error);
        }
    }
    
    private void displayStats() {
        int safeWins = prefs.getInt("safe_wins", 0);
        int safeLosses = prefs.getInt("safe_losses", 0);
        int mediumWins = prefs.getInt("medium_wins", 0);
        int mediumLosses = prefs.getInt("medium_losses", 0);
        int riskyWins = prefs.getInt("risky_wins", 0);
        int riskyLosses = prefs.getInt("risky_losses", 0);
        
        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.VERTICAL);
        statsCard.setBackgroundColor(Color.parseColor("#1976D2"));
        statsCard.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        statsCard.setLayoutParams(params);
        
        TextView title = new TextView(this);
        title.setText("📊 YOUR BETTING STATS");
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        statsCard.addView(title);
        
        statsCard.addView(createStatText("💎 Safe Tickets: " + safeWins + "W / " + safeLosses + "L " + 
            (safeWins + safeLosses > 0 ? "(" + (safeWins * 100 / (safeWins + safeLosses)) + "% win rate)" : "")));
        statsCard.addView(createStatText("🟢 Medium Tickets: " + mediumWins + "W / " + mediumLosses + "L " + 
            (mediumWins + mediumLosses > 0 ? "(" + (mediumWins * 100 / (mediumWins + mediumLosses)) + "% win rate)" : "")));
        statsCard.addView(createStatText("🔴 Risky Tickets: " + riskyWins + "W / " + riskyLosses + "L " + 
            (riskyWins + riskyLosses > 0 ? "(" + (riskyWins * 100 / (riskyWins + riskyLosses)) + "% win rate)" : "")));
        
        layout.addView(statsCard);
    }
    
    private TextView createStatText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(Color.WHITE);
        tv.setPadding(0, 5, 0, 5);
        return tv;
    }
    
    private void displayDailyTickets() {
        TextView header = new TextView(this);
        header.setText("🎫 TODAY'S RECOMMENDED TICKETS");
        header.setTextSize(20);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(0, 20, 0, 20);
        layout.addView(header);
        
        ArrayList<MatchPrediction> safeMatches = new ArrayList<>();
        ArrayList<MatchPrediction> mediumMatches = new ArrayList<>();
        ArrayList<MatchPrediction> riskyMatches = new ArrayList<>();
        
        for (MatchPrediction m : allMatches) {
            if (m.prob >= 70 && safeMatches.size() < 3) safeMatches.add(m);
            if (m.prob >= 60 && mediumMatches.size() < 4) mediumMatches.add(m);
            if (m.prob >= 50 && riskyMatches.size() < 5) riskyMatches.add(m);
        }
        
        if (safeMatches.size() >= 3) {
            layout.addView(createTicketCard("💎 SAFE TICKET (3 Matches)", safeMatches, "safe"));
        }
        if (mediumMatches.size() >= 4) {
            layout.addView(createTicketCard("🟢 MEDIUM TICKET (4 Matches)", mediumMatches, "medium"));
        }
        if (riskyMatches.size() >= 5) {
            layout.addView(createTicketCard("🔴 RISKY TICKET (5 Matches)", riskyMatches, "risky"));
        }
        
        if (safeMatches.size() < 3 && mediumMatches.size() < 4 && riskyMatches.size() < 5) {
            TextView noTickets = new TextView(this);
            noTickets.setText("⚠️ Not enough matches today for ticket generation.\nCheck back later!");
            noTickets.setTextSize(15);
            noTickets.setPadding(20, 20, 20, 20);
            layout.addView(noTickets);
        }
    }
    
    private LinearLayout createTicketCard(String title, ArrayList<MatchPrediction> matches, String type) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(0, 0, 0, 15);
        card.addView(titleView);
        
        double totalOdds = 1.0;
        double combinedProb = 1.0;
        
        for (int i = 0; i < matches.size(); i++) {
            MatchPrediction m = matches.get(i);
            totalOdds *= m.odds;
            combinedProb *= (m.prob / 100.0);
            
            TextView matchView = new TextView(this);
            matchView.setText((i + 1) + ". " + m.homeTeam + " vs " + m.awayTeam + "\n   Pick: " + m.winner + " (" + m.prob + "%)");
            matchView.setTextSize(14);
            matchView.setPadding(0, 8, 0, 8);
            card.addView(matchView);
        }
        
        combinedProb *= 100;
        
        card.addView(createInfoText(""));
        card.addView(createInfoText("📊 Total Odds: " + String.format("%.2f", totalOdds), Color.parseColor("#2196F3")));
        card.addView(createInfoText("🎯 Combined Win Chance: " + String.format("%.1f", combinedProb) + "%", Color.parseColor("#FF9800")));
        card.addView(createInfoText("💰 $1 stake = $" + String.format("%.2f", totalOdds) + " payout", Color.parseColor("#4CAF50")));
        
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 20, 0, 0);
        
        Button wonBtn = new Button(this);
        wonBtn.setText("✅ WON");
        wonBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
        wonBtn.setTextColor(Color.WHITE);
        wonBtn.setOnClickListener(v -> markTicket(type, true));
        
        Button lostBtn = new Button(this);
        lostBtn.setText("❌ LOST");
        lostBtn.setBackgroundColor(Color.parseColor("#F44336"));
        lostBtn.setTextColor(Color.WHITE);
        lostBtn.setOnClickListener(v -> markTicket(type, false));
        
        buttonLayout.addView(wonBtn);
        buttonLayout.addView(lostBtn);
        card.addView(buttonLayout);
        
        return card;
    }
    
    private void markTicket(String type, boolean won) {
        String winsKey = type + "_wins";
        String lossesKey = type + "_losses";
        
        int wins = prefs.getInt(winsKey, 0);
        int losses = prefs.getInt(lossesKey, 0);
        
        if (won) {
            wins++;
        } else {
            losses++;
        }
        
        prefs.edit().putInt(winsKey, wins).putInt(lossesKey, losses).apply();
        
        recreate();
    }
    
    private void displayAllMatches() {
        TextView header = new TextView(this);
        header.setText("📋 ALL TODAY'S MATCHES");
        header.setTextSize(20);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(0, 40, 0, 20);
        layout.addView(header);
        
        if (allMatches.isEmpty()) {
            TextView noMatches = new TextView(this);
            noMatches.setText("No matches found for today.");
            noMatches.setTextSize(15);
            layout.addView(noMatches);
        }
        
        for (MatchPrediction m : allMatches) {
            layout.addView(createSimpleMatchCard(m));
        }
    }
    
    private LinearLayout createSimpleMatchCard(MatchPrediction m) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(25, 25, 25, 25);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        card.setLayoutParams(params);
        
        TextView teams = new TextView(this);
        teams.setText("⚽ " + m.homeTeam + " vs " + m.awayTeam);
        teams.setTextSize(16);
        teams.setTextColor(Color.BLACK);
        card.addView(teams);
        
        TextView time = new TextView(this);
        time.setText("🕐 " + m.time);
        time.setTextSize(13);
        time.setTextColor(Color.parseColor("#666666"));
        card.addView(time);
        
        card.addView(createInfoText("🏆 " + m.winner + " - " + m.prob + "% (Odds: " + String.format("%.2f", m.odds) + ")", 
            Color.parseColor("#4CAF50")));
        
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
        String homeTeam, awayTeam, winner, time;
        int prob;
        double odds;
        
        MatchPrediction(String homeTeam, String awayTeam, String winner, int prob, double odds, String time) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.winner = winner;
            this.prob = prob;
            this.odds = odds;
            this.time = time;
        }
    }
                          }
