package am.example.tagtabstrip;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import am.util.viewpager.adapter.ViewsPagerAdapter;
import am.widget.tagtabstrip.TagTabStrip;

public class TagTabStripActivity extends BaseActivity {

    @Override
    protected int getContentViewLayoutResources() {
        return R.layout.activity_tagtabstrip;
    }

    @Override
    @SuppressWarnings("all")
    protected void initResource(Bundle savedInstanceState) {
        setSupportActionBar(R.id.tts_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        ViewPager vpContent = (ViewPager) findViewById(R.id.tts_vp_fragments);
        TagTabStrip ttsTags = (TagTabStrip) findViewById(R.id.tts_tts_tags);
        vpContent.setAdapter(new ViewsPagerAdapter(getPagers()));
        ttsTags.bindViewPager(vpContent);
    }

    private ArrayList<View> getPagers() {
        ArrayList<View> views = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TextView text = new TextView(this);
            text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 180);
            text.setText(String.format(Locale.getDefault(), "%d", i + 1));
            text.setGravity(Gravity.CENTER);
            text.setTextColor(0xff000000);
            views.add(text);
        }
        return views;
    }
}
