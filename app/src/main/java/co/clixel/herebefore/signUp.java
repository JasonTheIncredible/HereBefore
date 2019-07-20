package co.clixel.herebefore;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class signUp extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;
    private Button btnCreateAccount;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        //declare buttons and edit texts in onCreate
        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        btnCreateAccount = findViewById(R.id.createAccountButton);
        mAuth = FirebaseAuth.getInstance();

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                //TODO: Add onCompleteListener
                mAuth.createUserWithEmailAndPassword(email,pass);
                toastMessage("Account created");
            }
        });
    }

    /**
     * * customizable toast
     *
     * @param message
     */
    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
