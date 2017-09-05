package com.boommates.boommates;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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


public class ChoreManagerActivity extends AppCompatActivity {

    private final String TAG = "ChoreManager";

    private DatabaseReference groupList, userList;
    private RecyclerView choreView;
    private FirebaseUser user;
    private RecyclerView.Adapter adapter;
    private ArrayList<String> myChores;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chore_manager);
        getSupportActionBar().setTitle(getString(R.string.chore_manager_title));
        progressBar = (ProgressBar) findViewById(R.id.progress_manager);
        progressBar.setVisibility(View.VISIBLE);
        user = FirebaseAuth.getInstance().getCurrentUser();
        myChores = new ArrayList<>();
        initChoreView();
        updateUI();
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.plus_sign);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewListItem();
            }
        });
        userList = FirebaseDatabase.getInstance().getReference("users");
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String userGroup = dataSnapshot.getValue(String.class);
                DatabaseReference choreList = groupList.child(userGroup).child("groupChores");
                choreList.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChildren()) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG + "Cancelled", databaseError.toString());
                    }
                });

                choreList.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG + "Added", dataSnapshot.toString());
                        fetchData(dataSnapshot);
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG + "Changed", dataSnapshot.toString());
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d(TAG + "Removed", dataSnapshot.toString());
                        removeData(dataSnapshot);
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG + "Moved", dataSnapshot.toString());
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

    public void createNewListItem() {
        LayoutInflater li = LayoutInflater.from(this);
        View addChoreView = li.inflate(R.layout.dialog_add_chore, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(addChoreView);

        final EditText userInput = (EditText) addChoreView.findViewById(R.id.chore_name_input);

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("ADD TASK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String choreName = userInput.getText().toString();
                        if (choreName.equals("")) {
                            return;
                        } else {
                            for (String chore : myChores) {
                                if (choreName.equals(chore)) {
                                    Toast toast = Toast.makeText(ChoreManagerActivity.this, getString(R.string.err_chore_exists), Toast.LENGTH_SHORT);
                                    TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                    return;
                                }
                            }
                            userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot userGroupSnap) {
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot groupMembersSnap) {
                                            final long numberOfMembers = groupMembersSnap.getChildrenCount();
                                            groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot groupChoresSnap) {
                                                    long numberOfChores = groupChoresSnap.getChildrenCount();
                                                    if (numberOfChores >= numberOfMembers) {
                                                        Toast toast = Toast.makeText(ChoreManagerActivity.this, getString(R.string.err_num_chores), Toast.LENGTH_LONG);
                                                        TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                        text.setGravity(Gravity.CENTER);
                                                        toast.show();
                                                    } else {
                                                        addChore(choreName);
                                                        assignChores();
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

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG + "Cancelled", databaseError.toString());
                                }
                            });
                        }
                    }
                }).create()
                .show();
    }

    private void fetchData(DataSnapshot dataSnapshot) {
        String choreName = dataSnapshot.getKey();
        myChores.add(choreName);
        updateUI();
    }

    private void removeData(DataSnapshot dataSnapshot) {
        String choreName = dataSnapshot.getKey();
        for (int i = 0; i < myChores.size(); i++) {
            if (myChores.get(i).equals(choreName)) {
                myChores.remove(i);
            }
        }
        updateUI();
    }

    private void updateUI() {
        adapter = new ChoreManagerAdapter(myChores);
        choreView.setAdapter(adapter);
    }

    private void initChoreView() {
        choreView = (RecyclerView) findViewById(R.id.chore_list_recycler_view);
        choreView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        choreView.setLayoutManager(layoutManager);
        updateUI();
    }

    public void addChore(final String choreName) {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                groupList.child(dataSnapshot.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot user : snapshot.getChildren()) {
                            groupList.child(dataSnapshot.getValue(String.class)).child("groupChores").child(choreName).child("boomNumber").setValue(0);
                            groupList.child(dataSnapshot.getValue(String.class)).child("groupChores").child(choreName).child("lastBoom").setValue(0);
                            groupList.child(dataSnapshot.getValue(String.class)).child("groupChores").child(choreName).child(user.getKey()).child("boomTime").setValue(0);
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

    private void assignChores() {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot choresSnap) {
                        for (DataSnapshot chore : choresSnap.getChildren()) {
                            if (!chore.hasChild("choreUser")) {
                                groupList.child(userGroupSnap.getValue(String.class)).child("groupRotation").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot rotationSnap) {
                                        final int rotation = rotationSnap.getValue(Integer.class);
                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot membersSnap) {
                                                final ArrayList<String> groupMembers = new ArrayList<>();
                                                for (DataSnapshot member : membersSnap.getChildren()) {
                                                    groupMembers.add(member.getKey());
                                                }
                                                Collections.rotate(groupMembers, rotation);
                                                int memberNum = 0;
                                                for (DataSnapshot chore : choresSnap.getChildren()) {
                                                    chore.getRef().child("choreUser").setValue(groupMembers.get(memberNum));
                                                    memberNum++;
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
                                return;
                            }
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

    class ChoreManagerAdapter extends RecyclerView.Adapter<ChoreManagerAdapter.ViewHolder> {
        private ArrayList<String> chores;
        private Context context;

        public ChoreManagerAdapter(ArrayList<String> chores) {
            this.chores = chores;
            Collections.sort(this.chores);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card, viewGroup, false);
            this.context = viewGroup.getContext();
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
            viewHolder.tv_chore.setText(chores.get(i));
            viewHolder.button_boom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            groupList.child(dataSnapshot.getValue(String.class)).child("groupChores").child(chores.get(i)).removeValue();
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
            return chores.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_chore;
            private Button button_boom;

            public ViewHolder(View view) {
                super(view);
                tv_chore = view.findViewById(R.id.card_name);
                button_boom = view.findViewById(R.id.card_button);
                button_boom.setText(R.string.remove);
            }
        }

    }
}