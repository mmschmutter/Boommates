package com.boommates.boommates;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AccSettAct";

    private ProgressBar progressBar;
    private DatabaseReference groupList, userList;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);
        getSupportActionBar().setTitle(getString(R.string.account_settings_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        user = FirebaseAuth.getInstance().getCurrentUser();
        groupList = FirebaseDatabase.getInstance().getReference("groups");
        userList = FirebaseDatabase.getInstance().getReference("users");
        Button btnLogout = findViewById(R.id.btn_account_logout);
        Button btnLeaveGroup = findViewById(R.id.btn_account_leave);
        Button btnChangeName = findViewById(R.id.btn_account_name);
        Button btnChangePassword = findViewById(R.id.btn_account_password);
        Button btnDeleteAccount = findViewById(R.id.btn_account_delete);
        progressBar = findViewById(R.id.progress_settings);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        btnLeaveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leaveGroup();
            }
        });
        btnChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeName();
            }
        });
        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePassword();
            }
        });
        btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAccount();
            }
        });
    }

    private void logout() {
        progressBar.setVisibility(View.VISIBLE);
        userList.child(user.getUid()).child("userToken").setValue("none");
        FirebaseAuth.getInstance().signOut();
        progressBar.setVisibility(View.INVISIBLE);
        Intent intent = new Intent(AccountSettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void leaveGroup() {
        progressBar.setVisibility(View.VISIBLE);
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                final String groupID = userGroupSnap.getValue(String.class);
                groupList.child(groupID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot groupSnap) {
                        String admin = groupSnap.child("groupAdmin").getValue(String.class);
                        DataSnapshot groupMembersSnap = groupSnap.child("groupMembers");
                        if (admin.equals(user.getEmail())) {
                            if (groupMembersSnap.getChildrenCount() == 1) {
                                groupList.child(groupID).removeValue();
                                userList.child(user.getUid()).child("userGroup").setValue("none");
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast toast = Toast.makeText(AccountSettingsActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                                TextView text = toast.getView().findViewById(android.R.id.message);
                                text.setGravity(Gravity.CENTER);
                                toast.show();
                                Intent intent = new Intent(AccountSettingsActivity.this, JoinGroupActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                Intent intent = new Intent(AccountSettingsActivity.this, AdminManagerActivity.class);
                                intent.putExtra("delete", false);
                                startActivity(intent);
                            }
                        } else {
                            String userChore = groupMembersSnap.child(user.getUid()).getValue(String.class);
                            if (!userChore.equals("none")) {
                                groupList.child(groupID).child("groupChores").child(userChore).removeValue();
                            }
                            groupMembersSnap.getRef().child(user.getUid()).removeValue();
                            userList.child(user.getUid()).child("userGroup").setValue("none");
                            Iterable<DataSnapshot> groupChoresSnap = groupSnap.child("groupChores").getChildren();
                            for (DataSnapshot chore : groupChoresSnap) {
                                chore.child(user.getUid()).getRef().removeValue();
                            }
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(AccountSettingsActivity.this, getText(R.string.left_group), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Intent intent = new Intent(AccountSettingsActivity.this, JoinGroupActivity.class);
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
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG + "Cancelled", databaseError.toString());
            }
        });
    }

    private void changeName() {
        LayoutInflater li = LayoutInflater.from(AccountSettingsActivity.this);
        View changeNameView = li.inflate(R.layout.dialog_change_name, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AccountSettingsActivity.this, R.style.AlertDialogTheme);
        alertDialogBuilder.setView(changeNameView);
        final TextInputEditText userInput = changeNameView.findViewById(R.id.change_name_input);
        TextView title = new TextView(AccountSettingsActivity.this);
        title.setText(R.string.name_alert);
        title.setPadding(0, 50, 0, 0);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(ContextCompat.getColor(AccountSettingsActivity.this, R.color.colorPrimaryDark));
        title.setTextSize(20);
        AlertDialog alert = alertDialogBuilder
                .setCancelable(true)
                .setCustomTitle(title)
                .setPositiveButton("CHANGE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String userName = userInput.getText().toString().trim();
                        if (!userName.isEmpty()) {
                            userList.child(user.getUid()).child("userName").setValue(userName);
                            Toast toast = Toast.makeText(AccountSettingsActivity.this, getString(R.string.name_change_success), Toast.LENGTH_SHORT);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                        }
                    }
                }).create();
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alert.show();
    }

    private void changePassword() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(AccountSettingsActivity.this, getString(R.string.password_change_success), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Log.d(TAG, "Email sent.");
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(AccountSettingsActivity.this, getString(R.string.password_change_fail), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                        }
                    }
                });
    }

    private void deleteAccount() {
        progressBar.setVisibility(View.VISIBLE);
        userList.child(user.getUid()).child("userGroup").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot userGroupSnap) {
                final String groupID = userGroupSnap.getValue(String.class);
                groupList.child(groupID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot groupSnap) {
                        String admin = groupSnap.child("groupAdmin").getValue(String.class);
                        DataSnapshot groupMembersSnap = groupSnap.child("groupMembers");
                        if (admin.equals(user.getEmail())) {
                            if (groupMembersSnap.getChildrenCount() == 1) {
                                groupList.child(groupID).removeValue();
                                userList.child(user.getUid()).child("userGroup").setValue("none");
                                deleteUser();
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                Intent intent = new Intent(AccountSettingsActivity.this, AdminManagerActivity.class);
                                intent.putExtra("delete", true);
                                startActivity(intent);
                            }
                        } else {
                            String userChore = groupMembersSnap.child(user.getUid()).getValue(String.class);
                            if (!userChore.equals("none")) {
                                groupList.child(groupID).child("groupChores").child(userChore).removeValue();
                            }
                            groupMembersSnap.getRef().child(user.getUid()).removeValue();
                            userList.child(user.getUid()).child("userGroup").setValue("none");
                            Iterable<DataSnapshot> groupChoresSnap = groupSnap.child("groupChores").getChildren();
                            for (DataSnapshot chore : groupChoresSnap) {
                                chore.child(user.getUid()).getRef().removeValue();
                            }
                            deleteUser();
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

    private void deleteUser() {
        userList.child(user.getUid()).removeValue();
        user.delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User account deleted.");
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast toast = Toast.makeText(AccountSettingsActivity.this, getText(R.string.account_deleted), Toast.LENGTH_LONG);
                            TextView text = toast.getView().findViewById(android.R.id.message);
                            text.setGravity(Gravity.CENTER);
                            toast.show();
                            Intent intent = new Intent(AccountSettingsActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}