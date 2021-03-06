package net.optimusbs.videoapp.Activities;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.widget.IconTextView;

import net.optimusbs.videoapp.Adapters.VideoListByTagAdapter;
import net.optimusbs.videoapp.Classes.Video;
import net.optimusbs.videoapp.R;
import net.optimusbs.videoapp.UtilityClasses.Constants;
import net.optimusbs.videoapp.UtilityClasses.VolleyRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Santo on 1/2/2017.
 */

public class VideoPlayer extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {
    @InjectView(R.id.title)
    TextView title;

    @InjectView(R.id.likeCount)
    TextView likeCount;

    @InjectView(R.id.description)
    TextView description;

    @InjectView(R.id.viewCount)
    TextView viewCount;

    @InjectView(R.id.commentCount)
    TextView commentCount;


    @InjectView(R.id.indicator_description_visibility)
    IconTextView indicatorDescription;

    @InjectView(R.id.titleLayout)
    RelativeLayout titleLayout;

    @InjectView(R.id.related_videos_list)
    RecyclerView relatedVideosList;


    String videoId;
    Video video;

    boolean description_layout_visible = false;
    String tag;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());
        setContentView(R.layout.fragment_youtube_player);
        ButterKnife.inject(this);
        YouTubePlayerView playerView = (YouTubePlayerView) findViewById(R.id.player);
        playerView.initialize(Constants.API_KEY, this);
        initializeRecyclerView();
        getIntentData();


    }

    private void initializeRecyclerView() {
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        relatedVideosList.setLayoutManager(mLayoutManager);
        relatedVideosList.setItemAnimator(new DefaultItemAnimator());
    }

    private void getIntentData() {
        Bundle bundle = getIntent().getBundleExtra("bundle");
        if(bundle.containsKey("video_id")){
            videoId = getIntent().getBundleExtra("bundle").getString("video_id");
            getVideoData();
        }else if(bundle.containsKey("video")){
            video = (Video) bundle.getSerializable("video");
            videoId = video.getId();
            setUpViews(video);
        }
        tag = bundle.getString("tag");

        if(tag!=null && !tag.isEmpty()){
            getRelatedVideosByTag(tag);
        }


    }

    private void getRelatedVideosByTag(final String tag) {
        DatabaseReference tagRef = FirebaseDatabase.getInstance().getReference(Constants.TAG_REF);
        tagRef.child(tag).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> videoList = (ArrayList<String>) dataSnapshot.getValue();
                videoList.remove(videoId);
              //  Log.d("videoremove", String.valueOf(videoList.contains(video)));

                VideoListByTagAdapter videoListByTagAdapter = new VideoListByTagAdapter(videoList, getApplicationContext(),tag);

                relatedVideosList.setAdapter(videoListByTagAdapter);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getVideoData() {
        String url = Constants.getDataUrl(videoId);
        Log.d("url",url);
        VolleyRequest.sendRequestGet(this, url, new VolleyRequest.VolleyCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d("result", result);
                parseJson(result);
            }
        });
    }

    private void parseJson(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject itemsObject = jsonObject.getJSONArray("items").getJSONObject(0);
            JSONObject snippetObject = itemsObject.getJSONObject("snippet");
            JSONObject statisticsObject = itemsObject.getJSONObject("statistics");
            String videoId = itemsObject.getString("id");
            String thumbnail = snippetObject.getJSONObject("thumbnails").getJSONObject("default").getString("url");
            String publishedAt = snippetObject.getString("publishedAt");
            String title = snippetObject.getString("title");
            String description = snippetObject.getString("description");
            String viewCount = statisticsObject.getString("viewCount");

            String likeCount;
            if(statisticsObject.has("likeCount")){
                likeCount = statisticsObject.getString("likeCount");
            }else{
                likeCount = "0";
            }
            String commentCount = statisticsObject.getString("commentCount");


            Video video = new Video(videoId, title, description, publishedAt, viewCount, likeCount, commentCount,thumbnail);

            setUpViews(video);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void setUpViews(Video video) {
        title.setText(video.getTitle());
        description.setText(video.getDescription());

        titleLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(description_layout_visible){
                    description.setVisibility(View.GONE);
                    description_layout_visible = false;
                    indicatorDescription.setText("{fa-caret-down}");
                }else {
                    description.setVisibility(View.VISIBLE);
                    description_layout_visible = true;
                    indicatorDescription.setText("{fa-caret-up}");

                }
            }
        });

        viewCount.setText(video.getViewCount());
        likeCount.setText(video.getLikeCount());
        commentCount.setText(video.getCommentCount());

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        youTubePlayer.cueVideo(videoId);
        youTubePlayer.play();
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

    }


}
