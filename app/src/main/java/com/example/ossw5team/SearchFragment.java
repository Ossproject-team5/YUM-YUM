package com.example.ossw5team;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static android.Manifest.permission.CALL_PHONE;

public class SearchFragment extends Fragment {

    EditText editSearch;
    String url = "https://dapi.kakao.com/v2/local/search/keyword.json";
    String query = "선문대";
    int page = 1;
    int total=0;
    ArrayList<HashMap<String, String>> array=new ArrayList<>();;
    RecyclerView list;
    SearchAdapter ad = new SearchAdapter();
    boolean is_end;
    RecyclerView listSearch;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        editSearch = view.findViewById(R.id.editSearch);
        listSearch = view.findViewById(R.id.listSearch);
        listSearch.setLayoutManager(new LinearLayoutManager(getActivity()));
        listSearch.setAdapter(ad);
        array.clear();
        new KakaoThread().execute();
        ImageView btnMore = view.findViewById(R.id.btnmore);
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!is_end){
                page = page + 1;
                new KakaoThread().execute();
                Toast.makeText(getActivity(),"검색수:" + total + "/ 페이지:" + page, Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getActivity(),"마지막페이지입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageView btnSearch = view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                query = editSearch.getText().toString();
                page = 1;
                array.clear();
                new KakaoThread().execute();
            }
        });

        editSearch.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            query = editSearch.getText().toString();
                            page = 1;
                            array.clear();
                            new KakaoThread().execute();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        return view;
    }

    //KakaoThread
    class KakaoThread extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            String s= Kakao.connect(url + "?query=" + query + "&page=" + page + "&size=5" + "&radius=20000&category_group_code=FD6&y=36.799135&x=127.074868");
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            LocalParser(s);
            ad.notifyDataSetChanged();
            listSearch.scrollToPosition(array.size()-1);
        }
    }

    //localParser
    public void LocalParser(String s) {
        try {
            JSONObject jObject = new JSONObject(s).getJSONObject("meta");
            total=jObject.getInt("total_count");
            is_end = jObject.getBoolean("is_end");
                JSONArray jarray = new JSONObject(s).getJSONArray("documents");
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject obj = jarray.getJSONObject(i);
                    HashMap<String, String> map = new HashMap<>();
                    map.put("place_name", obj.getString("place_name"));
                    map.put("category_name", obj.getString("category_name"));
                    map.put("phone", obj.getString("phone"));
                    map.put("address_name", obj.getString("address_name"));
                    map.put("x", obj.getString("x"));
                    map.put("y", obj.getString("y"));
                    map.put("place_url",obj.getString("place_url"));
                    array.add(map);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //LocalAdapter
    class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        @NonNull
        @Override
        public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, final int position) {
            final HashMap<String, String> map = array.get(position);
            holder.title.setText(Html.fromHtml(map.get("place_name")));
            holder.category.setText(map.get("category_name"));
            holder.phone.setText(map.get("phone"));
            holder.place.setText(map.get("address_name"));
            holder.phone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+map.get("phone")));
                    if(ContextCompat.checkSelfPermission(getActivity(),CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
                        startActivity(intent);
                    } else requestPermissions(new String[]{CALL_PHONE}, 1);
                }
            });
        }

        @Override
        public int getItemCount() {
            return array.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView maps,chat;
            TextView title, category, phone, place;
            RelativeLayout itemBox;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemBox = itemView.findViewById(R.id.itemBox);
                title = itemView.findViewById(R.id.title);
                category = itemView.findViewById(R.id.category);
                phone = itemView.findViewById(R.id.phone);
                place = itemView.findViewById(R.id.place);
                maps = itemView.findViewById(R.id.image);
                chat = itemView.findViewById(R.id.chat);
            }
        }
    }
}