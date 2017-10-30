package com.boommates.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private Button btnSignUp;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private EditText signupInputEmail, signupInputPassword, signupInputConfirmPassword, signupInputName;
    private TextInputLayout signupInputLayoutEmail, signupInputLayoutPassword, signupInputLayoutConfirmPassword, signupInputLayoutName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getSupportActionBar().setTitle(getString(R.string.signup_title));
        auth = FirebaseAuth.getInstance();

        TextView directions = findViewById(R.id.signup_directions);
        directions.setText(R.string.create_account_directions);
        signupInputLayoutName = findViewById(R.id.signup_input_layout_name);
        signupInputLayoutEmail = findViewById(R.id.signup_input_layout_email);
        signupInputLayoutPassword = findViewById(R.id.signup_input_layout_password);
        signupInputLayoutConfirmPassword = findViewById(R.id.signup_input_layout_confirm_password);
        progressBar = findViewById(R.id.progress_signup);

        signupInputName = findViewById(R.id.signup_input_name);
        signupInputEmail = findViewById(R.id.signup_input_email);
        signupInputPassword = findViewById(R.id.signup_input_password);
        signupInputConfirmPassword = findViewById(R.id.signup_input_confirm_password);

        btnSignUp = findViewById(R.id.btn_signup);
        Button btnLinkToLogIn = findViewById(R.id.btn_link_login);


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();

            }
        });

        btnLinkToLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void submitForm() {
        signupInputEmail.setError(null);
        signupInputPassword.setError(null);
        signupInputConfirmPassword.setError(null);
        signupInputLayoutEmail.setError(null);
        signupInputLayoutPassword.setError(null);
        signupInputLayoutConfirmPassword.setError(null);
        signupInputLayoutEmail.setErrorEnabled(false);
        signupInputLayoutPassword.setErrorEnabled(false);

        final String name = signupInputName.getText().toString().trim();
        String email = signupInputEmail.getText().toString().trim().toLowerCase();
        String password = signupInputPassword.getText().toString().trim();

        if (!checkName()) {
            return;
        }
        if (!checkEmail()) {
            return;
        }
        if (!checkPassword()) {
            return;
        }

        btnSignUp.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                        if (!task.isSuccessful()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            btnSignUp.setVisibility(View.VISIBLE);
                            Toast toast = Toast.makeText(SignupActivity.this, getString(R.string.failed_registration), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                        } else {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            Map<String, String> userValues = new HashMap<>();
                            userValues.put("userName", name);
                            userValues.put("userEmail", user.getEmail());
                            userValues.put("userGroup", "none");
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/users/" + user.getUid(), userValues);
                            FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates);
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(SignupActivity.this, getString(R.string.successful_registration), Toast.LENGTH_SHORT);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Intent intent = new Intent(SignupActivity.this, JoinGroupActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private boolean checkName() {
        String name = signupInputName.getText().toString().trim();
        if (name.isEmpty()) {
            signupInputName.setError(getString(R.string.err_msg_required));
            requestFocus(signupInputName);
            return false;
        }
        return true;
    }

    private boolean checkEmail() {
        String email = signupInputEmail.getText().toString().trim();
        if (email.isEmpty()) {
            signupInputEmail.setError(getString(R.string.err_msg_required));
            requestFocus(signupInputEmail);
            return false;
        } else if (!isEmailValid(email)) {
            signupInputLayoutEmail.setError(getString(R.string.err_msg_email));
            requestFocus(signupInputEmail);
            return false;
        }
        signupInputLayoutEmail.setErrorEnabled(false);
        return true;
    }

    private boolean checkPassword() {
        String password = signupInputPassword.getText().toString().trim();
        String confirmPassword = signupInputConfirmPassword.getText().toString().trim();
        if (password.isEmpty()) {
            signupInputPassword.setError(getString(R.string.err_msg_required));
            requestFocus(signupInputPassword);
            return false;
        } else if (!isPasswordValid(password)) {
            signupInputLayoutPassword.setError(getString(R.string.err_msg_password));
            requestFocus(signupInputPassword);
            return false;
        } else if (confirmPassword.isEmpty()) {
            signupInputConfirmPassword.setError(getString(R.string.err_msg_required));
            requestFocus(signupInputPassword);
            return false;
        } else if (!password.equals(confirmPassword)) {
            signupInputLayoutConfirmPassword.setError(getString(R.string.err_msg_password_match));
            requestFocus(signupInputPassword);
            return false;
        }
        signupInputLayoutPassword.setErrorEnabled(false);
        return true;
    }

    private static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isPasswordValid(String password) {
        boolean lengthFlag = false;
        boolean numberFlag = false;
        boolean letterFlag = false;
        if (password.length() > 5) {
            lengthFlag = true;
        }
        char c;
        for (int i = 0; i < password.length(); i++) {
            c = password.charAt(i);
            if (Character.isDigit(c)) {
                numberFlag = true;
            } else if (Character.isLetter(c)) {
                letterFlag = true;
            }
        }
        return lengthFlag && numberFlag && letterFlag;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
        btnSignUp.setVisibility(View.VISIBLE);
    }
}