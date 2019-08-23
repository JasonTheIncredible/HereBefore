package co.clixel.herebefore;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class signUp extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;

    //TODO: General code cleanup.
    //TODO: Add notes.
    //TODO: Remove listeners.

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        mEmail = findViewById(R.id.createEmailAddress);
        mPassword = findViewById(R.id.createPassword);
        Button btnCreateAccount = findViewById(R.id.createAccountButton);
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

                mAuth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()){

                            toastMessage("Signed up");
                            startActivity(new Intent(signUp.this, Chat.class));
                        }if(task.getException() != null){

                            Toast.makeText(getApplicationContext(), "User Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }else{

                            toastMessage("An unknown error occurred. Please try again.");
                        }
                    }
                });
            }
        });
    }

    private void toastMessage(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
