package com.example.ossw5team;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserInfo extends AppCompatActivity {
    private FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    //현재 로그인 된 유저 정보를 담을 변수
    private FirebaseUser currentUser;
    String userEmail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();
        userEmail = currentUser.getEmail();
        System.out.println(userEmail);
        //String name = currentUser.fStoreEmail;
        //String userEmail = currentUser.userEmail;
        TextView textView = (TextView) findViewById(R.id.nickname) ;
        textView.setText("민송");
        TextView textView2 = (TextView) findViewById(R.id.email) ;
        textView2.setText("min@gmail.com");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
