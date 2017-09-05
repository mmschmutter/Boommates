package com.boommates.boommates;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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


public class MemberManagerActivity extends AppCompatActivity {

    private final String TAG = "MemberManager";

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
        getSupportActionBar().setTitle(getString(R.string.member_manager_title));
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
            viewHolder.button_remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot groupSnap) {
                            groupList.child(groupSnap.getValue(String.class)).child("groupMembers").orderByChild("userEmail").equalTo(members.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot membersSnap) {
                                    membersSnap.getChildren().iterator().next().getRef().removeValue();
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

                    userList.orderByChild("userEmail").equalTo(members.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot usersSnap) {
                            usersSnap.getChildren().iterator().next().child("userGroup").getRef().setValue("none");
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
            private Button button_remove;

            public ViewHolder(View view) {
                super(view);
                tv_email = view.findViewById(R.id.card_name);
                button_remove = view.findViewById(R.id.card_button);
                button_remove.setText(R.string.remove);
            }
        }

    }
}