package kim.hsl.bm;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        // 从资源文件中加载内存
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blog);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", bitmap.getWidth() + " , " +
                        bitmap.getHeight() + " , " +
                        bitmap.getByteCount());
    }

    public native String stringFromJNI();
}
