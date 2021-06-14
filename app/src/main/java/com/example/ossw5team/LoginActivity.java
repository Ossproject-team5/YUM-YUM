package com.example.ossw5team;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    EditText txtEmail,txtPW;
//    TextView txtEmail,txtPW;
    Button btnLogin, btnFindPW, btnRegister;
    String strEmail, strPW;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d("loginTest","로그인까진옴");
        //액션바 안보이게 지정
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        //파이어베이스 정보 가져오기
        mAuth = FirebaseAuth.getInstance();

        //전역변수 링크
        txtEmail = findViewById(R.id.email);
        txtPW = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnRegister = findViewById(R.id.btnRegister);

        intent = getIntent();

        txtEmail.setText(intent.getStringExtra("Email"));
        txtPW.setText(intent.getStringExtra("Password"));
        strEmail = txtEmail.getText().toString();
        strPW = txtPW.getText().toString();
        if (TextUtils.isEmpty(strEmail) != true && TextUtils.isEmpty(strPW) != true) {
            Toast.makeText(LoginActivity.this, "회원가입 완료", Toast.LENGTH_SHORT).show();
        }
        String getResetData = intent.getStringExtra("resetPW");
        if(getResetData != null){
            Toast.makeText(LoginActivity.this, "PW 재설정 E-Mail 전송을 완료했습니다.", Toast.LENGTH_SHORT).show();
        }
        getResetData = intent.getStringExtra("logout");
        if(getResetData != null){
            Toast.makeText(LoginActivity.this, "로그아웃 성공", Toast.LENGTH_SHORT).show();
        }
        txtPW.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            strEmail = txtEmail.getText().toString();
                            strPW = txtPW.getText().toString();
                            if (TextUtils.isEmpty(strEmail)) {
                                Toast.makeText(LoginActivity.this, "E-Mail 을 입력해주세요.", Toast.LENGTH_SHORT).show();
                            }else if (TextUtils.isEmpty(strPW)) {
                                Toast.makeText(LoginActivity.this, "PW를 입력해주세요.", Toast.LENGTH_SHORT).show();
                            }else if (strEmail.indexOf('@') < 0) {
                                Toast.makeText(LoginActivity.this, "E-Mail 형식이 올바르지 않습니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                            }else if (strPW.length() < 5) {
                                Toast.makeText(LoginActivity.this, "비밀번호는 5자리 이상입니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                            }else{
                                loginUser(strEmail,strPW);
                            }
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    public void loginUser(String Email, String Password){
        mAuth.signInWithEmailAndPassword(Email,Password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                    FirebaseUser user = mAuth.getCurrentUser();
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }else{
                    Toast.makeText(LoginActivity.this,"인증 실패, E-Mail/PW를 확인하세요.",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void mClick(View view) {
        switch (view.getId()){
            case R.id.btnLogin :        //로그인 버튼 누를 경우
                strEmail = txtEmail.getText().toString();
                strPW = txtPW.getText().toString();
                if (TextUtils.isEmpty(strEmail)) {
                    Toast.makeText(LoginActivity.this, "E-Mail 을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }else if (TextUtils.isEmpty(strPW)) {
                    Toast.makeText(LoginActivity.this, "PW를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }else if (strEmail.indexOf('@') < 0) {
                    Toast.makeText(LoginActivity.this, "E-Mail 형식이 올바르지 않습니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                }else if (strPW.length() < 5) {
                    Toast.makeText(LoginActivity.this, "비밀번호는 5자리 이상입니다. 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
                }else{
                    loginUser(strEmail,strPW);
                }
                break;
            case R.id.btnRegister :
                intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                break;
        }
    }
    //로그아웃 안했으면, 즉 로그인 되어있으면 자동으로 메인페이지로 이동시키기
//    @Override
//    public void onStart() {
//        super.onStart();
//        // Check if user is signed in (non-null) and update UI accordingly.
//        currentUser = mAuth.getCurrentUser();
//        if(currentUser != null){
//            intent = new Intent(LoginActivity.this, MainActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.putExtra("autoLogin", "autoLogin");
//            startActivity(intent);
//            finish();
//        }
//    }
}