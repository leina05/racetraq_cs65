package edu.dartmouth.cs.racetraq;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    Button mRegisterButton;
    Button mSignInButton;
    String mRegisteredEmail;
    String mRegisteredPassword;

    String mEmailInput;
    String mPasswordInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Sign in");

        mAuth = FirebaseAuth.getInstance();


        mRegisterButton = findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (check_credentials()) {
                    mAuth.createUserWithEmailAndPassword(mEmailInput, mPasswordInput)
                            .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("CreateUser:", "Successful!");
                                        Toast.makeText(SignInActivity.this,
                                                "Successfully Registered!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.d("CreateUser:", "Unsuccessful!");
                                    }

                                }
                            });
                }
            }
        });



        mSignInButton = findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (check_credentials()) {

                    mAuth.signInWithEmailAndPassword(mEmailInput, mPasswordInput)
                            .addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(SignInActivity.this,
                                                "Authentication Failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });


    }


    private boolean check_credentials() {

        boolean failed = false;

        EditText mSignInPassword = findViewById(R.id.sign_in_password_text);
        EditText mSignInEmail = findViewById(R.id.sign_in_email_text);

        mPasswordInput = mSignInPassword.getText().toString();
        mEmailInput = mSignInEmail.getText().toString();

        if (TextUtils.isEmpty(mPasswordInput)) {
            mSignInPassword.setError(getString(R.string.error_field_required));
            failed = true;
        } else if (mPasswordInput.length() < 6) {
            mSignInPassword.setError(getString(R.string.error_password_invalid));
            failed = true;
        }

        if (TextUtils.isEmpty(mEmailInput)) {
            mSignInEmail.setError(getString(R.string.error_field_required));
            failed = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(mEmailInput).matches()) {
            mSignInEmail.setError((getString(R.string.error_email_invalid)));
            failed = true;
        }

        if (failed) {
            return false;
        } else {

        }

        return true;

    }

}
