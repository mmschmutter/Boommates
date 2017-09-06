package com.boommates.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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

public class GroupChooserActivity extends AppCompatActivity {

    private static final String TAG = "GroupChooser";

    private Button btnJoinGroup, btnCreateGroup;
    private ProgressBar progressBar;
    private DatabaseReference groupList, userList;

    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chooser);
        getSupportActionBar().setTitle(getString(R.string.join_group_title));
        user = FirebaseAuth.getInstance().getCurrentUser();

        TextView directions = (TextView) findViewById(R.id.chooser_directions);
        directions.setText(R.string.chooser_directions);
        btnJoinGroup = (Button) findViewById(R.id.btn_chooser_join);
        btnCreateGroup = (Button) findViewById(R.id.btn_chooser_create);
        progressBar = (ProgressBar) findViewById(R.id.progress_create_apt);
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList = FirebaseDatabase.getInstance().getReference("users");

        btnJoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GroupChooserActivity.this, GroupJoinActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                groupList.orderByChild("groupAdmin").equalTo(user.getEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChildren()) {
                            dataSnapshot.getChildren().iterator().next().getRef().removeValue();
                        }
                        String groupID = groupList.push().getKey();
                        groupList.child(groupID).child("groupAdmin").setValue(user.getEmail());
                        groupList.child(groupID).child("groupRotation").setValue(0);
                        groupList.child(groupID).child("groupMembers").child(user.getUid()).child("userEmail").setValue(user.getEmail());
                        userList.child(user.getUid()).child("userGroup").setValue(groupID);
                        Toast toast = Toast.makeText(GroupChooserActivity.this, getString(R.string.created_group), Toast.LENGTH_LONG);
                        TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                        text.setGravity(Gravity.CENTER);
                        toast.show();
                        startActivity(new Intent(GroupChooserActivity.this, MainActivity.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG + "Cancelled", databaseError.toString());
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }
}