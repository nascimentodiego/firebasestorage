package br.com.dfn.storageexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private static String REFERECNCE = "gs://fcmexemplo.appspot.com";
    private static String REF_MEETUP = "meetup.jpg";
    private static String REF_IMAGES = "images/meetup.jpg";
    private static int REQUEST_TAKE_PICTURE = 1001;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private StorageReference imagesRef;

    private ImageView myImageView;
    private String mCurrentPhotoPath;
    private Bitmap mImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myImageView = (ImageView) findViewById(R.id.myImageView);

        ((Button) findViewById(R.id.btnTakePicture)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

    }


    private void takePicture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i(TAG, "IOException: " + ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(cameraIntent, REQUEST_TAKE_PICTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PICTURE && resultCode == RESULT_OK) {
            try {
                mImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(mCurrentPhotoPath));
                myImageView.setImageBitmap(mImageBitmap);

                //Upload Firebase Storage
                uploadImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadImage() {

        //1º First instantiate FirebaseStorage
        storage = FirebaseStorage.getInstance();

        //2º Create a storage reference from our app
        storageReference = storage.getReferenceFromUrl(REFERECNCE);

        //3º Create a reference to "meetup.jpg"
        StorageReference meetupRef = storageReference.child(REF_MEETUP);

        //4º Create a reference to images/meetup
        imagesRef = meetupRef.child(REF_IMAGES);

        // While the file names are the same, the references point to different files
        meetupRef.getName().equals(imagesRef.getName());    // true
        meetupRef.getPath().equals(imagesRef.getPath());    // false

        // Get the data from an ImageView as bytes
        myImageView.setDrawingCacheEnabled(true);
        myImageView.buildDrawingCache();
        Bitmap bitmap = myImageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = meetupRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size,
                // content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Toast.makeText(getApplicationContext(), "Download has done: " +
                        downloadUrl.getPath(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void downloadImage() {

    }
}
