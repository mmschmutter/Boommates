package com.boommates.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GroupJoinActivity extends AppCompatActivity {

    private static final String TAG = "GJoinActivity";

    private TextView directions;
    private Button btnJoinApt;
    private ProgressBar progressBar;
    private EditText aptInputID;
    private TextInputLayout aptInputLayout;
    private DatabaseReference groupList, userList;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_join);
        getSupportActionBar().setTitle(getString(R.string.join_group_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        user = FirebaseAuth.getInstance().getCurrentUser();
        directions = (TextView) findViewById(R.id.join_directions);
        directions.setText(R.string.join_directions);
        aptInputLayout = (TextInputLayout) findViewById(R.id.apt_input);
        progressBar = (ProgressBar) findViewById(R.id.progress_join_apt);
        aptInputID = (EditText) findViewById(R.id.input_apt_id);
        btnJoinApt = (Button) findViewById(R.id.btn_join_apt);
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList = FirebaseDatabase.getInstance().getReference("users");

        btnJoinApt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    private void submitForm() {
        aptInputID.setError(null);
        aptInputLayout.setError(null);

        String aptEmail = aptInputID.getText().toString().trim().toLowerCase();

        if (!checkEmail()) {
            return;
        }
        aptInputID.setError(null);
        aptInputLayout.setError(null);
        aptInputLayout.setErrorEnabled(false);

        progressBar.setVisibility(View.VISIBLE);

        groupList.orderByChild("groupAdmin").equalTo(aptEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                if (groupSnap.hasChildren()) {
                    String groupID = groupSnap.getChildren().iterator().next().getKey();
                    groupList.child(groupID).child("groupMembers").child(user.getUid()).setValue("none");
                    userList.child(user.getUid()).child("userGroup").setValue(groupID);
                    groupList.child(groupID).child("groupChores").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot groupChoresSnap) {
                            for (DataSnapshot chore : groupChoresSnap.getChildren()) {
                                chore.getRef().child(user.getUid()).setValue(0);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG + "Cancelled", databaseError.toString());
                        }
                    });
                    Intent intent = new Intent(GroupJoinActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    progressBar.setVisibility(View.GONE);
                    aptInputLayout.setError(getString(R.string.err_msg_group_id));
                    requestFocus(aptInputID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    private boolean checkEmail() {
        String email = aptInputID.getText().toString().trim();
        if (email.isEmpty()) {
            aptInputID.setError(getString(R.string.err_msg_required));
            requestFocus(aptInputID);
            return false;
        } else if (!isEmailValid(email)) {
            aptInputLayout.setError(getString(R.string.err_msg_email));
            requestFocus(aptInputID);
            return false;
        }
        aptInputLayout.setErrorEnabled(false);
        return true;
    }

    private static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}