package com.boomlabs.boommates;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JoinGroupActivity extends AppCompatActivity {

    private static final String TAG = "JoinGroupAct";

    private ProgressBar progressBar;
    private DatabaseReference groupList, userList;
    private Button btnCreateGroup;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);
        getSupportActionBar().setTitle(getString(R.string.join_group_title));
        user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView logo = findViewById(R.id.group_icon);
        logo.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.group));
        TextView directions = findViewById(R.id.join_group_directions);
        directions.setText(R.string.join_group_directions);
        btnCreateGroup = findViewById(R.id.btn_create_apt);
        progressBar = findViewById(R.id.progress_create_apt);
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList = FirebaseDatabase.getInstance().getReference("users");

        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCreateGroup.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                String groupID = groupList.push().getKey();
                groupList.child(groupID).child("groupAdmin").setValue(user.getEmail());
                groupList.child(groupID).child("groupRotation").setValue(0);
                groupList.child(groupID).child("groupMembers").child(user.getUid()).setValue("none");
                progressBar.setVisibility(View.INVISIBLE);
                Toast toast = Toast.makeText(JoinGroupActivity.this, getString(R.string.created_group), Toast.LENGTH_LONG);
                TextView text = toast.getView().findViewById(android.R.id.message);
                text.setGravity(Gravity.CENTER);
                toast.show();
                userList.child(user.getUid()).child("userGroup").setValue(groupID);
            }
        });

        userList.child(user.getUid()).child("userGroup").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userGroup) {
                if (!userGroup.getValue(String.class).equals("none")) {
                    Intent intent = new Intent(JoinGroupActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
        btnCreateGroup.setVisibility(View.VISIBLE);
    }
}