package com.example.kante.live_alone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Posting extends AppCompatActivity {

    private final int PICK_IMAGE_REQUEST = 71;
    private static final int RC_TAKE_PICTURE = 101;
    private FirebaseUser fbUser;
    private Button postingButton;
    private TextView text_title;
    private TextView text_context;
    private String uid;
    private String email;
    private String nickname;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private DocumentReference users;
    private String imagePath;
    private Uri filePath;
    private BootstrapButton btnChoose;
    private ImageView imageView;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posting);
        mFirestore = FirebaseFirestore.getInstance();

        //For storage
        storage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
        storageReference = storage.getReference();

        btnChoose = (BootstrapButton) findViewById(R.id.btnChoose);
        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });
        imageView = (ImageView) findViewById(R.id.postImage);

        postingButton = findViewById(R.id.btn_posting);
        text_context = findViewById(R.id.text_context);
        text_title = findViewById(R.id.text_title);
        fbUser = FirebaseAuth.getInstance().getCurrentUser();
        users = mFirestore.collection("users").document(fbUser.getUid());


        postingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserInfo(fbUser);
                Log.d("GGGG","QWEQWE");
                if(imageView.getDrawable()==null){
                    writeNewPost(uid, nickname, text_title.getText().toString(), text_context.getText().toString());
                    finish();
                }else{
                    uploadImage();
                    writeNewPost(uid, nickname, text_title.getText().toString(), text_context.getText().toString());
                }
            }
        });


        Button mymenu = findViewById(R.id.gotomymenu);
        mymenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Posting.this, MyMenu.class);
                startActivity(intent);
            }
        });

        ImageButton xButton = findViewById(R.id.xButtoninPosting);
        xButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void getUserInfo(FirebaseUser user){
        uid = user.getUid();
        nickname = user.getDisplayName();
        email = user.getEmail();
    }

    private void writeNewPost(String userId, String username, String title, String body) {
        WriteBatch batch = mFirestore.batch();
        DocumentReference posts = mFirestore.collection("posts").document();
        Map<String, Object> docData = new HashMap<>();
        docData.put("user_id", userId);
        docData.put("user_name", username);
        docData.put("email", email);
        docData.put("title",title);
        docData.put("body",body);
        docData.put("category",getIntent().getStringExtra("Category"));

        if(imagePath!=null){
            docData.put("image_url",imagePath);
        }
        SimpleDateFormat s = new SimpleDateFormat("yyyyMMddkkmmss");
        String format = s.format(new Date());

        docData.put("created_at",format);
        batch.set(posts, docData);
        batch.commit();
    }

    private void launchCamera() {
        Log.d("CAMERA", "launchCamera");

        // Pick an image from storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, RC_TAKE_PICTURE);
    }
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            btnChoose.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }

    }

    private void uploadImage() {

        if(filePath != null)
        {
            final ProgressDialog progressDialog = new ProgressDialog(Posting.this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            final StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            imagePath = ref.getPath();
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(Posting.this, "Uploaded", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(Posting.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
                        }
                    });
        }
    }


}
