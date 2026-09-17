package com.football.predictor;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        
        // Match 1
        layout.addView(createMatchCard(
            "Manchester United vs Liverpool",
            "Jan 20, 2024",
            "Liverpool", "65%",
            "Draw", "Liverpool",
            "Liverpool or Draw"
        ));
        
        // Match 2
        layout.addView(createMatchCard(
            "Real Madrid vs Barcelona",
            "Jan 21, 2024",
            "Real Madrid", "58%",
            "Real Madrid", "Draw",
            "Real Madrid or Draw"
        ));
        
        // Match 3
        layout.addView(createMatchCard(
            "PSG vs Bayern Munich",
            "Jan 22, 2024",
            "Bayern Munich", "62%",
            "Draw", "Bayern Munich",
            "Bayern or Draw"
        ));
        
        scrollView.addView(layout);
        setContentView(scrollView);
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
        card.addView(createInfoText("🏆 Winner: " + winner, Color.parseColor("#4CAF50")));
        card.addView(createInfoText("📊 Win Probability: " + prob, Color.parseColor("#2196F3")));
        card.addView(createInfoText(""));
        card.addView(createInfoText("⏱️ 1st Half Winner: " + firstHalf));
        card.addView(createInfoText("⏱️ 2nd Half Winner: " + secondHalf));
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
