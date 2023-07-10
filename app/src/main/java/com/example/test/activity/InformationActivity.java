package com.example.test.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.test.R;
import com.example.test.api.RetrofitClient;
import com.example.test.dto.DogDto;
import com.example.test.fragment.HomeFragment;
import com.example.test.room.DogDataDatabase;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InformationActivity extends AppCompatActivity {
    private Call<DogDto> call;
    private ImageView dogImg;
    private ImageButton bookmarkButton;
    private TextView dogBredFor;
    private TextView dogLifeSpan;
    private TextView dogTemperant;
    private TextView dogWeightHeight;
    private String url;
    private Button moreButton;
    private DogDataDatabase db;
    private Intent infoIntent;
    private Toolbar toolbar;
    private int id;
    private int position;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        initializeViews(); //view Inflating

        //액션바 뒤로가기 설정
        toolbar = findViewById(R.id.toolBar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //intent 생성
        infoIntent = getIntent();
        id = infoIntent.getIntExtra("id", 0); //id 값 받아오기
        position = infoIntent.getIntExtra("position", 0);
        url = infoIntent.getStringExtra("imgUrl");

        //화면 전환 시 북마크 체크 후 이미지 set
        db = Room.databaseBuilder(this, DogDataDatabase.class, "DogData").allowMainThreadQueries().build(); //db 빌드
        if (db.getDogDao().checkData(id))
            bookmarkButton.setImageResource(R.drawable.selected_bookmark_icon);
        else
            bookmarkButton.setImageResource(R.drawable.unselected_bookmark_icon);

        //북마크 버튼 클릭 시 이벤트 처리
        bookmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isBookmark = db.getDogDao().checkData(id);
                if (isBookmark) {
                    bookmarkButton.setImageResource(R.drawable.unselected_bookmark_icon);
                    db.getDogDao().updateBookmarkCheck(false, id);
                } else {
                    bookmarkButton.setImageResource(R.drawable.selected_bookmark_icon);
                    db.getDogDao().updateBookmarkCheck(true, id);
                }
            }
        });

        //API에서 상세 정보 검색
        getApiData();

    }


    private void initializeViews() {
        dogImg = findViewById(R.id.dogImg);
        bookmarkButton = findViewById(R.id.bookmarkButton);
        dogBredFor = findViewById(R.id.bredForData);
        dogLifeSpan = findViewById(R.id.lifeSpanData);
        dogTemperant = findViewById(R.id.temperantData);
        dogWeightHeight = findViewById(R.id.weightAndheightData);
        moreButton = findViewById(R.id.moreButton);
    }

    public void getApiData(){
        //API로 상세 정보 데이터 GET
        call = RetrofitClient.getApiService().getSearchData("live_mtPILdV1Vd1b0kcRxjsB1KVMOOHipR18xuUthvy0Y8gH9ZGvNW69RrCip5CErxth", id + "");
        call.enqueue(new Callback<DogDto>() {
            @Override
            public void onResponse(Call<DogDto> call, Response<DogDto> response) {

                Log.d("TAG", "onResponse: " + response.body().getBred_for());
                if (Objects.isNull(response.body().getBred_for()) || response.body().getBred_for().isEmpty()) {
                    dogBredFor.setText(". . . . ");
                } else
                    dogBredFor.setText(response.body().getBred_for());
                dogLifeSpan.setText(response.body().getLifeSpan());
                dogTemperant.setText(response.body().getTemperament());
                toolbar.setTitle(response.body().getName());

                //하단 버튼 클릭 시 구글에 검색해주는 이벤트 구현
                moreButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchUrl(response);
                    }
                });

                dogWeightHeight.setText(calculateAvg(response));

                Glide.with(getApplicationContext())
                        .load(url) // 이미지 소스 로드
                        .thumbnail(0.1f) // 실제 이미지 크기 중 30%만 먼저 가져와서 흐릿하게 보여줌
                        .into(dogImg); // 이미지 띄울 view 선택
            }

            @Override
            public void onFailure(Call<DogDto> call, Throwable t) {
                Log.d("TAG", "onFailure: API 호출 실패");
            }
        });

    }

    //강아지 상세 정보 google에 검색해서 띄워주는 기능 구현
    public void searchUrl(Response<DogDto> response){
        String url = "https://www.google.com/search?q=" + response.body().getName();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    //Weight, Height 평균 계산
    public String calculateAvg(Response<DogDto> response){
        //Weight, Height의 평균 구하기
        String avgWeight = response.body().getWeight().getMetric();
        String avgHeight = response.body().getHeight().getMetric();
        if (response.body().getWeight().getMetric().contains("-")){
            avgWeight = response.body().getWeight().getWeightAvg().toString();
        }
        if (response.body().getHeight().getMetric().contains("-")){
            avgHeight = response.body().getHeight().getHeightAvg().toString();
        }
        String result = "avg. " + avgWeight + "kg / " + avgHeight + "cm";
        return result;
    }
    
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { //툴바 뒤로가기 버튼 눌렸을 때 동작
            Intent finishIntent = new Intent(getApplicationContext(), HomeFragment.class);
            finishIntent.putExtra("position", position);
            finishIntent.putExtra("id", id);
            setResult(RESULT_OK, finishIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //기기 내의 뒤로가기 버튼 누를 때 실행
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, infoIntent);
        finish();
        super.onBackPressed();
    }


}