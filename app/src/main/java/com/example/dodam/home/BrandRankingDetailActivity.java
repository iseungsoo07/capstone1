package com.example.dodam.home;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dodam.R;
import com.example.dodam.data.CosmeticRankItemData;
import com.example.dodam.data.DataManagement;
import com.example.dodam.database.Callback;
import com.example.dodam.database.DatabaseManagement;
import com.squareup.picasso.Picasso;

import java.util.List;

public class BrandRankingDetailActivity extends AppCompatActivity implements View.OnClickListener, CosmeticRankItemRVAdapter.OnItemClickListener {
    private RecyclerView cosmeticRV;
    private CosmeticRankItemRVAdapter cosmeticRankItemRVAdapter;
    private String brandName;   // BrandRanking Activity로부터 받아온 브랜드 명
    private final int REQUEST_COSMETIC_DETAIL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_brand_ranking_detail);

        initailize();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(cosmeticRankItemRVAdapter == null) {
            initializeRecyclerView();
        }

        refreshBrandCosmetics();
    }

    // 필요한 항목 초기화
    private void initailize() {
        Intent intent;

        // 데이터 수신
        intent = getIntent();

        brandName = intent.getExtras().getString("brandName");

        initializeTextView();
        initializeImageView();
    }

    // TextView 초기화
    private void initializeTextView() {
        TextView brandNameTV;

        brandNameTV = findViewById(R.id.brandRankingDetail_brandNameTV);

        brandNameTV.setText(brandName);
    }

    // ImageView 초기화
    private void initializeImageView() {
        ImageView backIV;
        final ImageView brandIV;
        final Context context;

        backIV = findViewById(R.id.brandRankingDetail_backIV);
        brandIV = findViewById(R.id.brandRankingDetail_brandIV);

        context = this;

        DatabaseManagement.getInstance().getBrandImageFromStorage(brandName, new Callback<Uri>() {
            @Override
            public void onCallback(Uri data) {
                if(data != null) {
                    Picasso.with(context).load(data).resize(200, 200).into(brandIV);
                }
            }
        });

        backIV.setOnClickListener(this);
    }

    // RecyclerView 초기화
    private void initializeRecyclerView() {
        cosmeticRV = findViewById(R.id.brandRankingDetail_cosmeticRV);
        cosmeticRV.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        cosmeticRankItemRVAdapter = new CosmeticRankItemRVAdapter(this);

        // Item ClickListener 추가
        cosmeticRankItemRVAdapter.setOnItemClickListener(this);

        cosmeticRV.setAdapter(cosmeticRankItemRVAdapter);
    }

    // 해당 브랜드의 화장품 목록 새로고침
    private void refreshBrandCosmetics() {
        // 먼저 전부 지우기
        cosmeticRankItemRVAdapter.delAllItem();

        // DB로부터 해당 브랜드에 속하는 화장품들 받아오기
        DatabaseManagement.getInstance().getBrandCosmeticsFromDatabase(brandName, new Callback<List<CosmeticRankItemData>>() {
            @Override
            public void onCallback(List<CosmeticRankItemData> data) {
                // 데이터를 갖고왔을 때만
                if(data != null) {
                    TextView cosmeticCountTV;
                    int rank;

                    // 평점 순으로 정렬
                    data = DataManagement.getInstance().sortByCosemticRate(data);

                    rank = 1;

                    for(CosmeticRankItemData cosmeticRankItemData : data) {
                        cosmeticRankItemData.setRank(rank++);

                        // RecyclerView에 하나씩 추가
                        cosmeticRankItemRVAdapter.addItem(cosmeticRankItemData);
                    }

                    // 변경된 것을 알림
                    cosmeticRankItemRVAdapter.notifyDataSetChanged();

                    // 제품 수를 표시하는 TextView 변경
                    cosmeticCountTV = findViewById(R.id.brandRankingDetail_cosmeticCountTV);
                    cosmeticCountTV.setText(data.size() + "개의 제품");
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            // 뒤로가기
            case R.id.brandRankingDetail_backIV:
                finish();

                break;
        }
    }

    // RecyclerView Item 클릭시
    @Override
    public void onItemClick(View v, int pos) {
        Intent intent;
        CosmeticRankItemData cosmeticRankItemData;

        intent = new Intent(BrandRankingDetailActivity.this, CosmeticDetailActivity.class);

        // 해당 제품 정보 넘기기
        cosmeticRankItemData = cosmeticRankItemRVAdapter.getItem(pos);

        intent.putExtra("cosmeticItemData", cosmeticRankItemData);

        // 해당 제품 화면으로 넘어가기
        startActivityForResult(intent, REQUEST_COSMETIC_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == REQUEST_COSMETIC_DETAIL) {
                // 처리할 작업 없음
            }
        }
    }
}
