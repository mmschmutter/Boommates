package com.boommates.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class VerifyEmailActivity extends AppCompatActivity {

    private static final String TAG = "VerifyEmailActivity";

    private ProgressBar progressBar;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        getSupportActionBar().setTitle(getString(R.string.verify_email_title));
        user = FirebaseAuth.getInstance().getCurrentUser();

        ImageView logo = (ImageView) findViewById(R.id.email_icon);
        logo.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.mail));

        TextView directions = (TextView) findViewById(R.id.verify_directions);
        directions.setText(R.string.verify_directions);

        progressBar = (ProgressBar) findViewById(R.id.progress_signup_cancel);

        Button btnCheckVerification = (Button) findViewById(R.id.btn_check_verification);
        Button btnCancelSignup = (Button) findViewById(R.id.btn_signup_cancel);

        btnCheckVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                user.reload();
                checkVerification();
            }
        });
        btnCancelSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelSignup();
            }
        });
    }

    private void checkVerification() {
        if (user.isEmailVerified()) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast toast = Toast.makeText(VerifyEmailActivity.this, getString(R.string.account_verified), Toast.LENGTH_SHORT);
            TextView text = toast.getView().findViewById(android.R.id.message);
            text.setGravity(Gravity.CENTER);
            toast.show();
            Map<String, String> userValues = new HashMap<>();
            userValues.put("userEmail", user.getEmail());
            userValues.put("userGroup", "none");
            userValues.put("userToken", "none");
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/users/" + user.getUid(), userValues);
            FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates);
            Intent intent = new Intent(VerifyEmailActivity.this, GroupChooserActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            Toast toast = Toast.makeText(VerifyEmailActivity.this, getString(R.string.account_not_verified), Toast.LENGTH_LONG);
            TextView text = toast.getView().findViewById(android.R.id.message);
            text.setGravity(Gravity.CENTER);
            toast.show();
        }
    }

    private void cancelSignup() {
        progressBar.setVisibility(View.VISIBLE);
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(VerifyEmailActivity.this, getString(R.string.signup_cancel_fail), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                        } else {
                            Log.d(TAG, "User account deleted.");
                            progressBar.setVisibility(View.VISIBLE);
                            Toast toast = Toast.makeText(VerifyEmailActivity.this, getString(R.string.signup_cancel_success), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Intent intent = new Intent(VerifyEmailActivity.this, SignupActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
    }
}