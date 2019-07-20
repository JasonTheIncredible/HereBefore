package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class signIn extends AppCompatActivity{

    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;
    private Button btnSignIn, btnLogOut, btnGoToCreateAccount;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);
        //declare buttons and edit texts in onCreate
        mEmail = findViewById(R.id.emailAddress);
        mPassword = findViewById(R.id.password);
        btnSignIn = findViewById(R.id.signInButton);
        btnGoToCreateAccount = findViewById(R.id.goToCreateAccountButton);
        btnLogOut = findViewById(R.id.logOutButton);
        mAuth = FirebaseAuth.getInstance();

        //TODO: check if account exists. If not, make a toast message.
        //TODO: make button for creating account.
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                String email = mEmail.getText().toString().toLowerCase();
                String pass = mPassword.getText().toString();
                if(!email.equals("") && !pass.equals("")) {
                    //TODO: add onCompleteListener
                    mAuth.signInWithEmailAndPassword(email,pass);
                    toastMessage("Signing in...(unless this account doesn't exist)");
                    //TODO: move to chat once signed in.
                    //TODO: check if email is valid
                }if (email.equals("") && !pass.equals("")) {
                    toastMessage("Email required");
                    mEmail.requestFocus();
                }if (pass.equals("") && !email.equals("")){
                    toastMessage("Password required");
                    mPassword.requestFocus();
                }if (pass.equals("") && email.equals("")) {
                    toastMessage("Please enter your email and password");
                }else{
                    toastMessage("Something went wrong");
                }
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

    //TODO: needs to go to chat if user is signed in. If not signed in, needs to go to sign in page.
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

    /**
     * * customizable toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}
