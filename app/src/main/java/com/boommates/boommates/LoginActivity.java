package com.boommates.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private Button btnLogin, btnLinkToSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private EditText loginInputEmail, loginInputPassword;
    private TextInputLayout loginInputLayoutEmail, loginInputLayoutPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        ImageView logo = (ImageView) findViewById(R.id.login_logo);
        logo.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.temp_icon));

        loginInputLayoutEmail = (TextInputLayout) findViewById(R.id.login_input_layout_email);
        loginInputLayoutPassword = (TextInputLayout) findViewById(R.id.login_input_layout_password);
        progressBar = (ProgressBar) findViewById(R.id.progress_login);

        loginInputEmail = (EditText) findViewById(R.id.login_input_email);
        loginInputPassword = (EditText) findViewById(R.id.login_input_password);

        btnLogin = (Button) findViewById(R.id.btn_login);
        btnLinkToSignUp = (Button) findViewById(R.id.btn_link_signup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });

        btnLinkToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void submitForm() {
        loginInputEmail.setError(null);
        loginInputPassword.setError(null);
        loginInputLayoutEmail.setError(null);
        loginInputLayoutPassword.setError(null);

        String email = loginInputEmail.getText().toString().trim();
        String password = loginInputPassword.getText().toString().trim();

        if (!checkEmail()) {
            return;
        }
        if (!checkPassword()) {
            return;
        }
        loginInputEmail.setError(null);
        loginInputPassword.setError(null);
        loginInputLayoutEmail.setError(null);
        loginInputLayoutPassword.setError(null);
        loginInputLayoutEmail.setErrorEnabled(false);
        loginInputLayoutPassword.setErrorEnabled(false);

        progressBar.setVisibility(View.VISIBLE);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (!task.isSuccessful()) {
                            Toast toast = Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG);
                            TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Log.d(TAG, "Login failed: " + task.getException());
                        } else {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private boolean checkEmail() {
        String email = loginInputEmail.getText().toString().trim();
        if (email.isEmpty()) {
            loginInputEmail.setError(getString(R.string.err_msg_required));
            requestFocus(loginInputEmail);
            return false;
        } else if (!isEmailValid(email)) {
            loginInputLayoutEmail.setError(getString(R.string.err_msg_email));
            requestFocus(loginInputEmail);
            return false;
        }
        loginInputLayoutEmail.setErrorEnabled(false);
        return true;
    }

    private boolean checkPassword() {
        String password = loginInputPassword.getText().toString().trim();
        if (password.isEmpty()) {
            loginInputPassword.setError(getString(R.string.err_msg_required));
            requestFocus(loginInputPassword);
            return false;
        } else if (!isPasswordValid(password)) {
            loginInputLayoutPassword.setError(getString(R.string.err_msg_password));
            requestFocus(loginInputPassword);
            return false;
        }
        loginInputLayoutPassword.setErrorEnabled(false);
        return true;
    }

    private static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isPasswordValid(String password) {
        return (password.length() >= 6);
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}