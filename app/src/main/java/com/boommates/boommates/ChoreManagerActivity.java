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
import java.util.HashMap;
import java.util.Map;


public class ChoreManagerActivity extends AppCompatActivity {

    private final String TAG = "ChoreManager";

    private DatabaseReference groupList, userList;
    private RecyclerView choreView;
    private FirebaseUser user;
    private RecyclerView.Adapter adapter;
    private ArrayList<String> chores;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chore_manager);
        getSupportActionBar().setTitle(getString(R.string.chore_manager_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        progressBar = (ProgressBar) findViewById(R.id.progress_manager);
        progressBar.setVisibility(View.VISIBLE);
        Toast toast = Toast.makeText(ChoreManagerActivity.this, getText(R.string.edit_tasks_warning), Toast.LENGTH_LONG);
        TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
        text.setGravity(Gravity.CENTER);
        toast.show();
        user = FirebaseAuth.getInstance().getCurrentUser();
        chores = new ArrayList<>();
        initChoreView();
        updateUI();
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.plus_sign);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                            createNewListItem();
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
        });
        userList = FirebaseDatabase.getInstance().getReference("users");
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userGroupSnap) {
                String userGroup = userGroupSnap.getValue(String.class);
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
                        chores.add(dataSnapshot.getKey());
                        updateUI();
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        Log.d(TAG + "Changed", dataSnapshot.toString());
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d(TAG + "Removed", dataSnapshot.toString());
                        String choreName = dataSnapshot.getKey();
                        for (String chore : chores) {
                            if (chore.equals(choreName)) {
                                chores.remove(chore);
                                break;
                            }
                        }
                        updateUI();
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
                            for (String chore : chores) {
                                if (choreName.equals(chore)) {
                                    Toast toast = Toast.makeText(ChoreManagerActivity.this, getString(R.string.err_chore_exists), Toast.LENGTH_SHORT);
                                    TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                    return;
                                }
                            }
                            resetBooms();
                            addChore(choreName);
                        }
                    }
                }).create()
                .show();
    }

    private void updateUI() {
        adapter = new ChoreManagerAdapter(chores);
        choreView.setAdapter(adapter);
    }

    private void initChoreView() {
        choreView = (RecyclerView) findViewById(R.id.chore_list_recycler_view);
        choreView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        choreView.setLayoutManager(layoutManager);
        updateUI();
    }

    private void addChore(final String choreName) {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot groupMembersSnap) {
                        groupMembersSnap.getRef().orderByValue().equalTo("none").limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot chorelessMemberSnap) {
                                String chorelessMember = chorelessMemberSnap.getChildren().iterator().next().getKey();
                                Map<String, Object> choreValues = new HashMap<>();
                                for (DataSnapshot member : groupMembersSnap.getChildren()) {
                                    choreValues.put(member.getKey(), 0);
                                }
                                choreValues.put("boomNumber", 0);
                                choreValues.put("lastBoom", 0);
                                choreValues.put("gracePeriodEnd", 0);
                                choreValues.put("choreUser", chorelessMember);
                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put("/groups/" + userGroupSnap.getValue(String.class) + "/groupChores/" + choreName, choreValues);
                                FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates);
                                groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").child(chorelessMember).setValue(choreName);
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

    private void resetBooms() {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot groupMembersSnap) {
                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot groupChoresSnap) {
                                for (DataSnapshot chore : groupChoresSnap.getChildren()) {
                                    for (DataSnapshot member : groupMembersSnap.getChildren()) {
                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chore.getKey()).child("boomNumber").setValue(0);
                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chore.getKey()).child("lastBoom").setValue(0);
                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chore.getKey()).child("gracePeriodEnd").setValue(0);
                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chore.getKey()).child(member.getKey()).setValue(0);
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

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class ChoreManagerAdapter extends RecyclerView.Adapter<ChoreManagerAdapter.ViewHolder> {
        private ArrayList<String> chores;
        private Context context;

        ChoreManagerAdapter(ArrayList<String> chores) {
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
                        public void onDataChange(final DataSnapshot userGroupSnap) {
                            groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").orderByValue().equalTo(chores.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot choreUserSnap) {
                                    choreUserSnap.getChildren().iterator().next().getRef().setValue("none");
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).removeValue();
                                    resetBooms();
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
            return chores.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_chore;
            private Button button_boom;

            ViewHolder(View view) {
                super(view);
                tv_chore = view.findViewById(R.id.card_name);
                button_boom = view.findViewById(R.id.card_button);
                button_boom.setText(R.string.remove);
            }
        }

    }
}