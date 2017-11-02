package com.boomlabs.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;


public class AdminManagerActivity extends AppCompatActivity {

    private final String TAG = "AdminManAct";

    private DatabaseReference groupList, userList;
    private RecyclerView memberView;
    private FirebaseUser user;
    private ArrayList<String> members;
    private ProgressBar progressBar;
    private boolean delete;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_selector);
        getSupportActionBar().setTitle(getString(R.string.admin_manager_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        progressBar = findViewById(R.id.progress_manager);
        progressBar.setVisibility(View.VISIBLE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            delete = extras.getBoolean("delete");
        }
        members = new ArrayList<>();
        initMemberView();
        updateUI();
        userList = FirebaseDatabase.getInstance().getReference("users");
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                DatabaseReference memberList = groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers");

                memberList.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Added", memberSnap.toString());
                        String memberID = memberSnap.getKey();
                        if (!memberID.equals(user.getUid())) {
                            members.add(memberID);
                            updateUI();
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Changed", memberSnap.toString());
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot memberSnap) {
                        Log.d(TAG + "Removed", memberSnap.toString());
                        members.remove(memberSnap.getKey());
                        updateUI();
                    }

                    @Override
                    public void onChildMoved(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Moved", memberSnap.toString());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG + "Cancelled", databaseError.toString());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    private void updateUI() {
        RecyclerView.Adapter adapter = new MemberManagerAdapter(members);
        memberView.setAdapter(adapter);
    }

    private void initMemberView() {
        memberView = findViewById(R.id.list_selector_recycler_view);
        memberView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(AdminManagerActivity.this);
        memberView.setLayoutManager(layoutManager);
        updateUI();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class MemberManagerAdapter extends RecyclerView.Adapter<MemberManagerAdapter.ViewHolder> {
        private ArrayList<String> members;

        MemberManagerAdapter(ArrayList<String> members) {
            this.members = members;
            Collections.sort(this.members);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
            userList.child(members.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(final DataSnapshot userSnap) {
                    final String userEmail = userSnap.child("userEmail").getValue(String.class);
                    final String userName = userSnap.child("userName").getValue(String.class);
                    final String groupID = userSnap.child("userGroup").getValue(String.class);
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(groupID);
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(viewHolder.tv_email, 15, 35, 10, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                    viewHolder.tv_email.setText(userName);
                    viewHolder.button_set_admin.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            groupList.child(groupID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupSnap) {
                                    groupList.child(groupID).child("groupAdmin").setValue(userEmail);
                                    String userChore = groupSnap.child("groupMembers").child(user.getUid()).getValue(String.class);
                                    if (!userChore.equals("none")) {
                                        groupList.child(groupID).child("groupChores").child(userChore).removeValue();
                                    }
                                    groupSnap.getRef().child("groupMembers").child(user.getUid()).removeValue();
                                    userList.child(user.getUid()).child("userGroup").setValue("none");
                                    Iterable<DataSnapshot> groupChoresSnap = groupSnap.child("groupChores").getChildren();
                                    for (DataSnapshot chore : groupChoresSnap) {
                                        chore.child(user.getUid()).getRef().removeValue();
                                    }
                                    if (delete) {
                                        deleteUser();
                                    } else {
                                        Toast toast = Toast.makeText(AdminManagerActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                                        TextView text = toast.getView().findViewById(android.R.id.message);
                                        text.setGravity(Gravity.CENTER);
                                        toast.show();
                                        Intent intent = new Intent(AdminManagerActivity.this, JoinGroupActivity.class);
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
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG + "Cancelled", databaseError.toString());
                }
            });
        }

        private void deleteUser() {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(user.getUid());
            userList.child(user.getUid()).removeValue();
            user.delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User account deleted.");
                                Toast toast = Toast.makeText(AdminManagerActivity.this, getText(R.string.account_deleted), Toast.LENGTH_LONG);
                                TextView text = toast.getView().findViewById(android.R.id.message);
                                text.setGravity(Gravity.CENTER);
                                toast.show();
                                Intent intent = new Intent(AdminManagerActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_email;
            private Button button_set_admin;

            ViewHolder(View view) {
                super(view);
                tv_email = view.findViewById(R.id.card_name);
                button_set_admin = view.findViewById(R.id.card_button);
                button_set_admin.setText(R.string.set_admin);
            }
        }

    }
}