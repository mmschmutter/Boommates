package com.boommates.boommates;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;


public class AdminManagerActivity extends AppCompatActivity {

    private final String TAG = "AdminManager";

    private DatabaseReference groupList, userList;
    private RecyclerView memberView;
    private FirebaseUser user;
    private RecyclerView.Adapter adapter;
    private ArrayList<String> myMembers;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_manager);
        getSupportActionBar().setTitle(getString(R.string.admin_manager_title));
        progressBar = (ProgressBar) findViewById(R.id.progress_manager);
        progressBar.setVisibility(View.VISIBLE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        myMembers = new ArrayList<>();
        initMemberView();
        updateUI();
        userList = FirebaseDatabase.getInstance().getReference("users");
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                DatabaseReference memberList = groupList.child(groupSnap.getValue(String.class)).child("groupMembers");
                memberList.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot membersSnap) {
                        if (!membersSnap.hasChildren()) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG + "Cancelled", databaseError.toString());
                    }
                });

                memberList.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Added", memberSnap.toString());
                        fetchData(memberSnap);
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Changed", memberSnap.toString());
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot memberSnap) {
                        Log.d(TAG + "Removed", memberSnap.toString());
                        removeData(memberSnap);
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

    private void fetchData(DataSnapshot memberSnap) {
        String memberEmail = memberSnap.child("userEmail").getValue(String.class);
        if (!memberEmail.equals(user.getEmail())) {
            myMembers.add(memberEmail);
        }
        updateUI();
    }

    private void removeData(DataSnapshot memberSnap) {
        String memberEmail = memberSnap.child("userEmail").getValue(String.class);
        for (int i = 0; i < myMembers.size(); i++) {
            if (myMembers.get(i).equals(memberEmail)) {
                myMembers.remove(i);
            }
        }
        updateUI();
    }

    private void updateUI() {
        adapter = new MemberManagerAdapter(myMembers);
        memberView.setAdapter(adapter);
    }

    private void initMemberView() {
        memberView = (RecyclerView) findViewById(R.id.member_recycler_view);
        memberView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        memberView.setLayoutManager(layoutManager);
        updateUI();
    }

    class MemberManagerAdapter extends RecyclerView.Adapter<MemberManagerAdapter.ViewHolder> {
        private ArrayList<String> members;
        private Context context;

        public MemberManagerAdapter(ArrayList<String> members) {
            this.members = members;
            Collections.sort(this.members);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card, viewGroup, false);
            this.context = viewGroup.getContext();
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
            viewHolder.tv_email.setText(members.get(i));
            viewHolder.button_set_admin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot userGroupSnap) {
                            groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").orderByChild("choreUser").equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot choresSnap) {
                                    if (choresSnap.exists() && choresSnap.hasChildren()) {
                                        choresSnap.getChildren().iterator().next().getRef().removeValue();
                                    }
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupAdmin").getRef().setValue(members.get(i));
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").child(user.getUid()).removeValue();
                                    userList.child(user.getUid()).child("userGroup").setValue("none");
                                    Toast toast = Toast.makeText(AdminManagerActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                                    TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                    Intent intent = new Intent(AdminManagerActivity.this, GroupChooserActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
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
            });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_email;
            private Button button_set_admin;

            public ViewHolder(View view) {
                super(view);
                tv_email = view.findViewById(R.id.card_name);
                button_set_admin = view.findViewById(R.id.card_button);
                button_set_admin.setText(R.string.set_admin);
            }
        }

    }
}