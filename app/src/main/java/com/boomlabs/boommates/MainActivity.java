package com.boomlabs.boommates;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DatabaseReference databaseTime, groupList, userList;
    private ProgressBar topProgressBar, bottomProgressBar;
    private RecyclerView choreView;
    private FirebaseUser user;
    private ArrayList<String> chores;
    private TextView yourChoreView;
    private TextView timerText;
    private ImageView firstX, secondX, thirdX, fourthX;
    private CountDownTimer timer;
    private RecyclerView.Adapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SpannableStringBuilder title = new SpannableStringBuilder("BOOMmates");
        ForegroundColorSpan red = new ForegroundColorSpan(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
        StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
        title.setSpan(red, 0, 4, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        title.setSpan(bold, 0, 4, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        getSupportActionBar().setTitle(title);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_main);
            topProgressBar = findViewById(R.id.progress_main_top);
            topProgressBar.setVisibility(View.VISIBLE);
            bottomProgressBar = findViewById(R.id.progress_main_bottom);
            bottomProgressBar.setVisibility(View.VISIBLE);
            databaseTime = FirebaseDatabase.getInstance().getReference("currentTime");
            groupList = FirebaseDatabase.getInstance().getReference("groups");
            userList = FirebaseDatabase.getInstance().getReference("users");
            chores = new ArrayList<>();
            rotateChores();
            userList.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot userSnap) {
                    if (userSnap.hasChild("unsubscribe")) {
                        for (DataSnapshot group : userSnap.child("unsubscribe").getChildren()) {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic(group.getKey());
                        }
                        userSnap.child("unsubscribe").getRef().removeValue();
                    }
                    String groupID = userSnap.child("userGroup").getValue(String.class);
                    if (groupID.equals("none")) {
                        Intent intent = new Intent(MainActivity.this, JoinGroupActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        FirebaseMessaging.getInstance().subscribeToTopic(groupID);
                        FirebaseMessaging.getInstance().subscribeToTopic(user.getUid());
                        userList.child(user.getUid()).child("userName").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userNameSnap) {
                                TextView header = findViewById(R.id.header);
                                header.setText("Hey " + userNameSnap.getValue(String.class) + getString(R.string.header));
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d(TAG + "Cancelled", databaseError.toString());
                            }
                        });
                        checkBoomExpirations(groupID);
                        initDashboard(groupID);
                        initChoreView(groupID);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG + "Cancelled", databaseError.toString());
                }
            });
        }
    }

    private void checkBoomExpirations(String groupID) {
        groupList.child(groupID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot groupSnap) {
                databaseTime.setValue(ServerValue.TIMESTAMP);
                databaseTime.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot currentTimeSnap) {
                        long currentTime = currentTimeSnap.getValue(Long.class);
                        Iterable<DataSnapshot> groupChoresSnap = groupSnap.child("groupChores").getChildren();
                        for (DataSnapshot chore : groupChoresSnap) {
                            long lastBoomTime = chore.child("lastBoom").getValue(Long.class);
                            if ((currentTime - lastBoomTime) >= 86400000) {
                                chore.getRef().child("boomNumber").setValue(0);
                                chore.getRef().child("lastBoom").setValue(0);
                                chore.getRef().child("gracePeriodEnd").setValue(0);
                                Iterable<DataSnapshot> groupMembersSnap = groupSnap.child("groupMembers").getChildren();
                                for (DataSnapshot member : groupMembersSnap) {
                                    chore.getRef().child(member.getKey()).setValue(0);
                                }
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

    private void initDashboard(final String groupID) {
        yourChoreView = findViewById(R.id.your_chore_banner);
        firstX = findViewById(R.id.first_x);
        secondX = findViewById(R.id.second_x);
        thirdX = findViewById(R.id.third_x);
        fourthX = findViewById(R.id.fourth_x);
        timerText = findViewById(R.id.timer);
        groupList.child(groupID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot groupSnap) {
                if (groupSnap.child("groupMembers").child(user.getUid()).exists()) {
                    final String yourChoreName = groupSnap.child("groupMembers").child(user.getUid()).getValue(String.class);
                    if (!yourChoreName.equals("none")) {
                        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(yourChoreView, 40, 50, 1, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                        yourChoreView.setPadding(0, 0, 0, 0);
                        yourChoreView.setText(yourChoreName);
                        final DataSnapshot yourChoreSnap = groupSnap.child("groupChores").child(yourChoreName);
                        if (yourChoreSnap.child("boomNumber").exists() && yourChoreSnap.child("lastBoom").exists()) {
                            final int boomNumber = yourChoreSnap.child("boomNumber").getValue(Integer.class);
                            final long lastBoomTime = yourChoreSnap.child("lastBoom").getValue(Long.class);
                            databaseTime.setValue(ServerValue.TIMESTAMP);
                            databaseTime.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot currentTimeSnap) {
                                    long currentTime = currentTimeSnap.getValue(Long.class);
                                    if (timer != null) {
                                        timer.cancel();
                                        timer = null;
                                    }
                                    long remainingTime = 86400000 - (currentTime - lastBoomTime);
                                    if (boomNumber == 0) {
                                        setNoXs();
                                    } else if (boomNumber == 1) {
                                        setOneX(remainingTime, groupID);
                                    } else if (boomNumber == 2) {
                                        long gracePeriodEndTime = groupSnap.child("groupChores").child(yourChoreName).child("gracePeriodEnd").getValue(Long.class);
                                        if (currentTime > gracePeriodEndTime) {
                                            setTwoXs(remainingTime, groupID);
                                        } else {
                                            setTwoXsGrace(gracePeriodEndTime - currentTime, groupID);
                                        }
                                    } else if (boomNumber == 3) {
                                        setThreeXs(remainingTime, groupID);
                                    } else if (boomNumber == 4) {
                                        setFourXs();
                                    }
                                    setXToasts();
                                    showDashboard();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG + "Cancelled", databaseError.toString());
                                }
                            });
                        }
                    } else {
                        setNoChore();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }

        });
    }

    private void setNoXs() {
        firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
        secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
        thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
        fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x_empty));
        timerText.setText(getText(R.string.no_booms));
    }

    private void setOneX(long remainingTime, final String groupID) {
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
                checkBoomExpirations(groupID);
            }
        }.start();
    }

    private void setTwoXsGrace(long remainingTime, final String groupID) {
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
                setTwoXs(86400000, groupID);
            }
        }.start();
    }

    private void setTwoXs(long remainingTime, final String groupID) {
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
                String combinedText = timerString + " " + getString(R.string.timer_reset);
                timerText.setText(combinedText);
            }

            public void onFinish() {
                checkBoomExpirations(groupID);
            }
        }.start();
    }

    private void setThreeXs(long remainingTime, final String groupID) {
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
                checkBoomExpirations(groupID);
            }
        }.start();
    }

    private void setFourXs() {
        firstX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
        secondX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
        thirdX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
        fourthX.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.boom_x));
        timerText.setText(R.string.exposed);
    }

    private void showDashboard() {
        firstX.setVisibility(View.VISIBLE);
        secondX.setVisibility(View.VISIBLE);
        thirdX.setVisibility(View.VISIBLE);
        fourthX.setVisibility(View.VISIBLE);
        timerText.setVisibility(View.VISIBLE);
        topProgressBar.setVisibility(View.GONE);
    }

    private void setNoChore() {
        yourChoreView.setPadding(0, 0, 0, 10);
        yourChoreView.setText(getString(R.string.no_chore));
        firstX.setVisibility(View.GONE);
        secondX.setVisibility(View.GONE);
        thirdX.setVisibility(View.GONE);
        fourthX.setVisibility(View.GONE);
        timerText.setVisibility(View.GONE);
        topProgressBar.setVisibility(View.GONE);
    }

    private void setXToasts() {
        firstX.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.first_x_explanation), Toast.LENGTH_LONG);
                TextView text = toast.getView().findViewById(android.R.id.message);
                text.setGravity(Gravity.CENTER);
                toast.show();
                return false;
            }
        });
        secondX.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.second_x_explanation), Toast.LENGTH_LONG);
                TextView text = toast.getView().findViewById(android.R.id.message);
                text.setGravity(Gravity.CENTER);
                toast.show();
                return false;
            }
        });
        thirdX.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.third_x_explanation), Toast.LENGTH_LONG);
                TextView text = toast.getView().findViewById(android.R.id.message);
                text.setGravity(Gravity.CENTER);
                toast.show();
                return false;
            }
        });
        fourthX.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Toast toast = Toast.makeText(MainActivity.this, getText(R.string.fourth_x_explanation), Toast.LENGTH_LONG);
                TextView text = toast.getView().findViewById(android.R.id.message);
                text.setGravity(Gravity.CENTER);
                toast.show();
                return false;
            }
        });
    }

    private void initChoreView(final String groupID) {
        choreView = findViewById(R.id.chore_view);
        choreView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        choreView.setLayoutManager(layoutManager);
        updateChoreView(groupID);

        DatabaseReference choreList = groupList.child(groupID).child("groupChores");

        choreList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot choresSnap) {
                if (!choresSnap.hasChildren()) {
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
            public void onChildAdded(DataSnapshot choresSnap, String s) {
                Log.d(TAG + "Added", choresSnap.toString());
                chores.add(choresSnap.getKey());
                updateChoreView(groupID);
                bottomProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onChildChanged(DataSnapshot choresSnap, String s) {
                Log.d(TAG + "Changed", choresSnap.toString());
            }

            @Override
            public void onChildRemoved(DataSnapshot choresSnap) {
                Log.d(TAG + "Removed", choresSnap.toString());
                String choreName = choresSnap.getKey();
                for (String chore : chores) {
                    if (chore.equals(choreName)) {
                        chores.remove(chore);
                        break;
                    }
                }
                updateChoreView(groupID);
            }

            @Override
            public void onChildMoved(DataSnapshot choresSnap, String s) {
                Log.d(TAG + "Moved", choresSnap.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    private void updateChoreView(final String groupID) {
        groupList.child(groupID).child("groupMembers").child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userChoreSnap) {
                final String myChore = userChoreSnap.getValue(String.class);
                groupList.child(groupID).child("groupAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot groupAdminSnap) {
                        String instructionsA = getString(R.string.instructions_a);
                        String instructionsB = getString(R.string.instructions_b);
                        if (user.getEmail().equals(groupAdminSnap.getValue(String.class))) {
                            instructionsA = getString(R.string.admin_instructions_a);
                            instructionsB = getString(R.string.admin_instructions_b);
                        }
                        ArrayList<String> fixedChores = new ArrayList<>(chores);
                        if (myChore != null && !myChore.equals("none")) {
                            fixedChores.remove(myChore);
                            if (fixedChores.isEmpty()) {
                                fixedChores.add(instructionsA);
                                fixedChores.add(instructionsB);
                                adapter = new ChoreBoomAdapter(fixedChores, true);
                            } else {
                                adapter = new ChoreBoomAdapter(fixedChores, false);
                            }
                        } else {
                            if (fixedChores.isEmpty()) {
                                fixedChores.add(instructionsA);
                                fixedChores.add(instructionsB);
                                adapter = new ChoreBoomAdapter(fixedChores, true);
                            } else {
                                adapter = new ChoreBoomAdapter(fixedChores, false);
                            }
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

    private void rotateChores() {
        databaseTime.setValue(ServerValue.TIMESTAMP);
        databaseTime.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot currentTimeSnap) {
                final long currentTime = currentTimeSnap.getValue(Long.class);
                FirebaseDatabase.getInstance().getReference("lastShift").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot lastShiftSnap) {
                        long lastShift = lastShiftSnap.getValue(Long.class);
                        if ((currentTime - lastShift) >= 604800000) {
                            lastShift += 604800000;
                            FirebaseDatabase.getInstance().getReference("lastShift").setValue(lastShift);
                            groupList.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupsSnap) {
                                    for (DataSnapshot group : groupsSnap.getChildren()) {
                                        int rotation = group.child("groupRotation").getValue(Integer.class);
                                        group.getRef().child("groupRotation").setValue(rotation + 1);
                                        Iterable<DataSnapshot> groupMembersSnap = group.child("groupMembers").getChildren();
                                        ArrayList<String> groupMembers = new ArrayList<>();
                                        for (DataSnapshot member : groupMembersSnap) {
                                            member.getRef().setValue("none");
                                            groupMembers.add(member.getKey());
                                        }
                                        Collections.rotate(groupMembers, rotation);
                                        Iterable<DataSnapshot> groupChoresSnap = group.child("groupChores").getChildren();
                                        int memberNum = 0;
                                        for (DataSnapshot chore : groupChoresSnap) {
                                            chore.getRef().child("boomNumber").setValue(0);
                                            chore.getRef().child("lastBoom").setValue(0);
                                            chore.getRef().child("gracePeriodEnd").setValue(0);
                                            groupMembersSnap = group.child("groupMembers").getChildren();
                                            for (DataSnapshot member : groupMembersSnap) {
                                                chore.getRef().child(member.getKey()).setValue(0);
                                            }
                                            chore.getRef().child("choreUser").setValue(groupMembers.get(memberNum));
                                            group.getRef().child("groupMembers").child(groupMembers.get(memberNum)).setValue(chore.getKey());
                                            memberNum++;
                                        }
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
            setAdminSettings(menu);
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return true;
    }

    void setAdminSettings(final Menu menu) {
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userGroupSnap) {
                if (!userGroupSnap.getValue(String.class).equals("none")) {
                    groupList.child(userGroupSnap.getValue(String.class)).child("groupAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot adminSnap) {
                            if (adminSnap.exists() && adminSnap.getValue(String.class).equals(user.getEmail())) {
                                menu.setGroupVisible(R.id.admin_menu_group, true);
                                menu.getItem(2).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_tasks:
                startActivity(new Intent(MainActivity.this, ChoreManagerActivity.class));
                break;
            case R.id.action_remove_members:
                startActivity(new Intent(MainActivity.this, MemberManagerActivity.class));
                break;
            case R.id.action_account_settings:
                startActivity(new Intent(MainActivity.this, AccountSettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ChoreBoomAdapter extends RecyclerView.Adapter<ChoreBoomAdapter.ViewHolder> {
        private ArrayList<String> chores;
        private boolean isEmpty;

        ChoreBoomAdapter(ArrayList<String> chores, boolean isEmpty) {
            this.isEmpty = isEmpty;
            this.chores = chores;
            Collections.sort(this.chores);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            if (isEmpty) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.empty_card, viewGroup, false);
                return new ViewHolder(view, true);

            } else {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card, viewGroup, false);
                return new ViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int i) {
            if (!isEmpty) {
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(viewHolder.tv_chore, 15, 35, 10, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                viewHolder.tv_chore.setText(chores.get(i));
                viewHolder.button_boom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        databaseTime.setValue(ServerValue.TIMESTAMP);
                        databaseTime.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(final DataSnapshot currentTimeSnap) {
                                final long currentTime = currentTimeSnap.getValue(Long.class);
                                userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(final DataSnapshot userGroupSnap) {
                                        final String groupID = userGroupSnap.getValue(String.class);
                                        groupList.child(groupID).child("groupChores").child(chores.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot choreSnap) {
                                                long lastBoomTime = choreSnap.child(user.getUid()).getValue(Long.class);
                                                long gracePeriodEndTime = choreSnap.child("gracePeriodEnd").getValue(Long.class);
                                                if ((currentTime - lastBoomTime) < 86400000) {
                                                    Toast toast = Toast.makeText(MainActivity.this, "You have already BOOMed " + chores.get(i) + " within the past 24 hours", Toast.LENGTH_SHORT);
                                                    TextView text = toast.getView().findViewById(android.R.id.message);
                                                    text.setGravity(Gravity.CENTER);
                                                    toast.show();
                                                } else if (currentTime < gracePeriodEndTime) {
                                                    Toast toast = Toast.makeText(MainActivity.this, chores.get(i) + " is within the 24 hour grace period and cannot be BOOMed", Toast.LENGTH_SHORT);
                                                    TextView text = toast.getView().findViewById(android.R.id.message);
                                                    text.setGravity(Gravity.CENTER);
                                                    toast.show();
                                                } else {
                                                    int boomNumber = choreSnap.child("boomNumber").getValue(Integer.class);
                                                    if (boomNumber == 1) {
                                                        groupList.child(groupID).child("groupChores").child(chores.get(i)).child("gracePeriodEnd").setValue(currentTime + 86400000);
                                                        groupList.child(groupID).child("groupChores").child(chores.get(i)).child("lastBoom").setValue(currentTime + 86400000);
                                                    } else {
                                                        groupList.child(groupID).child("groupChores").child(chores.get(i)).child("lastBoom").setValue(currentTime);
                                                    }
                                                    groupList.child(groupID).child("groupChores").child(chores.get(i)).child("boomNumber").setValue(boomNumber + 1);
                                                    groupList.child(groupID).child("groupChores").child(chores.get(i)).child(user.getUid()).setValue(currentTime);
                                                    Toast toast = Toast.makeText(MainActivity.this, chores.get(i) + " BOOMed", Toast.LENGTH_SHORT);
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
            } else {
                viewHolder.tv_chore.setText(chores.get(i));
            }
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
                button_boom.setText(R.string.boom);
            }

            ViewHolder(View view, boolean isEmpty) {
                super(view);
                tv_chore = view.findViewById(R.id.card_text);
            }
        }
    }
}