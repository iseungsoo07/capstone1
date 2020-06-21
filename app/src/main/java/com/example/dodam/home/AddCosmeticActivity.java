package com.example.dodam.home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.dodam.R;
import com.example.dodam.data.IngredientItem;
import com.example.dodam.data.IngredientItemData;
import com.example.dodam.database.Callback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class AddCosmeticActivity extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView ingredientRV;
    private IngredientItemRVAdapter ingredientItemRVAdapter;
    private final int REQUEST_CAPTURE_IMAGE = 1;
    private final int REQUEST_CAPTURE_INGREDENT = 2;
    private final int REQUEST_CAPTURE_CROP = 3;
    private String currentPhotoPath;
    private Bitmap cosmeticBitmap;  // 제품 사진 Bitmap
    private String selectedTexts;   // Dialog에서 선택한 문자열
    private ArrayList<String> ingredients;  // 추출한 성분

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_cosmetic);

        // 필요한 항목 초기화
        initialize();
    }

    public class parsingAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1471057/FcssReportPrdlstInforService/getReportItemList"); /*URL*/
                urlBuilder.append("?" + URLEncoder.encode("ServiceKey","UTF-8") + "=uiUB1CijP3FPRbVz%2B0JCzKoB90rQvVWrfcKYgCvJuSvH3jXAcUtYjsJHb%2FM2Jbt%2FwL8BYOTRfn12j5jDsT58Mg%3D%3D"); /*Service Key*/
                urlBuilder.append("&" + URLEncoder.encode("item_seq","UTF-8") + "=" + URLEncoder.encode("", "UTF-8")); /*품목일련번호*/
                urlBuilder.append("&" + URLEncoder.encode("item_name","UTF-8") + "=" + URLEncoder.encode("자오연자하거진액", "UTF-8")); /*품목명*/
                urlBuilder.append("&" + URLEncoder.encode("cosmetic_report_seq","UTF-8") + "=" + URLEncoder.encode("2008000103", "UTF-8")); /*보고일련번호*/
                urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")); /*페이지 번호*/
                urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("3", "UTF-8")); /*한 페이지 결과수*/

                URL url = new URL(urlBuilder.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");

                conn.setRequestProperty("Content-type", "application/json");

                conn.connect();

                System.out.println("Response code: " + conn.getResponseCode());
                BufferedReader rd;
                if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                conn.disconnect();
                System.out.println("성공: " + sb.toString());
            } catch (IOException e) {
                System.out.println("실패");
                e.printStackTrace();
            }

            return null;
        }
    }

    // 필요한 항목 초기화
    private void initialize() {
        initializeImageView();
        initializeButton();
        initializeRecyclerView();

        //parsingAsyncTask asyncTask = new parsingAsyncTask();
        //asyncTask.execute();
    }

    // ImageView 초기화
    private void initializeImageView() {
        ImageView backIV, addCosmeticIV;

        backIV = findViewById(R.id.addCosmetic_backIV);
        addCosmeticIV = findViewById(R.id.addCosmetic_addCosmeticImageIV);

        backIV.setOnClickListener(this);
        addCosmeticIV.setOnClickListener(this);
    }

    // Button 초기화
    private void initializeButton() {
        Button completeBtn, captureIngredientBtn;

        completeBtn = findViewById(R.id.addCosmetic_completeBtn);
        captureIngredientBtn = findViewById(R.id.addCosmetic_captureIngredientBtn);

        completeBtn.setOnClickListener(this);
        captureIngredientBtn.setOnClickListener(this);
    }

    // RecyclerView 초기화
    private void initializeRecyclerView() {
        ingredientRV = findViewById(R.id.addCosmetic_ingredientRV);

        ingredientRV.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        ingredientItemRVAdapter = new IngredientItemRVAdapter();

        ingredientRV.setAdapter(ingredientItemRVAdapter);
    }

    // 추출한 성분 RecyclerView에 추가
    private void addIngredientsToRecyclerView() {
        // 먼저 초기화
        ingredientItemRVAdapter.delAllItem();

        // 성분 하나씩 뽑아서 추가
        for(String ingredient : ingredients) {
            IngredientItemData ingredientItemData;

            ingredientItemData = new IngredientItemData();

            ingredientItemData.setIngredientName(ingredient);

            ingredientItemRVAdapter.addItem(ingredientItemData);
        }

        // 변경됬음을 표시
        ingredientItemRVAdapter.notifyDataSetChanged();
    }

    // 브랜드 명 및 제품 명 EditText 사용 가능하게
    private void setUsableBrandAndCosmeticName() {
        EditText brandNameET, cosmeticNameET;

        brandNameET = findViewById(R.id.addCosmetic_brandNameET);
        cosmeticNameET = findViewById(R.id.addCosmetic_cosmeticNameET);

        brandNameET.setCursorVisible(true);
        brandNameET.setFocusable(true);

        cosmeticNameET.setCursorVisible(true);
        cosmeticNameET.setFocusable(true);

        // 키보드가 화면 가리는 현상 방지
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // 브랜드 명에 포커스주기
        brandNameET.requestFocus();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            // 뒤로가기
            case R.id.addCosmetic_backIV:
                finish();

                break;

            // 완료
            case R.id.addCosmetic_completeBtn:
                break;

            // 화장품 이미지 등록
            case R.id.addCosmetic_addCosmeticImageIV:
                // 카메라 띄우기
                showCamera(REQUEST_CAPTURE_IMAGE);

                break;

            // 성분 찍기
            case R.id.addCosmetic_captureIngredientBtn:
                // 카메라 띄우기
                showCamera(REQUEST_CAPTURE_INGREDENT);

                break;
        }
    }

    // 카메라 띄우기
    private void showCamera(int flag) {
        Intent intent;
        File photoFile;
        Uri photoUri;

        photoFile = null;

        try {
            photoFile = createImageFile();
        } catch(IOException e) {
            System.out.println("임시 이미지 파일 생성 실패: " + e.getMessage());

            return;
        }

        photoUri = FileProvider.getUriForFile(this, "com.example.dodam.fileprovider", photoFile);

        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        startActivityForResult(intent, flag);
    }

    // 카메라 요청에 대한 응답
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 요청이 성공
        if(resultCode == RESULT_OK) {
            // 카메라 요청에 대한 응답일 때
            if (requestCode == REQUEST_CAPTURE_IMAGE || requestCode == REQUEST_CAPTURE_INGREDENT) {
                ImageDecoder.Source source;
                ImageView addCosmeticIV;
                File file;

                cosmeticBitmap = null;

                file = new File(currentPhotoPath);

                try {
                    if (Build.VERSION.SDK_INT < 28) {
                        cosmeticBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(file));
                    } else {
                        source = ImageDecoder.createSource(this.getContentResolver(), Uri.fromFile(file));
                        cosmeticBitmap = ImageDecoder.decodeBitmap(source);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 제품 촬영 때
                if(requestCode == REQUEST_CAPTURE_IMAGE) {
                    // 브랜드 명과 제품 명을 직접 입력할 건지 선택 Dialog 띄우기
                    showChoiceInputDialog();

                    addCosmeticIV = findViewById(R.id.addCosmetic_addCosmeticImageIV);

                    addCosmeticIV.setImageBitmap(cosmeticBitmap);
                } else if(requestCode == REQUEST_CAPTURE_INGREDENT) {
                    // 성분 촬영
                    cropImage(file);
                }
            } else if(requestCode == REQUEST_CAPTURE_CROP) {
                File file;
                ImageDecoder.Source source;

                cosmeticBitmap = null;

                file = new File(currentPhotoPath);

                try {
                    if (Build.VERSION.SDK_INT < 28) {
                        cosmeticBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(file));
                    } else {
                        source = ImageDecoder.createSource(this.getContentResolver(), Uri.fromFile(file));
                        cosmeticBitmap = ImageDecoder.decodeBitmap(source);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                analysisBitmap(cosmeticBitmap, REQUEST_CAPTURE_INGREDENT);
            }
        }
    }

    // Crop 요청
    private void cropImage(File file) {
        Intent intent;
        Uri photoUri;

        photoUri = FileProvider.getUriForFile(this, "com.example.dodam.fileprovider", file);

        intent = new Intent("com.android.camera.action.CROP");

        intent.setDataAndType(photoUri, "image/*");

        intent.putExtra("outputX", cosmeticBitmap.getWidth());
        intent.putExtra("outputY", cosmeticBitmap.getHeight());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        // Uri 접근에 대한 권한 승인
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        grantUriPermission("com.example.dodam.fileprovider", photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_CAPTURE_CROP);
    }

    // 카메라로 촬영한 이미지를 파일로 저장
    private File createImageFile() throws IOException {
        String timeStamp;
        String imageFileName;
        File storageDir;
        File image;

        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        imageFileName = "JPEG_" + timeStamp + "_";

        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        image = File.createTempFile(imageFileName, ".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    // 비트맵 분석
    private void analysisBitmap(Bitmap bitmap, final int requestCode) {
        FirebaseVisionImage visionImage;
        FirebaseVisionTextRecognizer detector;
        FirebaseVisionCloudTextRecognizerOptions options;

        options = new FirebaseVisionCloudTextRecognizerOptions.Builder().setLanguageHints(Arrays.asList("en", "ko")).build();

        detector = FirebaseVision.getInstance().getCloudTextRecognizer(options);

        visionImage = FirebaseVisionImage.fromBitmap(bitmap);

        detector.processImage(visionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                // 제품 촬영 때
                if(requestCode == REQUEST_CAPTURE_IMAGE) {
                    ArrayList<String> lineTexts;

                    lineTexts = new ArrayList<String>();

                    for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                        for (FirebaseVisionText.Line line : block.getLines()) {
                            String lineText = line.getText();

                            lineTexts.add(lineText);
                        }
                    }

                    // 브랜드 및 제품명 선택
                    selectBrandAndCosmeticName(lineTexts);
                } else if (requestCode == REQUEST_CAPTURE_INGREDENT) {
                    // 성분 명 촬영 때
                    for(FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                        String blockText;
                        String preText, postText;
                        int index, nextIndex;

                        blockText = block.getText();

                        // '전성분'이 안보이면 다음 블록으로
                        if(!blockText.contains("전성분")) {
                            continue;
                        }

                        System.out.println("원본");
                        System.out.println(blockText);

                        // 먼저 블록단위로 '('로 시작해서 ')'로 끝나는 부분 전부 제거
                        while((index = blockText.indexOf('(')) != -1) {
                            preText = blockText.substring(0, index);
                            index = blockText.indexOf(')');
                            postText = blockText.substring(index + 1);

                            blockText = preText + postText;
                        }

                        // 전성분 단어 빼내기
                        index = blockText.indexOf(('분'));

                        if(index != -1) {
                            blockText = blockText.substring(index + 2);
                        }

                        ingredients = new ArrayList<String>();

                        // ',' 단위로 자르는데 앞뒤가 숫자일 경우 자르지 말고 집어넣기
                        while((index = blockText.indexOf(',')) != -1) {
                            // 뒤가 숫자인지 확인
                            if(blockText.charAt(index + 1) >= '0' && blockText.charAt(index + 1) <= '9') {
                                // 다음 ',' 찾기
                                nextIndex = blockText.indexOf(',', index + 1);

                                // 한 성분 뗴어내기
                                preText = blockText.substring(0, nextIndex);

                                index = nextIndex;
                            } else {
                                // 한 성분 뗴어내기
                                preText = blockText.substring(0, index);
                            }

                            // 성분 목록에 추가
                            ingredients.add(preText);

                            // 시작위치 재설정
                            blockText = blockText.substring(index + 1);
                        }

                        // 마지막 항목 넣기
                        ingredients.add(blockText);

                        System.out.println("추출");

                        // 문자열 안에 줄넘김 및 공백 제거하기
                        for(int i = 0; i < ingredients.size(); i++) {
                            // 줄넘김 및 공백 제거
                            ingredients.set(i, ingredients.get(i).replaceAll("(\n| )", ""));
                            System.out.println(ingredients.get(i));
                        }

                        // RecyclerView에 추가
                        addIngredientsToRecyclerView();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        });
    }

    // 브랜드 명과 제품 명을 인식으로 추출할 건지 직접 입력할 건지 Dialog 띄우기
    private void showChoiceInputDialog() {
        AlertDialog.Builder builder;
        AlertDialog alertDialog;
        String[] items = {"사진에서 추출", "직접 입력"};

        builder = new AlertDialog.Builder(this);

        builder.setTitle("브랜드 및 제품 명 입력");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 사진에서 추출
                if(which == 0) {
                    analysisBitmap(cosmeticBitmap, REQUEST_CAPTURE_IMAGE);
                } else {
                    // EditText 입력 가능하게 하고 포커스 맞추기
                    setUsableBrandAndCosmeticName();
                }
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    // 선택형 Dialog 띄우기
    private void showCheckBoxDialog(String title, final ArrayList<String> texts, final Callback<Boolean> callback) {
        final AlertDialog.Builder builder;
        final AlertDialog alertDialog;
        final ArrayList<String> selectedItems;
        final String[] items;

        items = texts.toArray(new String[texts.size()]);

        selectedItems = new ArrayList<String>();

        builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // Checked 상태
                if(isChecked == true) {
                    selectedItems.add(items[which]);
                } else {
                    selectedItems.remove(items[which]);
                }
            }
        });

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedTexts = "";

                // Checked 된 항목들 붙이기
                for(String text : selectedItems) {
                    selectedTexts += text + " ";

                    // texts에 있는 문자열중에 선택된 항목들 제외시키기
                    texts.remove(text);
                }

                // 마지막 공백 제거
                selectedTexts = selectedTexts.substring(0, selectedTexts.length() - 1);

                // 브랜드 명 바꿔주기
                if(callback != null) {
                    EditText brandNameET;

                    brandNameET = findViewById(R.id.addCosmetic_brandNameET);
                    brandNameET.setText(selectedTexts);

                    callback.onCallback(false);
                } else {
                    // 제품 명 바꿔주기
                    EditText cosmeticNameET;

                    cosmeticNameET = findViewById(R.id.addCosmetic_cosmeticNameET);
                    cosmeticNameET.setText(selectedTexts);
                }
            }
        });

        alertDialog = builder.create();

        alertDialog.show();
    }

    // 브랜드 명 선택
    private void selectBrandAndCosmeticName(final ArrayList<String> texts) {
        showCheckBoxDialog("브랜드 명 선택", texts, new Callback<Boolean>() {
            @Override
            public void onCallback(Boolean data) {
                showCheckBoxDialog("제품 명 선택", texts, null);
            }
        });
    }
}