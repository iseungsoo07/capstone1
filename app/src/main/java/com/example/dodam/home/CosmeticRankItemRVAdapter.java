package com.example.dodam.home;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dodam.R;
import com.example.dodam.data.CosmeticRankItemData;
import com.example.dodam.database.Callback;
import com.example.dodam.database.DatabaseManagement;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CosmeticRankItemRVAdapter extends RecyclerView.Adapter<CosmeticRankItemRVAdapter.ItemViewHolder> {
    private ArrayList<CosmeticRankItemData> listData = new ArrayList<>(); // adapter에 들어갈 list
    private OnItemClickListener mListener = null;                  // listener 객체
    private Context context;

    public CosmeticRankItemRVAdapter(Context context) {
        this.context = context;
    }

    // listener interface
    public interface OnItemClickListener {
        void onItemClick(View v, int pos);
    }

    // OnItemClickListener 객체 설정
    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        // LayoutInflater를 이용하여 item.xml을 inflate
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cosmetic_rank_item, parent, false);

        return new ItemViewHolder(view);
    }

    // Item 항목 하나하나씩 bind, 즉 보여주는 메소드
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.onBind(listData.get(position));
    }

    // RecyclerView의 총 개수 가져오기
    @Override
    public int getItemCount() {
        return listData.size();
    }

    // 외부에서 item 항목 추가
    public void addItem(CosmeticRankItemData data) {
        listData.add(data);
    }

    // item 항목 가져오기
    public CosmeticRankItemData getItem(int pos) { return listData.get(pos); }

    // 외부에서 모든 item 항목 삭제
    public void delAllItem() {
        listData.clear();
    }

    // subView 셋팅
    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView rankTV, brandNameTV, cosmeticNameTV, rateTV;
        private ImageView cosmeticIV;

        ItemViewHolder(View itemView) {
            super(itemView);

            // ClickListener 설정
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos;

                    pos = getAdapterPosition();

                    // listener 객체의 메서드 호출
                    if(pos != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(v, pos);
                    }
                }
            });

            rankTV = itemView.findViewById(R.id.cosmeticRankItem_rankTV);
            brandNameTV = itemView.findViewById(R.id.cosmeticRankItem_brandNameTV);
            cosmeticNameTV = itemView.findViewById(R.id.cosmeticRankItem_cosmeticNameTV);
            rateTV = itemView.findViewById(R.id.cosmeticRankItem_rateTV);
            cosmeticIV = itemView.findViewById(R.id.cosmeticRankItem_cosmeticIV);
        }

        void onBind(CosmeticRankItemData data) {
            // 랭킹이 0이면 안보이게

            if(data.getRank() == 0) {
                rankTV.setVisibility(View.INVISIBLE);
            } else {
                //rankTV.setText(String.valueOf(data.getRank()));
                rankTV.setText(String.valueOf(listData.indexOf(data) + 1));
            }

            brandNameTV.setText(data.getBrandName());
            cosmeticNameTV.setText(data.getCosmeticName());
            rateTV.setText(Double.toString(data.getRate()));

            DatabaseManagement.getInstance().getCosmeticImageFromStorage(data.getBrandName(), data.getCosmeticName()
                    , new Callback<Uri>() {
                        @Override
                        public void onCallback(Uri data) {
                            if(data != null) {
                                Picasso.with(context).load(data).resize(200, 200).into(cosmeticIV);
                            }
                        }
                    });
        }
    }
}
