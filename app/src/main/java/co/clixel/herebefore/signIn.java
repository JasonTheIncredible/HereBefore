package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class signIn extends AppCompatActivity{

    //TODO: Sign in with Google account.

    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        //declare buttons and edit texts in onCreate
        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        Button btnSignIn = findViewById(R.id.signInButton);
        Button btnGoToCreateAccount = findViewById(R.id.goToCreateAccountButton);
        Button btnLogOut = findViewById(R.id.logOutButton);
        mAuth = FirebaseAuth.getInstance();

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String email = mEmail.getText().toString().toLowerCase();
                String pass = mPassword.getText().toString();
                if (email.equals("") && !pass.equals("")) {
                    toastMessage("Email address required");
                    mEmail.requestFocus();
                    return;
                }if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    toastMessage("Please enter a valid email address");
                    mEmail.requestFocus();
                    return;
                }if (pass.equals("") && !email.equals("")) {
                    toastMessage("Password required");
                    mPassword.requestFocus();
                    return;
                }if (pass.length()<6){
                    toastMessage("Password must be at least 6 characters long");
                    mPassword.requestFocus();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            toastMessage("Signed in");
                            startActivity(new Intent(signIn.this, Chat.class));
                        }if(task.getException() != null){
                            Toast.makeText(getApplicationContext(), "User Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }else{
                            toastMessage("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });

        btnGoToCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                startActivity(new Intent(signIn.this, signUp.class));
            }
        });

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                mAuth.signOut();
                toastMessage("Signing out...");
                finish();
                startActivity(getIntent());
            }
        });
    }

    protected void updateUI(FirebaseUser currentUser) {
        if (currentUser != null) {
            findViewById(R.id.signInButton).setVisibility(View.GONE);
            findViewById(R.id.logOutButton).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.signInButton).setVisibility(View.VISIBLE);
            findViewById(R.id.goToCreateAccountButton).setVisibility(View.VISIBLE);
            findViewById(R.id.logOutButton).setVisibility(View.GONE);
        }}

    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}
