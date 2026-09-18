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
        loading.setText("⚽ Loading worldwide matches...\n📊 With advanced predictions");
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
                    "soccer_england_championship",
                    "soccer_spain_la_liga",
                    "soccer_spain_segunda_division",
                    "soccer_germany_bundesliga",
                    "soccer_germany_bundesliga2",
                    "soccer_italy_serie_a",
                    "soccer_italy_serie_b",
                    "soccer_france_ligue_one",
                    "soccer_france_ligue_two",
                    "soccer_netherlands_eredivisie",
                    "soccer_portugal_primeira_liga",
                    "soccer_belgium_pro_league",
                    "soccer_turkey_super_lig",
                    "soccer_scotland_premiership",
                    "soccer_switzerland_superleague",
                    "soccer_austria_bundesliga",
                    "soccer_denmark_superliga",
                    "soccer_sweden_allsvenskan",
                    "soccer_norway_eliteserien",
                    "soccer_poland_ekstraklasa",
                    "soccer_czech_first_league",
                    "soccer_greece_super_league",
                    "soccer_croatia_hnl",
                    "soccer_serbia_superliga",
                    "soccer_romania_liga1",
                    "soccer_uefa_champs_league",
                    "soccer_uefa_europa_league",
                    "soccer_uefa_europa_conference_league",
                    "soccer_brazil_campeonato",
                    "soccer_brazil_serie_b",
                    "soccer_argentina_primera_division",
                    "soccer_mexico_ligamx",
                    "soccer_usa_mls",
                    "soccer_canada_cpl",
                    "soccer_japan_j_league",
                    "soccer_korea_kleague1",
                    "soccer_china_superleague",
                    "soccer_australia_aleague",
                    "soccer_india_isl",
                    "soccer_saudi_pro_league",
                    "soccer_qatar_stars_league",
                    "soccer_uae_pro_league",
                    "soccer_egypt_premier_league",
                    "soccer_morocco_botola",
                    "soccer_south_africa_psl",
                    "soccer_nigeria_npfl",
                    "soccer_ghana_premier_league",
                    "soccer_kenya_premier_league"
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
                    
                    dayMatches.sort((m1, m2) -> Integer.compare(m2.prob, m1.prob));
                    
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
        
        card.addView(createSectionHeader("FULL TIME PREDICTION"));
        card.addView(createInfoText("🏆 Winner: " + m.winner, Color.parseColor("#4CAF50")));
        card.addView(createInfoText("⚽ Score: " + m.predictedScore, Color.parseColor("#2196F3")));
        card.addView(createInfoText("📊 Probability: " + m.prob + "%", Color.parseColor("#FF9800")));
        
        card.addView(createInfoText(""));
        
        card.addView(createSectionHeader("1ST HALF PREDICTION"));
        String halfTimeResult = getHalfTimePrediction(m.prob, m.winner, m.homeTeam, m.awayTeam);
        card.addView(createInfoText("⏱️ 1st Half: " + halfTimeResult, Color.parseColor("#9C27B0")));
        
        card.addView(createSectionHeader("2ND HALF PREDICTION"));
        String secondHalfResult = getSecondHalfPrediction(m.prob, m.winner);
        card.addView(createInfoText("⏱️ 2nd Half: " + secondHalfResult, Color.parseColor("#9C27B0")));
        
        card.addView(createInfoText(""));
        
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
            return winner + " to win 2nd half";
        } else if (prob >= 60) {
            return winner + " or Draw 2nd half";
        } else {
            return "Draw 2nd half";
        }
    }
    
    private String getDoubleChance(int prob, String winner, String homeTeam, String awayTeam) {
        if (winner.equals("Draw")) {
            return "Any result possible";
        }
        
        if (prob >= 70) {
            return winner + " or Draw ✅ (Safe)";
        } else if (prob >= 60) {
            return winner + " or Draw ⚠️ (Medium)";
        } else {
            if (winner.equals(homeTeam)) {
                return homeTeam + " or Draw / " + awayTeam + " or Draw";
            } else {
                return awayTeam + " or Draw / " + homeTeam + " or Draw";
            }
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
        
        if (prob >= 70 && totalGoals >= 2) {
            return "Over 1.5 Goals ✅";
        } else if (totalGoals >= 3) {
            return "Over 2.5 Goals";
        } else if (totalGoals <= 1) {
            return "Under 1.5 Goals";
        } else {
            return "Over 1.5 Goals";
        }
    }
    
    private String getBTTS(int prob, String score) {
        String[] parts = score.split("-");
        try {
            int homeGoals = Integer.parseInt(parts[0]);
            int awayGoals = Integer.parseInt(parts[1]);
            
            if (homeGoals > 0 && awayGoals > 0) {
                return "Yes ✅ (Both teams score)";
            } else {
                return "No ❌ (One team won't score)";
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
