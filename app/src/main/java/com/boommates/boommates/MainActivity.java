package com.boommates.boommates;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DatabaseReference boommatesDB, groupList, userList;
    private ProgressBar topProgressBar, bottomProgressBar;
    private RecyclerView choreView;
    private FirebaseUser user;
    private ArrayList<String> chores;
    private TextView header, yourChore, timerText;
    private ImageView firstX, secondX, thirdX, fourthX;
    private CountDownTimer timer;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            boommatesDB = FirebaseDatabase.getInstance().getReference();
            userList = FirebaseDatabase.getInstance().getReference("users");
            groupList = FirebaseDatabase.getInstance().getReference("groups");
            userList.child(user.getUid()).child("userToken").setValue(FirebaseInstanceId.getInstance().getToken());
            userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot userGroupSnap) {
                    if (userGroupSnap.getValue(String.class).equals("none")) {
                        Intent intent = new Intent(MainActivity.this, GroupChooserActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        setContentView(R.layout.activity_main);
                        topProgressBar = (ProgressBar) findViewById(R.id.progress_main_top);
                        topProgressBar.setVisibility(View.VISIBLE);
                        bottomProgressBar = (ProgressBar) findViewById(R.id.progress_main_bottom);
                        bottomProgressBar.setVisibility(View.VISIBLE);
                        chores = new ArrayList<>();
                        rotateChores();
                        initDashboard();
                        initChoreView();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG + "Cancelled", databaseError.toString());
                }
            });
        }
    }

    private void fetchData(DataSnapshot dataSnapshot) {
        String choreName = dataSnapshot.getKey();
        chores.add(choreName);
        updateUI();
    }

    private void removeData(DataSnapshot dataSnapshot) {
        String choreName = dataSnapshot.getKey();
        for (int i = 0; i < chores.size(); i++) {
            if (chores.get(i).equals(choreName)) {
                chores.remove(i);
            }
        }
        updateUI();
    }

    private void updateUI() {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").orderByChild("choreUser").equalTo(user.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot groupChoreSnap) {
                        if (groupChoreSnap.hasChildren()) {
                            ArrayList<String> myChores = new ArrayList<>();
                            for (String chore : chores) {
                                if (!chore.equals(groupChoreSnap.getChildren().iterator().next().getKey())) {
                                    myChores.add(chore);
                                }
                            }
                            adapter = new ChoreBoomAdapter(myChores);
                        } else {
                            adapter = new ChoreBoomAdapter(chores);
                        }
                        choreView.setAdapter(adapter);
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

    private void initDashboard() {
        header = (TextView) findViewById(R.id.header);
        header.setText(R.string.header);
        yourChore = (TextView) findViewById(R.id.your_chore_banner);
        firstX = (ImageView) findViewById(R.id.first_x);
        secondX = (ImageView) findViewById(R.id.second_x);
        thirdX = (ImageView) findViewById(R.id.third_x);
        fourthX = (ImageView) findViewById(R.id.fourth_x);
        timerText = (TextView) findViewById(R.id.timer);
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").orderByChild("choreUser").equalTo(user.getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot groupChoreSnap) {
                        if (groupChoreSnap.hasChildren()) {
                            yourChore.setText(groupChoreSnap.getChildren().iterator().next().getKey());
                            groupChoreSnap.getChildren().iterator().next().getRef().child("boomNumber").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot boomNumberSnap) {
                                    groupChoreSnap.getChildren().iterator().next().getRef().child("lastBoom").addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(final DataSnapshot lastBoomSnap) {
                                            if (lastBoomSnap.exists()) {
                                                boommatesDB.child("currentTime").setValue(ServerValue.TIMESTAMP);
                                                boommatesDB.child("currentTime").addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(final DataSnapshot currentTimeSnap) {
                                                        if (timer != null) {
                                                            timer.cancel();
                                                            timer = null;
                                                        }
                                                        long remainingTime = 86400000 - (currentTimeSnap.getValue(Long.class) - lastBoomSnap.getValue(Long.class));
                                                        if (boomNumberSnap.getValue(Integer.class) == 0) {
                                                            firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            timerText.setText(getText(R.string.no_booms));
                                                        } else if (boomNumberSnap.getValue(Integer.class) == 1) {
                                                            firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            timer = new CountDownTimer(remainingTime, 1000) {
                                                                public void onTick(long millisUntilFinished) {
                                                                    long secondsInMilli = 1000;
                                                                    long minutesInMilli = secondsInMilli * 60;
                                                                    long hoursInMilli = minutesInMilli * 60;
                                                                    long elapsedHours = millisUntilFinished / hoursInMilli;
                                                                    millisUntilFinished = millisUntilFinished % hoursInMilli;
                                                                    long elapsedMinutes = millisUntilFinished / minutesInMilli;
                                                                    millisUntilFinished = millisUntilFinished % minutesInMilli;
                                                                    long elapsedSeconds = millisUntilFinished / secondsInMilli;
                                                                    String timerString = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
                                                                    String combinedText = timerString + " " + getString(R.string.timer_reset);
                                                                    timerText.setText(combinedText);
                                                                }

                                                                public void onFinish() {
                                                                    boomNumberSnap.getRef().setValue(0);
                                                                }
                                                            }.start();
                                                        } else if (boomNumberSnap.getValue(Integer.class) == 2) {
                                                            firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            timer = new CountDownTimer(remainingTime, 1000) {
                                                                public void onTick(long millisUntilFinished) {
                                                                    long secondsInMilli = 1000;
                                                                    long minutesInMilli = secondsInMilli * 60;
                                                                    long hoursInMilli = minutesInMilli * 60;
                                                                    long elapsedHours = millisUntilFinished / hoursInMilli;
                                                                    millisUntilFinished = millisUntilFinished % hoursInMilli;
                                                                    long elapsedMinutes = millisUntilFinished / minutesInMilli;
                                                                    millisUntilFinished = millisUntilFinished % minutesInMilli;
                                                                    long elapsedSeconds = millisUntilFinished / secondsInMilli;
                                                                    String timerString = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
                                                                    String combinedText = timerString + " " + getString(R.string.timer_vulnerable);
                                                                    timerText.setText(combinedText);
                                                                }

                                                                public void onFinish() {
                                                                    timer.cancel();
                                                                    timer = null;
                                                                    timer = new CountDownTimer(86400000, 1000) {
                                                                        public void onTick(long millisUntilFinished) {
                                                                            long secondsInMilli = 1000;
                                                                            long minutesInMilli = secondsInMilli * 60;
                                                                            long hoursInMilli = minutesInMilli * 60;
                                                                            long elapsedHours = millisUntilFinished / hoursInMilli;
                                                                            millisUntilFinished = millisUntilFinished % hoursInMilli;
                                                                            long elapsedMinutes = millisUntilFinished / minutesInMilli;
                                                                            millisUntilFinished = millisUntilFinished % minutesInMilli;
                                                                            long elapsedSeconds = millisUntilFinished / secondsInMilli;
                                                                            String timerString = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
                                                                            String combinedText = timerString + " " + getString(R.string.timer_reset);
                                                                            timerText.setText(combinedText);
                                                                        }

                                                                        public void onFinish() {
                                                                            boomNumberSnap.getRef().setValue(0);
                                                                        }
                                                                    }.start();
                                                                }
                                                            }.start();
                                                        } else if (boomNumberSnap.getValue(Integer.class) == 3) {
                                                            firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
                                                            fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
                                                            timer = new CountDownTimer(remainingTime, 1000) {
                                                                public void onTick(long millisUntilFinished) {
                                                                    long secondsInMilli = 1000;
                                                                    long minutesInMilli = secondsInMilli * 60;
                                                                    long hoursInMilli = minutesInMilli * 60;
                                                                    long elapsedHours = millisUntilFinished / hoursInMilli;
                                                                    millisUntilFinished = millisUntilFinished % hoursInMilli;
                                                                    long elapsedMinutes = millisUntilFinished / minutesInMilli;
                                                                    millisUntilFinished = millisUntilFinished % minutesInMilli;
                                                                    long elapsedSeconds = millisUntilFinished / secondsInMilli;
                                                                    String timerString = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
                                                                    String combinedText = timerString + " " + getString(R.string.timer_reset);
                                                                    timerText.setText(combinedText);
                                                                }

                                                                public void onFinish() {
                                                                    boomNumberSnap.getRef().setValue(0);
                                                                }
                                                            }.start();
                                                        } else if (boomNumberSnap.getValue(Integer.class) == 4) {
                                                            boomNumberSnap.getRef().setValue(0);
                                                        }
                                                        firstX.setOnTouchListener(new View.OnTouchListener() {
                                                            @Override
                                                            public boolean onTouch(View v, MotionEvent event) {
                                                                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.first_x_explanation), Toast.LENGTH_LONG);
                                                                TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                                text.setGravity(Gravity.CENTER);
                                                                toast.show();
                                                                return false;
                                                            }
                                                        });
                                                        secondX.setOnTouchListener(new View.OnTouchListener() {
                                                            @Override
                                                            public boolean onTouch(View v, MotionEvent event) {
                                                                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.second_x_explanation), Toast.LENGTH_LONG);
                                                                TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                                text.setGravity(Gravity.CENTER);
                                                                toast.show();
                                                                return false;
                                                            }
                                                        });
                                                        thirdX.setOnTouchListener(new View.OnTouchListener() {
                                                            @Override
                                                            public boolean onTouch(View v, MotionEvent event) {
                                                                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.third_x_explanation), Toast.LENGTH_LONG);
                                                                TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                                text.setGravity(Gravity.CENTER);
                                                                toast.show();
                                                                return false;
                                                            }
                                                        });
                                                        fourthX.setOnTouchListener(new View.OnTouchListener() {
                                                            @Override
                                                            public boolean onTouch(View v, MotionEvent event) {
                                                                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.fourth_x_explanation), Toast.LENGTH_LONG);
                                                                TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                                text.setGravity(Gravity.CENTER);
                                                                toast.show();
                                                                return false;
                                                            }
                                                        });
                                                        firstX.setVisibility(View.VISIBLE);
                                                        secondX.setVisibility(View.VISIBLE);
                                                        thirdX.setVisibility(View.VISIBLE);
                                                        fourthX.setVisibility(View.VISIBLE);
                                                        timerText.setVisibility(View.VISIBLE);
                                                        topProgressBar.setVisibility(View.GONE);
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        Log.d(TAG + "Cancelled", databaseError.toString());
                                                    }
                                                });
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
                        } else {
                            yourChore.setText(getString(R.string.no_chore));
                            firstX.setVisibility(View.GONE);
                            secondX.setVisibility(View.GONE);
                            thirdX.setVisibility(View.GONE);
                            fourthX.setVisibility(View.GONE);
                            timerText.setVisibility(View.GONE);
                            topProgressBar.setVisibility(View.GONE);
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

    private void initChoreView() {
        choreView = (RecyclerView) findViewById(R.id.chore_view);
        choreView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getApplicationContext());
        choreView.setLayoutManager(layoutManager);
        updateUI();
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userGroupSnap) {
                DatabaseReference choreList = groupList.child(userGroupSnap.getValue(String.class)).child("groupChores");
                choreList.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChildren()) {
                            bottomProgressBar.setVisibility(View.GONE);
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
                        bottomProgressBar.setVisibility(View.GONE);
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

    private void rotateChores() {
        boommatesDB.child("currentTime").setValue(ServerValue.TIMESTAMP);
        boommatesDB.child("currentTime").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot currentTimeSnap) {
                final long currentTime = currentTimeSnap.getValue(Long.class);
                boommatesDB.child("lastShift").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot lastShiftSnap) {
                        long lastShift = lastShiftSnap.getValue(Long.class);
                        if ((currentTime - lastShift) >= 604800000) {
                            boommatesDB.child("lastShift").setValue(lastShift += 604800000);
                            groupList.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupsSnap) {
                                    for (final DataSnapshot group : groupsSnap.getChildren()) {
                                        group.getRef().child("groupRotation").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot rotationSnap) {
                                                final int rotation = rotationSnap.getValue(Integer.class) + 1;
                                                group.getRef().child("groupRotation").setValue(rotation);
                                                group.getRef().child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot membersSnap) {
                                                        final ArrayList<String> groupMembers = new ArrayList<>();
                                                        for (DataSnapshot member : membersSnap.getChildren()) {
                                                            groupMembers.add(member.getKey());
                                                        }
                                                        Collections.rotate(groupMembers, rotation);
                                                        group.getRef().child("groupChores").addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot choresSnap) {
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
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.d(TAG + "Cancelled", databaseError.toString());
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG + "Cancelled", databaseError.toString());
                                }
                            });
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (user != null) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            setAdminSettings(menu);
        }
        return true;
    }

    void setAdminSettings(final Menu menu) {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot adminSnap) {
                        if (adminSnap.exists() && !adminSnap.getValue(String.class).equals(user.getEmail())) {
                            menu.setGroupVisible(R.id.admin_menu_group, false);
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

    void leaveGroup() {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                groupList.child(userGroupSnap.getValue(String.class)).child("groupAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot adminSnap) {
                        if (adminSnap.exists() && adminSnap.getValue(String.class).equals(user.getEmail())) {
                            groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupMembersSnap) {
                                    if (groupMembersSnap.getChildrenCount() == 1) {
                                        groupList.child(userGroupSnap.getValue(String.class)).removeValue();
                                        userList.child(user.getUid()).child("userGroup").setValue("none");
                                        Toast toast = Toast.makeText(MainActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                                        TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                        text.setGravity(Gravity.CENTER);
                                        toast.show();
                                        Intent intent = new Intent(MainActivity.this, GroupChooserActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        startActivity(new Intent(MainActivity.this, AdminManagerActivity.class));
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG + "Cancelled", databaseError.toString());
                                }
                            });
                        } else {
                            groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").orderByChild("choreUser").equalTo(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot choresSnap) {
                                    if (choresSnap.exists() && choresSnap.hasChildren()) {
                                        choresSnap.getChildren().iterator().next().getRef().removeValue();
                                    }
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupMembers").child(user.getUid()).removeValue();
                                    userList.child(user.getUid()).child("userGroup").setValue("none");
                                    Toast toast = Toast.makeText(MainActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                                    TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                    text.setGravity(Gravity.CENTER);
                                    toast.show();
                                    Intent intent = new Intent(MainActivity.this, GroupChooserActivity.class);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_tasks:
                startActivity(new Intent(MainActivity.this, ChoreManagerActivity.class));
                break;
            case R.id.action_remove_members:
                startActivity(new Intent(MainActivity.this, MemberManagerActivity.class));
                break;
            case R.id.action_leave_group:
                leaveGroup();
                break;
            case R.id.action_logout:
                userList.child(user.getUid()).child("userToken").setValue("none");
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ChoreBoomAdapter extends RecyclerView.Adapter<ChoreBoomAdapter.ViewHolder> {
        private ArrayList<String> chores;
        private Context context;

        public ChoreBoomAdapter(ArrayList<String> chores) {
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
                    boommatesDB.child("currentTime").setValue(ServerValue.TIMESTAMP);
                    boommatesDB.child("currentTime").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot currentTimeSnap) {
                            final long currentTime = currentTimeSnap.getValue(Long.class);
                            userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot userGroupSnap) {
                                    groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).child(user.getUid()).child("boomTime").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot lastBoomTimeSnap) {
                                            long lastBoomTime = lastBoomTimeSnap.getValue(Long.class);
                                            if ((currentTime - lastBoomTime) >= 86400000) {
                                                groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).child("boomNumber").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot boomNumberSnap) {
                                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).child("boomNumber").setValue(boomNumberSnap.getValue(Integer.class) + 1);
                                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).child("lastBoom").setValue(ServerValue.TIMESTAMP);
                                                        groupList.child(userGroupSnap.getValue(String.class)).child("groupChores").child(chores.get(i)).child(user.getUid()).child("boomTime").setValue(ServerValue.TIMESTAMP);
                                                        Toast toast = Toast.makeText(MainActivity.this, chores.get(i) + " BOOMed", Toast.LENGTH_SHORT);
                                                        TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
                                                        text.setGravity(Gravity.CENTER);
                                                        toast.show();
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        Log.d(TAG + "Cancelled", databaseError.toString());
                                                    }
                                                });
                                            } else {
                                                Toast toast = Toast.makeText(MainActivity.this, "You have already BOOMed " + chores.get(i) + " within the past 24 hours", Toast.LENGTH_SHORT);
                                                TextView text = (TextView) toast.getView().findViewById(android.R.id.message);
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

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_chore;
            private Button button_boom;

            public ViewHolder(View view) {
                super(view);
                tv_chore = view.findViewById(R.id.card_name);
                button_boom = view.findViewById(R.id.card_button);
                button_boom.setText(R.string.boom);
            }
        }
    }
}