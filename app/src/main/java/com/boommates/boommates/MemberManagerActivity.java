package com.boommates.boommates;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import java.util.Iterator;


public class MemberManagerActivity extends AppCompatActivity {

    private final String TAG = "MemberManager";

    private DatabaseReference groupList, userList;
    private RecyclerView memberView;
    private FirebaseUser user;
    private ArrayList<String> members;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_manager);
        getSupportActionBar().setTitle(getString(R.string.member_manager_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        progressBar = findViewById(R.id.progress_manager);
        progressBar.setVisibility(View.VISIBLE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        members = new ArrayList<>();
        initMemberView();
        updateUI();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setImageResource(R.drawable.plus_sign);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(MemberManagerActivity.this);
                View addMemberView = li.inflate(R.layout.dialog_add_member, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MemberManagerActivity.this);
                alertDialogBuilder.setView(addMemberView);
                final TextInputEditText userInput = addMemberView.findViewById(R.id.add_member_input);
                alertDialogBuilder
                        .setCancelable(true)
                        .setPositiveButton("ADD MEMBER", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                final String memberEmail = userInput.getText().toString().trim().toLowerCase();
                                if (isEmailValid(memberEmail)) {
                                    addMember(memberEmail);
                                } else {
                                    Toast toast = Toast.makeText(MemberManagerActivity.this, getString(R.string.err_invalid_email), Toast.LENGTH_SHORT);
                                    TextView text = toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                }
                            }
                        }).create().show();
            }
        });
        userList = FirebaseDatabase.getInstance().getReference("users");
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                DatabaseReference memberList = groupList.child(groupSnap.getValue(String.class)).child("groupMembers");
                memberList.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot membersSnap) {
                        if (membersSnap.getChildrenCount() < 2) {
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
                        String memberID = memberSnap.getKey();
                        if (!memberID.equals(user.getUid())) {
                            userList.child(memberID).child("userEmail").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot memberEmailSnap) {
                                    members.add(memberEmailSnap.getValue(String.class));
                                    updateUI();
                                    progressBar.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG + "Cancelled", databaseError.toString());
                                }
                            });
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot memberSnap, String s) {
                        Log.d(TAG + "Changed", memberSnap.toString());
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot memberSnap) {
                        Log.d(TAG + "Removed", memberSnap.toString());
                        userList.child(memberSnap.getKey()).child("userEmail").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot memberEmailSnap) {
                                String memberEmail = memberEmailSnap.getValue(String.class);
                                Iterator<String> membersIterator = members.iterator();
                                while (membersIterator.hasNext()) {
                                    if (membersIterator.next().equals(memberEmail)) {
                                        membersIterator.remove();
                                    }
                                }
                                updateUI();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d(TAG + "Cancelled", databaseError.toString());
                            }
                        });
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
        memberView = findViewById(R.id.list_manager_recycler_view);
        memberView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        memberView.setLayoutManager(layoutManager);
        updateUI();
    }

    private void addMember(final String memberEmail) {
        progressBar.setVisibility(View.VISIBLE);
        userList.orderByChild("userEmail").equalTo(memberEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot usersSnap) {
                if (usersSnap.hasChildren()) {
                    final DataSnapshot member = usersSnap.getChildren().iterator().next();
                    final String memberID = member.getKey();
                    final String memberGroup = member.child("userGroup").getValue(String.class);
                    userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userGroupSnap) {
                            String groupID = userGroupSnap.getValue(String.class);
                            if (memberGroup.equals("none")) {
                                userList.child(memberID).child("userGroup").setValue(groupID);
                                groupList.child(groupID).child("groupMembers").child(memberID).setValue("none");
                                groupList.child(groupID).child("groupChores").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot groupChoresSnap) {
                                        for (DataSnapshot chore : groupChoresSnap.getChildren()) {
                                            chore.getRef().child(memberID).setValue(0);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.d(TAG + "Cancelled", databaseError.toString());
                                    }
                                });
                                progressBar.setVisibility(View.GONE);
                                Toast toast = Toast.makeText(MemberManagerActivity.this, memberEmail + " " + getString(R.string.member_added), Toast.LENGTH_SHORT);
                                TextView text = toast.getView().findViewById(android.R.id.message);
                                text.setGravity(Gravity.CENTER);
                                toast.show();
                            } else {
                                if (groupID.equals(memberGroup)) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast toast = Toast.makeText(MemberManagerActivity.this, getString(R.string.err_duplicate_user), Toast.LENGTH_SHORT);
                                    TextView text = toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    Toast toast = Toast.makeText(MemberManagerActivity.this, getString(R.string.err_unavailable_user), Toast.LENGTH_SHORT);
                                    TextView text = toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                }

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d(TAG + "Cancelled", databaseError.toString());
                        }
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast toast = Toast.makeText(MemberManagerActivity.this, getString(R.string.err_invalid_user), Toast.LENGTH_SHORT);
                    TextView text = toast.getView().findViewById(android.R.id.message);
                    text.setGravity(Gravity.CENTER);
                    toast.show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    private static boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
            viewHolder.tv_email.setTextSize(25);
            viewHolder.tv_email.setText(members.get(i));
            viewHolder.button_remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userList.orderByChild("userEmail").equalTo(members.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot usersSnap) {
                            final DataSnapshot targetUser = usersSnap.getChildren().iterator().next();
                            final String userID = targetUser.getKey();
                            final String groupID = targetUser.child("userGroup").getValue(String.class);
                            groupList.child(groupID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupSnap) {
                                    String userChore = groupSnap.child("groupMembers").child(userID).getValue(String.class);
                                    if (!userChore.equals("none")) {
                                        groupList.child(groupID).child("groupChores").child(userChore).removeValue();
                                    }
                                    groupSnap.getRef().child("groupMembers").child(userID).removeValue();
                                    userList.child(userID).child("userGroup").setValue("none");
                                    Iterable<DataSnapshot> groupChoresSnap = groupSnap.child("groupChores").getChildren();
                                    for (DataSnapshot chore : groupChoresSnap) {
                                        chore.child(userID).getRef().removeValue();
                                    }
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

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_email;
            private Button button_remove;

            ViewHolder(View view) {
                super(view);
                tv_email = view.findViewById(R.id.card_name);
                button_remove = view.findViewById(R.id.card_button);
                button_remove.setText(R.string.remove);
            }
        }

    }
}