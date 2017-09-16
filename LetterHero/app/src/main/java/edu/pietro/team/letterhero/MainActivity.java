package edu.pietro.team.letterhero;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.pietro.team.letterhero.entities.Document;
import edu.pietro.team.letterhero.event.FeedFilterClicked;
import edu.pietro.team.letterhero.event.OnDocumentProcessed;
import edu.pietro.team.letterhero.event.OnErrorDuringDetectionPostProcessing;
import edu.pietro.team.letterhero.event.OnImageCaptureRequested;
import edu.pietro.team.letterhero.event.OnStartDetectionPostProcessing;
import edu.pietro.team.letterhero.event.OnStopMessage;
import edu.pietro.team.letterhero.helper.DownloadImageTask;
import edu.pietro.team.letterhero.helper.ProcessingState;
import edu.pietro.team.letterhero.helper.Utils;
import edu.pietro.team.letterhero.social.MoneyTransfer;
import edu.pietro.team.letterhero.social.User;
import edu.pietro.team.letterhero.vision.CameraSourcePreview;
import edu.pietro.team.letterhero.vision.ImageFetchingDetector;
import edu.pietro.team.letterhero.vision.OcrDetectionProcessor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "MainActivity";

    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final int RC_HANDLE_STORAGE_PERM = 3;

    private CameraSource mCameraSource;

    private Boolean mFeedFilterIsPublic = true;

    private CameraSourcePreview mPreview;

    private CollectionPagerAdapter mCollectionPagerAdapter;

    private TextRecognizer mTextRecognizer;

    private ViewPager mViewPager;

    private Object mProcessingLock = new Object();
    private ProcessingState mProcessingState = ProcessingState.NOLOCK;

    private static MainActivity currentActivity = null;

    private Document currentDoc = null;

    public static MainActivity getCurrentActivity() {
        return currentActivity;
    }

    private static String[] BT_CONTEXT_DEVICES = new String[] {
            "04:59:06:09:52:06",
            "f8:e0:79:4c:ea:e6",
            "b4:ce:f6:22:d7:16",
            "78:02:f8:e7:96:ae"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_STORAGE_PERM);*/

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        mCollectionPagerAdapter =
                new CollectionPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCollectionPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setCurrentItem(0);
        mViewPager.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return true;
            }
        });
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        }
    }

    /**
     * Creates and starts the camera.
     */
    private void createCameraSource() {
        Context ctx = getApplicationContext();
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(displaySize);

        // dummy detector saving the last frame in order to send it to Microsoft in case of face detection
        ImageFetchingDetector imageFetchingDetector = new ImageFetchingDetector();

        mTextRecognizer = new TextRecognizer.Builder(ctx).build();
        mTextRecognizer.setProcessor(new OcrDetectionProcessor());
        // TODO: Check if the TextRecognizer is operational.

        MultiDetector multiDetector = new MultiDetector.Builder()
                .add(imageFetchingDetector)
                .add(mTextRecognizer)
                .build();

        mCameraSource = new CameraSource.Builder(ctx, multiDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(5.0f)
                .setRequestedPreviewSize(displaySize.y, displaySize.x)
                .build();
    }

    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services unavailable.");
            return;
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.friend_feed, menu);
        if (menu.size() > 0) {
            menu.getItem(0).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorite:
                String newTitle;
                if (mFeedFilterIsPublic) {
                    newTitle = "Show friends purchases";
                } else {
                    newTitle = "Show own purchases";
                }
                mFeedFilterIsPublic ^= true;
                EventBus.getDefault().post(new FeedFilterClicked(!mFeedFilterIsPublic));
                item.setTitle(newTitle);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
        currentActivity = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("Back", "I'll be back");
    }

    @Subscribe
    public void showConfirmation(OnDocumentProcessed e) {
        final Document document = e.getDocument();
        final ProcessingState assumedProcessingState = e.getAssumedProcessingState();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mProcessingLock) {
                    if (mViewPager.getCurrentItem() == 0
                            && (mProcessingState == assumedProcessingState || mProcessingState == ProcessingState.NOLOCK)) {

                        View view = mCollectionPagerAdapter.getItem(1).getView();
                        populateConfirmationView(view, document);

                        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(150);
                        EventBus.getDefault().post(new OnStopMessage());

                        mViewPager.setCurrentItem(1);
                        mProcessingState = ProcessingState.NOLOCK;
                    }
                }
            }
        });
    }

    @Subscribe
    public void onMessageEvent(OnImageCaptureRequested e) {

        Log.d("EVENT_BUS", "Image capture requested.");

        /*if (!onTryStartProcessing(ProcessingState.OBJECT_LOCK)){
            return;
        }*/

        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onPictureTaken(final byte[] bytes) {
                EventBus.getDefault().post(new OnStartDetectionPostProcessing("Processing document..."));

                // Need to send data not on rendering thread.
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            Log.d("Image resolution", bitmap.getWidth() + " x " + bitmap.getHeight());

                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

                            StringBuilder detectedTextBuilder = new StringBuilder(); ;
                            SparseArray<TextBlock> detectedTextRaw = mTextRecognizer.detect(frame);
                            for (int i = 0; i < detectedTextRaw.size(); ++i) {
                                TextBlock item = detectedTextRaw.valueAt(i);
                                List components = item.getComponents();
                                for (int j = 0; j < components.size(); ++j) {
                                    if (components.get(j) instanceof Line) {
                                        Line l = (Line) components.get(j);
                                        detectedTextBuilder.append(l.getValue());
                                        detectedTextBuilder.append(" ");
                                    }
                                }
                            }

                            // add unique id to both send operations
                            String id = UUID.randomUUID().toString();

                            //sendImage(bitmap, id);
                            JSONObject response = sendText(detectedTextBuilder.toString(), id);

                            System.out.println("json received: " + response.toString());
                            Document doc = Document.fromJSON(response);
                            doc.setBitmap(bitmap);

                            EventBus.getDefault().post(new OnDocumentProcessed(
                                    doc,
                                    ProcessingState.OBJECT_LOCK
                            ));

                        } catch (Exception x) {
                            EventBus.getDefault().post(new OnErrorDuringDetectionPostProcessing("No internet connection"));

                            x.printStackTrace();
                        } finally {
                            onStopProcessing(ProcessingState.OBJECT_LOCK);
                        }
                    }
                });
                thread.start();
            }

        });
    }

    private void populateConfirmationView(View v, Document d) {
        this.currentDoc = d;

        Map<String, String> context = d.getContext();
        Bitmap bitmapImage = d.getBitmap();

        EditText companyEdit = (EditText) v.findViewById(R.id.company);
        TextView category = (TextView) v.findViewById(R.id.category);
        EditText dateEdit = (EditText) v.findViewById(R.id.date);
        EditText typeEdit = (EditText) v.findViewById(R.id.type);
        EditText contextEdit = (EditText) v.findViewById(R.id.context);
        ImageView imageView = (ImageView) v.findViewById(R.id.img);

        if (!d.getCategory().isEmpty()) {
            category.setText(d.getCategory());
            category.setVisibility(View.VISIBLE);
        }

        //companyEdit.setText(d.getSender());
        companyEdit.setText("Zurich Insurance");
        typeEdit.setText(d.getType());
        imageView.setImageBitmap(bitmapImage);

        if (context.containsKey("dateCreation")) {
            dateEdit.setText(context.get("dateCreation"));
        }
    }

    private void sendImage(Bitmap bitmap, String id) {
        final String ENDPOINT_IMAGE = "http://2702645a.ngrok.io/textrec/image";
        final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

        URL url = null;
        try {
            url = new URL(ENDPOINT_IMAGE);
        } catch (Exception e) {
            // Ignore
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        long timeNow = System.currentTimeMillis();
        String filename = timeNow + ".png";
        FileOutputStream outputStream;
        File file = null;
        try {
            file = File.createTempFile(filename, null, getCacheDir());
            outputStream = new FileOutputStream(file);
            outputStream.write(byteArray);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        RequestBody req = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", id)
                .addFormDataPart("file", filename, RequestBody.create(MEDIA_TYPE_PNG, file)).build();
        Request request = new Request.Builder()
                .url(url)
                .post(req)
                .build();

        OkHttpClient client = new OkHttpClient();
        String responseStr = "";
        try {
            Response response = client.newCall(request).execute();
            responseStr = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("[sendImage]", "Response: " + responseStr);
    }

    private JSONObject sendText(String text, String id) {
        System.out.println("sending text: " + text);

        final String ENDPOINT_TEXT = "http://2702645a.ngrok.io/textrec/text";
        final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

        URL url = null;
        try {
            url = new URL(ENDPOINT_TEXT);
        } catch (Exception e) {
            // Ignore
        }

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id", id)
                .addFormDataPart("text", text).build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        String responseStr = "";
        try {
            Response response = client.newCall(request).execute();
            responseStr = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("[sendText]", "Response: " + responseStr);

        JSONObject result = null;
        try {
            result = new JSONObject(responseStr);
        } catch (JSONException e) {

        }

        return result;
    }

    public void disableScrolling() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mViewPager.setOnTouchListener(new View.OnTouchListener() {

                    public boolean onTouch(View arg0, MotionEvent arg1) {
                        return true;
                    }
                });
            }
        });
    }

    public void resetConfirmationView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(0);

                View v = mCollectionPagerAdapter.getItem(1).getView();

                EditText dateCreation = (EditText) v.findViewById(R.id.date);
                TextView category = (TextView) v.findViewById(R.id.category);
                ImageView recipientImage = (ImageView) v.findViewById(R.id.profileImage);

                dateCreation.setText("");
                category.setVisibility(View.INVISIBLE);

                /*recipientImage.setImageDrawable(getResources().getDrawable(R.drawable.default_user));

                ibanEdit.setEnabled(true);
                nameEdit.setEnabled(true);
                amountEdit.setEnabled(true);

                nameEdit.setText("");
                amountEdit.setText("");
                ibanEdit.setText("");

                amountEdit.clearAnimation();
                titleView.setText("Money transfer");
                purchasableView.setImageDrawable(getResources().getDrawable(R.drawable.ic_dollar_bill));
                message.setHint("Enter reference line");
                message.setText("");

                Button payBtn = (Button) v.findViewById(R.id.payButton);
                payBtn.setText("SEND NOW");
                payBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.right_24dp), null);
                ((ProgressBar) v.findViewById(R.id.payProgress)).setVisibility(View.INVISIBLE);
                ((ImageView) v.findViewById(R.id.paySuccess)).setVisibility(View.INVISIBLE);*/
            }
        });
    }

    public Document getCurrentDoc(){
        return this.currentDoc;
    }

    public void setCurrentDoc(Document doc){
        this.currentDoc = doc;
    }

    public boolean onTryStartProcessing(ProcessingState ps) {
        synchronized (mProcessingLock) {
            if (mProcessingState == ProcessingState.NOLOCK && mViewPager.getCurrentItem() == 1) {
                mProcessingState = ps;
                return true;
            }
            return false;
        }
    }

    public void onStopProcessing(ProcessingState ps) {
        synchronized (mProcessingLock) {
            if (mProcessingState != ps)
                Log.e(TAG, "Processing state reset by another process");
            mProcessingState = ProcessingState.NOLOCK;
        }
    }
}
