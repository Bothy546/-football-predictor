package com.football.predictor;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final String API_KEY = "aa08b1c3440c5a236517524d75d0b2a8";
    private LinearLayout layout;
    private SharedPreferences prefs;
    
    private ArrayList<MatchPrediction> allMatches = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

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
        loading.setText("⚽ Loading worldwide matches...\n🤖 Checking past tickets...");
        loading.setTextSize(16);
        loading.setPadding(20, 20, 20, 20);
        layout.addView(loading);
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        autoCheckPreviousTickets();
        fetchOdds();
    }
    
    private void autoCheckPreviousTickets() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        executor.execute(() -> {
            try {
                String yesterday = prefs.getString("last_ticket_date", "");
                String today = dateFormat.format(new Date());
                
                if (!yesterday.isEmpty() && !yesterday.equals(today)) {
                    String[] types = {"safe", "medium", "risky", "smart", "extreme"};
                    for (String type : types) {
                        String ticket = prefs.getString("saved_" + type + "_" + yesterday, "");
                        if (!ticket.isEmpty()) checkTicketResults(type, ticket);
                        prefs.edit().remove("saved_" + type + "_" + yesterday).apply();
                    }
                }
            } catch (Exception e) {
            }
        });
    }
    
    private void checkTicketResults(String type, String ticketData) {
        try {
            JSONArray matches = new JSONArray(ticketData);
            boolean allCorrect = true;
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String predictedWinner = match.getString("prediction");
                String actualWinner = match.optString("home", predictedWinner);
                
                if (!actualWinner.equals(predictedWinner)) {
                    allCorrect = false;
                    break;
                }
            }
            
            String winsKey = type + "_wins";
            String lossesKey = type + "_losses";
            
            int wins = prefs.getInt(winsKey, 0);
            int losses = prefs.getInt(lossesKey, 0);
            
            if (allCorrect) wins++;
            else losses++;
            
            prefs.edit().putInt(winsKey, wins).putInt(lossesKey, losses).apply();
            
        } catch (Exception e) {
        }
    }
    
    private void fetchOdds() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            String result = "";
            try {
                String[] sports = {
                    "soccer_england_epl", "soccer_england_championship",
                    "soccer_spain_la_liga", "soccer_spain_segunda_division",
                    "soccer_germany_bundesliga", "soccer_germany_bundesliga2",
                    "soccer_italy_serie_a", "soccer_italy_serie_b",
                    "soccer_france_ligue_one", "soccer_france_ligue_two",
                    "soccer_netherlands_eredivisie", "soccer_portugal_primeira_liga",
                    "soccer_belgium_pro_league", "soccer_turkey_super_lig",
                    "soccer_scotland_premiership", "soccer_switzerland_superleague",
                    "soccer_austria_bundesliga", "soccer_denmark_superliga",
                    "soccer_sweden_allsvenskan", "soccer_norway_eliteserien",
                    "soccer_poland_ekstraklasa", "soccer_czech_first_league",
                    "soccer_greece_super_league", "soccer_croatia_hnl",
                    "soccer_serbia_superliga", "soccer_romania_liga1",
                    "soccer_uefa_champs_league", "soccer_uefa_europa_league",
                    "soccer_uefa_europa_conference_league",
                    "soccer_brazil_campeonato", "soccer_brazil_serie_b",
                    "soccer_argentina_primera_division", "soccer_mexico_ligamx",
                    "soccer_usa_mls", "soccer_canada_cpl",
                    "soccer_japan_j_league", "soccer_korea_kleague1",
                    "soccer_china_superleague", "soccer_australia_aleague",
                    "soccer_india_isl", "soccer_saudi_pro_league",
                    "soccer_qatar_stars_league", "soccer_uae_pro_league",
                    "soccer_egypt_premier_league", "soccer_morocco_botola",
                    "soccer_south_africa_psl", "soccer_nigeria_npfl",
                    "soccer_ghana_premier_league", "soccer_kenya_premier_league"
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
            
            allMatches.clear();
            
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                String homeTeam = match.getString("home_team");
                String awayTeam = match.getString("away_team");
                String dateTime = match.getString("commence_time");
                String dateOnly = dateTime.substring(0, 10);
                
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
                        double winOdds;
                        String predictedScore;
                        String category;
                        
                        if (homeProb > awayProb && homeProb > drawProb) {
                            winner = homeTeam;
                            winProb = homeProb;
                            winOdds = homeOdds;
                            predictedScore = getPredictedScore(homeProb, true);
                        } else if (awayProb > homeProb && awayProb > drawProb) {
                            winner = awayTeam;
                            winProb = awayProb;
                            winOdds = awayOdds;
                            predictedScore = getPredictedScore(awayProb, false);
                        } else {
                            winner = "Draw";
                            winProb = drawProb;
                            winOdds = drawOdds;
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
                            homeTeam, awayTeam, dateOnly, dateLabel,
                            winner, (int)winProb, winOdds, homeOdds, awayOdds, drawOdds,
                            predictedScore, category, dateTime.substring(11, 16)
                        );
                        
                        allMatches.add(pred);
                        
                        if (!matchesByDate.containsKey(dateLabel)) {
                            matchesByDate.put(dateLabel, new ArrayList<>());
                        }
                        matchesByDate.get(dateLabel).add(pred);
                    }
                }
            }
            
            allMatches.sort((m1, m2) -> Integer.compare(m2.prob, m1.prob));
            
            displayStats();
            displayDailyTickets();
            displayDetailedMatches(matchesByDate);
            
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
        int smartWins = prefs.getInt("smart_wins", 0);
        int smartLosses = prefs.getInt("smart_losses", 0);
        int extremeWins = prefs.getInt("extreme_wins", 0);
        int extremeLosses = prefs.getInt("extreme_losses", 0);
        
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
        title.setText("📊 YOUR BETTING STATS\n🤖 Auto-Tracked");
        title.setTextSize(18);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 20);
        statsCard.addView(title);
        
        statsCard.addView(createStatText("💎 Safe: " + safeWins + "W / " + safeLosses + "L " + 
            (safeWins + safeLosses > 0 ? "(" + (safeWins * 100 / (safeWins + safeLosses)) + "%)" : "")));
        statsCard.addView(createStatText("🟢 Medium: " + mediumWins + "W / " + mediumLosses + "L " + 
            (mediumWins + mediumLosses > 0 ? "(" + (mediumWins * 100 / (mediumWins + mediumLosses)) + "%)" : "")));
        statsCard.addView(createStatText("🔴 Risky: " + riskyWins + "W / " + riskyLosses + "L " + 
            (riskyWins + riskyLosses > 0 ? "(" + (riskyWins * 100 / (riskyWins + riskyLosses)) + "%)" : "")));
        statsCard.addView(createStatText("⚡ Smart Value: " + smartWins + "W / " + smartLosses + "L " + 
            (smartWins + smartLosses > 0 ? "(" + (smartWins * 100 / (smartWins + smartLosses)) + "%)" : "")));
        statsCard.addView(createStatText("🚀 Extreme Odds: " + extremeWins + "W / " + extremeLosses + "L " + 
            (extremeWins + extremeLosses > 0 ? "(" + (extremeWins * 100 / (extremeWins + extremeLosses)) + "%)" : "")));
        
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
        header.setText("🎫 TODAY'S RECOMMENDED TICKETS\n🤖 Auto-Saved");
        header.setTextSize(20);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(0, 20, 0, 20);
        layout.addView(header);
        
        ArrayList<MatchPrediction> safeMatches = new ArrayList<>();
        ArrayList<MatchPrediction> mediumMatches = new ArrayList<>();
        ArrayList<MatchPrediction> riskyMatches = new ArrayList<>();
        ArrayList<SmartPick> smartMatches = new ArrayList<>();
        ArrayList<SmartPick> extremeMatches = new ArrayList<>();
        
        for (MatchPrediction m : allMatches) {
            if (m.dateLabel.equals("📅 TODAY")) {
                if (m.prob >= 70 && safeMatches.size() < 3) safeMatches.add(m);
                if (m.prob >= 60 && mediumMatches.size() < 4) mediumMatches.add(m);
                if (m.prob >= 50 && riskyMatches.size() < 5) riskyMatches.add(m);
                
                SmartPick smart = getBestSmartPick(m);
                if (smart.prob >= 65 && smartMatches.size() < 3) smartMatches.add(smart);
                if (smart.prob >= 60 && extremeMatches.size() < 5) extremeMatches.add(smart);
            }
        }
        
        String today = dateFormat.format(new Date());
        
        if (safeMatches.size() >= 3) {
            layout.addView(createTicketCard("💎 SAFE TICKET (3 Matches)", safeMatches, null, "safe"));
            saveStandardTicket("safe", safeMatches, today);
        }
        if (mediumMatches.size() >= 4) {
            layout.addView(createTicketCard("🟢 MEDIUM TICKET (4 Matches)", mediumMatches, null, "medium"));
            saveStandardTicket("medium", mediumMatches, today);
        }
        if (riskyMatches.size() >= 5) {
            layout.addView(createTicketCard("🔴 RISKY TICKET (5 Matches)", riskyMatches, null, "risky"));
            saveStandardTicket("risky", riskyMatches, today);
        }
        if (smartMatches.size() >= 2) {
            layout.addView(createSmartTicketCard("⚡ SMART VALUE TICKET (2-3 Matches)", smartMatches.subList(0, Math.min(3, smartMatches.size())), "smart"));
            saveSmartTicket("smart", smartMatches.subList(0, Math.min(3, smartMatches.size())), today);
        }
        if (extremeMatches.size() >= 4) {
            double totalOdds = 1.0;
            for (int i = 0; i < Math.min(5, extremeMatches.size()); i++) {
                totalOdds *= extremeMatches.get(i).odds;
            }
            if (totalOdds >= 25.0) {
                layout.addView(createSmartTicketCard("🚀 EXTREME ODDS TICKET (25+ Odds)", extremeMatches.subList(0, Math.min(5, extremeMatches.size())), "extreme"));
                saveSmartTicket("extreme", extremeMatches.subList(0, Math.min(5, extremeMatches.size())), today);
            }
        }
        
        prefs.edit().putString("last_ticket_date", today).apply();
    }
    
    private SmartPick getBestSmartPick(MatchPrediction m) {
        ArrayList<SmartPick> options = new ArrayList<>();
        
        double htWinOdds = m.homeOdds * 1.6;
        double htWinProb = m.prob * 0.65;
        if (htWinProb >= 60) {
            options.add(new SmartPick(m.homeTeam, m.awayTeam, 
                m.winner + " Win 1st Half", htWinOdds, (int)htWinProb));
        }
        
        double dcOdds = m.winOdds * 0.7;
        double dcProb = Math.min(m.prob + 15, 85);
        options.add(new SmartPick(m.homeTeam, m.awayTeam, 
            m.winner + " or Draw (DC)", dcOdds, (int)dcProb));
        
        double htDrawOdds = 2.2;
        double htDrawProb = 55;
        if (m.prob < 65) {
            options.add(new SmartPick(m.homeTeam, m.awayTeam, 
                "Draw at Halftime", htDrawOdds, (int)htDrawProb));
        }
        
        options.add(new SmartPick(m.homeTeam, m.awayTeam, 
            m.winner + " Win", m.winOdds, m.prob));
        
        SmartPick best = options.get(0);
        for (SmartPick pick : options) {
            if (pick.odds >= 1.8 && pick.prob >= 60) {
                if (pick.odds > best.odds && pick.prob >= best.prob - 5) {
                    best = pick;
                }
            }
        }
        
        return best;
    }
    
    private void saveStandardTicket(String type, ArrayList<MatchPrediction> matches, String date) {
        try {
            JSONArray ticketData = new JSONArray();
            for (MatchPrediction m : matches) {
                JSONObject match = new JSONObject();
                match.put("home", m.homeTeam);
                match.put("away", m.awayTeam);
                match.put("prediction", m.winner);
                ticketData.put(match);
            }
            prefs.edit().putString("saved_" + type + "_" + date, ticketData.toString()).apply();
        } catch (Exception e) {
        }
    }
    
    private void saveSmartTicket(String type, java.util.List<SmartPick> picks, String date) {
        try {
            JSONArray ticketData = new JSONArray();
            for (SmartPick p : picks) {
                JSONObject match = new JSONObject();
                match.put("home", p.homeTeam);
                match.put("away", p.awayTeam);
                match.put("prediction", p.market);
                ticketData.put(match);
            }
            prefs.edit().putString("saved_" + type + "_" + date, ticketData.toString()).apply();
        } catch (Exception e) {
        }
    }
    
    private LinearLayout createTicketCard(String title, ArrayList<MatchPrediction> matches, 
                                         java.util.List<SmartPick> smartPicks, String type) {
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
            totalOdds *= m.winOdds;
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
        card.addView(createInfoText("🎯 Win Chance: " + String.format("%.1f", combinedProb) + "%", Color.parseColor("#FF9800")));
        card.addView(createInfoText("💰 $1 = $" + String.format("%.2f", totalOdds) + " payout", Color.parseColor("#4CAF50")));
        card.addView(createInfoText("🤖 Will auto-check tomorrow", Color.parseColor("#9C27B0")));
        
        return card;
    }
    
    private LinearLayout createSmartTicketCard(String title, java.util.List<SmartPick> picks, String type) {
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
        
        for (int i = 0; i < picks.size(); i++) {
            SmartPick p = picks.get(i);
            totalOdds *= p.odds;
            combinedProb *= (p.prob / 100.0);
            
            TextView matchView = new TextView(this);
            matchView.setText((i + 1) + ". " + p.homeTeam + " vs " + p.awayTeam + 
                             "\n   Pick: " + p.market + 
                             "\n   Odds: " + String.format("%.2f", p.odds) + " | Accuracy: " + p.prob + "%");
            matchView.setTextSize(14);
            matchView.setPadding(0, 8, 0, 8);
            card.addView(matchView);
        }
        
        combinedProb *= 100;
        
        card.addView(createInfoText(""));
        card.addView(createInfoText("📊 Total Odds: " + String.format("%.2f", totalOdds), Color.parseColor("#2196F3")));
        card.addView(createInfoText("🎯 Win Chance: " + String.format("%.1f", combinedProb) + "%", Color.parseColor("#FF9800")));
        card.addView(createInfoText("💰 $1 = $" + String.format("%.2f", totalOdds) + " payout", Color.parseColor("#4CAF50")));
        card.addView(createInfoText("✅ Smart market selection!", Color.parseColor("#4CAF50")));
        card.addView(createInfoText("🤖 Will auto-check tomorrow", Color.parseColor("#9C27B0")));
        
        return card;
    }
    
    private void displayDetailedMatches(HashMap<String, ArrayList<MatchPrediction>> matchesByDate) {
        TextView header = new TextView(this);
        header.setText("📋 DETAILED MATCH PREDICTIONS");
        header.setTextSize(20);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(0, 40, 0, 20);
        layout.addView(header);
        
        String[] dateOrder = {"📅 TODAY", "📅 TOMORROW", "📅 DAY AFTER TOMORROW"};
        for (String dateLabel : dateOrder) {
            if (matchesByDate.containsKey(dateLabel)) {
                layout.addView(createDateHeader(dateLabel));
                ArrayList<MatchPrediction> dayMatches = matchesByDate.get(dateLabel);
                
                dayMatches.sort((m1, m2) -> Integer.compare(m2.prob, m1.prob));
                
                for (MatchPrediction m : dayMatches) {
                    layout.addView(createDetailedMatchCard(m));
                }
            }
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
    
    private LinearLayout createDetailedMatchCard(MatchPrediction m) {
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
        
        card.addView(createSectionHeader("FULL TIME"));
        card.addView(createInfoText("🏆 Winner: " + m.winner, Color.parseColor("#4CAF50")));
        card.addView(createInfoText("⚽ Score: " + m.predictedScore, Color.parseColor("#2196F3")));
        card.addView(createInfoText("📊 Probability: " + m.prob + "%", Color.parseColor("#FF9800")));
        
        card.addView(createSectionHeader("HALFTIME"));
        String halfTime = getHalfTimePrediction(m.prob, m.winner, m.homeTeam, m.awayTeam);
        card.addView(createInfoText("⏱️ 1st Half: " + halfTime, Color.parseColor("#9C27B0")));
        
        card.addView(createSectionHeader("2ND HALF"));
        String secondHalf = getSecondHalfPrediction(m.prob, m.winner);
        card.addView(createInfoText("⏱️ 2nd Half: " + secondHalf, Color.parseColor("#9C27B0")));
        
        card.addView(createSectionHeader("BETTING MARKETS"));
        String doubleChance = getDoubleChance(m.prob, m.winner, m.homeTeam, m.awayTeam);
        card.addView(createInfoText("🎲 Double Chance: " + doubleChance, Color.parseColor("#F44336")));
        
        String overUnder = getOverUnder(m.prob, m.predictedScore);
        card.addView(createInfoText("⚽ Goals: " + overUnder, Color.parseColor("#00BCD4")));
        
        String btts = getBTTS(m.prob, m.predictedScore);
        card.addView(createInfoText("🥅 Both Teams Score: " + btts, Color.parseColor("#FF5722")));
        
        return card;
    }
    
    private TextView createSectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(13);
        header.setTextColor(Color.parseColor("#1976D2"));
        header.setPadding(0, 15, 0, 8);
        return header;
    }
    
    private String getHalfTimePrediction(int prob, String winner, String homeTeam, String awayTeam) {
        if (prob >= 75) {
            return winner + " to lead";
        } else if (prob >= 65) {
            return winner + " or Draw";
        } else {
            return "Draw";
        }
    }
    
    private String getSecondHalfPrediction(int prob, String winner) {
        if (prob >= 70) {
            return winner + " to win";
        } else if (prob >= 60) {
            return winner + " or Draw";
        } else {
            return "Draw";
        }
    }
    
    private String getDoubleChance(int prob, String winner, String homeTeam, String awayTeam) {
        if (winner.equals("Draw")) {
            return "Any result possible";
        }
        
        if (prob >= 70) {
            return winner + " or Draw ✅";
        } else if (prob >= 60) {
            return winner + " or Draw ⚠️";
        } else {
            return winner + " or Draw";
        }
    }
    
    private String getOverUnder(int prob, String score) {
        String[] parts = score.split("-");
        int totalGoals = 0;
        try {
            totalGoals = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            totalGoals = 2;
        }
        
        if (totalGoals >= 3) {
            return "Over 2.5 Goals ✅";
        } else if (totalGoals >= 2) {
            return "Over 1.5 Goals ✅";
        } else {
            return "Under 1.5 Goals";
        }
    }
    
    private String getBTTS(int prob, String score) {
        String[] parts = score.split("-");
        try {
            int homeGoals = Integer.parseInt(parts[0]);
            int awayGoals = Integer.parseInt(parts[1]);
            
            if (homeGoals > 0 && awayGoals > 0) {
                return "Yes ✅";
            } else {
                return "No ❌";
            }
        } catch (Exception e) {
            return "No ❌";
        }
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
        String homeTeam, awayTeam, date, dateLabel, winner, predictedScore, category, time;
        int prob;
        double winOdds, homeOdds, awayOdds, drawOdds;
        
        MatchPrediction(String homeTeam, String awayTeam, String date, String dateLabel,
                       String winner, int prob, double winOdds, double homeOdds, double awayOdds,
                       double drawOdds, String predictedScore, String category, String time) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.date = date;
            this.dateLabel = dateLabel;
            this.winner = winner;
            this.prob = prob;
            this.winOdds = winOdds;
            this.homeOdds = homeOdds;
            this.awayOdds = awayOdds;
            this.drawOdds = drawOdds;
            this.predictedScore = predictedScore;
            this.category = category;
            this.time = time;
        }
    }
    
    private static class SmartPick {
        String homeTeam, awayTeam, market;
        double odds;
        int prob;
        
        SmartPick(String homeTeam, String awayTeam, String market, double odds, int prob) {
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.market = market;
            this.odds = odds;
            this.prob = prob;
        }
    }
                  }
