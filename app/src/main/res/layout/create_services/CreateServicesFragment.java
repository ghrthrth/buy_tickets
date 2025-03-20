package com.example.telestock.ui.create_services;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.example.telestock.R;
import com.example.telestock.databinding.FragmentCreateServicesBinding;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CreateServicesFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;

    private OkHttpClient client = new OkHttpClient();

    private Uri selectedImageUri;

    private FragmentCreateServicesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CreateServicesViewModel createServicesViewModel =
                new ViewModelProvider(this).get(CreateServicesViewModel.class);

        binding = FragmentCreateServicesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView title = binding.title;
        final TextView description = binding.description;
        final TextView price = binding.textPrice;
        final TextView category = binding.category;
/*        final TextView fio = binding.textFio;*/
        final Button send = binding.send;
        final Button selectPhoto = binding.selectPhotos;

        LottieAnimationView lottieProgress = binding.lottieProgress; // Инициализируем LottieAnimationView

        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Проверка разрешений
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13 и выше
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                REQUEST_CODE_PERMISSION);
                    } else {
                        openGallery();
                    }
                } else {
                    // Android 12 и ниже
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_CODE_PERMISSION);
                    } else {
                        openGallery();
                    }
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String titles = title.getText().toString();
                String descriptions = description.getText().toString();
                String categorys = category.getText().toString();
                String prices = price.getText().toString();
/*                String fios = fio.getText().toString();*/
                new HttpRequestTask(lottieProgress).execute(titles, descriptions, categorys, prices);
            }
        });
        return root;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == getActivity().RESULT_OK) {
            selectedImageUri = data.getData();
            ImageView imageView = binding.imagePreview;
            imageView.setImageURI(selectedImageUri);
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String realPath = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            realPath = cursor.getString(column_index);
            cursor.close();
        }
        return realPath != null ? realPath : uri.getPath(); // Возвращаем исходный путь, если не удалось получить реальный путь
    }

    private class HttpRequestTask extends AsyncTask<String, Void, String> {
        private LottieAnimationView lottieProgress;

        public HttpRequestTask(LottieAnimationView lottieProgress) {
            this.lottieProgress = lottieProgress;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            binding.loadingOverlay.setVisibility(View.VISIBLE); // Показываем экран загрузки
            lottieProgress.setVisibility(View.VISIBLE);  // Показываем анимацию
            lottieProgress.playAnimation(); // Запускаем анимацию
            binding.send.setEnabled(false);  // Отключаем кнопку отправки
        }

        @Override
        protected String doInBackground(String... params) {
            String title = params[0];
            String description = params[1];
            String category = params[2];
            String price = params[3]; // Получаем категорию
/*            String fio = params[3];*/

            try {
                MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("title", title)
                        .addFormDataPart("description", description)
                        .addFormDataPart("category", category)
                        .addFormDataPart("price", price);
/*                        .addFormDataPart("fio", fio);*/

                if (selectedImageUri != null) {
                    String filePath = getRealPathFromUri(selectedImageUri);
                    File file = new File(filePath);
                    builder.addFormDataPart("photo", file.getName(),
                            RequestBody.create(MediaType.parse("image/*"), file));
                }

                RequestBody requestBody = builder.build();
                Request request = new Request.Builder()
                        .url("https://claimbes.store/telestock/admin_api/add.php")
                        .post(requestBody)
                        .build();

                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();
                return response.body() != null ? response.body().string() : "No response";
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            binding.loadingOverlay.setVisibility(View.GONE);  // Скрываем экран загрузки
            lottieProgress.setVisibility(View.GONE);  // Скрываем анимацию после завершения отправки
            lottieProgress.cancelAnimation();  // Останавливаем анимацию
            binding.send.setEnabled(true);  // Включаем кнопку отправки

            if (result != null && result.contains("Error")) {
                Toast.makeText(getContext(), "Ошибка: " + result, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Данные успешно отправлены", Toast.LENGTH_SHORT).show();

                // Очистка полей
                binding.title.setText("");         // Очистка заголовка
                binding.description.setText("");   // Очистка описания
                binding.category.setText("");
                binding.textPrice.setText("");     // Очистка цены

                binding.imagePreview.setImageResource(R.drawable.ic_menu_camera);
                selectedImageUri = null; // Сброс URI изображения
            }
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        // Отменяем все асинхронные запросы
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }
}
